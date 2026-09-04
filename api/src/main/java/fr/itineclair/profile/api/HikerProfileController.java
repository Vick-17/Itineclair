package fr.itineclair.profile.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import fr.itineclair.identity.AccountPrincipal;
import fr.itineclair.profile.HikerProfileService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/profile")
public class HikerProfileController {

    private final HikerProfileService hikerProfileService;

    public HikerProfileController(HikerProfileService hikerProfileService) {
        this.hikerProfileService = hikerProfileService;
    }

    @GetMapping
    public HikerProfileResponse get(
            @AuthenticationPrincipal AccountPrincipal principal) {
        return hikerProfileService.get(principal.id())
                .map(HikerProfileResponse::from)
                .orElseGet(HikerProfileResponse::notConfigured);
    }

    @PutMapping
    public HikerProfileResponse save(
            @AuthenticationPrincipal AccountPrincipal principal,
            @Valid @RequestBody SaveHikerProfileRequest request) {
        return HikerProfileResponse.from(
                hikerProfileService.save(
                        principal.id(),
                        request.toCommand()));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal AccountPrincipal principal) {
        hikerProfileService.delete(principal.id());
    }
}
