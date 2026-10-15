package com.altneb;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModuleArchitectureTest {

    private static final String[] DOMAIN_PACKAGES = {
        "com.altneb.identity..",
        "com.altneb.account..",
        "com.altneb.order..",
        "com.altneb.execution..",
        "com.altneb.portfolio..",
        "com.altneb.ledger..",
        "com.altneb.market..",
        "com.altneb.outbox.."
    };

    @Test
    void springModulesHaveNoCyclesOrIllegalDependencies() {
        var modules = ApplicationModules.of(AltnebApplication.class);

        modules.verify();
        assertThat(modules.stream().map(module -> module.getIdentifier().toString()))
                .containsExactlyInAnyOrder(
                        "identity",
                        "account",
                        "order",
                        "execution",
                        "portfolio",
                        "ledger",
                        "market",
                        "outbox",
                        "platform.error",
                        "platform.web",
                        "platform.security",
                        "platform.persistence",
                        "platform.messaging",
                        "platform.observability");
    }

    @Test
    void platformDoesNotDependOnDomainModules() {
        var classes = new ClassFileImporter().importPackages("com.altneb");

        noClasses()
                .that().resideInAPackage("com.altneb.platform..")
                .should().dependOnClassesThat().resideInAnyPackage(DOMAIN_PACKAGES)
                .check(classes);
    }
}
