package im.bigs.pg.api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate

/** HTTP 클라이언트 설정. 외부 API 호출에 사용되는 RestTemplate Bean을 등록합니다. */
@Configuration
class HttpClientConfig {

    @Bean fun restTemplate(): RestTemplate = RestTemplate()
}
