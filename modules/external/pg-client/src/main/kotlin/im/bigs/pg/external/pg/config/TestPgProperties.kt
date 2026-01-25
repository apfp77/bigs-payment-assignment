package im.bigs.pg.external.pg.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * TestPg API 연동 설정. application.yml에서 주입됩니다.
 *
 * @property baseUrl TestPg API 서버 주소
 * @property apiKey UUID 형식의 인증 키
 * @property iv Base64URL 인코딩된 12바이트 IV
 */
@ConfigurationProperties(prefix = "pg.test")
data class TestPgProperties(
        val baseUrl: String,
        val apiKey: String,
        val iv: String,
)
