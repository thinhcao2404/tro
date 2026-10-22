package org.acme.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateContractRequest {
    private Long roomId;
    private Long tenantId;
    private Double deposit;
    private String startDate;
    private String endDate;
}
