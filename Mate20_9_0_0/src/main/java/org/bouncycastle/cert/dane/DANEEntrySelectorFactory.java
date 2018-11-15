package org.bouncycastle.cert.dane;

import java.io.OutputStream;
import org.bouncycastle.operator.DigestCalculator;
import org.bouncycastle.util.Strings;
import org.bouncycastle.util.encoders.Hex;

public class DANEEntrySelectorFactory {
    private final DigestCalculator digestCalculator;

    public DANEEntrySelectorFactory(DigestCalculator digestCalculator) {
        this.digestCalculator = digestCalculator;
    }

    public DANEEntrySelector createSelector(String str) throws DANEException {
        byte[] toUTF8ByteArray = Strings.toUTF8ByteArray(str.substring(0, str.indexOf(64)));
        try {
            OutputStream outputStream = this.digestCalculator.getOutputStream();
            outputStream.write(toUTF8ByteArray);
            outputStream.close();
            toUTF8ByteArray = this.digestCalculator.getDigest();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(Strings.fromByteArray(Hex.encode(toUTF8ByteArray)));
            stringBuilder.append("._smimecert.");
            stringBuilder.append(str.substring(str.indexOf(64) + 1));
            return new DANEEntrySelector(stringBuilder.toString());
        } catch (Throwable e) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Unable to calculate digest string: ");
            stringBuilder2.append(e.getMessage());
            throw new DANEException(stringBuilder2.toString(), e);
        }
    }
}
