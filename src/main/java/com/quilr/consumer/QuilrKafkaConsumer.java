package com.quilr.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quilr.dto.RawEntityMessage;
import com.quilr.dto.TransformedEntity;
import com.quilr.service.EntityOutputService;
import com.quilr.service.EntityProcessingService;
import com.quilr.service.kafka.AbstractBatchKafkaConsumer;
import com.quilr.service.kafka.BatchConsumerService;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.*;

@Component
@Scope("prototype")
@Log4j2
public class QuilrKafkaConsumer extends AbstractBatchKafkaConsumer<String, String> implements Callable<Boolean> {

    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private EntityProcessingService entityProcessingService;
    
    @Autowired
    private EntityOutputService entityOutputService;
    
    @Value("${quilr.transformers.enabled:false}")
    private boolean transformersEnabled;

    private BatchConsumerService batchConsumerService;

    public QuilrKafkaConsumer() {
        super("org.apache.kafka.common.serialization.StringDeserializer",
                "org.apache.kafka.common.serialization.StringDeserializer");
    }

    /**
     * Required init method from AbstractBatchKafkaConsumer
     */
    @Override
    public void init(Properties props, BatchConsumerService batchConsumerService) {
        this.batchConsumerService = batchConsumerService;
        initProperties(props);
        log.info("QuilrKafkaConsumer initialized with properties and batchConsumerService");
    }

    @Override
    public boolean processBatchOfRecords(List<ConsumerRecord<String, String>> records, Map<TopicPartition, OffsetAndMetadata> offsetsToCommit) {
        if (records == null || records.isEmpty()) {
            return true; // Nothing to process
        }

        try {
            log.info("[CID:{}] Processing batch of {} records (Transformers enabled: {})", 
                this.getThreadId(), records.size(), transformersEnabled);

            int successCount = 0;
            int failureCount = 0;

            for (ConsumerRecord<String, String> record : records) {
                try {
                    if (transformersEnabled) {
                        // NEW: Transformation pipeline
                        processWithTransformation(record);
                    } else {
                        // LEGACY: Simple logging (backward compatibility)
                        log.info("[CID:{}] Consumed record - Topic: {}, Partition: {}, Offset: {}, Key: {}",
                            this.getThreadId(),
                            record.topic(),
                            record.partition(),
                            record.offset(),
                            record.key());
                    }
                    
                    successCount++;
                } catch (Exception ex) {
                    failureCount++;
                    log.error("[CID:{}] Error processing record at offset {}: {}", 
                        this.getThreadId(), record.offset(), ex.getMessage(), ex);
                }
            }
            
            log.info("[CID:{}] Batch processing complete - Success: {}, Failed: {}", 
                this.getThreadId(), successCount, failureCount);
            
            return true;
            
        } catch (Exception e) {
            log.error("[CID:{}] Error processing batch: {}", 
                this.getThreadId(), e.getMessage(), e);
            return false;
        } finally {
            commitProcessedOffsets(offsetsToCommit);
            records.clear();
        }
    }
    
    /**
     * Process record through transformation pipeline.
     * Flow: Parse JSON → Transform → Output
     */
    private void processWithTransformation(ConsumerRecord<String, String> record) {
        try {
            // Step 1: Parse JSON to RawEntityMessage
            String value = record.value();
            RawEntityMessage rawMessage = objectMapper.readValue(value, RawEntityMessage.class);
            
            log.debug("[CID:{}] Parsed message - Vendor: {}, Type: {}", 
                this.getThreadId(), rawMessage.getVendor(), rawMessage.getType());
            
            // Step 2: Transform through vendor-specific transformer
            TransformedEntity transformedEntity = entityProcessingService.processEntity(rawMessage);
            
            // Step 3: Output transformed entity
            entityOutputService.handleTransformedEntity(transformedEntity);
            
            log.debug("[CID:{}] Successfully processed and transformed record at offset {}", 
                this.getThreadId(), record.offset());
            
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            log.error("[CID:{}] JSON parsing error at offset {}: {}. Falling back to logging.", 
                this.getThreadId(), record.offset(), e.getMessage());
            // Fallback to logging if JSON parsing fails
            logRecordAsIs(record);
        } catch (IllegalArgumentException e) {
            log.error("[CID:{}] Invalid message or unsupported vendor at offset {}: {}. Falling back to logging.", 
                this.getThreadId(), record.offset(), e.getMessage());
            // Fallback to logging if vendor not supported
            logRecordAsIs(record);
        } catch (Exception e) {
            log.error("[CID:{}] Transformation error at offset {}: {}. Falling back to logging.", 
                this.getThreadId(), record.offset(), e.getMessage(), e);
            // Fallback to logging on any other error
            logRecordAsIs(record);
        }
    }
    
    /**
     * Fallback method to log record as-is (legacy behavior)
     */
    private void logRecordAsIs(ConsumerRecord<String, String> record) {
        log.info("[CID:{}] RAW_RECORD - Topic: {}, Partition: {}, Offset: {}, Key: {}",
            this.getThreadId(),
            record.topic(),
            record.partition(),
            record.offset(),
            record.key());
    }

    @Override
    public void sendToDlqNative(List<ConsumerRecord<String, String>> records, Map<TopicPartition, OffsetAndMetadata> offsetsToCommit) {
        if(isDlqEnabled()){
            for (ConsumerRecord<String, String> record : records) {
                ProducerRecord<String, String> producerRecord = new ProducerRecord<>(getDlqTopic(), record.key(), record.value());
                getDlqProducer().send(producerRecord, (metadata, exception) -> {
                    if (exception == null) {
                        log.info("[CID:{}] Sent to DLQ - Topic: {}, Offset: {}", 
                            this.getThreadId(), getDlqTopic(), metadata.offset());
                    } else {
                        log.error("[CID:{}] Failed to send to DLQ: {}", 
                            this.getThreadId(), exception.getMessage());
                    }
                });
            }
        }
    }

    @Override
    public boolean commitProcessedOffsets(Map<TopicPartition, OffsetAndMetadata> offsets) {
        try {
            if (consumer != null) {
                consumer.commitSync(offsets);
                log.info("[CID:{}] Committed offsets for {} partitions", 
                    this.getThreadId(), offsets.size());
                offsets.clear();
            }
        } catch (Exception e) {
            log.error("[CID:{}] Error committing offsets: {}", 
                this.getThreadId(), e.getMessage(), e);
            return false;
        }
        return true;
    }
}
