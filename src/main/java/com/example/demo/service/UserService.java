package com.example.demo.service;

import com.example.demo.dto.AuthDTOs.RegisterRequest;
import com.example.demo.dto.PetDTOs;
import com.example.demo.dto.UserDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.UserRecord;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class UserService {
    @Autowired
    PetService petService;

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

//    public UserResponse createUser(RegisterRequest request) {
//        try {
//            UserRecord.CreateRequest createRequest = new UserRecord.CreateRequest()
//                    .setEmail(request.getEmail())
//                    .setPassword(request.getPassword())
//                    .setDisplayName(request.getNombre() + " " + request.getApellido());
//
//            UserRecord userRecord = FirebaseAuth.getInstance().createUser(createRequest);
//
//            User user = new User();
//            user.setUid(userRecord.getUid());
//            user.setEmail(request.getEmail());
//            user.setNombre(request.getNombre());
//            user.setApellido(request.getApellido());
//            user.setTelefono(request.getTelefono());
//            user.setDireccion(request.getDireccion());
//            user.setRoles(request.getRoles());
//            user.setEnabled(true);
//            user.setActive(true);
//
//            getFirestore().collection("users").document(userRecord.getUid()).set(user).get();
//            return convertToUserResponse(user);
//        } catch (FirebaseAuthException | InterruptedException | ExecutionException e) {
//            throw new CustomExceptions.ProcessingException("Error creating user: " + e.getMessage());
//        }
//    }
//
    public UserResponse createUser(RegisterRequest request) {
        try {
            // Primero, verifica si el usuario ya existe en Firestore
            Optional<User> existingUser = findByEmail(request.getEmail());
            if (existingUser.isPresent()) {
                // Si el usuario ya existe, actualiza sus datos y devuelve la respuesta
                User user = existingUser.get();
                updateUserData(user, request);
                return convertToUserResponse(user);
            }

            // Si el usuario no existe, crea uno nuevo
            User newUser = new User();
            newUser.setUid(request.getUid()); // Esto debería ser el UID de Firebase
            newUser.setEmail(request.getEmail());
            newUser.setNombre(request.getNombre());
            newUser.setApellido(request.getApellido());
            newUser.setTelefono(request.getTelefono());
            newUser.setDireccion(request.getDireccion());
            newUser.setRoles(request.getRoles());
            newUser.setEnabled(true);
            newUser.setActive(true);

            // Guarda el nuevo usuario en Firestore
            getFirestore().collection("users").document(newUser.getUid()).set(newUser).get();

            return convertToUserResponse(newUser);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error creating user: " + e.getMessage());
        }
    }
    private void updateUserData(User user, RegisterRequest request) {
        user.setNombre(request.getNombre());
        user.setApellido(request.getApellido());
        user.setTelefono(request.getTelefono());
        user.setDireccion(request.getDireccion());
        user.setRoles(request.getRoles());
        // Actualiza el usuario en Firestore
        try {
            getFirestore().collection("users").document(user.getUid()).set(user).get();
        } catch (InterruptedException e) {
            System.err.println(e);
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            System.err.println(e);
            throw new RuntimeException(e);
        }
    }
    public Optional<User> findByEmail(String email) {
        try {
            QuerySnapshot querySnapshot = getFirestore().collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .get();
            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot document = querySnapshot.getDocuments().get(0);
                return Optional.of(document.toObject(User.class));
            }
            return Optional.empty();
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error finding user by email: " + e.getMessage());
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

    public List<UserResponse> getAllUsers(Boolean isActive, String role) {
        try {
            List<UserResponse> users = new ArrayList<>();
            getFirestore().collection("users").get().get().getDocuments().forEach(doc -> {
                User user = doc.toObject(User.class);
                if ((isActive == null || user.isActive() == isActive) &&
                        (role == null || user.getRoles().contains(Role.valueOf(role.toUpperCase())))) {
                    users.add(convertToUserResponse(user));
                }
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
    /*
    public UserResponse updateUserProfile(UpdateProfileRequest request) {
        String uid = getCurrentUserUid();
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
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating user profile: " + e.getMessage());
        }
    }

     */
    public boolean existsByEmail(String email) {
        try {
            return getFirestore().collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .get()
                    .size() > 0;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error checking user existence: " + e.getMessage());
        }
    }

    private String getCurrentUserUid() {
        // Obtener el contexto de seguridad actual
        SecurityContext securityContext = SecurityContextHolder.getContext();
        Authentication authentication = securityContext.getAuthentication();

        // Verificar si hay un usuario autenticado
        if (authentication != null && authentication.isAuthenticated()) {
            // El nombre principal en este caso será el UID de Firebase
            return authentication.getName();
        }

        // Si no hay usuario autenticado, lanzar una excepción
        throw new CustomExceptions.AuthenticationException("No authenticated user found");
    }
    public UserResponse updateUserProfile(UpdateProfileRequest request) {
        String uid = getCurrentUserUid();
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
            return convertToUserResponse(user);
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating user profile: " + e.getMessage());
        }
    }
    public List<PetDTOs.PetResponse> getUserPets(String userId) {
        return petService.getPetsByUserId(userId);
    }
}