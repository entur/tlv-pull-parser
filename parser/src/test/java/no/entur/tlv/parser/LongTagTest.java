package no.entur.tlv.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * The tag is returned as an int (without the 'more bytes follow' bits of the subsequent bytes), so it can hold at most 4 bytes.
 */

public class LongTagTest {

	@Test
	public void testTagLengthValueWithThreeByteTag() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("9F8101" + "01AA"));

		assertEquals(0x9F0101, parser.nextTag());
		assertArrayEquals(new byte[] {(byte) 0xAA}, parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithFourByteTag() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("DF818203" + "01AA"));

		assertEquals(0xDF010203, parser.nextTag());
		assertArrayEquals(new byte[] {(byte) 0xAA}, parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithFiveByteTagIsMalformed() {
		// the first byte was shifted out, so this was returned as tag 0x5F020304, as the 4 byte tag 5F 82 83 04
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("1FDF828304" + "01AA" + "5A0101"));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithFourByteTag() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("DF818203" + "01"));

		assertEquals(0xDF010203, parser.nextTag());
		assertEquals(1, parser.getLength());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithFiveByteTagIsMalformed() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("1FDF828304" + "01" + "5A01"));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

}
