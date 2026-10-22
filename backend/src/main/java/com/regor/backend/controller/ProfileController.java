package com.regor.backend.controller;
import com.regor.backend.dto.Api.*;
import com.regor.backend.repository.*;
import com.regor.backend.service.ProfileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api") @Validated
public class ProfileController {
    private final ProfileService service; private final DiscoveryRepository discovery; private final ProfileRepository profiles;
    public ProfileController(ProfileService service,DiscoveryRepository discovery,ProfileRepository profiles) {
        this.service=service;this.discovery=discovery;this.profiles=profiles;
    }
    private UUID id(Jwt jwt) { return UUID.fromString(jwt.getSubject()); }
    @GetMapping("/profiles/me") public ProfileView me(@AuthenticationPrincipal Jwt jwt) { return service.view(id(jwt),id(jwt)); }
    @GetMapping("/profiles/{id}") public ProfileView profile(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { return service.view(id,id(jwt)); }
    @PutMapping("/profiles/me") public ProfileView update(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ProfileEdit edit) { return service.update(id(jwt),edit); }
    @GetMapping("/preferences") public Preferences preferences(@AuthenticationPrincipal Jwt jwt) { return service.preferences(id(jwt)); }
    @PutMapping("/preferences") public Preferences preferences(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Preferences edit) { return service.preferences(id(jwt),edit); }
    @PostMapping("/photos") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,UUID> upload(@AuthenticationPrincipal Jwt jwt,@RequestParam MultipartFile file) { return Map.of("id",service.upload(id(jwt),file)); }
    @GetMapping("/photos/{id}") public ResponseEntity<byte[]> photo(@PathVariable UUID id) {
        var p=service.photo(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(p.mediaType))
            .cacheControl(CacheControl.noStore()).body(p.content);
    }
    @DeleteMapping("/photos/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { service.deletePhoto(id(jwt),id); }
    @GetMapping("/discovery") public List<ProfileView> discover(@AuthenticationPrincipal Jwt jwt,
        @RequestParam(defaultValue="") @Size(max=100) String q,
        @RequestParam(required=false) @Min(18) @Max(120) Integer minAge,
        @RequestParam(required=false) @Min(18) @Max(120) Integer maxAge,
        @RequestParam(required=false) @Size(max=120) String location,
        @RequestParam(required=false) @Size(max=80) String interest,
        @RequestParam(required=false) @Size(max=30) String gender,
        @RequestParam(required=false) @Min(1) @Max(20000) Integer distance,
        @RequestParam(defaultValue="0") @Min(0) @Max(10000) int page) {
        var prefs=service.preferences(id(jwt));
        if((minAge==null?prefs.minAge():minAge)>(maxAge==null?prefs.maxAge():maxAge))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Faixa etária inválida");
        if(distance!=null&&profiles.findById(id(jwt)).orElseThrow().latitude==null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Informe suas coordenadas no perfil para filtrar por distância");
        return discovery.discover(id(jwt),profiles.findById(id(jwt)).orElseThrow(),prefs,q,minAge,maxAge,location,interest,gender,distance,page)
            .stream().map(other->service.view(other,id(jwt))).toList();
    }
}
