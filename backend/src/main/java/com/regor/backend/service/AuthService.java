package com.regor.backend.service;
import com.regor.backend.dto.Api.*;
import com.regor.backend.entity.*;
import com.regor.backend.repository.*;
import java.time.*;
import java.util.*;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserRepository users;
    private final ProfileRepository profiles;
    private final UserPreferenceRepository preferences;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final String dummyHash;
    public AuthService(UserRepository users, ProfileRepository profiles, UserPreferenceRepository preferences,
                       PasswordEncoder passwords, JwtEncoder encoder) {
        this.users=users; this.profiles=profiles; this.preferences=preferences;
        this.passwords=passwords; this.encoder=encoder;
        this.dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    public static void checkAge(LocalDate date) {
        int age=Period.between(date,LocalDate.now()).getYears();
        if(age<18 || age>120) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A idade deve estar entre 18 e 120 anos");
    }
    private static void checkPassword(String password) {
        if(password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"A senha excede 72 bytes");
    }
    @Transactional
    public Session register(Register request) {
        checkAge(request.birthDate()); checkPassword(request.password());
        String email=request.email().strip().toLowerCase(Locale.ROOT);
        String username=request.username().toLowerCase(Locale.ROOT);
        if(users.existsByEmail(email)||profiles.existsByUsername(username))
            throw new ResponseStatusException(HttpStatus.CONFLICT,"E-mail ou nome de usuário indisponível");
        var user=new User(); user.email=email; user.passwordHash=passwords.encode(request.password());
        users.save(user);
        var profile=new Profile(); profile.id=user.id; profile.displayName=request.displayName().strip();
        profile.username=username; profile.birthDate=request.birthDate(); profiles.save(profile);
        var preference=new UserPreference(); preference.id=user.id; preferences.save(preference);
        return session(user);
    }
    @Transactional(readOnly=true)
    public Session login(Login request) {
        var user=users.findByEmail(request.email().strip().toLowerCase(Locale.ROOT));
        boolean valid=passwords.matches(request.password(),user.map(u->u.passwordHash).orElse(dummyHash));
        if(user.isEmpty()||!valid) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"E-mail ou senha inválidos");
        return session(user.get());
    }
    public Account account(UUID id) {
        var u=users.findById(id).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        return new Account(u.id,u.email,u.role,u.vip,u.verified,u.coins);
    }
    @Transactional
    public void changePassword(UUID id, PasswordChange request) {
        checkPassword(request.newPassword());
        var u=users.lockById(id).orElseThrow();
        if(!passwords.matches(request.currentPassword(),u.passwordHash))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Senha atual incorreta");
        u.passwordHash=passwords.encode(request.newPassword());
        u.tokenVersion++;
    }
    private Session session(User u) {
        Instant now=Instant.now(), expires=now.plusSeconds(3600);
        var claims=JwtClaimsSet.builder().issuer("regor").subject(u.id.toString())
            .claim("version",u.tokenVersion).issuedAt(now).expiresAt(expires).id(UUID.randomUUID().toString()).build();
        var token=encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new Session(token,expires,account(u.id));
    }
}
