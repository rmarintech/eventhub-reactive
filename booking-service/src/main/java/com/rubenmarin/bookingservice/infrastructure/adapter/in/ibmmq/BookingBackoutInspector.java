package com.rubenmarin.bookingservice.infrastructure.adapter.in.ibmmq;

import jakarta.jms.JMSException;
import jakarta.jms.TextMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

@Component
public class BookingBackoutInspector {

    private static final Logger _log = LoggerFactory.getLogger(BookingBackoutInspector.class);

    private final JmsTemplate jmsTemplate;

    public BookingBackoutInspector(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    public void inspect() {

        jmsTemplate.browse(
                "BOOKING.BACKOUT.Q",
                (session, browser) -> {
                    // Inspect messages
                    _log.info("************************************");

                    browser
                            .getEnumeration()
                            .asIterator()
                            .forEachRemaining(
                                    message ->
                                    {
                                        try {
                                            String messageID = ((TextMessage) message).getJMSMessageID();
                                            String textMessage = ((TextMessage) message).getText();
                                            _log.info("Message ID: {}", messageID);
                                            _log.info("Payload: {}", textMessage);
                                        } catch (JMSException e) {
                                            _log.error("Error while inspecting booking backout {}", e.getMessage());
                                        }
                                    }
                            );

                    return null;
                });
    }
}
