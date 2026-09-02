package fr.itineclair.security;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;

@Service
public class AccountSessionRegistry {

    private static final String SESSION_BINDING_ATTRIBUTE =
            AccountSessionRegistry.class.getName() + ".binding";

    private final ConcurrentMap<UUID, ConcurrentMap<String, HttpSession>>
            sessionsByAccount = new ConcurrentHashMap<>();

    public void register(UUID accountId, HttpSession session) {
        Objects.requireNonNull(accountId, "accountId is required.");
        Objects.requireNonNull(session, "session is required.");

        Object existingBinding = session.getAttribute(
                SESSION_BINDING_ATTRIBUTE);

        if (existingBinding != null) {
            session.removeAttribute(SESSION_BINDING_ATTRIBUTE);
        }

        String sessionId = session.getId();
        sessionsByAccount
                .computeIfAbsent(
                        accountId,
                        ignored -> new ConcurrentHashMap<>())
                .put(sessionId, session);

        try {
            session.setAttribute(
                    SESSION_BINDING_ATTRIBUTE,
                    new SessionBinding(this, accountId, sessionId));
        } catch (IllegalStateException exception) {
            unregister(accountId, sessionId);
            throw exception;
        }
    }

    public void invalidateAll(UUID accountId) {
        Objects.requireNonNull(accountId, "accountId is required.");

        ConcurrentMap<String, HttpSession> sessions =
                sessionsByAccount.remove(accountId);

        if (sessions == null) {
            return;
        }

        for (HttpSession session : sessions.values()) {
            try {
                session.invalidate();
            } catch (IllegalStateException ignored) {
                // La session a déjà expiré ou été invalidée.
            }
        }
    }

    private void unregister(UUID accountId, String sessionId) {
        sessionsByAccount.computeIfPresent(
                accountId,
                (ignored, sessions) -> {
                    sessions.remove(sessionId);
                    return sessions.isEmpty() ? null : sessions;
                });
    }

    private record SessionBinding(
            AccountSessionRegistry registry,
            UUID accountId,
            String sessionId)
            implements HttpSessionBindingListener {

        @Override
        public void valueUnbound(HttpSessionBindingEvent event) {
            registry.unregister(accountId, sessionId);
        }
    }
}
