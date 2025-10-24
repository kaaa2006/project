package org.team.mealkitshop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan(basePackages = "org.team.mealkitshop.config")

@SpringBootApplication
public class MealkitshopApplication {

    public static void main(String[] args) {
        SpringApplication.run(MealkitshopApplication.class, args);
    }

}