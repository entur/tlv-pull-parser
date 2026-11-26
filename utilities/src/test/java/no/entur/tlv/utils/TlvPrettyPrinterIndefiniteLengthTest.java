package no.entur.tlv.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * The indefinite length form (length byte '80', with the content terminated by '00 00') is not used by ISO/IEC 7816-4.
 */

public class TlvPrettyPrinterIndefiniteLengthTest {

	private static final TlvPrettyPrinter PRETTY_PRINTER = TlvPrettyPrinter.newBuilder().build();

	@Test
	public void testTlvWithIndefiniteLengthIsMalformed() {
		// 5A 01 01, then 30 with indefinite length, containing 02 01 05
		String prettyPrinted = PRETTY_PRINTER.tlv(HexFormat.of().parseHex("5A0101" + "3080" + "020105" + "0000")).toString();

		assertTrue(prettyPrinted.contains("5A 01"), prettyPrinted);
		assertFalse(prettyPrinted.contains("02 01"), prettyPrinted);
	}

	@Test
	public void testTagLengthWithIndefiniteLengthIsMalformed() {
		byte[] tagLengths = HexFormat.of().parseHex("9F0206" + "9F0380" + "9F1A02");

		StringBuilder output = new StringBuilder();
		PRETTY_PRINTER.tl(tagLengths, 0, tagLengths.length, output, 0, 0);
		String prettyPrinted = output.toString();

		assertTrue(prettyPrinted.contains("9F 02 06"), prettyPrinted);
		assertFalse(prettyPrinted.contains("9F 1A"), prettyPrinted);
	}

	@Test
	public void testTagLengthWithExternalValueWithIndefiniteLengthIsMalformed() {
		byte[] tagLengths = HexFormat.of().parseHex("9F0201" + "9F0380" + "9F1A01");
		byte[] values = HexFormat.of().parseHex("4243");

		StringBuilder output = new StringBuilder();
		PRETTY_PRINTER.tagLengthWithExternalValue(tagLengths, 0, tagLengths.length, output, 0, 0, values, 0);
		String prettyPrinted = output.toString();

		assertTrue(prettyPrinted.contains("42"), prettyPrinted);
		assertFalse(prettyPrinted.contains("9F 1A"), prettyPrinted);
	}

}
