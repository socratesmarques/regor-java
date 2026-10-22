package com.regor.backend;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
public abstract class PostgresTestBase {
    static final EmbeddedPostgres POSTGRES;
    static {
        try {
            POSTGRES=EmbeddedPostgres.builder().setPort(0).start();
            Runtime.getRuntime().addShutdownHook(new Thread(()->{ try { POSTGRES.close(); } catch(Exception ignored) {} }));
        } catch(Exception e) { throw new ExceptionInInitializerError(e); }
    }
    @DynamicPropertySource
    static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url",()->POSTGRES.getJdbcUrl("postgres","postgres"));
        properties.add("spring.datasource.username",()->"postgres");
        properties.add("spring.datasource.password",()->"postgres");
        properties.add("regor.jwt.secret",()->"test-only-secret-32-bytes-minimum-never-production");
    }
}
