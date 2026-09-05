package br.com.vinicius.personalfinance

import br.com.vinicius.personalfinance.ingestion.UploadPolicy
import br.com.vinicius.personalfinance.shared.DomainClock
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Publishes the injectable clock and binds the upload policy.
 *
 * `shared` stays framework-free, so the clock bean is declared here instead of
 * being annotated on the type itself. Tests override it with a fixed clock.
 */
@Configuration
@EnableConfigurationProperties(UploadPolicy::class)
class DomainClockConfiguration {
    @Bean
    fun domainClock(): DomainClock = DomainClock.systemUtc()
}
