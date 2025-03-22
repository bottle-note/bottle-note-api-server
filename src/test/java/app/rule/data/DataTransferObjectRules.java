package app.rule.data;

import app.rule.AbstractRules;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@Tag("rule")
@DisplayName("데이터 전송 객체(DTO) 아키텍처 규칙")
@SuppressWarnings({"NonAsciiCharacters", "JUnitTestClassNamingConvention"})
public class DataTransferObjectRules extends AbstractRules {
	/**
	 * 요청 DTO 네이밍 규칙 검증
	 * 요청 DTO 클래스는 이름이 'Request'로 끝나야 합니다.
	 */
	@Test
	@Disabled("테스트를 위해 비활성화")
	public void 요청_DTO_네이밍_규칙_검증() {
		ArchRule rule = classes()
			.that().resideInAPackage("..dto.request..")
			.should().haveSimpleNameEndingWith("Request")
			.because("요청 DTO 클래스는 명확한 식별을 위해 'Request'로 끝나야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 응답 DTO 네이밍 규칙 검증
	 * 응답 DTO 클래스는 이름이 'Response'로 끝나야 합니다.
	 */
	@Test
	@Disabled("테스트를 위해 비활성화")
	public void 응답_DTO_네이밍_규칙_검증() {
		ArchRule rule = classes()
			.that().resideInAPackage("..dto.response..")
			.should().haveSimpleNameEndingWith("Response")
			.because("응답 DTO 클래스는 명확한 식별을 위해 'Response'로 끝나야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 액션 용어 표준화 검증
	 * DTO 클래스 이름은 표준화된 액션 용어(Create, Update, Delete, Search, List, Detail)를 포함해야 합니다.
	 */
	@Test
	@Disabled("테스트를 위해 비활성화")
	public void 액션_용어_표준화_검증() {
		List<String> standardActions = Arrays.asList("Create", "Update", "Delete", "Search", "List", "Detail");

		ArchRule rule = classes()
			.that().haveSimpleNameEndingWith("Request").or().haveSimpleNameEndingWith("Response")
			.and().resideInAnyPackage("..dto.request..", "..dto.response..")
			.should(new ArchCondition<JavaClass>("follow standardized action terminology") {
				@Override
				public void check(JavaClass javaClass, ConditionEvents events) {
					String className = javaClass.getSimpleName();

					// Request나 Response 접미사 제거
					String baseClassName = className.endsWith("Request")
						? className.substring(0, className.length() - "Request".length())
						: className.substring(0, className.length() - "Response".length());

					boolean containsStandardAction = standardActions.stream()
						.anyMatch(baseClassName::contains);

					if (containsStandardAction) {
						events.add(SimpleConditionEvent.satisfied(javaClass,
							javaClass.getSimpleName() + " follows standardized action terminology"));
					} else {
						events.add(SimpleConditionEvent.violated(javaClass,
							javaClass.getSimpleName() + " does not follow standardized action terminology"));
					}
				}
			})
			.because("DTO 클래스 이름은 표준화된 액션 용어(Create, Update, Delete, Search, List, Detail)를 포함해야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 검색 조건 DTO 네이밍 규칙 검증
	 * 검색 조건 DTO 클래스는 이름이 'Criteria'로 끝나야 합니다.
	 */
	@Test
	public void 검색_조건_DTO_네이밍_규칙_검증() {
		ArchRule rule = classes()
			.that().resideInAPackage("..dto.dsl..")
			.should().haveSimpleNameEndingWith("Criteria")
			.because("검색 조건 DTO 클래스는 명확한 식별을 위해 'Criteria'로 끝나야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 요청 DTO 패키지 위치 검증
	 * 모든 요청 DTO 클래스는 '.dto.request' 패키지에 위치해야 합니다.
	 */
	@Test
	public void 요청_DTO_패키지_위치_검증() {
		ArchRule rule = classes()
			.that().haveSimpleNameEndingWith("Request")
			.and().doNotHaveSimpleName("GlobalResponse")
			.and().doNotHaveSimpleName("PageResponse")
			.should().resideInAPackage("..dto.request..")
			.because("요청 DTO 클래스는 구조적 일관성을 위해 '.dto.request' 패키지에 위치해야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 응답 DTO 패키지 위치 검증
	 * 모든 응답 DTO 클래스는 '.dto.response' 패키지에 위치해야 합니다.
	 */
	@Test
	public void 응답_DTO_패키지_위치_검증() {
		ArchRule rule = classes()
			.that().haveSimpleNameEndingWith("Response")
			.and().doNotHaveSimpleName("GlobalResponse")
			.and().doNotHaveSimpleName("PageResponse")
			.should().resideInAPackage("..dto.response..")
			.because("응답 DTO 클래스는 구조적 일관성을 위해 '.dto.response' 패키지에 위치해야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * 검색 조건 DTO 패키지 위치 검증
	 * 모든 검색 조건 DTO 클래스는 '.dto.dsl' 패키지에 위치해야 합니다.
	 */
	@Test
	public void 검색_조건_DTO_패키지_위치_검증() {
		ArchRule rule = classes()
			.that().haveSimpleNameEndingWith("Criteria")
			.or().haveSimpleNameEndingWith("criteria")
			.should().resideInAPackage("..dto.dsl..")
			.because("검색 조건 DTO 클래스는 구조적 일관성을 위해 '.dto.dsl' 패키지에 위치해야 합니다");

		rule.check(importedClasses);
	}

	/**
	 * DTO-엔티티 분리 검증
	 * DTO 클래스는 엔티티를 직접 참조하지 않아야 합니다.
	 */
	@Test
	public void DTO_엔티티_분리_검증() {
		ArchRule rule = noClasses()
			.that().resideInAnyPackage("..dto..")
			.should().dependOnClassesThat()
			.areAnnotatedWith(Entity.class)
			.because("DTO 클래스는 엔티티를 직접 참조하지 않아야 합니다");
		rule.check(importedClasses);
	}
}
