package no.entur.tlv.parser;


import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.github.devnied.emvnfccard.iso7816emv.EmvTags;
import com.github.devnied.emvnfccard.iso7816emv.ITag;

import net.sf.scuba.tlv.TLVInputStream;
import net.sf.scuba.tlv.TLVUtil;

public class TagLengthPullParserTest {

	public List<ITag> getTags() {
		List<ITag> tags = new ArrayList<>();
		Field[] fields = EmvTags.class.getFields();
		for (Field f : fields) {
			if (f.getType() == ITag.class) {
				try {
					tags.add((ITag) f.get(null));
				} catch (IllegalAccessException ex) {
					throw new RuntimeException(ex);
				}
			}
		}
		return tags;
	}
	
	public List<ITag> getTagsWithoutFF() {
		return getTags().stream().filter( (t) -> (t.getTagBytes()[0] & 0xFF) != 0xFF).collect(Collectors.toList());
	}

	@Test
	public void testParseShortLength() throws IOException {
		List<ITag> tags =  getTagsWithoutFF();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		int counter = 0;
		for (ITag iTag : tags) {
			bout.write(iTag.getTagBytes());
			bout.write(counter % 127);
			counter++;
		}
		
		TagLengthPullParser parser = new TagLengthPullParser();
		parser.setPayload(bout.toByteArray(), 0, bout.size());

		TLVInputStream in = new TLVInputStream(new ByteArrayInputStream(bout.toByteArray()));
		
		for (ITag iTag : tags) {
			assertEquals(in.readTag(), parser.nextTag());
			assertEquals(in.readLength(), parser.getLength());
		}
	}

	@Test
	public void testParseLongLength() throws IOException {
		List<ITag> tags =  getTagsWithoutFF();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();

		int counter = 0;
		for (ITag iTag : tags) {
			bout.write(iTag.getTagBytes());
			bout.write(TLVUtil.getLengthAsBytes(counter));
			counter++;
		}
		
		TagLengthPullParser parser = new TagLengthPullParser();
		parser.setPayload(bout.toByteArray(), 0, bout.size());

		TLVInputStream in = new TLVInputStream(new ByteArrayInputStream(bout.toByteArray()));
		
		for (ITag iTag : tags) {
			assertEquals(in.readTag(), parser.nextTag());
			assertEquals(in.readLength(), parser.getLength());
		}
	}
	
	@Test
	public void testWriteHeadersShortLength() throws IOException {
		assertWriteHeaders(false);
	}

	@Test
	public void testWriteHeadersLongLength() throws IOException {
		assertWriteHeaders(true);
	}

	private void assertWriteHeaders(boolean longLength) throws IOException {
		// all the tags, also those which start with FF
		List<ITag> tags = getTags();

		ByteArrayOutputStream bout = new ByteArrayOutputStream();
		ByteArrayOutputStream expectedTagsOutput = new ByteArrayOutputStream();

		int counter = 0;
		for (ITag iTag : tags) {
			bout.write(iTag.getTagBytes());
			if(longLength) {
				bout.write(TLVUtil.getLengthAsBytes(counter));
			} else {
				bout.write(counter % 127);
			}
			expectedTagsOutput.write(iTag.getTagBytes());
			counter++;
		}
		
		TagLengthPullParser parser = new TagLengthPullParser();
		parser.setPayload(bout.toByteArray(), 0, bout.size());
		
		ByteArrayOutputStream tagsOutput = new ByteArrayOutputStream();
		int count = parser.writeTagsToStream(tagsOutput);

		assertArrayEquals(expectedTagsOutput.toByteArray(), tagsOutput.toByteArray());
		assertEquals(tags.size(), count);

		// nothing left
		assertEquals(-1, parser.nextTag());

		// the same tags and as many, one by one
		parser.setPayload(bout.toByteArray(), 0, bout.size());

		counter = 0;
		for (ITag iTag : tags) {
			assertEquals(toTag(iTag.getTagBytes()), parser.nextTag(), iTag.toString());
			assertEquals(longLength ? counter : counter % 127, parser.getLength(), iTag.toString());
			counter++;
		}
		assertEquals(-1, parser.nextTag());
	}

	/**
	 * The value which {@linkplain TagLengthPullParser#nextTag()} returns for a tag: without the top bit of the
	 * subsequent bytes.
	 */

	private static int toTag(byte[] bytes) {
		int tag = bytes[0] & 0xFF;
		for (int i = 1; i < bytes.length; i++) {
			tag = (tag << 8) | (bytes[i] & 0x7F);
		}
		return tag;
	}

}
