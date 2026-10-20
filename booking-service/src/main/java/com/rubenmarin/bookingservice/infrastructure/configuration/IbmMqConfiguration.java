package com.rubenmarin.bookingservice.infrastructure.configuration;

import jakarta.jms.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class IbmMqConfiguration {

    // Bean jmsTemplate	@Primary	Productor e inspector
    // Bean reprocessingJmsTemplate	sessionTransacted(true)	Reprocesamiento

    @Bean
    @Primary
    //Importante: al crear nuestro propio JmsTemplate, Spring Boot normalmente deja de crear el suyo automáticamente.
    public JmsTemplate jmsTemplate(ConnectionFactory connectionFactory) {
        return new JmsTemplate(connectionFactory);
    }

    @Bean
    public JmsTemplate reprocessingJmsTemplate(ConnectionFactory connectionFactory) {
        JmsTemplate template = new JmsTemplate(connectionFactory);
        template.setSessionTransacted(true);
        return template;
    }

}
