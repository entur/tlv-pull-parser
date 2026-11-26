package no.entur.tlv.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HexFormat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * When no tag is returned, the payload (or length) of the previous tag must not be reported.
 */

public class PayloadResetTest {

	@Test
	public void testNoPayloadAfterLastTag() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A0101"));

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());
		assertEquals(2, parser.getBufferPayloadOffset());

		assertEquals(-1, parser.nextTag());
		assertArrayEquals(new byte[0], parser.getPayload());
		assertEquals(0, parser.getBufferPayloadLength());
		assertEquals(3, parser.getBufferPayloadOffset());
		assertEquals(-1, parser.parseTagLengthValuePayload().nextTag());

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		parser.writePayload(bout);
		assertEquals(0, bout.size());

		// and again
		assertEquals(-1, parser.nextTag());
		assertEmpty(parser);
		assertEquals(3, parser.getBufferPayloadOffset());
	}

	@Test
	public void testPayloadParsersAreEmptyAfterLastTag() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("8C039F0206"));

		assertEquals(0x8C, parser.nextTag());
		assertEquals(-1, parser.nextTag());

		// new payload parsers
		LenientTlvPullParser tlv = parser.parseTagLengthValuePayload();
		assertEquals(-1, tlv.nextTag());
		assertEmpty(tlv);

		TagLengthPullParser tl = parser.parseTagLengthPayload();
		assertEquals(0, tl.getLength());
		assertEquals(-1, tl.nextTag());
		assertEquals(0, tl.getLength());
	}

	@Test
	public void testReusedPayloadParsersAreEmptyAfterLastTag() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("8C035A0101"));

		assertEquals(0x8C, parser.nextTag());

		LenientTlvPullParser tlv = parser.parseTagLengthValuePayload();
		assertEquals(0x5A, tlv.nextTag());
		assertArrayEquals(new byte[] {0x01}, tlv.getPayload());

		TagLengthPullParser tl = parser.parseTagLengthPayload();
		assertEquals(0x5A, tl.nextTag());
		assertEquals(1, tl.getLength());

		assertEquals(-1, parser.nextTag());

		assertSame(tlv, parser.parseTagLengthValuePayload());
		assertEmpty(tlv);
		assertEquals(-1, tlv.nextTag());
		assertEmpty(tlv);

		assertSame(tl, parser.parseTagLengthPayload());
		assertEquals(0, tl.getLength());
		assertEquals(-1, tl.nextTag());
		assertEquals(0, tl.getLength());
	}

	/**
	 * Malformed data after a first, valid tag (5A with payload 01):
	 * <ul>
	 * <li>5F: a multi-byte tag without its second byte</li>
	 * <li>5F24: a tag without a length</li>
	 * <li>9F8102: a three-byte tag without a length</li>
	 * <li>5A81: a long-form length without its length byte</li>
	 * <li>5A8201: a long-form length with one of two length bytes</li>
	 * <li>5A02AA: a payload of two bytes with only one byte present</li>
	 * <li>5A: a single-byte tag without a length</li>
	 * <li>00: a trailing padding byte</li>
	 * </ul>
	 */

	@ParameterizedTest
	@ValueSource(strings = {"5F", "5F24", "9F8102", "5A81", "5A8201", "5A02AA", "5A", "00"})
	public void testNoPayloadAfterMalformedData(String malformed) {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A0101" + malformed));

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());

		assertEquals(-1, parser.nextTag());
		assertEmpty(parser);

		LenientTlvPullParser tlv = parser.parseTagLengthValuePayload();
		assertEquals(parser.getBufferPayloadOffset(), tlv.getBufferPayloadOffset());
		assertEquals(-1, tlv.nextTag());
		assertEmpty(tlv);
		assertEquals(parser.getBufferPayloadOffset(), tlv.getBufferPayloadOffset());

		assertEquals(-1, parser.parseTagLengthPayload().nextTag());
	}

	@ParameterizedTest
	@ValueSource(strings = {"9F", "5A81"})
	public void testNoLengthAfterMalformedData(String malformed) {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("9F0206" + malformed));

		assertEquals(0x9F02, parser.nextTag());
		assertEquals(6, parser.getLength());

		assertEquals(-1, parser.nextTag());
		assertEquals(0, parser.getLength());
	}

	@ParameterizedTest
	@ValueSource(strings = {"5A00", "5A8100"})
	public void testLastTagWithoutPayloadIsReturned(String hex) {
		byte[] bytes = HexFormat.of().parseHex(hex);
		LenientTlvPullParser parser = new LenientTlvPullParser(bytes);

		assertEquals(0x5A, parser.nextTag());
		assertEmpty(parser);
		assertEquals(bytes.length, parser.getBufferPayloadOffset());

		assertEquals(-1, parser.nextTag());
		assertEmpty(parser);
		assertEquals(bytes.length, parser.getBufferPayloadOffset());
	}

	@Test
	public void testLastTagEndsAtLimitOfRange() {
		// the range is 5A0101
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("FF5A0101AA"), 1, 3);

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());
		assertEquals(3, parser.getBufferPayloadOffset());
		assertEquals(1, parser.getBufferPayloadLength());

		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testNoPayloadBeforeFirstTagAndAfterLastTagOfRange() {
		// the range is 5A0101
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("FF5A0101AA"), 1, 3);
		assertEmpty(parser);
		assertEquals(1, parser.getBufferPayloadOffset());

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());

		assertEquals(-1, parser.nextTag());
		assertEmpty(parser);
		assertEquals(4, parser.getBufferPayloadOffset());
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
		assertEmpty(parser);
	}

	@Test
	public void testNoPayloadParserWhenTagNotFound() {
		byte[] bytes = HexFormat.of().parseHex("6F035A0101" + "8C039F0206");

		LenientTlvPullParser parser = new LenientTlvPullParser(bytes);
		assertNull(parser.parseTagLengthValuePayload(0x70));
		assertEmpty(parser);

		parser.setBuffer(bytes);
		assertNull(parser.parseTagLengthPayload(0x70));
		assertEmpty(parser);

		// whereas a tag which is found has a payload
		parser.setBuffer(bytes);
		assertTrue(parser.skipTo(0x8C));
		assertArrayEquals(HexFormat.of().parseHex("9F0206"), parser.getPayload());

		parser.setBuffer(bytes);
		LenientTlvPullParser tlv = parser.parseTagLengthValuePayload(0x6F);
		assertEquals(0x5A, tlv.nextTag());
		assertArrayEquals(new byte[] {0x01}, tlv.getPayload());
		assertEquals(-1, tlv.nextTag());

		TagLengthPullParser tl = parser.parseTagLengthPayload(0x8C);
		assertEquals(0x9F02, tl.nextTag());
		assertEquals(6, tl.getLength());
		assertEquals(-1, tl.nextTag());
	}

	@Test
	public void testNoPayloadAfterSetBuffer() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A03010203"));
		assertEquals(0x5A, parser.nextTag());

		// the payload of the previous buffer (offset 2, length 3) is past the end of the new buffer
		parser.setBuffer(new byte[] {0x01});
		assertArrayEquals(new byte[0], parser.getPayload());
		assertEmpty(parser);
	}

	@Test
	public void testNoPayloadAfterSetBufferWithOffset() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A03010203"));
		assertEquals(0x5A, parser.nextTag());
		assertEquals(2, parser.getBufferPayloadOffset());
		assertEquals(3, parser.getBufferPayloadLength());

		// the range is 5A0101
		parser.setBuffer(HexFormat.of().parseHex("FF5A0101AA"), 1, 3);
		assertEmpty(parser);
		assertEquals(1, parser.getBufferPayloadOffset());

		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());
		assertEquals(3, parser.getBufferPayloadOffset());

		assertEquals(-1, parser.nextTag());
		assertEmpty(parser);
		assertEquals(4, parser.getBufferPayloadOffset());
	}

	@Test
	public void testNoPayloadAfterSetResponseApduPayload() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A03010203"));
		assertEquals(0x5A, parser.nextTag());

		// only the status bytes
		parser.setResponseApduPayload(HexFormat.of().parseHex("9000"));
		assertEmpty(parser);
		assertEquals(-1, parser.nextTag());
		assertEmpty(parser);

		parser.setResponseApduPayload(HexFormat.of().parseHex("5A0101" + "9000"));
		assertEmpty(parser);
		assertEquals(0x5A, parser.nextTag());
		assertArrayEquals(new byte[] {0x01}, parser.getPayload());
		assertEquals(-1, parser.nextTag());
		assertEmpty(parser);
	}

	@Test
	public void testNoPayloadWithoutBuffer() {
		assertNoBuffer(new LenientTlvPullParser());
		assertNoBuffer(LenientTlvPullParser.newInstance());
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 3, 4})
	public void testNoPayloadWithoutBufferForDepth(int depth) {
		LenientTlvPullParser parser = LenientTlvPullParser.newInstance(depth);

		// prepared payload parsers, and those created on demand
		for(int i = 0; i < 5; i++) {
			assertNoBuffer(parser);

			parser = parser.parseTagLengthValuePayload();
		}
	}

	@Test
	public void testNoLengthWithoutBuffer() {
		TagLengthPullParser parser = new TagLengthPullParser();

		assertNotNull(parser.getBuffer());
		assertEquals(0, parser.getBuffer().length);

		assertEquals(0, parser.getLength());
		assertEquals(-1, parser.nextTag());
		assertEquals(0, parser.getLength());
	}

	@Test
	public void testReusedPayloadParserHasNoPayloadBeforeFirstTag() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("6F035A0101" + "70035A0102"));

		assertEquals(0x6F, parser.nextTag());
		LenientTlvPullParser payloadParser = parser.parseTagLengthValuePayload();
		assertEmpty(payloadParser);
		assertEquals(2, payloadParser.getBufferPayloadOffset());
		assertEquals(0x5A, payloadParser.nextTag());
		assertArrayEquals(new byte[] {0x01}, payloadParser.getPayload());

		assertEquals(0x70, parser.nextTag());
		assertSame(payloadParser, parser.parseTagLengthValuePayload());
		assertEmpty(payloadParser);
		assertEquals(7, payloadParser.getBufferPayloadOffset());
		assertEquals(0x5A, payloadParser.nextTag());
		assertArrayEquals(new byte[] {0x02}, payloadParser.getPayload());
		assertEquals(-1, payloadParser.nextTag());
	}

	@Test
	public void testNewPayloadParserHasNoLengthBeforeFirstTag() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("8C039F0206"));

		assertEquals(0x8C, parser.nextTag());
		assertPayloadParserLength(parser.parseTagLengthPayload(), 0x9F02, 6);
	}

	@Test
	public void testPreparedPayloadParserHasNoLengthBeforeFirstTag() {
		LenientTlvPullParser parser = LenientTlvPullParser.newInstance(2);
		parser.setBuffer(HexFormat.of().parseHex("8C039F0206"));

		assertEquals(0x8C, parser.nextTag());
		assertPayloadParserLength(parser.parseTagLengthPayload(), 0x9F02, 6);
	}

	@Test
	public void testReusedPayloadParserHasNoLengthBeforeFirstTag() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("8C039F0206" + "8D039F0306"));

		assertEquals(0x8C, parser.nextTag());
		TagLengthPullParser payloadParser = parser.parseTagLengthPayload();
		assertPayloadParserLength(payloadParser, 0x9F02, 6);

		assertEquals(0x8D, parser.nextTag());
		assertSame(payloadParser, parser.parseTagLengthPayload());
		assertPayloadParserLength(payloadParser, 0x9F03, 6);
	}

	@Test
	public void testNoLengthAfterSetPayload() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("9F0206"));
		assertEquals(0x9F02, parser.nextTag());
		assertEquals(6, parser.getLength());

		parser.setPayload(HexFormat.of().parseHex("5A01"));
		assertPayloadParserLength(parser, 0x5A, 1);

		parser.setPayload(HexFormat.of().parseHex("9F0206"));
		assertEquals(0x9F02, parser.nextTag());
		assertEquals(6, parser.getLength());

		// the range is 5A01
		parser.setPayload(HexFormat.of().parseHex("FF5A01AA"), 1, 2);
		assertPayloadParserLength(parser, 0x5A, 1);
	}

	@Test
	public void testHeldPayloadParsersAreEmptyAfterSetBuffer() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("5A0101" + "8C039F0206"));
		assertEquals(0x5A, parser.nextTag());
		assertEquals(0x8C, parser.nextTag());

		// payload offset 5, length 3
		LenientTlvPullParser tlv = parser.parseTagLengthValuePayload();
		TagLengthPullParser tl = parser.parseTagLengthPayload();

		parser.setBuffer(new byte[] {0x01});

		assertEmpty(tlv);
		assertEquals(-1, tlv.nextTag());
		assertEmpty(tlv);

		assertEquals(0, tl.getLength());
		assertEquals(-1, tl.nextTag());
		assertEquals(0, tl.getLength());
	}

	@Test
	public void testHeldPayloadParsersDoNotParseTheNewBuffer() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("8C039F0206"));
		assertEquals(0x8C, parser.nextTag());

		LenientTlvPullParser tlv = parser.parseTagLengthValuePayload();
		TagLengthPullParser tl = parser.parseTagLengthPayload();

		// the payload parsers have the array, but not an offset and a length
		byte[] bytes = HexFormat.of().parseHex("5A0101" + "5F240102");
		parser.setBuffer(bytes);

		assertSame(bytes, tlv.getBuffer());
		assertEquals(-1, tlv.nextTag());
		assertEmpty(tlv);

		assertSame(bytes, tl.getBuffer());
		assertEquals(-1, tl.nextTag());
		assertEquals(0, tl.getLength());

		// the parser itself parses the new buffer
		assertEquals(0x5A, parser.nextTag());
		assertEquals(0x5F24, parser.nextTag());
		assertArrayEquals(new byte[] {0x02}, parser.getPayload());
	}

	@Test
	public void testHeldPayloadParsersOfPayloadParsersAreEmptyAfterSetBuffer() {
		LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("700A" + "6F035A0101" + "8C039F0206"));
		assertEquals(0x70, parser.nextTag());

		LenientTlvPullParser first = parser.parseTagLengthValuePayload();
		assertEquals(0x6F, first.nextTag());
		LenientTlvPullParser second = first.parseTagLengthValuePayload();
		assertEquals(0x8C, first.nextTag());
		TagLengthPullParser tl = first.parseTagLengthPayload();

		byte[] bytes = HexFormat.of().parseHex("5A0101");
		parser.setBuffer(bytes);

		assertSame(bytes, first.getBuffer());
		assertEmpty(first);
		assertEquals(-1, first.nextTag());

		assertSame(bytes, second.getBuffer());
		assertEmpty(second);
		assertEquals(-1, second.nextTag());

		assertSame(bytes, tl.getBuffer());
		assertEquals(0, tl.getLength());
		assertEquals(-1, tl.nextTag());
		assertEquals(0, tl.getLength());
	}

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 2, 3, 4, 5})
	public void testPayloadParsersAfterSetBuffer(int depth) {
		LenientTlvPullParser parser = LenientTlvPullParser.newInstance(depth);

		// the same data objects, at different offsets
		assertPayloadParsers(parser, "5A0109" + "5A0109", "");
		assertPayloadParsers(parser, "", "5A0109");
		assertPayloadParsers(parser, "5A0109", "");
		assertPayloadParsers(parser, "5A0109", "");
	}

	private static void assertPayloadParsers(LenientTlvPullParser parser, String before, String after) {
		String hex = before + "700A" + "6F08" + "5A0102" + "8C039F0206" + after;
		int offset = before.length() / 2;

		parser.setBuffer(HexFormat.of().parseHex(hex));
		assertEmpty(parser);
		assertEquals(0, parser.getBufferPayloadOffset());

		assertTrue(parser.skipTo(0x70));
		assertEquals(offset + 2, parser.getBufferPayloadOffset());
		assertEquals(10, parser.getBufferPayloadLength());

		LenientTlvPullParser first = parser.parseTagLengthValuePayload();
		assertEmpty(first);
		assertEquals(offset + 2, first.getBufferPayloadOffset());
		assertEquals(0x6F, first.nextTag());
		assertEquals(offset + 4, first.getBufferPayloadOffset());
		assertEquals(8, first.getBufferPayloadLength());

		LenientTlvPullParser second = first.parseTagLengthValuePayload();
		assertEmpty(second);
		assertEquals(0x5A, second.nextTag());
		assertArrayEquals(new byte[] {0x02}, second.getPayload());
		assertEquals(offset + 6, second.getBufferPayloadOffset());
		assertEquals(0x8C, second.nextTag());
		assertArrayEquals(HexFormat.of().parseHex("9F0206"), second.getPayload());

		LenientTlvPullParser third = second.parseTagLengthValuePayload();
		assertEmpty(third);
		assertEquals(offset + 9, third.getBufferPayloadOffset());

		TagLengthPullParser tl = second.parseTagLengthPayload();
		assertPayloadParserLength(tl, 0x9F02, 6);

		assertEquals(-1, second.nextTag());
		assertEquals(-1, first.nextTag());

		// the payload parsers are reused
		assertSame(first, parser.parseTagLengthValuePayload());
		assertSame(second, first.parseTagLengthValuePayload());
		assertSame(third, second.parseTagLengthValuePayload());
		assertSame(tl, second.parseTagLengthPayload());
	}

	private static void assertPayloadParserLength(TagLengthPullParser parser, int tag, int length) {
		// no length until a tag has been read
		assertEquals(0, parser.getLength());

		assertEquals(tag, parser.nextTag());
		assertEquals(length, parser.getLength());

		assertEquals(-1, parser.nextTag());
		assertEquals(0, parser.getLength());
	}

	private static void assertNoBuffer(LenientTlvPullParser parser) {
		assertNotNull(parser.getBuffer());
		assertEquals(0, parser.getBuffer().length);

		assertEmpty(parser);
		assertEquals(-1, parser.nextTag());
		assertEmpty(parser);
		assertEquals(0, parser.getBufferPayloadOffset());

		TagLengthPullParser tl = parser.parseTagLengthPayload();
		assertNotNull(tl.getBuffer());
		assertEquals(0, tl.getLength());
		assertEquals(-1, tl.nextTag());
		assertEquals(0, tl.getLength());
	}

	/**
	 * Assert that there is no payload, and that this does not throw an exception.
	 */

	private static void assertEmpty(LenientTlvPullParser parser) {
		assertArrayEquals(new byte[0], parser.getPayload());
		assertEquals(0, parser.getBufferPayloadLength());

		// where the next tag is, or would have been
		assertEquals(parser.getNextTagOffset(), parser.getBufferPayloadOffset());

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		parser.writePayload(bout);
		assertEquals(0, bout.size());

		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			parser.writePayload((OutputStream)out);
		} catch(IOException e) {
			throw new AssertionError(e);
		}
		assertEquals(0, out.size());
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
