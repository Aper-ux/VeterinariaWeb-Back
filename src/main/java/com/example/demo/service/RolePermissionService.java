package com.example.demo.service;

import com.example.demo.dto.UserDTOs;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.security.CustomUserDetails;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class RolePermissionService {
    private Firestore getFirestore() {
        return FirestoreClient.getFirestore();
    }

    private final Map<Role, List<String>> rolePermissions;

    public RolePermissionService() {
        rolePermissions = new HashMap<>();
        rolePermissions.put(Role.VETERINARIO, List.of("VER_USUARIOS", "GESTIONAR_USUARIOS", "GESTIONAR_ROLES"));
        rolePermissions.put(Role.RECEPCIONISTA, List.of("VER_USUARIOS"));
        rolePermissions.put(Role.CLIENTE, List.of("VER_PERFIL_PROPIO", "EDITAR_PERFIL_PROPIO"));
    }



    // Verificar si el usuario tiene un permiso específico
//    public boolean hasPermission(Object principal, String permission) {
//        // Obtener el usuario autenticado y sus roles
//        CustomUserDetails userDetails = (CustomUserDetails) principal;
//        for (Role role : userDetails.getRoles()) {
//            List<String> permissions = rolePermissions.get(role);
//            if (permissions != null && permissions.contains(permission)) {
//                return true;
//            }
//        }
//        return false;
//    }
    public boolean hasPermission(String uid, String permission) {
        try {
            List<Role> userRoles = getUserRoles(uid);
            for (Role role : userRoles) {
                List<String> rolePermissions = getRolePermissions(role.name());
                if (rolePermissions.contains(permission)) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            throw new CustomExceptions.ProcessingException("Error checking user permission: " + e.getMessage());
        }
    }
    public UserDTOs.RolePermissionDTO updateRolePermissions(String roleName, List<String> permissions) {
        try {
            ApiFuture<QuerySnapshot> future = getFirestore().collection("roles").whereEqualTo("name", roleName).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            if (!documents.isEmpty()) {
                String docId = documents.get(0).getId();
                getFirestore().collection("roles").document(docId).update("permissions", permissions).get();

                UserDTOs.RolePermissionDTO dto = new UserDTOs.RolePermissionDTO();
                dto.setRole(roleName);
                dto.setPermissions(permissions);
                return dto;
            } else {
                throw new CustomExceptions.ProcessingException("Role not found: " + roleName);
            }
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating role permissions: " + e.getMessage());
        }
    }

    public List<UserDTOs.RolePermissionDTO> getAllRolePermissions() {
        try {
            List<UserDTOs.RolePermissionDTO> rolePermissions = new ArrayList<>();
            ApiFuture<QuerySnapshot> future = getFirestore().collection("roles").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                UserDTOs.RolePermissionDTO dto = new UserDTOs.RolePermissionDTO();

                // Usar el campo 'name' como el nombre del rol en lugar del ID del documento
                String roleName = document.getString("name");
                dto.setRole(roleName != null ? roleName : document.getId());

                @SuppressWarnings("unchecked")
                List<String> permissions = (List<String>) document.get("permissions");
                dto.setPermissions(permissions);

                rolePermissions.add(dto);
            }

            return rolePermissions;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching role permissions: " + e.getMessage());
        }
    }
    private List<Role> getUserRoles(String uid) throws ExecutionException, InterruptedException {
        DocumentSnapshot document = getFirestore().collection("users").document(uid).get().get();
        if (document.exists()) {
            @SuppressWarnings("unchecked")
            List<String> roleStrings = (List<String>) document.get("roles");
            return roleStrings.stream()
                    .map(Role::valueOf)
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
    /*
    private List<String> getRolePermissions(String role) throws ExecutionException, InterruptedException {
        return (List<String>) getFirestore().collection("role_permissions").document(role).get().get().get("permissions");
    }

     */
    private List<String> getRolePermissions(String roleName) throws ExecutionException, InterruptedException {
        ApiFuture<QuerySnapshot> future = getFirestore().collection("roles").whereEqualTo("name", roleName).get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        if (!documents.isEmpty()) {
            @SuppressWarnings("unchecked")
            List<String> permissions = (List<String>) documents.get(0).get("permissions");
            return permissions != null ? permissions : new ArrayList<>();
        }
        return new ArrayList<>();
    }
}
