package com.example.mail.controller;

import com.example.mail.dto.request.UserRequestDTO;
import com.example.mail.model.UserMaster;
import com.example.mail.service.UserMasterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserMasterController {

    private final UserMasterService service;

    public UserMasterController(UserMasterService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<UserMaster>> getAll() {
        return ResponseEntity.ok(service.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserMaster> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserMaster> add(@RequestBody UserRequestDTO userDto) {
        return ResponseEntity.ok(service.addUser(userDto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserMaster> edit(@PathVariable Long id, @RequestBody UserRequestDTO userDto) {
        return ResponseEntity.ok(service.editUser(id, userDto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}