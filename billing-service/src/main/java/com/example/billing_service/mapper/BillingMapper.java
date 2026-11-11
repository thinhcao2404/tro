package com.example.billing_service.mapper;
import com.example.billing_service.dto.response.InvoiceDetailResponse;
import com.example.billing_service.entity.BillInvoice;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface BillingMapper {
    InvoiceDetailResponse invoicetoInvoiceDetailResponse(BillInvoice invoice);
}
