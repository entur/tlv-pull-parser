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
		setPayload(buffer);
	}
	
	public TagLengthPullParser(byte[] buffer, int offset, int length) {
		setPayload(buffer, offset, length);
	}

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
				b = buffer[offset] & 0xFF;
				offset++;

				payloadLength <<= 8;
				payloadLength += b & 0xFF;
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
	
	public int writeTagsToStream(ByteArrayOutputStream bout) {
		int tagCount = 0;
		while(true) {
			if(offset >= limit) {
				return tagCount;
			}
			
			while(buffer[offset] == 0x00 || buffer[offset] == 0xFF) {
				offset++;
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
			if(limit <= offset) {
				bout.write(buffer, startOffset, endOffset - startOffset);
			}
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
