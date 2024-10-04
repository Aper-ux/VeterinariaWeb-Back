package com.example.demo.controller;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.AuthDTOs;
import com.example.demo.dto.PetDTOs;
import com.example.demo.dto.UserDTOs.*;
import com.example.demo.exception.CustomExceptions;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(@RequestBody ChangePasswordRequest request) {
        String userId = SecurityContextHolder.getContext().getAuthentication().getName();
        try {
            userService.changePassword(userId, request);
            return ResponseEntity.ok(ApiResponse.success(null));
        } catch (CustomExceptions.UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("UNAUTHORIZED", e.getMessage()));
        } catch (CustomExceptions.InvalidPasswordException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_PASSWORD", e.getMessage()));
        } catch (CustomExceptions.ProcessingException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("PROCESSING_ERROR", e.getMessage()));
        }
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
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUserProfile() {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUserProfile()));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUserProfile(@RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.updateCurrentUserProfile(request)));
    }
    @GetMapping("/me/pets")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PetDTOs.PetResponse>>> getCurrentUserPets() {
        return ResponseEntity.ok(ApiResponse.success(userService.getCurrentUserPets()));
    }
    @GetMapping("/{id}/pets")
    @PreAuthorize("hasRole('VETERINARIO') or hasRole('RECEPCIONISTA') or @userService.isCurrentUser(#id)")
    public ResponseEntity<ApiResponse<List<PetDTOs.PetResponse>>> getUserPets(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getUserPets(id)));
    }
}