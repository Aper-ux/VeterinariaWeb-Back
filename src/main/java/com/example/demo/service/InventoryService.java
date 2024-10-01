package com.example.demo.service;

import com.example.demo.dto.InventoryDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.InventoryItem;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class InventoryService {

    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    public List<InventoryItemResponse> getAllItems() {
        try {
            List<InventoryItemResponse> items = new ArrayList<>();
            getFirestore().collection("inventory").get().get().getDocuments().forEach(doc -> {
                InventoryItem item = doc.toObject(InventoryItem.class);
                items.add(convertToInventoryItemResponse(item));
            });
            return items;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching inventory items: " + e.getMessage());
        }
    }

    public InventoryItemResponse addItem(AddInventoryItemRequest request) {
        InventoryItem item = new InventoryItem();
        item.setId(UUID.randomUUID().toString());
        item.setName(request.getName());
        item.setQuantity(request.getQuantity());
        item.setMinThreshold(request.getMinThreshold());
        item.setDateAdded(new Date());

        try {
            getFirestore().collection("inventory").document(item.getId()).set(item).get();
            return convertToInventoryItemResponse(item);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error adding inventory item: " + e.getMessage());
        }
    }

    public InventoryItemResponse updateItem(String id, UpdateInventoryItemRequest request) {
        try {
            InventoryItem item = getFirestore().collection("inventory").document(id).get().get().toObject(InventoryItem.class);
            if (item == null) {
                throw new CustomExceptions.NotFoundException("Inventory item not found with id: " + id);
            }
            item.setQuantity(request.getQuantity());
            item.setMinThreshold(request.getMinThreshold());

            getFirestore().collection("inventory").document(id).set(item).get();
            return convertToInventoryItemResponse(item);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating inventory item: " + e.getMessage());
        }
    }

    public List<InventoryItemResponse> getLowStockItems() {
        try {
            List<InventoryItemResponse> lowStockItems = new ArrayList<>();
            getFirestore().collection("inventory").get().get().getDocuments().forEach(doc -> {
                InventoryItem item = doc.toObject(InventoryItem.class);
                if (item.getQuantity() <= item.getMinThreshold()) {
                    lowStockItems.add(convertToInventoryItemResponse(item));
                }
            });
            return lowStockItems;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching low stock items: " + e.getMessage());
        }
    }

    private InventoryItemResponse convertToInventoryItemResponse(InventoryItem item) {
        InventoryItemResponse response = new InventoryItemResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setQuantity(item.getQuantity());
        response.setMinThreshold(item.getMinThreshold());
        response.setDateAdded(item.getDateAdded());
        return response;
    }
}