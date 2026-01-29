package com.rentflow.payment.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentflow.events.*;
import com.rentflow.payment.model.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentEventPublisher {

    private final KafkaTemplate<UUID, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topics.payment-created:payment-created}")
    private String paymentCreatedTopic;

    @Value("${kafka.topics.payment-completed:payment-completed}")
    private String paymentCompletedTopic;

    @Value("${kafka.topics.payment-failed:payment-failed}")
    private String paymentFailedTopic;

    public void publishPaymentCreated(Payment payment) {
        var event = new PaymentCreated(
            payment.getId(),
            payment.getTenantId(),
            payment.getPropertyId(),
            payment.getLeaseId(),
            payment.getAmount(),
            payment.getCurrency(),
            payment.getPaymentMethod(),
            payment.getPaymentType(),
            payment.getScheduledFor(),
            Instant.now(),
            1
        );

        publish(paymentCreatedTopic, payment.getId(), event);
    }

    public void publishPaymentCompleted(Payment payment) {
        var event = new PaymentCompleted(
            payment.getId(),
            payment.getTransactionId(),
            payment.getSettledAmount() != null ? payment.getSettledAmount() : payment.getAmount(),
            payment.getFeeAmount() != null ? payment.getFeeAmount() : BigDecimal.ZERO,
            payment.getPaymentMethod().name(),
            payment.getCompletedAt() != null ? payment.getCompletedAt() : Instant.now(),
            Instant.now(),
            1
        );

        publish(paymentCompletedTopic, payment.getId(), event);
    }

    public void publishPaymentFailed(Payment payment, Throwable error) {
        var event = new PaymentFailed(
            payment.getId(),
            error.getClass().getSimpleName(),
            error.getMessage(),
            payment.canRetry(),
            payment.getRetryAfter(),
            Instant.now(),
            1
        );

        publish(paymentFailedTopic, payment.getId(), event);
    }

    private void publish(String topic, UUID key, Object event) {
        try {
            CompletableFuture<SendResult<UUID, Object>> future = kafkaTemplate.send(topic, key, event);
            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event to {}: {}", topic, ex.getMessage());
                } else {
                    log.info("Published event to {} with key {}", topic, key);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing to {}: {}", topic, e.getMessage(), e);
        }
    }
}
