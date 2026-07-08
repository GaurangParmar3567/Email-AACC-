package com.example.mail.service;

import com.example.mail.dto.request.UserRequestDTO;
import com.example.mail.model.SkillMaster;
import com.example.mail.model.UserMaster;
import com.example.mail.repository.SkillMasterRepo;
import com.example.mail.repository.UserMasterRepo;
import org.springframework.stereotype.Service;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class UserMasterService {

    private final UserMasterRepo userRepository;
    private final SkillMasterRepo skillRepository;

    public UserMasterService(UserMasterRepo userRepository, SkillMasterRepo skillRepository) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
    }

    public List<UserMaster> getAllUsers() {
        return userRepository.findAll();
    }

    public UserMaster getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User profile not found with id: " + id));
    }

    public UserMaster addUser(UserRequestDTO dto) {
        UserMaster user = new UserMaster();
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        // Convert plain IDs from the payload into managed Hibernate entities
        if (dto.getSkillIds() != null && !dto.getSkillIds().isEmpty()) {
            List<SkillMaster> matchedSkills = skillRepository.findAllById(dto.getSkillIds());
            user.setSkillSet(new HashSet<>(matchedSkills));
        }

        return userRepository.save(user); // ID is automatically generated here
    }

    public UserMaster editUser(Long id, UserRequestDTO dto) {
        UserMaster existing = getUserById(id);
        existing.setFirstName(dto.getFirstName());
        existing.setLastName(dto.getLastName());

        if (dto.getSkillIds() != null) {
            List<SkillMaster> matchedSkills = skillRepository.findAllById(dto.getSkillIds());
            existing.setSkillSet(new HashSet<>(matchedSkills));
        } else {
            existing.getSkillSet().clear();
        }

        return userRepository.save(existing);
    }

    public void deleteUser(Long id) {
        UserMaster user = getUserById(id);
        userRepository.delete(user);
    }
}