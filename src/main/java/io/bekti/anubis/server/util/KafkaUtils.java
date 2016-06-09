package io.bekti.anubis.server.util;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.PartitionInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class KafkaUtils {

    private static final Logger log = LoggerFactory.getLogger(KafkaUtils.class);

    private static final int DEFAULT_ZK_SESSION_TIMEOUT_MS = 10 * 1000;
    private static final int DEFAULT_ZK_CONNECTION_TIMEOUT_MS = 8 * 1000;

    private static Set<String> topicListCache = new HashSet<>();

    static {
        updateTopicListCache();
    }

    public static boolean topicExists(String topic, String zkServers, boolean isSecureKafkaCluster) {

        boolean result = false;

        try {
            ZkClient zkClient = new ZkClient(
                    zkServers,
                    DEFAULT_ZK_SESSION_TIMEOUT_MS,
                    DEFAULT_ZK_CONNECTION_TIMEOUT_MS,
                    ZKStringSerializer$.MODULE$
            );

            ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zkServers), isSecureKafkaCluster);

            result = AdminUtils.topicExists(zkUtils, topic);

            zkClient.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return result;
    }

    public static void createTopic(
            String topic,
            int partitions,
            int replicationFactor,
            Properties topicConfig,
            String zkServers,
            boolean isSecureKafkaCluster) {

        try {
            ZkClient zkClient = new ZkClient(
                    zkServers,
                    DEFAULT_ZK_SESSION_TIMEOUT_MS,
                    DEFAULT_ZK_CONNECTION_TIMEOUT_MS,
                    ZKStringSerializer$.MODULE$
            );

            ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zkServers), isSecureKafkaCluster);

            AdminUtils.createTopic(zkUtils, topic, partitions, replicationFactor, topicConfig, RackAwareMode.Disabled$.MODULE$);

            zkClient.close();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void updateTopicListCache() {
        Properties props = new Properties();
        props.put("bootstrap.servers", ConfigUtils.getString("bootstrap.servers"));
        props.put("enable.auto.commit", ConfigUtils.getString("enable.auto.commit"));
        props.put("key.deserializer", ConfigUtils.getString("key.deserializer"));
        props.put("value.deserializer", ConfigUtils.getString("value.deserializer"));
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

        String zkServers = ConfigUtils.getString("zookeeper.connect");
        boolean isSecureKafkaCluster = ConfigUtils.getBoolean("secure.cluster");
        Properties topicConfig = new Properties();

        if (!KafkaUtils.topicExists(topic, zkServers, isSecureKafkaCluster)) {
            int numPartitions = ConfigUtils.getInteger("num.partitions");
            int numReplications = ConfigUtils.getInteger("num.replications");

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
