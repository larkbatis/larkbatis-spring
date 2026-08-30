package com.example.lbspring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Nothing to configure. There is no {@code @MapperScan}, no
 * {@code SqlSessionFactoryBean}, no {@code SqlSessionTemplate} — the default
 * {@code @ComponentScan} of {@code @SpringBootApplication} finds the
 * generated {@code LightBatisMapperConfiguration} in this very package, and
 * the Boot auto-configuration supplies the one {@code LightBatisSession} it
 * asks for.
 */
@SpringBootApplication
public class SpringSampleApp {

    public static void main(String[] args) {
        SpringApplication.run(SpringSampleApp.class, args);
    }
}
