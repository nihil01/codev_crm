package com.crm.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import java.net.URI;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReminderDeliveryJob {
    private final ReminderQueueService queue;

    @Value("${app.reminders.mode:MANUAL}")
    private String mode;

    @Value("${app.reminders.webhook-url:}")
    private String webhookUrl;

    @Value("${app.reminders.webhook-token:}")
    private String webhookToken;

    @Scheduled(cron="0 0 9 * * *",zone="Asia/Baku")
    public void dispatch() {
        if(!"WEBHOOK".equalsIgnoreCase(mode))return;
        URI target=URI.create(webhookUrl);
        if(!"https".equalsIgnoreCase(target.getScheme()) || target.getHost()==null ||
           webhookToken==null || webhookToken.isBlank()) {
            log.error("Reminder delivery disabled: HTTPS webhook and token are required");
            return;
        }
        queue.recoverStaleSending();
        queue.enqueueOverdue();

        SimpleClientHttpRequestFactory factory=new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);
        factory.setReadTimeout(5000);
        RestClient client=RestClient.builder().requestFactory(factory).build();

        for(var item:queue.pendingBatch()) {
            var payload=queue.claim(item.getId());
            if(payload.isEmpty())continue;
            var send=payload.get();
            try {
                client.post().uri(target)
                    .header("Authorization","Bearer "+webhookToken)
                    .header("Idempotency-Key",send.key())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("phone",send.phone(),"message",send.message(),"channel","PAYMENT_REMINDER"))
                    .retrieve().toBodilessEntity();
                queue.finish(send.id(),true,null);
            } catch(Exception e) {
                // Do not log phone, message, bearer token or remote response body.
                log.warn("Reminder webhook delivery failed for dispatch id {}",send.id());
                queue.finish(send.id(),false,e.getClass().getSimpleName());
            }
        }
    }
}
