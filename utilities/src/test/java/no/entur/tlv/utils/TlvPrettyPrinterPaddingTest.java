package no.entur.tlv.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * ISO/IEC 7816-4: '00' and 'FF' bytes without any meaning may occur before, between and after data objects.
 */

public class TlvPrettyPrinterPaddingTest {

	private static final TlvPrettyPrinter PRETTY_PRINTER = TlvPrettyPrinter.newBuilder()
			.withTag(ByteArrayKey.create("5A"), "Application PAN")
			.withTag(ByteArrayKey.create("9F02"), "Amount")
			.build();

	@Test
	public void testTlvPadding() {
		String prettyPrinted = PRETTY_PRINTER.tlv(HexFormat.of().parseHex("00FF" + "5A0101" + "FF00")).toString();

		assertTrue(prettyPrinted.startsWith("5A 01 -- Application PAN"), prettyPrinted);
		assertFalse(prettyPrinted.contains("FF"), prettyPrinted);
	}

	@Test
	public void testTagLengthPadding() {
		byte[] tagLengths = HexFormat.of().parseHex("00FF" + "9F0206" + "FF00");

		StringBuilder output = new StringBuilder();
		PRETTY_PRINTER.tl(tagLengths, 0, tagLengths.length, output, 0, 0);
		String prettyPrinted = output.toString();

		assertTrue(prettyPrinted.startsWith("9F 02 06 -- Amount"), prettyPrinted);
		assertFalse(prettyPrinted.contains("FF"), prettyPrinted);
	}

	@Test
	public void testTagLengthWithExternalValuePadding() {
		byte[] tagLengths = HexFormat.of().parseHex("00FF" + "9F0201" + "FF00");
		byte[] values = HexFormat.of().parseHex("42");

		StringBuilder output = new StringBuilder();
		PRETTY_PRINTER.tagLengthWithExternalValue(tagLengths, 0, tagLengths.length, output, 0, 0, values, 0);
		String prettyPrinted = output.toString();

		assertTrue(prettyPrinted.startsWith("9F 02 01 -- Amount:"), prettyPrinted);
		assertTrue(prettyPrinted.contains("42"), prettyPrinted);
	}

}
