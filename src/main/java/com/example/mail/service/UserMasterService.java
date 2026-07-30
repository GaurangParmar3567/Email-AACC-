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
        user.setAgentId(dto.getAgentId());

        SkillMaster skill = skillRepository.findById(dto.getSkillId())
                .orElseThrow(() -> new RuntimeException("Skill not found with id: " + dto.getSkillId()));
        user.setSkillId(skill.getId());

        return userRepository.save(user);
    }

    public UserMaster editUser(Long id, UserRequestDTO dto) {
        UserMaster existing = getUserById(id);
        if(dto.getFirstName()!=null){
            existing.setFirstName(dto.getFirstName());
        }
        if(dto.getLastName()!=null){
            existing.setLastName(dto.getLastName());
        }
        if(dto.getAgentId()!=null){
            existing.setAgentId(dto.getAgentId());
        }
        if (dto.getSkillId() != null){
            SkillMaster skill = skillRepository.findById(dto.getSkillId())
                    .orElseThrow(() -> new RuntimeException("Skill not found with id: " + dto.getSkillId()));
            existing.setSkillId(skill.getId());
        }

        return userRepository.save(existing);
    }

    public void deleteUser(Long id) {
        UserMaster user = getUserById(id);
        userRepository.delete(user);
    }
}