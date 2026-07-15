package com.tracepilot.api.Config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.DefaultJacksonJavaTypeMapper;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE_NAME = "tracepilot.audits";

    @Bean
    public TopicExchange tracepilotExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue auditJobsDlq() {
        return new Queue("audit.jobs.dlq", true); // durable
    }

    @Bean
    public Queue auditJobsQueue() {
        // Automatically route rejected/failed messages to the Dead Letter Queue
        return QueueBuilder.durable("audit.jobs")
                .withArgument("x-dead-letter-exchange", EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", "audit.job.dlq")
                .build();
    }

    @Bean
    public Queue auditProgressQueue() {
        return QueueBuilder.durable("audit.progress").withArgument("x-message-ttl", 60000).build();
        // no DLQ — losing a progress ping isn't worth retry
    }

    @Bean
    public Binding auditProgressBinding(Queue auditProgressQueue, TopicExchange tracepilotExchange) {
        return BindingBuilder.bind(auditProgressQueue).to(tracepilotExchange).with("audit.progress");
    }

    @Bean
    public Queue auditResultsDlq() {
        return new Queue("audit.results.dlq", true);
    }

    @Bean
    public Queue auditResultsQueue() {
        return QueueBuilder.durable("audit.results")
                .withArgument("x-dead-letter-exchange", EXCHANGE_NAME)
                .withArgument("x-dead-letter-routing-key", "audit.result.dlq")
                .build();
    }

    @Bean
    public Binding auditJobsBinding(Queue auditJobsQueue, TopicExchange tracepilotExchange) {
        return BindingBuilder.bind(auditJobsQueue).to(tracepilotExchange).with("audit.job");
    }

    @Bean
    public Binding auditJobsDlqBinding(Queue auditJobsDlq, TopicExchange tracepilotExchange) {
        return BindingBuilder.bind(auditJobsDlq).to(tracepilotExchange).with("audit.job.dlq");
    }

    @Bean
    public Binding auditResultsBinding(Queue auditResultsQueue, TopicExchange tracepilotExchange) {
        return BindingBuilder.bind(auditResultsQueue).to(tracepilotExchange).with("audit.result");
    }

    @Bean
    public Binding auditResultsDlqBinding(Queue auditResultsDlq, TopicExchange tracepilotExchange) {
        return BindingBuilder.bind(auditResultsDlq).to(tracepilotExchange).with("audit.result.dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        JacksonJsonMessageConverter converter = new JacksonJsonMessageConverter();
        DefaultJacksonJavaTypeMapper typeMapper = new DefaultJacksonJavaTypeMapper();
        typeMapper.setTrustedPackages("com.tracepilot.api.DTO.Messages");
        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}