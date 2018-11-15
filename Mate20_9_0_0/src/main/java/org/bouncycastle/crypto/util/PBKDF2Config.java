package org.bouncycastle.crypto.util;

import java.util.HashMap;
import java.util.Map;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.cryptopro.CryptoProObjectIdentifiers;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.rosstandart.RosstandartObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.util.Integers;

public class PBKDF2Config extends PBKDFConfig {
    private static final Map PRFS_SALT = new HashMap();
    public static final AlgorithmIdentifier PRF_SHA1 = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_hmacWithSHA1, DERNull.INSTANCE);
    public static final AlgorithmIdentifier PRF_SHA256 = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_hmacWithSHA256, DERNull.INSTANCE);
    public static final AlgorithmIdentifier PRF_SHA3_256 = new AlgorithmIdentifier(NISTObjectIdentifiers.id_hmacWithSHA3_256, DERNull.INSTANCE);
    public static final AlgorithmIdentifier PRF_SHA3_512 = new AlgorithmIdentifier(NISTObjectIdentifiers.id_hmacWithSHA3_512, DERNull.INSTANCE);
    public static final AlgorithmIdentifier PRF_SHA512 = new AlgorithmIdentifier(PKCSObjectIdentifiers.id_hmacWithSHA512, DERNull.INSTANCE);
    private final int iterationCount;
    private final AlgorithmIdentifier prf;
    private final int saltLength;

    public static class Builder {
        private int iterationCount = 1024;
        private AlgorithmIdentifier prf = PBKDF2Config.PRF_SHA1;
        private int saltLength = -1;

        public PBKDF2Config build() {
            return new PBKDF2Config(this);
        }

        public Builder withIterationCount(int i) {
            this.iterationCount = i;
            return this;
        }

        public Builder withPRF(AlgorithmIdentifier algorithmIdentifier) {
            this.prf = algorithmIdentifier;
            return this;
        }

        public Builder withSaltLength(int i) {
            this.saltLength = i;
            return this;
        }
    }

    static {
        PRFS_SALT.put(PKCSObjectIdentifiers.id_hmacWithSHA1, Integers.valueOf(20));
        PRFS_SALT.put(PKCSObjectIdentifiers.id_hmacWithSHA256, Integers.valueOf(32));
        PRFS_SALT.put(PKCSObjectIdentifiers.id_hmacWithSHA512, Integers.valueOf(64));
        PRFS_SALT.put(PKCSObjectIdentifiers.id_hmacWithSHA224, Integers.valueOf(28));
        PRFS_SALT.put(PKCSObjectIdentifiers.id_hmacWithSHA384, Integers.valueOf(48));
        PRFS_SALT.put(NISTObjectIdentifiers.id_hmacWithSHA3_224, Integers.valueOf(28));
        PRFS_SALT.put(NISTObjectIdentifiers.id_hmacWithSHA3_256, Integers.valueOf(32));
        PRFS_SALT.put(NISTObjectIdentifiers.id_hmacWithSHA3_384, Integers.valueOf(48));
        PRFS_SALT.put(NISTObjectIdentifiers.id_hmacWithSHA3_512, Integers.valueOf(64));
        PRFS_SALT.put(CryptoProObjectIdentifiers.gostR3411Hmac, Integers.valueOf(32));
        PRFS_SALT.put(RosstandartObjectIdentifiers.id_tc26_hmac_gost_3411_12_256, Integers.valueOf(32));
        PRFS_SALT.put(RosstandartObjectIdentifiers.id_tc26_hmac_gost_3411_12_512, Integers.valueOf(64));
        PRFS_SALT.put(GMObjectIdentifiers.hmac_sm3, Integers.valueOf(32));
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:6:0x0029 in {2, 4, 5} preds:[]
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.computeDominators(BlockProcessor.java:238)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.processBlocksTree(BlockProcessor.java:48)
        	at jadx.core.dex.visitors.blocksmaker.BlockProcessor.visit(BlockProcessor.java:38)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:27)
        	at jadx.core.dex.visitors.DepthTraversal.lambda$visit$1(DepthTraversal.java:14)
        	at java.util.ArrayList.forEach(ArrayList.java:1249)
        	at jadx.core.dex.visitors.DepthTraversal.visit(DepthTraversal.java:14)
        	at jadx.core.ProcessClass.process(ProcessClass.java:32)
        	at jadx.core.ProcessClass.lambda$processDependencies$0(ProcessClass.java:51)
        	at java.lang.Iterable.forEach(Iterable.java:75)
        	at jadx.core.ProcessClass.processDependencies(ProcessClass.java:51)
        	at jadx.core.ProcessClass.process(ProcessClass.java:37)
        	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
        	at jadx.api.JavaClass.decompile(JavaClass.java:62)
        	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
        */
    private PBKDF2Config(org.bouncycastle.crypto.util.PBKDF2Config.Builder r2) {
        /*
        r1 = this;
        r0 = org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers.id_PBKDF2;
        r1.<init>(r0);
        r0 = r2.iterationCount;
        r1.iterationCount = r0;
        r0 = r2.prf;
        r1.prf = r0;
        r0 = r2.saltLength;
        if (r0 >= 0) goto L_0x0024;
    L_0x0017:
        r2 = r1.prf;
        r2 = r2.getAlgorithm();
        r2 = getSaltSize(r2);
    L_0x0021:
        r1.saltLength = r2;
        return;
    L_0x0024:
        r2 = r2.saltLength;
        goto L_0x0021;
        return;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.crypto.util.PBKDF2Config.<init>(org.bouncycastle.crypto.util.PBKDF2Config$Builder):void");
    }

    static int getSaltSize(ASN1ObjectIdentifier aSN1ObjectIdentifier) {
        if (PRFS_SALT.containsKey(aSN1ObjectIdentifier)) {
            return ((Integer) PRFS_SALT.get(aSN1ObjectIdentifier)).intValue();
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("no salt size for algorithm: ");
        stringBuilder.append(aSN1ObjectIdentifier);
        throw new IllegalStateException(stringBuilder.toString());
    }

    public int getIterationCount() {
        return this.iterationCount;
    }

    public AlgorithmIdentifier getPRF() {
        return this.prf;
    }

    public int getSaltLength() {
        return this.saltLength;
    }
}
