package com.sism.config;

import com.sism.shared.infrastructure.event.EventStore;
import com.sism.shared.infrastructure.event.EventStoreInMemory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventStoreFallbackConfig {

    @Bean
    @ConditionalOnMissingBean(EventStore.class)
    public EventStore eventStore() {
        return new EventStoreInMemory();
    }
}
