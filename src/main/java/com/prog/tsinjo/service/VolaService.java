package com.prog.tsinjo.service;

import com.prog.tsinjo.domain.Payment;
import com.prog.tsinjo.repository.PaymentRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class VolaService {
    @Value("${vola.api.url}")
    private String volaApiUrl;

    @Value("${vola.api.key}")
    private String volaApiKey;

    private static final Duration VERIFICATION_INTERVAL = Duration.ofSeconds(5);
    private static final Duration TIMEOUT = Duration.ofMinutes(10);

    private final RestTemplate restTemplate;
    private final PaymentRepository paymentRepository;

    @Async
    public void verifyPaymentAsync(String paymentId) {
        LocalDateTime startTime = LocalDateTime.now();

        try {
            while (true) {
                Payment payment = paymentRepository.findById(paymentId)
                        .orElseThrow(() -> new RuntimeException("Payment not found"));

                if (Duration.between(startTime, LocalDateTime.now()).compareTo(TIMEOUT) > 0) {
                    updatePaymentStatus(payment, Payment.Status.FAILED, "Timeout");
                    break;
                }

                Payment.Status newStatus = fetchPaymentStatusFromVola(paymentId);
                if (newStatus != Payment.Status.VERIFYING) {
                    updatePaymentStatus(payment, newStatus, "Statut final");
                    break;
                }

                Thread.sleep(VERIFICATION_INTERVAL.toMillis());
            }
        } catch (Exception e) {
            log.error("Error verifying payment {}", paymentId, e);
            paymentRepository.findById(paymentId).ifPresent(payment -> {
                updatePaymentStatus(payment, Payment.Status.FAILED, "Erreur technique");
            });
        }
    }

    private Payment.Status fetchPaymentStatusFromVola(String paymentId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + volaApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            ResponseEntity<VolaStatusResponse> response = restTemplate.exchange(
                    String.format("%s/%s/status", volaApiUrl, paymentId),
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    VolaStatusResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return mapVolaStatus(response.getBody().getStatus());
            }
            return Payment.Status.VERIFYING;
        } catch (Exception e) {
            log.warn("Error fetching status from Vola for payment {}", paymentId, e);
            return Payment.Status.VERIFYING;
        }
    }

    private Payment.Status mapVolaStatus(String volaStatus) {
        return switch (volaStatus) {
            case "SUCCESS" -> Payment.Status.SUCCEEDED;
            case "FAILED" -> Payment.Status.FAILED;
            default -> Payment.Status.VERIFYING;
        };
    }

    private void updatePaymentStatus(Payment payment, Payment.Status status, String reason) {
        payment.setStatus(status);
        paymentRepository.save(payment);
        log.info("Payment {} updated to {} - Reason: {}", payment.getId(), status, reason);
    }

    @Getter
    @Setter
    private static class VolaStatusResponse {
        private String status;
    }
}