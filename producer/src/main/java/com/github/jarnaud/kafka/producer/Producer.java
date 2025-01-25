package com.github.jarnaud.kafka.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.serialization.StringSerializer;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Random;

@Slf4j
public class Producer {

    private static final String TOPIC_PREFIX = "prices-";

    private final String kafkaBootstrap;
    private final String ticker;
    private final String topicName;

    /**
     * @param kafkaBootstrap the list of comma-separated addresses of Kafka bootstrap servers (from brokers list).
     * @param ticker         the product ticker.
     */
    public Producer(String kafkaBootstrap, String ticker) {

        if (ticker == null || ticker.isBlank()) {
            throw new RuntimeException("No ticker specified for price producer");
        }

        this.kafkaBootstrap = kafkaBootstrap;
        this.ticker = ticker.trim().toUpperCase();
        this.topicName = TOPIC_PREFIX + this.ticker;

        createTopic();
    }

    private void createTopic() {
        AdminClient admin = AdminClient.create(loadProperties());
        try (admin) {
            // Describe cluster.
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


            // Create topic.
            NewTopic topic = new NewTopic(topicName, 1, (short) 1);
            CreateTopicsResult result = admin.createTopics(List.of(topic));
            log.debug("Topic creation results:\n{}", result.values());
            try {
                Void r = result.all().get();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
        log.debug("End of topic creation");
    }

    public void sendMessages(int nbMessages) {
        Properties properties = loadProperties();
        KafkaProducer<String, String> producer = new KafkaProducer<>(properties);
        Random random = new Random();
        try (producer) {
            for (int i = 0; i < nbMessages; i++) {
                sendMessage(producer, random);
                Thread.sleep(1000);
            }
        } catch (JsonProcessingException | InterruptedException e) {
            log.error(e.getMessage(), e);
        }
    }

    private void sendMessage(KafkaProducer<String, String> producer, Random random) throws JsonProcessingException {
        // Random price generation.
        double price = 100 + random.nextDouble(100);
        double volume = 1000 + random.nextInt(10000);

        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("ticker", ticker);
        node.put("last", formatPrice(price));
        node.put("volume", formatPrice(volume));
        node.put("date", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
        String msg = mapper.writer().writeValueAsString(node);


        ProducerRecord<String, String> record = new ProducerRecord<>(topicName, msg);
        log.debug("Created record: {}", record);

        producer.send(record);
        producer.flush();
    }

    /**
     * Format a price with 2 digits for rendering.
     * <p>
     * See https://docs.oracle.com/javase/tutorial/java/data/numberformat.html
     *
     * @param price the price to format.
     * @return the formatted price.
     */
    private String formatPrice(double price) {
        return String.format("%.2f", price);
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrap); // Kafka brokers addresses.
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1000);
        props.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 5000);
        return props;
    }
}
