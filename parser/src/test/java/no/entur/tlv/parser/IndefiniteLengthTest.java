package no.entur.tlv.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * The indefinite length form (length byte '80', with the content terminated by '00 00') is not used by ISO/IEC 7816-4.
 */

public class IndefiniteLengthTest {

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
	public void testTagLengthWithIndefiniteLengthIsMalformed() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("9F0206" + "9F0380" + "9F1A02"));

		assertEquals(0x9F02, parser.nextTag());
		assertEquals(6, parser.getLength());

		// was returned as tag 0x9F03 with length 0
		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

}
