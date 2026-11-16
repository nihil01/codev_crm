package com.crm;

import com.crm.dto.PaymentDto;
import com.crm.enums.PaymentStatus;
import com.crm.service.BillingMetricsService;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class BillingMetricsServiceTest {
    private PaymentDto invoice(String amount,String paid,String discount,PaymentStatus status,
                               boolean frozen) {
        PaymentDto p=new PaymentDto();
        p.setAmount(new BigDecimal(amount));
        p.setPaidAmount(new BigDecimal(paid));
        p.setDiscountAmount(new BigDecimal(discount));
        p.setStatus(status);
        p.setFrozen(frozen);
        return p;
    }

    @Test
    void partialPaymentsAndDiscountDoNotConfuseCashWithExpectedRevenue() {
        var entries=List.of(
            invoice("300.00","100.00","20.00",PaymentStatus.GOZLENILIR,false),
            invoice("200.00","0.00","0.00",PaymentStatus.BAGISLANIB,false),
            invoice("150.00","0.00","0.00",PaymentStatus.DONDURULUB,true));
        var totals=BillingMetricsService.fromInvoices(entries);
        assertEquals(new BigDecimal("280.00"),totals.expected());
        assertEquals(new BigDecimal("100.00"),totals.paidAgainstInvoices());
        assertEquals(new BigDecimal("180.00"),totals.remaining());
        assertEquals(new BigDecimal("35.71"),totals.collectionRate());
    }

    @Test
    void completelyDiscountedInvoiceHasZeroDueAndZeroCollectionRate() {
        var total=BillingMetricsService.fromInvoices(List.of(
            invoice("100.00","0.00","100.00",PaymentStatus.ODENILIB,false)));
        assertEquals(new BigDecimal("0.00"),total.expected());
        assertEquals(BigDecimal.ZERO,total.collectionRate());
        assertEquals(new BigDecimal("0.00"),total.remaining());
    }

    @Test
    void paidInvoicesHaveFullCollectionRate() {
        var totals=BillingMetricsService.fromInvoices(List.of(
            invoice("250.00","250.00","0.00",PaymentStatus.ODENILIB,false)));
        assertEquals(new BigDecimal("100.00"),totals.collectionRate());
        assertEquals(new BigDecimal("0.00"),totals.remaining());
    }
}
