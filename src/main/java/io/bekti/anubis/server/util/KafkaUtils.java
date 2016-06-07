package io.bekti.anubis.server.util;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class KafkaUtils {

    private static final Logger log = LoggerFactory.getLogger(KafkaUtils.class);

    private static final int DEFAULT_ZK_SESSION_TIMEOUT_MS = 10 * 1000;
    private static final int DEFAULT_ZK_CONNECTION_TIMEOUT_MS = 8 * 1000;

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

}
