package com.example.billing_service.controller;

import com.example.billing_service.dto.InvoiceDTO;
import com.example.billing_service.listener.ContractEventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getAllInvoices() {
        return ResponseEntity.ok(new ArrayList<>(ContractEventListener.invoiceDb.values()));
    }
}