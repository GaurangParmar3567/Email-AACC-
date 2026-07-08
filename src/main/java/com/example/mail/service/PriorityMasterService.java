package com.example.mail.service;

import com.example.mail.dto.request.PriorityRequestDTO;
import com.example.mail.model.PriorityMaster;
import com.example.mail.repository.PriorityMasterRepo;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PriorityMasterService {

    private final PriorityMasterRepo repository;

    public PriorityMasterService(PriorityMasterRepo repository) {
        this.repository = repository;
    }

    public List<PriorityMaster> getAllPriorities() {
        return repository.findAll();
    }

    public PriorityMaster getPriorityById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Priority rule not found with id: " + id));
    }

    public PriorityMaster addPriority(PriorityRequestDTO dto) {
        PriorityMaster priority = new PriorityMaster();
        priority.setPriorityLevel(dto.getPriorityLevel());
        priority.setKeywords(dto.getKeywords());
        return repository.save(priority);
    }

    public PriorityMaster editPriority(Long id, PriorityRequestDTO dto) {
        PriorityMaster existing = getPriorityById(id);
        existing.setPriorityLevel(dto.getPriorityLevel());
        existing.setKeywords(dto.getKeywords());
        return repository.save(existing);
    }

    public void deletePriority(Long id) {
        PriorityMaster priority = getPriorityById(id);
        repository.delete(priority);
    }
}