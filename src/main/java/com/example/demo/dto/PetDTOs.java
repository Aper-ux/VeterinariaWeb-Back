package com.example.demo.dto;

import lombok.Data;

import java.util.Date;

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
        private int age;
    }

    @Data
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
    }

    @Data
    public static class MedicalRecordResponse {
        private String id;
        private Date date;
        private String diagnosis;
        private String treatment;
        private String veterinarianId;
    }
}