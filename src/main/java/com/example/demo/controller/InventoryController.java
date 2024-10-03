package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.InventoryDTOs.*;
import com.example.demo.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @GetMapping
    @PreAuthorize("hasRole('VETERINARIO') or hasRole('RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getAllItems() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllItems()));
    }

    @PostMapping
    @PreAuthorize("hasRole('VETERINARIO')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> addItem(@RequestBody AddInventoryItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.addItem(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VETERINARIO')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> updateItem(@PathVariable String id, @RequestBody UpdateInventoryItemRequest request) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.updateItem(id, request)));
    }

    @GetMapping("/low-stock")
    @PreAuthorize("hasRole('VETERINARIO') or hasRole('RECEPCIONISTA')")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getLowStockItems() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getLowStockItems()));
    }
}