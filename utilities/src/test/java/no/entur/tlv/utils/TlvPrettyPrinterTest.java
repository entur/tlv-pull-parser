package no.entur.tlv.utils;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class TlvPrettyPrinterTest {

	private static final byte[] MESSAGE1 = HexFormat.of().parseHex("6F568407A0000000041010A54B50104465626974204D6173746572636172648701015F2D086E6F656E646173769F1101019F12104D617374657263617264204465626574BF0C119F0A04000101019F6E0705780000303000");

	private static final byte[] MESSAGE2 = HexFormat.of().parseHex("7081E08F01059F320103922404F0DF9E4DC1E925D78169E27D0994DB57A7618113FB66AAD1E2FA1A217BB7DFC33C6B5B9081B02474B5620F1EDBA21827B346865CEA928A767374D9259C011FB1EF8EA44929FEA186F4C6DA80AF11783158ECA2E3273ABC65B2DB96F2AE19E4F8E0FEF2BC97F0B77E6C920491D6DFB8485B91B4B28A142A27A89FB2818E513005854C5083CCF69E44273071362BC45126CA1357E6EC8F8F9B762502A78F36F4E0AA646AE202B713D60709D62040EDFB78A1E34AD7E05089C3914FB58549CBCD47D2A277D140B039D8DA4CEB8AB0EA8A6EE71A16D8CE42");

	// https://stackoverflow.com/questions/74757198/emv-internal-authenticate-6985-respond
	private static final byte[] MESSAGE3 = HexFormat.of().parseHex("6F4F8407A0000000031010A544500A564953412044454249548701019F38189F66049F02069F03069F1A0295055F2A029A039C019F37045F2D02656EBF0C129F5A0511084008405F550255534203474165");

	private static TlvPrettyPrinter PRETTY_PRINTER;

	@BeforeAll
	public static void init() throws IOException {
		InputStream is = TlvPrettyPrinterTest.class.getResourceAsStream("/emv/tags.csv");
		InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8);

		Set<ByteArrayKey> strings = new HashSet<>();

		Map<ByteArrayKey, String> tags = new HashMap<>();
		CSVParser parser = CSVParser.parse(reader, CSVFormat.RFC4180);
		for (CSVRecord csvRecord : parser) {
			String tag = csvRecord.get(0);
			String description = csvRecord.get(1);

			ByteArrayKey key = ByteArrayKey.create(tag);

			tags.put(key, description);

			if(csvRecord.size() > 2) {
				String type = csvRecord.get(2);
				if(type.equals("string")) {
					strings.add(key);
				}
			}
		}

		PRETTY_PRINTER = TlvPrettyPrinter.newBuilder().withStrings(strings).withTags(tags).build();
	}

	@Test
	public void test1() {
		String prettyPrinted = PRETTY_PRINTER.tlv(MESSAGE1).toString();
		System.out.println(prettyPrinted);
		assertTrue(prettyPrinted.contains("Debit Mastercard"));
	}

	@Test
	public void test2() {
		String prettyPrinted = PRETTY_PRINTER.tlv(MESSAGE2).toString();
		System.out.println(prettyPrinted);
		assertTrue(prettyPrinted.contains("Issuer Public Key Remainder"));
	}

	@Test
	public void test3() {
		String prettyPrinted = PRETTY_PRINTER.tlv(MESSAGE3).toString();
		System.out.println(prettyPrinted);
		assertTrue(prettyPrinted.contains("Processing Options Data Object List (PDOL)"));
	}

}
