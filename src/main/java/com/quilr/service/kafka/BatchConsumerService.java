package com.quilr.service.kafka;

public interface BatchConsumerService {
    public void initialise();
    void startConsumer(AbstractBatchKafkaConsumer consumer);
    void stopConsumer();
}
