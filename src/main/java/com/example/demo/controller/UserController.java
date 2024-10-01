package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.AuthDTOs;
import com.example.demo.dto.PetDTOs;
import com.example.demo.dto.UserDTOs.*;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers(
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String role) {
        return ResponseEntity.ok(ApiResponse.success(userService.getAllUsers(isActive, role)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> createUser(@RequestBody AuthDTOs.RegisterRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.createUser(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUser(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@PathVariable String id, @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<ApiResponse<UserResponse>> toggleUserStatus(@PathVariable String id, @RequestBody ToggleUserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.toggleUserStatus(id, request)));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<Void>> addNote(@PathVariable String id, @RequestBody AddNoteRequest request) {
        userService.addNote(id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/{id}/notes")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUserNotes(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserNotes(id)));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateUserProfile(request)));
    }
    @GetMapping("/{id}/pets")
    @PreAuthorize("hasRole('VETERINARIO') or hasRole('RECEPCIONISTA') or @userService.isCurrentUser(#id)")
    public ResponseEntity<ApiResponse<List<PetDTOs.PetResponse>>> getUserPets(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserPets(id)));
    }
}