package io.github.hamzahamidhar.fraud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Where it all starts. Run this class and Spring Boot will:
 *
 *   1. scan this package (and everything inside it) for classes marked
 *      @Component, @Service, @RestController, @Configuration and so on,
 *   2. create them and wire them together,
 *   3. start a web server on http://localhost:8080.
 */
@SpringBootApplication
public class FraudDetectionApplication {

    public static void main(String[] args) {
        SpringApplication.run(FraudDetectionApplication.class, args);
    }
}
