package com.quilr;

import com.quilr.cache.ConsumerCache;
import com.quilr.cache.KafkaCache;
import com.quilr.config.QuilrConfigs;
import com.quilr.constants.QuilrConstants;
import com.quilr.consumer.QuilrKafkaConsumer;
import com.quilr.response.ServiceResponse;
import com.quilr.service.kafka.BatchConsumerService;
import com.quilr.service.kafka.KafkaConfigService;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Properties;
import java.util.concurrent.locks.LockSupport;

@Component
@Order(0)
@Log4j2
public class AppListener implements ApplicationListener<ApplicationReadyEvent> {
    @Autowired
    @Lazy
    private com.quilr.service.kafka.BatchConsumerService batchConsumerService;


   @Value("${spring.application.name}")
    private String appName;

    @Autowired
    private QuilrConfigs quilrConfigs;

    @Autowired
    private KafkaConfigService kafkaConfigService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Properties consumerProps;


    private ConsumerCache CONSUMER_CACHE = ConsumerCache.INSTANCE;
    private KafkaCache KAFKA_CACHE = KafkaCache.INSTANCE;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("[AN:{}] Starting Consumers....", quilrConfigs.getAppName());
        quilrConfigs.setAppName(appName);
        CONSUMER_CACHE.setConfig(quilrConfigs);
        KAFKA_CACHE.setConfig(quilrConfigs);
        startConsumers();
        log.info("[AN:{}] Started Consumers....", quilrConfigs.getAppName());
        log.debug("[AN:{}] All set!", quilrConfigs.getAppName());
    }

    /**
     * Stop all services while termination.
     */
    @PreDestroy
    public void onExit() {
        cleanupServices();
    }

    private void cleanupServices() {

        log.warn("Stopping Services...");

        try {
            log.warn("[AN:{}] Entities service Closed...", quilrConfigs.getAppName());
            LockSupport.parkNanos(java.util.concurrent.TimeUnit.SECONDS.toNanos(2));
        } catch (Exception ex) {
            log.warn("[AN:{}] Thread interrupted", quilrConfigs.getAppName(), ex);
            Thread.currentThread().interrupt();
        }
        log.warn("--------------------------------------");
        log.warn("[AN:{}] Shutdown complete and Happy Exit...", quilrConfigs.getAppName());
        log.warn("--------------------------------------");
    }



    public ServiceResponse startConsumers() {
        ServiceResponse serviceResponse = new ServiceResponse();
        log.info("[SN:{}] Starting consumers ..",quilrConfigs.getAppName());
        try{
            Properties consumerProps = kafkaConfigService.getConsumerDetails(quilrConfigs.getAppName(), QuilrConstants.DEFAULT_PIPELINE_ID);
            String clusterName = consumerProps.getProperty(QuilrConstants.CLUSTER_NAME);
            String serviceName = quilrConfigs.getAppName()+"_"+ QuilrConstants.DEFAULT_PIPELINE_ID;

            try {
                consumerProps.put("key.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
                consumerProps.put("value.deserializer", "org.apache.kafka.common.serialization.StringDeserializer");
                consumerProps.put("auto.offset.reset", "latest");
                consumerProps.put("enable.auto.commit", "false");
                consumerProps.put("max.poll.records", "1000");

                int threads = (int) consumerProps.getOrDefault("num.stream.threads", 1);
                log.info("Starting {} Batch Kafka Consumer threads...",threads);
                for(int i=1; i<=threads; i++) {
                    QuilrKafkaConsumer consumer = getConsumerBean(consumerProps, batchConsumerService);
                    consumer.setThreadId(i);
                    batchConsumerService.startConsumer(consumer);
                }
            }
            catch (RuntimeException ex) {
                log.error("[SN:{}] Error starting consumer for {} ..",serviceName, clusterName, ex);
                serviceResponse.addExtraInfo(clusterName, "ERROR:" + ex.getMessage());
            }
        }catch (Exception ex){
            log.error("[SN:{}] Error starting consumers ..",quilrConfigs.getAppName(), ex);
            serviceResponse.setResponse(false);
            serviceResponse.setResponse("Error starting consumers."+ex.getMessage());
        }

        return serviceResponse;
    }

    public QuilrKafkaConsumer getConsumerBean(Properties consumerProps, BatchConsumerService batchConsumerService) {
        QuilrKafkaConsumer jbc = context.getBean(QuilrKafkaConsumer.class);
        jbc.init(consumerProps, batchConsumerService);
        return jbc;
    }


    {
    }
}
