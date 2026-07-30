package br.com.vinicius.personalfinance.portfolio

import org.springframework.modulith.ApplicationModule

@ApplicationModule(
    displayName = "Portfolio",
    allowedDependencies = ["shared", "audit"],
)
internal class PortfolioModule
