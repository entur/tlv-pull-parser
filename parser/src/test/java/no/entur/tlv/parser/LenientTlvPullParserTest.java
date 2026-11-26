package no.entur.tlv.parser;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LenientTlvPullParserTest {

    // NOTE: TAG int value is not same as wire format. Leftmost bit (0x80) indicates more bytes in byte index 1 and on.
    public static final int FCI_TEMPLATE = 0x6F;
    public static final int FCI_PROPRIETARY_TEMPLATE = 0xA5;
    public static final int FCI_ISSUER_DISCRETIONARY_DATA = 0xBF0C;


    // 6F 56 -- Template, File Control Parameters and File Management Data (FCI)
    //       84 07 -- Dedicated File (DF) Name
    //             A0 00 00 00 04 10 10
    //       A5 4B -- File Control Information (FCI) Proprietary Template
    //             50 10 -- Application Label
    //                   Debit Mastercard
    //             87 01 -- Application Priority Indicator
    //                   01
    //             5F 2D 08 -- Language Preference
    //                      noendasv
    //             9F 11 01 -- Issuer Code Table Index
    //                      01
    //             9F 12 10 -- Application Preferred Name
    //                      Mastercard Debet
    //             BF 0C 11 -- File Control Information (FCI) Issuer Discretionary Data
    //                      9F 0A 04 -- Application Selection Registered Proprietary Data list
    //                               00 01 01 01
    //                      9F 6E 07 -- Form Factor Indicator (FFI)
    //                               05 78 00 00 30 30 00

    private static final byte[] PPSE_RESPONSE_APDU = HexFormat.of().parseHex("6F568407A0000000041010A54B50104465626974204D6173746572636172648701015F2D086E6F656E646173769F1101019F12104D617374657263617264204465626574BF0C119F0A04000101019F6E07057800003030009000");

    @Test
    public void testParseSelectEmvApplicationResponse() {
        LenientTlvPullParser pullParser = new LenientTlvPullParser();

        pullParser.setBuffer(PPSE_RESPONSE_APDU, 0, PPSE_RESPONSE_APDU.length - 2);

        int count = -1;

        LenientTlvPullParser fciTemplate = pullParser.parseTagLengthValuePayload(FCI_TEMPLATE);
        if(fciTemplate != null) {
            LenientTlvPullParser proprietaryTemplate = fciTemplate.parseTagLengthValuePayload(FCI_PROPRIETARY_TEMPLATE);
            if(proprietaryTemplate != null) {
                LenientTlvPullParser issuerDiscretionaryData = proprietaryTemplate.parseTagLengthValuePayload(FCI_ISSUER_DISCRETIONARY_DATA);
                if(issuerDiscretionaryData != null) {
                    count = parseFileControlInformation(issuerDiscretionaryData);
                }
            }
        }
        assertEquals(2, count);
    }

    protected int parseFileControlInformation(LenientTlvPullParser pullParser) {
        int count = 0;
        do {
            int tag = pullParser.nextTag();
            if(tag == -1) {
                break;
            }
            count++;
        } while(true);

        return count;
    }

    @Test
    public void testPrepare() {
        LenientTlvPullParser lenientTlvPullParser = LenientTlvPullParser.newInstance(4);
        assertNotNull(lenientTlvPullParser);
    }

    @Test
    public void testLongFormLengthOverflowingToNegativeIsMalformed() {
        // 5A 02 12 34, then 64 with length 84 80 00 00 00 = 0x80000000, which is negative as an int
        byte[] bytes = HexFormat.of().parseHex("5A021234" + "6484800000004142");

        LenientTlvPullParser parser = new LenientTlvPullParser(bytes);
        assertEquals(0x5A, parser.nextTag());
        assertArrayEquals(new byte[]{0x12, 0x34}, parser.getPayload());

        assertEquals(-1, parser.nextTag());
        assertEquals(-1, parser.nextTag());

        assertNull(new LenientTlvPullParser(bytes).parseTagLengthValuePayload(0x64));
        assertNull(new LenientTlvPullParser(bytes).parseTagLengthPayload(0x64));
    }

    @Test
    public void testLongFormLengthOverflowingOffsetIsMalformed() {
        // 64 with length 84 7F FF FF FF = Integer.MAX_VALUE, so offset + length overflows
        byte[] bytes = HexFormat.of().parseHex("64847FFFFFFF4142");

        LenientTlvPullParser parser = new LenientTlvPullParser(bytes);
        assertEquals(-1, parser.nextTag());
        assertEquals(-1, parser.nextTag());

        assertNull(new LenientTlvPullParser(bytes).parseTagLengthValuePayload(0x64));
    }

    @Test
    public void testLongFormLengthWithMoreBytesThanFitInAnIntIsMalformed() {
        // 64 with length 85 01 00 00 00 00 = 2^32, which truncates to 0 as an int
        LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("648501000000004142"));
        assertEquals(-1, parser.nextTag());
        assertEquals(-1, parser.nextTag());
    }

    @Test
    public void testLongFormLengthPastLimitIsMalformed() {
        // 64 with length 82 01 00 = 256, but only two bytes of payload
        LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("648201004142"));
        assertEquals(-1, parser.nextTag());
        assertEquals(-1, parser.nextTag());
    }

    @Test
    public void testLongFormLengthWithFourBytes() {
        // 64 with length 84 00 00 00 02, then 5A 01 FF
        LenientTlvPullParser parser = new LenientTlvPullParser(HexFormat.of().parseHex("6484000000024142" + "5A01FF"));
        assertEquals(0x64, parser.nextTag());
        assertArrayEquals(new byte[]{0x41, 0x42}, parser.getPayload());
        assertEquals(0x5A, parser.nextTag());
        assertArrayEquals(new byte[]{(byte) 0xFF}, parser.getPayload());
        assertEquals(-1, parser.nextTag());
    }

}
