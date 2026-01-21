package com.tezzad.vpnscheduler.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class SyncLogicService {

    private static final Logger log = LoggerFactory.getLogger(SyncLogicService.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Autowired
    private  SlackMessageService slackMessageService;

    @Value("${app.gluetun.container-name}")
    private String gluetunName;
    @Value("${app.gluetun.base-url}")
    private String gluetunUrl;
    @Value("${app.qbittorrent.container-name}")
    private String qbName;
    @Value("${app.qbittorrent.base-url}")
    private String qbUrl;


    public SyncLogicService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.create();
    }

    @PostConstruct
    private void logContainerInfo() {
        log.info("Gluetun Container: {}, Base URL: {}", gluetunName, gluetunUrl);
        log.info("qBittorrent Container: {}, Base URL: {}", qbName, qbUrl);

        slackMessageService.sendSlackNotification("VPN Scheduler started for Gluetun: " + gluetunName + " and qBittorrent: " + qbName);
        slackMessageService.sendSlackNotification("Gluetun Base URL: " + gluetunUrl + " | qBittorrent Base URL: " + qbUrl);
    }

    /**
     * This method runs automatically based on the frequency defined in
     * application.properties.
     * 'fixedDelayString' means the timer starts counting ONLY after the previous
     * execution finishes.
     */
    @Scheduled(fixedDelayString = "${app.sync.frequency-ms}")
    public void performSync() {
        log.info("Starting scheduled sync check...");

        // Check External IP (VPN Status)
        String externalIp = getGluetunPublicIp();
        if (externalIp == null || externalIp.isBlank()) {
            log.error("External IP is empty. VPN might be down.");
            return;
        }
        log.info("External IP is {} (VPN UP)", externalIp);
        //slackMessageService.sendSlackNotification("External IP is " + externalIp + " (VPN UP)");

        // Get Ports
        Integer gluetunPort = getGluetunPort();
        Integer currentQbPort = getQbitPort();

        log.info("Gluetun Port: {}, qBit Port: {}", gluetunPort, currentQbPort);

        // Validate Ports
        if (gluetunPort == null || currentQbPort == null) {
            log.error("Could not retrieve ports. Aborting sync.");
            slackMessageService.sendSlackNotification("Could not retrieve ports. Aborting sync.");
            return;
        }

        // Compare and Update
        if (gluetunPort.equals(currentQbPort)) {
            log.info("Ports match ({}). No action needed.", currentQbPort);
        } else {
            log.info("Port Mismatch! Updating qBittorrent to Gluetun port {}", gluetunPort);
            slackMessageService.sendSlackNotification("Port Mismatch! Updating qBittorrent to Gluetun port " + gluetunPort);
            updateQbitPort(gluetunPort);

            Integer newPort = getQbitPort();
            log.info("Update complete. New qBittorrent listen port: {}", newPort);
        }

        log.info("Sync check finished.");
    }


    private String getGluetunPublicIp() {
        try {
            String response = restClient.get()
                    .uri(gluetunUrl + "/v1/publicip/ip")
                    .retrieve()
                    .body(String.class);

            if (response == null || response.isBlank()) {
                log.info("There was no response from call");
                return null;
            }

            response = response.trim();

            log.debug("Service response {}", response);

            // Some Gluetun endpoints return plain text (the IP) with content-type text/plain.
            // If it's JSON, parse it and extract the expected field; otherwise return the text.
            if (response.startsWith("{")) {
                JsonNode node = objectMapper.readTree(response);
                log.debug("node parse {}", node.toPrettyString());
                if (node.hasNonNull("public_ip")) {
                    log.debug("External IP found: {}", node.get("public_ip").stringValue());
                    return node.get("public_ip").stringValue();
                }
                if (node.hasNonNull("ip")) {
                    log.debug("External IP found: {}", node.get("ip").stringValue());
                    return node.get("ip").stringValue();
                }
                return null;
            }

            return response;
        } catch (Exception e) {
            log.error("Error fetching public IP: {}", e.getMessage());
            slackMessageService.sendSlackNotification("Error fetching public IP: " + e.getMessage());
            return null;
        }
    }

    private Integer getGluetunPort() {
        try {
            String response = restClient.get()
                    .uri(gluetunUrl + "/v1/portforward")
                    .retrieve()
                    .body(String.class);

            log.debug("Gluetun response portforwared: {}", response);

            JsonNode node = objectMapper.readTree(response);

            log.debug("Gluetun response portforwared: {}", node.toPrettyString());

            if (node.hasNonNull("port")) {
                return node.get("port").asInt();
            } else if (node.hasNonNull("listen_port")) {
                return node.get("listen_port").asInt();
            }
        } catch (Exception e) {
            log.error("Error fetching Gluetun port: {}", e.getMessage());
            slackMessageService.sendSlackNotification("Error fetching Gluetun port: " + e.getMessage());
        }
        return null;
    }

    private Integer getQbitPort() {
        try {
            return restClient.get()
                    .uri(qbUrl + "/api/v2/app/preferences")
                    .retrieve()
                    .body(JsonNode.class)
                    .path("listen_port")
                    .asInt();
        } catch (Exception e) {
            log.error("Error fetching qBittorrent port: {}", e.getMessage());
            slackMessageService.sendSlackNotification("Error fetching qBittorrent port: " + e.getMessage());
            return null;
        }
    }

    private void updateQbitPort(int newPort) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(new PortPayload(newPort));
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("json", jsonPayload);

            restClient.post()
                    .uri(qbUrl + "/api/v2/app/setPreferences")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Error updating qBittorrent port: {}", e.getMessage());
            slackMessageService.sendSlackNotification("Error updating qBittorrent port: " + e.getMessage());
        }
    }

    private record PortPayload(int listen_port) {
    }

    
}
