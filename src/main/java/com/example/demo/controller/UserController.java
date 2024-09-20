package com.example.demo.controller;

import com.example.demo.dto.UserDTOs.*;
import com.example.demo.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable String id, @RequestBody UpdateUserRequest request) {
        return ResponseEntity.ok(userService.updateUser(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/change-password")
    public ResponseEntity<?> changePassword(@PathVariable String id, @RequestBody ChangePasswordRequest request) {
        userService.changePassword(id, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/toggle-status")
    public ResponseEntity<?> toggleUserStatus(@PathVariable String id, @RequestBody ToggleUserStatusRequest request) {
        return ResponseEntity.ok(userService.toggleUserStatus(id, request));
    }

    @PostMapping("/{id}/notes")
    public ResponseEntity<?> addNote(@PathVariable String id, @RequestBody AddNoteRequest request) {
        userService.addNote(id, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/notes")
    public ResponseEntity<?> getUserNotes(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserNotes(id));
    }
}
