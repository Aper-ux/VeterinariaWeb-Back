package com.example.demo.service;

import com.example.demo.dto.AuthDTOs.RegisterRequest;
import com.example.demo.dto.UserDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class UserService {

    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }
    public List<Map<String, Object>> getUserNotes(String userId) {
        try {
            List<Map<String, Object>> notes = new ArrayList<>();
            getFirestore().collection("users").document(userId).collection("notes").get().get()
                    .getDocuments().forEach(doc -> notes.add(doc.getData()));
            return notes;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching user notes: " + e.getMessage());
        }
    }

    public UserResponse createUser(String uid, RegisterRequest request) {
        User user = new User();
        user.setUid(uid);
        user.setEmail(request.getEmail());
        user.setNombre(request.getNombre());
        user.setApellido(request.getApellido());
        user.setTelefono(request.getTelefono());
        user.setDireccion(request.getDireccion());

        List<Role> roles = Optional.ofNullable(request.getRoles())
                .map(roleList -> roleList.stream()
                        .filter(Objects::nonNull)
                        .map(Object::toString)
                        .map(roleString -> {
                            try {
                                return Role.valueOf(roleString.toUpperCase());
                            } catch (IllegalArgumentException e) {
                                System.out.println("Invalid role: " + roleString);
                                return null;
                            }
                        })
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList()))
                .orElse(Collections.singletonList(Role.CLIENTE));

        user.setRoles(roles);
        user.setEnabled(true);
        user.setActive(true);

        try {
            getFirestore().collection("users").document(uid).set(user).get();
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error saving user to Firestore: " + e.getMessage());
        }
    }

    public UserResponse getUserById(String id) {
        try {
            User user = getFirestore().collection("users").document(id).get().get().toObject(User.class);
            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + id);
            }
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching user: " + e.getMessage());
        }
    }

    public List<UserResponse> getAllUsers() {
        try {
            List<UserResponse> users = new ArrayList<>();
            getFirestore().collection("users").get().get().getDocuments().forEach(doc -> {
                User user = doc.toObject(User.class);
                users.add(convertToUserResponse(user));
            });
            return users;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching users: " + e.getMessage());
        }
    }

    public UserResponse updateUser(String id, UpdateUserRequest request) {
        try {
            User user = getFirestore().collection("users").document(id).get().get().toObject(User.class);
            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + id);
            }
            user.setNombre(request.getNombre());
            user.setApellido(request.getApellido());
            user.setTelefono(request.getTelefono());
            user.setDireccion(request.getDireccion());

            getFirestore().collection("users").document(id).set(user).get();
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating user: " + e.getMessage());
        }
    }

    public void deleteUser(String id) {
        try {
            FirebaseAuth.getInstance().deleteUser(id);
            getFirestore().collection("users").document(id).delete().get();
        } catch (FirebaseAuthException | InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error deleting user: " + e.getMessage());
        }
    }

    public void changePassword(String id, ChangePasswordRequest request) {
        try {
            FirebaseAuth.getInstance().updateUser(new UserRecord.UpdateRequest(id)
                    .setPassword(request.getNewPassword()));
        } catch (FirebaseAuthException e) {
            throw new CustomExceptions.ProcessingException("Error changing password: " + e.getMessage());
        }
    }

    public UserResponse toggleUserStatus(String id, ToggleUserStatusRequest request) {
        try {
            User user = getFirestore().collection("users").document(id).get().get().toObject(User.class);
            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + id);
            }
            user.setActive(request.isActive());
            getFirestore().collection("users").document(id).set(user).get();
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error toggling user status: " + e.getMessage());
        }
    }

    public void addNote(String userId, AddNoteRequest request) {
        try {
            Map<String, Object> note = new HashMap<>();
            note.put("content", request.getContent());
            note.put("timestamp", new Date());

            getFirestore().collection("users").document(userId).collection("notes").add(note).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error adding note: " + e.getMessage());
        }
    }

    private UserResponse convertToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setUid(user.getUid());
        response.setEmail(user.getEmail());
        response.setNombre(user.getNombre());
        response.setApellido(user.getApellido());
        response.setTelefono(user.getTelefono());
        response.setDireccion(user.getDireccion());
        response.setRoles(user.getRoles());
        response.setEnabled(user.isEnabled());
        response.setActive(user.isActive());
        return response;
    }

    public void updateUserProfile(UpdateProfileRequest request, String uid) {
        try {

            User user = getFirestore().collection("users").document(uid).get().get().toObject(User.class);
            if (user == null) {
                throw new CustomExceptions.UserNotFoundException("User not found with id: " + uid);
            }
            user.setNombre(request.getNombre());
            user.setApellido(request.getApellido());
            user.setTelefono(request.getTelefono());
            user.setDireccion(request.getDireccion());
            getFirestore().collection("users").document(uid).set(user).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating user profile: " + e.getMessage());
        }
    }
}