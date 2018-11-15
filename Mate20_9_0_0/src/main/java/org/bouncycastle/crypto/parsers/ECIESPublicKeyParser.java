package org.bouncycastle.crypto.parsers;

import java.io.IOException;
import java.io.InputStream;
import org.bouncycastle.crypto.KeyParser;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.util.io.Streams;

public class ECIESPublicKeyParser implements KeyParser {
    private ECDomainParameters ecParams;

    public ECIESPublicKeyParser(ECDomainParameters eCDomainParameters) {
        this.ecParams = eCDomainParameters;
    }

    public AsymmetricKeyParameter readKey(InputStream inputStream) throws IOException {
        int fieldSize;
        int read = inputStream.read();
        switch (read) {
            case 0:
                throw new IOException("Sender's public key invalid.");
            case 2:
            case 3:
                fieldSize = (this.ecParams.getCurve().getFieldSize() + 7) / 8;
                break;
            case 4:
            case 6:
            case 7:
                fieldSize = 2 * ((this.ecParams.getCurve().getFieldSize() + 7) / 8);
                break;
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Sender's public key has invalid point encoding 0x");
                stringBuilder.append(Integer.toString(read, 16));
                throw new IOException(stringBuilder.toString());
        }
        byte[] bArr = new byte[(fieldSize + 1)];
        bArr[0] = (byte) read;
        Streams.readFully(inputStream, bArr, 1, bArr.length - 1);
        return new ECPublicKeyParameters(this.ecParams.getCurve().decodePoint(bArr), this.ecParams);
    }
}
