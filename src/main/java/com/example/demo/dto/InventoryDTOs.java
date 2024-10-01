package com.example.demo.dto;

import lombok.Data;

import java.util.Date;

public class InventoryDTOs {

    @Data
    public static class AddInventoryItemRequest {
        private String name;
        private int quantity;
        private int minThreshold;
    }

    @Data
    public static class UpdateInventoryItemRequest {
        private int quantity;
        private int minThreshold;
    }

    @Data
    public static class InventoryItemResponse {
        private String id;
        private String name;
        private int quantity;
        private int minThreshold;
        private Date dateAdded;
    }
}