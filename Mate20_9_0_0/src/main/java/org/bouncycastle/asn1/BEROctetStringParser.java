package org.bouncycastle.asn1;

import java.io.IOException;
import java.io.InputStream;
import org.bouncycastle.util.io.Streams;

public class BEROctetStringParser implements ASN1OctetStringParser {
    private ASN1StreamParser _parser;

    BEROctetStringParser(ASN1StreamParser aSN1StreamParser) {
        this._parser = aSN1StreamParser;
    }

    public ASN1Primitive getLoadedObject() throws IOException {
        return new BEROctetString(Streams.readAll(getOctetStream()));
    }

    public InputStream getOctetStream() {
        return new ConstructedOctetStream(this._parser);
    }

    public ASN1Primitive toASN1Primitive() {
        try {
            return getLoadedObject();
        } catch (Throwable e) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("IOException converting stream to byte array: ");
            stringBuilder.append(e.getMessage());
            throw new ASN1ParsingException(stringBuilder.toString(), e);
        }
    }
}
