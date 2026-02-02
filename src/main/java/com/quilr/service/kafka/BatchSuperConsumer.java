package com.quilr.service.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public interface BatchSuperConsumer<K, V> {
    Boolean call() throws Exception;
    boolean processBatchOfRecords(List<ConsumerRecord<K, V>> records, Map<TopicPartition, OffsetAndMetadata> offsetsToCommit);
    boolean commitProcessedOffsets(Map<TopicPartition, OffsetAndMetadata> offsets);
    void handleError(Exception e);
    void init(Properties props, BatchConsumerService batchConsumerService);
}
