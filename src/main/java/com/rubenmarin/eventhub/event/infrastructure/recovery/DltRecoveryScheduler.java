package com.rubenmarin.eventhub.event.infrastructure.recovery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class DltRecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(DltRecoveryScheduler.class);
    private final DltRecoveryService dltRecoveryService;
    private final  AtomicBoolean running = new AtomicBoolean(false);

    public DltRecoveryScheduler(DltRecoveryService dltRecoveryService) {
        this.dltRecoveryService = dltRecoveryService;
    }

    @Scheduled(fixedDelay = 60000)
    public void processUnblockExpired() {
        log.info("Running DLT recovery unblock scheduler");
        //Si el valor actual es false, cámbialo a true y devuelve true. Si ya era true, no lo modifiques y devuelve false.
        boolean acquired = running.compareAndSet(false, true);
        if (acquired) {
            dltRecoveryService.unblockExpired()
                    .doFinally(dltEvent -> {
                        running.set(false);
                    })
                    .subscribe( value -> {}, throwable-> {
                        log.error("Error while processing unblock expired event", throwable);
                    });
        }
    }

}
