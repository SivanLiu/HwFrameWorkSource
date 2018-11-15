package com.huawei.security.keystore;

import java.security.interfaces.ECPublicKey;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;

public class HwUniversalKeyStoreECPublicKey extends HwUniversalKeyStorePublicKey implements ECPublicKey {
    private final ECParameterSpec mParams;
    private final ECPoint mW;

    public HwUniversalKeyStoreECPublicKey(String alias, int uid, byte[] x509EncodedForm, ECParameterSpec params, ECPoint w) {
        super(alias, uid, HwKeyProperties.KEY_ALGORITHM_EC, x509EncodedForm);
        this.mParams = params;
        this.mW = w;
    }

    public HwUniversalKeyStoreECPublicKey(String alias, int uid, ECPublicKey info) {
        this(alias, uid, info.getEncoded(), info.getParams(), info.getW());
        if (!"X.509".equalsIgnoreCase(info.getFormat())) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unsupported key export format: ");
            stringBuilder.append(info.getFormat());
            throw new IllegalArgumentException(stringBuilder.toString());
        }
    }

    public ECParameterSpec getParams() {
        return this.mParams;
    }

    public ECPoint getW() {
        return this.mW;
    }
}
