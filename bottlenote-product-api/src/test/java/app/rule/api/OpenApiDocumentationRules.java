package app.rule.api;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import app.rule.AbstractRules;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("rule")
@DisplayName("OpenAPI 문서 아키텍처 규칙")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
public class OpenApiDocumentationRules extends AbstractRules {

  @Test
  public void Operation_파라미터는_스키마를_명시한다() {
    ArchRule rule =
        classes()
            .that()
            .areAnnotations()
            .and()
            .areAnnotatedWith(Operation.class)
            .should(declareParameterSchemas())
            .because("@Operation 파라미터는 유효한 OpenAPI 문서를 위해 schema, array, content 또는 ref를 선언해야 합니다");

    rule.check(importedClasses);
  }

  private ArchCondition<JavaClass> declareParameterSchemas() {
    return new ArchCondition<>("@Operation의 모든 파라미터에 스키마를 선언한다") {
      @Override
      public void check(JavaClass javaClass, ConditionEvents events) {
        Operation operation = javaClass.getAnnotationOfType(Operation.class);
        Arrays.stream(operation.parameters())
            .filter(parameter -> !declaresSchema(parameter))
            .forEach(
                parameter ->
                    events.add(
                        SimpleConditionEvent.violated(
                            javaClass,
                            javaClass.getName()
                                + "의 @Parameter(name = \""
                                + parameter.name()
                                + "\")에 스키마 선언이 없습니다")));
      }
    };
  }

  private boolean declaresSchema(Parameter parameter) {
    return !parameter.ref().isBlank()
        || parameter.content().length > 0
        || declaresSchema(parameter.schema())
        || declaresSchema(parameter.array().schema());
  }

  private boolean declaresSchema(Schema schema) {
    return schema.implementation() != Void.class
        || !schema.type().isBlank()
        || !schema.ref().isBlank();
  }
}
