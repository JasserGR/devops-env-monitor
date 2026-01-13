package com.devops.project.envmonitor;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Collection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

// This annotation starts the full application on a random free port for testing
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EnvMonitorApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate; // Ensure this name matches what you use below

    @Test
    void contextLoads() {
        // Basic check to ensure Spring starts
    }

    @Test
    void testCreateAndGetReading() {
        // 1. Test POST - Use 'restTemplate', not 'client'
        SensorReading reading = new SensorReading("Zone-Alpha", 25.0, 40.0);
        ResponseEntity<String> postResponse = restTemplate.postForEntity("/api/v1/environment/readings", reading, String.class);

        assertThat(postResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(postResponse.getBody()).isEqualTo("Data synchronized");

        // 2. Test GET All
        ResponseEntity<Collection> getResponse = restTemplate.getForEntity("/api/v1/environment/readings", Collection.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isNotEmpty();
    }

    @Test
    void testExtremeTemperatureLogic() {
        SensorReading extremeReading = new SensorReading("Sahara-01", 55.0, 10.0);
        ResponseEntity<String> response = restTemplate.postForEntity("/api/v1/environment/readings", extremeReading, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void testValidationFailure() {
        // Tests the @ExceptionHandler - Temperature 100 is invalid
        SensorReading invalidReading = new SensorReading("Error-Zone", 100.0, 40.0);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/environment/readings", invalidReading, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("Invalid sensor data provided");
    }
}