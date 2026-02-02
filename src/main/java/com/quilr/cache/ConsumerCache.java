package com.quilr.cache;

import com.quilr.config.QuilrConfigs;
import com.quilr.kstreams.SuperConsumer;
import lombok.extern.log4j.Log4j2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Cache for SEC service
 *
 * @author visheshchaitanya
 */
@Log4j2
public enum ConsumerCache {

    INSTANCE;

    /**
     * Map holding consumers, per pod
     */
    private static final Map<String, SuperConsumer> consumers = new ConcurrentHashMap<>();

    private QuilrConfigs quilrConfigs;
    public void setConfig(QuilrConfigs quilrConfigs){
        this.quilrConfigs = quilrConfigs;
    }
    /**
     * Adds consumer to the cache
     *
     * @param pod      Kafka pod
     * @param consumer Consumer instance
     */
    public void addConsumer(String pod, SuperConsumer consumer) {

        log.debug("[AN:{}] Adding consumer to the cache for pod {} ..",quilrConfigs.getAppName(), pod);
        consumers.put(pod, consumer);
    }

    /**
     * Removes consumer from the cache for the given pod
     *
     * @param pod Kafka pod
     */
    public void removeConsumer(String pod) {

        log.warn("[AN:{}] Removing consumer for pod {} ..",quilrConfigs.getAppName(), pod);
        consumers.remove(pod);
    }

    public SuperConsumer getConsumer(String pod) {

        log.debug("[AN:{}] Getting consumer for pod {} ..",quilrConfigs.getAppName(), pod);
        return consumers.get(pod);
    }

    /**
     * Returns all the consumers present in the cache
     *
     * @return A map holding consumers against PODs
     */
    public Map<String, SuperConsumer> getConsumers() {

        log.debug("[AN:{}] Getting all consumers from the cache [# {}] ..",quilrConfigs.getAppName(), consumers.size());
        return consumers;
    }
}
