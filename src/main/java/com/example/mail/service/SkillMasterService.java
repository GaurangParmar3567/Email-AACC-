package com.example.mail.service;

import com.example.mail.dto.request.SkillRequestDTO;
import com.example.mail.model.SkillMaster;
import com.example.mail.repository.SkillMasterRepo;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class SkillMasterService {

    private final SkillMasterRepo repository;

    public SkillMasterService(SkillMasterRepo repository) {
        this.repository = repository;
    }


    public List<SkillMaster> getAllSkills() {
        return repository.findAll();
    }

    public SkillMaster getSkillById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + id));
    }

    public SkillMaster addSkill(SkillRequestDTO dto) {
        SkillMaster skill = new SkillMaster();
        skill.setName(dto.getName());
        skill.setKeywords(dto.getKeywords());
        return repository.save(skill);
    }

    public SkillMaster editSkill(Long id, SkillRequestDTO dto) {
        SkillMaster existing = getSkillById(id);
        existing.setName(dto.getName());
        existing.setKeywords(dto.getKeywords());
        return repository.save(existing);
    }

    public void deleteSkill(Long id) {
        SkillMaster skill = getSkillById(id);
        repository.delete(skill);
    }
}