package no.entur.tlv.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import java.util.HexFormat;

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
	public void testWriteTagsWithPadding() {
		// trailing padding ran past the end of the buffer
		assertTags("00" + "9F0206" + "FF" + "9505" + "0000", "9F02" + "95", 2);
	}

	@Test
	public void testWriteTagsWithMissingLength() {
		assertTags("9F0206" + "95", "9F02", 1);
	}

	@Test
	public void testWriteTagsWithIncompleteLongFormLength() {
		assertTags("9F0206" + "958201", "9F02", 1);
	}

	@Test
	public void testWriteTagsEmpty() {
		assertTags("", "", 0);
		assertTags("00FF", "", 0);
	}

	private static void assertTags(String tagLengths, String expectedTags, int expectedCount) {
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex(tagLengths));

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		int count = parser.writeTagsToStream(bout);

		assertEquals(expectedTags, HexFormat.of().withUpperCase().formatHex(bout.toByteArray()));
		assertEquals(expectedCount, count);
	}

}
