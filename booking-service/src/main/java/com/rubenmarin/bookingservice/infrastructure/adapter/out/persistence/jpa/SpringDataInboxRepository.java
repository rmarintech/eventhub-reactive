package com.rubenmarin.bookingservice.infrastructure.adapter.out.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataInboxRepository extends JpaRepository<InboxEntity, UUID> {
}
