package com.rubenmarin.aiassistantservice;

import com.rubenmarin.aiassistantservice.application.port.in.GetAvailableSeatsUseCase;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.time.Duration;
import java.util.UUID;

@SpringBootApplication
public class AiAssistantServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAssistantServiceApplication.class, args);
    }

    @Bean
    @Profile("!test")
    CommandLineRunner test(GetAvailableSeatsUseCase getAvailableSeatsUseCase) {
        return args -> {
            System.out.println("Application started!");

            UUID uuid = UUID.fromString("0ebbca7e-87bb-4c1d-915c-676b59ffe329");
            Integer available = getAvailableSeatsUseCase
                    .getAvailableSeats(uuid)
                    .blockOptional(Duration.ofSeconds(10))
                    .orElse(null);

            System.out.println("Available Seats: " + available);
        };
    }
}
