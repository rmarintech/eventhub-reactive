package com.rubenmarin.eventhub.event.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

@Configuration
public class KafkaErrorHandlerConfiguration {

    private static final Logger _log = LoggerFactory.getLogger(KafkaErrorHandlerConfiguration.class);

    private static final String DLT_TOPIC = "booking-created.DLT";

    @Bean
    public NewTopic bookingCreatedDltTopic() {
        return TopicBuilder.name(DLT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }


    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // 1 intento inicial + 2 reintentos.
        // Espera de 1000 ms entre intentos.
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        // Publicar el mensaje fallido en nuestra DLT.
        // Por defecto, Spring Kafka envía el mensaje fallido a la misma partición
        // del topic DLT. Por eso, la DLT debe tener al menos tantas particiones
        // como el topic original. Con TopicPartition(DLT_TOPIC, -1),
        // Kafka puede elegir la partición de destino.
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(DLT_TOPIC, -1)
                );

        // No dar por recuperado el mensaje si falla su publicación en la DLT.
        // El recoverer lanzará una excepción si falla la publicación.

        recoverer.setFailIfSendResultIsError(true);

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Error de conexión PostgreSQL → Retry → Retry → DLT
        // SON inválido   → DLT directamente
        // Un JSON inválido no se arreglará reintentándolo.
        errorHandler.addNotRetryableExceptions(JacksonException.class);


        return errorHandler;
    }
}
