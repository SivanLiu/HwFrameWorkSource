package org.bouncycastle.jcajce.provider.symmetric.util;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.IvParameterSpec;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.util.Arrays;

public class IvAlgorithmParameters extends BaseAlgorithmParameters {
    private byte[] iv;

    protected byte[] engineGetEncoded() throws IOException {
        return engineGetEncoded("ASN.1");
    }

    protected byte[] engineGetEncoded(String str) throws IOException {
        return isASN1FormatString(str) ? new DEROctetString(engineGetEncoded("RAW")).getEncoded() : str.equals("RAW") ? Arrays.clone(this.iv) : null;
    }

    protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
        if (algorithmParameterSpec instanceof IvParameterSpec) {
            this.iv = ((IvParameterSpec) algorithmParameterSpec).getIV();
            return;
        }
        throw new InvalidParameterSpecException("IvParameterSpec required to initialise a IV parameters algorithm parameters object");
    }

    protected void engineInit(byte[] bArr) throws IOException {
        if (bArr.length % 8 != 0 && bArr[0] == (byte) 4 && bArr[1] == bArr.length - 2) {
            bArr = ((ASN1OctetString) ASN1Primitive.fromByteArray(bArr)).getOctets();
        }
        this.iv = Arrays.clone(bArr);
    }

    protected void engineInit(byte[] bArr, String str) throws IOException {
        if (isASN1FormatString(str)) {
            try {
                engineInit(((ASN1OctetString) ASN1Primitive.fromByteArray(bArr)).getOctets());
            } catch (Exception e) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Exception decoding: ");
                stringBuilder.append(e);
                throw new IOException(stringBuilder.toString());
            }
        } else if (str.equals("RAW")) {
            engineInit(bArr);
        } else {
            throw new IOException("Unknown parameters format in IV parameters object");
        }
    }

    protected String engineToString() {
        return "IV Parameters";
    }

    protected AlgorithmParameterSpec localEngineGetParameterSpec(Class cls) throws InvalidParameterSpecException {
        if (cls == IvParameterSpec.class || cls == AlgorithmParameterSpec.class) {
            return new IvParameterSpec(this.iv);
        }
        throw new InvalidParameterSpecException("unknown parameter spec passed to IV parameters object.");
    }
}
