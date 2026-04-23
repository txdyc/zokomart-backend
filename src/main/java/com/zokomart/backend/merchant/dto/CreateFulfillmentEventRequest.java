package com.zokomart.backend.merchant.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateFulfillmentEventRequest(
        @NotBlank String status,
        String notes
) {
}
