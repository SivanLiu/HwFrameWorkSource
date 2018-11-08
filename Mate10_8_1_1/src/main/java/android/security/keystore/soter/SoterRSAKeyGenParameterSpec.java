package android.security.keystore.soter;

import java.math.BigInteger;
import java.security.spec.RSAKeyGenParameterSpec;

public class SoterRSAKeyGenParameterSpec extends RSAKeyGenParameterSpec {
    private boolean isAutoAddCounterWhenGetPublicKey;
    private boolean isAutoSignedWithCommonkWhenGetPublicKey;
    private boolean isForSoter;
    private boolean isNeedUseNextAttk;
    private boolean isSecmsgFidCounterSignedWhenSign;
    private String mAutoSignedKeyNameWhenGetPublicKey;

    public SoterRSAKeyGenParameterSpec(int keysize, BigInteger publicExponent, boolean isForSoter, boolean isAutoSignedWithCommonkWhenGetPublicKey, String signedKeyNameWhenGetPublicKey, boolean isSecmsgFidCounterSignedWhenSign, boolean isAutoAddCounterWhenGetPublicKey, boolean isNeedNextAttk) {
        super(keysize, publicExponent);
        this.isForSoter = false;
        this.isAutoSignedWithCommonkWhenGetPublicKey = false;
        this.mAutoSignedKeyNameWhenGetPublicKey = "";
        this.isSecmsgFidCounterSignedWhenSign = false;
        this.isAutoAddCounterWhenGetPublicKey = false;
        this.isNeedUseNextAttk = false;
        this.isForSoter = isForSoter;
        this.isAutoSignedWithCommonkWhenGetPublicKey = isAutoSignedWithCommonkWhenGetPublicKey;
        this.mAutoSignedKeyNameWhenGetPublicKey = signedKeyNameWhenGetPublicKey;
        this.isSecmsgFidCounterSignedWhenSign = isSecmsgFidCounterSignedWhenSign;
        this.isAutoAddCounterWhenGetPublicKey = isAutoAddCounterWhenGetPublicKey;
        this.isNeedUseNextAttk = isNeedNextAttk;
    }

    public SoterRSAKeyGenParameterSpec(boolean isForSoter, boolean isAutoSignedWithCommonkWhenGetPublicKey, String signedKeyNameWhenGetPublicKey, boolean isSecmsgFidCounterSignedWhenSign, boolean isAutoAddCounterWhenGetPubli, boolean isNeedNextAttkcKey) {
        this(2048, RSAKeyGenParameterSpec.F4, isForSoter, isAutoSignedWithCommonkWhenGetPublicKey, signedKeyNameWhenGetPublicKey, isSecmsgFidCounterSignedWhenSign, isAutoAddCounterWhenGetPubli, isNeedNextAttkcKey);
    }

    public boolean isForSoter() {
        return this.isForSoter;
    }

    public void setIsForSoter(boolean isForSoter) {
        this.isForSoter = isForSoter;
    }

    public boolean isAutoSignedWithCommonkWhenGetPublicKey() {
        return this.isAutoSignedWithCommonkWhenGetPublicKey;
    }

    public void setIsAutoSignedWithCommonkWhenGetPublicKey(boolean isAutoSignedWithCommonkWhenGetPublicKey) {
        this.isAutoSignedWithCommonkWhenGetPublicKey = isAutoSignedWithCommonkWhenGetPublicKey;
    }

    public String getAutoSignedKeyNameWhenGetPublicKey() {
        return this.mAutoSignedKeyNameWhenGetPublicKey;
    }

    public void setAutoSignedKeyNameWhenGetPublicKey(String mAutoSignedKeyNameWhenGetPublicKey) {
        this.mAutoSignedKeyNameWhenGetPublicKey = mAutoSignedKeyNameWhenGetPublicKey;
    }

    public boolean isSecmsgFidCounterSignedWhenSign() {
        return this.isSecmsgFidCounterSignedWhenSign;
    }

    public void setIsSecmsgFidCounterSignedWhenSign(boolean isSecmsgFidCounterSignedWhenSign) {
        this.isSecmsgFidCounterSignedWhenSign = isSecmsgFidCounterSignedWhenSign;
    }

    public boolean isAutoAddCounterWhenGetPublicKey() {
        return this.isAutoAddCounterWhenGetPublicKey;
    }

    public void setIsAutoAddCounterWhenGetPublicKey(boolean isAutoAddCounterWhenGetPublicKey) {
        this.isAutoAddCounterWhenGetPublicKey = isAutoAddCounterWhenGetPublicKey;
    }

    public boolean isNeedUseNextAttk() {
        return this.isNeedUseNextAttk;
    }

    public void setIsNeedUseNextAttk(boolean isNeedUseNextAttk) {
        this.isNeedUseNextAttk = isNeedUseNextAttk;
    }

    public String toString() {
        return "SoterRSAKeyGenParameterSpec{isForSoter=" + this.isForSoter + ", isAutoSignedWithCommonkWhenGetPublicKey=" + this.isAutoSignedWithCommonkWhenGetPublicKey + ", mAutoSignedKeyNameWhenGetPublicKey='" + this.mAutoSignedKeyNameWhenGetPublicKey + '\'' + ", isSecmsgFidCounterSignedWhenSign=" + this.isSecmsgFidCounterSignedWhenSign + ", isAutoAddCounterWhenGetPublicKey=" + this.isAutoAddCounterWhenGetPublicKey + ", isNeedUseNextAttk=" + this.isNeedUseNextAttk + '}';
    }
}
