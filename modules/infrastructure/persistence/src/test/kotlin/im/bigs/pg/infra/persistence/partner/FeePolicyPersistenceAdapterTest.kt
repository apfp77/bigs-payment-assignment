package im.bigs.pg.infra.persistence.partner

import im.bigs.pg.infra.persistence.config.JpaConfig
import im.bigs.pg.infra.persistence.partner.adapter.FeePolicyPersistenceAdapter
import im.bigs.pg.infra.persistence.partner.entity.FeePolicyEntity
import im.bigs.pg.infra.persistence.partner.repository.FeePolicyJpaRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@DataJpaTest
@ContextConfiguration(classes = [JpaConfig::class])
@Import(FeePolicyPersistenceAdapter::class)
class FeePolicyPersistenceAdapterTest
@Autowired
constructor(
    private val adapter: FeePolicyPersistenceAdapter,
    private val repository: FeePolicyJpaRepository,
) {

    @Test
    @DisplayName("effective_from 기준 최신 정책이 적용되어야 한다")
    fun `effective_from 기준 최신 정책이 적용되어야 한다`() {
        // Given: 동일 partnerId에 여러 정책 존재
        val partnerId = 1L
        repository.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0200"),
                fixedFee = BigDecimal("50"),
            )
        )
        val newPolicy =
            repository.save(
                FeePolicyEntity(
                    partnerId = partnerId,
                    effectiveFrom = Instant.parse("2024-01-01T00:00:00Z"),
                    percentage = BigDecimal("0.0300"),
                    fixedFee = BigDecimal("100"),
                )
            )
        repository.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2030-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0500"),
                fixedFee = BigDecimal("200"),
            )
        )

        // When: 2025년 시점으로 조회
        val queryTime = LocalDateTime.of(2025, 6, 1, 12, 0, 0)
        val result = adapter.findEffectivePolicy(partnerId, queryTime)

        // Then: 2024년 정책이 반환 (2025년 이전 중 가장 최신)
        assertNotNull(result)
        assertEquals(newPolicy.id, result.id)
        assertEquals(BigDecimal("0.0300"), result.percentage)
        assertEquals(BigDecimal("100"), result.fixedFee)
    }

    @Test
    @DisplayName("현재 시점 이전 정책이 없으면 null 반환")
    fun `현재 시점 이전 정책이 없으면 null 반환`() {
        // Given: 미래 정책만 존재
        val partnerId = 2L
        repository.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2030-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0500"),
                fixedFee = null,
            )
        )

        // When: 현재 시점으로 조회
        val queryTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0)
        val result = adapter.findEffectivePolicy(partnerId, queryTime)

        // Then: null 반환
        assertNull(result)
    }

    @Test
    @DisplayName("다른 partnerId의 정책은 조회되지 않아야 한다")
    fun `다른 partnerId의 정책은 조회되지 않아야 한다`() {
        // Given: partnerId=1, 2 각각 정책 존재
        repository.save(
            FeePolicyEntity(
                partnerId = 1L,
                effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0200"),
                fixedFee = null,
            )
        )
        repository.save(
            FeePolicyEntity(
                partnerId = 2L,
                effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0400"),
                fixedFee = null,
            )
        )

        // When: partnerId=1 조회
        val queryTime = LocalDateTime.of(2025, 1, 1, 0, 0, 0)
        val result = adapter.findEffectivePolicy(1L, queryTime)

        // Then: partnerId=1의 정책만 반환
        assertNotNull(result)
        assertEquals(1L, result.partnerId)
        assertEquals(BigDecimal("0.0200"), result.percentage)
    }
}
