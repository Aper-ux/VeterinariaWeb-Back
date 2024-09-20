package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.UserDTOs;
import com.example.demo.model.Role;
import com.example.demo.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RolePermissionService rolePermissionService;

    @GetMapping("/permissions")
    public ResponseEntity<ApiResponse<Map<Role, List<String>>>> getAllRolePermissions() {
        Map<Role, List<String>> roles = rolePermissionService.getAllRolePermissions();
        return ResponseEntity.ok(ApiResponse.success(roles));
    }
}