package com.android.org.bouncycastle.asn1;

import com.android.org.bouncycastle.util.io.Streams;
import java.io.IOException;
import java.io.InputStream;

public class BEROctetStringParser implements ASN1OctetStringParser {
    private ASN1StreamParser _parser;

    BEROctetStringParser(ASN1StreamParser parser) {
        this._parser = parser;
    }

    public InputStream getOctetStream() {
        return new ConstructedOctetStream(this._parser);
    }

    public ASN1Primitive getLoadedObject() throws IOException {
        return new BEROctetString(Streams.readAll(getOctetStream()));
    }

    public ASN1Primitive toASN1Primitive() {
        try {
            return getLoadedObject();
        } catch (IOException e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IOException converting stream to byte array: ");
            stringBuilder.append(e.getMessage());
            throw new ASN1ParsingException(stringBuilder.toString(), e);
        }
    }
}
