package com.deliveranything.domain.payment.service;

import com.deliveranything.domain.order.enums.Publisher;
import com.deliveranything.domain.payment.config.TossPaymentsConfig;
import com.deliveranything.domain.payment.entitiy.Payment;
import com.deliveranything.domain.payment.enums.PaymentStatus;
import com.deliveranything.domain.payment.event.PaymentCancelSuccessEvent;
import com.deliveranything.domain.payment.event.PaymentFailedEvent;
import com.deliveranything.domain.payment.event.PaymentSuccessEvent;
import com.deliveranything.domain.payment.repository.PaymentRepository;
import com.deliveranything.global.exception.CustomException;
import com.deliveranything.global.exception.ErrorCode;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentService {

  private final PaymentRepository paymentRepository;
  private final TossPaymentsConfig tossPaymentsConfig;
  private final WebClient.Builder webClientBuilder;
  private final ApplicationEventPublisher eventPublisher;

  private WebClient tossWebClient;
  private String encodedSecretKey;

  @PostConstruct
  public void init() {
    this.encodedSecretKey = Base64.getEncoder()
        .encodeToString((tossPaymentsConfig.getSecretKey() + ":").getBytes(StandardCharsets.UTF_8));

    log.info("시크릿키 확인: {}, 토스 url 확인: {}", tossPaymentsConfig.getSecretKey(),
        tossPaymentsConfig.getTossUrl());

    this.tossWebClient = webClientBuilder
        .baseUrl(tossPaymentsConfig.getTossUrl())
        .defaultHeaders(headers -> {
          headers.setBasicAuth(encodedSecretKey);
          headers.setContentType(MediaType.APPLICATION_JSON);
        })
        .filter(logRequest())
        .build();
  }

  private ExchangeFilterFunction logRequest() {
    return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
      log.info(">>>>>> [REQUEST] {} {}", clientRequest.method(), clientRequest.url());
      clientRequest.headers().forEach((name, values) ->
          values.forEach(value -> log.info("  {}: {}", name, value))
      );

      return Mono.just(clientRequest);
    });
  }

  @Transactional
  public void createPayment(String paymentKey, String merchantUid, Long amount) {
    paymentRepository.save(new Payment(merchantUid, paymentKey, amount, PaymentStatus.READY));
  }

  @Transactional
  public void confirmPayment(String paymentKey, String merchantUid, Long orderAmount) {
    Payment payment = getPayment(merchantUid, PaymentStatus.READY);

    if (!orderAmount.equals(payment.getAmount())) {
      payment.updateStatus(PaymentStatus.FAILED);
      eventPublisher.publishEvent(new PaymentFailedEvent(merchantUid));
      throw new CustomException(ErrorCode.PAYMENT_AMOUNT_INVALID);
    }

    /*
    PaymentConfirmResponse pgResponse = null;
    try {
      log.info("paymentKey: {}, orderId: {}, amount: {}", paymentKey, merchantUid, orderAmount);
       //응답 수신 확인
      pgResponse = tossWebClient.post()
          .uri("/v1/payments/confirm")
          .bodyValue(new PaymentConfirmRequest(paymentKey, merchantUid, orderAmount))
          .retrieve()
          .bodyToMono(PaymentConfirmResponse.class)
          .block();
      log.info("결제 성공했냐?");

    } catch (WebClientResponseException e) {
      // PG사로부터 4xx, 5xx 에러 응답을 받은 경우
      payment.updateStatus(PaymentStatus.FAILED);
      eventPublisher.publishEvent(new PaymentFailedEvent(merchantUid));

      // 에러 응답 본문을 String으로 가져와서 로깅하거나 파싱할 수 있습니다.
      String errorBody = e.getResponseBodyAsString();
      log.error("PG사 결제 승인 실패. 응답: {}", errorBody);
    }

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
    */

    payment.updateStatus(PaymentStatus.PAID);

    eventPublisher.publishEvent(new PaymentSuccessEvent(payment.getMerchantUid()));
  }

  @Transactional
  public void cancelPayment(String merchantUid, String cancelReason, Publisher publisher) {
    Payment payment = getPayment(merchantUid, PaymentStatus.PAID);

    /*
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
    */

    paymentRepository.save(new Payment(merchantUid, payment.getPaymentKey(), payment.getAmount(),
        PaymentStatus.CANCELED));

    eventPublisher.publishEvent(new PaymentCancelSuccessEvent(payment.getMerchantUid(), publisher));
  }

  private Payment getPayment(String merchantUid, PaymentStatus status) {
    return paymentRepository.findByMerchantUidAndStatus(merchantUid, status)
        .orElseThrow(() -> new CustomException(ErrorCode.PAYMENT_NOT_FOUND));
  }
}
