package com.example.demo.service;

import com.example.demo.dto.PetDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.Pet;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class PetService {


    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    public PetResponse createPet(CreatePetRequest createPetRequest) {
        // Get the current user's ID for pet ownership association
        String currentUserUid = getCurrentUserUid();

        // Create a new Pet object and populate it with data from the request
        Pet newPet = new Pet();
        // The ID will be generated by Firestore
        newPet.setName(createPetRequest.getName());
        newPet.setSpecies(createPetRequest.getSpecies());
        newPet.setBreed(createPetRequest.getBreed());
        newPet.setAge(createPetRequest.getAge());
        newPet.setOwnerId(currentUserUid);


        // Attempt to save the new pet to Firestore
        try {
            String petId = savePetToFirestore(newPet);
            newPet.setId(petId); // Set the ID generated by Firestore
            return convertToPetResponse(newPet);
        } catch (InterruptedException | ExecutionException firestoreException) {
            String errorMessage = "Error occurred while creating pet: " + firestoreException.getMessage();
            throw new CustomExceptions.ProcessingException(errorMessage);
        }
    }
    private String savePetToFirestore(Pet petToSave) throws InterruptedException, ExecutionException {
        // Add the pet to Firestore and get the auto-generated ID
        return getFirestore().collection("pets").add(petToSave).get().getId();
    }

    public PetResponse updatePet(String id, UpdatePetRequest request) {
        try {
            PetResponse pet = getPetById(id);
            pet.setName(request.getName());
            pet.setAge(request.getAge());

            getFirestore().collection("pets").document(id).set(convertToPet(pet)).get();
            return pet;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating pet: " + e.getMessage());
        }
    }

    public PetResponse getPetById(String id) {
        try {
            Pet pet = getFirestore().collection("pets").document(id).get().get().toObject(Pet.class);
            if (pet == null) {
                throw new CustomExceptions.NotFoundException("Pet not found with id: " + id);
            }
            return convertToPetResponse(pet);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching pet: " + e.getMessage());
        }
    }

    public List<PetResponse> getPetsByUserId(String userId) {
        try {
            List<PetResponse> pets = new ArrayList<>();
            getFirestore().collection("pets").whereEqualTo("ownerId", userId).get().get().getDocuments().forEach(doc -> {
                Pet pet = doc.toObject(Pet.class);
                pets.add(convertToPetResponse(pet));
            });
            return pets;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching user's pets: " + e.getMessage());
        }
    }

    public List<MedicalRecordResponse> getPetMedicalHistory(String petId) {
        try {
            List<MedicalRecordResponse> medicalRecords = new ArrayList<>();
            getFirestore().collection("pets").document(petId).collection("medicalRecords").get().get().getDocuments().forEach(doc -> {
                MedicalRecordResponse record = doc.toObject(MedicalRecordResponse.class);
                medicalRecords.add(record);
            });
            return medicalRecords;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching pet's medical history: " + e.getMessage());
        }
    }

    public MedicalRecordResponse addMedicalRecord(String petId, AddMedicalRecordRequest request) {
        try {
            MedicalRecordResponse record = new MedicalRecordResponse();
            record.setId(UUID.randomUUID().toString());
            record.setDate(new Date());
            record.setDiagnosis(request.getDiagnosis());
            record.setTreatment(request.getTreatment());
            record.setVeterinarianId(getCurrentUserUid());

            getFirestore().collection("pets").document(petId).collection("medicalRecords").document(record.getId()).set(record).get();
            return record;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error adding medical record: " + e.getMessage());
        }
    }

    public boolean isOwner(String petId) {
        String currentUserId = getCurrentUserUid();
        try {
            Pet pet = getFirestore().collection("pets").document(petId).get().get().toObject(Pet.class);
            return pet != null && pet.getOwnerId().equals(currentUserId);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error checking pet ownership: " + e.getMessage());
        }
    }

    private String getCurrentUserUid() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }

    private PetResponse convertToPetResponse(Pet pet) {
        PetResponse response = new PetResponse();
        response.setId(pet.getId());
        response.setName(pet.getName());
        response.setSpecies(pet.getSpecies());
        response.setBreed(pet.getBreed());
        response.setAge(pet.getAge());
        response.setOwnerId(pet.getOwnerId());
        return response;
    }
    private Pet convertToPet(PetResponse petResponse) {
        Pet pet = new Pet();
        pet.setId(petResponse.getId());
        pet.setName(petResponse.getName());
        pet.setAge(petResponse.getAge());
        // Set other fields as necessary
        return pet;
    }
}