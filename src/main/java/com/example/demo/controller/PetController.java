package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.PetDTOs.*;
import com.example.demo.service.PetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pets")
public class PetController {

    @Autowired
    private PetService petService;

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PetResponse>> createPet(@RequestBody CreatePetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(petService.createPet(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('VETERINARIO') or @petService.isOwner(#id)")
    public ResponseEntity<ApiResponse<PetResponse>> updatePet(@PathVariable String id, @RequestBody UpdatePetRequest request) {
        return ResponseEntity.ok(ApiResponse.success(petService.updatePet(id, request)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('VETERINARIO') or hasRole('RECEPCIONISTA') or @petService.isOwner(#id)")
    public ResponseEntity<ApiResponse<PetResponse>> getPetById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(petService.getPetById(id)));
    }

    @GetMapping("/{id}/medical-history")
    @PreAuthorize("hasRole('VETERINARIO') or @petService.isOwner(#id)")
    public ResponseEntity<ApiResponse<List<MedicalRecordResponse>>> getPetMedicalHistory(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(petService.getPetMedicalHistory(id)));
    }

    @PostMapping("/{id}/medical-record")
    @PreAuthorize("hasRole('VETERINARIO')")
    public ResponseEntity<ApiResponse<MedicalRecordResponse>> addMedicalRecord(@PathVariable String id, @RequestBody AddMedicalRecordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(petService.addMedicalRecord(id, request)));
    }
}