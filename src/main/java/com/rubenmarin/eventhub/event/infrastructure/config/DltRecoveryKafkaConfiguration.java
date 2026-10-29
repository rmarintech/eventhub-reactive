package com.rubenmarin.eventhub.event.infrastructure.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class DltRecoveryKafkaConfiguration {

    @Bean
    public DefaultErrorHandler dltRecoveryErrorHandler() {

        FixedBackOff backOff = new FixedBackOff(
                FixedBackOff.DEFAULT_INTERVAL,
                FixedBackOff.UNLIMITED_ATTEMPTS
        );

        //return new DefaultErrorHandler(backOff);
        DefaultErrorHandler handler = new DefaultErrorHandler(backOff);

        handler.setRetryListeners((record, ex, deliveryAttempt) -> {
            System.out.printf(
                    "DLT RETRY: topic=%s partition=%d offset=%d attempt=%d error=%s%n",
                    record.topic(),
                    record.partition(),
                    record.offset(),
                    deliveryAttempt,
                    ex.getClass().getSimpleName()
            );
        });

        return handler;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String> dltRecoveryKafkaListenerContainerFactory(
            ConsumerFactory<String, String> consumerFactory,
            @Qualifier("dltRecoveryErrorHandler") DefaultErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        factory.setConsumerFactory(consumerFactory);
        factory.setCommonErrorHandler(errorHandler);

        return factory;
    }

}