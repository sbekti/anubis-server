package io.bekti.anubis.server.util;

import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class TopicInitializer {

    private static final Logger log = LoggerFactory.getLogger(TopicInitializer.class);

    private static Set<String> topicListCache = new HashSet<>();

    static {
        updateTopicListCache();
    }

    public static void updateTopicListCache() {
        Properties props = new Properties();
        props.put("bootstrap.servers", SharedConfiguration.getString("bootstrap.servers"));
        props.put("enable.auto.commit", SharedConfiguration.getString("enable.auto.commit"));
        props.put("key.deserializer", SharedConfiguration.getString("key.deserializer"));
        props.put("value.deserializer", SharedConfiguration.getString("value.deserializer"));
        props.put("group.id", UUID.randomUUID().toString());

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        Map<String, List<PartitionInfo>> topics = consumer.listTopics();
        consumer.close();

        topicListCache.clear();
        topics.forEach((k, v) -> topicListCache.add(k));
    }

    public static void initializeTopic(String topic) {
        if (topicListCache.contains(topic)) {
            return;
        }

        String zkServers = SharedConfiguration.getString("zookeeper.connect");
        boolean isSecureKafkaCluster = SharedConfiguration.getBoolean("secure.cluster");
        Properties topicConfig = new Properties();

        if (!KafkaUtils.topicExists(topic, zkServers, isSecureKafkaCluster)) {
            int numPartitions = SharedConfiguration.getInteger("num.partitions");
            int numReplications = SharedConfiguration.getInteger("num.replications");

            KafkaUtils.createTopic(
                    topic,
                    numPartitions,
                    numReplications,
                    topicConfig,
                    zkServers,
                    isSecureKafkaCluster
            );

            updateTopicListCache();
        }
    }

}
