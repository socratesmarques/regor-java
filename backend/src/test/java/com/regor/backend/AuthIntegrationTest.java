package com.regor.backend;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest @AutoConfigureMockMvc
class AuthIntegrationTest extends PostgresTestBase {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired JdbcTemplate jdbc;
    @Autowired org.springframework.security.oauth2.jwt.JwtEncoder encoder;
    @BeforeEach void clean() { jdbc.execute("TRUNCATE users CASCADE"); }
    String registration(String extra) {
        return """
            {"email":"ana@example.com","password":"strong-password","displayName":"Ana","username":"ana",
             "birthDate":"1995-05-10"%s}
            """.formatted(extra);
    }
    @Test void registerLoginAndDefaults() throws Exception {
        var response=mvc.perform(post("/api/auth/register").contentType("application/json").content(registration("")))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.user.role").value("USER"))
            .andExpect(jsonPath("$.user.vip").value(false)).andExpect(jsonPath("$.user.verified").value(false))
            .andExpect(jsonPath("$.user.coins").value(0)).andReturn();
        String token=json.readTree(response.getResponse().getContentAsString()).get("token").asText();
        mvc.perform(get("/api/users/me").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select password_hash from users",String.class)).startsWith("$2");
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"email\":\"ana@example.com\",\"password\":\"strong-password\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.token").isString());
        mvc.perform(post("/api/auth/login").contentType("application/json")
            .content("{\"email\":\"ana@example.com\",\"password\":\"wrong\"}")).andExpect(status().isUnauthorized());
    }
    @Test void rejectAnonymousInvalidJwtAndPrivilegeInjection() throws Exception {
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/users/me").header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
        for(String field:List.of("\"role\":\"ADMIN\"","\"vip\":true","\"verified\":true","\"coins\":999","\"permissions\":[\"ADMIN\"]"))
            mvc.perform(post("/api/auth/register").contentType("application/json").content(registration(","+field)))
                .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select count(*) from users",Integer.class)).isZero();
    }
    @Test void rejectUnderageAndDuplicateRegistration() throws Exception {
        mvc.perform(post("/api/auth/register").contentType("application/json")
            .content(registration("").replace("1995-05-10",java.time.LocalDate.now().minusYears(17).toString())))
            .andExpect(status().isBadRequest());
        mvc.perform(post("/api/auth/register").contentType("application/json").content(registration(""))).andExpect(status().isCreated());
        mvc.perform(post("/api/auth/register").contentType("application/json").content(registration(""))).andExpect(status().isConflict());
    }
    @Test void allDomainTablesCreatedByFlyway() {
        var tables=jdbc.queryForList("select tablename from pg_tables where schemaname='public'",String.class);
        assertThat(tables).contains("users","profiles","photos","swipes","matches","chats","chat_participants","messages","notifications","user_preferences","flyway_schema_history");
    }

    @Test void expiredJwtAndPasswordChangeRevokeAccess() throws Exception {
        var result=mvc.perform(post("/api/auth/register").contentType("application/json").content(registration("")))
            .andExpect(status().isCreated()).andReturn();
        var session=json.readTree(result.getResponse().getContentAsString());
        String token=session.get("token").asText(),id=session.at("/user/id").asText();
        var claims=org.springframework.security.oauth2.jwt.JwtClaimsSet.builder().issuer("regor").subject(id)
            .claim("version",0).issuedAt(java.time.Instant.now().minusSeconds(7200)).expiresAt(java.time.Instant.now().minusSeconds(3600)).build();
        String expired=encoder.encode(org.springframework.security.oauth2.jwt.JwtEncoderParameters.from(
            org.springframework.security.oauth2.jwt.JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build(),claims)).getTokenValue();
        mvc.perform(get("/api/users/me").header("Authorization","Bearer "+expired)).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/users/me/password").header("Authorization","Bearer "+token).contentType("application/json")
            .content(json.writeValueAsString(Map.of("currentPassword","strong-password","newPassword","new-strong-password"))))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/users/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
}
