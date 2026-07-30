package com.example.mail.controller;

import com.example.mail.dto.request.SkillRequestDTO;
import com.example.mail.model.SkillMaster;
import com.example.mail.service.SkillMasterService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@CrossOrigin(origins = "https://ccdemsuat.sbi:6001")
public class SkillMasterController {

    private final SkillMasterService service;

    public SkillMasterController(SkillMasterService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<SkillMaster>> getAll() {
        return ResponseEntity.ok(service.getAllSkills());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SkillMaster> getById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getSkillById(id));
    }

    @PostMapping
    public ResponseEntity<SkillMaster> add(@RequestBody SkillRequestDTO dto) {
        return ResponseEntity.ok(service.addSkill(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SkillMaster> edit(@PathVariable Long id, @RequestBody SkillRequestDTO dto) {
        return ResponseEntity.ok(service.editSkill(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteSkill(id);
        return ResponseEntity.noContent().build();
    }
}