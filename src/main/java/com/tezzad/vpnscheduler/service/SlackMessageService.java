package com.tezzad.vpnscheduler.service;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class SlackMessageService {

    private static final Logger log = LoggerFactory.getLogger(SlackMessageService.class);

    private final RestClient restClient;
    
    @Value("${app.slack.webhook-url}")
    private String slackUrl;

    public SlackMessageService() {
        this.restClient = RestClient.create();
    }

    public void sendSlackNotification(String message) {
        try {

            Map<String, String> payload = Map.of(
                "text", message
            );

            restClient.post()
                    .uri(slackUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Error sending Slack notification: {}", e.getMessage());
        }
    }

}
