package com.tracepilot.api.Config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue auditJobsQueue() {
        return new Queue("audit.jobs", true); // durable
    }

    @Bean
    public TopicExchange tracepilotExchange() {
        return new TopicExchange("tracepilot.audits");
    }

    @Bean
    public Binding auditJobsBinding(Queue auditJobsQueue, TopicExchange tracepilotExchange) {
        return BindingBuilder.bind(auditJobsQueue).to(tracepilotExchange).with("audit.job");
    }
}
