package org.bouncycastle.jcajce.provider.config;

import java.io.OutputStream;
import java.security.KeyStore.PasswordProtection;
import java.security.KeyStore.ProtectionParameter;

public class PKCS12StoreParameter extends org.bouncycastle.jcajce.PKCS12StoreParameter {
    public PKCS12StoreParameter(OutputStream outputStream, ProtectionParameter protectionParameter) {
        super(outputStream, protectionParameter, false);
    }

    public PKCS12StoreParameter(OutputStream outputStream, ProtectionParameter protectionParameter, boolean z) {
        super(outputStream, protectionParameter, z);
    }

    public PKCS12StoreParameter(OutputStream outputStream, char[] cArr) {
        super(outputStream, cArr, false);
    }

    public PKCS12StoreParameter(OutputStream outputStream, char[] cArr, boolean z) {
        super(outputStream, new PasswordProtection(cArr), z);
    }
}
