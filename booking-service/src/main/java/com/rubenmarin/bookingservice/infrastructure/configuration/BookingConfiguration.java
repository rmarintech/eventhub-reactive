package com.rubenmarin.bookingservice.infrastructure.configuration;

import com.rubenmarin.bookingservice.application.port.in.BookingUseCase;
import com.rubenmarin.bookingservice.application.port.out.BookingRepository;
import com.rubenmarin.bookingservice.application.port.out.EventAvailabilityPort;
import com.rubenmarin.bookingservice.application.service.BookingService;
import com.rubenmarin.bookingservice.infrastructure.transaction.TransactionalBookingUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
    @Bean
    public BookingUseCase bookingUseCase(BookingRepository repository, EventAvailabilityPort eventAvailability) {
        BookingUseCase service = new BookingService(repository, eventAvailability);
        return new TransactionalBookingUseCase(service);
    }

}
