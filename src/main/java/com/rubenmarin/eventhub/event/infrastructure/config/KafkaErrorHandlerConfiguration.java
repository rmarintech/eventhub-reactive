package com.rubenmarin.eventhub.event.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import tools.jackson.core.JacksonException;

@Configuration
public class KafkaErrorHandlerConfiguration {

    private static final Logger _log = LoggerFactory.getLogger(KafkaErrorHandlerConfiguration.class);
    private static final String BOOKING_CREATED_DLT_TOPIC = "booking-created.DLT";

    @Bean
    public NewTopic bookingCreatedDltTopic() {
        return TopicBuilder.name(BOOKING_CREATED_DLT_TOPIC)
                .partitions(1)
                .replicas(1)
                .build();
    }


    @Bean
    @Primary
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // 1 intento inicial + 2 reintentos.Espera de 1000 ms entre intentos.
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        // Enviamos el evento a <topic-original>.DLT.
        // Con partición -1, Kafka selecciona la partición de destino,
        // utilizando la key del mensaje cuando corresponda.
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, exception) ->
                                new TopicPartition(record.topic() + ".DLT", -1)
                );

        // Evita considerar recuperado un mensaje si falla su publicación en el DLT
        // El recoverer lanzará una excepción si falla la publicación.
        recoverer.setFailIfSendResultIsError(true);

        /** ERROR HANDLER (Envío a Dlt, Reintentar si falla) */
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);

        // Error de conexión PostgreSQL → Retry → Retry → DLT
        // JSON inválido → DLT directamente
        errorHandler.addNotRetryableExceptions(JacksonException.class);


        return errorHandler;
    }
}
