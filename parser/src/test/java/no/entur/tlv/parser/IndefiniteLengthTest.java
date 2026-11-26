package no.entur.tlv.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * The indefinite length form (length byte '80', with the content terminated by '00 00') is not used by ISO/IEC 7816-4.
 * In a tag-length list (DOL) the length is a single byte, so there '80' is the length 128.
 */

public class IndefiniteLengthTest {

	private static final String VALUE_128_BYTES = "AB".repeat(128);

	@Test
	public void testTagLengthValueWithIndefiniteLengthIsMalformed() {
		// 5A 01 01, then 30 with indefinite length, containing 02 01 05
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A0101" + "3080" + "020105" + "0000"));

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());

		// was returned as tag 0x30 with an empty payload, followed by its content as tag 0x02
		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithIndefiniteLengthAsLastByteIsMalformed() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A80"));

		// was returned as tag 0x5A with an empty payload
		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithIndefiniteLengthInChildParser() {
		// 70 07 containing 30 with indefinite length, followed by 5A 01 01
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("7007" + "3080" + "020105" + "0000" + "5A0101"));

		assertEquals(0x70, parser.nextTag());

		LenientTlvPullParser child = parser.parseTagLengthValuePayload();
		// was returned as tag 0x30 with an empty payload, followed by its content as tag 0x02
		assertEquals(-1, child.nextTag());
		assertEquals(-1, child.nextTag());

		// the parent parser is not affected
		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testSkipToDoesNotFindContentOfIndefiniteLength() {
		byte[] bytes = HexFormat.of().parseHex("5A0101" + "3080" + "020105" + "0000");

		LenientTlvPullParser parser = new LenientTlvPullParser(bytes);
		// was found, as the content of tag 0x30
		assertFalse(parser.skipTo(0x02));
		assertFalse(parser.skipTo(0x02));
		assertEquals(-1, parser.nextTag());

		parser.setBuffer(bytes);
		assertTrue(parser.skipTo(0x5A));
		assertFalse(parser.skipTo(0x30));
	}

	@Test
	public void testTagLengthValueWithLength128InLongForm() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A8180" + VALUE_128_BYTES + "5B0101"));

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(HexFormat.of().parseHex(VALUE_128_BYTES), parser.getPayload());

		assertEquals(0x5B, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithTagByte80() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("8001AA" + "5A0101"));

		assertEquals(0x80, parser.nextTag());
		assertArrayEquals(new byte[] {(byte) 0xAA}, parser.getPayload());

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithTagContinuationByte80() {
		// tag 9F 80 01, length 01
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("9F800101AA" + "5A0101"));

		assertEquals(0x9F0001, parser.nextTag());
		assertArrayEquals(new byte[] {(byte) 0xAA}, parser.getPayload());

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithLength128() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("9F0206" + "9F0380" + "9F1A02"));

		assertEquals(0x9F02, parser.nextTag());
		assertEquals(6, parser.getLength());

		// was returned with length 0
		assertEquals(0x9F03, parser.nextTag());
		assertEquals(128, parser.getLength());

		assertEquals(0x9F1A, parser.nextTag());
		assertEquals(2, parser.getLength());

		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithLength128AsLastByte() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("9F0206" + "9F0380"));

		assertEquals(0x9F02, parser.nextTag());
		assertEquals(6, parser.getLength());

		// was returned with length 0
		assertEquals(0x9F03, parser.nextTag());
		assertEquals(128, parser.getLength());

		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithLength128InLongForm() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("9F0206" + "9F038180" + "9F1A02"));

		assertEquals(0x9F02, parser.nextTag());
		assertEquals(6, parser.getLength());

		assertEquals(0x9F03, parser.nextTag());
		assertEquals(128, parser.getLength());

		assertEquals(0x9F1A, parser.nextTag());
		assertEquals(2, parser.getLength());

		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithLength128InTagLengthValue() {
		// 8C 08 with a tag-length list, followed by 5A 01 01
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("8C08" + "9F0206" + "9F0380" + "9505" + "5A0101"));

		assertEquals(0x8C, parser.nextTag());

		TagLengthPullParser tagLengths = parser.parseTagLengthPayload();
		assertEquals(0x9F02, tagLengths.nextTag());
		assertEquals(6, tagLengths.getLength());
		assertEquals(0x9F03, tagLengths.nextTag());
		assertEquals(128, tagLengths.getLength());
		assertEquals(0x95, tagLengths.nextTag());
		assertEquals(5, tagLengths.getLength());
		assertEquals(-1, tagLengths.nextTag());

		assertEquals(0x5A, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

}
