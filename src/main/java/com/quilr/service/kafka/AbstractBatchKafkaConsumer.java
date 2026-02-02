package com.quilr.service.kafka;

import com.quilr.kstreams.KafkaConstants;
import lombok.Setter;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang.StringUtils;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreatePartitionsResult;
import org.apache.kafka.clients.admin.NewPartitions;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
@Log4j2
public abstract class AbstractBatchKafkaConsumer<K, V> implements Callable<Boolean>, BatchSuperConsumer<K, V> {

    @Setter
    private int threadId;
    protected Properties consumerProps = new Properties();
    protected KafkaConsumer<K, V> consumer;
    protected final AtomicBoolean running = new AtomicBoolean(true);
    private int maxRetries;
    private long retryBackoffMs;
    private KafkaProducer<K, V> dlqProducer;
    private final boolean dlqEnabled = false;
    private String dlqTopic = "quilr_entities_dlp";
    private String dlqProducerAcks = "all";
    private String dlqProducerRetries = "3";
    private String dlqProducerLingerMs = "1000";
    private String sourceTopic;

    final List<ConsumerRecord<K, V>> allPartitionRecords = new ArrayList<>();
    Map<TopicPartition, OffsetAndMetadata> offsetsToCommit = new HashMap<>();

    public AbstractBatchKafkaConsumer(String keyDeserializer, String valueDeserializer) {
        this.consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
        this.consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
    }
    public void initProperties(Properties kafkaProps){
        this.consumerProps.putAll(kafkaProps);
        this.sourceTopic = kafkaProps.getProperty(KafkaConstants.TOPICS);
        this.maxRetries = StringUtils.isNotEmpty(kafkaProps.getProperty("maxRetries")) ? Integer.parseInt(kafkaProps.getProperty("maxRetries").trim()):3;
        this.retryBackoffMs = StringUtils.isNotEmpty(kafkaProps.getProperty("retryBackoffMs")) ? Integer.parseInt(kafkaProps.getProperty("retryBackoffMs").trim()):30000;

        log.info("Properties are....");
    }

    public void shutdown() {
        try{
            log.info("[CID:{}] Shutting down consumer...",this.threadId );
            running.set(false);
            LockSupport.parkNanos(java.util.concurrent.TimeUnit.SECONDS.toNanos(2));
            log.info("[CID:{}] Processing remaining records in batch before shutting down consumer...{} {}",this.threadId, allPartitionRecords.size(), offsetsToCommit.size() );
            processBatchOfRecords(allPartitionRecords, offsetsToCommit);
            log.info("[CID:{}] Processed remaining records in batch before shutting down consumer...{} {}",this.threadId, allPartitionRecords.size(), offsetsToCommit.size() );
            if (consumer != null) {
                consumer.wakeup();
            }

        }catch (Exception e){
            log.error("[CID:{}] Error shutting down consumer: {}",this.threadId, e.getMessage(), e);
        }finally {
            if (dlqProducer != null) {
                dlqProducer.close();
            }
        }

    }

    protected void initializeConsumer() {
        try{
            consumerProps.forEach((k,v) -> {
                log.info("[CID:{}] Properties Key: {}, Value: {}",this.threadId, k, v);

            });

            consumer = new KafkaConsumer<>(consumerProps);
            consumer.subscribe(Collections.singletonList(this.sourceTopic ));
            Map<String, List<PartitionInfo>> topicsList = consumer.listTopics(Duration.ofSeconds(5));
            if (topicsList.size() > 0){
                log.info("[[CID:{}] topics:{}] Consumer intialised...",this.threadId, this.sourceTopic);
            }else{
                log.warn("[CID:{}] Kafka is not available",this.threadId);
                throw new RuntimeException("Kafka is not available "+this.threadId);
            }

        }catch (Exception e){
            log.error("Error while connecting Kafka----",e);
            throw new RuntimeException("CID: "+this.threadId+" Error initializing Kafka consumer: "+e.getMessage(), e);
        }
    }

    /**
     * Initializes the Kafka consumer with retry logic. The method attempts to initialize the consumer
     * using a configurable retry template. If initialization fails after the maximum number of retries,
     * an exception is thrown.
     *
     * The retry logic logs each attempt, including the number of retries conducted so far.
     * In case all retry attempts are exhausted, a detailed error message is logged,
     * and a RuntimeException is thrown to indicate the failure.
     *
     * This method is designed to ensure robustness in initializing the Kafka consumer in scenarios
     * where transient errors might prevent a successful initialization on the first attempt.
     *
     * The retry behavior, including backoff policies and the maximum number of attempts,
     * is determined by the configuration of the retry template.
     *
     * @throws RuntimeException if the Kafka consumer initialization fails after all retry attempts.
     */
    public void initializeWithRetry() {
        getRetryTemplate().execute(context -> {
            log.info("[CID:{}] Initializing Kafka consumer. Attempt {} ct:{}",this.threadId, context.getRetryCount() + 1, System.currentTimeMillis());
            initializeConsumer();
            return null;
        }, context -> {
            log.error("[CID:{}] All retry attempts exhausted. Failed to initialize Kafka consumer. retry.count: {}",this.threadId, context.getRetryCount());
            System.exit(0);
            throw new RuntimeException("CID: "+this.threadId+" Kafka consumer initialization failed after retries retry.count: "+context.getRetryCount());
        });
    }


    /**
     *
     * @return
     */
    public RetryTemplate getRetryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();
        long currentRetryCount = 0;
        // Retry up to 5 times
        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy();
        retryPolicy.setMaxAttempts(5);

        // Exponential backoff
        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000);  // 1 second
        backOffPolicy.setMultiplier(2.0);        // double each time
        backOffPolicy.setMaxInterval(10000);     // cap at 10 seconds

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        log.info("[CID:{}] Retry template initialized with max retries: {} and backoff policy: {}",this.threadId, retryPolicy.getMaxAttempts(), backOffPolicy.getClass().getSimpleName());
        return retryTemplate;
    }

    @Override
    public Boolean call() throws Exception {
        consumeAndProcess();
        return null;
    }

    /**
     * Consumes and processes messages from a Kafka topic in a batch-oriented manner.
     *
     * This method initializes a Kafka consumer to poll records from the configured topic,
     * processes them in batches, and commits offsets to Kafka if processing is successful.
     * It accounts for retries and error handling, ensuring reliability and fault tolerance.
     *
     * Key features include:
     * - Continuously polls records from the Kafka topic while the consumer is running and
     *   the thread is not interrupted.
     * - Aggregates records from all partitions to a single list for batch processing.
     * - Processes each batch of records with retry logic until either the batch is successfully
     *   processed or the maximum retry count is reached.
     * - On successful processing, commits the offsets for the processed records.
     * - Handles interruptions and errors gracefully by providing appropriate log messages,
     *   cleanup, and, where feasible, attempts recovery for unprocessed records.
     *
     * Note that this method assumes:
     * - Proper initialization of the Kafka consumer.
     * - The implementation of the `processBatchOfRecords` method which defines the logic
     *   for processing the records.
     * - The implementation of the `commitProcessedOffsets` method which handles the offset
     *   commits after processing.
     *
     * Error handling:
     * - Logs any errors encountered during batch processing or offset committing.
     * - Attempts retries on processing failures up to the configured maximum retries.
     * - In case of thread interruption or critical failures during processing, it logs the
     *   size of the unprocessed records and provides recommendations for recovery strategies
     *   (e.g., rollback or storing data temporarily for reprocessing).
     *
     * Lifecycle management:
     * - Ensures proper shutdown of the Kafka consumer even if exceptions occur.
     */
    protected void consumeAndProcess() {
        log.info("[CID:{}] Starting to consume messages from topic: {}",this.threadId, this.sourceTopic );
        initializeWithRetry();
        try {
            while (running.get() && !Thread.currentThread().isInterrupted()) {
                ConsumerRecords<K, V> records = consumer.poll(Duration.ofMillis(100));
                if (!records.isEmpty()) {
                    log.info("[CID:{}] Received a batch of {} messages",this.threadId, records.count());
                    int retryCount = 0;
                    boolean batchProcessed = false;

                    for (TopicPartition partition : records.partitions()) {
                        allPartitionRecords.addAll(records.records(partition));
                        if (!records.records(partition).isEmpty()) {
                            long lastOffset = records.records(partition).get(records.records(partition).size() - 1).offset();
                            offsetsToCommit.put(partition, new OffsetAndMetadata(lastOffset + 1)); // Initial commit offset
                        }
                    }

                    while (retryCount <= maxRetries && !batchProcessed && running.get()) {
                        try {
                            log.info("thread_id: {} allPartitionRecords: {}",this.threadId, allPartitionRecords.size());
                            batchProcessed = processBatchOfRecords(allPartitionRecords, offsetsToCommit);
                            if (batchProcessed && !offsetsToCommit.isEmpty()) {
                                commitProcessedOffsets(offsetsToCommit);
                            }
                        } catch (Exception e) {
                            log.error("[CID:{}] Error processing batch (attempt {}): {}",this.threadId, retryCount + 1, e.getMessage(), e);
                            retryCount++;
                            if (retryCount <= maxRetries && running.get()) {
                                log.warn("[CID:{}] Retrying batch processing (attempt {})...",this.threadId, retryCount);
                                try {
                                    Thread.sleep(retryBackoffMs);
                                } catch (InterruptedException ie) {
                                    Thread.currentThread().interrupt();
                                    running.set(false);
                                    Thread.currentThread().interrupt(); // Re-interrupt
                                }
                            } else {
                                log.error("[CID:{}] Max retries reached for batch. Skipping commit.",this.threadId);
                                // Consider error handling for the entire batch (e.g., send to DLQ)
                                break; // Exit retry loop
                            }
                        }
                    }
                }
            }
            log.warn("[CID:{}] Consumer thread interrupted: {}, Shutting down... status:{}",this.threadId, running.get(), Thread.currentThread().isInterrupted() );
        } catch (WakeupException e) {
            if (running.get()) {
                log.info("Consumer woken up.");
            }
        }catch (Exception e){
            log.error("Error while consuming messages from topic: {}", this.sourceTopic, e);
        }
        finally {
            if (Thread.currentThread().isInterrupted() && !allPartitionRecords.isEmpty()) {
                log.warn("[CID:{}] Consumer thread interrupted with {} unprocessed records in the current batch. Consider your rollback or recovery strategy.",this.threadId, allPartitionRecords.size());
                // You might want to:
                // - Log these records for potential reprocessing later.
                // - Attempt to store them in a temporary buffer.
                // - Based on your application's requirements, you might even attempt a partial commit.

                commitProcessedOffsets(offsetsToCommit);
            }
            if (consumer != null) {
                consumer.close();
                log.info("[CID:{}] Consumer closed.",this.threadId);
            }
        }
    }

    public void initializeDlqProducer() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, props.getProperty("bootstrap.servers"));
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,  props.getProperty ("key.serializer"));
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, props.getProperty ( "value.serializer"));
        props.put(ProducerConfig.ACKS_CONFIG, dlqProducerAcks);
        props.put(ProducerConfig.RETRIES_CONFIG, dlqProducerRetries);
        props.put(ProducerConfig.LINGER_MS_CONFIG, dlqProducerLingerMs);
        this.dlqProducer = new KafkaProducer<K,V>(props);
    }

    protected abstract void sendToDlqNative(List<ConsumerRecord<String, String>> records, Map<TopicPartition, OffsetAndMetadata> offsetsToCommit);
    @Override
    public void handleError(Exception e) {
        log.error("[CID:{}] Error in consumer: {}",this.threadId, e.getMessage(), e);
        // Implement specific error handling logic at the abstract level if needed
    }

    public String getDlqTopic() {
        return dlqTopic;
    }

    public KafkaProducer<K, V> getDlqProducer() {
        return dlqProducer;
    }

    public boolean isDlqEnabled() {
        return dlqEnabled;
    }

    public Properties getConsumerProps() {
        return consumerProps;
    }

    public int getThreadId() {
        return threadId;
    }

}
