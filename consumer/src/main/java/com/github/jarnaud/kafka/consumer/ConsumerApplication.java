package com.github.jarnaud.kafka.consumer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class ConsumerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConsumerApplication.class, args);
    }

    @RequestMapping("/")
    public String home() {
        return "Welcome to Kafka market";
    }

//    @RequestMapping("/orders")
//    public String orders() {
//        Market market = new Market();
//        return "Market closed";
//    }
}
