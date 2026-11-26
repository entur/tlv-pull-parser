package no.entur.tlv.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HexFormat;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Pretty print responses of real EMV cards (see the parser module's EmvCardResponsesTest).
 */

public class TlvPrettyPrinterEmvCardResponsesTest {

	// the responses are shared with the parser module
	private static final Path DIRECTORY = Paths.get("..", "parser", "src", "test", "resources", "emv");

	private static final TlvPrettyPrinter PRETTY_PRINTER = TlvPrettyPrinter.newBuilder().build();

	static Stream<Path> files() throws IOException {
		try (Stream<Path> files = Files.list(DIRECTORY)) {
			return files.filter(f -> f.toString().endsWith(".txt")).sorted().toList().stream();
		}
	}

	@ParameterizedTest
	@MethodSource("files")
	public void testPrettyPrint(Path file) throws IOException {
		int index = 0;
		for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
			if (line.startsWith("#") || line.isBlank()) {
				continue;
			}
			byte[] response = HexFormat.of().parseHex(line.split(" ")[1]);

			String prettyPrinted = PRETTY_PRINTER.tlv(response, 0, response.length - 2).toString();

			String message = file.getFileName() + " response " + index++;
			assertFalse(prettyPrinted.isEmpty(), message);
			assertFalse(prettyPrinted.contains("[malformed length]"), message);
		}
	}

}
