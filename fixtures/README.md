# Fixtures

Fixtures públicas e golden files exclusivamente sintéticos.

Nenhum PDF, texto extraído, saldo, posição, identificador ou senha real pode ser armazenado neste diretório.

Os PDFs não são versionados: são gerados em tempo de teste a partir de definições legíveis em Kotlin, e o que fica aqui são os goldens em texto. Ver [`inter-position/README.md`](inter-position/README.md) para os casos, o processo de descoberta privada e como regenerar.

`FixturePrivacyTests` varre este diretório automaticamente e falha se encontrar dado com forma de PII ou arquivo binário.
