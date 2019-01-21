package org.bouncycastle.jcajce.provider.asymmetric.dh;

import java.io.IOException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.spec.DHParameterSpec;
import org.bouncycastle.asn1.ASN1Encoding;
import org.bouncycastle.asn1.pkcs.DHParameter;

public class AlgorithmParametersSpi extends java.security.AlgorithmParametersSpi {
    DHParameterSpec currentSpec;

    protected byte[] engineGetEncoded() {
        try {
            return new DHParameter(this.currentSpec.getP(), this.currentSpec.getG(), this.currentSpec.getL()).getEncoded(ASN1Encoding.DER);
        } catch (IOException e) {
            throw new RuntimeException("Error encoding DHParameters");
        }
    }

    protected byte[] engineGetEncoded(String str) {
        return isASN1FormatString(str) ? engineGetEncoded() : null;
    }

    protected AlgorithmParameterSpec engineGetParameterSpec(Class cls) throws InvalidParameterSpecException {
        if (cls != null) {
            return localEngineGetParameterSpec(cls);
        }
        throw new NullPointerException("argument to getParameterSpec must not be null");
    }

    protected void engineInit(AlgorithmParameterSpec algorithmParameterSpec) throws InvalidParameterSpecException {
        if (algorithmParameterSpec instanceof DHParameterSpec) {
            this.currentSpec = (DHParameterSpec) algorithmParameterSpec;
            return;
        }
        throw new InvalidParameterSpecException("DHParameterSpec required to initialise a Diffie-Hellman algorithm parameters object");
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x0030 in {3, 5, 7, 11, 14} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:242)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:52)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:42)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1257)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    protected void engineInit(byte[] r4) throws java.io.IOException {
        /*
        r3 = this;
        r4 = org.bouncycastle.asn1.pkcs.DHParameter.getInstance(r4);	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        r0 = r4.getL();	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        if (r0 == 0) goto L_0x0022;	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        r0 = new javax.crypto.spec.DHParameterSpec;	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        r1 = r4.getP();	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        r2 = r4.getG();	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        r4 = r4.getL();	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        r4 = r4.intValue();	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        r0.<init>(r1, r2, r4);	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        r3.currentSpec = r0;	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        return;	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        r0 = new javax.crypto.spec.DHParameterSpec;	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        r1 = r4.getP();	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        r4 = r4.getG();	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        r0.<init>(r1, r4);	 Catch:{ ClassCastException -> 0x003a, ArrayIndexOutOfBoundsException -> 0x0031 }
        goto L_0x001f;
        return;
        r4 = move-exception;
        r4 = new java.io.IOException;
        r0 = "Not a valid DH Parameter encoding.";
        r4.<init>(r0);
        throw r4;
        r4 = move-exception;
        r4 = new java.io.IOException;
        r0 = "Not a valid DH Parameter encoding.";
        r4.<init>(r0);
        throw r4;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.jcajce.provider.asymmetric.dh.AlgorithmParametersSpi.engineInit(byte[]):void");
    }

    protected void engineInit(byte[] bArr, String str) throws IOException {
        if (isASN1FormatString(str)) {
            engineInit(bArr);
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown parameter format ");
        stringBuilder.append(str);
        throw new IOException(stringBuilder.toString());
    }

    protected String engineToString() {
        return "Diffie-Hellman Parameters";
    }

    protected boolean isASN1FormatString(String str) {
        return str == null || str.equals("ASN.1");
    }

    protected AlgorithmParameterSpec localEngineGetParameterSpec(Class cls) throws InvalidParameterSpecException {
        if (cls == DHParameterSpec.class || cls == AlgorithmParameterSpec.class) {
            return this.currentSpec;
        }
        throw new InvalidParameterSpecException("unknown parameter spec passed to DH parameters object.");
    }
}
