package com.example.worker_fifo.config;

import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Value("${spring.rabbitmq.host}")
    private String habbit_host;

    @Value("${spring.rabbitmq.username}")
    private String habbit_username;

    @Value("${spring.rabbitmq.password}")
    private String habbit_password;

    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory connectionFactory =
                new CachingConnectionFactory(habbit_host);

        connectionFactory.setUsername(habbit_username);
        connectionFactory.setPassword(habbit_password);
        return connectionFactory;
    }

    @Bean
    public RabbitAdmin rbAdmin() {
        return new RabbitAdmin(connectionFactory());
    }

    @Bean
    public RabbitTemplate rabbitTemplate() {
        return new RabbitTemplate(connectionFactory());
    }

}