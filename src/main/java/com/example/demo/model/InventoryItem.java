package com.example.demo.model;

import lombok.Data;

import java.util.Date;

@Data
public class InventoryItem {
    private String id;
    private String name;
    private int quantity;
    private int minThreshold;
    private Date dateAdded;
}