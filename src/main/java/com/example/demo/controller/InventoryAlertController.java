package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.InventoryDTOs;
import com.example.demo.service.InventoryAlertService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryAlertController {

    @Autowired
    private InventoryAlertService alertService;

    @GetMapping("/low-stock")
    @PreAuthorize("hasPermission(null, 'STOCK_ALERT')")
    public ResponseEntity<ApiResponse<List<InventoryDTOs.LowStockAlertDTO>>> getLowStockAlerts() {
        return ResponseEntity.ok(ApiResponse.success(alertService.getLowStockAlerts()));
    }

    @PostMapping("/{productId}/threshold")
    @PreAuthorize("hasPermission(null, 'STOCK_ALERT')")
    public ResponseEntity<ApiResponse<InventoryDTOs.InventoryItemResponse>> updateThreshold(
            @PathVariable String productId,
            @Valid @RequestBody InventoryDTOs.UpdateThresholdRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                alertService.updateThreshold(productId, request.getMinThreshold())));
    }

    @PostMapping("/restock-orders")
    @PreAuthorize("hasPermission(null, 'STOCK_ALERT')")
    public ResponseEntity<ApiResponse<InventoryDTOs.RestockOrderDTO>> createRestockOrder(
            @Valid @RequestBody InventoryDTOs.CreateRestockOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                alertService.createRestockOrder(request)));
    }
}