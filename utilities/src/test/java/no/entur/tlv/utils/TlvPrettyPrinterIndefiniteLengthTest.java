package no.entur.tlv.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * The indefinite length form (length byte '80', with the content terminated by '00 00') is not used by ISO/IEC 7816-4.
 * In a tag-length list (DOL) the length is a single byte, so there '80' is the length 128.
 */

public class TlvPrettyPrinterIndefiniteLengthTest {

	private static final TlvPrettyPrinter PRETTY_PRINTER = TlvPrettyPrinter.newBuilder().build();

	private static final String VALUE_128_BYTES = "AB".repeat(128);
	private static final String VALUE_128_BYTES_PRINTED = "AB ".repeat(128).trim();

	@Test
	public void testTlvWithIndefiniteLengthIsMalformed() {
		// 5A 01 01, then 30 with indefinite length, containing 02 01 05
		String prettyPrinted = PRETTY_PRINTER.tlv(HexFormat.of().parseHex("5A0101" + "3080" + "020105" + "0000")).toString();

		// the content was printed as the elements following tag 30
		assertEquals(
				"5A 01 \n" +
				"      01 \n" +
				"30 80 \n",
				prettyPrinted);
	}

	@Test
	public void testNestedTlvWithIndefiniteLengthIsMalformed() {
		// 70 07 containing 30 with indefinite length, followed by 5A 01 01
		String prettyPrinted = PRETTY_PRINTER.tlv(HexFormat.of().parseHex("7007" + "3080" + "020105" + "0000" + "5A0101")).toString();

		// the content of 70 ends at 30, and printing continues after 70
		assertEquals(
				"70 07 \n" +
				"      30 80 \n" +
				"5A 01 \n" +
				"      01 \n",
				prettyPrinted);
	}

	@Test
	public void testTagLengthWithLength128InTlv() {
		// 8C 08 with a tag-length list, followed by 5A 01 01
		String prettyPrinted = PRETTY_PRINTER.tlv(HexFormat.of().parseHex("8C08" + "9F0206" + "9F0380" + "9505" + "5A0101")).toString();

		assertEquals(
				"8C 08 \n" +
				"      9F 02 06 \n" +
				"      9F 03 80 \n" +
				"      95 05 \n" +
				"5A 01 \n" +
				"      01 \n",
				prettyPrinted);
	}

	@Test
	public void testTagLengthWithLength128() {
		assertEquals(
				"9F 02 06 \n" +
				"9F 03 80 \n" +
				"9F 1A 02 \n",
				tl("9F0206" + "9F0380" + "9F1A02"));
	}

	@Test
	public void testTagLengthWithLength128AsLastByte() {
		assertEquals(
				"9F 02 06 \n" +
				"9F 03 80 \n",
				tl("9F0206" + "9F0380"));
	}

	@Test
	public void testTagLengthWithLength128InLongForm() {
		assertEquals(
				"9F 02 06 \n" +
				"9F 03 81 \n" +
				"9F 1A 02 \n",
				tl("9F0206" + "9F038180" + "9F1A02"));
	}

	@Test
	public void testTagLengthWithExternalValueWithLength128() {
		// was printed with no value for 9F 03, and 9F 1A with the first byte of it
		assertEquals(
				"9F 02 01 :\n" +
				"         42\n" +
				"9F 03 80 :\n" +
				"         " + VALUE_128_BYTES_PRINTED + "\n" +
				"9F 1A 01 :\n" +
				"         43\n",
				tagLengthWithExternalValue("9F0201" + "9F0380" + "9F1A01", "42" + VALUE_128_BYTES + "43"));
	}

	@Test
	public void testTagLengthWithExternalValueWithLength128AsLastByte() {
		assertEquals(
				"9F 02 01 :\n" +
				"         42\n" +
				"9F 03 80 :\n" +
				"         " + VALUE_128_BYTES_PRINTED + "\n",
				tagLengthWithExternalValue("9F0201" + "9F0380", "42" + VALUE_128_BYTES));
	}

	@Test
	public void testTagLengthWithExternalValueWithLength128InLongForm() {
		assertEquals(
				"9F 02 01 :\n" +
				"         42\n" +
				"9F 03 81 :\n" +
				"         " + VALUE_128_BYTES_PRINTED + "\n" +
				"9F 1A 01 :\n" +
				"         43\n",
				tagLengthWithExternalValue("9F0201" + "9F038180" + "9F1A01", "42" + VALUE_128_BYTES + "43"));
	}

	private static String tl(String tagLengthsAsHex) {
		byte[] tagLengths = HexFormat.of().parseHex(tagLengthsAsHex);

		StringBuilder output = new StringBuilder();
		PRETTY_PRINTER.tl(tagLengths, 0, tagLengths.length, output, 0, 0);
		return output.toString();
	}

	private static String tagLengthWithExternalValue(String tagLengthsAsHex, String valuesAsHex) {
		byte[] tagLengths = HexFormat.of().parseHex(tagLengthsAsHex);
		byte[] values = HexFormat.of().parseHex(valuesAsHex);

		StringBuilder output = new StringBuilder();
		PRETTY_PRINTER.tagLengthWithExternalValue(tagLengths, 0, tagLengths.length, output, 0, 0, values, 0);
		return output.toString();
	}

}
