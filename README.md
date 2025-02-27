# tlv-pull-parser
This project hosts a TLV parser and a few utilities for working with TLV content from smart cards. 

 * Efficient TLV pull-parser
   * reusable instances
   * minimal/zero object creation
   * simple drill-down support 
     * code structure mirrors TLV data structure
 * Customizable TLV pretty-printer
 
The project has zero dependencies.

# License
[European Union Public Licence v1.2](https://eupl.eu/).

# Usage
Get your ADPU response, then create a parser:

```java
byte[] responseAdpu = ...

LenientTlvPullParser pullParser = new LenientTlvPullParser(responseAdpu, 0, responseAdpu.length - 2);
```

and note that parser instances are reusable. 

Iterate over tags

```java
do {
  int tag = pullParser.nextTag();
  if(tag == -1) {
    break;
  }
  // your code here
} while(true);
```

The parser does not automatically go into child containers (i.e. like a JSON pull parser would), drill down must be done manually.  

For targeting a structure like 

```
6F 56 -- Template, File Control Parameters and File Management Data (FCI)
      84 07 -- Dedicated File (DF) Name
            A0 00 00 00 04 10 10
      A5 4B -- File Control Information (FCI) Proprietary Template
            50 10 -- Application Label
                  Debit Mastercard
```

drill down the built-in payload parser chaining:

```
LenientTlvPullParser fciTemplate = pullParser.parseTagLengthValuePayload(0x6F); // skip to tag + drill down
if(fciTemplate != null) {
  LenientTlvPullParser proprietaryTemplate = fciTemplate.parseTagLengthValuePayload(0xA5);
  if(proprietaryTemplate != null) {
     // process application labal and so on
  }
}
```

where each call to `parseTagLengthValuePayload` returns a child `LenientTlvPullParser` which works on the same buffer, 
but with different offsets. Consuming all child parser contents before accessing the parent parser is not necessary. 

