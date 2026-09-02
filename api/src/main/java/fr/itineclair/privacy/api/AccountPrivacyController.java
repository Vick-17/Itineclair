package fr.itineclair.privacy.api;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.privacy.AccountActionRateLimitExceededException;
import fr.itineclair.privacy.AccountDataExportService;
import fr.itineclair.privacy.AccountDeletionService;
import fr.itineclair.privacy.InvalidCurrentPasswordException;
import fr.itineclair.privacy.PreparedAccountExport;
import fr.itineclair.security.AccountSessionRegistry;
import fr.itineclair.security.LoginAttemptLimiter;
import fr.itineclair.security.LoginRateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/account")
public class AccountPrivacyController {

    private static final MediaType ZIP_MEDIA_TYPE =
            MediaType.parseMediaType("application/zip");

    private final AccountDataExportService accountDataExportService;
    private final AccountDeletionService accountDeletionService;
    private final LoginAttemptLimiter loginAttemptLimiter;
    private final SecurityContextHolderStrategy securityContextHolderStrategy;
    private final AccountSessionRegistry accountSessionRegistry;
    private final boolean secureCookies;

    public AccountPrivacyController(
            AccountDataExportService accountDataExportService,
            AccountDeletionService accountDeletionService,
            LoginAttemptLimiter loginAttemptLimiter,
            SecurityContextHolderStrategy securityContextHolderStrategy,
            AccountSessionRegistry accountSessionRegistry,
            @Value("${itineclair.security.secure-cookies:false}")
            boolean secureCookies) {
        this.accountDataExportService = accountDataExportService;
        this.accountDeletionService = accountDeletionService;
        this.loginAttemptLimiter = loginAttemptLimiter;
        this.securityContextHolderStrategy = securityContextHolderStrategy;
        this.accountSessionRegistry = accountSessionRegistry;
        this.secureCookies = secureCookies;
    }

    @PostMapping(
            value = "/export",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = "application/zip")
    public ResponseEntity<StreamingResponseBody> exportAccountData(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody AccountExportRequest request,
            HttpServletRequest httpRequest) {
        LoginAttemptLimiter.Permit permit = reservePermit(
                principal,
                httpRequest);

        PreparedAccountExport preparedExport;

        try {
            preparedExport = accountDataExportService.prepareExport(
                    principal,
                    request.currentPassword());
            loginAttemptLimiter.authenticationSucceeded(permit);
        } catch (InvalidCurrentPasswordException exception) {
            throw exception;
        } catch (AuthenticationServiceException exception) {
            loginAttemptLimiter.authenticationUnavailable(permit);
            throw exception;
        } catch (RuntimeException exception) {
            loginAttemptLimiter.authenticationSucceeded(permit);
            throw exception;
        }

        StreamingResponseBody body = outputStream ->
                accountDataExportService.writeExport(
                        preparedExport,
                        outputStream);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition
                                .attachment()
                                .filename(
                                        preparedExport.filename(),
                                        UTF_8)
                                .build()
                                .toString())
                .contentType(ZIP_MEDIA_TYPE)
                .body(body);
    }

    @DeleteMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> deleteAccount(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody DeleteAccountRequest request,
            HttpServletRequest httpRequest) {
        LoginAttemptLimiter.Permit permit = reservePermit(
                principal,
                httpRequest);

        try {
            accountDeletionService.deleteAccount(
                    principal,
                    request.currentPassword(),
                    request.confirmationEmail());
            loginAttemptLimiter.authenticationSucceeded(permit);
        } catch (InvalidCurrentPasswordException exception) {
            throw exception;
        } catch (AuthenticationServiceException exception) {
            loginAttemptLimiter.authenticationUnavailable(permit);
            throw exception;
        } catch (RuntimeException exception) {
            loginAttemptLimiter.authenticationSucceeded(permit);
            throw exception;
        }

        accountSessionRegistry.invalidateAll(principal.id());
        securityContextHolderStrategy.clearContext();

        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CacheControl.noStore());
        headers.add(HttpHeaders.SET_COOKIE, expiredSessionCookie().toString());
        headers.add(HttpHeaders.SET_COOKIE, expiredCsrfCookie().toString());

        return new ResponseEntity<>(headers, HttpStatus.NO_CONTENT);
    }

    private LoginAttemptLimiter.Permit reservePermit(
            AccountPrincipal principal,
            HttpServletRequest request) {
        try {
            return loginAttemptLimiter.beforeAuthentication(
                    principal.email(),
                    request);
        } catch (LoginRateLimitExceededException exception) {
            throw new AccountActionRateLimitExceededException(
                    exception.retryAfterSeconds());
        }
    }

    private ResponseCookie expiredSessionCookie() {
        return ResponseCookie.from("ITINECLAIR_SESSION", "")
                .path("/api")
                .httpOnly(true)
                .secure(secureCookies)
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
    }

    private ResponseCookie expiredCsrfCookie() {
        return ResponseCookie.from("XSRF-TOKEN", "")
                .path("/")
                .httpOnly(false)
                .secure(secureCookies)
                .sameSite("Lax")
                .maxAge(Duration.ZERO)
                .build();
    }
}
