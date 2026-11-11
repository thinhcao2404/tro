package com.example.billing_service.service;

import com.example.billing_service.dto.ContractCreatedEvent;
import com.example.billing_service.dto.request.InvoiceCreateRequest;
import com.example.billing_service.dto.response.*;
import com.example.billing_service.entity.BillInvoice;
import com.example.billing_service.entity.UtilityConfig;
import com.example.billing_service.enums.InvoiceStatus;
import com.example.billing_service.exception.BadRequestException;
import com.example.billing_service.exception.ExternalServiceException;
import com.example.billing_service.exception.ResourceNotFoundException;
import com.example.billing_service.exception.ServiceUnavailableException;
import com.example.billing_service.mapper.BillingMapper;
import com.example.billing_service.repository.BillInvoiceRepository;
import com.example.billing_service.repository.UtilityConfigRepository;
import com.example.billing_service.util.PageResponseUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BillingService {
    private final BillInvoiceRepository billInvoiceRepository;
    private final UtilityConfigRepository utilityConfigRepository;
    private final RestClient contractServiceRestClient;
    private final RestClient propertyServiceRestClient;
    private final BillingMapper  billingMapper;

    public InvoiceDetailResponse getTotalAmount(Long billingId) {
        BillInvoice bill = billInvoiceRepository.findById(billingId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn."));
        return billingMapper.invoicetoInvoiceDetailResponse(bill);
    }
    @Transactional
    public void createDepositInvoice(ContractCreatedEvent event) {
        if (event == null || event.getContractId() == null) {
            throw new IllegalArgumentException("ContractCreatedEvent thiếu contractId");
        }
        if (event.getDeposit() != null && event.getDeposit() < 0) {
            throw new IllegalArgumentException("Tiền cọc không được âm");
        }

        // Quy ước month = 0 là hóa đơn cọc ban đầu
        if (billInvoiceRepository.existsByContractIdAndMonth(event.getContractId(), 0)) {
            return;
        }

        BigDecimal deposit = event.getDeposit() != null
                ? BigDecimal.valueOf(event.getDeposit())
                : BigDecimal.ZERO;

        BillInvoice depositInvoice = BillInvoice.builder()
                .contractId(event.getContractId())
                .month(0)
                .year(LocalDate.now().getYear())
                .oldElec(BigDecimal.ZERO)
                .newElec(BigDecimal.ZERO)
                .elecCost(BigDecimal.ZERO)
                .waterCost(BigDecimal.ZERO)
                .roomCost(deposit)
                .otherCost(BigDecimal.ZERO)
                .totalAmount(deposit)
                .status("UNPAID")
                .build();

        billInvoiceRepository.save(depositInvoice);
    }


    @Transactional
    public InvoiceDetailResponse createMonthlyInvoice(InvoiceCreateRequest request) {
        if (request.getNewElec().compareTo(request.getOldElec()) < 0) {
            throw new BadRequestException("Số điện mới không thể nhỏ hơn số cũ.");
        }
        BigDecimal otherCost = request.getOtherCost() != null ? request.getOtherCost() : BigDecimal.ZERO;
        if (otherCost.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Chi phí khác không được âm.");
        }
        if (billInvoiceRepository.existsByContractIdAndMonthAndYear(
                request.getContractId(), request.getMonth(), request.getYear())) {
            throw new BadRequestException("Tháng này đã lập hóa đơn.");
        }

        ContractSummary contract = fetchExternal(
                contractServiceRestClient, "/api/v1/contracts/{contractId}",
                ContractSummary.class, "hợp đồng ID: " + request.getContractId(), "contract-service",
                request.getContractId());
        if (!"ACTIVE".equalsIgnoreCase(contract.status())) {
            throw new BadRequestException("Hợp đồng không ở trạng thái ACTIVE.");
        }

        RoomSummary room = fetchExternal(
                propertyServiceRestClient,
                "/api/v1/rooms/{id}",
                RoomSummary.class,
                "phòng ID: " + contract.roomId(),
                "property-service",
                contract.roomId());
        if (room.price() == null) {
            throw new ExternalServiceException("Không lấy được giá phòng từ property-service.");
        }

        UtilityConfig elecConfig = getActiveUtility("ELEC", "điện");
        UtilityConfig waterConfig = getActiveUtility("WATER", "nước");

        BigDecimal elecCost = request.getNewElec()
                .subtract(request.getOldElec())
                .multiply(elecConfig.getUnitPrice());
        BigDecimal waterCost = "FIXED".equalsIgnoreCase(waterConfig.getUnitType())
                ? waterConfig.getUnitPrice()
                : request.getWaterUsageOrCost().multiply(waterConfig.getUnitPrice());
        BigDecimal roomCost = room.price();
        BigDecimal totalAmount = elecCost.add(waterCost).add(roomCost).add(otherCost);

        BillInvoice saved = billInvoiceRepository.save(BillInvoice.builder()
                .contractId(request.getContractId())
                .roomNumber(room.roomNumber())
                .month(request.getMonth())
                .year(request.getYear())
                .oldElec(request.getOldElec())
                .newElec(request.getNewElec())
                .elecCost(elecCost)
                .waterCost(waterCost)
                .roomCost(roomCost)
                .otherCost(otherCost)
                .totalAmount(totalAmount)
                .status("UNPAID")
                .build());

        return InvoiceDetailResponse.builder()
                .id(saved.getId())
                .contractId(saved.getContractId())
                .roomNumber(room.roomNumber())
                .month(saved.getMonth())
                .year(saved.getYear())
                .oldElec(saved.getOldElec())
                .newElec(saved.getNewElec())
                .elecCost(saved.getElecCost())
                .waterCost(saved.getWaterCost())
                .roomCost(saved.getRoomCost())
                .otherCost(saved.getOtherCost())
                .totalAmount(saved.getTotalAmount())
                .status(saved.getStatus())
                .paymentDate(saved.getPaymentDate())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    private UtilityConfig getActiveUtility(String serviceCode, String label) {
        return utilityConfigRepository.findByServiceCode(serviceCode)
                .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
                .orElseThrow(() -> new BadRequestException(
                        "Chưa cấu hình đơn giá " + label + " (" + serviceCode + ") đang hoạt động."));
    }

    private <T> T fetchExternal(
            RestClient client, String uri, Class<T> type, String entityLabel, String serviceName, Object... uriVars) {
        try {
            T result = client.get().uri(uri, uriVars).retrieve().body(type);
            if (result == null) {
                throw new ResourceNotFoundException("Không tìm thấy " + entityLabel);
            }
            return result;
        } catch (ResourceAccessException e) {
            throw new ServiceUnavailableException(serviceName + " hiện không khả dụng.");
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResourceNotFoundException("Không tìm thấy " + entityLabel);
            }
            throw new ExternalServiceException("Lỗi từ " + serviceName + ": " + e.getStatusCode().value());
        }
    }

    public PageResponse<InvoiceDetailResponse> getInvoicesForLandLord(
            Integer month,
            Integer year,
            String status,
            int page,
            int size,
            String sort) {

        validatePagination(page, size);

        // Xây dựng điều kiện lọc
        Specification<BillInvoice> spec = (root, query, cb) ->
                cb.conjunction();

        if (month != null) {
            if (month < 0 || month > 12) throw new BadRequestException("Tháng phải nằm trong khoảng 0 đến 12.");
            spec = spec.and((root, query, cb) -> cb.equal(root.get("month"), month));
        }
        if (year != null) {
            if (year < 2020) throw new BadRequestException("Năm phải >= 2020.");
            spec = spec.and((root, query, cb) -> cb.equal(root.get("year"), year));
        }

        if (status != null && !status.isBlank()) {
            try {
                InvoiceStatus enumStatus = InvoiceStatus.valueOf(status.trim().toUpperCase());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), enumStatus.name()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Trạng thái hóa đơn '" + status + "' không hợp lệ.");
            }
        }

        // Xác định trường sắp xếp
        String[] sortParams = (sort == null || sort.isBlank()) ? new String[0] : sort.split(",");
        String rawField = sortParams.length > 0 ? sortParams[0].trim() : "id";

        // Chỉ cho phép các trường được hỗ trợ
        String sortField = switch (rawField) {
            case "id", "month", "year", "status" -> rawField;
            default -> "id";
        };

        Sort.Direction direction =
                sortParams.length > 1
                        && "desc".equalsIgnoreCase(sortParams[1].trim())
                        ? Sort.Direction.DESC
                        : Sort.Direction.ASC;
        Sort sorting = Sort.by(direction, sortField);

        // Tạo thông tin phân trang
        Pageable pageable = PageRequest.of(page, size, sorting);

        // Database lọc, sắp xếp và phân trang
        Page<BillInvoice> billPage = billInvoiceRepository.findAll(spec, pageable);
        Page<InvoiceDetailResponse> responsePage = billPage.map(billingMapper::invoicetoInvoiceDetailResponse);
        return PageResponseUtil.from(responsePage);
    }

    public PageResponse<InvoiceDetailResponse> getInvoicesForTenant(Long tenantId, int page, int size) {
        if (tenantId == null) {
            throw new BadRequestException("Thiếu thông tin tenantId.");
        }
        validatePagination(page, size);

        List<Long> contractIds = fetchCurrentTenantContractIds();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "year", "month", "id"));
        if (contractIds.isEmpty()) {
            return PageResponseUtil.from(Page.empty(pageable));
        }

        Page<BillInvoice> billPage = billInvoiceRepository.findByContractIdIn(contractIds, pageable);
        Page<InvoiceDetailResponse> responsePage = billPage.map(billingMapper::invoicetoInvoiceDetailResponse);
        return PageResponseUtil.from(responsePage);
    }

    private List<Long> fetchCurrentTenantContractIds() {
        ContractSummary[] contracts = fetchExternal(
                contractServiceRestClient, "/api/v1/contracts/my-contracts",
                ContractSummary[].class, "danh sách hợp đồng", "contract-service");
        return Arrays.stream(contracts)
                .map(ContractSummary::id)
                .filter(Objects::nonNull)
                .toList();
    }

    private void validatePagination(int page, int size) {
        if (page < 0 || size <= 0 || size > 100) {
            throw new BadRequestException("Page phải >= 0 và size phải trong khoảng 1..100.");
        }
    }

    public RevenueReportResponse getRevenueReport(Integer month, Integer year) {
        int targetMonth = (month != null) ? month : LocalDate.now().getMonthValue();
        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        if (targetYear < 2020) {
            throw new BadRequestException("Năm không hợp lệ.");
        }
        if  (targetMonth < 0 || targetMonth > 12) {
            throw new BadRequestException("Tháng không hợp lệ.");
        }
        List<Object[]> rows = billInvoiceRepository.getRevenueReportByMonthAndYear(targetMonth, targetYear);
        Object[] row = rows.isEmpty() ? new Object[6] : rows.get(0);

        long totalPaidInvoices = (row[0] instanceof Number n) ? n.longValue() : 0L;
        BigDecimal roomRev  = toBigDecimal(row[1]);
        BigDecimal elecRev  = toBigDecimal(row[2]);
        BigDecimal waterRev = toBigDecimal(row[3]);
        BigDecimal otherRev = toBigDecimal(row[4]);
        BigDecimal grandRev = toBigDecimal(row[5]);

        return RevenueReportResponse.builder()
                                    .month(targetMonth)
                                    .year(targetYear)
                                    .grandTotalRevenue(grandRev)
                                    .totalElecRevenue(elecRev)
                                    .totalWaterRevenue(waterRev)
                                    .totalOtherRevenue(otherRev)
                                    .totalRoomRevenue(roomRev)
                                    .totalPaidInvoices(totalPaidInvoices)
                                    .build();
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) return bd;
        if (value instanceof Number n) return BigDecimal.valueOf(n.longValue());
        return BigDecimal.ZERO;
    }

    @Transactional
    public InvoiceDetailResponse updateInvoiceStatus(Long id, String status) {
        BillInvoice billInvoice = billInvoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy hóa đơn."));
        if (!"UNPAID".equalsIgnoreCase(billInvoice.getStatus())) {
            throw new BadRequestException("Chỉ có thể cập nhật hóa đơn đang ở trạng thái UNPAID.");
        }
        if (status == null || status.isBlank()) {
            throw new BadRequestException("Trạng thái hóa đơn không được để trống.");
        }
        try {
            InvoiceStatus invoiceStatus = InvoiceStatus.valueOf(status.trim().toUpperCase());
            billInvoice.setStatus(invoiceStatus.name());
            if (invoiceStatus == InvoiceStatus.PAID) {
                billInvoice.setPaymentDate(LocalDateTime.now());
            }
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Trạng thái hóa đơn '" + status + "' không hợp lệ.");
        }
        BillInvoice saved = billInvoiceRepository.save(billInvoice);

        return billingMapper.invoicetoInvoiceDetailResponse(saved);
    }
}
