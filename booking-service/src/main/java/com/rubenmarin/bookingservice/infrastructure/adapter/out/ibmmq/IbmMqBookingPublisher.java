package com.rubenmarin.bookingservice.infrastructure.adapter.out.ibmmq;

import com.rubenmarin.bookingservice.application.event.BookingCreatedIntegrationEvent;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class IbmMqBookingPublisher {

private final JmsTemplate jmsTemplate;
private final ObjectMapper objectMapper;

    public  IbmMqBookingPublisher (
            JmsTemplate jmsTemplate,
            ObjectMapper objectMapper){
        this.jmsTemplate=jmsTemplate;
        this.objectMapper=objectMapper;
    }

  public void  publish(BookingCreatedIntegrationEvent event){

      String json = objectMapper.writeValueAsString(event);

      jmsTemplate.convertAndSend("BOOKING.CREATED.Q", json);
  }
}
