package com.rubenmarin.eventhub.event.infrastructure.config;

import com.rubenmarin.eventhub.event.application.event.BookingCreatedIntegrationEvent;
import com.rubenmarin.eventhub.event.application.model.InboxEvent;
import com.rubenmarin.eventhub.event.application.port.out.InboxRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;
import reactor.core.publisher.Mono;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;


@ExtendWith(MockitoExtension.class)
class KafkaErrorHandlerConfigurationTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private SendResult<String, String> sendResult;
    @Mock
    private Consumer<String, String> consumer;
    @Mock
    private MessageListenerContainer messageListenerContainer;


    private ConsumerRecord<String, String> consumerRecord;
    private ObjectMapper mapper = new JsonMapper();
    private KafkaErrorHandlerConfiguration configuration = new KafkaErrorHandlerConfiguration();
    private DefaultErrorHandler errorHandler;


    @BeforeEach
    void setUp() {
        errorHandler = configuration.kafkaErrorHandler(kafkaTemplate);
        consumerRecord = new ConsumerRecord<>(
                "booking-created",
                3,
                42L,
                "booking-1",
                "{Invalid Json}"
        );
    }

    // Orden    Test	                Qué queremos demostrar
    // 1	JSON inválido	            Se envía directamente a DLT
    // 2	Error temporal	            Se permiten dos reintentos
    // 3	Reintentos agotados	        El tercer fallo provoca publicación en DLT
    // 4	Fallo publicando en DLT	    El mensaje no se considera recuperado

    @Test
    void shouldSendInvalidJsonDirectlyToDlt() {
        // Json que no puede deserializarse, tiene que ir directo a DLT

        // Una excepción real de Jackson que represente un error de deserialización.
        JacksonException jacksonException = Assertions
                .assertThrows(JacksonException.class, () -> {
                    mapper.readerFor(BookingCreatedIntegrationEvent.class)
                            .readValue("{Invalid Json}");

                });

        // Simular que KafkaTemplate publica correctamente el mensaje.
        Mockito.when(kafkaTemplate.send(Mockito.any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));


        // Ejecutar el DefaultErrorHandler y comprobar el resultado.
        boolean handled = errorHandler.handleOne(jacksonException, consumerRecord, consumer, messageListenerContainer);
        Assertions.assertTrue(handled);

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        Mockito.verify(kafkaTemplate).send(captor.capture());

        ProducerRecord<String, String> record = captor.getValue();
        Assertions.assertEquals("booking-created.DLT", record.topic());
        // new TopicPartition(DLT_TOPIC, -1).
        // Spring Kafka transforma esa partición negativa en null,
        // delegando la selección de partición al productor Kafka.
        Assertions.assertNull(record.partition());

        Assertions.assertEquals(consumerRecord.key(), record.key());
        Assertions.assertEquals(consumerRecord.value(), record.value());
    }

    @Test
    void shouldAllowTwoRetriesWithoutPublishingToDlt() {
        // Consumidor intenta guardar el evento en PostgreSQL,
        // pero la base de datos está temporalmente caída. Lo debiera intentar 2 veces.
        DataAccessResourceFailureException bbddException =
                new DataAccessResourceFailureException("PostgreSQL unavailable");

        boolean handled1 = errorHandler.handleOne(bbddException, consumerRecord, consumer, messageListenerContainer);
        Assertions.assertFalse(handled1);
        boolean handled2 = errorHandler.handleOne(bbddException, consumerRecord, consumer, messageListenerContainer);
        Assertions.assertFalse(handled2);

        Mockito.verify(kafkaTemplate, Mockito.never()).send(Mockito.any(ProducerRecord.class));
    }

    @Test
    void shouldSendToDltAfterExhaustingRetries() {

        // consumidor intenta guardar el evento en PostgreSQL, pero está caída permanentemente.
        // Lo debiera intentar 2 veces y enviarlo a DLT a la 3a.

        DataAccessResourceFailureException bbddException =
                new DataAccessResourceFailureException("PostgreSQL unavailable");

        // Simular que KafkaTemplate publica correctamente el mensaje.
        Mockito.when(kafkaTemplate.send(Mockito.any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(sendResult));

        // false	El error todavía no se ha recuperado.
        //          El contenedor de Kafka debe volver a procesar el mensaje.
        // true	    El error se ha gestionado correctamente.
        //          No es necesario volver a procesar ese mensaje por ese error.
        // true no significa que el negocio se haya procesado correctamente.
        // Significa que el error se ha gestionado, por ejemplo enviando el mensaje a la DLT.

        boolean handled1 = errorHandler.handleOne(bbddException, consumerRecord, consumer, messageListenerContainer);
        Assertions.assertFalse(handled1);
        boolean handled2 = errorHandler.handleOne(bbddException, consumerRecord, consumer, messageListenerContainer);
        Assertions.assertFalse(handled2);
        boolean handled3 = errorHandler.handleOne(bbddException, consumerRecord, consumer, messageListenerContainer);
        Assertions.assertTrue(handled3);

        ArgumentCaptor<ProducerRecord<String, String>> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        Mockito.verify(kafkaTemplate).send(captor.capture());

        ProducerRecord<String, String> record = captor.getValue();
        Assertions.assertEquals("booking-created.DLT", record.topic());
        // new TopicPartition(DLT_TOPIC, -1).
        // Spring Kafka transforma esa partición negativa en null,
        // delegando la selección de partición al productor Kafka.
        Assertions.assertNull(record.partition());

        Assertions.assertEquals(consumerRecord.key(), record.key());
        Assertions.assertEquals(consumerRecord.value(), record.value());
    }

    @Test
    void shouldNotRecoverWhenDltPublishingFails() {


        RuntimeException kafkaException =  new RuntimeException("Kafka DLT unavailable");
        DataAccessResourceFailureException bbddException =
                new DataAccessResourceFailureException("PostgreSQL unavailable");

        // Simular que KafkaTemplate lanza una excepción.
        Mockito.when(kafkaTemplate.send(Mockito.any(ProducerRecord.class)))
                .thenThrow(kafkaException);

        boolean handled1 = errorHandler.handleOne(bbddException, consumerRecord, consumer, messageListenerContainer);
        Assertions.assertFalse(handled1);
        boolean handled2 = errorHandler.handleOne(bbddException, consumerRecord, consumer, messageListenerContainer);
        Assertions.assertFalse(handled2);

        boolean handled3 = errorHandler.handleOne(bbddException, consumerRecord, consumer, messageListenerContainer);
        Assertions.assertFalse(handled3);

        Mockito.verify(kafkaTemplate).send(Mockito.any(ProducerRecord.class));

    }
}
