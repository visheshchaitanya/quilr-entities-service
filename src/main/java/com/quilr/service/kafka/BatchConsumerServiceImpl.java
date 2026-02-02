package com.quilr.service.kafka;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

@Service
@Log4j2
public class BatchConsumerServiceImpl implements BatchConsumerService {

    private ExecutorService executorService;
    private List<Future<?>> consumerFutures = new ArrayList<>();
    private List<AbstractBatchKafkaConsumer> allConsumers = new ArrayList<>();

    @PostConstruct
    public void initialise() {
        this.executorService = Executors.newSingleThreadExecutor();
        executorService = Executors.newFixedThreadPool(2); // should updated based on the db configs
    }
    @Override
    public void startConsumer(AbstractBatchKafkaConsumer consumer) {
        try{
            log.info("[CID:{}] Starting Native Kafka Consumer thread {}...",consumer.getThreadId(), consumerFutures.size());
            Future<?> future = executorService.submit(consumer);
            allConsumers.add(consumer);
            consumerFutures.add(future);
            log.info("[CID:{}] Batch Kafka Consumer thread {} started.",consumer.getThreadId(), consumerFutures.size());
        }catch (Exception ex){
            log.error("Error starting consumer..", ex);
        }
    }

    @PreDestroy
    public void stopConsumer() {
        log.info("Shutting down all the Kafka Consumers...");
        allConsumers.forEach(AbstractBatchKafkaConsumer::shutdown);
        LockSupport.parkNanos(TimeUnit.SECONDS.toNanos(3));
        for (Future<?> future : consumerFutures) {
            future.cancel(true); // Interrupt the consumer thread
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Shutdown completed for all the Kafka Consumers...");
    }
}
