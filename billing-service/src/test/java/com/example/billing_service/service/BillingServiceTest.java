package com.example.billing_service.service;

import com.example.billing_service.dto.request.InvoiceCreateRequest;
import com.example.billing_service.dto.response.ContractSummary;
import com.example.billing_service.dto.response.InvoiceDetailResponse;
import com.example.billing_service.dto.response.PageResponse;
import com.example.billing_service.dto.response.RevenueReportResponse;
import com.example.billing_service.dto.response.RoomSummary;
import com.example.billing_service.entity.BillInvoice;
import com.example.billing_service.entity.UtilityConfig;
import com.example.billing_service.exception.BadRequestException;
import com.example.billing_service.mapper.BillingMapper;
import com.example.billing_service.repository.BillInvoiceRepository;
import com.example.billing_service.repository.UtilityConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private BillInvoiceRepository billInvoiceRepository;
    @Mock
    private UtilityConfigRepository utilityConfigRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient contractServiceRestClient;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient propertyServiceRestClient;
    @Mock
    private BillingMapper billingMapper;

    private BillingService billingService;

    @BeforeEach
    void setUp() {
        billingService = new BillingService(
                billInvoiceRepository, utilityConfigRepository,
                contractServiceRestClient, propertyServiceRestClient, billingMapper);
    }

    @Test
    void createMonthlyInvoice_calculatesTotalsAndSaves() {
        InvoiceCreateRequest req = InvoiceCreateRequest.builder()
                .contractId(10L).month(11).year(2026)
                .oldElec(new BigDecimal("100")).newElec(new BigDecimal("150"))
                .waterUsageOrCost(new BigDecimal("5")).otherCost(new BigDecimal("50000"))
                .build();

        when(billInvoiceRepository.existsByContractIdAndMonthAndYear(10L, 11, 2026)).thenReturn(false);
        when(contractServiceRestClient.get()
                .uri("/api/v1/contracts/{contractId}", 10L)
                .retrieve()
                .body(ContractSummary.class))
                .thenReturn(new ContractSummary(10L, 5L, 2L, "ACTIVE"));
        when(propertyServiceRestClient.get()
                .uri("/api/v1/rooms/{id}", 5L)
                .retrieve()
                .body(RoomSummary.class))
                .thenReturn(new RoomSummary(5L, "P.201", new BigDecimal("3000000")));
        when(utilityConfigRepository.findByServiceCode("ELEC"))
                .thenReturn(Optional.of(UtilityConfig.builder()
                        .serviceCode("ELEC").unitPrice(new BigDecimal("3500")).unitType("KWH").isActive(true).build()));
        when(utilityConfigRepository.findByServiceCode("WATER"))
                .thenReturn(Optional.of(UtilityConfig.builder()
                        .serviceCode("WATER").unitPrice(new BigDecimal("20000")).unitType("M3").isActive(true).build()));
        when(billInvoiceRepository.save(any(BillInvoice.class))).thenAnswer(inv -> {
            BillInvoice b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        InvoiceDetailResponse res = billingService.createMonthlyInvoice(req);

        assertEquals(new BigDecimal("175000"), res.getElecCost());
        assertEquals(new BigDecimal("100000"), res.getWaterCost());
        assertEquals(new BigDecimal("3325000"), res.getTotalAmount());
        assertEquals("P.201", res.getRoomNumber());
        verify(billInvoiceRepository).save(any(BillInvoice.class));
    }

    @Test
    void createMonthlyInvoice_rejectsLowerNewElec() {
        InvoiceCreateRequest req = InvoiceCreateRequest.builder()
                .contractId(10L).month(11).year(2026)
                .oldElec(new BigDecimal("200")).newElec(new BigDecimal("150"))
                .waterUsageOrCost(BigDecimal.ONE)
                .build();

        assertThrows(BadRequestException.class, () -> billingService.createMonthlyInvoice(req));
        verifyNoInteractions(billInvoiceRepository);
    }

    @Test
    void getInvoicesForLandLord_appliesSortAndMapsPage() {
        BillInvoice invoice = BillInvoice.builder().id(1L).contractId(10L).month(11).year(2026).status("UNPAID").build();
        InvoiceDetailResponse dto = InvoiceDetailResponse.builder().id(1L).contractId(10L).month(11).year(2026).status("UNPAID").build();

        when(billInvoiceRepository.findAll(org.mockito.ArgumentMatchers.<Specification<BillInvoice>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(invoice)));
        when(billingMapper.invoicetoInvoiceDetailResponse(invoice)).thenReturn(dto);

        PageResponse<InvoiceDetailResponse> res = billingService.getInvoicesForLandLord(11, 2026, "unpaid", 0, 10, "month,desc");

        assertEquals(1, res.getContent().size());
        assertEquals(1L, res.getContent().get(0).getId());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(billInvoiceRepository).findAll(org.mockito.ArgumentMatchers.<Specification<BillInvoice>>any(), pageableCaptor.capture());
        Sort.Order order = pageableCaptor.getValue().getSort().getOrderFor("month");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());
    }

    @Test
    void getInvoicesForLandLord_handlesMalformedOrNullSort() {
        when(billInvoiceRepository.findAll(org.mockito.ArgumentMatchers.<Specification<BillInvoice>>any(), any(Pageable.class)))
                .thenReturn(Page.empty());

        billingService.getInvoicesForLandLord(null, null, null, 0, 10, ",");
        billingService.getInvoicesForLandLord(null, null, null, 0, 10, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(billInvoiceRepository, times(2))
                .findAll(org.mockito.ArgumentMatchers.<Specification<BillInvoice>>any(), pageableCaptor.capture());
        for (Pageable pageable : pageableCaptor.getAllValues()) {
            Sort.Order order = pageable.getSort().getOrderFor("id");
            assertNotNull(order);
            assertEquals(Sort.Direction.ASC, order.getDirection());
        }
    }

    @Test
    void getInvoicesForLandLord_rejectsInvalidFiltersAndSize() {
        assertThrows(BadRequestException.class,
                () -> billingService.getInvoicesForLandLord(null, 0, null, 0, 10, "id,asc"));
        assertThrows(BadRequestException.class,
                () -> billingService.getInvoicesForLandLord(null, null, null, 0, 101, "id,asc"));
        assertThrows(BadRequestException.class,
                () -> billingService.getInvoicesForLandLord(null, null, "BAD_STATUS", 0, 10, "id,asc"));
        verifyNoInteractions(billInvoiceRepository);
    }

    @Test
    void getInvoicesForTenant_returnsEmptyWhenNoContractsAndRejectsNullTenantId() {
        assertThrows(BadRequestException.class, () -> billingService.getInvoicesForTenant(null, 0, 10));

        when(contractServiceRestClient.get()
                .uri("/api/v1/contracts/my-contracts")
                .retrieve()
                .body(ContractSummary[].class))
                .thenReturn(new ContractSummary[0]);

        PageResponse<InvoiceDetailResponse> emptyRes = billingService.getInvoicesForTenant(5L, 0, 10);
        assertTrue(emptyRes.getContent().isEmpty());
        verifyNoInteractions(billInvoiceRepository);
    }

    @Test
    void getInvoicesForTenant_queriesByContractIds() {
        BillInvoice invoice = BillInvoice.builder().id(9L).contractId(10L).month(11).year(2026).build();
        InvoiceDetailResponse dto = InvoiceDetailResponse.builder().id(9L).contractId(10L).build();
        when(contractServiceRestClient.get()
                .uri("/api/v1/contracts/my-contracts")
                .retrieve()
                .body(ContractSummary[].class))
                .thenReturn(new ContractSummary[]{new ContractSummary(10L, 3L, 5L, "ACTIVE")});
        when(billInvoiceRepository.findByContractIdIn(eq(List.of(10L)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(invoice)));
        when(billingMapper.invoicetoInvoiceDetailResponse(invoice)).thenReturn(dto);

        PageResponse<InvoiceDetailResponse> res = billingService.getInvoicesForTenant(5L, 0, 10);
        assertEquals(1, res.getContent().size());
        assertEquals(9L, res.getContent().get(0).getId());
    }

    @Test
    void updateInvoiceStatus_marksPaidWithTimestampAndRejectsNonUnpaid() {
        BillInvoice invoice = BillInvoice.builder().id(1L).status("UNPAID").build();
        when(billInvoiceRepository.findById(1L)).thenReturn(Optional.of(invoice));
        when(billInvoiceRepository.save(invoice)).thenReturn(invoice);
        when(billingMapper.invoicetoInvoiceDetailResponse(invoice))
                .thenReturn(InvoiceDetailResponse.builder().id(1L).status("PAID").build());

        assertThrows(BadRequestException.class, () -> billingService.updateInvoiceStatus(1L, " "));
        assertThrows(BadRequestException.class, () -> billingService.updateInvoiceStatus(1L, "INVALID"));

        InvoiceDetailResponse res = billingService.updateInvoiceStatus(1L, "paid");
        assertEquals("PAID", invoice.getStatus());
        assertNotNull(invoice.getPaymentDate());
        assertEquals("PAID", res.getStatus());

        assertThrows(BadRequestException.class, () -> billingService.updateInvoiceStatus(1L, "CANCELLED"));
    }

    @Test
    void getRevenueReport_mapsColumnsCorrectly() {
        Object[] row = new Object[]{
                3L,
                new BigDecimal("9000000"),
                new BigDecimal("450000"),
                new BigDecimal("200000"),
                new BigDecimal("50000"),
                new BigDecimal("9700000")
        };
        when(billInvoiceRepository.getRevenueReportByMonthAndYear(11, 2026))
                .thenReturn(List.<Object[]>of(row));

        RevenueReportResponse report = billingService.getRevenueReport(11, 2026);
        assertEquals(3L, report.getTotalPaidInvoices());
        assertEquals(new BigDecimal("9000000"), report.getTotalRoomRevenue());
        assertEquals(new BigDecimal("450000"), report.getTotalElecRevenue());
        assertEquals(new BigDecimal("200000"), report.getTotalWaterRevenue());
        assertEquals(new BigDecimal("50000"), report.getTotalOtherRevenue());
        assertEquals(new BigDecimal("9700000"), report.getGrandTotalRevenue());
    }
}
