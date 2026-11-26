package no.entur.tlv.utils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

// https://stackoverflow.com/questions/58299515/read-emv-data-from-mastercard-visa-debit-credit-card

public class TlvPrettyPrinter {

	public static Builder newBuilder() {
		return new Builder();
	}
	
	public static class Builder {

		public static Builder newBuilder() {
			return new Builder();
		}

		private Map<ByteArrayKey, TagValuePrettyPrinter> prettyPrinters = new HashMap<>();

		private Map<ByteArrayKey, String> tags = new HashMap<>();
		private Set<ByteArrayKey> strings = new HashSet<>();

		public Builder withTags(Map<ByteArrayKey, String> tags) {
			this.tags.putAll(tags);
			return this;
		}

		public Builder withTag(ByteArrayKey key, String string) {
			this.tags.put(key, string);
			return this;
		}

		public Builder withStrings(Set<ByteArrayKey> strings) {
			this.strings.addAll(strings);
			return this;
		}

		public Builder withString(ByteArrayKey key) {
			this.strings.add(key);
			return this;
		}

		public Builder withPrettyPrinter(int tagAsBytes, ValueParser<?> parser) {
			return withPrettyPrinter(tagAsBytes, new ToStringTagValuePrettyPrinter(parser));
		}

		public Builder withPrettyPrinter(int tagAsBytes, TagValuePrettyPrinter prettyPrinter) {

			if(tagAsBytes >= 0x10000) {
				throw new IllegalArgumentException();
			}

			byte[] key;
			if(tagAsBytes >= 0x100) {
				key = new byte[] {(byte) ((tagAsBytes >>> 8) & 0xFF), (byte) ((tagAsBytes >>> 0) & 0xFF)};
			} else {
				key = new byte[] {(byte) ((tagAsBytes >>> 0) & 0xFF)};
			}

			prettyPrinters.put(new ByteArrayKey(key), prettyPrinter);

			return this;
		}

		public TlvPrettyPrinter build() {
			return new TlvPrettyPrinter(strings, tags, prettyPrinters);
		}

	}

	
	private final Map<ByteArrayKey, String> tags;
	private final Set<ByteArrayKey> strings;
	private final Map<ByteArrayKey, TagValuePrettyPrinter> values;
	
	public TlvPrettyPrinter(Set<ByteArrayKey> strings, Map<ByteArrayKey, String> tags, Map<ByteArrayKey, TagValuePrettyPrinter> values) {
		this.tags = tags;
		this.strings = strings;
		this.values = values;
	}

	public String responseAPDU(byte[] buffer) {
		return tlv(buffer, 0, buffer.length - 2).toString();
	}

	public String responseAPDU(byte[] buffer, int indent) {
		return tlv(buffer, 0, buffer.length - 2, indent).toString();
	}

	public StringBuilder tlv(byte[] buffer) {
		return tlv(buffer, 0, buffer.length);
	}

	public StringBuilder tlv(byte[] buffer, int indent) {
		return tlv(buffer, 0, buffer.length, indent);
	}

	public StringBuilder tlv(byte[] buffer, int offset, int length) {
		return tlv(buffer, offset, length, 0);
	}
	
	public StringBuilder tlv(byte[] buffer, int offset, int length, int indent) {
		StringBuilder stringBuilder = new StringBuilder();

		tlv(buffer, offset, offset + length, stringBuilder, 0, indent);

		return stringBuilder;
	}

	public void tlv(byte[] buffer, int offset, int limit, StringBuilder stringBuilder, int level, int indent) {
		while(offset < limit) {
			int tag;
			int b;
			do {
				b = buffer[offset];
				offset++;
				if(offset >= limit) {
					return;
				}
			} while(b == 0x00); // padding; FF is not padding, it is a valid first tag byte in EMV

			// after any padding
			int tagStartOffset = offset - 1;

			boolean constructed = (b & 0x20) != 0;

			switch (b & 0x1F) {
			case 0x1F:
				tag = (b & 0xFF); /* We store the first byte including LHS nibble */
				b = buffer[offset];
				offset++;
				if(offset >= limit) {
					// not enough bytes for payload length

					return;
				}
				while ((b & 0x80) == 0x80) {
					tag <<= 8;
					tag |= (b & 0x7F);
					b = buffer[offset];
					offset++;
					if(offset >= limit) {
						// not enough bytes for payload length

						return;
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

			int tagLength = offset - tagStartOffset;

			for(int i = 0; i < indent; i++) {
				stringBuilder.append(' ');
			}
			for(int i = 0; i < level * 3; i++) {
				stringBuilder.append(' ');
			}
			
			for(int i = tagStartOffset; i <= offset; i++) {
				stringBuilder.append(String.format("%02X", buffer[i]));
				stringBuilder.append(' ');
			}
			ByteArrayKey key = new ByteArrayKey(buffer, tagStartOffset, tagLength);
			String description = tags.get(key);
			if(description != null)  {
				stringBuilder.append('-');
				stringBuilder.append('-');
				stringBuilder.append(' ');
				stringBuilder.append(description);
				stringBuilder.append(' ');
			}
			
			TagValuePrettyPrinter tagValuePrettyPrinter = values.get(key);

			int payloadLength;

			b = buffer[offset];
			offset++;

			if((b & 0xFF) == 0x80) {
				// indefinite length, which ISO/IEC 7816-4 does not use; this level ends here
				stringBuilder.append('\n');
				return;
			}

			if ((b & 0x80) == 0x00) {
				/* short form */
				payloadLength = b;
			} else {
				/* long form */
				int count = b & 0x7F;
				if(count > 4 || count > limit - offset) {
					// length does not fit in an int, or not enough bytes for count
					malformedLength(stringBuilder);
					return;
				}

				payloadLength = 0;
				for (int i = 0; i < count; i++) {
					b = buffer[offset];
					offset++;

					payloadLength <<= 8;
					payloadLength += b & 0xFF;
				}
			}

			if(payloadLength < 0 || payloadLength > limit - offset) {
				// length does not fit in an int, or not enough bytes for payload
				malformedLength(stringBuilder);
				return;
			}

			int payloadOffset = offset;

			offset += payloadLength;

			 if(isTagLength(tag)) {
				stringBuilder.append('\n');
				tl(buffer, payloadOffset, payloadOffset + payloadLength, stringBuilder, level + tagLength + 1, indent);			
			 } else if(constructed) {
				stringBuilder.append('\n');
				tlv(buffer, payloadOffset, payloadOffset + payloadLength, stringBuilder, level + tagLength + 1, indent);
			} else {
				boolean certificate = isCertificateData(tag);
				
				int insets = level + tagLength + 1;
				stringBuilder.append('\n');
				for(int i = 0; i < indent; i++) {
					stringBuilder.append(' ');
				}
				for(int i = 0; i < insets * 3; i++) {
					stringBuilder.append(' ');
				}

				if(strings.contains(key)) {
					stringBuilder.append(new String(buffer, payloadOffset, payloadLength, StandardCharsets.UTF_8));
					stringBuilder.append(' ');
				} else {
					for(int i = payloadOffset; i < payloadOffset + payloadLength; i++) {
						stringBuilder.append(String.format("%02X", buffer[i]));
						if(!certificate) {
							stringBuilder.append(' ');
						}
					}
				}
				
				stringBuilder.append('\n');
				
				if(tagValuePrettyPrinter != null) {
					tagValuePrettyPrinter.append(buffer, payloadOffset, payloadLength, stringBuilder, indent + insets * 3);
					stringBuilder.append('\n');
				}
				
			}
		}
	}

	private boolean isTagLength(int tag) {
		return tag == 0x8C || tag == 0x8D || tag == 0x9F38;
	}

	private boolean isCertificateData(int tag) {
		
		// issuer
		switch(tag) {
		case 0x92: 
		case 0x9F32:
		case 0x90:
			return true;
		default :
		}
		
		// icc
		switch(tag) {
		case 0x9F48: 
		case 0x9F47:
		case 0x9F46:
			return true;
		default :
		}
		
		return false;
	}

	public void tl(byte[] buffer, int offset, int limit, StringBuilder stringBuilder, int level, int indent) {
		while(offset < limit) {
			int tag;
			int b;
			do {
				b = buffer[offset];
				offset++;
				if(offset >= limit) {
					return;
				}
			} while(b == 0x00); // padding; FF is not padding, it is a valid first tag byte in EMV

			// after any padding
			int tagStartOffset = offset - 1;

			boolean constructed = (b & 0x20) != 0;

			switch (b & 0x1F) {
			case 0x1F:
				tag = (b & 0xFF); /* We store the first byte including LHS nibble */
				b = buffer[offset];
				offset++;
				if(offset >= limit) {
					// not enough bytes for payload length

					return;
				}
				while ((b & 0x80) == 0x80) {
					tag <<= 8;
					tag |= (b & 0x7F);
					b = buffer[offset];
					offset++;
					if(offset >= limit) {
						// not enough bytes for payload length

						return;
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

			int tagLength = offset - tagStartOffset;
			for(int i = 0; i < indent; i++) {
				stringBuilder.append(' ');
			}
			for(int i = 0; i < level * 3; i++) {
				stringBuilder.append(' ');
			}
			for(int i = tagStartOffset; i <= offset; i++) {
				stringBuilder.append(String.format("%02X", buffer[i]));
				stringBuilder.append(' ');
			}
			ByteArrayKey key = new ByteArrayKey(buffer, tagStartOffset, tagLength);
			String description = tags.get(key);
			if(description != null)  {
				stringBuilder.append('-');
				stringBuilder.append('-');
				stringBuilder.append(' ');
				stringBuilder.append(description);
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
					malformedLength(stringBuilder);
					return;
				}

				payloadLength = 0;
				for (int i = 0; i < count; i++) {
					b = buffer[offset];
					offset++;

					payloadLength <<= 8;
					payloadLength += b & 0xFF;
				}
			}

			if(payloadLength < 0) {
				// length does not fit in an int
				malformedLength(stringBuilder);
				return;
			}

			stringBuilder.append('\n');
		}
	}
	
	public void tagLengthWithExternalValue(byte[] buffer, int offset, int limit, StringBuilder stringBuilder, int level, int indent, byte[] values, int valuesOffset) {
		while(offset < limit) {
			int tag;
			int b;
			do {
				b = buffer[offset];
				offset++;
				if(offset >= limit) {
					return;
				}
			} while(b == 0x00); // padding; FF is not padding, it is a valid first tag byte in EMV

			// after any padding
			int tagStartOffset = offset - 1;

			boolean constructed = (b & 0x20) != 0;

			switch (b & 0x1F) {
			case 0x1F:
				tag = (b & 0xFF); /* We store the first byte including LHS nibble */
				b = buffer[offset];
				offset++;
				if(offset >= limit) {
					// not enough bytes for payload length

					return;
				}
				while ((b & 0x80) == 0x80) {
					tag <<= 8;
					tag |= (b & 0x7F);
					b = buffer[offset];
					offset++;
					if(offset >= limit) {
						// not enough bytes for payload length

						return;
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

			int tagLength = offset - tagStartOffset;
			for(int i = 0; i < indent; i++) {
				stringBuilder.append(' ');
			}
			for(int i = 0; i < level * 3; i++) {
				stringBuilder.append(' ');
			}
			for(int i = tagStartOffset; i <= offset; i++) {
				stringBuilder.append(String.format("%02X", buffer[i]));
				stringBuilder.append(' ');
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
					malformedLength(stringBuilder);
					return;
				}

				payloadLength = 0;
				for (int i = 0; i < count; i++) {
					b = buffer[offset];
					offset++;

					payloadLength <<= 8;
					payloadLength += b & 0xFF;
				}
			}
			
			if(payloadLength < 0 || payloadLength > values.length - valuesOffset) {
				// length does not fit in an int, or not enough bytes for value
				malformedLength(stringBuilder);
				return;
			}

			ByteArrayKey key = new ByteArrayKey(buffer, tagStartOffset, tagLength);
			String description = tags.get(key);
			if(description != null)  {
				stringBuilder.append('-');
				stringBuilder.append('-');
				stringBuilder.append(' ');
				stringBuilder.append(description);
			}
			stringBuilder.append(':');

			stringBuilder.append('\n');
			
			for(int i = 0; i < indent; i++) {
				stringBuilder.append(' ');
			}
			for(int i = 0; i < level * 3 + tagLength * 3 + 3; i++) {
				stringBuilder.append(' ');
			}

			for(int i = 0; i < payloadLength; i++) {
				stringBuilder.append(String.format("%02X", values[valuesOffset + i]));
				stringBuilder.append(' ');
			}
			stringBuilder.setLength(stringBuilder.length() - 1);

			stringBuilder.append('\n');

			TagValuePrettyPrinter tagValuePrettyPrinter = this.values.get(key);
			if(tagValuePrettyPrinter != null) {
				tagValuePrettyPrinter.append(values, valuesOffset, payloadLength, stringBuilder, indent + level * 3 + tagLength * 3 + 3);
			}
			
			valuesOffset += payloadLength;
			
		}
	}

	private static void malformedLength(StringBuilder stringBuilder) {
		// end the line of an element which is not printed further, so that the output stays well-formed
		if(stringBuilder.length() > 0 && stringBuilder.charAt(stringBuilder.length() - 1) != ' ') {
			stringBuilder.append(' ');
		}
		stringBuilder.append("[malformed length]\n");
	}

}
