package com.nihongolisten;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class NihongoListenApplication {

    public static void main(String[] args) {
        SpringApplication.run(NihongoListenApplication.class, args);
    }
}
