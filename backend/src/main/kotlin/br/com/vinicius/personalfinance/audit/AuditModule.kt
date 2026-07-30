package br.com.vinicius.personalfinance.audit

import org.springframework.modulith.ApplicationModule

@ApplicationModule(
    displayName = "Audit",
    allowedDependencies = ["shared"],
)
internal class AuditModule
