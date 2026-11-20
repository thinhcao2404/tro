package com.example.payment_service.repository;

import com.example.payment_service.entity.PaymentTransaction;
import com.example.payment_service.enums.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long>,
        JpaSpecificationExecutor<PaymentTransaction> {

    Optional<PaymentTransaction> findByTransactionCode(String transactionCode);

    boolean existsByInvoiceIdAndStatus(Long invoiceId, PaymentStatus status);

    List<PaymentTransaction> findByInvoiceIdAndStatus(Long invoiceId, PaymentStatus status);
}
