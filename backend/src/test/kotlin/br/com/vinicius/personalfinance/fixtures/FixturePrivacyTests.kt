package br.com.vinicius.personalfinance.fixtures

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import java.nio.file.Files
import java.nio.file.Path
import kotlin.streams.asSequence

/** Automated guard against real data reaching the public fixture pack. */
class FixturePrivacyTests {
    private data class Forbidden(
        val name: String,
        val pattern: Regex,
    )

    private val forbidden =
        listOf(
            Forbidden("CPF", Regex("""\d{3}\.\d{3}\.\d{3}-\d{2}""")),
            Forbidden("CNPJ", Regex("""\d{2}\.\d{3}\.\d{3}/\d{4}-\d{2}""")),
            Forbidden("e-mail", Regex("""[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}""")),
            Forbidden("telefone", Regex("""\(\d{2}\)\s?9?\d{4}-\d{4}""")),
            Forbidden("cartão", Regex("""\b(?:\d{4}[ -]?){3}\d{4}\b""")),
            Forbidden("chave PIX aleatória", Regex("""\b[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-""")),
            Forbidden(
                "segredo atribuído",
                Regex(
                    """(?i)\b(api[_-]?key|secret|token|senha|password)\b\s*[:=]\s*["']?[A-Za-z0-9]{12,}""",
                ),
            ),
        )

    private fun fixtureFiles(): List<Path> =
        Files.walk(FixturePaths.interPositionDirectory).use { paths ->
            paths.asSequence().filter { Files.isRegularFile(it) }.toList()
        }

    @TestFactory
    fun `no committed fixture file carries personal data`(): List<DynamicTest> =
        fixtureFiles().map { file ->
            DynamicTest.dynamicTest(file.fileName.toString()) {
                val content = Files.readString(file)
                forbidden.forEach { rule ->
                    val hit = rule.pattern.find(content)
                    assertTrue(
                        hit == null,
                        "${file.fileName} looks like it contains a ${rule.name}",
                    )
                }
            }
        }

    @Test
    fun `the generated fixture text carries no personal data either`() {
        InterPositionFixture.all.forEach { case ->
            val text = case.lines.joinToString("\n")
            forbidden.forEach { rule ->
                assertTrue(
                    rule.pattern.find(text) == null,
                    "fixture case ${case.name} looks like it contains a ${rule.name}",
                )
            }
        }
    }

    @Test
    fun `no PDF or archive is committed under fixtures`() {
        val binaries =
            fixtureFiles()
                .map { it.fileName.toString() }
                .filter { name ->
                    listOf(".pdf", ".zip", ".xlsx", ".ofx", ".csv").any { name.endsWith(it) }
                }

        assertTrue(binaries.isEmpty(), "only text goldens belong here, found: $binaries")
    }

    @Test
    fun `the account number is obviously fabricated`() {
        val account =
            InterPositionFixture.complete.lines.first { line -> line.contains("/ Conta ") }

        assertTrue(
            account.contains("00000000"),
            "a fixture account must be visibly fake, got: $account",
        )
    }

    @Test
    fun `the holder identity is visibly synthetic`() {
        assertTrue(
            InterPositionFixture.complete.lines.contains("TITULAR FICTÍCIO"),
            "a fixture must never carry a real holder identity",
        )
    }

    @Test
    fun `the fixture password announces that it is test-only`() {
        assertTrue(
            InterPositionFixture.TEST_PASSWORD.contains("fixture"),
            "the test password must be self-evidently not a real one",
        )
    }

    @Test
    fun `ticker symbols are placeholders rather than real listed companies`() {
        val tickerLine = Regex("""^[A-Z]{4}(?:\d{1,2})?$""")
        val tickers =
            InterPositionFixture.complete.lines.filter { line -> tickerLine.matches(line) }

        assertTrue(tickers.isNotEmpty(), "the equity sections should carry placeholder tickers")
        tickers.forEach { ticker ->
            assertTrue(
                ticker.take(4).toSet().size == 1,
                "a fixture ticker must be a repeated-letter placeholder, got: $ticker",
            )
        }
    }
}
