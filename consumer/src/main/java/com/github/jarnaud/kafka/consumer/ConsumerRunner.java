package com.github.jarnaud.kafka.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ConsumerRunner implements CommandLineRunner {

    @Value("${KAFKA_BOOTSTRAP:localhost:9090}")
    private String kafkaBootstrap;

    @Value("${TICKER}")
    private String ticker;

    @Override
    public void run(String... args) throws Exception {
        Consumer consumer = new Consumer(kafkaBootstrap, ticker);
        consumer.receiveMessages();
    }
}
