package com.crm.service;

import com.crm.entity.AuditEvent;
import com.crm.repository.AuditEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ChangeAuditService {
    private static final Set<String> SECRET_KEYS = Set.of(
        "password", "passwordhash", "token", "secret", "authorization",
        "accesstoken", "refreshtoken", "cookie", "credential", "apikey"
    );
    private final AuditEventRepository events;
    private final ObjectMapper mapper;

    /**
     * Call from the same transaction that writes the business entity, otherwise
     * the business action and its audit record could disagree after a rollback.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void record(String action, String entityType, Long entityId, Object before, Object after) {
        Map<String,Object> oldValues = safeMap(before);
        Map<String,Object> newValues = safeMap(after);
        Set<String> fields = new LinkedHashSet<>(oldValues.keySet());
        fields.addAll(newValues.keySet());
        Map<String,Object> changedBefore = new LinkedHashMap<>();
        Map<String,Object> changedAfter = new LinkedHashMap<>();
        for (String key : fields) {
            if (!Objects.equals(oldValues.get(key),newValues.get(key))) {
                changedBefore.put(key,oldValues.get(key));
                changedAfter.put(key,newValues.get(key));
            }
        }
        if (changedBefore.isEmpty() && changedAfter.isEmpty()) return;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actor = auth != null && auth.isAuthenticated()
            && !"anonymousUser".equals(auth.getName()) ? auth.getName() : "SYSTEM";
        ServletRequestAttributes attrs =
            RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes a ? a : null;

        AuditEvent item = new AuditEvent();
        item.setActorEmail(actor);
        item.setHttpMethod(attrs == null ? "CHANGE" : attrs.getRequest().getMethod());
        item.setPath(attrs == null ? "/system/" + entityType
            : trimPath(attrs.getRequest().getRequestURI()));
        item.setStatusCode(200);
        item.setEntityType(entityType);
        item.setEntityId(entityId);
        item.setAction(action);
        item.setBeforeJson(toJson(changedBefore));
        item.setAfterJson(toJson(changedAfter));
        events.save(item);
    }

    private Map<String,Object> safeMap(Object item) {
        if (item == null) return Map.of();
        Map<String,Object> raw=mapper.convertValue(item,
            new TypeReference<LinkedHashMap<String,Object>>() {});
        @SuppressWarnings("unchecked")
        Map<String,Object> cleaned=(Map<String,Object>)sanitize(raw);
        return cleaned;
    }

    private Object sanitize(Object value) {
        if (value instanceof Map<?,?> raw) {
            Map<String,Object> result=new LinkedHashMap<>();
            for (var entry:raw.entrySet()) {
                String key=String.valueOf(entry.getKey());
                String normalized=key.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]","");
                if (SECRET_KEYS.contains(normalized) || normalized.endsWith("password") ||
                    normalized.endsWith("token") || normalized.endsWith("secret") ||
                    normalized.endsWith("apikey")) continue;
                result.put(key,sanitize(entry.getValue()));
            }
            return result;
        }
        if (value instanceof Collection<?> list)
            return list.stream().map(this::sanitize).toList();
        return value;
    }

    private String toJson(Object item) {
        try { return mapper.writeValueAsString(item); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Audit JSON xətası",ex); }
    }

    private String trimPath(String value) {
        return value.length()>512?value.substring(0,512):value;
    }
}
