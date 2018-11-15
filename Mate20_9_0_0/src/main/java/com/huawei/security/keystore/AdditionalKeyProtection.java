package com.huawei.security.keystore;

import java.security.KeyStore.ProtectionParameter;

public final class AdditionalKeyProtection implements ProtectionParameter {
    private boolean mBiometricAuthenticationRequired;
    private int mBiometricType;
    private boolean mIsInvalidatedBySystemRooting;
    private boolean mIsTemplateBound;

    public static final class Builder {
        private boolean mBiometricAuthenticationRequired;
        private int mBiometricType;
        private boolean mIsInvalidatedBySystemRooting;
        private boolean mIsTemplateBound;

        public Builder setBiometricAuthenticationRequired(int type) {
            this.mBiometricType = type;
            return this;
        }

        public Builder setBiometricAuthenticationRequired(int type, boolean bindTemplate) {
            this.mBiometricType = type;
            this.mIsTemplateBound = bindTemplate;
            return this;
        }

        public Builder setInvalidatedBySystemRooting(boolean invalidateKey) {
            this.mIsInvalidatedBySystemRooting = invalidateKey;
            return this;
        }

        public AdditionalKeyProtection build() {
            return new AdditionalKeyProtection(this.mBiometricAuthenticationRequired, this.mBiometricType, this.mIsTemplateBound, this.mIsInvalidatedBySystemRooting);
        }
    }

    public AdditionalKeyProtection(boolean mBiometricAuthenticationRequired, int mBiometricType, boolean mIsTemplateBound, boolean mIsInvalidatedBySystemRooting) {
        this.mBiometricAuthenticationRequired = mBiometricAuthenticationRequired;
        this.mBiometricType = mBiometricType;
        this.mIsTemplateBound = mIsTemplateBound;
        this.mIsInvalidatedBySystemRooting = mIsInvalidatedBySystemRooting;
    }

    public boolean getBiometricAuthenticationRequired() {
        return this.mBiometricAuthenticationRequired;
    }

    public int getBiometricType() {
        return this.mBiometricType;
    }

    public boolean isTemplateBound() {
        return this.mIsTemplateBound;
    }

    public boolean isInvalidatedBySystemRooting() {
        return this.mIsInvalidatedBySystemRooting;
    }
}
