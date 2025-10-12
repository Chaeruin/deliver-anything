package com.deliveranything.domain.payment.service;

import com.deliveranything.domain.order.enums.Publisher;
import com.deliveranything.domain.payment.config.TossPaymentsConfig;
import com.deliveranything.domain.payment.dto.PaymentCancelRequest;
import com.deliveranything.domain.payment.dto.PaymentCancelResponse;
import com.deliveranything.domain.payment.dto.PaymentConfirmRequest;
import com.deliveranything.domain.payment.dto.PaymentConfirmResponse;
import com.deliveranything.domain.payment.entitiy.Payment;
import com.deliveranything.domain.payment.enums.PaymentStatus;
import com.deliveranything.domain.payment.event.PaymentCancelFailedEvent;
import com.deliveranything.domain.payment.event.PaymentCancelSuccessEvent;
import com.deliveranything.domain.payment.event.PaymentFailedEvent;
import com.deliveranything.domain.payment.event.PaymentSuccessEvent;
import com.deliveranything.domain.payment.repository.PaymentRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@RequiredArgsConstructor
@Service
public class PaymentService {

  private final TossPaymentsConfig tossPaymentsConfig;
  private final WebClient.Builder webClientBuilder;
  private final PaymentRepository paymentRepository;
  private final ApplicationEventPublisher eventPublisher;

  private WebClient tossWebClient;
  private String encodedSecretKey;

  @PostConstruct
  public void init() {
    this.encodedSecretKey = Base64.getEncoder()
        .encodeToString((tossPaymentsConfig.getSecretKey() + ":").getBytes());

    this.tossWebClient = webClientBuilder
        .baseUrl(tossPaymentsConfig.getTossUrl())
        .defaultHeaders(headers -> {
          headers.setBasicAuth(encodedSecretKey);
          headers.setContentType(MediaType.APPLICATION_JSON);
        })
        .build();
  }

  @Transactional
  public void createPayment(String merchantUid, Long amount) {
    paymentRepository.save(new Payment(merchantUid, amount));
  }

  @Transactional
  public void confirmPayment(String paymentKey, String merchantUid, Long orderAmount) {
    Payment payment = getPayment(merchantUid, PaymentStatus.READY);

    if (orderAmount.equals(payment.getAmount())) {
      payment.updateStatus(PaymentStatus.FAILED);
      eventPublisher.publishEvent(new PaymentFailedEvent(merchantUid));
      throw new CustomException(ErrorCode.PAYMENT_AMOUNT_INVALID);
    }

    // 응답 수신 확인
    PaymentConfirmResponse pgResponse = tossWebClient.post()
        .uri("/v1/payments/confirm")
        .bodyValue(new PaymentConfirmRequest(paymentKey, merchantUid, orderAmount))
        .retrieve()
        .bodyToMono(PaymentConfirmResponse.class)
        .block();

    if (pgResponse == null) {
      payment.updateStatus(PaymentStatus.FAILED);
      eventPublisher.publishEvent(new PaymentFailedEvent(merchantUid));
      throw new CustomException(ErrorCode.PG_PAYMENT_NOT_FOUND);
    }

    // zero trust 검증
    if (!(paymentKey.equals(pgResponse.paymentKey()) && merchantUid.equals(pgResponse.orderId())
        && orderAmount.equals(pgResponse.totalAmount()))) {
      payment.updateStatus(PaymentStatus.FAILED);
      eventPublisher.publishEvent(new PaymentFailedEvent(merchantUid));
      throw new CustomException(ErrorCode.PG_PAYMENT_CONFIRM_FAILED);
    }

    payment.updateStatus(PaymentStatus.PAID);

    eventPublisher.publishEvent(new PaymentSuccessEvent(payment.getMerchantUid()));
  }

  @Transactional
  public void cancelPayment(String merchantUid, String cancelReason, Publisher publisher) {
    Payment payment = getPayment(merchantUid, PaymentStatus.PAID);

    // 응답 수신 확인
    PaymentCancelResponse pgResponse = tossWebClient.post()
        .uri("/v1/payments/{paymentKey}/cancel", payment.getPaymentKey())
        .bodyValue(new PaymentCancelRequest(payment.getPaymentKey(), cancelReason))
        .retrieve()
        .bodyToMono(PaymentCancelResponse.class)
        .block();

    if (pgResponse == null) {
      eventPublisher.publishEvent(new PaymentCancelFailedEvent(payment.getMerchantUid()));

      paymentRepository.save(new Payment(merchantUid, payment.getPaymentKey(), payment.getAmount(),
          PaymentStatus.CANCEL_FAILED));

      throw new CustomException(ErrorCode.PG_PAYMENT_CANCEL_FAILED);
    }

    // zero trust 검증 (결제 번호, 주문 번호, 가격)
    if (!(payment.getPaymentKey().equals(pgResponse.paymentKey())
        && merchantUid.equals(pgResponse.orderId()) && pgResponse.cancels().size() == 1
        && payment.getAmount().longValue() == pgResponse.cancels().getFirst().cancelAmount())
    ) {
      eventPublisher.publishEvent(new PaymentCancelFailedEvent(payment.getMerchantUid()));

      paymentRepository.save(new Payment(merchantUid, payment.getPaymentKey(), payment.getAmount(),
          PaymentStatus.CANCEL_FAILED));

      throw new CustomException(ErrorCode.PG_PAYMENT_CANCEL_FAILED);
    }

    paymentRepository.save(new Payment(merchantUid, payment.getPaymentKey(), payment.getAmount(),
        PaymentStatus.CANCELED));

    eventPublisher.publishEvent(new PaymentCancelSuccessEvent(payment.getMerchantUid(), publisher));
  }

  private Payment getPayment(String merchantUid, PaymentStatus status) {
    return paymentRepository.findByMerchantUidAndStatus(merchantUid, status)
        .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
  }
}
