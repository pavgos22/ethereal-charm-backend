package com.ethereal.order.repository;

import com.ethereal.order.entity.Deliver;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeliverRepository extends JpaRepository<Deliver, Long> {
    Optional<Deliver> findByUuid(String uuid);
}
