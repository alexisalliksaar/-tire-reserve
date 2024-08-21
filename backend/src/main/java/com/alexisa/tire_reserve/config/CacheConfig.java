package com.alexisa.tire_reserve.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@Slf4j
public class CacheConfig {

    @Value("${workshops.cache.expire-after-write}")
    private Duration expireAfterWrite;

    @Value("${workshops.cache.refresh-after-write}")
    private Duration refreshAfterWrite;

    private static String formatDuration(Duration duration){
        long s = duration.getSeconds();
        return String.format("%d:%02d:%02d", s / 3600, (s % 3600) / 60, (s % 60));
    }

    @Bean
    public Caffeine<Object, Object> getCaffeine() {

        log.info(
                "Configured workshops cache to expire after write in '{}' and refresh after write in '{}'",
                formatDuration(expireAfterWrite),
                formatDuration(refreshAfterWrite)
        );
        return Caffeine.newBuilder()
                .expireAfterWrite(expireAfterWrite)
                .refreshAfterWrite(refreshAfterWrite);
    }
}
