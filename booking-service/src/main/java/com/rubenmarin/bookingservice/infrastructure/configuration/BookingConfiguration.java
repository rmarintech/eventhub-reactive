package com.rubenmarin.bookingservice.infrastructure.configuration;

import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.application.port.out.EventCapacityPort;
import com.rubenmarin.bookingservice.application.port.out.OutboxRepository;
import com.rubenmarin.bookingservice.application.service.BookingService;
import com.rubenmarin.bookingservice.infrastructure.transaction.CompensatingBookingUseCase;
import com.rubenmarin.bookingservice.infrastructure.transaction.TransactionalBookingUseCase;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.scheduling.annotation.EnableScheduling;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class BookingConfiguration {

    // Implementación inicial en memoria.
    // Ya no la registramos como bean porque ahora utilizamos
    // JpaBookingRepositoryAdapter para persistir en PostgreSQL.
    // Se conserva como referencia de la evolución del proyecto.
//    @Bean
//    public BookingRepository bookingRepository() {
//        return new InMemoryBookingRepository();
//    }

    // Antes registrábamos directamente BookingService como BookingUseCase.
    // Para mantener la capa de aplicación independiente de Spring,
    // ahora envolvemos el servicio en un decorador transaccional.
//    @Bean
//    public BookingUseCase bookingUseCase(BookingRepository repository) {
//        return new BookingService(repository);
//    }


    // Registramos un único bean BookingUseCase: el decorador.
    // Spring aplica @Transactional al decorador mediante un proxy AOP.
    // BookingService mantiene la lógica de aplicación sin depender de Spring.
//    @Bean
//    public BookingUseCase bookingUseCase(
//            BookingRepository repository
//    ) {
//        BookingUseCase service = new BookingService(repository);
//        return new TransactionalBookingUseCase(service);
//    }


    /**
     * Separamos la transacción JPA de la compensación en dos decoradores.
     *
     * TransactionalBookingUseCase es un bean independiente para que
     * Spring aplique su proxy @Transactional (BEGIN / COMMIT / ROLLBACK).
     *
     * CompensatingBookingUseCase queda fuera de esa transacción:
     * si falla el guardado o el COMMIT, captura la excepción y solicita
     * liberar las plazas reservadas en Event Service.
     *
     * La operación remota no participa en la transacción JPA.
     *
     *
     * BookingController
     *        ↓
     * CompensatingBookingUseCase
     *        ├── Reservar plaza en Event Service (REST)
     *        ▼
     * TransactionalBookingUseCase
     *        ├── BEGIN TRANSACTION
     *        ├── BookingService.createBooking()
     *        │       └── INSERT bookings
     *        ├── Crear BookingCreatedIntegrationEvent
     *        ├── OutboxRepository.createOutboxEvent()
     *        │       └── INSERT outbox_events (PENDING)
     *        ├── COMMIT / ROLLBACK
     *        ▼
     * CompensatingBookingUseCase
     *        ├── Éxito → Devolver Booking
     *        └── Si falla → Liberar plaza en Event Service
     *
     *  La publicación en Kafka NO se realiza dentro de esta transacción.
     *  Un scheduler procesará posteriormente los eventos pendientes
     *  del Outbox y los publicará en Kafka.
     */

    @Bean
    public BookingUseCase transactionalBookingUseCase(
            BookingRepository repository,
            OutboxRepository outboxRepository,
            ObjectMapper objectMapper) {

        // 1. Lógica de Booking, sin dependencias de infraestructura
        BookingUseCase service = new BookingService(repository);

        // 2. Transacción JPA: incluye el COMMIT
        return new TransactionalBookingUseCase(service,outboxRepository,objectMapper);
    }

    @Bean
    @Primary
    public BookingUseCase bookingUseCase(
            @Qualifier("transactionalBookingUseCase") BookingUseCase transactionalService,
            EventCapacityPort eventCapacityPort
    ) {
        // 3. Coordinación remota y compensación, fuera de la transacción
        BookingUseCase compensatingService =
                new CompensatingBookingUseCase(
                        transactionalService,
                        eventCapacityPort
                );

        return compensatingService;
    }

}
