package no.entur.tlv.parser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * Pull-parser which allows for drilling down level by level using a child parser.
 * All child parsers work on the same byte-array instance.
 * The parser can be reused by updating the buffer / offset / length.
 */

public class LenientTlvPullParser {

	public static LenientTlvPullParser newInstance() {
		return new LenientTlvPullParser();
	}

	/**
	 *
	 * Create new and prepare it for parsing payloads to a specific depth.
	 * <br><br>
	 * Note that child parsers will be created on-demand if necessary.
	 *
	 * @param depth prepare level
	 * @return new parser
	 */

	public static LenientTlvPullParser newInstance(int depth) {
		LenientTlvPullParser root = new LenientTlvPullParser();

		LenientTlvPullParser parser = root;
		while(depth > 1) {
			parser.preparePayloadParsers();
			parser = parser.tlvPayloadParser;
			depth--;
		}

		return root;
	}

	private byte[] buffer;
	private int offset;
	private int limit;
	
	private int payloadOffset;
	private int payloadLength;
	
	private LenientTlvPullParser tlvPayloadParser;
	private TagLengthPullParser tagLengthPayloadParser;
	
	public LenientTlvPullParser() {
	}

	public LenientTlvPullParser(byte[] bytes) {
		setBuffer(bytes);
	}
	
	public LenientTlvPullParser(byte[] bytes, int offset, int length) {
		setBuffer(bytes, offset, length);
	}

	/**
	 * Get the next tag, or -1 if not more tags available.
	 *
	 * @return next tag
	 */

	public int nextTag() {
		if(offset >= limit) {
			return -1;
		}
		
		int tag = -1;
		int b;
		do {
			b = buffer[offset];
			offset++;
			if(offset >= limit) {
				return -1;
			}
		} while(b == 0x00 || b == 0xFF);

		switch (b & 0x1F) {
		case 0x1F:
			tag = (b & 0xFF); /* We store the first byte including LHS nibble */
			b = buffer[offset];
			offset++;
			if(offset >= limit) {
				// not enough bytes for payload length

				return -1;
			}
			while ((b & 0x80) == 0x80) {
				tag <<= 8;
				tag |= (b & 0x7F);
				b = buffer[offset];
				offset++;
				if(offset >= limit) {
					// not enough bytes for payload length

					return -1;
				}
			}
			tag <<= 8;
			tag |= (b & 0x7F);
			/*
			 * Byte with MSB set is last byte of
			 * tag...
			 */
			break;
		default:
			tag = (b & 0xFF);
			break;
		}
		
		int payloadLength;

		b = buffer[offset];
		offset++;

		if ((b & 0x80) == 0x00) {
			/* short form */
			payloadLength = b;
		} else {
			/* long form */
			int count = b & 0x7F;
			if(offset + count > limit) {
				// not enough bytes for count
				return -1;
			}
			
			payloadLength = 0;
			for (int i = 0; i < count; i++) {
				b = buffer[offset];
				offset++;

				payloadLength <<= 8;
				payloadLength += b & 0xFF;
			}
		}
		
		if(offset + payloadLength > limit) {
			return -1;
		}

		this.payloadOffset = offset;
		this.payloadLength = payloadLength;
		
		offset += payloadLength;
		
		return tag;
	}

	public int getNextTagOffset() {
		return offset;
	}

	public byte[] getPayload() {
		byte[] payload = new byte[payloadLength];
		System.arraycopy(buffer, payloadOffset, payload, 0, payloadLength);
		return payload;
	}

	public int getBufferPayloadOffset() {
		return payloadOffset;
	}
	
	public int getBufferPayloadLength() {
		return payloadLength;
	}

	public byte[] getBuffer() {
		return buffer;
	}

	public LenientTlvPullParser parseTagLengthValuePayload() {
		if(tlvPayloadParser == null) {
			tlvPayloadParser = new LenientTlvPullParser(buffer, payloadOffset, payloadLength);
		} else {
			tlvPayloadParser.setBufferOffsetLength(payloadOffset, payloadLength);
		}
		
		return tlvPayloadParser;
	}
	
	public TagLengthPullParser parseTagLengthPayload() {
		if(tagLengthPayloadParser == null) {
			tagLengthPayloadParser = new TagLengthPullParser(buffer, payloadOffset, payloadLength);
		} else {
			tagLengthPayloadParser.setPayloadOffsetLength(payloadOffset, payloadLength);
		}
		
		return tagLengthPayloadParser;
	}

	/**
	 *
	 * Set a payload from an ADPU response.
	 *
	 * @param bytes response ADPU, including to status bytes.
	 */
	
	public void setResponseAdpuPayload(byte[] bytes) {
		setBuffer(bytes, 0, bytes.length - 2);
	}

	public void setBuffer(byte[] bytes, int offset, int length) {
		this.buffer = bytes;
		setBufferOffsetLength(offset, length);

		// set array, but do not set offset/length on child parsers
		if(tlvPayloadParser != null) {
			tlvPayloadParser.setBuffer(bytes);
		}
		if(tagLengthPayloadParser != null) {
			tagLengthPayloadParser.setPayloadBuffer(bytes);
		}
	}
	public void setBuffer(byte[] bytes) {
		setBuffer(bytes, 0, bytes.length);
	}

	protected void setBufferOffsetLength(int offset, int length) {
		this.offset = offset;
		this.limit = offset + length;
	}

	public boolean skipTo(int targetTag) {
		do {
			int tag = nextTag();
			if(tag == -1) {
				return false;
			}
			if(tag == targetTag) {
				return true;
			}
		} while(true);
	}
	
	public LenientTlvPullParser parseTagLengthValuePayload(int skipTo) {
		int tag;
		do {
			tag = nextTag();
			if(tag == -1) {
				return null;
			}
			if(tag == skipTo) {
				return parseTagLengthValuePayload();
			}
		} while(true);
	}

	public TagLengthPullParser parseTagLengthPayload(int skipTo) {
		int tag;
		do {
			tag = nextTag();
			if(tag == -1) {
				return null;
			}
			if(tag == skipTo) {
				return parseTagLengthPayload();
			}
		} while(true);
	}

	public void writePayload(ByteArrayOutputStream out) {
		out.write(buffer, payloadOffset, payloadLength);
	}
	
	public void writePayload(OutputStream out) throws IOException {
		out.write(buffer, payloadOffset, payloadLength);
	}

	/**
	 *
	 * Initialize payload parsers
	 *
	 */

	protected void preparePayloadParsers() {
		tlvPayloadParser = new LenientTlvPullParser();
		tagLengthPayloadParser = new TagLengthPullParser();
	}

}
