package com.tracepilot.api.Services;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.tracepilot.api.DTO.Response.AuditResponse;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AuditEmitterRegistry {
    private final Map<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    public SseEmitter register(UUID auditId) {
        log.debug("Registering SSE emitter for audit {}", auditId);
        SseEmitter emitter = new SseEmitter(10 * 60 * 1000L); // 10 min, longer than Spring's 30s default
        emitters.computeIfAbsent(auditId, id -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            log.debug("Cleaning up SSE emitter for audit {}", auditId);
            List<SseEmitter> list = emitters.get(auditId);
            if (list != null)
                list.remove(emitter);
        };
        emitter.onCompletion(() -> {
            log.debug("SSE emitter completed for audit {}", auditId);
            cleanup.run();
        });

        emitter.onTimeout(() -> {
            log.warn("SSE emitter timed out for audit {}", auditId);
            cleanup.run();
        });

        emitter.onError(e -> {
            log.warn("SSE emitter error for audit {}", auditId, e);
            cleanup.run();
        });

        return emitter;
    }

    public void pushAndComplete(UUID auditId, AuditResponse payload) {
        log.debug("Pushing audit response to SSE clients for audit {}", auditId);
        List<SseEmitter> list = emitters.remove(auditId);
        if (list == null) {
            log.debug("No registered SSE emitters found for audit {}", auditId);
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("status").data(payload));
                emitter.complete();
                log.debug("Successfully sent SSE event for audit {}", auditId);
            } catch (IOException e) {
                log.error("Failed to send SSE event for audit {}", auditId, e);
                emitter.completeWithError(e);
            }
        }
    }

    public void push(UUID auditId, Object payload) {
        log.debug("Pushing progress event to SSE clients for audit {}", auditId);

        List<SseEmitter> list = emitters.get(auditId);
        if (list == null || list.isEmpty()) {
            log.debug("No registered SSE emitters found for audit {}", auditId);
            return;
        }

        if (payload == null) {
            log.warn("Attempting to push null payload for audit {}", auditId);
        }

        for (SseEmitter emitter : list) {
            try {
                emitter.send(SseEmitter.event().name("progress").data(payload));
                log.debug("Successfully sent progress event for audit {}", auditId);
            } catch (IOException e) {
                log.error("Failed to send progress event for audit {}", auditId, e);
                emitter.completeWithError(e);
            }
        }
    }
}
