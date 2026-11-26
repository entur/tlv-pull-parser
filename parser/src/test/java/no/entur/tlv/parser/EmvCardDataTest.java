package no.entur.tlv.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Test;

import no.entur.tlv.parser.EmvCard.Application;

/**
 * Read the data of each recorded EMV card (see {@link EmvCardResponsesTest}) the way a kernel would, and check the values: the PPSE
 * directory, and for each application its FCI and PDOL, the AIP and AFL, the records (data object lists, CVM list, codes, and the length of
 * masked values like the PAN and the certificates), and the GENERATE AC response.
 */

public class EmvCardDataTest {

	@Test
	public void testBankaxept1() throws IOException {
		EmvCard card = EmvCard.load("bankaxept-1.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("A0000000031010:02", "D5780000021010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(1, applications.size());

		// Bankaxept
		Application bankaxept = applications.get(0);
		assertEquals("D5780000021010", bankaxept.aid());
		assertEquals("01", bankaxept.priority());
		assertEquals("noen", bankaxept.language());
		assertEquals(List.of("9F59:3", "9F5A:1", "9F02:6", "9F1A:2", "5F2A:2", "9F37:4", "9F58:1"), bankaxept.pdol());

		assertEquals("1000", bankaxept.aip());
		assertEquals("08010300", bankaxept.afl());
		assertEquals(3, bankaxept.aflRecords());
		assertEquals(3, bankaxept.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F45:2", "9F4C:8", "9F34:3"), bankaxept.recordDol(0x8C));
		assertEquals(List.of("91:10", "8A:2", "95:5", "9F37:4", "9F4C:8"), bankaxept.recordDol(0x8D));
		assertEquals("00000000000000000203", EmvCard.hex(bankaxept.record(0x8E)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x9F42)));
		assertEquals("AB80", EmvCard.hex(bankaxept.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(bankaxept.record(0x9F08)));
		assertMasked(bankaxept.record(0x5A), 9);
		assertMasked(bankaxept.record(0x5F24), 3);
		assertMasked(bankaxept.record(0x57), 19);
		assertFalse(bankaxept.hasGenerateAc());
	}

	@Test
	public void testBankaxept2() throws IOException {
		EmvCard card = EmvCard.load("bankaxept-2.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("D5780000021010:01", "A0000000041010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(1, applications.size());

		// Bankaxept
		Application bankaxept = applications.get(0);
		assertEquals("D5780000021010", bankaxept.aid());
		assertEquals("01", bankaxept.priority());
		assertEquals("noendasv", bankaxept.language());
		assertEquals(List.of("9F59:3", "9F5A:1", "9F02:6", "9F1A:2", "5F2A:2", "9F37:4", "9F58:1"), bankaxept.pdol());

		assertEquals("1000", bankaxept.aip());
		assertEquals("08010200", bankaxept.afl());
		assertEquals(2, bankaxept.aflRecords());
		assertEquals(2, bankaxept.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F45:2", "9F4C:8", "9F34:3"), bankaxept.recordDol(0x8C));
		assertEquals("00000000000000000203", EmvCard.hex(bankaxept.record(0x8E)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x9F42)));
		assertEquals("AB80", EmvCard.hex(bankaxept.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(bankaxept.record(0x9F08)));
		assertMasked(bankaxept.record(0x5A), 9);
		assertMasked(bankaxept.record(0x5F24), 3);
		assertMasked(bankaxept.record(0x57), 18);
		assertFalse(bankaxept.hasGenerateAc());
	}

	@Test
	public void testBankaxeptMastercardPlastic1() throws IOException {
		EmvCard card = EmvCard.load("bankaxept-mastercard-plastic-1.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("D5780000021010:01", "A0000000041010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(2, applications.size());

		// Mastercard
		Application mastercard = applications.get(0);
		assertEquals("A0000000041010", mastercard.aid());
		assertEquals("01", mastercard.priority());
		assertEquals("noendasv", mastercard.language());
		assertEquals(List.of(), mastercard.pdol());

		assertEquals("1980", mastercard.aip());
		assertEquals("080101001001010120010200", mastercard.afl());
		assertEquals(4, mastercard.aflRecords());
		assertEquals(4, mastercard.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F35:1", "9F45:2", "9F4C:8", "9F34:3", "9F21:3", "9F7C:20"), mastercard.recordDol(0x8C));
		assertEquals(List.of("91:10", "8A:2", "95:5", "9F37:4", "9F4C:8"), mastercard.recordDol(0x8D));
		assertEquals("000000000000000042031F03", EmvCard.hex(mastercard.record(0x8E)));
		assertEquals("0578", EmvCard.hex(mastercard.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(mastercard.record(0x9F42)));
		assertEquals("FFC0", EmvCard.hex(mastercard.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(mastercard.record(0x9F08)));
		assertEquals("05", EmvCard.hex(mastercard.record(0x8F)));
		assertMasked(mastercard.record(0x5A), 8);
		assertMasked(mastercard.record(0x5F24), 3);
		assertMasked(mastercard.record(0x57), 19);
		assertMasked(mastercard.record(0x90), 176);
		assertMasked(mastercard.record(0x9F46), 176);

		assertEquals("40", EmvCard.hex(mastercard.generateAc(0x9F27)));
		assertMasked(mastercard.generateAc(0x9F36), 2);
		assertMasked(mastercard.generateAc(0x9F4B), 128);

		// Bankaxept
		Application bankaxept = applications.get(1);
		assertEquals("D5780000021010", bankaxept.aid());
		assertEquals("01", bankaxept.priority());
		assertEquals("noendasv", bankaxept.language());
		assertEquals(List.of("9F59:3", "9F5A:1", "9F02:6", "9F1A:2", "5F2A:2", "9F37:4", "9F58:1"), bankaxept.pdol());

		assertEquals("1000", bankaxept.aip());
		assertEquals("08010200", bankaxept.afl());
		assertEquals(2, bankaxept.aflRecords());
		assertEquals(2, bankaxept.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F45:2", "9F4C:8", "9F34:3"), bankaxept.recordDol(0x8C));
		assertEquals("00000000000000000203", EmvCard.hex(bankaxept.record(0x8E)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x9F42)));
		assertEquals("AB80", EmvCard.hex(bankaxept.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(bankaxept.record(0x9F08)));
		assertMasked(bankaxept.record(0x5A), 9);
		assertMasked(bankaxept.record(0x5F24), 3);
		assertMasked(bankaxept.record(0x57), 18);
		assertFalse(bankaxept.hasGenerateAc());
	}

	@Test
	public void testBankaxeptMastercardPlastic2() throws IOException {
		EmvCard card = EmvCard.load("bankaxept-mastercard-plastic-2.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("D5780000021010:01", "A0000000041010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(2, applications.size());

		// Mastercard
		Application mastercard = applications.get(0);
		assertEquals("A0000000041010", mastercard.aid());
		assertEquals("01", mastercard.priority());
		assertEquals("noendasv", mastercard.language());
		assertEquals(List.of(), mastercard.pdol());

		assertEquals("1980", mastercard.aip());
		assertEquals("080101001001010120010200", mastercard.afl());
		assertEquals(4, mastercard.aflRecords());
		assertEquals(4, mastercard.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F35:1", "9F45:2", "9F4C:8", "9F34:3", "9F21:3", "9F7C:20"), mastercard.recordDol(0x8C));
		assertEquals(List.of("91:10", "8A:2", "95:5", "9F37:4", "9F4C:8"), mastercard.recordDol(0x8D));
		assertEquals("000000000000000042031F03", EmvCard.hex(mastercard.record(0x8E)));
		assertEquals("0578", EmvCard.hex(mastercard.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(mastercard.record(0x9F42)));
		assertEquals("FFC0", EmvCard.hex(mastercard.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(mastercard.record(0x9F08)));
		assertEquals("05", EmvCard.hex(mastercard.record(0x8F)));
		assertMasked(mastercard.record(0x5A), 8);
		assertMasked(mastercard.record(0x5F24), 3);
		assertMasked(mastercard.record(0x57), 19);
		assertMasked(mastercard.record(0x90), 176);
		assertMasked(mastercard.record(0x9F46), 176);

		assertEquals("80", EmvCard.hex(mastercard.generateAc(0x9F27)));
		assertMasked(mastercard.generateAc(0x9F36), 2);
		assertMasked(mastercard.generateAc(0x9F4B), 128);

		// Bankaxept
		Application bankaxept = applications.get(1);
		assertEquals("D5780000021010", bankaxept.aid());
		assertEquals("01", bankaxept.priority());
		assertEquals("noendasv", bankaxept.language());
		assertEquals(List.of("9F59:3", "9F5A:1", "9F02:6", "9F1A:2", "5F2A:2", "9F37:4", "9F58:1"), bankaxept.pdol());

		assertEquals("1000", bankaxept.aip());
		assertEquals("08010200", bankaxept.afl());
		assertEquals(2, bankaxept.aflRecords());
		assertEquals(2, bankaxept.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F45:2", "9F4C:8", "9F34:3"), bankaxept.recordDol(0x8C));
		assertEquals("00000000000000000203", EmvCard.hex(bankaxept.record(0x8E)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x9F42)));
		assertEquals("AB80", EmvCard.hex(bankaxept.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(bankaxept.record(0x9F08)));
		assertMasked(bankaxept.record(0x5A), 9);
		assertMasked(bankaxept.record(0x5F24), 3);
		assertMasked(bankaxept.record(0x57), 18);
		assertFalse(bankaxept.hasGenerateAc());
	}

	@Test
	public void testBankaxeptMastercardPlastic3() throws IOException {
		EmvCard card = EmvCard.load("bankaxept-mastercard-plastic-3.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("D5780000021010:01", "A0000000041010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(2, applications.size());

		// Mastercard
		Application mastercard = applications.get(0);
		assertEquals("A0000000041010", mastercard.aid());
		assertEquals("01", mastercard.priority());
		assertEquals("nosvdaen", mastercard.language());
		assertEquals(List.of(), mastercard.pdol());

		assertEquals("1980", mastercard.aip());
		assertEquals("100102001007080018010101", mastercard.afl());
		assertEquals(5, mastercard.aflRecords());
		assertEquals(5, mastercard.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F35:1", "9F45:2", "9F4C:8", "9F34:3", "9F21:3", "9F7C:20", "9F1D:8"), mastercard.recordDol(0x8C));
		assertEquals(List.of("91:10", "8A:2", "95:5", "9F37:4", "9F4C:8", "9F02:6", "9F03:6"), mastercard.recordDol(0x8D));
		assertEquals("000000000000000042031F03", EmvCard.hex(mastercard.record(0x8E)));
		assertEquals("0578", EmvCard.hex(mastercard.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(mastercard.record(0x9F42)));
		assertEquals("FFC0", EmvCard.hex(mastercard.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(mastercard.record(0x9F08)));
		assertEquals("EF", EmvCard.hex(mastercard.record(0x8F)));
		assertMasked(mastercard.record(0x5A), 8);
		assertMasked(mastercard.record(0x5F24), 3);
		assertMasked(mastercard.record(0x57), 19);
		assertMasked(mastercard.record(0x90), 248);
		assertMasked(mastercard.record(0x9F46), 247);

		assertEquals("80", EmvCard.hex(mastercard.generateAc(0x9F27)));
		assertMasked(mastercard.generateAc(0x9F36), 2);
		assertMasked(mastercard.generateAc(0x9F4B), 176);

		// Bankaxept
		Application bankaxept = applications.get(1);
		assertEquals("D5780000021010", bankaxept.aid());
		assertEquals("01", bankaxept.priority());
		assertEquals("noen", bankaxept.language());
		assertEquals(List.of("9F59:3", "9F5A:1", "9F02:6", "9F1A:2", "5F2A:2", "9F37:4", "9F58:1"), bankaxept.pdol());

		assertEquals("1100", bankaxept.aip());
		assertEquals("080101010802050018010200", bankaxept.afl());
		assertEquals(7, bankaxept.aflRecords());
		assertEquals(7, bankaxept.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F45:2", "9F4C:8", "9F34:3"), bankaxept.recordDol(0x8C));
		assertEquals(List.of("91:10", "8A:2", "95:5", "9F37:4", "9F4C:8"), bankaxept.recordDol(0x8D));
		assertEquals("00000000000000000203", EmvCard.hex(bankaxept.record(0x8E)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x9F42)));
		assertEquals("AB80", EmvCard.hex(bankaxept.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(bankaxept.record(0x9F08)));
		assertEquals("82", EmvCard.hex(bankaxept.record(0x8F)));
		assertMasked(bankaxept.record(0x5A), 9);
		assertMasked(bankaxept.record(0x5F24), 3);
		assertMasked(bankaxept.record(0x57), 19);
		assertMasked(bankaxept.record(0x90), 240);
		assertMasked(bankaxept.record(0x9F46), 240);
		assertFalse(bankaxept.hasGenerateAc());
	}

	@Test
	public void testBankaxeptVisaPlastic1() throws IOException {
		EmvCard card = EmvCard.load("bankaxept-visa-plastic-1.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("D5780000021010:01", "A0000000031010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(1, applications.size());

		// Bankaxept
		Application bankaxept = applications.get(0);
		assertEquals("D5780000021010", bankaxept.aid());
		assertEquals("01", bankaxept.priority());
		assertEquals("noen", bankaxept.language());
		assertEquals(List.of("9F59:3", "9F5A:1", "9F02:6", "9F1A:2", "5F2A:2", "9F37:4", "9F58:1"), bankaxept.pdol());

		assertEquals("1000", bankaxept.aip());
		assertEquals("08010200", bankaxept.afl());
		assertEquals(2, bankaxept.aflRecords());
		assertEquals(2, bankaxept.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F45:2", "9F4C:8", "9F34:3"), bankaxept.recordDol(0x8C));
		assertEquals("00000000000000000203", EmvCard.hex(bankaxept.record(0x8E)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x9F42)));
		assertEquals("AB80", EmvCard.hex(bankaxept.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(bankaxept.record(0x9F08)));
		assertMasked(bankaxept.record(0x5A), 9);
		assertMasked(bankaxept.record(0x5F24), 3);
		assertMasked(bankaxept.record(0x57), 19);
		assertFalse(bankaxept.hasGenerateAc());
	}

	@Test
	public void testBankaxeptVisaPlastic2() throws IOException {
		EmvCard card = EmvCard.load("bankaxept-visa-plastic-2.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("A0000000031010:02", "D5780000021010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(2, applications.size());

		// Visa
		Application visa = applications.get(0);
		assertEquals("A0000000031010", visa.aid());
		assertEquals("02", visa.priority());
		assertEquals("nosvdaen", visa.language());
		assertEquals(List.of("9F66:4", "9F02:6", "9F03:6", "9F1A:2", "95:5", "9A:3", "5F2A:2", "9C:1", "9F37:4"), visa.pdol());

		assertEquals("2020", visa.aip());
		assertEquals("10020300", visa.afl());
		assertEquals(2, visa.aflRecords());
		assertEquals(2, visa.records().size());
		assertEquals("40", EmvCard.hex(visa.gpo(0x9F27)));
		assertMasked(visa.gpo(0x57), 19);
		assertMasked(visa.gpo(0x9F26), 8);

		assertEquals("0578", EmvCard.hex(visa.record(0x5F28)));
		assertEquals("C200", EmvCard.hex(visa.record(0x9F07)));
		assertEquals("08", EmvCard.hex(visa.record(0x8F)));
		assertMasked(visa.record(0x5A), 8);
		assertMasked(visa.record(0x5F24), 3);
		assertMasked(visa.record(0x90), 176);
		assertMasked(visa.record(0x9F46), 176);
		assertFalse(visa.hasGenerateAc());

		// Bankaxept
		Application bankaxept = applications.get(1);
		assertEquals("D5780000021010", bankaxept.aid());
		assertEquals("01", bankaxept.priority());
		assertEquals("noen", bankaxept.language());
		assertEquals(List.of("9F59:3", "9F5A:1", "9F02:6", "9F1A:2", "5F2A:2", "9F37:4", "9F58:1"), bankaxept.pdol());

		assertEquals("1000", bankaxept.aip());
		assertEquals("08010300", bankaxept.afl());
		assertEquals(3, bankaxept.aflRecords());
		assertEquals(3, bankaxept.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F45:2", "9F4C:8", "9F34:3"), bankaxept.recordDol(0x8C));
		assertEquals(List.of("91:10", "8A:2", "95:5", "9F37:4", "9F4C:8"), bankaxept.recordDol(0x8D));
		assertEquals("00000000000000000203", EmvCard.hex(bankaxept.record(0x8E)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(bankaxept.record(0x9F42)));
		assertEquals("AB80", EmvCard.hex(bankaxept.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(bankaxept.record(0x9F08)));
		assertMasked(bankaxept.record(0x5A), 9);
		assertMasked(bankaxept.record(0x5F24), 3);
		assertMasked(bankaxept.record(0x57), 19);
		assertFalse(bankaxept.hasGenerateAc());
	}

	@Test
	public void testJcb1() throws IOException {
		EmvCard card = EmvCard.load("jcb-1.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("A0000008381010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(1, applications.size());

		// Application A0000008381010
		Application application = applications.get(0);
		assertEquals("A0000008381010", application.aid());
		assertEquals("01", application.priority());
		assertEquals("sven", application.language());
		assertEquals(List.of("9F52:1"), application.pdol());

		assertEquals("0980", application.aip());
		assertEquals("080101001001020118010300", application.afl());
		assertEquals(6, application.aflRecords());
		assertEquals(6, application.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F35:1", "9F34:3", "9F21:3", "9F16:15", "9F1C:8", "9F7C:20"), application.recordDol(0x8C));
		assertEquals(List.of("8A:2", "91:10", "95:5"), application.recordDol(0x8D));
		assertEquals("00000000000000001F00", EmvCard.hex(application.record(0x8E)));
		assertEquals("0752", EmvCard.hex(application.record(0x5F28)));
		assertEquals("FFC0", EmvCard.hex(application.record(0x9F07)));
		assertEquals("0000", EmvCard.hex(application.record(0x9F08)));
		assertEquals("03", EmvCard.hex(application.record(0x8F)));
		assertMasked(application.record(0x5A), 10);
		assertMasked(application.record(0x5F24), 3);
		assertMasked(application.record(0x57), 19);
		assertMasked(application.record(0x90), 176);
		assertMasked(application.record(0x9F46), 144);

		assertEquals("40", EmvCard.hex(application.generateAc(0x9F27)));
		assertMasked(application.generateAc(0x9F36), 2);
		assertMasked(application.generateAc(0x9F4B), 128);
	}

	@Test
	public void testMastercardMobile1() throws IOException {
		EmvCard card = EmvCard.load("mastercard-mobile-1.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("A0000000041010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(1, applications.size());

		// Mastercard
		Application mastercard = applications.get(0);
		assertEquals("A0000000041010", mastercard.aid());
		assertEquals("01", mastercard.priority());
		assertEquals("noen", mastercard.language());
		assertEquals(List.of("9F1D:8", "9F1A:2", "9F35:1", "9F7E:1", "9F4E:32"), mastercard.pdol());

		assertEquals("1B80", mastercard.aip());
		assertEquals("0801010010010501", mastercard.afl());
		assertEquals(6, mastercard.aflRecords());
		assertEquals(6, mastercard.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F35:1", "9F45:2", "9F4C:8", "9F34:3", "9F15:2"), mastercard.recordDol(0x8C));
		assertEquals(List.of("91:10", "8A:2", "95:5", "9F37:4", "9F4C:8"), mastercard.recordDol(0x8D));
		assertEquals("0000000000000000420142031F03", EmvCard.hex(mastercard.record(0x8E)));
		assertEquals("0578", EmvCard.hex(mastercard.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(mastercard.record(0x9F42)));
		assertEquals("FFC0", EmvCard.hex(mastercard.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(mastercard.record(0x9F08)));
		assertEquals("06", EmvCard.hex(mastercard.record(0x8F)));
		assertMasked(mastercard.record(0x5A), 8);
		assertMasked(mastercard.record(0x5F24), 3);
		assertMasked(mastercard.record(0x57), 19);
		assertMasked(mastercard.record(0x90), 248);
		assertMasked(mastercard.record(0x9F46), 176);

		assertEquals("00", EmvCard.hex(mastercard.generateAc(0x9F27)));
		assertMasked(mastercard.generateAc(0x9F36), 2);
		assertMasked(mastercard.generateAc(0x9F4B), 128);
	}

	@Test
	public void testMastercardPlastic1() throws IOException {
		EmvCard card = EmvCard.load("mastercard-plastic-1.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("A0000000041010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(1, applications.size());

		// Mastercard
		Application mastercard = applications.get(0);
		assertEquals("A0000000041010", mastercard.aid());
		assertEquals("01", mastercard.priority());
		assertEquals("nosvdaen", mastercard.language());
		assertEquals(List.of(), mastercard.pdol());

		assertEquals("1980", mastercard.aip());
		assertEquals("08020200180101001802020120010100", mastercard.afl());
		assertEquals(4, mastercard.aflRecords());
		assertEquals(4, mastercard.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F35:1", "9F45:2", "9F4C:8", "9F34:3", "9F21:3", "9F7C:20"), mastercard.recordDol(0x8C));
		assertEquals(List.of("91:10", "8A:2", "95:5", "9F37:4", "9F4C:8"), mastercard.recordDol(0x8D));
		assertEquals("000000000000000042031F03", EmvCard.hex(mastercard.record(0x8E)));
		assertEquals("0578", EmvCard.hex(mastercard.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(mastercard.record(0x9F42)));
		assertEquals("FF00", EmvCard.hex(mastercard.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(mastercard.record(0x9F08)));
		assertEquals("05", EmvCard.hex(mastercard.record(0x8F)));
		assertMasked(mastercard.record(0x5A), 8);
		assertMasked(mastercard.record(0x5F24), 3);
		assertMasked(mastercard.record(0x57), 19);
		assertMasked(mastercard.record(0x90), 176);
		assertMasked(mastercard.record(0x9F46), 176);

		assertEquals("40", EmvCard.hex(mastercard.generateAc(0x9F27)));
		assertMasked(mastercard.generateAc(0x9F36), 2);
		assertMasked(mastercard.generateAc(0x9F4B), 128);
	}

	@Test
	public void testMastercardPlastic2() throws IOException {
		EmvCard card = EmvCard.load("mastercard-plastic-2.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("D5780000021010:01", "A0000000041010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(1, applications.size());

		// Mastercard
		Application mastercard = applications.get(0);
		assertEquals("A0000000041010", mastercard.aid());
		assertEquals("01", mastercard.priority());
		assertEquals("noendasv", mastercard.language());
		assertEquals(List.of(), mastercard.pdol());

		assertEquals("1980", mastercard.aip());
		assertEquals("080101001001010120010200", mastercard.afl());
		assertEquals(4, mastercard.aflRecords());
		assertEquals(4, mastercard.records().size());

		assertEquals(List.of("9F02:6", "9F03:6", "9F1A:2", "95:5", "5F2A:2", "9A:3", "9C:1", "9F37:4", "9F35:1", "9F45:2", "9F4C:8", "9F34:3", "9F21:3", "9F7C:20"), mastercard.recordDol(0x8C));
		assertEquals(List.of("91:10", "8A:2", "95:5", "9F37:4", "9F4C:8"), mastercard.recordDol(0x8D));
		assertEquals("000000000000000042031F03", EmvCard.hex(mastercard.record(0x8E)));
		assertEquals("0578", EmvCard.hex(mastercard.record(0x5F28)));
		assertEquals("0578", EmvCard.hex(mastercard.record(0x9F42)));
		assertEquals("FFC0", EmvCard.hex(mastercard.record(0x9F07)));
		assertEquals("0002", EmvCard.hex(mastercard.record(0x9F08)));
		assertEquals("05", EmvCard.hex(mastercard.record(0x8F)));
		assertMasked(mastercard.record(0x5A), 8);
		assertMasked(mastercard.record(0x5F24), 3);
		assertMasked(mastercard.record(0x57), 19);
		assertMasked(mastercard.record(0x90), 176);
		assertMasked(mastercard.record(0x9F46), 176);

		assertEquals("80", EmvCard.hex(mastercard.generateAc(0x9F27)));
		assertMasked(mastercard.generateAc(0x9F36), 2);
		assertMasked(mastercard.generateAc(0x9F4B), 128);
	}

	@Test
	public void testVisaPlastic1() throws IOException {
		EmvCard card = EmvCard.load("visa-plastic-1.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("A0000000031010:01", "A0000008351010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(1, applications.size());

		// Visa
		Application visa = applications.get(0);
		assertEquals("A0000000031010", visa.aid());
		assertEquals("01", visa.priority());
		assertEquals("nosvdaen", visa.language());
		assertEquals(List.of("9F66:4", "9F02:6", "9F03:6", "9F1A:2", "95:5", "9A:3", "5F2A:2", "9C:1", "9F37:4"), visa.pdol());

		assertEquals("2020", visa.aip());
		assertEquals("10010200100708001003030018090900", visa.afl());
		assertEquals(6, visa.aflRecords());
		assertEquals(6, visa.records().size());
		assertEquals("40", EmvCard.hex(visa.gpo(0x9F27)));
		assertMasked(visa.gpo(0x57), 19);
		assertMasked(visa.gpo(0x9F26), 8);

		assertEquals("0578", EmvCard.hex(visa.record(0x5F28)));
		assertEquals("09", EmvCard.hex(visa.record(0x8F)));
		assertMasked(visa.record(0x5A), 8);
		assertMasked(visa.record(0x5F24), 3);
		assertMasked(visa.record(0x90), 248);
		assertMasked(visa.record(0x9F46), 247);
		assertFalse(visa.hasGenerateAc());
	}

	@Test
	public void testVisaPlastic2() throws IOException {
		EmvCard card = EmvCard.load("visa-plastic-2.txt");

		assertEquals("2PAY.SYS.DDF01", card.directoryName());
		assertEquals(List.of("A0000000031010:02", "D5780000021010:01"), card.directory());

		List<Application> applications = card.applications();
		assertEquals(1, applications.size());

		// Visa
		Application visa = applications.get(0);
		assertEquals("A0000000031010", visa.aid());
		assertEquals("02", visa.priority());
		assertEquals("nosvdaen", visa.language());
		assertEquals(List.of("9F66:4", "9F02:6", "9F03:6", "9F1A:2", "95:5", "9A:3", "5F2A:2", "9C:1", "9F37:4"), visa.pdol());

		assertEquals("2020", visa.aip());
		assertEquals("10020300", visa.afl());
		assertEquals(2, visa.aflRecords());
		assertEquals(2, visa.records().size());
		assertEquals("40", EmvCard.hex(visa.gpo(0x9F27)));
		assertMasked(visa.gpo(0x57), 19);
		assertMasked(visa.gpo(0x9F26), 8);

		assertEquals("0578", EmvCard.hex(visa.record(0x5F28)));
		assertEquals("C200", EmvCard.hex(visa.record(0x9F07)));
		assertEquals("08", EmvCard.hex(visa.record(0x8F)));
		assertMasked(visa.record(0x5A), 8);
		assertMasked(visa.record(0x5F24), 3);
		assertMasked(visa.record(0x90), 176);
		assertMasked(visa.record(0x9F46), 176);
		assertFalse(visa.hasGenerateAc());
	}

	private static void assertMasked(byte[] value, int length) {
		assertNotNull(value);
		assertEquals(length, value.length);
		// filler, see the file header
		for (byte b : value) {
			assertTrue(b == 0x00 || b == 'X');
		}
	}

}
