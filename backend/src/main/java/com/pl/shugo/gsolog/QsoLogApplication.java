package com.pl.shugo.gsolog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for QSO Log Backend.
 *
 * Ham Radio QSO Logging Application - Reactive backend with WebFlux.
 */
@SpringBootApplication
public class QsoLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(QsoLogApplication.class, args);
    }
}

