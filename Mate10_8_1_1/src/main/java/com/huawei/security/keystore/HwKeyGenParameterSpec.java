package com.huawei.security.keystore;

import android.security.keystore.KeyGenParameterSpec;
import android.text.TextUtils;
import java.math.BigInteger;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Date;
import javax.security.auth.x500.X500Principal;

public final class HwKeyGenParameterSpec implements AlgorithmParameterSpec, UserAuthArgs {
    private static final Date DEFAULT_CERT_NOT_AFTER = new Date(2461449600000L);
    private static final Date DEFAULT_CERT_NOT_BEFORE = new Date(0);
    private static final BigInteger DEFAULT_CERT_SERIAL_NUMBER = new BigInteger("1");
    private static final X500Principal DEFAULT_CERT_SUBJECT = new X500Principal("CN=fake");
    private final byte[] mAttestationChallenge;
    private final String[] mBlockModes;
    private final Date mCertificateNotAfter;
    private final Date mCertificateNotBefore;
    private final BigInteger mCertificateSerialNumber;
    private final X500Principal mCertificateSubject;
    private final String[] mDigests;
    private final String[] mEncryptionPaddings;
    private final boolean mInvalidatedByBiometricEnrollment;
    private final boolean mIsAdditionalProtectionAllowed;
    private final boolean mIsStrongBoxBacked;
    private final int mKeySize;
    private final Date mKeyValidityForConsumptionEnd;
    private final Date mKeyValidityForOriginationEnd;
    private final Date mKeyValidityStart;
    private final String mKeystoreAlias;
    private final int mPurposes;
    private final boolean mRandomizedEncryptionRequired;
    private final String[] mSignaturePaddings;
    private final AlgorithmParameterSpec mSpec;
    private final int mUid;
    private final boolean mUniqueIdIncluded;
    private final boolean mUnlockedDeviceRequired;
    private final boolean mUserAuthenticationRequired;
    private final boolean mUserAuthenticationValidWhileOnBody;
    private final int mUserAuthenticationValidityDurationSeconds;
    private final boolean mUserConfirmationRequired;
    private final boolean mUserPresenceRequired;

    public static final class Builder {
        private byte[] mAttestationChallenge;
        private String[] mBlockModes;
        private Date mCertificateNotAfter;
        private Date mCertificateNotBefore;
        private BigInteger mCertificateSerialNumber;
        private X500Principal mCertificateSubject;
        private String[] mDigests;
        private String[] mEncryptionPaddings;
        private boolean mInvalidatedByBiometricEnrollment;
        private boolean mIsAdditionalProtectionAllowed;
        private boolean mIsStrongBoxBacked;
        private int mKeySize;
        private Date mKeyValidityForConsumptionEnd;
        private Date mKeyValidityForOriginationEnd;
        private Date mKeyValidityStart;
        private final String mKeystoreAlias;
        private int mPurposes;
        private boolean mRandomizedEncryptionRequired;
        private String[] mSignaturePaddings;
        private AlgorithmParameterSpec mSpec;
        private int mUid;
        private boolean mUniqueIdIncluded;
        private boolean mUnlockedDeviceRequired;
        private boolean mUserAuthenticationRequired;
        private boolean mUserAuthenticationValidWhileOnBody;
        private int mUserAuthenticationValidityDurationSeconds;
        private boolean mUserConfirmationRequired;
        private boolean mUserPresenceRequired;

        public Builder(String keystoreAlias, int purposes) {
            this.mUid = -1;
            this.mKeySize = -1;
            this.mRandomizedEncryptionRequired = true;
            this.mUserAuthenticationValidityDurationSeconds = -1;
            this.mUserPresenceRequired = false;
            this.mAttestationChallenge = null;
            this.mUniqueIdIncluded = false;
            this.mInvalidatedByBiometricEnrollment = true;
            this.mIsStrongBoxBacked = false;
            this.mUnlockedDeviceRequired = false;
            this.mIsAdditionalProtectionAllowed = false;
            if (keystoreAlias == null) {
                throw new NullPointerException("keystoreAlias == null");
            } else if (keystoreAlias.isEmpty()) {
                throw new IllegalArgumentException("keystoreAlias must not be empty");
            } else {
                this.mKeystoreAlias = keystoreAlias;
                this.mPurposes = purposes;
            }
        }

        public Builder(HwKeyGenParameterSpec sourceSpec) {
            this(sourceSpec.getKeystoreAlias(), sourceSpec.getPurposes());
            this.mUid = sourceSpec.getUid();
            this.mKeySize = sourceSpec.getKeySize();
            this.mSpec = sourceSpec.getAlgorithmParameterSpec();
            this.mCertificateSubject = sourceSpec.getCertificateSubject();
            this.mCertificateSerialNumber = sourceSpec.getCertificateSerialNumber();
            this.mCertificateNotBefore = sourceSpec.getCertificateNotBefore();
            this.mCertificateNotAfter = sourceSpec.getCertificateNotAfter();
            this.mKeyValidityStart = sourceSpec.getKeyValidityStart();
            this.mKeyValidityForOriginationEnd = sourceSpec.getKeyValidityForOriginationEnd();
            this.mKeyValidityForConsumptionEnd = sourceSpec.getKeyValidityForConsumptionEnd();
            this.mPurposes = sourceSpec.getPurposes();
            if (sourceSpec.isDigestsSpecified()) {
                this.mDigests = sourceSpec.getDigests();
            }
            this.mEncryptionPaddings = sourceSpec.getEncryptionPaddings();
            this.mSignaturePaddings = sourceSpec.getSignaturePaddings();
            this.mBlockModes = sourceSpec.getBlockModes();
            this.mRandomizedEncryptionRequired = sourceSpec.isRandomizedEncryptionRequired();
            this.mUserAuthenticationRequired = sourceSpec.isUserAuthenticationRequired();
            this.mUserAuthenticationValidityDurationSeconds = sourceSpec.getUserAuthenticationValidityDurationSeconds();
            this.mUserPresenceRequired = sourceSpec.isUserPresenceRequired();
            this.mAttestationChallenge = sourceSpec.getAttestationChallenge();
            this.mUniqueIdIncluded = sourceSpec.isUniqueIdIncluded();
            this.mUserAuthenticationValidWhileOnBody = sourceSpec.isUserAuthenticationValidWhileOnBody();
            this.mInvalidatedByBiometricEnrollment = sourceSpec.isInvalidatedByBiometricEnrollment();
            this.mIsAdditionalProtectionAllowed = sourceSpec.isAdditionalProtectionAllowed();
        }

        public Builder setUid(int uid) {
            this.mUid = uid;
            return this;
        }

        public Builder setKeySize(int keySize) {
            if (keySize < 0) {
                throw new IllegalArgumentException("keySize < 0");
            }
            this.mKeySize = keySize;
            return this;
        }

        public Builder setAlgorithmParameterSpec(AlgorithmParameterSpec spec) {
            if (spec == null) {
                throw new NullPointerException("spec == null");
            }
            this.mSpec = spec;
            return this;
        }

        public Builder setCertificateSubject(X500Principal subject) {
            if (subject == null) {
                throw new NullPointerException("subject == null");
            }
            this.mCertificateSubject = subject;
            return this;
        }

        public Builder setCertificateSerialNumber(BigInteger serialNumber) {
            if (serialNumber == null) {
                throw new NullPointerException("serialNumber == null");
            }
            this.mCertificateSerialNumber = serialNumber;
            return this;
        }

        public Builder setCertificateNotBefore(Date date) {
            if (date == null) {
                throw new NullPointerException("date == null");
            }
            this.mCertificateNotBefore = ArrayUtils.cloneIfNotNull(date);
            return this;
        }

        public Builder setCertificateNotAfter(Date date) {
            if (date == null) {
                throw new NullPointerException("date == null");
            }
            this.mCertificateNotAfter = ArrayUtils.cloneIfNotNull(date);
            return this;
        }

        public Builder setKeyValidityStart(Date startDate) {
            this.mKeyValidityStart = ArrayUtils.cloneIfNotNull(startDate);
            return this;
        }

        public Builder setKeyValidityEnd(Date endDate) {
            setKeyValidityForOriginationEnd(endDate);
            setKeyValidityForConsumptionEnd(endDate);
            return this;
        }

        public Builder setKeyValidityForOriginationEnd(Date endDate) {
            this.mKeyValidityForOriginationEnd = ArrayUtils.cloneIfNotNull(endDate);
            return this;
        }

        public Builder setKeyValidityForConsumptionEnd(Date endDate) {
            this.mKeyValidityForConsumptionEnd = ArrayUtils.cloneIfNotNull(endDate);
            return this;
        }

        public Builder setDigests(String... digests) {
            this.mDigests = ArrayUtils.cloneIfNotEmpty(digests);
            return this;
        }

        public Builder setEncryptionPaddings(String... paddings) {
            this.mEncryptionPaddings = ArrayUtils.cloneIfNotEmpty(paddings);
            return this;
        }

        public Builder setSignaturePaddings(String... paddings) {
            this.mSignaturePaddings = ArrayUtils.cloneIfNotEmpty(paddings);
            return this;
        }

        public Builder setBlockModes(String... blockModes) {
            this.mBlockModes = ArrayUtils.cloneIfNotEmpty(blockModes);
            return this;
        }

        public Builder setRandomizedEncryptionRequired(boolean required) {
            this.mRandomizedEncryptionRequired = required;
            return this;
        }

        public Builder setUserAuthenticationRequired(boolean required) {
            this.mUserAuthenticationRequired = required;
            return this;
        }

        public Builder setUserConfirmationRequired(boolean required) {
            this.mUserConfirmationRequired = required;
            return this;
        }

        public Builder setUserAuthenticationValidityDurationSeconds(int seconds) {
            if (seconds < -1) {
                throw new IllegalArgumentException("seconds must be -1 or larger");
            }
            this.mUserAuthenticationValidityDurationSeconds = seconds;
            return this;
        }

        public Builder setUserPresenceRequired(boolean required) {
            this.mUserPresenceRequired = required;
            return this;
        }

        public Builder setAttestationChallenge(byte[] attestationChallenge) {
            this.mAttestationChallenge = ArrayUtils.cloneIfNotNull(attestationChallenge);
            return this;
        }

        public Builder setUniqueIdIncluded(boolean uniqueIdIncluded) {
            this.mUniqueIdIncluded = uniqueIdIncluded;
            return this;
        }

        public Builder setUserAuthenticationValidWhileOnBody(boolean remainsValid) {
            this.mUserAuthenticationValidWhileOnBody = remainsValid;
            return this;
        }

        public Builder setInvalidatedByBiometricEnrollment(boolean invalidateKey) {
            this.mInvalidatedByBiometricEnrollment = invalidateKey;
            return this;
        }

        public Builder setIsStrongBoxBacked(boolean isStrongBoxBacked) {
            this.mIsStrongBoxBacked = isStrongBoxBacked;
            return this;
        }

        public Builder setUnlockedDeviceRequired(boolean unlockedDeviceRequired) {
            this.mUnlockedDeviceRequired = unlockedDeviceRequired;
            return this;
        }

        public Builder setIsAddtionalProtectionAllowed(boolean allowed) {
            this.mIsAdditionalProtectionAllowed = allowed;
            return this;
        }

        public HwKeyGenParameterSpec build() {
            return new HwKeyGenParameterSpec(this.mKeystoreAlias, this.mUid, this.mKeySize, this.mSpec, this.mCertificateSubject, this.mCertificateSerialNumber, this.mCertificateNotBefore, this.mCertificateNotAfter, this.mKeyValidityStart, this.mKeyValidityForOriginationEnd, this.mKeyValidityForConsumptionEnd, this.mPurposes, this.mDigests, this.mEncryptionPaddings, this.mSignaturePaddings, this.mBlockModes, this.mRandomizedEncryptionRequired, this.mUserAuthenticationRequired, this.mUserAuthenticationValidityDurationSeconds, this.mUserPresenceRequired, this.mAttestationChallenge, this.mUniqueIdIncluded, this.mUserAuthenticationValidWhileOnBody, this.mInvalidatedByBiometricEnrollment, this.mIsStrongBoxBacked, this.mUserConfirmationRequired, this.mUnlockedDeviceRequired, this.mIsAdditionalProtectionAllowed);
        }
    }

    public HwKeyGenParameterSpec(String keyStoreAlias, int uid, int keySize, AlgorithmParameterSpec spec, X500Principal certificateSubject, BigInteger certificateSerialNumber, Date certificateNotBefore, Date certificateNotAfter, Date keyValidityStart, Date keyValidityForOriginationEnd, Date keyValidityForConsumptionEnd, int purposes, String[] digests, String[] encryptionPaddings, String[] signaturePaddings, String[] blockModes, boolean randomizedEncryptionRequired, boolean userAuthenticationRequired, int userAuthenticationValidityDurationSeconds, boolean userPresenceRequired, byte[] attestationChallenge, boolean uniqueIdIncluded, boolean userAuthenticationValidWhileOnBody, boolean invalidatedByBiometricEnrollment, boolean isStrongBoxBacked, boolean userConfirmationRequired, boolean unlockedDeviceRequired, boolean isAdditionalProtectionAllowed) {
        if (TextUtils.isEmpty(keyStoreAlias)) {
            throw new IllegalArgumentException("keyStoreAlias must not be empty");
        }
        if (certificateSubject == null) {
            certificateSubject = DEFAULT_CERT_SUBJECT;
        }
        if (certificateNotBefore == null) {
            certificateNotBefore = DEFAULT_CERT_NOT_BEFORE;
        }
        if (certificateNotAfter == null) {
            certificateNotAfter = DEFAULT_CERT_NOT_AFTER;
        }
        if (certificateSerialNumber == null) {
            certificateSerialNumber = DEFAULT_CERT_SERIAL_NUMBER;
        }
        if (certificateNotAfter.before(certificateNotBefore)) {
            throw new IllegalArgumentException("certificateNotAfter < certificateNotBefore");
        }
        this.mKeystoreAlias = keyStoreAlias;
        this.mUid = uid;
        this.mKeySize = keySize;
        this.mSpec = spec;
        this.mCertificateSubject = certificateSubject;
        this.mCertificateSerialNumber = certificateSerialNumber;
        this.mCertificateNotBefore = ArrayUtils.cloneIfNotNull(certificateNotBefore);
        this.mCertificateNotAfter = ArrayUtils.cloneIfNotNull(certificateNotAfter);
        this.mKeyValidityStart = ArrayUtils.cloneIfNotNull(keyValidityStart);
        this.mKeyValidityForOriginationEnd = ArrayUtils.cloneIfNotNull(keyValidityForOriginationEnd);
        this.mKeyValidityForConsumptionEnd = ArrayUtils.cloneIfNotNull(keyValidityForConsumptionEnd);
        this.mPurposes = purposes;
        this.mDigests = ArrayUtils.cloneIfNotEmpty(digests);
        this.mEncryptionPaddings = ArrayUtils.cloneIfNotEmpty(ArrayUtils.nullToEmpty(encryptionPaddings));
        this.mSignaturePaddings = ArrayUtils.cloneIfNotEmpty(ArrayUtils.nullToEmpty(signaturePaddings));
        this.mBlockModes = ArrayUtils.cloneIfNotEmpty(ArrayUtils.nullToEmpty(blockModes));
        this.mRandomizedEncryptionRequired = randomizedEncryptionRequired;
        this.mUserAuthenticationRequired = userAuthenticationRequired;
        this.mUserPresenceRequired = userPresenceRequired;
        this.mUserAuthenticationValidityDurationSeconds = userAuthenticationValidityDurationSeconds;
        this.mAttestationChallenge = ArrayUtils.cloneIfNotNull(attestationChallenge);
        this.mIsAdditionalProtectionAllowed = isAdditionalProtectionAllowed;
        this.mUniqueIdIncluded = uniqueIdIncluded;
        this.mUserAuthenticationValidWhileOnBody = userAuthenticationValidWhileOnBody;
        this.mInvalidatedByBiometricEnrollment = invalidatedByBiometricEnrollment;
        this.mIsStrongBoxBacked = isStrongBoxBacked;
        this.mUserConfirmationRequired = userConfirmationRequired;
        this.mUnlockedDeviceRequired = unlockedDeviceRequired;
    }

    public static HwKeyGenParameterSpec getInstance(KeyGenParameterSpec sourceSpec) {
        return new HwKeyGenParameterSpec(sourceSpec.getKeystoreAlias(), -1, sourceSpec.getKeySize(), sourceSpec.getAlgorithmParameterSpec(), sourceSpec.getCertificateSubject(), sourceSpec.getCertificateSerialNumber(), sourceSpec.getCertificateNotBefore(), sourceSpec.getCertificateNotAfter(), sourceSpec.getKeyValidityStart(), sourceSpec.getKeyValidityForOriginationEnd(), sourceSpec.getKeyValidityForConsumptionEnd(), sourceSpec.getPurposes(), sourceSpec.getDigests(), sourceSpec.getEncryptionPaddings(), sourceSpec.getSignaturePaddings(), sourceSpec.getBlockModes(), sourceSpec.isRandomizedEncryptionRequired(), sourceSpec.isUserAuthenticationRequired(), sourceSpec.getUserAuthenticationValidityDurationSeconds(), false, sourceSpec.getAttestationChallenge(), false, sourceSpec.isUserAuthenticationValidWhileOnBody(), sourceSpec.isInvalidatedByBiometricEnrollment(), false, false, false, false);
    }

    public String getKeystoreAlias() {
        return this.mKeystoreAlias;
    }

    public int getUid() {
        return this.mUid;
    }

    public int getKeySize() {
        return this.mKeySize;
    }

    public AlgorithmParameterSpec getAlgorithmParameterSpec() {
        return this.mSpec;
    }

    public X500Principal getCertificateSubject() {
        return this.mCertificateSubject;
    }

    public BigInteger getCertificateSerialNumber() {
        return this.mCertificateSerialNumber;
    }

    public Date getCertificateNotBefore() {
        return ArrayUtils.cloneIfNotNull(this.mCertificateNotBefore);
    }

    public Date getCertificateNotAfter() {
        return ArrayUtils.cloneIfNotNull(this.mCertificateNotAfter);
    }

    public Date getKeyValidityStart() {
        return ArrayUtils.cloneIfNotNull(this.mKeyValidityStart);
    }

    public Date getKeyValidityForConsumptionEnd() {
        return ArrayUtils.cloneIfNotNull(this.mKeyValidityForConsumptionEnd);
    }

    public Date getKeyValidityForOriginationEnd() {
        return ArrayUtils.cloneIfNotNull(this.mKeyValidityForOriginationEnd);
    }

    public int getPurposes() {
        return this.mPurposes;
    }

    public String[] getDigests() {
        if (this.mDigests != null) {
            return ArrayUtils.cloneIfNotEmpty(this.mDigests);
        }
        throw new IllegalStateException("Digests not specified");
    }

    public boolean isDigestsSpecified() {
        return this.mDigests != null;
    }

    public String[] getEncryptionPaddings() {
        return ArrayUtils.cloneIfNotEmpty(this.mEncryptionPaddings);
    }

    public String[] getSignaturePaddings() {
        return ArrayUtils.cloneIfNotEmpty(this.mSignaturePaddings);
    }

    public String[] getBlockModes() {
        return ArrayUtils.cloneIfNotEmpty(this.mBlockModes);
    }

    public boolean isRandomizedEncryptionRequired() {
        return this.mRandomizedEncryptionRequired;
    }

    public boolean isUserAuthenticationRequired() {
        return this.mUserAuthenticationRequired;
    }

    public boolean isUserConfirmationRequired() {
        return this.mUserConfirmationRequired;
    }

    public int getUserAuthenticationValidityDurationSeconds() {
        return this.mUserAuthenticationValidityDurationSeconds;
    }

    public boolean isUserPresenceRequired() {
        return this.mUserPresenceRequired;
    }

    public byte[] getAttestationChallenge() {
        return ArrayUtils.cloneIfNotNull(this.mAttestationChallenge);
    }

    public boolean isUniqueIdIncluded() {
        return this.mUniqueIdIncluded;
    }

    public boolean isUserAuthenticationValidWhileOnBody() {
        return this.mUserAuthenticationValidWhileOnBody;
    }

    public boolean isInvalidatedByBiometricEnrollment() {
        return this.mInvalidatedByBiometricEnrollment;
    }

    public boolean isStrongBoxBacked() {
        return this.mIsStrongBoxBacked;
    }

    public boolean isUnlockedDeviceRequired() {
        return this.mUnlockedDeviceRequired;
    }

    public boolean isAdditionalProtectionAllowed() {
        return this.mIsAdditionalProtectionAllowed;
    }

    public long getBoundToSpecificSecureUserId() {
        return 0;
    }
}
