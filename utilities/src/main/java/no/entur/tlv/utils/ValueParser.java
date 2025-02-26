package no.entur.tlv.utils;

public interface ValueParser<T> {

	T parse(byte[] payload, int offset, int length);
	
	default T parse(byte[] payload) {
		return parse(payload, 0, payload.length);
	}
	
}
