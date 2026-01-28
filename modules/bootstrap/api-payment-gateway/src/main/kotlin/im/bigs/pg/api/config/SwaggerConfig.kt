package im.bigs.pg.api.config

import com.fasterxml.jackson.databind.ObjectMapper
import im.bigs.pg.api.payment.dto.CreatePaymentRequest
import im.bigs.pg.api.payment.dto.MockPgCardData
import im.bigs.pg.api.payment.dto.NewPgCardData
import im.bigs.pg.api.payment.dto.PgCardData
import im.bigs.pg.api.payment.dto.TestPgCardData
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.models.examples.Example
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

/**
 * Swagger 설정.
 *
 * [CreatePaymentRequest]에 대한 PG별 예제를 `@Schema(example = ...)` 값으로 동적 생성합니다. 하드코딩된 JSON 대신 DTO 클래스의
 * 어노테이션을 기반으로 예제를 자동 생성하여 코드와 문서의 일관성을 유지합니다.
 *
 * ## 동작 방식
 * 1. [OpenApiCustomizer]를 통해 OpenAPI 스펙 후처리
 * 2. [PgCardData] 구현체들의 `@Schema(example)` 값을 리플렉션으로 추출
 * 3. [ObjectMapper]로 JSON 직렬화하여 Swagger UI에 예제 추가
 *
 * @property objectMapper JSON 직렬화에 사용되는 ObjectMapper
 * @see PgCardData
 * @see CreatePaymentRequest
 */
@Configuration
class SwaggerConfig(
    private val objectMapper: ObjectMapper,
) {

    /**
     * 결제 생성 API에 PG별 예제를 주입하는 [OpenApiCustomizer] 빈.
     *
     * POST /api/v1/payments 엔드포인트의 requestBody에 MOCK, TEST_PG, NEW_PG 예제를 동적으로 추가합니다.
     */
    @Bean
    fun createPaymentExamplesCustomizer(): OpenApiCustomizer = OpenApiCustomizer { openApi ->
        val examples = buildPaymentExamples()

        openApi.paths
            ?.get("/api/v1/payments")
            ?.post
            ?.requestBody
            ?.content
            ?.get("application/json")
            ?.let { mediaType ->
                examples.forEach { (name, example) -> mediaType.addExamples(name, example) }
            }
    }

    /**
     * PG별 [CreatePaymentRequest] 예제를 생성합니다.
     *
     * @return 예제 이름(MOCK, TEST_PG, NEW_PG)과 [Example] 객체의 맵
     */
    private fun buildPaymentExamples(): Map<String, Example> {
        return listOf(
            Triple("MOCK", MockPgCardData::class, 1L),
            Triple("TEST_PG", TestPgCardData::class, 2L),
            Triple("NEW_PG", NewPgCardData::class, 3L),
        )
            .associate { (name, klass, partnerId) ->
                val pgCardData = createFromSchemaExamples(klass)
                val request =
                    CreatePaymentRequest(
                        partnerId = partnerId,
                        amount = BigDecimal("1000"),
                        pgCardData = pgCardData,
                    )
                name to
                    Example().apply {
                        summary = "${name.replace("_", "")} (partnerId=$partnerId)"
                        value = objectMapper.writeValueAsString(request)
                    }
            }
    }

    /**
     * `@Schema(example = ...)` 어노테이션을 기반으로 데이터 클래스 인스턴스를 생성합니다.
     *
     * - example이 있으면 해당 값 사용
     * - example이 없고 파라미터가 optional이면 Kotlin 기본값 사용
     * - example이 없고 파라미터가 required면 타입별 기본값 사용
     *
     * @param klass 생성할 [PgCardData] 구현체 클래스
     * @return 생성된 인스턴스
     * @throws IllegalArgumentException primary constructor가 없는 경우
     */
    private fun <T : PgCardData> createFromSchemaExamples(klass: KClass<T>): T {
        val constructor =
            klass.primaryConstructor
                ?: throw IllegalArgumentException(
                    "Primary constructor not found for ${klass.simpleName}"
                )

        val args =
            constructor
                .parameters
                .mapNotNull { param ->
                    val value = getExampleValue(klass, param)
                    when {
                        value != null -> param to value
                        param.isOptional -> null
                        else -> param to getDefaultForType(param)
                    }
                }
                .toMap()

        return constructor.callBy(args)
    }

    /**
     * 파라미터의 `@Schema(example)` 값을 추출하고 적절한 타입으로 변환합니다.
     *
     * Java Reflection을 사용하여 getter 메서드의 `@get:Schema` 어노테이션에서 example 값을 가져옵니다.
     *
     * @param klass 대상 클래스
     * @param param 파라미터 정보
     * @return 변환된 값, example이 없으면 null
     */
    private fun getExampleValue(klass: KClass<*>, param: KParameter): Any? {
        val getterName = "get${param.name!!.replaceFirstChar { it.uppercase() }}"
        val getter = runCatching { klass.java.getMethod(getterName) }.getOrNull()

        val schema = getter?.getAnnotation(Schema::class.java)
        val example = schema?.example ?: ""

        if (example.isEmpty()) return null

        return convertToType(example, param)
    }

    /**
     * 문자열 값을 파라미터 타입에 맞게 변환합니다.
     *
     * 지원 타입: String, Int, Long, Double, Float, Boolean, BigDecimal, List, Set, Map
     *
     * @param value 변환할 문자열 (JSON 형식일 수 있음)
     * @param param 파라미터 정보
     * @return 변환된 값, 변환 실패 시 null
     */
    private fun convertToType(value: String, param: KParameter): Any? {
        return when (param.type.classifier) {
            String::class -> value
            Int::class -> value.toIntOrNull()
            Long::class -> value.toLongOrNull()
            Double::class -> value.toDoubleOrNull()
            Float::class -> value.toFloatOrNull()
            Boolean::class -> value.toBooleanStrictOrNull()
            BigDecimal::class -> runCatching { BigDecimal(value) }.getOrNull()
            List::class ->
                runCatching { objectMapper.readValue(value, List::class.java) }.getOrNull()
            Set::class -> runCatching { objectMapper.readValue(value, Set::class.java) }.getOrNull()
            Map::class -> runCatching { objectMapper.readValue(value, Map::class.java) }.getOrNull()
            else -> value
        }
    }

    /**
     * 파라미터 타입에 맞는 기본값을 반환합니다.
     *
     * `@Schema(example)` 어노테이션이 없는 required 파라미터에 사용됩니다.
     *
     * @param param 파라미터 정보
     * @return 타입별 기본값
     */
    private fun getDefaultForType(param: KParameter): Any {
        return when (param.type.classifier) {
            String::class -> ""
            Int::class -> 0
            Long::class -> 0L
            Double::class -> 0.0
            Float::class -> 0.0f
            Boolean::class -> false
            BigDecimal::class -> BigDecimal.ZERO
            List::class -> emptyList<Any>()
            Set::class -> emptySet<Any>()
            Map::class -> emptyMap<Any, Any>()
            Array::class -> emptyArray<Any>()
            else -> ""
        }
    }
}
