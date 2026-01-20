package com.tezzad.vpnscheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VpnschedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(VpnschedulerApplication.class, args);
	}

}
