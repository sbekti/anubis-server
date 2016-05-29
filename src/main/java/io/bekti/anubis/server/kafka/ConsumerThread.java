package io.bekti.anubis.server.kafka;

import io.bekti.anubis.server.types.InboundMessage;
import io.bekti.anubis.server.types.SeekRequest;
import io.bekti.anubis.server.utils.SharedConfiguration;
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
                            consumer.seekToBeginning(getPartitionsForTopic(seekRequest.getTopic(), consumer));
                            break;
                        case "end":
                            consumer.seekToEnd(getPartitionsForTopic(seekRequest.getTopic(), consumer));
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

                ConsumerRecords<String, String> records = consumer.poll(100);

                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);

                    for (ConsumerRecord<String, String> record : partitionRecords) {
                        String topic = record.topic();
                        long offset = record.offset();
                        String key = record.key();
                        String value = record.value();

                        log.debug("Received from {} with offset {}: {} -> {}", topic, offset, key, value);

                        inboundQueue.put(new InboundMessage(topic, offset, key, value));
                        consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(offset + 1)));
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

    private List<TopicPartition> getPartitionsForTopic(String topic, KafkaConsumer<?, ?> consumer) {
        List<TopicPartition> partitions = consumer.assignment()
                .stream()
                .filter(partition -> partition.topic().equals(topic))
                .collect(Collectors.toList());

        return partitions;
    }

}
