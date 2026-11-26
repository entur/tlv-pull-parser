package no.entur.tlv.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * ISO/IEC 7816-4: '00' and 'FF' bytes without any meaning may occur before, between and after data objects.
 */

public class PaddingTest {

	@Test
	public void testTagLengthValuePadding() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("00FF" + "5A0101" + "FF00" + "5F240102" + "00FF"));

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());
		assertEquals(0x5F24, parser.nextTag());
		assertArrayEquals(new byte[] {0x02}, parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthValueFFPadding() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("FFFF" + "5A0101"));

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testTagLengthPadding() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("00FF" + "5A01" + "FF00" + "5F2402" + "00FF"));

		assertEquals(0x5A, parser.nextTag());
		assertEquals(1, parser.getLength());
		assertEquals(0x5F24, parser.nextTag());
		assertEquals(2, parser.getLength());
		assertEquals(-1, parser.nextTag());
	}

}
