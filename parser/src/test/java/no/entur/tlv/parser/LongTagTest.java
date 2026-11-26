package no.entur.tlv.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * The tag is returned as an int (without the 'more bytes follow' bits of the subsequent bytes), so it can hold at most 4 bytes.
 * A longer tag is malformed: the parser returns -1 and stops, so data objects after the tag (at the same level) are not returned.
 */

public class LongTagTest {

	private static final String FIVE_BYTE_TAG = "1FDF828304";
	private static final String SIX_BYTE_TAG = "1FDF82838405";

	// tag, length and value

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

		// the sign bit of the int is set
		assertEquals(0xDF010203, parser.nextTag());
		assertArrayEquals(new byte[] {(byte) 0xAA}, parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithFourByteTagWithSmallestFirstByte() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("1FDF8203" + "01AA" + "5A0101"));

		assertEquals(0x1F5F0203, parser.nextTag());
		assertArrayEquals(new byte[] {(byte) 0xAA}, parser.getPayload());
		assertEquals(0x5A, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithFiveByteTagIsMalformed() {
		// the first byte was shifted out, so this was returned as tag 0x5F020304, as the 4 byte tag 5F 82 83 04
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex(FIVE_BYTE_TAG + "01AA" + "5A0101"));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithSixByteTagIsMalformed() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex(SIX_BYTE_TAG + "01AA" + "5A0101"));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueStaysStoppedAfterFiveByteTag() {
		byte[] bytes = HexFormat.of().parseHex(FIVE_BYTE_TAG + "01AA" + "5A0101");
		LenientTlvPullParser parser = new LenientTlvPullParser(bytes);

		for(int i = 0; i < 5; i++) {
			assertEquals(-1, parser.nextTag());
			assertEquals(bytes.length, parser.getNextTagOffset());
		}
	}

	@Test
	public void testTagLengthValueWithFiveByteTagHidesLaterTags() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A01AA" + FIVE_BYTE_TAG + "01AA" + "5A01AA"));

		assertEquals(0x5A, parser.nextTag());
		assertEquals(-1, parser.nextTag());
		// the second 5A is not returned
		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueSkipToDoesNotPassFiveByteTag() {
		byte[] bytes = HexFormat.of().parseHex(FIVE_BYTE_TAG + "01AA" + "5A0101");
		LenientTlvPullParser parser = new LenientTlvPullParser(bytes);

		// 5A is there, but after the malformed tag
		assertFalse(parser.skipTo(0x5A));
		assertEquals(bytes.length, parser.getNextTagOffset());
		assertFalse(parser.skipTo(0x5A));
	}

	@Test
	public void testTagLengthValueWithFiveByteTagInChildDoesNotStopParent() {
		// 70 with 7 bytes of payload, which is a 5 byte tag with length and value
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("7007" + FIVE_BYTE_TAG + "01AA" + "5A0102"));

		assertEquals(0x70, parser.nextTag());

		LenientTlvPullParser child = parser.parseTagLengthValuePayload();
		assertEquals(-1, child.nextTag());
		// the child stops at the end of its own payload
		assertEquals(9, child.getNextTagOffset());
		assertEquals(-1, child.nextTag());

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x02}, parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithFiveByteTagAtEndOfBuffer() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex(FIVE_BYTE_TAG));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithTruncatedFiveByteTag() {
		// the last byte says that more tag bytes follow
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("1FDF8283"));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithFourByteTagAtEndOfBuffer() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("1FDF8203"));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueWithFourOrMoreFFBytesIsMalformed() {
		// FF is a valid first tag byte, and as a subsequent tag byte it says that more tag bytes follow.
		// So FF FF FF FF 5A is a 5 byte tag. It was returned as tag 0x7F7F7F5A, followed by tag 57.
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("FFFFFFFF" + "5A01AA" + "5701AA"));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	// tag and length

	@Test
	public void testTagLengthWithThreeByteTag() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("9F8101" + "01"));

		assertEquals(0x9F0101, parser.nextTag());
		assertEquals(1, parser.getLength());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithFourByteTag() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("DF818203" + "01"));

		// the sign bit of the int is set
		assertEquals(0xDF010203, parser.nextTag());
		assertEquals(1, parser.getLength());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithFourByteTagWithSmallestFirstByte() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("1FDF8203" + "01" + "5A02"));

		assertEquals(0x1F5F0203, parser.nextTag());
		assertEquals(1, parser.getLength());
		assertEquals(0x5A, parser.nextTag());
		assertEquals(2, parser.getLength());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithFiveByteTagIsMalformed() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex(FIVE_BYTE_TAG + "01" + "5A01"));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithSixByteTagIsMalformed() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex(SIX_BYTE_TAG + "01" + "5A01"));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithFiveByteTagHidesLaterTags() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("5A01" + FIVE_BYTE_TAG + "01" + "5A01"));

		assertEquals(0x5A, parser.nextTag());
		// the second 5A is not returned
		for(int i = 0; i < 5; i++) {
			assertEquals(-1, parser.nextTag());
		}
	}

	@Test
	public void testTagLengthWithFiveByteTagInChildDoesNotStopParent() {
		// 70 with 6 bytes of payload, which is a 5 byte tag with length
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("7006" + FIVE_BYTE_TAG + "01" + "5A0102"));

		assertEquals(0x70, parser.nextTag());

		TagLengthPullParser child = parser.parseTagLengthPayload();
		assertEquals(-1, child.nextTag());
		assertEquals(-1, child.nextTag());

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x02}, parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithFiveByteTagAtEndOfBuffer() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex(FIVE_BYTE_TAG));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithTruncatedFiveByteTag() {
		// the last byte says that more tag bytes follow
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("1FDF8283"));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithFourByteTagAtEndOfBuffer() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("1FDF8203"));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthWithFourOrMoreFFBytesIsMalformed() {
		// as a 5 byte tag; was returned as tag 0x7F7F7F5A, followed by tag 57
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("FFFFFFFF" + "5A01" + "5701"));

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

}
