package no.entur.tlv.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Parse responses of real EMV cards, and check that every byte is accounted for.
 * <p>
 * The responses were recorded from real cards. Personal and card-specific values, and the application labels, are masked (keeping their
 * length), so the structure is the same as on the card. Each file states the number of elements, as counted when the responses were
 * exported.
 */

public class EmvCardResponsesTest {

	public static final Path DIRECTORY = Paths.get("src", "test", "resources", "emv");

	private static class Counts {
		int responses;
		int elements;
		int tagLengthEntries;
	}

	static Stream<Path> files() throws IOException {
		try (Stream<Path> files = Files.list(DIRECTORY)) {
			return files.filter(f -> f.toString().endsWith(".txt")).sorted().toList().stream();
		}
	}

	@ParameterizedTest
	@MethodSource("files")
	public void testParse(Path file) throws IOException {
		List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);

		Map<String, Integer> expected = new HashMap<>();
		Counts counts = new Counts();
		for (String line : lines) {
			if (line.startsWith("# expect:")) {
				for (String field : line.substring("# expect:".length()).trim().split(" ")) {
					String[] keyValue = field.split("=");
					expected.put(keyValue[0], Integer.parseInt(keyValue[1]));
				}
			} else if (!line.startsWith("#") && !line.isBlank()) {
				byte[] response = HexFormat.of().parseHex(line.split(" ")[1]);

				LenientTlvPullParser parser = new LenientTlvPullParser();
				parser.setResponseApduPayload(response);

				walk(parser, 0, response.length - 2, counts, file.getFileName() + " response " + counts.responses);
				counts.responses++;
			}
		}

		assertEquals(expected.get("responses"), counts.responses, file.toString());
		assertEquals(expected.get("elements"), counts.elements, file.toString());
		assertEquals(expected.get("tag-length-entries"), counts.tagLengthEntries, file.toString());
	}

	private static void walk(LenientTlvPullParser parser, int offset, int limit, Counts counts, String message) {
		// end of the last element
		int end = offset;

		int tag;
		while ((tag = parser.nextTag()) != -1) {
			counts.elements++;

			int payloadOffset = parser.getBufferPayloadOffset();
			int payloadLength = parser.getBufferPayloadLength();
			assertTrue(payloadOffset >= end && payloadOffset + payloadLength <= limit, message);
			end = payloadOffset + payloadLength;

			if (isTagLengthList(tag)) {
				counts.tagLengthEntries += tagLengthEntries(parser, message);
			} else if (isConstructed(tag)) {
				walk(parser.parseTagLengthValuePayload(), payloadOffset, end, counts, message);
			}
		}

		// all of the bytes were parsed, apart from any padding at the end
		byte[] buffer = parser.getBuffer();
		for (int i = end; i < limit; i++) {
			assertEquals(0, buffer[i], message);
		}
	}

	private static int tagLengthEntries(LenientTlvPullParser parser, String message) {
		TagLengthPullParser tagLengthParser = parser.parseTagLengthPayload();
		ByteArrayOutputStream tags = new ByteArrayOutputStream();
		int entries = 0;
		int tag;
		while ((tag = tagLengthParser.nextTag()) != -1) {
			writeTag(tag, tags);
			entries++;
		}

		// the same tags are written, in the same order
		TagLengthPullParser writer = new TagLengthPullParser(parser.getBuffer(), parser.getBufferPayloadOffset(), parser.getBufferPayloadLength());
		ByteArrayOutputStream written = new ByteArrayOutputStream();
		assertEquals(entries, writer.writeTagsToStream(written), message);
		assertArrayEquals(tags.toByteArray(), written.toByteArray(), message);

		return entries;
	}

	// the tag bytes as on the card: nextTag() leaves out the 'more bytes follow' bit of the bytes between the first and the last
	private static void writeTag(int tag, ByteArrayOutputStream out) {
		int length = 1;
		while (length < 4 && (tag >>> (length * 8)) != 0) {
			length++;
		}
		for (int i = length - 1; i >= 0; i--) {
			int b = (tag >>> (i * 8)) & 0xFF;
			if (i > 0 && i < length - 1) {
				b |= 0x80;
			}
			out.write(b);
		}
	}

	private static boolean isTagLengthList(int tag) {
		// CDOL1, CDOL2 and PDOL
		return tag == 0x8C || tag == 0x8D || tag == 0x9F38;
	}

	private static boolean isConstructed(int tag) {
		// the first byte of the tag is the most significant non-zero byte
		int first = tag;
		while ((first >>> 8) != 0) {
			first >>>= 8;
		}
		return (first & 0x20) != 0;
	}

}
