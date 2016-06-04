package io.bekti.anubis.server.workers;

import io.bekti.anubis.server.messages.*;
import io.bekti.anubis.server.models.KafkaPartition;
import io.bekti.anubis.server.utils.SharedConfiguration;
import io.bekti.anubis.server.utils.TopicInitializer;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class ConsumerThread extends Thread {

    private static Logger log = LoggerFactory.getLogger(ConsumerThread.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private BlockingQueue<BaseMessage> consumerQueue;
    private List<String> topics;
    private String groupId;

    private KafkaConsumer<String, String> consumer;
    private Queue<SeekMessage> seekRequestQueue = new LinkedList<>();
    private Queue<CommitMessage> commitRequestQueue = new LinkedList<>();

    public ConsumerThread(List<String> topics, String groupId, BlockingQueue<BaseMessage> consumerQueue) {
        this.consumerQueue = consumerQueue;
        this.topics = topics;
        this.groupId = groupId;
    }

    @Override
    public void run() {
        running.set(true);

        topics.forEach(TopicInitializer::initializeTopic);

        consumer = getConsumer(groupId);

        consumer.subscribe(topics, new ConsumerRebalanceListener() {

            @Override
            public void onPartitionsRevoked(Collection<TopicPartition> partitions) {
                List<KafkaPartition> revokedPartitions = new LinkedList<>();

                for (TopicPartition partition : partitions) {
                    revokedPartitions.add(new KafkaPartition(partition.topic(), partition.partition()));
                }

                RevokeMessage revokeMessage = new RevokeMessage();
                revokeMessage.setPartitions(revokedPartitions);

                consumerQueue.add(revokeMessage);
            }

            @Override
            public void onPartitionsAssigned(Collection<TopicPartition> partitions) {
                List<KafkaPartition> assignedPartitions = new LinkedList<>();

                for (TopicPartition partition : partitions) {
                    assignedPartitions.add(new KafkaPartition(partition.topic(), partition.partition()));
                }

                AssignMessage assignMessage = new AssignMessage();
                assignMessage.setPartitions(assignedPartitions);

                consumerQueue.add(assignMessage);
            }

        });

        try {
            while (true) {
                processSeekRequests();

                processCommitRequests();

                ConsumerRecords<String, String> records = consumer.poll(100);

                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);

                    for (ConsumerRecord<String, String> record : partitionRecords) {
                        String topic = record.topic();
                        long offset = record.offset();
                        String key = record.key();
                        String value = record.value();
                        int partitionId = partition.partition();

                        log.debug("Received from {}-{} offset {}: {} -> {}", topic, partitionId, offset, key, value);

                        ConsumerMessage consumerMessage = new ConsumerMessage();
                        consumerMessage.setTopic(topic);
                        consumerMessage.setPartition(partitionId);
                        consumerMessage.setOffset(offset);
                        consumerMessage.setKey(key);
                        consumerMessage.setValue(value);

                        consumerQueue.put(consumerMessage);
                    }
                }
            }
        } catch (WakeupException ignored) {

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            if (consumer != null) {
                consumer.close();
            }
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public void shutdown() {
        if (running.get()) {
            running.set(false);

            if (consumer != null) {
                log.info("Closing Kafka consumer...");
                consumer.wakeup();
            }
        }
    }

    public void commit(CommitMessage commitMessage) {
        commitRequestQueue.add(commitMessage);
    }

    public void seek(SeekMessage seekMessage) {
        seekRequestQueue.add(seekMessage);
    }

    private KafkaConsumer<String, String> getConsumer(String groupId) {
        Properties props = new Properties();

        props.put("bootstrap.servers", SharedConfiguration.getString("bootstrap.servers"));
        props.put("enable.auto.commit", SharedConfiguration.getString("enable.auto.commit"));
        props.put("key.deserializer", SharedConfiguration.getString("key.deserializer"));
        props.put("value.deserializer", SharedConfiguration.getString("value.deserializer"));
        props.put("group.id", groupId);

        return new KafkaConsumer<>(props);
    }

    private List<TopicPartition> getPartitionsForTopic(String topic) {
        List<TopicPartition> partitions = consumer.assignment()
                .stream()
                .filter(partition -> partition.topic().equals(topic))
                .collect(Collectors.toList());

        return partitions;
    }

    private void processSeekRequests() {
        while (!seekRequestQueue.isEmpty()) {
            SeekMessage seekMessage = seekRequestQueue.remove();

            switch (seekMessage.getOffset()) {
                case "beginning":
                    consumer.seekToBeginning(getPartitionsForTopic(seekMessage.getTopic()));
                    break;
                case "end":
                    consumer.seekToEnd(getPartitionsForTopic(seekMessage.getTopic()));
                    break;
                default:
                    consumer.assignment()
                            .stream()
                            .filter(partition -> partition.topic().equals(seekMessage.getTopic()))
                            .forEach(partition ->
                                    consumer.seek(partition, Long.parseLong(seekMessage.getOffset()))
                            );
                    break;
            }
        }
    }

    private void processCommitRequests() {
        while (!commitRequestQueue.isEmpty()) {
            CommitMessage commitMessage = commitRequestQueue.remove();

            String topic = commitMessage.getTopic();
            int partitionId = commitMessage.getPartition();
            long offset = commitMessage.getOffset();

            TopicPartition topicPartition = consumer.assignment()
                    .stream()
                    .filter(partition -> partition.topic().equals(topic))
                    .filter(partition -> partition.partition() == partitionId)
                    .findFirst()
                    .get();

            if (topicPartition == null) continue;

            consumer.commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(offset + 1)));
        }
    }

}
