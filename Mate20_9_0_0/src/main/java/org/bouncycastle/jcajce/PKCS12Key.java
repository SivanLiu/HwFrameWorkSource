package org.bouncycastle.jcajce;

import org.bouncycastle.crypto.PBEParametersGenerator;

public class PKCS12Key implements PBKDFKey {
    private final char[] password;
    private final boolean useWrongZeroLengthConversion;

    public PKCS12Key(char[] cArr) {
        this(cArr, false);
    }

    public PKCS12Key(char[] cArr, boolean z) {
        Object cArr2;
        if (cArr2 == null) {
            cArr2 = new char[0];
        }
        this.password = new char[cArr2.length];
        this.useWrongZeroLengthConversion = z;
        System.arraycopy(cArr2, 0, this.password, 0, cArr2.length);
    }

    public String getAlgorithm() {
        return "PKCS12";
    }

    public byte[] getEncoded() {
        return (this.useWrongZeroLengthConversion && this.password.length == 0) ? new byte[2] : PBEParametersGenerator.PKCS12PasswordToBytes(this.password);
    }

    public String getFormat() {
        return "PKCS12";
    }

    public char[] getPassword() {
        return this.password;
    }
}
