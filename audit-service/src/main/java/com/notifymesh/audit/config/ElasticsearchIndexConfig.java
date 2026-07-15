package com.notifymesh.audit.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ElasticsearchIndexConfig {

    /**
     * Referenced via SpEL ({@code #{@auditIndexName}}) from {@code @Document(indexName = ...)}
     * so the index name stays driven by {@code notifymesh.elasticsearch.index.deliveries}
     * instead of being hardcoded in the entity.
     */
    @Bean
    public String auditIndexName(@Value("${notifymesh.elasticsearch.index.deliveries}") String indexName) {
        return indexName;
    }
}
