package com.finsmart.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

/** Ensures all LocalDate.now() calls use one explicit business timezone. */
@Configuration
public class ApplicationTimeZoneConfig {

    @Value("${app.time-zone:Asia/Kolkata}")
    private String timeZone;

    @PostConstruct
    void configureDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(timeZone)));
    }
}
