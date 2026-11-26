package no.entur.tlv.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TlvPrettyPrinterTest {

	private static final byte[] MESSAGE1 = HexFormat.of().parseHex("6F568407A0000000041010A54B50104465626974204D6173746572636172648701015F2D086E6F656E646173769F1101019F12104D617374657263617264204465626574BF0C119F0A04000101019F6E0705780000303000");

	private static final byte[] MESSAGE2 = HexFormat.of().parseHex("7081E08F01059F320103922404F0DF9E4DC1E925D78169E27D0994DB57A7618113FB66AAD1E2FA1A217BB7DFC33C6B5B9081B02474B5620F1EDBA21827B346865CEA928A767374D9259C011FB1EF8EA44929FEA186F4C6DA80AF11783158ECA2E3273ABC65B2DB96F2AE19E4F8E0FEF2BC97F0B77E6C920491D6DFB8485B91B4B28A142A27A89FB2818E513005854C5083CCF69E44273071362BC45126CA1357E6EC8F8F9B762502A78F36F4E0AA646AE202B713D60709D62040EDFB78A1E34AD7E05089C3914FB58549CBCD47D2A277D140B039D8DA4CEB8AB0EA8A6EE71A16D8CE42");

	// https://stackoverflow.com/questions/74757198/emv-internal-authenticate-6985-respond
	private static final byte[] MESSAGE3 = HexFormat.of().parseHex("6F4F8407A0000000031010A544500A564953412044454249548701019F38189F66049F02069F03069F1A0295055F2A029A039C019F37045F2D02656EBF0C129F5A0511084008405F550255534203474165");

	private static TlvPrettyPrinter PRETTY_PRINTER;

	@BeforeAll
	public static void init() throws IOException {
		InputStream is = TlvPrettyPrinterTest.class.getResourceAsStream("/emv/tags.csv");
		InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);

		Set<ByteArrayKey> strings = new HashSet<>();

		Map<ByteArrayKey, String> tags = new HashMap<>();
		CSVParser parser = CSVParser.parse(reader, CSVFormat.RFC4180);
		for (CSVRecord csvRecord : parser) {
			String tag = csvRecord.get(0);
			String description = csvRecord.get(1);

			ByteArrayKey key = ByteArrayKey.create(tag);

			tags.put(key, description);

			if(csvRecord.size() > 2) {
				String type = csvRecord.get(2);
				if(type.equals("string")) {
					strings.add(key);
				}
			}
		}

		PRETTY_PRINTER = TlvPrettyPrinter.newBuilder().withStrings(strings).withTags(tags).build();
	}

	@Test
	public void test1() {
		String prettyPrinted = PRETTY_PRINTER.tlv(MESSAGE1).toString();
		System.out.println(prettyPrinted);
		assertTrue(prettyPrinted.contains("Debit Mastercard"));
	}

	@Test
	public void test2() {
		String prettyPrinted = PRETTY_PRINTER.tlv(MESSAGE2).toString();
		System.out.println(prettyPrinted);
		assertTrue(prettyPrinted.contains("Issuer Public Key Remainder"));
	}

	@Test
	public void test3() {
		String prettyPrinted = PRETTY_PRINTER.tlv(MESSAGE3).toString();
		System.out.println(prettyPrinted);
		assertTrue(prettyPrinted.contains("Processing Options Data Object List (PDOL)"));
	}

	private static final String PAN = "-- Application Primary Account Number (PAN) ";

	private static String tlv(String hex) {
		return PRETTY_PRINTER.tlv(HexFormat.of().parseHex(hex)).toString();
	}

	private static String tl(String hex) {
		byte[] bytes = HexFormat.of().parseHex(hex);
		StringBuilder output = new StringBuilder();
		PRETTY_PRINTER.tl(bytes, 0, bytes.length, output, 0, 0);
		return output.toString();
	}

	private static String tagLengthWithExternalValue(String hex, String values, int valuesOffset) {
		byte[] bytes = HexFormat.of().parseHex(hex);
		StringBuilder output = new StringBuilder();
		PRETTY_PRINTER.tagLengthWithExternalValue(bytes, 0, bytes.length, output, 0, 0, HexFormat.of().parseHex(values), valuesOffset);
		return output.toString();
	}

	@Test
	public void testLongFormLengthOverflowingToNegativeIsMalformed() {
		// 5A 02 12 34, then 5A with length 84 80 00 00 00 = 0x80000000, which is negative as an int
		assertEquals("5A 02 " + PAN + "\n      12 34 \n5A 84 " + PAN + "[malformed length]\n", tlv("5A021234" + "5A84800000004142"));
	}

	@Test
	public void testLongFormLengthOverflowingOffsetIsMalformed() {
		// 5A 02 12 34, then 5A with length 84 7F FF FF FF = Integer.MAX_VALUE, so offset + length overflows
		assertEquals("5A 02 " + PAN + "\n      12 34 \n5A 84 " + PAN + "[malformed length]\n", tlv("5A021234" + "5A847FFFFFFF4142"));
	}

	@Test
	public void testLongFormLengthWithMoreBytesThanFitInAnIntIsMalformed() {
		// 5A 02 12 34, then 5A with length 85 01 00 00 00 00 = 2^32, which truncates to 0 as an int
		assertEquals("5A 02 " + PAN + "\n      12 34 \n5A 85 " + PAN + "[malformed length]\n", tlv("5A021234" + "5A8501000000004142"));
	}

	@Test
	public void testLongFormLengthWithFiveBytesIsMalformedEvenIfTheLengthFits() {
		// 5A with length 85 00 00 00 00 02; ISO 7816-4 allows at most 4 length bytes
		assertEquals("5A 85 " + PAN + "[malformed length]\n", tlv("5A8500000000024142" + "5A01FF"));
	}

	@Test
	public void testLongFormLengthWithTruncatedLengthBytesIsMalformed() {
		// 2 length bytes announced, 1 present
		assertEquals("5A 82 " + PAN + "[malformed length]\n", tlv("5A8201"));
		// 1 length byte announced, none present
		assertEquals("5A 81 " + PAN + "[malformed length]\n", tlv("5A81"));
	}

	@Test
	public void testLongFormLengthPastLimitIsMalformed() {
		// 5A 02 12 34, then 5A with length 82 01 00 = 256, but no payload
		assertEquals("5A 02 " + PAN + "\n      12 34 \n5A 82 " + PAN + "[malformed length]\n", tlv("5A021234" + "5A820100"));
	}

	@Test
	public void testMalformedLengthOfTagWithoutDescription() {
		assertEquals("C1 82 [malformed length]\n", tlv("C18201"));
	}

	@Test
	public void testMalformedLengthWithinConstructedTag() {
		// 6F 08 with 5A 02 12 34, then 5A with length 82 01 00 = 256
		assertEquals("6F 08 -- Template, File Control Parameters and File Management Data (FCI) \n"
				+ "      5A 02 " + PAN + "\n"
				+ "            12 34 \n"
				+ "      5A 82 " + PAN + "[malformed length]\n", tlv("6F08" + "5A021234" + "5A820100"));
	}

	@Test
	public void testLongFormLengthWithFourBytesUpToLimit() {
		assertEquals("5A 84 " + PAN + "\n      41 42 \n", tlv("5A84000000024142"));
		// one more than what is left
		assertEquals("5A 84 " + PAN + "[malformed length]\n", tlv("5A84000000034142"));
	}

	@Test
	public void testLongFormLengthWithOneByteUpToLimit() {
		assertEquals("5A 81 " + PAN + "\n      41 42 \n", tlv("5A81024142"));
		// one more than what is left
		assertEquals("5A 81 " + PAN + "[malformed length]\n", tlv("5A81034142"));
	}

	@Test
	public void testTagLengthLongFormLengthWithMoreBytesThanFitInAnIntIsMalformed() {
		// 9F02 with length 85 01 00 00 00 00 = 2^32, then 9F03 01
		assertEquals("9F 02 85 -- Authorised Amount (Numeric) [malformed length]\n", tl("9F02850100000000" + "9F0301"));
		// 9F02 06, then 9F03 with length 85 00 00 00 00 01
		assertEquals("9F 02 06 -- Authorised Amount (Numeric)\n9F 03 85 -- Amount, Other (Numeric) [malformed length]\n", tl("9F0206" + "9F03850000000001"));
	}

	@Test
	public void testTagLengthLongFormLengthOverflowingToNegativeIsMalformed() {
		// 9F02 with length 84 80 00 00 00 = 0x80000000, which is negative as an int, then 9F03 01
		assertEquals("9F 02 84 -- Authorised Amount (Numeric) [malformed length]\n", tl("9F028480000000" + "9F0301"));
	}

	@Test
	public void testTagLengthLongFormLengthWithTruncatedLengthBytesIsMalformed() {
		// 2 length bytes announced, 1 present
		assertEquals("9F 02 82 -- Authorised Amount (Numeric) [malformed length]\n", tl("9F028201"));
		// tag without description
		assertEquals("C1 82 [malformed length]\n", tl("C18201"));
	}

	@Test
	public void testTagLengthLongFormLength() {
		// 9F02 with length 84 00 00 00 06, then 9F03 01
		assertEquals("9F 02 84 -- Authorised Amount (Numeric)\n9F 03 01 -- Amount, Other (Numeric)\n", tl("9F028400000006" + "9F0301"));
		// 9F02 with length 81 06, then 9F03 01
		assertEquals("9F 02 81 -- Authorised Amount (Numeric)\n9F 03 01 -- Amount, Other (Numeric)\n", tl("9F028106" + "9F0301"));
	}

	@Test
	public void testTagLengthWithExternalValueLongFormLengthOverflowingToNegativeIsMalformed() {
		// 9F02 with length 84 80 00 00 00 = 0x80000000, which is negative as an int, then 9F03 01
		assertEquals("9F 02 84 [malformed length]\n", tagLengthWithExternalValue("9F028480000000" + "9F0301", "010203", 0));
	}

	@Test
	public void testTagLengthWithExternalValueLongFormLengthWithFiveBytesIsMalformedEvenIfTheLengthFits() {
		// 9F02 with length 85 00 00 00 00 02; ISO 7816-4 allows at most 4 length bytes
		assertEquals("9F 02 85 [malformed length]\n", tagLengthWithExternalValue("9F02850000000002", "010203", 0));
	}

	@Test
	public void testTagLengthWithExternalValueLongFormLengthWithTruncatedLengthBytesIsMalformed() {
		// 2 length bytes announced, 1 present
		assertEquals("9F 02 82 [malformed length]\n", tagLengthWithExternalValue("9F028201", "010203", 0));
	}

	@Test
	public void testTagLengthWithExternalValueLengthPastValuesIsMalformed() {
		// 9F02 06, but only three value bytes
		assertEquals("9F 02 06 [malformed length]\n", tagLengthWithExternalValue("9F0206", "010203", 0));
		// C1 02, then 9F03 02, but only one value byte left for it
		assertEquals("C1 02 :\n      01 02\n9F 03 02 [malformed length]\n", tagLengthWithExternalValue("C102" + "9F0302", "010203", 0));
	}

	@Test
	public void testTagLengthWithExternalValueUpToEndOfValues() {
		assertEquals("9F 02 03 -- Authorised Amount (Numeric):\n         01 02 03\n", tagLengthWithExternalValue("9F0203", "010203", 0));
	}

	@Test
	public void testTagLengthWithExternalValueAtValuesOffset() {
		assertEquals("9F 02 02 -- Authorised Amount (Numeric):\n         02 03\n", tagLengthWithExternalValue("9F0202", "010203", 1));
		// one value byte left
		assertEquals("9F 02 02 [malformed length]\n", tagLengthWithExternalValue("9F0202", "010203", 2));
		// offset past the values
		assertEquals("9F 02 02 [malformed length]\n", tagLengthWithExternalValue("9F0202", "010203", 4));
	}

	@Test
	public void testTagLengthWithExternalValue() {
		// 9F02 with length 81 02, then 9F03 01
		assertEquals("9F 02 81 -- Authorised Amount (Numeric):\n         01 02\n9F 03 01 -- Amount, Other (Numeric):\n         03\n", tagLengthWithExternalValue("9F028102" + "9F0301", "010203", 0));
		// 9F02 with length 84 00 00 00 02, then 9F03 01
		assertEquals("9F 02 84 -- Authorised Amount (Numeric):\n         01 02\n9F 03 01 -- Amount, Other (Numeric):\n         03\n", tagLengthWithExternalValue("9F028400000002" + "9F0301", "010203", 0));
	}

}
