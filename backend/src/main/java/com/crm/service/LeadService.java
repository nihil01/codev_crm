package com.crm.service;

import com.crm.dto.LeadDto;
import com.crm.entity.Lead;
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
        return dto(leads.save(lead));
    }

    @Transactional
    public LeadDto update(Long id, LeadDto data) {
        Lead lead = find(id);
        apply(lead, data, actor());
        return dto(leads.save(lead));
    }

    @Transactional
    public void delete(Long id) {
        leads.delete(find(id));
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
        result.setAssignedManagerName(lead.getAssignedManager() == null ? null : lead.getAssignedManager().getFullName());
        result.setConsultationAt(lead.getConsultationAt());
        result.setLostReason(lead.getLostReason());
        result.setInterestedCourse(lead.getInterestedCourse());
        result.setNote(lead.getNote());
        result.setCreatedAt(lead.getCreatedAt());
        return result;
    }
}
