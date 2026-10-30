package com.usanmap.usan.service;

import com.usanmap.usan.entity.CreditOrder;
import com.usanmap.usan.entity.Payment;
import com.usanmap.usan.entity.enums.OrderStatus;
import com.usanmap.usan.entity.enums.PaymentStatus;
import com.usanmap.usan.entity.enums.PgProvider;
import com.usanmap.usan.repository.CreditOrderRepository;
import com.usanmap.usan.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentStatusService {

    private final CreditOrderRepository creditOrderRepository;
    private final PaymentRepository paymentRepository;

    /** 결과 대기 시간이 지난 헥토 카드 주문만 미완료 상태로 전환한다. */
    @Transactional
    public ExpirationResult expireHectoOrder(Long orderId, LocalDateTime cutoff) {
        CreditOrder order = creditOrderRepository.findByIdForUpdate(orderId).orElse(null);
        if (order == null || order.getOrderStatus() != OrderStatus.READY
                || order.getOrderedAt().isAfter(cutoff)) {
            return null;
        }

        Payment payment = paymentRepository.findByCreditOrder(order).orElse(null);
        if (payment == null || payment.getPgProvider() != PgProvider.HECTO
                || payment.getPaymentStatus() != PaymentStatus.READY) {
            return null;
        }

        order.setOrderStatus(OrderStatus.EXPIRED);
        payment.setPaymentStatus(PaymentStatus.EXPIRED);
        return new ExpirationResult(order.getId(), order.getOrderNo(), payment.getId());
    }

    public record ExpirationResult(Long orderId, String orderNo, Long paymentId) {}
}
