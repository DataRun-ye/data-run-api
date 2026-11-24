package org.nmcpye.datarun;

import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.base.DescribedPredicate.alwaysTrue;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

@AnalyzeClasses(packagesOf = DataRunApiApp.class, importOptions = DoNotIncludeTests.class)
class TechnicalStructureTest {

    // prettier-ignore
    @ArchTest
    static final ArchRule respectsTechnicalArchitectureLayers = layeredArchitecture()
        .consideringAllDependencies()
        .ignoreDependency(resideInAPackage("org.nmcpye.datarun.acl"), alwaysTrue())
        .ignoreDependency(resideInAPackage("org.nmcpye.datarun.jpa.auditing"), alwaysTrue())
        .ignoreDependency(belongToAnyOf(DataRunApiApp.class), alwaysTrue())
        .ignoreDependency(alwaysTrue(), belongToAnyOf(
            org.nmcpye.datarun.config.Constants.class,
            org.nmcpye.datarun.config.ApplicationProperties.class
        ));
}
