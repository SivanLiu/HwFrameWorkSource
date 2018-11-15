package org.bouncycastle.jcajce.provider.asymmetric.elgamal;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.DHParameterSpec;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.oiw.ElGamalParameter;
import org.bouncycastle.jcajce.provider.symmetric.util.BaseAlgorithmParameters;
import org.bouncycastle.jce.spec.ElGamalParameterSpec;

public class AlgorithmParametersSpi extends BaseAlgorithmParameters {
    ElGamalParameterSpec currentSpec;

    protected byte[] engineGetEncoded() {
        try {
            return new ElGamalParameter(this.currentSpec.getP(), this.currentSpec.getG()).getEncoded(ASN1Encoding.DER);
        } catch (IOException e) {
            throw new RuntimeException("Error encoding ElGamalParameters");
        }
    }

    protected byte[] engineGetEncoded(String str) {
        return (isASN1FormatString(str) || str.equalsIgnoreCase("X.509")) ? engineGetEncoded() : null;
    }

    protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
        boolean z = algorithmParameterSpec instanceof ElGamalParameterSpec;
        if (!z && !(algorithmParameterSpec instanceof DHParameterSpec)) {
            throw new InvalidParameterSpecException("DHParameterSpec required to initialise a ElGamal algorithm parameters object");
        } else if (z) {
            this.currentSpec = (ElGamalParameterSpec) algorithmParameterSpec;
        } else {
            DHParameterSpec dHParameterSpec = (DHParameterSpec) algorithmParameterSpec;
            this.currentSpec = new ElGamalParameterSpec(dHParameterSpec.getP(), dHParameterSpec.getG());
        }
    }

    protected void engineInit(byte[] bArr) throws IOException {
        try {
            ElGamalParameter instance = ElGamalParameter.getInstance(ASN1Primitive.fromByteArray(bArr));
            this.currentSpec = new ElGamalParameterSpec(instance.getP(), instance.getG());
        } catch (ClassCastException e) {
            throw new IOException("Not a valid ElGamal Parameter encoding.");
        } catch (ArrayIndexOutOfBoundsException e2) {
            throw new IOException("Not a valid ElGamal Parameter encoding.");
        }
    }

    protected void engineInit(byte[] bArr, String str) throws IOException {
        if (isASN1FormatString(str) || str.equalsIgnoreCase("X.509")) {
            engineInit(bArr);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown parameter format ");
        stringBuilder.append(str);
        throw new IOException(stringBuilder.toString());
    }

    protected String engineToString() {
        return "ElGamal Parameters";
    }

    protected AlgorithmParameterSpec localEngineGetParameterSpec(Class cls) throws InvalidParameterSpecException {
        if (cls == ElGamalParameterSpec.class || cls == AlgorithmParameterSpec.class) {
            return this.currentSpec;
        }
        if (cls == DHParameterSpec.class) {
            return new DHParameterSpec(this.currentSpec.getP(), this.currentSpec.getG());
        }
        throw new InvalidParameterSpecException("unknown parameter spec passed to ElGamal parameters object.");
    }
}
