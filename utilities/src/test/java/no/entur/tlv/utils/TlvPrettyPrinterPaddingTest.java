package no.entur.tlv.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * '00' bytes without any meaning may occur before, between and after data objects (EMV Book 3, Annex B).
 * 'FF' is not padding: it is the first byte of a (private class, constructed) tag, like the EMV tags FF8105 or FF60.
 */

public class TlvPrettyPrinterPaddingTest {

	private static final TlvPrettyPrinter PRETTY_PRINTER = TlvPrettyPrinter.newBuilder()
			.withTag(ByteArrayKey.create("5A"), "Application PAN")
			.withTag(ByteArrayKey.create("9F02"), "Amount")
			.withTag(ByteArrayKey.create("FF8105"), "Data Record")
			.build();

	private static byte[] hex(String hex) {
		return HexFormat.of().parseHex(hex);
	}

	private static String tlv(String hex) {
		return PRETTY_PRINTER.tlv(hex(hex)).toString();
	}

	private static String tl(String hex) {
		byte[] tagLengths = hex(hex);

		StringBuilder output = new StringBuilder();
		PRETTY_PRINTER.tl(tagLengths, 0, tagLengths.length, output, 0, 0);
		return output.toString();
	}

	private static String tagLengthWithExternalValue(String hex, String valuesHex) {
		byte[] tagLengths = hex(hex);

		StringBuilder output = new StringBuilder();
		PRETTY_PRINTER.tagLengthWithExternalValue(tagLengths, 0, tagLengths.length, output, 0, 0, hex(valuesHex), 0);
		return output.toString();
	}

	// 00 padding: the tag starts after the padding

	@Test
	public void testTlvPadding() {
		String expected = "5A 01 -- Application PAN \n"
				+ "      01 \n";

		assertEquals(expected, tlv("5A0101"));
		assertEquals(expected, tlv("00" + "5A0101"));
		assertEquals(expected, tlv("0000" + "5A0101" + "00"));
		assertEquals(expected + expected, tlv("5A0101" + "0000" + "5A0101" + "0000"));
	}

	@Test
	public void testTlvPaddingInChildPayload() {
		String expected = "70 05 \n"
				+ "      5A 01 -- Application PAN \n"
				+ "            01 \n";

		assertEquals(expected, tlv("7005" + "00" + "5A0101" + "00"));
	}

	@Test
	public void testTlvPaddingOnly() {
		assertEquals("", tlv("00"));
		assertEquals("", tlv("0000"));
	}

	@Test
	public void testTagLengthPadding() {
		String expected = "9F 02 06 -- Amount\n";

		assertEquals(expected, tl("9F0206"));
		assertEquals(expected, tl("00" + "9F0206"));
		assertEquals(expected, tl("0000" + "9F0206" + "00"));
		assertEquals(expected + "5A 08 -- Application PAN\n", tl("9F0206" + "0000" + "5A08" + "0000"));
	}

	@Test
	public void testTagLengthPaddingOnly() {
		assertEquals("", tl("00"));
		assertEquals("", tl("0000"));
	}

	@Test
	public void testTagLengthWithExternalValuePadding() {
		String expected = "9F 02 01 -- Amount:\n"
				+ "         42\n";

		assertEquals(expected, tagLengthWithExternalValue("9F0201", "42"));
		assertEquals(expected, tagLengthWithExternalValue("00" + "9F0201", "42"));
		assertEquals(expected, tagLengthWithExternalValue("0000" + "9F0201" + "00", "42"));
		assertEquals(expected + "5A 01 -- Application PAN:\n      43\n", tagLengthWithExternalValue("9F0201" + "0000" + "5A01" + "0000", "4243"));
	}

	@Test
	public void testTagLengthWithExternalValuePaddingOnly() {
		assertEquals("", tagLengthWithExternalValue("00", "42"));
		assertEquals("", tagLengthWithExternalValue("0000", "42"));
	}

	// FF is the first byte of a tag

	@Test
	public void testTlvTagStartingWithFF() {
		String dataRecord = "FF 81 05 03 -- Data Record \n"
				+ "            5A 01 -- Application PAN \n"
				+ "                  AA \n";

		assertEquals(dataRecord, tlv("FF810503" + "5A01AA"));

		String pan = "5A 01 -- Application PAN \n"
				+ "      01 \n";

		assertEquals(pan + dataRecord + pan, tlv("5A0101" + "FF810503" + "5A01AA" + "5A0101"));
	}

	@Test
	public void testTlvTagStartingWithFFAfterPadding() {
		String dataRecord = "FF 81 05 03 -- Data Record \n"
				+ "            5A 01 -- Application PAN \n"
				+ "                  AA \n";

		assertEquals(dataRecord, tlv("00" + "FF810503" + "5A01AA" + "00"));
	}

	@Test
	public void testTlvTwoByteTagStartingWithFF() {
		// constructed
		String expected = "FF 60 03 \n"
				+ "         5A 01 -- Application PAN \n"
				+ "               AA \n";

		assertEquals(expected, tlv("FF6003" + "5A01AA"));
	}

	@Test
	public void testTlvIncompleteTagStartingWithFF() {
		String pan = "5A 01 -- Application PAN \n"
				+ "      01 \n";

		for(String incomplete : new String[] {"FF", "FFFF"}) {
			assertEquals("", tlv(incomplete), incomplete);
			assertEquals(pan, tlv("5A0101" + incomplete), incomplete);
		}
	}

	@Test
	public void testTagLengthTagStartingWithFF() {
		assertEquals("9F 02 06 -- Amount\nFF 81 05 20 -- Data Record\n", tl("9F0206" + "FF810520"));
	}

	@Test
	public void testTagLengthTagStartingWithFFAfterPadding() {
		assertEquals("FF 81 05 20 -- Data Record\n9F 02 06 -- Amount\n", tl("00" + "FF810520" + "00" + "9F0206"));
	}

	@Test
	public void testTagLengthIncompleteTagStartingWithFF() {
		for(String incomplete : new String[] {"FF", "FFFF"}) {
			assertEquals("", tl(incomplete), incomplete);
			assertEquals("9F 02 06 -- Amount\n", tl("9F0206" + incomplete), incomplete);
		}
	}

	@Test
	public void testTagLengthWithExternalValueTagStartingWithFF() {
		String expected = "9F 02 01 -- Amount:\n"
				+ "         42\n"
				+ "FF 81 05 02 -- Data Record:\n"
				+ "            43 44\n";

		assertEquals(expected, tagLengthWithExternalValue("9F0201" + "FF810502", "424344"));
	}

	@Test
	public void testTagLengthWithExternalValueTagStartingWithFFAfterPadding() {
		String expected = "FF 81 05 02 -- Data Record:\n"
				+ "            43 44\n";

		assertEquals(expected, tagLengthWithExternalValue("00" + "FF810502" + "00", "4344"));
	}

	@Test
	public void testTagLengthWithExternalValueIncompleteTagStartingWithFF() {
		String expected = "9F 02 01 -- Amount:\n"
				+ "         42\n";

		for(String incomplete : new String[] {"FF", "FFFF"}) {
			assertEquals("", tagLengthWithExternalValue(incomplete, "42"), incomplete);
			assertEquals(expected, tagLengthWithExternalValue("9F0201" + incomplete, "42"), incomplete);
		}
	}

}
