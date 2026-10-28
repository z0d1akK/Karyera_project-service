package com.zodiakk.projectservice.config;

import com.zodiakk.projectservice.common.constants.KafkaEventConstants;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${app.kafka.topics.project-events}")
    private String projectEventsTopic;

    @Bean
    public NewTopic projectEventsTopic() {
        return buildTopic(projectEventsTopic);
    }

    @Bean
    public NewTopic projectEventsDltTopic() {
        return buildTopic(projectEventsTopic + KafkaEventConstants.DLT_SUFFIX);
    }

    private NewTopic buildTopic(String name) {
        return TopicBuilder.name(name)
                .partitions(3)
                .replicas(1)
                .build();
    }
}
