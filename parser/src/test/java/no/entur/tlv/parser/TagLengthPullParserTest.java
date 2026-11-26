package no.entur.tlv.parser;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HexFormat;
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
	public void testWriteHeaders() throws IOException {
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
		
		ByteArrayOutputStream tagsOutput = new ByteArrayOutputStream();
		parser.writeTagsToStream(tagsOutput);

		parser.setPayload(bout.toByteArray(), 0, bout.size());
		
		TLVInputStream in = new TLVInputStream(new ByteArrayInputStream(bout.toByteArray()));
		
		for (ITag iTag : tags) {
			assertEquals(in.readTag(), parser.nextTag(), iTag.toString());
			assertEquals(in.readLength(), parser.getLength(), iTag.toString());
		}
	}

	@Test
	public void testLongFormLengthOverflowingToNegativeIsMalformed() {
		// 9F02 06, then 9F03 with length 84 80 00 00 00 = 0x80000000, which is negative as an int
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("9F0206" + "9F038480000000" + "9F1A02"));
		assertEquals(0x9F02, parser.nextTag());
		assertEquals(6, parser.getLength());

		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testLongFormLengthWithMoreBytesThanFitInAnIntIsMalformed() {
		// 9F03 with length 85 01 00 00 00 00 = 2^32, which truncates to 0 as an int
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("9F03850100000000" + "9F1A02"));
		assertEquals(-1, parser.nextTag());
		assertEquals(-1, parser.nextTag());
	}

	@Test
	public void testLongFormLengthWithFourBytes() {
		// 9F03 with length 84 7F FF FF FF = Integer.MAX_VALUE, then 9F1A 02
		TagLengthPullParser parser = new TagLengthPullParser(HexFormat.of().parseHex("9F03847FFFFFFF" + "9F1A02"));
		assertEquals(0x9F03, parser.nextTag());
		assertEquals(Integer.MAX_VALUE, parser.getLength());
		assertEquals(0x9F1A, parser.nextTag());
		assertEquals(2, parser.getLength());
		assertEquals(-1, parser.nextTag());
	}

}
