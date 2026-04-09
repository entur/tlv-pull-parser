package no.entur.tlv.utils;

import org.junit.jupiter.api.Test;

public class ToStringTagValuePrettyPrinterTest {

	@Test
	public void testFormat1() {
		String toString = "MastercardThirdPartyData [countryCode=578, deviceType=0x3231, uniqueIdentifier=0, SomeOtherSubclass [countryCode=578, deviceType=0x3231, uniqueIdentifier=0]]";

		StringBuilder builder = new StringBuilder();
		ToStringTagValuePrettyPrinter.format(toString, 0, builder);
		
		System.out.println(builder);
	}

	@Test
	public void testFormat2() {
		String toString = "MastercardIssuerApplicationData [keyDerivationIndex=1, cryptogramVersionNumber=17, mastercardCardVerificationResults=MastercardCardVerificationResultsChipAdvance [applicationCryptogramReturnedInSecondGenerateApplicationCryptogram=null, applicationCryptogramReturnedInFirstGenerateApplicationCryptogram=TransactionCertificate, offlinePinVerificationPerformed=false, offlineEncryptedPinVerificationPerformed=true, offlinePinVerificationSuccessful=false, ddaReturned=true, combinedDdaApplicationCryptogramGenerationReturnedInFirstGenerateApplicationCryptogram=false, combinedDdaApplicationCryptogramGenerationReturnedInSecondGenerateApplicationCryptogram=false, issuerAuthenticationPerformed=true, ciacDefaultSkippedOnCAT3=true, scriptCounter=2, pinTryCounter=7, unableToGoOnlineIndicated=false, offlinePinVerificationNotPerformed=false, offlinePinVerificationFailed=false, ptlExceeded=false, internationalTransaction=false, domesticTransaction=false, terminalErroneouslyConsidersOfflinePinOK=true, lowerConsecutiveOfflineLimitExceeded=true, upperConsecutiveOfflineLimitExceeded=false, lowerCumulativeOfflineLimitExceeded=false, upperCumulativeOfflineLimitExceeded=false, goOnlineOnNextTransactionWasSet=false, issuerAuthenticationFailed=false, scriptReceived=false, scriptFailed=false, matchFoundInAdditionalCheckTable=true, noMatchFoundInAdditionalCheckTable=true, dateCheckFailed=true, OfflineChangePinResult=true, issuerDiscretionaryMChipAdvance=true, issuerDiscretionaryContactlessInterface=true, lastOnlineTransactionNotCompleted=false, lowerConsecutiveCounter2LimitExceeded=true, upperConsecutiveCounter2LimitExceeded=false, lowerCumulativeAccumulator2LimitExceeded=false, upperCumulativeAccumulator2LimitExceeded=false, mtaLimitExceeded=false, numberOfDaysOfflineLimitExceeded=false]]";
		
		StringBuilder builder = new StringBuilder();
		ToStringTagValuePrettyPrinter.format(toString, 0, builder);
		
		System.out.println(builder);
	}

}
