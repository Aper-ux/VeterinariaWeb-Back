package com.example.demo.service;

import com.example.demo.dto.UserDTOs;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.security.CustomUserDetails;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import lombok.Data;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

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
    public UserDTOs.RolePermissionDTO updateRolePermissions(String role, List<String> permissions) {
        try {
            getFirestore().collection("role_permissions").document(role).set(Map.of("permissions", permissions)).get();
            UserDTOs.RolePermissionDTO dto = new UserDTOs.RolePermissionDTO();
            dto.setRole(role);
            dto.setPermissions(permissions);
            return dto;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error updating role permissions: " + e.getMessage());
        }
    }


    public List<UserDTOs.RolePermissionDTO> getAllRolePermissions() {
        try {
            List<UserDTOs.RolePermissionDTO> rolePermissions = new ArrayList<>();
            getFirestore().collection("role_permissions").get().get().getDocuments().forEach(doc -> {
                UserDTOs.RolePermissionDTO dto = new UserDTOs.RolePermissionDTO();
                dto.setRole(doc.getId());
                dto.setPermissions((List<String>) doc.get("permissions"));
                rolePermissions.add(dto);
            });
            return rolePermissions;
        } catch (InterruptedException | ExecutionException e) {
            throw new CustomExceptions.ProcessingException("Error fetching role permissions: " + e.getMessage());
        }
    }
    private List<Role> getUserRoles(String uid) throws ExecutionException, InterruptedException {
        return (List<Role>) getFirestore().collection("users").document(uid).get().get().get("roles");
    }

    private List<String> getRolePermissions(String role) throws ExecutionException, InterruptedException {
        return (List<String>) getFirestore().collection("role_permissions").document(role).get().get().get("permissions");
    }
}
