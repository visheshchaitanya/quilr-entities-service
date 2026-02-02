package com.quilr.service.kafka;

import com.quilr.entities.KafkaServiceConfigEntity;
import com.quilr.response.ServiceResponse;

import java.util.Properties;
import java.util.Set;

/**
 *
 *  * @author visheshchaitanya
 */
public interface KafkaConfigService {

    KafkaServiceConfigEntity getClusterInfoByNameAndPipeline(String service, int pipeline_id);
    Properties getConsumerDetails(String serviceName, int pipelineId);
    ServiceResponse<Boolean> createTopicIfNotExists(String bootstrapServers, String topicName);
    ServiceResponse<Boolean> checkIfTopicsExists(String bootstrapServers, String topicName);
    ServiceResponse<Set<String>> listAllTopics(String bootstrapServers);
    ServiceResponse<Boolean> deleteTopicIExists(String bootstrapServers, String topicName) ;
}
