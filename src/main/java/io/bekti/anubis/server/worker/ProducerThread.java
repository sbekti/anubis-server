package io.bekti.anubis.server.worker;

import io.bekti.anubis.server.model.message.ProducerMessage;
import io.bekti.anubis.server.util.ConfigUtils;
import io.bekti.anubis.server.util.KafkaUtils;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class ProducerThread extends Thread {

    private static final Logger log = LoggerFactory.getLogger(ProducerThread.class);
    private AtomicBoolean running = new AtomicBoolean(false);

    private BlockingQueue<ProducerMessage> producerQueue;
    private KafkaProducer<String, String> producer;

    public ProducerThread(BlockingQueue<ProducerMessage> producerQueue) {
        this.producerQueue = producerQueue;
    }

    @Override
    public void run() {
        running.set(true);

        producer = getProducer();

        while (running.get()) {
            try {
                ProducerMessage producerMessage = producerQueue.poll(100, TimeUnit.MILLISECONDS);

                if (producerMessage == null) continue;

                String topic = producerMessage.getTopic();
                String key = producerMessage.getKey();
                String value = producerMessage.getValue();

                KafkaUtils.initializeTopic(topic);

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

        props.put("bootstrap.servers", ConfigUtils.getString("bootstrap.servers"));
        props.put("acks", ConfigUtils.getString("acks"));
        props.put("retries", ConfigUtils.getString("retries"));
        props.put("batch.size", ConfigUtils.getString("batch.size"));
        props.put("linger.ms", ConfigUtils.getString("linger.ms"));
        props.put("buffer.memory", ConfigUtils.getString("buffer.memory"));
        props.put("key.serializer", ConfigUtils.getString("key.serializer"));
        props.put("value.serializer", ConfigUtils.getString("value.serializer"));

        return new KafkaProducer<>(props);
    }

}
