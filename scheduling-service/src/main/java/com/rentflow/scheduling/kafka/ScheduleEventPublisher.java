package com.rentflow.scheduling.kafka;

import com.rentflow.scheduling.events.PaymentCreated;
import com.rentflow.scheduling.events.PaymentScheduled;
import com.rentflow.scheduling.model.PaymentSchedule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduleEventPublisher {

    private final KafkaTemplate<UUID, Object> kafkaTemplate;

    @Value("${kafka.topics.payment-created:payment-created}")
    private String paymentCreatedTopic;

    @Value("${kafka.topics.payment-scheduled:payment-scheduled}")
    private String paymentScheduledTopic;

    public void publishPaymentCreated(PaymentCreated event, UUID scheduleId) {
        publish(paymentCreatedTopic, scheduleId, event);
    }

    public void publishScheduleCreated(PaymentSchedule schedule) {
        var event = new PaymentScheduled(
            schedule.getId(),
            schedule.getId(),
            schedule.getRecurrencePattern().name(),
            schedule.getNextExecutionTime(),
            Instant.now(),
            1
        );
        publish(paymentScheduledTopic, schedule.getId(), event);
    }

    private void publish(String topic, UUID key, Object event) {
        try {
            CompletableFuture<org.springframework.kafka.support.SendResult<UUID, Object>> future =
                kafkaTemplate.send(topic, key, event);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish to {}: {}", topic, ex.getMessage());
                } else {
                    log.info("Published to {} with key {}", topic, key);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing to {}: {}", topic, e.getMessage(), e);
        }
    }
}
