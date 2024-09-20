package com.example.demo.service;

import com.example.demo.model.Role;
import com.example.demo.security.CustomUserDetails;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RolePermissionService {

    private final Map<Role, List<String>> rolePermissions;

    public RolePermissionService() {
        rolePermissions = new HashMap<>();
        rolePermissions.put(Role.VETERINARIO, List.of("VER_USUARIOS", "GESTIONAR_USUARIOS", "GESTIONAR_ROLES"));
        rolePermissions.put(Role.RECEPCIONISTA, List.of("VER_USUARIOS"));
        rolePermissions.put(Role.CLIENTE, List.of("VER_PERFIL_PROPIO", "EDITAR_PERFIL_PROPIO"));
    }

    // Verificar si el usuario tiene un permiso específico
    public boolean hasPermission(Object principal, String permission) {
        // Obtener el usuario autenticado y sus roles
        CustomUserDetails userDetails = (CustomUserDetails) principal;
        for (Role role : userDetails.getRoles()) {
            List<String> permissions = rolePermissions.get(role);
            if (permissions != null && permissions.contains(permission)) {
                return true;
            }
        }
        return false;
    }

    public Map<Role, List<String>> getAllRolePermissions() {
        return new HashMap<>(rolePermissions);
    }
}
