package com.example.mail.controller;

import com.example.mail.dto.request.PriorityRequestDTO;
import com.example.mail.model.PriorityMaster;
import com.example.mail.service.PriorityMasterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/priorities")
public class PriorityMasterController {

    private final PriorityMasterService service;

    public PriorityMasterController(PriorityMasterService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PriorityMaster>> getAll() {
        return ResponseEntity.ok(service.getAllPriorities());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PriorityMaster> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getPriorityById(id));
    }

    @PostMapping
    public ResponseEntity<PriorityMaster> add(@RequestBody PriorityRequestDTO dto) {
        return ResponseEntity.ok(service.addPriority(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PriorityMaster> edit(@PathVariable Long id, @RequestBody PriorityRequestDTO dto) {
        return ResponseEntity.ok(service.editPriority(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deletePriority(id);
        return ResponseEntity.noContent().build();
    }
}