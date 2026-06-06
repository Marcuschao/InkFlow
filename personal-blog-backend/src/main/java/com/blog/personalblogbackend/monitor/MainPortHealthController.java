package com.blog.personalblogbackend.monitor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MainPortHealthController {

    @GetMapping({"/actuator/health", "/health"})
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
