package io.bekti.anubis.server.kafka;

import io.bekti.anubis.server.types.OutboundMessage;
import io.bekti.anubis.server.utils.SharedConfiguration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.eclipse.jetty.websocket.api.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PublisherThread extends Thread {

    private static Logger log = LoggerFactory.getLogger(PublisherThread.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private BlockingQueue<OutboundMessage> outboundQueue;
    private Session session;

    private KafkaProducer<String, String> producer;

    public PublisherThread(BlockingQueue<OutboundMessage> outboundQueue, Session session) {
        this.outboundQueue = outboundQueue;
        this.session = session;
    }

    @Override
    public void run() {
        running.set(true);

        producer = getProducer();

        while (running.get()) {
            OutboundMessage outboundMessage;

            try {
                outboundMessage = outboundQueue.poll(100, TimeUnit.MILLISECONDS);

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
    }

    public boolean isRunning() {
        return running.get();
    }

    public void shutdown() {
        if (running.get()) {
            running.set(false);

            if (producer != null) {
                log.info("Closing Kafka producer...");
                producer.close();
            }
        }
    }

    private KafkaProducer<String, String> getProducer() {
        Properties props = new Properties();

        props.put("bootstrap.servers", SharedConfiguration.getString("bootstrap.servers"));
        props.put("acks", SharedConfiguration.getString("acks"));
        props.put("retries", SharedConfiguration.getString("retries"));
        props.put("batch.size", SharedConfiguration.getString("batch.size"));
        props.put("linger.ms", SharedConfiguration.getString("linger.ms"));
        props.put("buffer.memory", SharedConfiguration.getString("buffer.memory"));
        props.put("key.serializer", SharedConfiguration.getString("key.serializer"));
        props.put("value.serializer", SharedConfiguration.getString("value.serializer"));

        return new KafkaProducer<>(props);
    }

}
