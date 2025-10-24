package org.team.mealkitshop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "items.stop")
public record ItemDeletePolicyProperties(int retentionDays) { }
