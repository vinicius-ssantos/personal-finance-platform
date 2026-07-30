package br.com.vinicius.personalfinance.persistence

import org.springframework.modulith.ApplicationModule

@ApplicationModule(
    displayName = "Persistence",
    allowedDependencies = ["shared", "audit", "ingestion", "portfolio"],
)
internal class PersistenceModule
