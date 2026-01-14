package com.devops.project.envmonitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
@RequestMapping("/api/v1/environment")
public class EnvMonitorApplication {

    private static final Logger log = LoggerFactory.getLogger(EnvMonitorApplication.class);
    private final Map<String, SensorReading> readings = new ConcurrentHashMap<>();
    private final Counter extremeTempCounter;

    public EnvMonitorApplication(MeterRegistry registry) {
        // Custom Metric: Counts how many times extreme weather is reported
        this.extremeTempCounter = Counter.builder("env.sensor.extreme.alerts")
                .description("Counts extreme temperature detections")
                .register(registry);
    }

    public static void main(String[] args) {
        SpringApplication.run(EnvMonitorApplication.class, args);
    }

    @PostMapping("/readings")
    public ResponseEntity<String> updateReading(@Valid @RequestBody SensorReading reading) {
        log.info("Update received for zone: {}", reading.getZoneId()); // This will now be JSON

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
        return readings.values(); }

    @ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(Exception e) {
        return ResponseEntity.badRequest().body(Map.of("error", "Invalid sensor data provided", "status", "400"));
    }
}

@Data @NoArgsConstructor @AllArgsConstructor
class SensorReading {
    @NotBlank String zoneId;
    @Min(-50) @Max(60) double temperature;
    @Min(0) @Max(100) double humidity;
}