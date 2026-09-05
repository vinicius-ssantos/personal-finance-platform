package br.com.vinicius.personalfinance

import br.com.vinicius.personalfinance.shared.DomainClock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Publishes the injectable clock.
 *
 * `shared` stays framework-free, so the bean is declared here instead of being
 * annotated on the type itself. Tests override it with a fixed clock.
 */
@Configuration
class DomainClockConfiguration {
    @Bean
    fun domainClock(): DomainClock = DomainClock.systemUtc()
}
