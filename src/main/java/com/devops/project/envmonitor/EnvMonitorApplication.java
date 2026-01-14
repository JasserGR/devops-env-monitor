package com.devops.project.envmonitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
@RequestMapping("/api/v1/environment")
public class EnvMonitorApplication {

    private static final Logger log = LoggerFactory.getLogger(EnvMonitorApplication.class);

    // Package-private so the Health Indicator can see it
    final Map<String, SensorReading> readings = new ConcurrentHashMap<>();
    private final Counter extremeTempCounter;

    public EnvMonitorApplication(MeterRegistry registry) {
        // Custom Metric for Prometheus
        this.extremeTempCounter = Counter.builder("env.sensor.extreme.alerts")
                .description("Counts extreme temperature detections")
                .register(registry);
    }

    public static void main(String[] args) {
        SpringApplication.run(EnvMonitorApplication.class, args);
    }

    @PostMapping("/readings")
    public ResponseEntity<String> updateReading(@Valid @RequestBody SensorReading reading) {
        log.info("Update received for zone: {}", reading.getZoneId());

        if (reading.getTemperature() > 40 || reading.getTemperature() < -10) {
            extremeTempCounter.increment();
            log.warn("EXTREME_TEMP_ALERT in zone: {}", reading.getZoneId());
        }

        readings.put(reading.getZoneId(), reading);
        return ResponseEntity.ok("Data synchronized");
    }

    @GetMapping("/readings")
    public Collection<SensorReading> getAll() {
        log.info("Fetching all environment readings");
        return readings.values();
    }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid sensor data provided", "status", "400"));
    }
}

/**
 * Custom Health Indicator for Kubernetes Readiness/Liveness Probes
 */
@Component
class SensorHealthIndicator implements HealthIndicator {
    private final EnvMonitorApplication app;

    public SensorHealthIndicator(EnvMonitorApplication app) {
        this.app = app;
    }

    @Override
    public Health health() {
        if (app.readings == null) {
            return Health.down().withDetail("reason", "Storage failure").build();
        }
        return Health.up()
                .withDetail("active_zones", app.readings.size())
                .withDetail("status", "System is healthy")
                .build();
    }
}

/**
 * Data Model with Validation Constraints
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
class SensorReading {
    @NotBlank(message = "Zone ID required")
    private String zoneId;

    @Min(-50) @Max(60)
    private double temperature;

    @Min(0) @Max(100)
    private double humidity;
}