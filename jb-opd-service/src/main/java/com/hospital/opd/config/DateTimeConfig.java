package com.hospital.opd.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

/**
 * The hospital runs on Nepal time. Business dates (token day, "today") use VisitService.HOSPITAL_ZONE
 * explicitly; this default only makes logs and ad-hoc OffsetDateTime.now() calls read naturally.
 */
@Configuration
public class DateTimeConfig {

    public static final ZoneId NEPAL_ZONE = ZoneId.of("Asia/Kathmandu");

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(NEPAL_ZONE));
    }
}
