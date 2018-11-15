package org.bouncycastle.jcajce;

import java.io.OutputStream;
import java.security.KeyStore.LoadStoreParameter;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.ProtectionParameter;

public class PKCS12StoreParameter implements LoadStoreParameter {
    private final boolean forDEREncoding;
    private final OutputStream out;
    private final ProtectionParameter protectionParameter;

    public PKCS12StoreParameter(OutputStream outputStream, ProtectionParameter protectionParameter) {
        this(outputStream, protectionParameter, false);
    }

    public PKCS12StoreParameter(OutputStream outputStream, ProtectionParameter protectionParameter, boolean z) {
        this.out = outputStream;
        this.protectionParameter = protectionParameter;
        this.forDEREncoding = z;
    }

    public PKCS12StoreParameter(OutputStream outputStream, char[] cArr) {
        this(outputStream, cArr, false);
    }

    public PKCS12StoreParameter(OutputStream outputStream, char[] cArr, boolean z) {
        this(outputStream, new PasswordProtection(cArr), z);
    }

    public OutputStream getOutputStream() {
        return this.out;
    }

    public ProtectionParameter getProtectionParameter() {
        return this.protectionParameter;
    }

    public boolean isForDEREncoding() {
        return this.forDEREncoding;
    }
}
