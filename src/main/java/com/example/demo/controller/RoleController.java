package com.example.demo.controller;

import com.example.demo.service.RolePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    @Autowired
    private RolePermissionService rolePermissionService;

    @GetMapping("/permissions")
    public ResponseEntity<?> getAllRolePermissions() {
        return ResponseEntity.ok(rolePermissionService.getAllRolePermissions());
    }
}