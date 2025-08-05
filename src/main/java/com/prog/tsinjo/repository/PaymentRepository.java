package com.prog.tsinjo.repository;

import com.prog.tsinjo.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, String> {
}
