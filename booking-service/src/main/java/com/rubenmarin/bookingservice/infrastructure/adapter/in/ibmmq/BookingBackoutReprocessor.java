package com.rubenmarin.bookingservice.infrastructure.adapter.in.ibmmq;

import jakarta.jms.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.lang.IllegalStateException;

@Component
public class BookingBackoutReprocessor {

    private static final Logger _log = LoggerFactory.getLogger(BookingBackoutReprocessor.class);

    private final JmsTemplate jmsTemplate;

    public BookingBackoutReprocessor(
            @Qualifier("reprocessingJmsTemplate") JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void reprocess(String jmsMessageId) {

        jmsTemplate.execute(session -> {
            String selector = "JMSMessageID = '" + jmsMessageId + "'";
            Queue source = session.createQueue("BOOKING.BACKOUT.Q");
            Queue destination = session.createQueue("BOOKING.CREATED.Q");


            try (MessageConsumer consumer = session.createConsumer(source, selector);
                 MessageProducer producer = session.createProducer(destination)) {

                //receive(): recupera el mensaje de BOOKING.BACKOUT.Q.
                Message message = consumer.receive(1000);

                if (message == null) {
                    throw new IllegalStateException("Message not found: " + jmsMessageId);
                }

                //send(): lo vuelve a publicar en BOOKING.CREATED.Q para que el consumidor lo procese de nuevo
                producer.send(message);

                session.commit();
            }

            return null;
        },true);
    }
}
