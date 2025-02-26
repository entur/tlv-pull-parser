package no.entur.tlv.utils;

public class MutableByteArrayKey extends ByteArrayKey {

	public MutableByteArrayKey(byte[] content, int offset, int length) {
		super(content, offset, length);
	}

	public MutableByteArrayKey(byte[] content) {
		super(content);
	}

	public void setContent(byte[] content, int offset, int length) {
		this.content = content;
		this.offset = offset;
		this.limit = offset + length;
	}
	
}
