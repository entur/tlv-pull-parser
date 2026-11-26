package no.entur.tlv.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A recording of an EMV card (see {@link EmvCardResponsesTest}), read the way a kernel would: the PPSE directory, then for each selected
 * application its SELECT, GET PROCESSING OPTIONS, READ RECORD and GENERATE AC responses. All data is extracted with the parsers.
 */

public class EmvCard {

	public static final int SELECT = 0xA4;
	public static final int GET_PROCESSING_OPTIONS = 0xA8;
	public static final int READ_RECORD = 0xB2;
	public static final int GENERATE_AC = 0xAE;

	public static final int FCI_TEMPLATE = 0x6F;
	public static final int DF_NAME = 0x84;
	public static final int FCI_PROPRIETARY_TEMPLATE = 0xA5;
	public static final int FCI_ISSUER_DISCRETIONARY_DATA = 0xBF0C;
	public static final int DIRECTORY_ENTRY = 0x61;
	public static final int APPLICATION_IDENTIFIER = 0x4F;
	public static final int APPLICATION_PRIORITY_INDICATOR = 0x87;
	public static final int PDOL = 0x9F38;
	public static final int LANGUAGE_PREFERENCE = 0x5F2D;
	public static final int RESPONSE_MESSAGE_TEMPLATE_FORMAT_2 = 0x77;
	public static final int APPLICATION_INTERCHANGE_PROFILE = 0x82;
	public static final int APPLICATION_FILE_LOCATOR = 0x94;
	public static final int RECORD_TEMPLATE = 0x70;

	private static final HexFormat HEX = HexFormat.of().withUpperCase();

	public record Response(int ins, byte[] apdu) {
	}

	public static EmvCard load(String name) throws IOException {
		List<Response> responses = new ArrayList<>();
		for (String line : Files.readAllLines(EmvCardResponsesTest.DIRECTORY.resolve(name), StandardCharsets.UTF_8)) {
			if (line.isBlank() || line.startsWith("#")) {
				continue;
			}
			String[] fields = line.split(" ");
			responses.add(new Response(Integer.parseInt(fields[0], 16), HEX.parseHex(fields[1])));
		}
		return new EmvCard(responses);
	}

	private final List<Response> responses;

	public EmvCard(List<Response> responses) {
		this.responses = responses;
	}

	/**
	 * @return name of the directory selected first (6F / 84)
	 */

	public String directoryName() {
		return new String(values(open(responses.get(0).apdu()).parseTagLengthValuePayload(FCI_TEMPLATE)).get(DF_NAME), StandardCharsets.US_ASCII);
	}

	/**
	 * @return the directory entries (6F / A5 / BF0C / 61), as AID:priority
	 */

	public List<String> directory() {
		LenientTlvPullParser discretionaryData = open(responses.get(0).apdu()).parseTagLengthValuePayload(FCI_TEMPLATE)
				.parseTagLengthValuePayload(FCI_PROPRIETARY_TEMPLATE)
				.parseTagLengthValuePayload(FCI_ISSUER_DISCRETIONARY_DATA);

		List<String> entries = new ArrayList<>();
		while (discretionaryData.skipTo(DIRECTORY_ENTRY)) {
			Map<Integer, byte[]> entry = values(discretionaryData.parseTagLengthValuePayload());
			entries.add(hex(entry.get(APPLICATION_IDENTIFIER)) + ":" + hex(entry.get(APPLICATION_PRIORITY_INDICATOR)));
		}
		return entries;
	}

	/**
	 * @return the applications, in the order they were selected
	 */

	public List<Application> applications() {
		List<Application> applications = new ArrayList<>();
		Application application = null;
		for (Response response : responses.subList(1, responses.size())) {
			switch (response.ins()) {
			case SELECT -> {
				application = new Application(response.apdu());
				applications.add(application);
			}
			case GET_PROCESSING_OPTIONS -> application.getProcessingOptions = response.apdu();
			case READ_RECORD -> application.records.add(response.apdu());
			case GENERATE_AC -> application.generateAc = response.apdu();
			default -> throw new IllegalStateException("Unexpected INS " + response.ins());
			}
		}
		return applications;
	}

	public static class Application {

		private final byte[] select;
		private byte[] getProcessingOptions;
		private final List<byte[]> records = new ArrayList<>();
		private byte[] generateAc;

		public Application(byte[] select) {
			this.select = select;
		}

		public String aid() {
			return hex(values(open(select).parseTagLengthValuePayload(FCI_TEMPLATE)).get(DF_NAME));
		}

		/**
		 * @param tag tag in the FCI proprietary template (6F / A5)
		 * @return value, or null
		 */

		public byte[] fci(int tag) {
			return values(open(select).parseTagLengthValuePayload(FCI_TEMPLATE).parseTagLengthValuePayload(FCI_PROPRIETARY_TEMPLATE)).get(tag);
		}

		public String priority() {
			return hex(fci(APPLICATION_PRIORITY_INDICATOR));
		}

		public String language() {
			return new String(fci(LANGUAGE_PREFERENCE), StandardCharsets.US_ASCII);
		}

		/**
		 * @return the PDOL (6F / A5 / 9F38) as tag:length, empty if there is none
		 */

		public List<String> pdol() {
			LenientTlvPullParser proprietaryTemplate = open(select).parseTagLengthValuePayload(FCI_TEMPLATE).parseTagLengthValuePayload(FCI_PROPRIETARY_TEMPLATE);
			TagLengthPullParser pdol = proprietaryTemplate.parseTagLengthPayload(PDOL);
			return pdol == null ? List.of() : tagLengths(pdol);
		}

		/**
		 * @param tag tag in the GET PROCESSING OPTIONS response (77)
		 * @return value, or null
		 */

		public byte[] gpo(int tag) {
			return values(open(getProcessingOptions).parseTagLengthValuePayload(RESPONSE_MESSAGE_TEMPLATE_FORMAT_2)).get(tag);
		}

		public String aip() {
			return hex(gpo(APPLICATION_INTERCHANGE_PROFILE));
		}

		public String afl() {
			return hex(gpo(APPLICATION_FILE_LOCATOR));
		}

		/**
		 * @return the number of records listed in the AFL
		 */

		public int aflRecords() {
			byte[] afl = gpo(APPLICATION_FILE_LOCATOR);
			int count = 0;
			for (int i = 0; i < afl.length; i += 4) {
				count += (afl[i + 2] & 0xFF) - (afl[i + 1] & 0xFF) + 1;
			}
			return count;
		}

		public List<byte[]> records() {
			return records;
		}

		/**
		 * @param tag tag in a record template (70)
		 * @return value from the first record which has it, or null
		 */

		public byte[] record(int tag) {
			for (byte[] record : records) {
				LenientTlvPullParser template = open(record).parseTagLengthValuePayload(RECORD_TEMPLATE);
				if (template.skipTo(tag)) {
					return template.getPayload();
				}
			}
			return null;
		}

		/**
		 * @param tag tag of a data object list in a record template (70), like CDOL1 (8C)
		 * @return entries as tag:length
		 */

		public List<String> recordDol(int tag) {
			for (byte[] record : records) {
				TagLengthPullParser dol = open(record).parseTagLengthValuePayload(RECORD_TEMPLATE).parseTagLengthPayload(tag);
				if (dol != null) {
					return tagLengths(dol);
				}
			}
			return null;
		}

		/**
		 * @param tag tag in the GENERATE AC response (77)
		 * @return value, or null
		 */

		public byte[] generateAc(int tag) {
			return values(open(generateAc).parseTagLengthValuePayload(RESPONSE_MESSAGE_TEMPLATE_FORMAT_2)).get(tag);
		}

		public boolean hasGenerateAc() {
			return generateAc != null;
		}
	}

	private static LenientTlvPullParser open(byte[] responseApdu) {
		LenientTlvPullParser parser = new LenientTlvPullParser();
		parser.setResponseApduPayload(responseApdu);
		return parser;
	}

	// the values of the tags on one level
	private static Map<Integer, byte[]> values(LenientTlvPullParser parser) {
		Map<Integer, byte[]> values = new LinkedHashMap<>();
		int tag;
		while ((tag = parser.nextTag()) != -1) {
			values.putIfAbsent(tag, parser.getPayload());
		}
		return values;
	}

	private static List<String> tagLengths(TagLengthPullParser parser) {
		List<String> entries = new ArrayList<>();
		int tag;
		while ((tag = parser.nextTag()) != -1) {
			entries.add(Integer.toHexString(tag).toUpperCase() + ":" + parser.getLength());
		}
		return entries;
	}

	public static String hex(byte[] bytes) {
		return bytes == null ? null : HEX.formatHex(bytes);
	}

}
