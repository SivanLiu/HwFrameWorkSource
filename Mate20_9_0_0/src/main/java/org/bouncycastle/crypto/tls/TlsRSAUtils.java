package org.bouncycastle.crypto.tls;

import java.io.IOException;
import java.io.OutputStream;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSABlindedEngine;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.util.Arrays;

public class TlsRSAUtils {
    public static byte[] generateEncryptedPreMasterSecret(TlsContext tlsContext, RSAKeyParameters rSAKeyParameters, OutputStream outputStream) throws IOException {
        byte[] bArr = new byte[48];
        tlsContext.getSecureRandom().nextBytes(bArr);
        TlsUtils.writeVersion(tlsContext.getClientVersion(), bArr, 0);
        PKCS1Encoding pKCS1Encoding = new PKCS1Encoding(new RSABlindedEngine());
        pKCS1Encoding.init(true, new ParametersWithRandom(rSAKeyParameters, tlsContext.getSecureRandom()));
        try {
            byte[] processBlock = pKCS1Encoding.processBlock(bArr, 0, bArr.length);
            if (TlsUtils.isSSL(tlsContext)) {
                outputStream.write(processBlock);
                return bArr;
            }
            TlsUtils.writeOpaque16(processBlock, outputStream);
            return bArr;
        } catch (InvalidCipherTextException e) {
            throw new TlsFatalAlert((short) 80, e);
        }
    }

    public static byte[] safeDecryptPreMasterSecret(TlsContext tlsContext, RSAKeyParameters rSAKeyParameters, byte[] bArr) {
        byte[] processBlock;
        ProtocolVersion clientVersion = tlsContext.getClientVersion();
        byte[] bArr2 = new byte[48];
        tlsContext.getSecureRandom().nextBytes(bArr2);
        byte[] clone = Arrays.clone(bArr2);
        int i = 0;
        try {
            PKCS1Encoding pKCS1Encoding = new PKCS1Encoding(new RSABlindedEngine(), bArr2);
            pKCS1Encoding.init(false, new ParametersWithRandom(rSAKeyParameters, tlsContext.getSecureRandom()));
            processBlock = pKCS1Encoding.processBlock(bArr, 0, bArr.length);
        } catch (Exception e) {
            processBlock = clone;
        }
        int majorVersion = (clientVersion.getMajorVersion() ^ (processBlock[0] & 255)) | (clientVersion.getMinorVersion() ^ (processBlock[1] & 255));
        majorVersion |= majorVersion >> 1;
        majorVersion |= majorVersion >> 2;
        majorVersion = ~(((majorVersion | (majorVersion >> 4)) & 1) - 1);
        while (i < 48) {
            processBlock[i] = (byte) ((processBlock[i] & (~majorVersion)) | (bArr2[i] & majorVersion));
            i++;
        }
        return processBlock;
    }
}
