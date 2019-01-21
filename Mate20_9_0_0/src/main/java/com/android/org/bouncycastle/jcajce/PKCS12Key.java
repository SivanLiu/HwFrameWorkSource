package com.android.org.bouncycastle.jcajce;

import com.android.org.bouncycastle.crypto.PBEParametersGenerator;

public class PKCS12Key implements PBKDFKey {
    private final char[] password;
    private final boolean useWrongZeroLengthConversion;

    public PKCS12Key(char[] password) {
        this(password, false);
    }

    public PKCS12Key(char[] password, boolean useWrongZeroLengthConversion) {
        Object password2;
        if (password2 == null) {
            password2 = new char[0];
        }
        this.password = new char[password2.length];
        this.useWrongZeroLengthConversion = useWrongZeroLengthConversion;
        System.arraycopy(password2, 0, this.password, 0, password2.length);
    }

    public char[] getPassword() {
        return this.password;
    }

    public String getAlgorithm() {
        return "PKCS12";
    }

    public String getFormat() {
        return "PKCS12";
    }

    public byte[] getEncoded() {
        if (this.useWrongZeroLengthConversion && this.password.length == 0) {
            return new byte[2];
        }
        return PBEParametersGenerator.PKCS12PasswordToBytes(this.password);
    }
}
