package no.entur.tlv.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.ByteArrayOutputStream;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;

/**
 * When no tag is returned, the payload (or length) of the previous tag must not be reported.
 */

public class PayloadResetTest {

	@Test
	public void testNoPayloadAfterLastTag() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A0101"));

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());

		assertEquals(-1, parser.nextTag());
		assertArrayEquals(new byte[0], parser.getPayload());
		assertEquals(0, parser.getBufferPayloadLength());
		assertEquals(-1, parser.parseTagLengthValuePayload().nextTag());

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		parser.writePayload(bout);
		assertEquals(0, bout.size());
	}

	@Test
	public void testNoPayloadAfterMalformedTag() {
		// 5F24 with length 5, but only one byte of payload
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A0101" + "5F240501"));

		assertEquals(0x5A, parser.nextTag());
		assertEquals(-1, parser.nextTag());
		assertArrayEquals(new byte[0], parser.getPayload());
	}

	@Test
	public void testNoPayloadWhenTagNotFound() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A0101" + "5F240102"));

		assertFalse(parser.skipTo(0x9F02));
		assertArrayEquals(new byte[0], parser.getPayload());
	}

	@Test
	public void testNoPayloadAfterSetBuffer() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A03010203"));
		assertEquals(0x5A, parser.nextTag());

		// the payload of the previous buffer (offset 2, length 3) is past the end of the new buffer
		parser.setBuffer(new byte[] {0x01});
		assertArrayEquals(new byte[0], parser.getPayload());
	}

	@Test
	public void testNoLengthAfterLastTag() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("9F0206"));

		assertEquals(0x9F02, parser.nextTag());
		assertEquals(6, parser.getLength());

		assertEquals(-1, parser.nextTag());
		assertEquals(0, parser.getLength());
	}

}
