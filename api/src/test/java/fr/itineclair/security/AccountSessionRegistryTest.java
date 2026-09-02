package fr.itineclair.security;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionBindingListener;

class AccountSessionRegistryTest {

    private static final UUID ACCOUNT_ID =
            UUID.fromString("936dd470-a45c-46fa-a0bd-94a76e4b836a");

    @Test
    void invalidatesEverySessionRegisteredForTheAccount() {
        AccountSessionRegistry registry = new AccountSessionRegistry();
        HttpSession first = session("first-session");
        HttpSession second = session("second-session");

        registry.register(ACCOUNT_ID, first);
        registry.register(ACCOUNT_ID, second);
        registry.invalidateAll(ACCOUNT_ID);

        verify(first).invalidate();
        verify(second).invalidate();
    }

    @Test
    void aNaturallyDestroyedSessionIsUnregistered() {
        AccountSessionRegistry registry = new AccountSessionRegistry();
        HttpSession session = session("expired-session");
        ArgumentCaptor<Object> binding = ArgumentCaptor.forClass(Object.class);

        registry.register(ACCOUNT_ID, session);
        verify(session).setAttribute(
                eq(AccountSessionRegistry.class.getName() + ".binding"),
                binding.capture());

        HttpSessionBindingListener listener =
                (HttpSessionBindingListener) binding.getValue();
        listener.valueUnbound(mock(HttpSessionBindingEvent.class));

        registry.invalidateAll(ACCOUNT_ID);
        verify(session, never()).invalidate();
    }

    @Test
    void anAlreadyInvalidSessionDoesNotBlockTheOthers() {
        AccountSessionRegistry registry = new AccountSessionRegistry();
        HttpSession expired = session("expired-session");
        HttpSession active = session("active-session");
        org.mockito.Mockito.doThrow(new IllegalStateException("Expired"))
                .when(expired)
                .invalidate();

        registry.register(ACCOUNT_ID, expired);
        registry.register(ACCOUNT_ID, active);
        registry.invalidateAll(ACCOUNT_ID);

        verify(active).invalidate();
    }

    private HttpSession session(String id) {
        HttpSession session = mock(HttpSession.class);
        when(session.getId()).thenReturn(id);
        return session;
    }
}
