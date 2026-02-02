package com.quilr.service.kafka;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.quilr.cache.KafkaCache;
import com.quilr.config.QuilrConfigs;
import com.quilr.constants.QuilrConstants;
import com.quilr.entities.KafkaServiceConfigEntity;
import com.quilr.kstreams.KafkaConstants;
import com.quilr.repository.KafkaServiceAppRepository;
import com.quilr.response.ServiceResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DeleteTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Kafka Service to load /retrieve kafka and app details.
 *
 * @author visheshchaitanya
 */
@Log4j2
@Service
public class KafkaConfigServiceImpl implements KafkaConfigService {

//    @Autowired
//    private KafkaClusterRepository kafkaClusterRepository;

    @Autowired
    private KafkaServiceAppRepository kafkaServiceAppRepository;

    private KafkaCache KAFKA_CACHE = KafkaCache.INSTANCE;

    private Gson GSON = new Gson();

    @Autowired
    private QuilrConfigs quilrConfigs;

    @PostConstruct
    public void initialize() {
        initCache();
    }

    private void initCache() {
//        loadClusterCache();
        loadServiceAppCache();
    }
    /**
     * Reload app cache.
     */
    public synchronized void reLoadAppCache() {
        try {
            log.info("[AN:{}] Reloading app cache..",quilrConfigs.getAppName());
            KAFKA_CACHE.getAllAppDetails().clear();
            loadServiceAppCache();
        }catch (Exception ex){
            log.error("[AN:{}] Error while re-loading app details...",quilrConfigs.getAppName(), ex);
        }
    }

    /**
     * Load service app cache from db
     */
    private void loadServiceAppCache() {
        log.info("[AN:{}] Reloading service app cache..",quilrConfigs.getAppName());
        List<KafkaServiceConfigEntity> serviceConfigEntityList = kafkaServiceAppRepository.findAll();
        if (serviceConfigEntityList != null ){
            final Map<String, Map<Integer, KafkaServiceConfigEntity>> appEntityMap = new HashMap<>();
            for (KafkaServiceConfigEntity serviceConfigEntity : serviceConfigEntityList){
                Map<Integer, KafkaServiceConfigEntity> pipelineMap = appEntityMap.getOrDefault(serviceConfigEntity.getServiceName(), new HashMap<>());
                pipelineMap.put(serviceConfigEntity.getPipelineId(), serviceConfigEntity);
                appEntityMap.put(serviceConfigEntity.getServiceName().trim(), pipelineMap);
            }
            KAFKA_CACHE.addAllToAppCache(appEntityMap);
        }
    }

    /**
     * @param serviceName
     * @param pipelineId
     * @return
     */
    @Override
    public KafkaServiceConfigEntity getClusterInfoByNameAndPipeline(String serviceName, int pipelineId){
        log.info("[AN:{}] Getting service details by name :{} and pipeline: {}",quilrConfigs.getAppName(), serviceName, pipelineId);
        return KAFKA_CACHE.getAppDetailsByName(serviceName.trim(), pipelineId);
    }


    /**
     * Get the Consumer Properties for the Kafka.
     * @param serviceName
     * @param pipelineId
     * @return
     */
    public Properties getConsumerDetails(String serviceName, int pipelineId){
        log.info("[AN:{}] Retrieving Kafka consumer details for the servicename:{} pipelineid:{}",quilrConfigs.getAppName(), serviceName, pipelineId);
        final Properties kafkaProps = new Properties();
        try{
            KafkaServiceConfigEntity serviceConfig = getClusterInfoByNameAndPipeline(serviceName.trim(), pipelineId);

            if (serviceConfig != null){

                Type mapType = new TypeToken<Map<String, Object>>(){}.getType();
                Map<String,Object> consumerProps = GSON.fromJson(serviceConfig.getSourceProperties(), mapType);

                consumerProps.forEach((k,v) -> kafkaProps.put(k,getCastedValue(v)));

                kafkaProps.put(KafkaConstants.TOPICS,serviceConfig.getSourceTopics().trim());

                kafkaProps.put(org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG, quilrConfigs.getAppName()+"_"+serviceConfig.getPipelineId()+"_appId");
                kafkaProps.put(QuilrConstants.CLUSTER_NAME, serviceConfig.getClusterName());
                kafkaProps.put(QuilrConstants.DEFAULT_PIPELINE, serviceConfig.getPipelineId()+"");

                createTopicIfNotExists(kafkaProps.getProperty("bootstrap.servers"), kafkaProps.getProperty(KafkaConstants.TOPICS));
            }else{
                log.warn("[AN:{} SN:{}] Service name not found in DB..",quilrConfigs.getAppName(),serviceConfig.getClusterName());
            }
        }catch (Exception ex){
            log.error("[AN:{}] Error while retrieving the kafka details...",quilrConfigs.getAppName(),ex);
        }
        return kafkaProps;
    }

    private Object getCastedValue(Object v) {
        if (v instanceof Double || v instanceof Long || v instanceof Integer){
            return  ((Number) v).intValue();
        }else if (v instanceof Boolean){
            return ((Boolean) v).booleanValue();
        }else{
            return v.toString();
        }
    }

    /**
     * Create topic if not exists.
     * @param bootstrapServers
     * @param topicName
     * @return
     */
    public ServiceResponse<Boolean> createTopicIfNotExists(String bootstrapServers, String topicName) {
        ServiceResponse<Boolean> serviceResponse = new ServiceResponse<>();
        try{
//        String bootstrapServers = "127.0.0.1:9092"; // change as needed
            log.info("Creation - Connecting to Kafka bootstrap servers:{} topicName:{} ",bootstrapServers, topicName);
            // --- Topic details ---
            int partitions = 5;
            short replicationFactor = 1;
            // --- Kafka AdminClient config ---
            Properties config = new Properties();
            config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
            config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 10000);

            // --- Create AdminClient ---
            try (AdminClient admin = AdminClient.create(config)) {

                // Step 1: Get existing topic names
                Set<String> existingTopics = admin.listTopics().names().get(10, TimeUnit.SECONDS);

                // Step 2: Check if topic exists
                if (existingTopics.contains(topicName)) {
                    log.info("Topic already exists: " + topicName);
                    serviceResponse.setResponse(false);
                    serviceResponse.setMessage("Topic already exists: " + topicName);
                }else{
                    // Step 3: Create new topic
                    NewTopic newTopic = new NewTopic(topicName, partitions, replicationFactor);

                    admin.createTopics(Collections.singletonList(newTopic)).all().get(10, TimeUnit.SECONDS);
                    log.info("Topic created successfully: " + topicName);
                    serviceResponse.setResponse(true);
                    serviceResponse.setMessage("Topic created successfully: " + topicName);
                }
            } catch (ExecutionException ex) {
                log.error("Error creating topic: " + ex.getCause());
                serviceResponse.setMessage("Error while creating topic: " + ex.getMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.error("Topic creation interrupted");
                serviceResponse.setMessage("Error while creating topic: " + ex.getMessage());
            }
        }catch (Exception ex){
            log.error("Error while creating topic: " + ex.getMessage());
            serviceResponse.setMessage("Error while creating topic: " + ex.getMessage());
        }

        return serviceResponse;
    }

    /**
     * Create topic if not exists.
     * @param bootstrapServers
     * @param topicName
     * @return
     */
    public ServiceResponse<Boolean> deleteTopicIExists(String bootstrapServers, String topicName) {
        ServiceResponse<Boolean> serviceResponse = new ServiceResponse<>();
        try{
//        String bootstrapServers = "127.0.0.1:9092"; // change as needed
            log.info("Deletion - Connecting to Kafka bootstrap servers:{} topicName:{} ",bootstrapServers, topicName);
            Properties config = new Properties();
            config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
            config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 10000);

            // --- Create AdminClient ---
            try (AdminClient admin = AdminClient.create(config)) {

                // Step 1: Get existing topic names
                Set<String> existingTopics = admin.listTopics().names().get(10, TimeUnit.SECONDS);

                // Step 2: Check if topic exists
                if (existingTopics.contains(topicName)) {
                    log.info("Topic already exists: " + topicName);

                    DeleteTopicsResult deleteResult = admin.deleteTopics(Collections.singletonList(topicName));

                    // Wait for deletion to complete
                    deleteResult.all().get(10, TimeUnit.SECONDS);
                    log.info("Topic deleted successfully: " + topicName);
                    serviceResponse.setResponse(true);
                    serviceResponse.setMessage("Topic deleted successfully: " + topicName);

                }else{
                    log.info("Topic not exist to delete: " + topicName);
                    serviceResponse.setResponse(true);
                    serviceResponse.setMessage("Topic not  exists to delete: " + topicName);
                }
            } catch (ExecutionException ex) {
                log.error("Error deleting topic: " + ex.getCause());
                serviceResponse.setMessage("Error while deleting topic: " + ex.getMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.error("Topic deleting interrupted");
                serviceResponse.setMessage("Error while deleting topic: " + ex.getMessage());
            }
        }catch (Exception ex){
            log.error("Error while deleting topic: " + ex.getMessage());
            serviceResponse.setMessage("Error while deleting topic: " + ex.getMessage());
        }

        return serviceResponse;
    }
    /**
     * Check if topic exists.
     * @param bootstrapServers
     * @param topicName
     * @return
     */
    public ServiceResponse<Boolean> checkIfTopicsExists(String bootstrapServers, String topicName) {

        ServiceResponse<Boolean> serviceResponse = new ServiceResponse<>();
        try{
            log.info("Connecting to Kafka bootstrap servers:{} topicName:{} ",bootstrapServers, topicName);
            Properties config = new Properties();
            config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
            config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 10000);

            // --- Create AdminClient ---
            try (AdminClient admin = AdminClient.create(config)) {

                // Step 1: Get existing topic names
                Set<String> existingTopics = admin.listTopics().names().get(10, TimeUnit.SECONDS);

                // Step 2: Check if topic exists
                if (existingTopics.contains(topicName)) {
                    log.info("Topic already exists: " + topicName);
                    serviceResponse.setResponse(true);
                    serviceResponse.setMessage("Topic already exists: " + topicName);
                }else{
                    log.info("Topic not exists: " + topicName);
                    serviceResponse.setResponse(false);
                    serviceResponse.setMessage("Topic not exists: " + topicName);
                }
            } catch (ExecutionException ex) {
                log.error("Error checking topic: " + ex.getCause());
                serviceResponse.setMessage("Error while creating topic: " + ex.getMessage());
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                log.error("Topic checking interrupted");
                serviceResponse.setMessage("Error while creating topic: " + ex.getMessage());
            }
        }catch (Exception ex){
            log.error("Error while checking topic: " + ex.getMessage());
            serviceResponse.setMessage("Error while checking topic: " + ex.getMessage());
        }
        return serviceResponse;

    }

    /**
     * Check if topic exists.
     * @param bootstrapServers
     * @return
     */
    public ServiceResponse<Set<String>> listAllTopics(String bootstrapServers) {

        ServiceResponse<Set<String>> serviceResponse = new ServiceResponse<>();
        try{
            log.info("Connecting to Kafka bootstrap servers:{} ",bootstrapServers);
            Properties config = new Properties();
            config.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
            config.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 10000);
            config.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 10000);

            // --- Create AdminClient ---
            try (AdminClient admin = AdminClient.create(config)) {

                // Step 1: Get existing topic names
                Set<String> existingTopics = admin.listTopics().names().get(10, TimeUnit.SECONDS);
                serviceResponse.setResponse(existingTopics);

            } catch (Exception ex) {
                log.error("Error checking topic: " + ex.getCause());
                serviceResponse.setMessage("Error while creating topic: " + ex.getMessage());
            }
        }catch (Exception ex){
            log.error("Error while checking topic: " + ex.getMessage());
            serviceResponse.setMessage("Error while checking topic: " + ex.getMessage());
        }
        return serviceResponse;

    }
}
