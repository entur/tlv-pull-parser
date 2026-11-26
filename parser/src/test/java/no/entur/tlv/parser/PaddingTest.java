package no.entur.tlv.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * '00' bytes without any meaning may occur before, between and after data objects (EMV Book 3, Annex B).
 * 'FF' is not padding: it is the first byte of a (private class, constructed) tag, like the EMV tags FF8105 or FF60.
 */

public class PaddingTest {

	private static byte[] hex(String hex) {
		return HexFormat.of().parseHex(hex);
	}

	// 00 padding, tag-length-value

	@Test
	public void testTagLengthValuePadding() {
		LenientTlvPullParser parser = new LenientTlvPullParser(hex("0000" + "5A0101" + "00" + "5F240102" + "0000"));

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(hex("01"), parser.getPayload());
		assertEquals(0x5F24, parser.nextTag());
		assertArrayEquals(hex("02"), parser.getPayload());
		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValuePaddingOnly() {
		assertEquals(-1, new LenientTlvPullParser(hex("00")).nextTag());
		assertEquals(-1, new LenientTlvPullParser(hex("0000")).nextTag());
	}

	@Test
	public void testTagLengthValueTrailingPadding() {
		for(String padding : new String[] {"00", "0000"}) {
			LenientTlvPullParser parser = new LenientTlvPullParser(hex("5A0101" + padding));

			assertEquals(0x5A, parser.nextTag(), padding);
			assertArrayEquals(hex("01"), parser.getPayload(), padding);
			assertEquals(-1, parser.nextTag(), padding);
		}
	}

	@Test
	public void testTagLengthValuePaddingInChildPayload() {
		LenientTlvPullParser parser = new LenientTlvPullParser(hex("70035A0000"));

		assertEquals(0x70, parser.nextTag());

		LenientTlvPullParser child = parser.parseTagLengthValuePayload();
		assertEquals(0x5A, child.nextTag());
		assertEquals(0, child.getBufferPayloadLength());
		assertEquals(-1, child.nextTag());

		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValuePaddingAroundElementsInChildPayload() {
		LenientTlvPullParser parser = new LenientTlvPullParser(hex("700B" + "00" + "5A0101" + "0000" + "5F240102" + "00" + "9F020103"));

		assertEquals(0x70, parser.nextTag());

		LenientTlvPullParser child = parser.parseTagLengthValuePayload();
		assertEquals(0x5A, child.nextTag());
		assertArrayEquals(hex("01"), child.getPayload());
		assertEquals(0x5F24, child.nextTag());
		assertArrayEquals(hex("02"), child.getPayload());
		assertEquals(-1, child.nextTag());

		assertEquals(0x9F02, parser.nextTag());
		assertArrayEquals(hex("03"), parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthPaddingInChildPayload() {
		LenientTlvPullParser parser = new LenientTlvPullParser(hex("8C07" + "00" + "9F0206" + "00" + "5A08"));

		assertEquals(0x8C, parser.nextTag());

		TagLengthPullParser child = parser.parseTagLengthPayload();
		assertEquals(0x9F02, child.nextTag());
		assertEquals(6, child.getLength());
		assertEquals(0x5A, child.nextTag());
		assertEquals(8, child.getLength());
		assertEquals(-1, child.nextTag());
	}

	// 00 padding, tag-length

	@Test
	public void testTagLengthPadding() {
		TagLengthPullParser parser = new TagLengthPullParser(hex("0000" + "5A01" + "00" + "5F2402" + "0000"));

		assertEquals(0x5A, parser.nextTag());
		assertEquals(1, parser.getLength());
		assertEquals(0x5F24, parser.nextTag());
		assertEquals(2, parser.getLength());
		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthPaddingOnly() {
		assertEquals(-1, new TagLengthPullParser(hex("00")).nextTag());
		assertEquals(-1, new TagLengthPullParser(hex("0000")).nextTag());
	}

	@Test
	public void testTagLengthTrailingPadding() {
		for(String padding : new String[] {"00", "0000"}) {
			TagLengthPullParser parser = new TagLengthPullParser(hex("5A01" + padding));

			assertEquals(0x5A, parser.nextTag(), padding);
			assertEquals(1, parser.getLength(), padding);
			assertEquals(-1, parser.nextTag(), padding);
		}
	}

	// FF is the first byte of a tag, tag-length-value

	@Test
	public void testTagLengthValueThreeByteTagStartingWithFF() {
		// EMV Data Record
		LenientTlvPullParser parser = new LenientTlvPullParser(hex("FF8105" + "03" + "5A01AA"));

		assertEquals(0xFF0105, parser.nextTag());
		assertArrayEquals(hex("5A01AA"), parser.getPayload());

		LenientTlvPullParser child = parser.parseTagLengthValuePayload();
		assertEquals(0x5A, child.nextTag());
		assertArrayEquals(hex("AA"), child.getPayload());
		assertEquals(-1, child.nextTag());

		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueThreeByteTagStartingWithFFAndPrimitivePayload() {
		// EMV Tags To Write Before Gen AC
		LenientTlvPullParser parser = new LenientTlvPullParser(hex("FF8102" + "02" + "AABB"));

		assertEquals(0xFF0102, parser.nextTag());
		assertArrayEquals(hex("AABB"), parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueTwoByteTagStartingWithFF() {
		LenientTlvPullParser parser = new LenientTlvPullParser(hex("FF5A" + "01" + "01"));

		assertEquals(0xFF5A, parser.nextTag());
		assertArrayEquals(hex("01"), parser.getPayload());
		assertEquals(-1, parser.nextTag());

		parser = new LenientTlvPullParser(hex("FF01" + "01" + "AA"));

		assertEquals(0xFF01, parser.nextTag());
		assertArrayEquals(hex("AA"), parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueTagStartingWithFFAfterElement() {
		LenientTlvPullParser parser = new LenientTlvPullParser(hex("5A0101" + "FF810501AA" + "5F240102"));

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(hex("01"), parser.getPayload());
		assertEquals(0xFF0105, parser.nextTag());
		assertArrayEquals(hex("AA"), parser.getPayload());
		assertEquals(0x5F24, parser.nextTag());
		assertArrayEquals(hex("02"), parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueTagStartingWithFFAfterPadding() {
		LenientTlvPullParser parser = new LenientTlvPullParser(hex("00" + "FF810501AA" + "00"));

		assertEquals(0xFF0105, parser.nextTag());
		assertArrayEquals(hex("AA"), parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueFFIsNotSkipped() {
		// FF FF 5A is a three byte tag, not padding followed by tag 5A
		LenientTlvPullParser parser = new LenientTlvPullParser(hex("FFFF5A" + "01" + "01"));

		assertEquals(0xFF7F5A, parser.nextTag());
		assertArrayEquals(hex("01"), parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueIncompleteTagStartingWithFF() {
		// a tag without its last byte and / or length; there is no more
		for(String incomplete : new String[] {"FF", "FFFF"}) {
			assertEquals(-1, new LenientTlvPullParser(hex(incomplete)).nextTag(), incomplete);

			LenientTlvPullParser parser = new LenientTlvPullParser(hex("5A0101" + incomplete));

			assertEquals(0x5A, parser.nextTag(), incomplete);
			assertArrayEquals(hex("01"), parser.getPayload(), incomplete);
			assertEquals(-1, parser.nextTag(), incomplete);
		}
	}

	// FF is the first byte of a tag, tag-length

	@Test
	public void testTagLengthTagStartingWithFF() {
		TagLengthPullParser parser = new TagLengthPullParser(hex("9F0206" + "FF810520"));

		assertEquals(0x9F02, parser.nextTag());
		assertEquals(6, parser.getLength());
		assertEquals(0xFF0105, parser.nextTag());
		assertEquals(32, parser.getLength());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthTagStartingWithFFFirstAndAfterPadding() {
		TagLengthPullParser parser = new TagLengthPullParser(hex("FF810520" + "00" + "FF6002" + "5A08"));

		assertEquals(0xFF0105, parser.nextTag());
		assertEquals(32, parser.getLength());
		assertEquals(0xFF60, parser.nextTag());
		assertEquals(2, parser.getLength());
		assertEquals(0x5A, parser.nextTag());
		assertEquals(8, parser.getLength());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthIncompleteTagStartingWithFF() {
		for(String incomplete : new String[] {"FF", "FFFF"}) {
			assertEquals(-1, new TagLengthPullParser(hex(incomplete)).nextTag(), incomplete);

			TagLengthPullParser parser = new TagLengthPullParser(hex("5A01" + incomplete));

			assertEquals(0x5A, parser.nextTag(), incomplete);
			assertEquals(1, parser.getLength(), incomplete);
			assertEquals(-1, parser.nextTag(), incomplete);
		}
	}

}
