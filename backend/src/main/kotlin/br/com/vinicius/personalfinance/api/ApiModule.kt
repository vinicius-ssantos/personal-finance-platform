package br.com.vinicius.personalfinance.api

import org.springframework.modulith.ApplicationModule

@ApplicationModule(
    displayName = "API",
    allowedDependencies = ["shared", "audit", "ingestion", "portfolio"],
)
internal class ApiModule
