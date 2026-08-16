package app.rule.api

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.domain.JavaParameterizedType
import com.tngtech.archunit.core.domain.JavaWildcardType
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Schema
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * 컨트롤러 응답 본문 타입 명시 규칙을 검증한다.
 *
 * bottlenote-product-api의 ControllerLayerRules#컨트롤러_응답_타입_명시_검증과 같은 declareResponseBodyType 조건을 재사용한다.
 * app.bottlenote 패키지를 임포트하면 admin-api 테스트 런타임 클래스패스에 함께 올라오는 admin 자체 컨트롤러와
 * mono의 공유 클래스가 모두 검사 대상이 된다.
 */
@Tag("rule")
@DisplayName("Admin API 컨트롤러 응답 타입 규칙")
class ControllerLayerRules {

	companion object {
		// 임포트는 JVM당 한 번이면 충분하다. 규칙 검사는 임포트 결과를 변경하지 않는다.
		private val IMPORTED_CLASSES: JavaClasses = ClassFileImporter().importPackages("app.bottlenote")
	}

	private val importedClasses: JavaClasses = IMPORTED_CLASSES

	@Test
	fun openApiParametersDeclareSchemas() {
		val rule: ArchRule = classes()
			.that()
			.areAnnotations()
			.and()
			.areAnnotatedWith(Operation::class.java)
			.should(declareOpenApiParameterSchemas())
			.because("@Operation 파라미터는 schema, array, content 또는 ref를 선언해야 합니다")

		rule.check(importedClasses)
	}

	@Test
	fun controllersDeclareResponseBodyTypes() {
		val rule: ArchRule = methods()
			.that()
			.arePublic()
			.and()
			.areDeclaredInClassesThat(respondToClients())
			.should(declareResponseBodyType())
			.because("응답 본문 타입은 API 문서 생성의 근거이므로 ResponseEntity<?> 대신 실제 타입을 선언해야 합니다")

		rule.check(importedClasses)
	}

	private fun declareOpenApiParameterSchemas(): ArchCondition<JavaClass> = object : ArchCondition<JavaClass>("@Operation의 모든 파라미터에 스키마를 선언한다") {
		override fun check(javaClass: JavaClass, events: ConditionEvents) {
			val operation = javaClass.getAnnotationOfType(Operation::class.java)
			operation.parameters
				.filter { parameter -> !declaresSchema(parameter) }
				.forEach { parameter ->
					events.add(
						SimpleConditionEvent.violated(
							javaClass,
							"${javaClass.name}의 @Parameter(name = \"${parameter.name}\")에 스키마 선언이 없습니다"
						)
					)
				}
		}
	}

	private fun declaresSchema(parameter: Parameter): Boolean = parameter.ref.isNotBlank() ||
		parameter.content.isNotEmpty() ||
		declaresSchema(parameter.schema) ||
		declaresSchema(parameter.array.schema)

	private fun declaresSchema(schema: Schema): Boolean = schema.implementation != Void::class.java || schema.type.isNotBlank() || schema.ref.isNotBlank()

	/** 클라이언트에게 응답 본문을 직접 내보내는 클래스. */
	private fun respondToClients(): DescribedPredicate<JavaClass> = object : DescribedPredicate<JavaClass>("@RestController 또는 @RestControllerAdvice 로 선언된") {
		override fun test(javaClass: JavaClass): Boolean = javaClass.isAnnotatedWith(RestController::class.java) ||
			javaClass.isAnnotatedWith(RestControllerAdvice::class.java)
	}

	private fun declareResponseBodyType(): ArchCondition<JavaMethod> = object : ArchCondition<JavaMethod>("ResponseEntity의 본문 타입을 구체적으로 선언한다") {
		override fun check(method: JavaMethod, events: ConditionEvents) {
			if (!method.rawReturnType.isAssignableTo(ResponseEntity::class.java)) {
				return
			}
			val returnType = method.returnType
			if (returnType !is JavaParameterizedType) {
				events.add(
					SimpleConditionEvent.violated(
						method,
						"${method.fullName} 이(가) 타입 인자 없는 ResponseEntity 를 반환합니다. 본문 타입을 선언하세요"
					)
				)
				return
			}
			val hasWildcard = returnType.actualTypeArguments.any { it is JavaWildcardType }
			if (hasWildcard) {
				events.add(
					SimpleConditionEvent.violated(
						method,
						"${method.fullName} 이(가) ResponseEntity<?> 를 반환합니다. 실제 본문 타입을 선언하세요"
					)
				)
			} else {
				events.add(SimpleConditionEvent.satisfied(method, "응답 본문 타입을 선언했습니다"))
			}
		}
	}
}
