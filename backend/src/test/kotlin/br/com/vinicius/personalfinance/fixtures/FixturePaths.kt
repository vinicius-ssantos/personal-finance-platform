package br.com.vinicius.personalfinance.fixtures

import java.nio.file.Files
import java.nio.file.Path

/**
 * Locates the repository's `fixtures/` directory from wherever the tests run.
 *
 * Gradle sets the working directory to the module, so a fixed `../fixtures`
 * would work today and break the moment the module moves. Walking up until the
 * marker is found is stable against that.
 */
object FixturePaths {
    private const val MAX_DEPTH = 6

    val interPositionDirectory: Path by lazy { repositoryFixtures().resolve("inter-position") }

    private fun repositoryFixtures(): Path {
        var candidate: Path? = Path.of("").toAbsolutePath()
        repeat(MAX_DEPTH) {
            val current = candidate ?: return@repeat
            val fixtures = current.resolve("fixtures")
            if (Files.isDirectory(fixtures)) return fixtures
            candidate = current.parent
        }
        error("cannot locate the repository fixtures directory from ${Path.of("").toAbsolutePath()}")
    }
}
