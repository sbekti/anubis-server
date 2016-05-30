package io.bekti.anubis.server.workers;

import io.bekti.anubis.server.types.CommitRequest;
import io.bekti.anubis.server.types.InboundMessage;
import io.bekti.anubis.server.types.SeekRequest;
import io.bekti.anubis.server.utils.SharedConfiguration;
import io.bekti.anubis.server.utils.TopicInitializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
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

    private BlockingQueue<InboundMessage> inboundQueue;
    private List<String> topics;
    private String groupId;

    private KafkaConsumer<String, String> consumer;
    private Queue<SeekRequest> seekRequestQueue = new LinkedList<>();
    private Queue<CommitRequest> commitRequestQueue = new LinkedList<>();

    public ConsumerThread(List<String> topics, String groupId, BlockingQueue<InboundMessage> inboundQueue) {
        this.inboundQueue = inboundQueue;
        this.topics = topics;
        this.groupId = groupId;
    }

    @Override
    public void run() {
        running.set(true);

        consumer = getConsumer(groupId);

        topics.forEach(TopicInitializer::initializeTopic);

        consumer.subscribe(topics);

        try {
            while (true) {
                while (!seekRequestQueue.isEmpty()) {
                    SeekRequest seekRequest = seekRequestQueue.remove();

                    switch (seekRequest.getOffset()) {
                        case "beginning":
                            consumer.seekToBeginning(getPartitionsForTopic(seekRequest.getTopic()));
                            break;
                        case "end":
                            consumer.seekToEnd(getPartitionsForTopic(seekRequest.getTopic()));
                            break;
                        default:
                            consumer.assignment()
                                    .stream()
                                    .filter(partition -> partition.topic().equals(seekRequest.getTopic()))
                                    .forEach(partition ->
                                            consumer.seek(partition, Long.parseLong(seekRequest.getOffset()))
                                    );
                            break;
                    }
                }

                while (!commitRequestQueue.isEmpty()) {
                    CommitRequest commitRequest = commitRequestQueue.remove();

                    String topic = commitRequest.getTopic();
                    int partitionId = commitRequest.getPartition();
                    long offset = commitRequest.getOffset();

                    TopicPartition topicPartition = consumer.assignment()
                            .stream()
                            .filter(partition -> partition.topic().equals(topic))
                            .filter(partition -> partition.partition() == partitionId)
                            .findFirst()
                            .get();

                    if (topicPartition == null) continue;

                    consumer.commitSync(Collections.singletonMap(topicPartition, new OffsetAndMetadata(offset + 1)));
                }

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

                        inboundQueue.put(new InboundMessage(topic, partitionId, offset, key, value));
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

    public void commit(String topic, int partitionId, long offset) {
        commitRequestQueue.add(new CommitRequest(topic, partitionId, offset));
    }

    public void seek(String topic, String offset) {
        seekRequestQueue.add(new SeekRequest(topic, offset));
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

}
