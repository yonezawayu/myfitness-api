package com.myfitness.api.common.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.ClassicConfiguration;
import org.flywaydb.core.internal.database.DatabaseType;
import org.flywaydb.core.internal.parser.ParsingContext;
import org.flywaydb.core.internal.plugin.PluginRegister;
import org.flywaydb.core.internal.resource.filesystem.FileSystemResource;
import org.flywaydb.core.internal.resolver.ChecksumCalculator;
import org.flywaydb.database.postgresql.PostgreSQLDatabaseType;
import org.flywaydb.database.postgresql.PostgreSQLParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** Uses only isolated in-memory H2 or offline parsing, never a PostgreSQL connection. */
class FlywayCompatibilityTest {
    @TempDir
    Path migrations;

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class,
                        FlywayAutoConfiguration.class))
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:flyway-compat-" + UUID.randomUUID(),
                        "spring.datasource.driver-class-name=org.h2.Driver",
                        "spring.datasource.username=sa", "spring.datasource.password=",
                        "spring.flyway.locations=filesystem:" + migrations.toAbsolutePath(),
                        "spring.flyway.clean-disabled=true", "spring.flyway.baseline-on-migrate=false");
    }

    @Test
    void applicationDefaultsKeepAutomaticMigrationDisabledAndHibernateValidation() {
        runner().run(context -> {
            assertThat(context).hasNotFailed().doesNotHaveBean(Flyway.class);
            assertThat(context.getEnvironment().getProperty("spring.flyway.enabled")).isEqualTo("false");
            assertThat(context.getEnvironment().getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");
        });
    }

    @Test
    void bootAutoConfigurationCanMigrateAndValidateOnlyAnExplicitlyEnabledH2Database() throws Exception {
        Files.writeString(migrations.resolve("V1__probe.sql"), "CREATE TABLE flyway_compat_probe(id bigint PRIMARY KEY);");
        runner().withPropertyValues("spring.flyway.enabled=true", "spring.flyway.target=1")
                .run(context -> {
                    assertThat(context).hasNotFailed().hasSingleBean(Flyway.class);
                    Flyway flyway = context.getBean(Flyway.class);
                    assertThat(flyway.getConfiguration().getDataSource()).isNotNull();
                    assertThat(flyway.getConfiguration().getTarget().getVersion()).isEqualTo("1");
                    assertThat(flyway.getConfiguration().isCleanDisabled()).isTrue();
                    assertThat(flyway.getConfiguration().isBaselineOnMigrate()).isFalse();
                    assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("1");
                    flyway.validate();
                });
    }

    @Test
    void migrationFailurePreventsContextStartupWithoutCleaningOrRepairing() throws Exception {
        Files.writeString(migrations.resolve("V1__broken.sql"), "THIS IS INTENTIONALLY INVALID SQL;");
        runner().withPropertyValues("spring.flyway.enabled=true").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasStackTraceContaining("FlywayMigrateException");
        });
    }

    @Test
    void hibernateValidateCanRunAfterFlywayInitializationOnIsolatedH2() throws Exception {
        Files.writeString(migrations.resolve("V1__probe.sql"), "CREATE TABLE flyway_compat_probe(id bigint PRIMARY KEY);");
        runner().withConfiguration(AutoConfigurations.of(HibernateJpaAutoConfiguration.class))
                .withUserConfiguration(ProbeJpaConfiguration.class)
                .withPropertyValues("spring.flyway.enabled=true", "spring.jpa.hibernate.ddl-auto=validate")
                .run(context -> assertThat(context).hasNotFailed().hasSingleBean(EntityManagerFactory.class));
    }

    @TestConfiguration(proxyBeanMethods = false)
    @EntityScan(basePackageClasses = ProbeEntity.class)
    static class ProbeJpaConfiguration {
    }

    @Entity
    @Table(name = "flyway_compat_probe")
    static class ProbeEntity {
        @Id
        private Long id;
    }

    @Test
    void communityPostgresqlModuleIsDiscoveredWithTheSameVersionAsCore() throws Exception {
        assertThat(new PluginRegister().getPlugins(DatabaseType.class))
                .anyMatch(PostgreSQLDatabaseType.class::isInstance);
        String coreVersion = artifactVersion("flyway-core");
        assertThat(coreVersion).isEqualTo("11.20.3");
        assertThat(artifactVersion("flyway-database-postgresql")).isEqualTo(coreVersion);
    }

    private String artifactVersion(String artifact) throws Exception {
        Properties metadata = new Properties();
        try (var input = Flyway.class.getResourceAsStream("/META-INF/maven/org.flywaydb/" + artifact + "/pom.properties")) {
            assertThat(input).isNotNull();
            metadata.load(input);
        }
        return metadata.getProperty("version");
    }

    @Test
    void originalMigrationBytesAndTransactionalParsingRemainUnchanged() throws Exception {
        Map<String, String> expected = Map.of(
                "V1__baseline_schema.sql", "602f6581b854e23cb66cda3fb5d9cd95325c9f43accea44be017186d697a9e4d",
                "V2__add_training_session_owner.sql", "0b2a08945d19d916a31c4aede77c13f80eb553f56cb9e2036416a6ad0a82b00c",
                "V3__require_training_session_owner.sql", "d611f77d37728318a0333f090608414c61a36210a5005a2edf622b960a96d06f",
                "V4__add_training_session_creation_idempotency.sql", "d23ef6a1f55445c645732e4301bd1a140d53bf72be58d00e3d13a209eb96eecc");
        ClassicConfiguration config = new ClassicConfiguration();
        config.setPlaceholderReplacement(false);
        Map<String, Integer> originalChecksums = Map.of(
                "V1__baseline_schema.sql", 1495888521,
                "V2__add_training_session_owner.sql", -256376701,
                "V3__require_training_session_owner.sql", 1159973059,
                "V4__add_training_session_creation_idempotency.sql", 257425531);
        int statements = 0;
        for (var entry : expected.entrySet()) {
            Path path = Path.of("src/main/resources/db/migration", entry.getKey());
            assertThat(HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(Files.readAllBytes(path)))).isEqualTo(entry.getValue());
            assertThat(ChecksumCalculator.calculate(
                    new FileSystemResource(null, path.toString(), StandardCharsets.UTF_8, false)))
                    .isEqualTo(originalChecksums.get(entry.getKey()));
            var parser = new PostgreSQLParser(config, new ParsingContext());
            try (var iterator = parser.parse(new FileSystemResource(null, path.toString(), StandardCharsets.UTF_8, false))) {
                while (iterator.hasNext()) {
                    assertThat(iterator.next().canExecuteInTransaction()).isTrue();
                    statements++;
                }
            }
        }
        assertThat(statements).isEqualTo(26);
    }
}
