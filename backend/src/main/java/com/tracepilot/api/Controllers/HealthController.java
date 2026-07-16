package com.tracepilot.api.Controllers;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final ConnectionFactory rabbitConnectionFactory;
    private final RestTemplate restTemplate;

    @Value("${tracepilot.worker.health-url}")
    private String workerHealthUrl; // e.g. http://ai-worker:8001/ai-worker/v1/health

    @Value("${tracepilot.jaeger.health-url}")
    private String jaegerHealthUrl; // e.g. http://jaeger:14269/ (Jaeger's admin health port)

    public HealthController(JdbcTemplate jdbcTemplate, ConnectionFactory rabbitConnectionFactory) {
        this.jdbcTemplate = jdbcTemplate;
        this.rabbitConnectionFactory = rabbitConnectionFactory;
        this.restTemplate = new RestTemplateBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .readTimeout(Duration.ofSeconds(2))
                .build();
    }

    @GetMapping("/api/v1/health")
    public ResponseEntity<Map<String, Object>> health() {
        CompletableFuture<Boolean> dbCheck = CompletableFuture.supplyAsync(this::checkDatabase);
        CompletableFuture<Boolean> workerCheck = CompletableFuture.supplyAsync(this::checkWorker);
        CompletableFuture<Boolean> rabbitCheck = CompletableFuture.supplyAsync(this::checkRabbit);
        CompletableFuture<Boolean> tracingCheck = CompletableFuture.supplyAsync(this::checkTracing);

        CompletableFuture.allOf(dbCheck, workerCheck, rabbitCheck, tracingCheck).join();

        boolean db = dbCheck.join();
        boolean worker = workerCheck.join();
        boolean rabbit = rabbitCheck.join();
        boolean tracing = tracingCheck.join();
        boolean allUp = db && worker && rabbit && tracing;

        Map<String, Object> components = new LinkedHashMap<>();
        components.put("db", db ? "UP" : "DOWN");
        components.put("worker", worker ? "UP" : "DOWN");
        components.put("rabbitmq", rabbit ? "UP" : "DOWN");
        components.put("tracing", tracing ? "UP" : "DOWN");

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", allUp ? "UP" : "DOWN");
        body.put("components", components);
        log.info(
                "Health check: status={}, db={}, worker={}, rabbitmq={}, tracing={}",
                allUp ? "UP" : "DOWN",
                db ? "UP" : "DOWN",
                worker ? "UP" : "DOWN",
                rabbit ? "UP" : "DOWN",
                tracing ? "UP" : "DOWN");
        return ResponseEntity.status(allUp ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    private boolean checkDatabase() {
        try {
            Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return result != null && result == 1;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkWorker() {
        try {
            restTemplate.getForEntity(workerHealthUrl, String.class);
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }

    private boolean checkRabbit() {
        try (var connection = rabbitConnectionFactory.createConnection()) {
            return connection.isOpen();
        } catch (Exception e) {
            return false;
        }
    }

    private boolean checkTracing() {
        try {
            restTemplate.getForEntity(jaegerHealthUrl, String.class);
            return true;
        } catch (RestClientException e) {
            return false;
        }
    }
}