package io.bekti.anubis.server.kafka;

import io.bekti.anubis.server.types.InboundMessage;
import io.bekti.anubis.server.types.OutboundMessage;
import io.bekti.anubis.server.utils.SharedConfiguration;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

public class KafkaWebSocketClient extends Thread {

    private static Logger log = LoggerFactory.getLogger(KafkaWebSocketClient.class);

    private Properties consumerProps;
    private Properties producerProps;
    private KafkaConsumer<String, String> consumer;
    private KafkaProducer<String, String> producer;
    private BlockingQueue<InboundMessage> consumerQueue;
    private BlockingQueue<OutboundMessage> producerQueue;

    private Session session;
    private List<String> topics;

    private AtomicBoolean seekRequired = new AtomicBoolean(false);
    private AtomicReference<String> seekTopic = new AtomicReference<>();
    private AtomicReference<String> seekOffset = new AtomicReference<>();

    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private AtomicBoolean running = new AtomicBoolean(false);


    public KafkaWebSocketClient(String groupId, List<String> topics, Session session) {
        consumerProps = new Properties();
        consumerProps.put("bootstrap.servers", SharedConfiguration.getString("bootstrap.servers"));
        consumerProps.put("enable.auto.commit", SharedConfiguration.getString("enable.auto.commit"));
        consumerProps.put("key.deserializer", SharedConfiguration.getString("key.deserializer"));
        consumerProps.put("value.deserializer", SharedConfiguration.getString("value.deserializer"));
        consumerProps.put("group.id", groupId);

        producerProps = new Properties();
        producerProps.put("bootstrap.servers", SharedConfiguration.getString("bootstrap.servers"));
        producerProps.put("acks", SharedConfiguration.getString("acks"));
        producerProps.put("retries", SharedConfiguration.getString("retries"));
        producerProps.put("batch.size", SharedConfiguration.getString("batch.size"));
        producerProps.put("linger.ms", SharedConfiguration.getString("linger.ms"));
        producerProps.put("buffer.memory", SharedConfiguration.getString("buffer.memory"));
        producerProps.put("key.serializer", SharedConfiguration.getString("key.serializer"));
        producerProps.put("value.serializer", SharedConfiguration.getString("value.serializer"));

        this.topics = topics;
        this.session = session;

        consumerQueue = new LinkedBlockingQueue<>();
        producerQueue = new LinkedBlockingQueue<>();
    }

    @Override
    public void run() {
        log.info("Starting thread...");
        running.set(true);

        consumer = new KafkaConsumer<>(consumerProps);
        producer = new KafkaProducer<>(producerProps);

        executorService.submit(() -> {
            while (this.running.get()) {
                InboundMessage inboundMessage = null;

                try {
                    inboundMessage = consumerQueue.poll(1000, TimeUnit.MILLISECONDS);

                    if (inboundMessage == null) continue;

                    if (session.isOpen()) {
                        JSONObject jsonResponse = new JSONObject();
                        jsonResponse.put("topic", inboundMessage.getTopic());
                        jsonResponse.put("offset", inboundMessage.getOffset());
                        jsonResponse.put("key", inboundMessage.getKey());
                        jsonResponse.put("value", inboundMessage.getValue());

                        session.getRemote().sendString(jsonResponse.toString());
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        executorService.submit(() -> {
            while (this.running.get()) {
                OutboundMessage outboundMessage = null;

                try {
                    outboundMessage = producerQueue.poll(1000, TimeUnit.MILLISECONDS);

                    if (outboundMessage == null) continue;

                    String topic = outboundMessage.getTopic();
                    String value = outboundMessage.getValue();
                    String key = outboundMessage.getKey();

                    TopicInitializer.initializeTopic(topic);

                    producer.send(new ProducerRecord<>(topic, key, value), (metadata, exception) -> {
                        if (exception != null) {
                            log.error(exception.getMessage(), exception);
                        } else {
                            log.debug("Sent record to {} with offset {}", metadata.topic(), metadata.offset());
                        }
                    });
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                }
            }
        });

        log.debug("Entering main client loop...");

        try {
            topics.forEach(TopicInitializer::initializeTopic);

            consumer.subscribe(topics);

            while (true) {
                if (seekRequired.get()) {
                    switch (seekOffset.get()) {
                        case "beginning":
                            consumer.seekToBeginning(getPartitionsForTopic(seekTopic.get(), consumer));
                            break;
                        case "end":
                            consumer.seekToEnd(getPartitionsForTopic(seekTopic.get(), consumer));
                            break;
                        default:
                            consumer.assignment()
                                    .stream()
                                    .filter(partition -> partition.topic().equals(seekTopic.get()))
                                    .forEach(partition ->
                                        consumer.seek(partition, Long.parseLong(seekOffset.get()))
                                    );
                            break;
                    }

                    seekRequired.set(false);
                }

                ConsumerRecords<String, String> records = consumer.poll(1000);

                for (TopicPartition partition : records.partitions()) {
                    List<ConsumerRecord<String, String>> partitionRecords = records.records(partition);

                    for (ConsumerRecord<String, String> record : partitionRecords) {
                        String topic = record.topic();
                        long offset = record.offset();
                        String key = record.key();
                        String value = record.value();

                        log.debug("Received from {} with offset {}: {} -> {}", topic, offset, key, value);

                        consumerQueue.put(new InboundMessage(topic, offset, key, value));
                        consumer.commitSync(Collections.singletonMap(partition, new OffsetAndMetadata(offset + 1)));
                    }
                }
            }
        } catch (WakeupException ignored) {

        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            consumer.close();
        }
    }

    public boolean isRunning() {
        return this.running.get();
    }

    public void shutdown() {
        if (this.running.get() && this.consumer != null) {
            this.running.set(false);

            log.info("Closing Kafka consumer...");
            consumer.wakeup();

            log.info("Closing Kafka producer...");
            producer.close();

            log.info("Shutting down worker...");
            executorService.shutdown();

            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();

                    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.error("Failed to shutdown worker.");
                    }
                }
            } catch (InterruptedException ie) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    public void enqueueOutboundMessage(OutboundMessage outboundMessage) {
        try {
            producerQueue.put(outboundMessage);
        } catch (InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void requestSeek(String topic, String offset) {
        seekTopic.set(topic);
        seekOffset.set(offset);
        seekRequired.set(true);
    }

    private List<TopicPartition> getPartitionsForTopic(String topic, KafkaConsumer<?, ?> consumer) {
        List<TopicPartition> partitions = consumer.assignment()
                .stream()
                .filter(partition -> partition.topic().equals(topic))
                .collect(Collectors.toList());

        return partitions;
    }

}
