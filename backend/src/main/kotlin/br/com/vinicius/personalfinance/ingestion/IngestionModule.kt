package br.com.vinicius.personalfinance.ingestion

import org.springframework.modulith.ApplicationModule

@ApplicationModule(
    displayName = "Ingestion",
    allowedDependencies = ["shared", "audit", "portfolio"],
)
internal class IngestionModule
