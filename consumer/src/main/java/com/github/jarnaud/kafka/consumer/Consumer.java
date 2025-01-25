package com.github.jarnaud.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Slf4j
public class Consumer {

    private static final String TOPIC_PREFIX = "prices-";

    private final String kafkaBootstrap;
    private final String topicName;

    /**
     * @param kafkaBootstrap the list of comma-separated addresses of Kafka bootstrap servers (from brokers list).
     * @param ticker         the product ticker.
     */
    public Consumer(String kafkaBootstrap, String ticker) {

        if (ticker == null || ticker.isBlank()) {
            throw new RuntimeException("No ticker specified for price consumer");
        }

        this.kafkaBootstrap = kafkaBootstrap;
        this.topicName = TOPIC_PREFIX + ticker.trim().toUpperCase();

        createTopic();
    }

    private void createTopic() {
        AdminClient admin = AdminClient.create(loadProperties());
        try (admin) {
            // Create topic.
            NewTopic topic = new NewTopic(topicName, 1, (short) 1);
            CreateTopicsResult result = admin.createTopics(List.of(topic));
            log.debug("Topic creation results:\n{}", result.values());
            try {
                KafkaFuture<Void> f = result.all();
                Void r = f.get(500, TimeUnit.MILLISECONDS);
            } catch (ExecutionException e) {
                if (e.getCause() instanceof TopicExistsException) {
                    log.warn("Topic {} already exists.", topicName);
                } else {
                    log.error(e.getMessage(), e);
                }
            } catch (InterruptedException | TimeoutException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void describeCluster(AdminClient admin) {
        log.debug("--- Describing cluster ---");
        try {
            Collection<Node> nodes = admin.describeCluster().nodes().get();
            if (nodes == null) {
                log.info("There seem to be no nodes in the cluster!");
            } else {
                log.info(String.format("Count of nodes: %s\n", nodes.size()));
                for (Node node : nodes) {
                    log.info("\tnode {}: {}", node.id(), node);
                }
            }
        } catch (Exception e) {
            log.error("Error while describing cluster");
            log.error(e.getMessage(), e);
        }
    }

    public void receiveMessages() {
        log.info("Market consumer starting");
        Properties properties = loadProperties();
        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(properties);
        try (consumer) {
            consumer.subscribe(List.of(topicName));
            int iter = 120;
            while (iter-- > 0) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                for (ConsumerRecord<String, String> record : records) {
                    log.info("[offset {}] Received record {}: {}", record.offset(), record.key(), record.value());
                }
            }
        }
        log.info("Market consumer terminating");
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap); // Kafka brokers addresses.
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "market-consumer-group");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); //choose from earliest/latest/none
        return props;
    }
}
