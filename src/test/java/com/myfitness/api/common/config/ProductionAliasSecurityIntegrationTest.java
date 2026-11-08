package com.myfitness.api.common.config;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@ActiveProfiles("production")
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:production-alias;MODE=PostgreSQL;DB_CLOSE_DELAY=-1")
class ProductionAliasSecurityIntegrationTest extends ProductionSecurityIntegrationTest {}
