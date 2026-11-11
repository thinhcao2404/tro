package com.example.billing_service.repository;

import com.example.billing_service.entity.BillInvoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillInvoiceRepository extends JpaRepository<BillInvoice, Long>, JpaSpecificationExecutor<BillInvoice> {

    boolean existsByContractIdAndMonthAndYear(Long contractId, Integer month, Integer year);

    boolean existsByContractIdAndMonth(Long contractId, Integer month);


    Page<BillInvoice> findByContractIdIn(List<Long> contractIds, Pageable pageable);


    @Query("SELECT COUNT(b), " +
           "COALESCE(SUM(b.roomCost), 0), " +
           "COALESCE(SUM(b.elecCost), 0), " +
           "COALESCE(SUM(b.waterCost), 0), " +
           "COALESCE(SUM(b.otherCost), 0), " +
           "COALESCE(SUM(b.totalAmount), 0) " +
           "FROM BillInvoice b " +
           "WHERE b.status = 'PAID' AND b.month = :month AND b.year = :year")
    List<Object[]> getRevenueReportByMonthAndYear(@Param("month") Integer month, @Param("year") Integer year);
}
