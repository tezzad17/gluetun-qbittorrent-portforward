package com.tezzad.vpnscheduler.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Service
public class DockerService {

    private static final Logger log = LoggerFactory.getLogger(DockerService.class);

    public boolean isContainerRunning(String containerName) {
        // Equivalent to: docker inspect -f '{{.State.Running}}' containerName
        ProcessBuilder processBuilder = new ProcessBuilder(
            "docker", "inspect", "-f", "{{.State.Running}}", containerName
        );

        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            boolean finished = process.waitFor(5, TimeUnit.SECONDS);
            
            if (finished && output.equals("true")) {
                return true;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Failed to check Docker status for {}: {}", containerName, e.getMessage());
        }
        return false;
    }

}
