package gift.architecture;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.INTERFACES;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.properties.CanBeAnnotated.Predicates.annotatedWith;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import gift.Application;
import org.springframework.data.repository.Repository;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

@AnalyzeClasses(packagesOf = Application.class, importOptions = DoNotIncludeTests.class)
class LayerDependencyTest {
    private static final DescribedPredicate<JavaClass> CONTROLLERS = resideInAPackage("gift..")
        .and(annotatedWith(Controller.class).or(annotatedWith(RestController.class)));
    private static final DescribedPredicate<JavaClass> REPOSITORIES = resideInAPackage("gift..")
        .and(INTERFACES)
        .and(assignableTo(Repository.class));

    @ArchTest
    static final ArchRule controllersDoNotDependOnRepositories = noClasses()
        .that(CONTROLLERS)
        .should().dependOnClassesThat(REPOSITORIES);

    @ArchTest
    static final ArchRule servicesDoNotDependOnControllers = noClasses()
        .that().areAnnotatedWith(Service.class)
        .should().dependOnClassesThat(CONTROLLERS);
}
