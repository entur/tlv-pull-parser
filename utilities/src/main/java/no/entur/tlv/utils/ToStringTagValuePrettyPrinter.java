package no.entur.tlv.utils;

public class ToStringTagValuePrettyPrinter implements TagValuePrettyPrinter {

	public static ToStringTagValuePrettyPrinter newInstance(ValueParser<?> parser) {
		return new ToStringTagValuePrettyPrinter(parser);
	}
	
	private final ValueParser<?> parser;
	
	public ToStringTagValuePrettyPrinter(ValueParser<?> parser) {
		this.parser = parser;
	}

	@Override
	public void append(byte[] payload, int offset, int length, StringBuilder output, int indent) {
		Object value = parser.parse(payload, offset, length);
		if(value == null) {
			return;
		}
		String string = value.toString();
		
		format(string, indent, output);
	}

	public static void format(String string, int indent, StringBuilder output) {
		
		for(int i = 0; i < indent + 3; i++) {
			output.append(' ');
		}

		if(!string.contains("[")) {
			output.append(string);
			output.append('\n');
			return;
		}
		
		int startIndex = 0;
		int level = 0;

		while(startIndex < string.length()) {
			int index = indexOfNextSpecialCharacter(string, startIndex);
			if(index == -1) {
				output.append(string, startIndex, string.length() - 1);
				
				break;
			}
			if(string.charAt(index) == '[') {
				// MastercardApplicationInterchangeProfile [
				if(string.charAt(index - 1) == ' ') {
					output.append(string, startIndex, index - 1);
				} else {
					output.append(string, startIndex, index);
				}
				if(level == 0) {
					output.append(":");
				}
				output.append('\n');
				
				level += 3;
			} else if(string.charAt(index) == '=') {
				// key
				for(int i = 0; i < level + indent + 3; i++) {
					output.append(' ');
				}
				writeSpaceBeforeEachUppercaseLetter(string, startIndex, index, output);
				output.append(": ");
			} else if(string.charAt(index) == ']') {
				output.append(string, startIndex, index);
				output.append('\n');
				level -= 3;
			} else if(string.charAt(index) == ',') {
				output.append(string, startIndex, index);
				output.append('\n');
				
				index++;
			}

			startIndex = index + 1;
		}
	}
	
	private static void writeSpaceBeforeEachUppercaseLetter(String string, int start, int end, StringBuilder output) {
		output.append(Character.toUpperCase(string.charAt(start)));
		for(int i = start + 1; i < end; i++) {
			
			char c = string.charAt(i);
			
			if(Character.isUpperCase(c)) {
				output.append(' ');
				output.append(Character.toLowerCase(c));
			} else {
				output.append(c);
			}
			
		}
	}

	private static int indexOfNextSpecialCharacter(String string, int fromIndex) {
		for(int i = fromIndex; i < string.length(); i++) {
			char c = string.charAt(i);
			switch(c) {
			case ',' : 
			case '=' : 
			case '[' : 
			case ']' : 
				return i;
			}
		}
		
		return -1;
	}

	// improve readability
	private void appendFieldName(StringBuilder output, String string, int start, int end) {
		output.append(Character.toUpperCase(string.charAt(start)));

		for(int i = start + 1; i < end; i++) {
			char c = string.charAt(i);
			if(Character.isUpperCase(c)) {
				
				if(i + 1 < end) {
					// skip over multiple uppercase chars
					if(Character.isUpperCase(string.charAt(i + 1))) {
						output.append(' ');
						output.append(c);

						i++;
						while(i + 1 < end && Character.isUpperCase(string.charAt(i))) {
							output.append(string.charAt(i));
							i++;
						}
						output.append(string.charAt(i));
						continue;
					}
				}
				
				output.append(' ');
				output.append(Character.toLowerCase(c));
			} else {
				output.append(c);
			}
		}
	}
	
}
