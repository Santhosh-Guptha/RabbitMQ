package com.example.ticketing_queues.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    @Autowired
    private RabbitMqProperties properties;

    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange(properties.getExchange());
    }

    @Bean
    public Queue crmQueue() {
        return new Queue(properties.getQueue().getCrm());
    }

    @Bean
    public Binding crmBinding() {
        return BindingBuilder
                .bind(crmQueue())
                .to(topicExchange())
                .with(properties.getRoutingKey().getCrm());
    }

    @Bean
    public Queue helpdeskQueue() {
        return new Queue(properties.getQueue().getHelpdesk());
    }

    @Bean
    public Binding helpdeskBinding() {
        return BindingBuilder
                .bind(helpdeskQueue())
                .to(topicExchange())
                .with(properties.getRoutingKey().getHelpdesk());
    }

    @Bean
    public Queue fieldmanagerQueue() {
        return new Queue(properties.getQueue().getFieldmanager());
    }

    @Bean
    public Binding fieldmanagerBinding() {
        return BindingBuilder
                .bind(fieldmanagerQueue())
                .to(topicExchange())
                .with(properties.getRoutingKey().getFieldmanager());
    }

    @Bean
    public Queue fieldrepresentativeQueue() {
        return new Queue(properties.getQueue().getFieldrepresentative());
    }

    @Bean
    public Binding fieldrepresentativeBinding() {
        return BindingBuilder
                .bind(fieldrepresentativeQueue())
                .to(topicExchange())
                .with(properties.getRoutingKey().getFieldrepresentative());
    }

    @Bean
    public Queue defaultQueue() {
        return new Queue("DEFAULT"); // Replace "default_queue" with your desired name
    }

    @Bean
    public Binding defaultBinding() {
        return BindingBuilder
                .bind(defaultQueue())
                .to(topicExchange())
                .with(properties.getRoutingKey().getDefaultRoutingKey());
    }

    @Bean
    public Queue ticketanalysisQueue() {
        return new Queue(properties.getQueue().getTicketanalysis());
    }

    @Bean
    public Binding ticketanalysisBinding() {
        return BindingBuilder
                .bind(ticketanalysisQueue())
                .to(topicExchange())
                .with(properties.getRoutingKey().getTicketanalysis());
    }

    @Bean
    public Queue sitemanagerQueue() {
        return new Queue(properties.getQueue().getSitemanager());
    }

    @Bean
    public Binding sitemanagerBinding() {
        return BindingBuilder
                .bind(sitemanagerQueue())
                .to(topicExchange())
                .with(properties.getRoutingKey().getSitemanager());
    }
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory){
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter());
        return rabbitTemplate;
    }
}
