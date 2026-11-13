package com.rubenmarin.aiassistantservice.application.service;

import com.rubenmarin.aiassistantservice.application.port.out.AiModelPort;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
 class AiServiceTest {

 @Mock
 AiModelPort aiModelPort;

 @InjectMocks
 AiService  aiService;

 @Test
 void shouldResponse() {

  String question = "Hello!";
  String response = "Hi!";

  Mockito.when(aiModelPort.generateResponse(question)).thenReturn(response);
  String result = aiService.ask(question);

  Assertions.assertEquals(response, result);

  Mockito.verify(aiModelPort, Mockito.times(1)).generateResponse(question);


 }

}
