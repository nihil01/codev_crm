package com.crm.service;

import com.crm.dto.PaymentDto;
import com.crm.enums.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** Invoice-period totals must not be confused with cash received in a calendar month. */
@Service
@RequiredArgsConstructor
public class BillingMetricsService {
    private final PaymentService payments;

    public record InvoiceTotals(BigDecimal expected, BigDecimal paidAgainstInvoices,
                                BigDecimal remaining, BigDecimal collectionRate) {}

    public InvoiceTotals forPeriod(int year,int month) {
        return fromInvoices(payments.getByPeriod(year,month));
    }

    public static InvoiceTotals fromInvoices(List<PaymentDto> invoices) {
        BigDecimal expected=BigDecimal.ZERO, paid=BigDecimal.ZERO, remaining=BigDecimal.ZERO;
        for(PaymentDto p:invoices) {
            if(p.isFrozen() || p.getStatus()==PaymentStatus.DONDURULUB ||
               p.getStatus()==PaymentStatus.BAGISLANIB) continue;
            BigDecimal discount=p.getDiscountAmount()==null?BigDecimal.ZERO:p.getDiscountAmount();
            BigDecimal net=p.getAmount().subtract(discount).max(BigDecimal.ZERO);
            BigDecimal cash=p.getPaidAmount()==null?BigDecimal.ZERO:p.getPaidAmount();
            expected=expected.add(net);
            paid=paid.add(cash);
            remaining=remaining.add(net.subtract(cash).max(BigDecimal.ZERO));
        }
        BigDecimal rate=expected.signum()>0
            ?paid.multiply(BigDecimal.valueOf(100)).divide(expected,2,RoundingMode.HALF_UP)
            :BigDecimal.ZERO;
        return new InvoiceTotals(expected,paid,remaining,rate);
    }

    public List<PaymentDto> allOverdue() {
        return payments.getAll().stream()
            .filter(p->p.getStatus()==PaymentStatus.GECIKIB)
            .filter(p->p.getRemainingAmount()!=null && p.getRemainingAmount().signum()>0).toList();
    }

    public BigDecimal totalOverdueDebt() {
        return allOverdue().stream().map(PaymentDto::getRemainingAmount)
            .reduce(BigDecimal.ZERO,BigDecimal::add);
    }
}
