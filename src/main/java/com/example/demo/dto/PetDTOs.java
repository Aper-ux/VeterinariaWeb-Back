package com.example.demo.dto;

import lombok.*;

import java.util.Date;
import java.util.List;

public class PetDTOs {

    @Data
    public static class CreatePetRequest {
        private String name;
        private String species;
        private String breed;
        private int age;
    }

    @Data
    public static class UpdatePetRequest {
        private String name;
        private String species;
        private String breed;
        private int age;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PetResponse {
        private String id;
        private String name;
        private String species;
        private String breed;
        private int age;
        private String ownerId;
    }

    @Data
    public static class AddMedicalRecordRequest {
        private String diagnosis;
        private String treatment;
        private String notes;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class MedicalRecordResponse {
        private String id;
        private Date date;
        private String diagnosis;
        private String treatment;
        private String notes;
        private String veterinarianId;
        private String veterinarianName;
        // Getters y Setters para petName
        private String petName;
        private String petId;
        private String species;
        private String breed;
        private int age;
        private String ownerId;


    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PetWithHistoryDTO {
        private String id;
        private String name;
        private String species;
        private String breed;
        private int age;
        private List<MedicalRecordResponse> medicalHistory;
    }
}
