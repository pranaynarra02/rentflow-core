package com.rentflow.gateway.repository;

import com.rentflow.gateway.model.PaymentInitiation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentInitiationRepository extends JpaRepository<PaymentInitiation, UUID> {

    Optional<PaymentInitiation> findByPaymentId(UUID paymentId);
    Optional<PaymentInitiation> findByStripePaymentIntentId(String paymentIntentId);
    Optional<PaymentInitiation> findByPlaidPaymentId(String plaidPaymentId);
}
