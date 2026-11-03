package com.rubenmarin.eventhub.event.infrastructure.recovery;

import com.rubenmarin.eventhub.event.infrastructure.adapter.in.kafka.KafkaHeadersCodec;
import com.rubenmarin.eventhub.event.infrastructure.adapter.out.persistence.r2dbc.recovery.SpringDataEventRecoveryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.r2dbc.core.DatabaseClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

@ExtendWith(MockitoExtension.class)
class DltRecoverySchedulerTest {

    @Mock
    private DltRecoveryService dltRecoveryService;

    // SQL directo únicamente para preparar la fecha de caducidad del fixture.
    @Autowired
    private DatabaseClient databaseClient;

    @Autowired
    private SpringDataEventRecoveryRepository dataEventRecoveryRepository;

    @Autowired
    private KafkaHeadersCodec kafkaHeadersCodec;

    private static final String DLT_TOPIC = "booking-created.DLT";

    @InjectMocks
    private DltRecoveryScheduler dltRecoveryScheduler;

    @Test
    void shouldRejectTwoConsecutiveSchedulerExecutions() {

        // Simulamos una operación que permanece en ejecución
        Sinks.Empty<Void> sink = Sinks.empty();

        Mockito.when(dltRecoveryService.unblockExpired()).thenReturn(sink.asMono());

        // Primera ejecución: adquiere el permiso
        dltRecoveryScheduler.processUnblockExpired();

        // Segunda ejecución: debe rechazarse porque la primera sigue activa
        dltRecoveryScheduler.processUnblockExpired();

        // Solo debe haberse invocado una vez
        Mockito.verify(dltRecoveryService, Mockito.times(1)).unblockExpired();

        // Finalizamos la primera ejecución
        sink.tryEmitEmpty();

        // Tercera ejecución: debe poder adquirir nuevamente el permiso
        dltRecoveryScheduler.processUnblockExpired();

        // Ahora deben existir dos invocaciones
        Mockito.verify(dltRecoveryService, Mockito.times(2)).unblockExpired();
    }

    @Test
    void shouldReleaseRunningFlagWhenUnblockFails() {

        // GIVEN: simulamos un fallo de PostgreSQL durante el desbloqueo.
        // Mono.error() genera una señal de error cuando alguien se suscribe.
        Mockito.when(dltRecoveryService.unblockExpired())
                .thenReturn(Mono.error(new RuntimeException("Error")));

        // WHEN: primera ejecución del scheduler.
        // 1. compareAndSet(false, true) adquiere el permiso.
        // 2. Se llama a unblockExpired() y se produce el error simulado.
        // 3. El error llega al consumidor de errores de subscribe().
        // 4. doFinally() libera el permiso ejecutando running.set(false).
        dltRecoveryScheduler.processUnblockExpired();

        // WHEN: segunda ejecución del scheduler.
        // Como la primera ha terminado y ha liberado el permiso, esta segunda ejecución puede adquirirlo nuevamente.
        dltRecoveryScheduler.processUnblockExpired();

        // THEN: verificamos que unblockExpired() se ha invocado dos veces.
        // Esto demuestra que el fallo de la primera ejecución no ha dejado el scheduler bloqueado permanentemente.
        Mockito.verify(dltRecoveryService, Mockito.times(2))
                .unblockExpired();
    }



}