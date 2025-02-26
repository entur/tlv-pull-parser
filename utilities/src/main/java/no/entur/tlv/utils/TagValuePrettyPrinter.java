package no.entur.tlv.utils;

public interface TagValuePrettyPrinter {

	void append(byte[] payload, int offset, int length, StringBuilder output, int indent);
	
}
