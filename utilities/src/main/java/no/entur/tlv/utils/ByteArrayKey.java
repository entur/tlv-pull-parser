package no.entur.tlv.utils;

public class ByteArrayKey  {

	public static ByteArrayKey create(String s) {
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
		}
		return new ByteArrayKey(data);
	}

	protected byte[] content;
	protected int offset;
	protected int limit;

	public ByteArrayKey(byte[] content) {
		this(content, 0, content.length);
	}

	public ByteArrayKey(byte[] content, int offset, int length) {
		this.content = content;
		this.offset = offset;
		this.limit = offset + length;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder((limit - offset) * 2);
		for (int i = offset; i < limit; i++) {
			sb.append(String.format("%02X", content[i]));
		}
		return sb.toString();
	}

	@Override
	public int hashCode() {
		int result = 1;
		result = 31  + byteArrayHashCode();
		return result;
	}
	
    private int byteArrayHashCode() {
        int result = 1;
        for (int i = offset; i < limit; i++) {
            result = 31 * result + content[i];
        }

        return result;
    }

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof ByteArrayKey)) 
			return false;
		ByteArrayKey other = (ByteArrayKey) obj;
		
		
		int length = limit - offset;
		if(length != other.limit - other.offset) {
			return false;
		}
		
		for(int i = 0; i < length; i++) {
			if(content[offset + i] != other.content[other.offset + i]) {
				return false;
			}
		}
		
		return true;
	}
	
}
