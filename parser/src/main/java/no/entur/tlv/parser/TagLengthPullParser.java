package no.entur.tlv.parser;

import java.io.ByteArrayOutputStream;

public class TagLengthPullParser {

	protected byte[] buffer; 
	protected int limit;
	protected int offset;
	
	protected int length;
	
	public TagLengthPullParser() {
	}

	public TagLengthPullParser(byte[] buffer) {
		this(buffer, 0, buffer.length);
	}
	
	public TagLengthPullParser(byte[] buffer, int offset, int length) {
		setPayload(buffer, offset, length);
	}

	/**
	 * Get the next tag, or -1 if no more tags are available.
	 *
	 * The value -1 is also returned if the data is malformed. If the tag is longer than 4 bytes (it does not fit in an int),
	 * the parser stops: this and all later calls return -1, also if more data follows.
	 *
	 * @return next tag, or -1
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
		} while(b == 0x00); // padding; FF is not padding, it is a valid first tag byte in EMV

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
				if((tag >>> 16) != 0) {
					// tag of more than 4 bytes, which does not fit in an int
					offset = limit;
					return -1;
				}
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

		if((b & 0xFF) == 0x80) {
			// a tag-length list has a one-byte length (EMV Book 3, 5.4), so this is 128 rather than the indefinite length form
			payloadLength = 128;
		} else
		if ((b & 0x80) == 0x00) {
			/* short form */
			payloadLength = b;
		} else {
			/* long form */
			int count = b & 0x7F;
			if(count > 4 || count > limit - offset) {
				// length does not fit in an int, or not enough bytes for count
				offset = limit;
				return -1;
			}
			
			payloadLength = 0;
			for (int i = 0; i < count; i++) {
				b = buffer[offset] & 0xFF;
				offset++;

				payloadLength <<= 8;
				payloadLength += b & 0xFF;
			}
			if(payloadLength < 0) {
				// length does not fit in an int
				offset = limit;
				return -1;
			}
		}
		this.length = payloadLength;
		
		return tag;
	}

	public byte[] getBuffer() {
		return buffer;
	}

	public int getLength() {
		return length;
	}

	public void setPayload(byte[] bytes, int offset, int length) {
		this.buffer = bytes;
		this.offset = offset;
		this.limit = offset + length;
	}
	
	/**
	 * Write the tags (without lengths) of the remaining tag-length pairs to a stream. <code>00</code> padding before a
	 * tag is skipped. Stops at the first incomplete pair.
	 * <p>
	 * This consumes the parser: the rest of the payload is read, so {@link #nextTag()} returns -1 and
	 * {@link #getLength()} returns 0 afterwards, until a new payload is set.
	 *
	 * @param bout stream to write to
	 * @return number of tags written
	 */
	public int writeTagsToStream(ByteArrayOutputStream bout) {
		// no tag is current after this
		this.length = 0;

		int tagCount = 0;
		while(true) {
			// skip padding; only 00, as FF is a valid first tag byte in EMV (FF60, FF8106)
			while(offset < limit && buffer[offset] == 0x00) {
				offset++;
			}

			if(offset >= limit) {
				return tagCount;
			}

			int startOffset = offset;
			
			int b = buffer[offset];
			offset++;
			if(offset >= limit) {
				// not enough bytes for payload length
				return tagCount;
			}

			switch (b & 0x1F) {
			case 0x1F:
				b = buffer[offset];
				offset++;
				if(offset >= limit) {
					// not enough bytes for payload length
					return tagCount;
				}
				while ((b & 0x80) == 0x80) {
					b = buffer[offset];
					offset++;
					if(offset >= limit) {
						// not enough bytes for payload length
						return tagCount;
					}
				}
				break;
			default:
				break;
			}
			
			int endOffset = offset;
	
			b = buffer[offset];
			offset++;
	
			if ((b & 0x80) != 0x00) {
				/* long form */
				int count = b & 0x7F;
				offset += count;
			}
			if(offset > limit) {
				// not enough bytes for the length; leave the parser at the end
				offset = limit;
				return tagCount;
			}
			bout.write(buffer, startOffset, endOffset - startOffset);
			tagCount++;
		}		
	}

	public void setPayload(byte[] bytes) {
		setPayload(bytes, 0, bytes.length);
	}
	
	protected void setPayloadBuffer(byte[] bytes) {
		this.buffer = bytes;
	}
	
	protected void setPayloadOffsetLength(int offset, int length) {
		this.offset = offset;
		this.limit = offset + length;
		this.length = length;
	}
	
	
}
