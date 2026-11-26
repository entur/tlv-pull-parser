package no.entur.tlv.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import org.junit.jupiter.api.Test;

public class WriteTagsToStreamTest {

	@Test
	public void testWriteTags() {
		// a typical data object list (PDOL)
		assertTags("9F0206" + "9F0306" + "9F1A02" + "9505" + "5F2A02" + "9A03" + "9C01" + "9F3704", "9F02" + "9F03" + "9F1A" + "95" + "5F2A" + "9A" + "9C" + "9F37", 8);
	}

	@Test
	public void testWriteTagsWithLongFormLength() {
		assertTags("9F028180" + "9F03820100" + "9505", "9F02" + "9F03" + "95", 3);
	}

	@Test
	public void testWriteTagsWithLongFormLengthAtTheEnd() {
		// the last length byte is the last byte
		assertTags("958100", "95", 1);
		assertTags("9F0206" + "95820100", "9F02" + "95", 2);

		// one byte short
		assertTags("9581", "", 0);
		assertTags("9F0206" + "958201", "9F02", 1);
	}

	@Test
	public void testWriteTagsWithPadding() {
		// trailing padding ran past the end of the buffer
		assertTags("00" + "9F0206" + "0000" + "9505" + "0000", "9F02" + "95", 2);
		assertTags("9500" + "00", "95", 1);
	}

	@Test
	public void testWriteTagsStartingWithFF() {
		// FF is the first byte of a tag, not padding
		assertTags("FF6001", "FF60", 1);
		assertTags("FF810602" + "9505", "FF8106" + "95", 2);
		assertTags("9F0206" + "FF6001" + "00" + "FF810181FF", "9F02" + "FF60" + "FF8101", 3);
	}

	@Test
	public void testWriteTagsOfThreeBytes() {
		assertTags("DF810102", "DF8101", 1);
		assertTags("9F0206" + "DF810102" + "9505", "9F02" + "DF8101" + "95", 3);

		// no length
		assertTags("DF8101", "", 0);
		assertTags("9F0206" + "DF8101", "9F02", 1);
	}

	@Test
	public void testWriteTagsWithMissingLength() {
		assertTags("9F0206" + "95", "9F02", 1);
		assertTags("95", "", 0);
	}

	@Test
	public void testWriteTagsWithIncompleteTag() {
		assertTags("9F", "", 0);
		assertTags("DF81", "", 0);
		assertTags("9F0206" + "9F", "9F02", 1);
		assertTags("9F0206" + "DF81", "9F02", 1);
	}

	@Test
	public void testWriteTagsWithIncompleteLongFormLength() {
		assertTags("9F0206" + "958201", "9F02", 1);
		// 127 length bytes, which are not there
		assertTags("95FF", "", 0);
		assertTags("9F0206" + "95FF" + "9F0306", "9F02", 1);
	}

	@Test
	public void testWriteTagsWithLongFormLengthOfFourBytes() {
		// only the tags are copied; the length is not interpreted
		assertTags("9584FFFFFFFF", "95", 1);
		assertTags("9584FFFFFF", "", 0);
	}

	@Test
	public void testWriteTagsEmpty() {
		assertTags("", "", 0);
		assertTags("00", "", 0);
		assertTags("0000", "", 0);
	}

	@Test
	public void testWriteTagsOfRange() {
		// the pair which is cut short by the limit continues in the bytes after it
		byte[] buffer = HexFormat.of().parseHex("9505" + "9F0206" + "958201" + "0000" + "9A03");

		TagLengthPullParser parser = new TagLengthPullParser(buffer, 2, 6);
		assertTags(parser, "9F02", 1);
		assertEquals(8, parser.offset);

		// complete pairs up to the limit
		parser.setPayload(buffer, 2, 3);
		assertTags(parser, "9F02", 1);
		assertEquals(5, parser.offset);

		// a tag without its length
		parser.setPayload(buffer, 2, 4);
		assertTags(parser, "9F02", 1);
		assertEquals(6, parser.offset);

		// padding up to the limit
		parser.setPayload(buffer, 8, 1);
		assertTags(parser, "", 0);
		assertEquals(9, parser.offset);

		parser.setPayload(buffer, 2, 0);
		assertTags(parser, "", 0);
		assertEquals(2, parser.offset);

		parser.setPayload(buffer, buffer.length, 0);
		assertTags(parser, "", 0);
		assertEquals(buffer.length, parser.offset);
	}

	@Test
	public void testWriteTagsOfTagLengthPayload() {
		// a PDOL in a template, with a truncated last pair, and bytes after it
		byte[] buffer = HexFormat.of().parseHex("A509" + "9F3806" + "9F0206" + "958201" + "0000" + "9000");

		LenientTlvPullParser parser = new LenientTlvPullParser(buffer, 0, 11);
		assertEquals(0xA5, parser.nextTag());

		LenientTlvPullParser template = parser.parseTagLengthValuePayload();
		assertEquals(0x9F38, template.nextTag());

		TagLengthPullParser tagLengths = template.parseTagLengthPayload();
		assertTags(tagLengths, "9F02", 1);

		// again, with the same parser
		tagLengths = template.parseTagLengthPayload();
		assertTags(tagLengths, "9F02", 1);
	}

	@Test
	public void testWriteTagsConsumesParser() {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("9F0206" + "9505" + "9A03"));

		assertEquals(0x9F02, parser.nextTag());
		assertEquals(6, parser.getLength());

		// the rest
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		assertEquals(2, parser.writeTagsToStream(bout));
		assertEquals("959A", HexFormat.of().withUpperCase().formatHex(bout.toByteArray()));

		// no length of an earlier tag
		assertEquals(0, parser.getLength());
		assertEquals(-1, parser.nextTag());
		assertEquals(0, parser.getLength());

		// and nothing more to write
		assertEquals(0, parser.writeTagsToStream(bout));
		assertEquals(2, bout.size());
	}

	@Test
	public void testWriteTagsAgreesWithNextTag() {
		String[] inputs = {
			// complete pairs
			"9F0206" + "9F0306" + "9F1A02" + "9505" + "5F2A02" + "9A03" + "9C01" + "9F3704",
			"9500",
			// padding
			"00" + "9F0206" + "0000" + "9505" + "0000",
			"950000",
			"00",
			"0000",
			"",
			// tags starting with FF
			"FF6001",
			"FF810602" + "9505",
			"9F0206" + "FF6001" + "00" + "FF810181FF",
			// long-form lengths
			"9F028180" + "9F03820100" + "9505",
			"958100",
			"9583010000" + "9A03",
			"958400000001",
			// tags of three and four bytes
			"DF810102",
			"DF810102" + "9F0206" + "DF81810302",
			// truncated
			"95",
			"9F",
			"DF81",
			"DF8101",
			"9581",
			"95FF",
			"9F0206" + "95",
			"9F0206" + "9F",
			"9F0206" + "DF81",
			"9F0206" + "DF8101",
			"9F0206" + "9581",
			"9F0206" + "958201",
			"9F0206" + "95FF" + "9F0306",
		};

		for (String input : inputs) {
			byte[] bytes = HexFormat.of().parseHex(input);
			assertAgreesWithNextTag(input, bytes, 0, bytes.length);

			// within a larger buffer, which continues with a length and another pair
			byte[] buffer = HexFormat.of().parseHex("9A03" + input + "01" + "9C01");
			assertAgreesWithNextTag(input, buffer, 2, bytes.length);
		}
	}

	private static void assertAgreesWithNextTag(String input, byte[] buffer, int offset, int length) {
		List<Integer> expected = new ArrayList<>();
		TagLengthPullParser parser = new TagLengthPullParser(buffer, offset, length);
		do {
			int tag = parser.nextTag();
			if(tag == -1) {
				break;
			}
			expected.add(tag);
		} while(true);

		parser = new TagLengthPullParser(buffer, offset, length);
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		int count = parser.writeTagsToStream(bout);

		assertEquals(expected, toTags(bout.toByteArray()), input);
		assertEquals(expected.size(), count, input);
	}

	/**
	 * Split tag bytes into tags, as the values which {@linkplain TagLengthPullParser#nextTag()} returns: without the
	 * top bit of the subsequent bytes.
	 */

	private static List<Integer> toTags(byte[] bytes) {
		List<Integer> tags = new ArrayList<>();
		int i = 0;
		while(i < bytes.length) {
			int tag = bytes[i] & 0xFF;
			if((bytes[i] & 0x1F) == 0x1F) {
				do {
					i++;
					tag = (tag << 8) | (bytes[i] & 0x7F);
				} while((bytes[i] & 0x80) == 0x80);
			}
			i++;
			tags.add(tag);
		}
		return tags;
	}

	private static void assertTags(String tagLengths, String expectedTags, int expectedCount) {
		assertTags(new TagLengthPullParser(HexFormat.of().parseHex(tagLengths)), expectedTags, expectedCount);
	}

	private static void assertTags(TagLengthPullParser parser, String expectedTags, int expectedCount) {
		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		int count = parser.writeTagsToStream(bout);

		assertEquals(expectedTags, HexFormat.of().withUpperCase().formatHex(bout.toByteArray()));
		assertEquals(expectedCount, count);

		// the parser is consumed
		assertEquals(parser.limit, parser.offset);
		assertEquals(0, parser.getLength());
		assertEquals(-1, parser.nextTag());
	}

}
