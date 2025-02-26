package no.entur.tlv.parser;

import org.junit.jupiter.api.Test;

import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    private static final byte[] PPSE_RESPONSE_ADPU = HexFormat.of().parseHex("6F568407A0000000041010A54B50104465626974204D6173746572636172648701015F2D086E6F656E646173769F1101019F12104D617374657263617264204465626574BF0C119F0A04000101019F6E07057800003030009000");

    @Test
    public void testParseSelectEmvApplicationResponse() {
        LenientTlvPullParser pullParser = new LenientTlvPullParser();

        pullParser.setBuffer(PPSE_RESPONSE_ADPU, 0, PPSE_RESPONSE_ADPU.length - 2);

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

}
