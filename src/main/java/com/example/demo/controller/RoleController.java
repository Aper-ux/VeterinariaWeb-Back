package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.UserDTOs;
import com.example.demo.model.Role;
import com.example.demo.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RolePermissionService rolePermissionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserDTOs.RolePermissionDTO>>> getAllRolePermissions() {
        return ResponseEntity.ok(ApiResponse.success(rolePermissionService.getAllRolePermissions()));
    }
    @PutMapping("/{role}")
    public ResponseEntity<ApiResponse<UserDTOs.RolePermissionDTO>> updateRolePermissions(
            @PathVariable String role,
            @RequestBody List<String> permissions) {
        return ResponseEntity.ok(ApiResponse.success(rolePermissionService.updateRolePermissions(role, permissions)));
    }
}