package com.quilr.cache;

import com.quilr.config.QuilrConfigs;
import com.quilr.entities.KafkaServiceConfigEntity;
import lombok.extern.log4j.Log4j2;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache for Kafka service
 *
 * @author visheshchaitanya
 */
@Log4j2
public enum KafkaCache {

    INSTANCE;

    private QuilrConfigs quilrConfigs;
    public void setConfig(QuilrConfigs quilrConfigs){
        this.quilrConfigs = quilrConfigs;
    }
//    /**
//     * Cache to hold cluster_name and properties - ex: brokers, ssl info
//     */
//    private Map<String, KafkaClusterEntity> CLUSTER_MAP = new HashMap<>();
    /**
     * Cache to hold service name, pipeline, consumer and producer properties - ex: ai_axis, 1, topics,
     */
    private Map<String, Map<Integer, KafkaServiceConfigEntity>> APP_DETAILS_MAP = new HashMap<>();


//    /**
//     * Retrieve all kafka cluster details from db and add to cache
//     * @param clusterMap
//     */
//    public synchronized void addAllToClusterCache(Map<String, KafkaClusterEntity> clusterMap){
//        CLUSTER_MAP.putAll(clusterMap);
//    }

//    /**
//     * Add / update kafka cluster details to cache
//     * @param kafkaCluster
//     * @param clusterEntity
//     */
//    public synchronized void addToClusterCache(String kafkaCluster, KafkaClusterEntity clusterEntity){
//        CLUSTER_MAP.put(kafkaCluster, clusterEntity);
//    }
//
//    /**
//     * Retrieve all cluster details.
//     * @return
//     */
//    public  Map<String, KafkaClusterEntity> getAllClusterDetails(){
//        return CLUSTER_MAP;
//    }
//
//    /**
//     * Get cluster details by cluster name.
//     * @param clusterName
//     * @return
//     */
//    public  KafkaClusterEntity getClusterDetailsByName(String clusterName){
//        return CLUSTER_MAP.get(clusterName);
//    }


    /**
     * Add all service app details to cache.
     * @param appMap
     */
    public synchronized void addAllToAppCache(Map<String, Map<Integer, KafkaServiceConfigEntity>> appMap){
        APP_DETAILS_MAP.putAll(appMap);
    }

    /**
     * Add the service details to cache by name.
     * @param serviceName
     * @param pipelineAppMap
     */
    public synchronized void addToAppCache(String serviceName, Map<Integer, KafkaServiceConfigEntity> pipelineAppMap){
        Map<Integer, KafkaServiceConfigEntity> exPipelineAppMap = APP_DETAILS_MAP.getOrDefault(serviceName, new HashMap<>());
        exPipelineAppMap.putAll(pipelineAppMap);
        APP_DETAILS_MAP.put(serviceName, exPipelineAppMap);

    }

    /**
     * Get the all service kafka consumer & producer details
     * @return
     */
    public  Map<String, Map<Integer, KafkaServiceConfigEntity>> getAllAppDetails(){
        return APP_DETAILS_MAP;
    }

    /**
     * Get the service kafka consumer & producer details by name and pipeline id.
     * @param clusterName
     * @param pipelineId
     * @return
     */
    public  KafkaServiceConfigEntity getAppDetailsByName(String clusterName, int pipelineId){
        if (APP_DETAILS_MAP.containsKey(clusterName)){
            return APP_DETAILS_MAP.get(clusterName).get(pipelineId);
        }else{
            return null;
        }
    }
}
