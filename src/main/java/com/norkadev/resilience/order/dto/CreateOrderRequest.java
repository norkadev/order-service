package com.norkadev.resilience.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos necesarios para crear una nueva orden")
public record CreateOrderRequest(
                @Schema(description = "Código identificador único del producto", example = "SKU-001") @NotBlank @Size(max = 64) String sku,
                @Schema(description = "Cantidad del producto a ordenar", example = "5", minimum = "1") @Min(1) @Max(1000) int quantity) {
}
