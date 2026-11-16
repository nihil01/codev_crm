package com.crm.service;

import com.crm.dto.LeadDto;
import com.crm.entity.Lead;
import com.crm.entity.Student;
import com.crm.entity.User;
import com.crm.enums.LeadStage;
import com.crm.enums.Role;
import com.crm.repository.LeadRepository;
import com.crm.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
public class LeadService {
    private final LeadRepository leads;
    private final UserRepository users;
    private final StudentService studentService;
    private final ChangeAuditService audit;

    @Transactional(readOnly = true)
    public List<LeadDto> all() {
        return leads.findAllByOrderByCreatedAtDesc().stream().map(this::dto).toList();
    }

    @Transactional(readOnly = true)
    public LeadDto one(Long id) {
        return dto(find(id));
    }

    @Transactional
    public LeadDto create(LeadDto data) {
        Lead lead = new Lead();
        apply(lead, data, actor());
        LeadDto created = dto(leads.save(lead));
        audit.record("CREATE","LEAD",created.getId(),null,created);
        return created;
    }

    @Transactional
    public LeadDto update(Long id, LeadDto data) {
        Lead lead = find(id);
        LeadDto before = dto(lead);
        apply(lead, data, actor());
        LeadDto changed = dto(leads.save(lead));
        audit.record("UPDATE","LEAD",id,before,changed);
        return changed;
    }

    @Transactional
    public void delete(Long id) {
        Lead lead = find(id);
        LeadDto before = dto(lead);
        leads.delete(lead);
        audit.record("DELETE","LEAD",id,before,null);
    }

    @Transactional
    public Long convert(Long leadId) {
        Lead lead = leads.lockById(leadId)
            .orElseThrow(()->new IllegalArgumentException("Lead tapılmadı"));
        LeadDto before = dto(lead);
        if (lead.getConvertedStudent()!=null)
            throw new IllegalArgumentException("Lead artıq tələbə kimi qeydiyyatdan keçib");
        String name = lead.getFullName().trim();
        String[] parts = name.split("\\s+",2);
        com.crm.dto.StudentDto dto=new com.crm.dto.StudentDto();
        dto.setFirstName(parts[0]);
        dto.setLastName(parts.length==2?parts[1]:"—");
        dto.setPhone(lead.getPhone());
        dto.setSource(lead.getSource());
        dto.setStatus(com.crm.enums.StudentStatus.AKTIV);
        com.crm.dto.StudentDto created = studentService.create(dto);
        Student student=studentService.getEntity(created.getId());
        lead.setStage(LeadStage.QEYDIYYAT);
        lead.setLostReason(null);
        lead.setConvertedStudent(student);
        leads.save(lead);
        audit.record("CONVERT","LEAD",leadId,before,dto(lead));
        return created.getId();
    }

    private Lead find(Long id) {
        return leads.findById(id).orElseThrow(() -> new IllegalArgumentException("Lead tapılmadı"));
    }

    private User actor() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmail(email).orElseThrow(() -> new AccessDeniedException("İstifadəçi tapılmadı"));
    }

    private void apply(Lead lead, LeadDto data, User actor) {
        if (data.getStage() == LeadStage.ITIRILDI &&
            (data.getLostReason() == null || data.getLostReason().isBlank()))
            throw new IllegalArgumentException("İtirilmiş lead üçün səbəb məcburidir");
        if (data.getStage() == LeadStage.SINAQ_KONSULTASIYA && data.getConsultationAt() == null)
            throw new IllegalArgumentException("Sınaq dərsi / konsultasiya tarixi məcburidir");

        Long requestedManager = data.getAssignedManagerId();
        Long assignedManager = lead.getAssignedManager() == null ? null : lead.getAssignedManager().getId();
        if (actor.getRole() != Role.ADMIN && requestedManager != null &&
            !requestedManager.equals(assignedManager))
            throw new AccessDeniedException("Lead təyinatını yalnız admin dəyişə bilər");

        if (actor.getRole() == Role.ADMIN) {
            if (requestedManager == null) lead.setAssignedManager(null);
            else {
                User selected = users.findById(requestedManager)
                    .orElseThrow(() -> new IllegalArgumentException("Menecer tapılmadı"));
                if (selected.getRole() != Role.MANAGER || !selected.isActive())
                    throw new IllegalArgumentException("Yalnız aktiv menecer seçilə bilər");
                lead.setAssignedManager(selected);
            }
        } else if (lead.getId() == null) {
            lead.setAssignedManager(actor);
        }
        lead.setFullName(data.getFullName().trim());
        lead.setPhone(data.getPhone().trim());
        lead.setSource(data.getSource());
        lead.setStage(data.getStage());
        lead.setConsultationAt(data.getConsultationAt());
        lead.setLostReason(data.getStage() == LeadStage.ITIRILDI ? data.getLostReason().trim() : null);
        lead.setInterestedCourse(data.getInterestedCourse());
        lead.setNote(data.getNote());
    }

    private LeadDto dto(Lead lead) {
        LeadDto result = new LeadDto();
        result.setId(lead.getId());
        result.setFullName(lead.getFullName());
        result.setPhone(lead.getPhone());
        result.setSource(lead.getSource());
        result.setStage(lead.getStage());
        result.setAssignedManagerId(lead.getAssignedManager() == null ? null : lead.getAssignedManager().getId());
        result.setConvertedStudentId(lead.getConvertedStudent()==null?null:lead.getConvertedStudent().getId());
        result.setAssignedManagerName(lead.getAssignedManager() == null ? null : lead.getAssignedManager().getFullName());
        result.setConsultationAt(lead.getConsultationAt());
        result.setLostReason(lead.getLostReason());
        result.setInterestedCourse(lead.getInterestedCourse());
        result.setNote(lead.getNote());
        result.setCreatedAt(lead.getCreatedAt());
        return result;
    }
}
