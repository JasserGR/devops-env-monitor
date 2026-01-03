package com.devops.project.envmonitor;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@SpringBootApplication
@RestController
@RequestMapping("/api/v1/environment")
public class EnvMonitorApplication {

    private static final Logger log = LoggerFactory.getLogger(EnvMonitorApplication.class);

    private final Map<String, SensorReading> readings = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        SpringApplication.run(EnvMonitorApplication.class, args);
    }

    @PostMapping("/readings")
    public ResponseEntity<String> updateReading(@Valid @RequestBody SensorReading reading) {
        log.info("Updating sensor data for zone: {}", reading.getZoneId());

        if (reading.getTemperature() > 50 || reading.getTemperature() < -20) {
            log.warn("Extreme temperature detected in zone: {}", reading.getZoneId());
        }

        readings.put(reading.getZoneId(), reading);
        return ResponseEntity.ok("Data synchronized successfully at " + LocalDateTime.now());
    }

    @GetMapping("/readings")
    public Collection<SensorReading> getAllReadings() {
        log.info("Fetching all environment readings. Total zones: {}", readings.size());
        return readings.values();
    }

    @GetMapping("/readings/{zoneId}")
    public ResponseEntity<SensorReading> getReadingByZone(@PathVariable String zoneId) {
        SensorReading reading = readings.get(zoneId);
        if (reading == null) {
            log.error("Zone ID {} not found", zoneId);
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(reading);
    }

    @DeleteMapping("/readings/{zoneId}")
    public ResponseEntity<Void> resetZoneData(@PathVariable String zoneId) {
        if (!readings.containsKey(zoneId)) return ResponseEntity.notFound().build();
        readings.remove(zoneId);
        return ResponseEntity.noContent().build();
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class SensorReading {
    @NotBlank(message = "Zone ID is mandatory")
    private String zoneId;

    @Min(-50) @Max(60)
    private double temperature;

    @Min(0) @Max(100)
    private double humidity;

    private String status = "HEALTHY";
}