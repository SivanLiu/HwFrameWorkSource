package org.bouncycastle.math.ec;

import java.math.BigInteger;
import org.bouncycastle.math.ec.ECCurve.AbstractF2m;

class Tnaf {
    private static final BigInteger MINUS_ONE = ECConstants.ONE.negate();
    private static final BigInteger MINUS_THREE = ECConstants.THREE.negate();
    private static final BigInteger MINUS_TWO = ECConstants.TWO.negate();
    public static final byte POW_2_WIDTH = (byte) 16;
    public static final byte WIDTH = (byte) 4;
    public static final ZTauElement[] alpha0 = new ZTauElement[]{null, new ZTauElement(ECConstants.ONE, ECConstants.ZERO), null, new ZTauElement(MINUS_THREE, MINUS_ONE), null, new ZTauElement(MINUS_ONE, MINUS_ONE), null, new ZTauElement(ECConstants.ONE, MINUS_ONE), null};
    public static final byte[][] alpha0Tnaf;
    public static final ZTauElement[] alpha1 = new ZTauElement[]{null, new ZTauElement(ECConstants.ONE, ECConstants.ZERO), null, new ZTauElement(MINUS_THREE, ECConstants.ONE), null, new ZTauElement(MINUS_ONE, ECConstants.ONE), null, new ZTauElement(ECConstants.ONE, ECConstants.ONE), null};
    public static final byte[][] alpha1Tnaf;

    static {
        r1 = new byte[8][];
        r1[1] = new byte[]{(byte) 1};
        r1[2] = null;
        r1[3] = new byte[]{(byte) -1, (byte) 0, (byte) 1};
        r1[4] = null;
        r1[5] = new byte[]{(byte) 1, (byte) 0, (byte) 1};
        r1[6] = null;
        r1[7] = new byte[]{(byte) -1, (byte) 0, (byte) 0, (byte) 1};
        alpha0Tnaf = r1;
        r0 = new byte[8][];
        r0[1] = new byte[]{(byte) 1};
        r0[2] = null;
        r0[3] = new byte[]{(byte) -1, (byte) 0, (byte) 1};
        r0[4] = null;
        r0[5] = new byte[]{(byte) 1, (byte) 0, (byte) 1};
        r0[6] = null;
        r0[7] = new byte[]{(byte) -1, (byte) 0, (byte) 0, (byte) -1};
        alpha1Tnaf = r0;
    }

    Tnaf() {
    }

    public static SimpleBigDecimal approximateDivisionByN(BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3, byte b, int i, int i2) {
        int i3 = ((i + 5) / 2) + i2;
        bigInteger = bigInteger2.multiply(bigInteger.shiftRight(((i - i3) - 2) + b));
        bigInteger = bigInteger.add(bigInteger3.multiply(bigInteger.shiftRight(i)));
        i3 -= i2;
        bigInteger2 = bigInteger.shiftRight(i3);
        if (bigInteger.testBit(i3 - 1)) {
            bigInteger2 = bigInteger2.add(ECConstants.ONE);
        }
        return new SimpleBigDecimal(bigInteger2, i2);
    }

    public static BigInteger[] getLucas(byte b, int i, boolean z) {
        if (b == (byte) 1 || b == (byte) -1) {
            BigInteger bigInteger;
            BigInteger valueOf;
            if (z) {
                bigInteger = ECConstants.TWO;
                valueOf = BigInteger.valueOf((long) b);
            } else {
                bigInteger = ECConstants.ZERO;
                valueOf = ECConstants.ONE;
            }
            BigInteger bigInteger2 = valueOf;
            valueOf = bigInteger;
            int i2 = 1;
            while (i2 < i) {
                i2++;
                BigInteger bigInteger3 = bigInteger2;
                bigInteger2 = (b == (byte) 1 ? bigInteger2 : bigInteger2.negate()).subtract(valueOf.shiftLeft(1));
                valueOf = bigInteger3;
            }
            return new BigInteger[]{valueOf, bigInteger2};
        }
        throw new IllegalArgumentException("mu must be 1 or -1");
    }

    public static byte getMu(int i) {
        return (byte) (i == 0 ? -1 : 1);
    }

    public static byte getMu(AbstractF2m abstractF2m) {
        if (abstractF2m.isKoblitz()) {
            return abstractF2m.getA().isZero() ? (byte) -1 : (byte) 1;
        } else {
            throw new IllegalArgumentException("No Koblitz curve (ABC), TNAF multiplication not possible");
        }
    }

    public static byte getMu(ECFieldElement eCFieldElement) {
        return (byte) (eCFieldElement.isZero() ? -1 : 1);
    }

    public static ECPoint.AbstractF2m[] getPreComp(ECPoint.AbstractF2m abstractF2m, byte b) {
        byte[][] bArr = b == (byte) 0 ? alpha0Tnaf : alpha1Tnaf;
        ECPoint[] eCPointArr = new ECPoint.AbstractF2m[((bArr.length + 1) >>> 1)];
        eCPointArr[0] = abstractF2m;
        int length = bArr.length;
        for (int i = 3; i < length; i += 2) {
            eCPointArr[i >>> 1] = multiplyFromTnaf(abstractF2m, bArr[i]);
        }
        abstractF2m.getCurve().normalizeAll(eCPointArr);
        return eCPointArr;
    }

    protected static int getShiftsForCofactor(BigInteger bigInteger) {
        if (bigInteger != null) {
            if (bigInteger.equals(ECConstants.TWO)) {
                return 1;
            }
            if (bigInteger.equals(ECConstants.FOUR)) {
                return 2;
            }
        }
        throw new IllegalArgumentException("h (Cofactor) must be 2 or 4");
    }

    public static BigInteger[] getSi(int i, int i2, BigInteger bigInteger) {
        byte mu = getMu(i2);
        int shiftsForCofactor = getShiftsForCofactor(bigInteger);
        BigInteger[] lucas = getLucas(mu, (i + 3) - i2, false);
        if (mu == (byte) 1) {
            lucas[0] = lucas[0].negate();
            lucas[1] = lucas[1].negate();
        }
        BigInteger shiftRight = ECConstants.ONE.add(lucas[1]).shiftRight(shiftsForCofactor);
        BigInteger negate = ECConstants.ONE.add(lucas[0]).shiftRight(shiftsForCofactor).negate();
        return new BigInteger[]{shiftRight, negate};
    }

    public static BigInteger[] getSi(AbstractF2m abstractF2m) {
        if (abstractF2m.isKoblitz()) {
            int fieldSize = abstractF2m.getFieldSize();
            int intValue = abstractF2m.getA().toBigInteger().intValue();
            byte mu = getMu(intValue);
            int shiftsForCofactor = getShiftsForCofactor(abstractF2m.getCofactor());
            BigInteger[] lucas = getLucas(mu, (fieldSize + 3) - intValue, false);
            if (mu == (byte) 1) {
                lucas[0] = lucas[0].negate();
                lucas[1] = lucas[1].negate();
            }
            BigInteger shiftRight = ECConstants.ONE.add(lucas[1]).shiftRight(shiftsForCofactor);
            BigInteger negate = ECConstants.ONE.add(lucas[0]).shiftRight(shiftsForCofactor).negate();
            return new BigInteger[]{shiftRight, negate};
        }
        throw new IllegalArgumentException("si is defined for Koblitz curves only");
    }

    public static BigInteger getTw(byte b, int i) {
        if (i == 4) {
            return b == (byte) 1 ? BigInteger.valueOf(6) : BigInteger.valueOf(10);
        } else {
            BigInteger[] lucas = getLucas(b, i, false);
            BigInteger bit = ECConstants.ZERO.setBit(i);
            return ECConstants.TWO.multiply(lucas[0]).multiply(lucas[1].modInverse(bit)).mod(bit);
        }
    }

    public static ECPoint.AbstractF2m multiplyFromTnaf(ECPoint.AbstractF2m abstractF2m, byte[] bArr) {
        ECPoint eCPoint = (ECPoint.AbstractF2m) abstractF2m.negate();
        ECPoint.AbstractF2m abstractF2m2 = (ECPoint.AbstractF2m) abstractF2m.getCurve().getInfinity();
        int i = 0;
        for (int length = bArr.length - 1; length >= 0; length--) {
            i++;
            byte b = bArr[length];
            if (b != (byte) 0) {
                abstractF2m2 = (ECPoint.AbstractF2m) abstractF2m2.tauPow(i).add(b > (byte) 0 ? abstractF2m : eCPoint);
                i = 0;
            }
        }
        return i > 0 ? abstractF2m2.tauPow(i) : abstractF2m2;
    }

    public static ECPoint.AbstractF2m multiplyRTnaf(ECPoint.AbstractF2m abstractF2m, BigInteger bigInteger) {
        AbstractF2m abstractF2m2 = (AbstractF2m) abstractF2m.getCurve();
        int fieldSize = abstractF2m2.getFieldSize();
        int intValue = abstractF2m2.getA().toBigInteger().intValue();
        byte mu = getMu(intValue);
        byte b = (byte) intValue;
        return multiplyTnaf(abstractF2m, partModReduction(bigInteger, fieldSize, b, abstractF2m2.getSi(), mu, (byte) 10));
    }

    public static ECPoint.AbstractF2m multiplyTnaf(ECPoint.AbstractF2m abstractF2m, ZTauElement zTauElement) {
        return multiplyFromTnaf(abstractF2m, tauAdicNaf(getMu(((AbstractF2m) abstractF2m.getCurve()).getA()), zTauElement));
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x0030 in {2, 4, 7, 10} preds:[]
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
    public static java.math.BigInteger norm(byte r3, org.bouncycastle.math.ec.ZTauElement r4) {
        /*
        r0 = r4.u;
        r1 = r4.u;
        r0 = r0.multiply(r1);
        r1 = r4.u;
        r2 = r4.v;
        r1 = r1.multiply(r2);
        r2 = r4.v;
        r4 = r4.v;
        r4 = r2.multiply(r4);
        r2 = 1;
        r4 = r4.shiftLeft(r2);
        if (r3 != r2) goto L_0x0028;
    L_0x001f:
        r3 = r0.add(r1);
    L_0x0023:
        r3 = r3.add(r4);
        return r3;
    L_0x0028:
        r2 = -1;
        if (r3 != r2) goto L_0x0031;
    L_0x002b:
        r3 = r0.subtract(r1);
        goto L_0x0023;
        return r3;
    L_0x0031:
        r3 = new java.lang.IllegalArgumentException;
        r4 = "mu must be 1 or -1";
        r3.<init>(r4);
        throw r3;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.math.ec.Tnaf.norm(byte, org.bouncycastle.math.ec.ZTauElement):java.math.BigInteger");
    }

    /*  JADX ERROR: JadxRuntimeException in pass: BlockProcessor
        jadx.core.utils.exceptions.JadxRuntimeException: Can't find immediate dominator for block B:8:0x0024 in {2, 4, 7, 10} preds:[]
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
    public static org.bouncycastle.math.ec.SimpleBigDecimal norm(byte r2, org.bouncycastle.math.ec.SimpleBigDecimal r3, org.bouncycastle.math.ec.SimpleBigDecimal r4) {
        /*
        r0 = r3.multiply(r3);
        r3 = r3.multiply(r4);
        r4 = r4.multiply(r4);
        r1 = 1;
        r4 = r4.shiftLeft(r1);
        if (r2 != r1) goto L_0x001c;
    L_0x0013:
        r2 = r0.add(r3);
    L_0x0017:
        r2 = r2.add(r4);
        return r2;
    L_0x001c:
        r1 = -1;
        if (r2 != r1) goto L_0x0025;
    L_0x001f:
        r2 = r0.subtract(r3);
        goto L_0x0017;
        return r2;
    L_0x0025:
        r2 = new java.lang.IllegalArgumentException;
        r3 = "mu must be 1 or -1";
        r2.<init>(r3);
        throw r2;
        */
        throw new UnsupportedOperationException("Method not decompiled: org.bouncycastle.math.ec.Tnaf.norm(byte, org.bouncycastle.math.ec.SimpleBigDecimal, org.bouncycastle.math.ec.SimpleBigDecimal):org.bouncycastle.math.ec.SimpleBigDecimal");
    }

    public static ZTauElement partModReduction(BigInteger bigInteger, int i, byte b, BigInteger[] bigIntegerArr, byte b2, byte b3) {
        byte b4 = b2;
        BigInteger add = b4 == (byte) 1 ? bigIntegerArr[0].add(bigIntegerArr[1]) : bigIntegerArr[0].subtract(bigIntegerArr[1]);
        int i2 = i;
        BigInteger bigInteger2 = bigInteger;
        BigInteger bigInteger3 = getLucas(b4, i2, true)[1];
        byte b5 = b;
        int i3 = i2;
        byte b6 = b3;
        ZTauElement round = round(approximateDivisionByN(bigInteger2, bigIntegerArr[0], bigInteger3, b5, i3, b6), approximateDivisionByN(bigInteger2, bigIntegerArr[1], bigInteger3, b5, i3, b6), b4);
        return new ZTauElement(bigInteger2.subtract(add.multiply(round.u)).subtract(BigInteger.valueOf(2).multiply(bigIntegerArr[1]).multiply(round.v)), bigIntegerArr[1].multiply(round.u).subtract(bigIntegerArr[0].multiply(round.v)));
    }

    /* JADX WARNING: Removed duplicated region for block: B:30:0x008c  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x007f  */
    /* JADX WARNING: Removed duplicated region for block: B:26:0x007f  */
    /* JADX WARNING: Removed duplicated region for block: B:30:0x008c  */
    /* JADX WARNING: Missing block: B:31:0x0092, code:
            if (r7.compareTo(MINUS_TWO) < 0) goto L_0x0087;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static ZTauElement round(SimpleBigDecimal simpleBigDecimal, SimpleBigDecimal simpleBigDecimal2, byte b) {
        if (simpleBigDecimal2.getScale() != simpleBigDecimal.getScale()) {
            throw new IllegalArgumentException("lambda0 and lambda1 do not have same scale");
        } else if (b == (byte) 1 || b == (byte) -1) {
            int i;
            BigInteger round = simpleBigDecimal.round();
            BigInteger round2 = simpleBigDecimal2.round();
            simpleBigDecimal = simpleBigDecimal.subtract(round);
            simpleBigDecimal2 = simpleBigDecimal2.subtract(round2);
            SimpleBigDecimal add = simpleBigDecimal.add(simpleBigDecimal);
            add = b == (byte) 1 ? add.add(simpleBigDecimal2) : add.subtract(simpleBigDecimal2);
            SimpleBigDecimal add2 = simpleBigDecimal2.add(simpleBigDecimal2).add(simpleBigDecimal2);
            simpleBigDecimal2 = add2.add(simpleBigDecimal2);
            if (b == (byte) 1) {
                add2 = simpleBigDecimal.subtract(add2);
                simpleBigDecimal = simpleBigDecimal.add(simpleBigDecimal2);
            } else {
                add2 = simpleBigDecimal.add(add2);
                simpleBigDecimal = simpleBigDecimal.subtract(simpleBigDecimal2);
            }
            int i2 = 0;
            if (add.compareTo(ECConstants.ONE) >= 0) {
                if (add2.compareTo(MINUS_ONE) >= 0) {
                    i = 0;
                    i2 = 1;
                    if (add.compareTo(MINUS_ONE) < 0) {
                        if (add2.compareTo(ECConstants.ONE) < 0) {
                            i2 = -1;
                            return new ZTauElement(round.add(BigInteger.valueOf((long) i2)), round2.add(BigInteger.valueOf((long) i)));
                        }
                    }
                    i = (byte) (-b);
                    return new ZTauElement(round.add(BigInteger.valueOf((long) i2)), round2.add(BigInteger.valueOf((long) i)));
                }
            } else if (simpleBigDecimal.compareTo(ECConstants.TWO) < 0) {
                i = 0;
                if (add.compareTo(MINUS_ONE) < 0) {
                }
                i = (byte) (-b);
                return new ZTauElement(round.add(BigInteger.valueOf((long) i2)), round2.add(BigInteger.valueOf((long) i)));
            }
            i = b;
            if (add.compareTo(MINUS_ONE) < 0) {
            }
            i = (byte) (-b);
            return new ZTauElement(round.add(BigInteger.valueOf((long) i2)), round2.add(BigInteger.valueOf((long) i)));
        } else {
            throw new IllegalArgumentException("mu must be 1 or -1");
        }
    }

    public static ECPoint.AbstractF2m tau(ECPoint.AbstractF2m abstractF2m) {
        return abstractF2m.tau();
    }

    public static byte[] tauAdicNaf(byte b, ZTauElement zTauElement) {
        if (b == (byte) 1 || b == (byte) -1) {
            int bitLength = norm(b, zTauElement).bitLength();
            Object obj = new byte[(bitLength > 30 ? bitLength + 4 : 34)];
            BigInteger bigInteger = zTauElement.u;
            BigInteger bigInteger2 = zTauElement.v;
            int i = 0;
            int i2 = i;
            while (true) {
                if (bigInteger.equals(ECConstants.ZERO) && bigInteger2.equals(ECConstants.ZERO)) {
                    i++;
                    Object obj2 = new byte[i];
                    System.arraycopy(obj, 0, obj2, 0, i);
                    return obj2;
                }
                if (bigInteger.testBit(0)) {
                    obj[i2] = (byte) ECConstants.TWO.subtract(bigInteger.subtract(bigInteger2.shiftLeft(1)).mod(ECConstants.FOUR)).intValue();
                    bigInteger = obj[i2] == (byte) 1 ? bigInteger.clearBit(0) : bigInteger.add(ECConstants.ONE);
                    i = i2;
                } else {
                    obj[i2] = null;
                }
                BigInteger shiftRight = bigInteger.shiftRight(1);
                bigInteger2 = b == (byte) 1 ? bigInteger2.add(shiftRight) : bigInteger2.subtract(shiftRight);
                i2++;
                BigInteger negate = bigInteger.shiftRight(1).negate();
                bigInteger = bigInteger2;
                bigInteger2 = negate;
            }
        } else {
            throw new IllegalArgumentException("mu must be 1 or -1");
        }
    }

    public static byte[] tauAdicWNaf(byte b, ZTauElement zTauElement, byte b2, BigInteger bigInteger, BigInteger bigInteger2, ZTauElement[] zTauElementArr) {
        if (b == (byte) 1 || b == (byte) -1) {
            int bitLength = norm(b, zTauElement).bitLength();
            byte[] bArr = new byte[((bitLength > 30 ? bitLength + 4 : 34) + b2)];
            BigInteger shiftRight = bigInteger.shiftRight(1);
            BigInteger bigInteger3 = zTauElement.u;
            BigInteger bigInteger4 = zTauElement.v;
            int i = 0;
            while (true) {
                if (bigInteger3.equals(ECConstants.ZERO) && bigInteger4.equals(ECConstants.ZERO)) {
                    return bArr;
                }
                if (bigInteger3.testBit(0)) {
                    int i2;
                    BigInteger mod = bigInteger3.add(bigInteger4.multiply(bigInteger2)).mod(bigInteger);
                    if (mod.compareTo(shiftRight) >= 0) {
                        mod = mod.subtract(bigInteger);
                    }
                    byte intValue = (byte) mod.intValue();
                    bArr[i] = intValue;
                    if (intValue < (byte) 0) {
                        i2 = (byte) (-intValue);
                        intValue = (byte) 0;
                    } else {
                        i2 = intValue;
                        intValue = (byte) 1;
                    }
                    if (intValue != (byte) 0) {
                        bigInteger3 = bigInteger3.subtract(zTauElementArr[i2].u);
                        bigInteger4 = bigInteger4.subtract(zTauElementArr[i2].v);
                    } else {
                        bigInteger3 = bigInteger3.add(zTauElementArr[i2].u);
                        bigInteger4 = bigInteger4.add(zTauElementArr[i2].v);
                    }
                } else {
                    bArr[i] = (byte) 0;
                }
                bigInteger4 = b == (byte) 1 ? bigInteger4.add(bigInteger3.shiftRight(1)) : bigInteger4.subtract(bigInteger3.shiftRight(1));
                i++;
                BigInteger negate = bigInteger3.shiftRight(1).negate();
                bigInteger3 = bigInteger4;
                bigInteger4 = negate;
            }
        } else {
            throw new IllegalArgumentException("mu must be 1 or -1");
        }
    }
}
