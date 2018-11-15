package org.bouncycastle.math.ec;

import java.math.BigInteger;
import java.util.Random;
import org.bouncycastle.math.raw.Mod;
import org.bouncycastle.math.raw.Nat;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.BigIntegers;

public abstract class ECFieldElement implements ECConstants {

    public static class F2m extends ECFieldElement {
        public static final int GNB = 1;
        public static final int PPB = 3;
        public static final int TPB = 2;
        private int[] ks;
        private int m;
        private int representation;
        LongArray x;

        public F2m(int i, int i2, int i3, int i4, BigInteger bigInteger) {
            if (bigInteger == null || bigInteger.signum() < 0 || bigInteger.bitLength() > i) {
                throw new IllegalArgumentException("x value invalid in F2m field element");
            }
            if (i3 == 0 && i4 == 0) {
                this.representation = 2;
                this.ks = new int[]{i2};
            } else if (i3 >= i4) {
                throw new IllegalArgumentException("k2 must be smaller than k3");
            } else if (i3 > 0) {
                this.representation = 3;
                this.ks = new int[]{i2, i3, i4};
            } else {
                throw new IllegalArgumentException("k2 must be larger than 0");
            }
            this.m = i;
            this.x = new LongArray(bigInteger);
        }

        public F2m(int i, int i2, BigInteger bigInteger) {
            this(i, i2, 0, 0, bigInteger);
        }

        F2m(int i, int[] iArr, LongArray longArray) {
            this.m = i;
            this.representation = iArr.length == 1 ? 2 : 3;
            this.ks = iArr;
            this.x = longArray;
        }

        public static void checkFieldElements(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            if ((eCFieldElement instanceof F2m) && (eCFieldElement2 instanceof F2m)) {
                F2m f2m = (F2m) eCFieldElement;
                F2m f2m2 = (F2m) eCFieldElement2;
                if (f2m.representation != f2m2.representation) {
                    throw new IllegalArgumentException("One of the F2m field elements has incorrect representation");
                } else if (f2m.m != f2m2.m || !Arrays.areEqual(f2m.ks, f2m2.ks)) {
                    throw new IllegalArgumentException("Field elements are not elements of the same field F2m");
                } else {
                    return;
                }
            }
            throw new IllegalArgumentException("Field elements are not both instances of ECFieldElement.F2m");
        }

        public ECFieldElement add(ECFieldElement eCFieldElement) {
            LongArray longArray = (LongArray) this.x.clone();
            longArray.addShiftedByWords(((F2m) eCFieldElement).x, 0);
            return new F2m(this.m, this.ks, longArray);
        }

        public ECFieldElement addOne() {
            return new F2m(this.m, this.ks, this.x.addOne());
        }

        public int bitLength() {
            return this.x.degree();
        }

        public ECFieldElement divide(ECFieldElement eCFieldElement) {
            return multiply(eCFieldElement.invert());
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof F2m)) {
                return false;
            }
            F2m f2m = (F2m) obj;
            return this.m == f2m.m && this.representation == f2m.representation && Arrays.areEqual(this.ks, f2m.ks) && this.x.equals(f2m.x);
        }

        public String getFieldName() {
            return "F2m";
        }

        public int getFieldSize() {
            return this.m;
        }

        public int getK1() {
            return this.ks[0];
        }

        public int getK2() {
            return this.ks.length >= 2 ? this.ks[1] : 0;
        }

        public int getK3() {
            return this.ks.length >= 3 ? this.ks[2] : 0;
        }

        public int getM() {
            return this.m;
        }

        public int getRepresentation() {
            return this.representation;
        }

        public int hashCode() {
            return (this.x.hashCode() ^ this.m) ^ Arrays.hashCode(this.ks);
        }

        public ECFieldElement invert() {
            return new F2m(this.m, this.ks, this.x.modInverse(this.m, this.ks));
        }

        public boolean isOne() {
            return this.x.isOne();
        }

        public boolean isZero() {
            return this.x.isZero();
        }

        public ECFieldElement multiply(ECFieldElement eCFieldElement) {
            return new F2m(this.m, this.ks, this.x.modMultiply(((F2m) eCFieldElement).x, this.m, this.ks));
        }

        public ECFieldElement multiplyMinusProduct(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement eCFieldElement3) {
            return multiplyPlusProduct(eCFieldElement, eCFieldElement2, eCFieldElement3);
        }

        public ECFieldElement multiplyPlusProduct(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement eCFieldElement3) {
            LongArray longArray = this.x;
            LongArray longArray2 = ((F2m) eCFieldElement).x;
            LongArray longArray3 = ((F2m) eCFieldElement2).x;
            LongArray longArray4 = ((F2m) eCFieldElement3).x;
            LongArray multiply = longArray.multiply(longArray2, this.m, this.ks);
            longArray3 = longArray3.multiply(longArray4, this.m, this.ks);
            if (multiply == longArray || multiply == longArray2) {
                multiply = (LongArray) multiply.clone();
            }
            multiply.addShiftedByWords(longArray3, 0);
            multiply.reduce(this.m, this.ks);
            return new F2m(this.m, this.ks, multiply);
        }

        public ECFieldElement negate() {
            return this;
        }

        public ECFieldElement sqrt() {
            return (this.x.isZero() || this.x.isOne()) ? this : squarePow(this.m - 1);
        }

        public ECFieldElement square() {
            return new F2m(this.m, this.ks, this.x.modSquare(this.m, this.ks));
        }

        public ECFieldElement squareMinusProduct(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            return squarePlusProduct(eCFieldElement, eCFieldElement2);
        }

        public ECFieldElement squarePlusProduct(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            LongArray longArray = this.x;
            LongArray longArray2 = ((F2m) eCFieldElement).x;
            LongArray longArray3 = ((F2m) eCFieldElement2).x;
            LongArray square = longArray.square(this.m, this.ks);
            longArray2 = longArray2.multiply(longArray3, this.m, this.ks);
            if (square == longArray) {
                square = (LongArray) square.clone();
            }
            square.addShiftedByWords(longArray2, 0);
            square.reduce(this.m, this.ks);
            return new F2m(this.m, this.ks, square);
        }

        public ECFieldElement squarePow(int i) {
            return i < 1 ? this : new F2m(this.m, this.ks, this.x.modSquareN(i, this.m, this.ks));
        }

        public ECFieldElement subtract(ECFieldElement eCFieldElement) {
            return add(eCFieldElement);
        }

        public boolean testBitZero() {
            return this.x.testBitZero();
        }

        public BigInteger toBigInteger() {
            return this.x.toBigInteger();
        }
    }

    public static class Fp extends ECFieldElement {
        BigInteger q;
        BigInteger r;
        BigInteger x;

        public Fp(BigInteger bigInteger, BigInteger bigInteger2) {
            this(bigInteger, calculateResidue(bigInteger), bigInteger2);
        }

        Fp(BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3) {
            if (bigInteger3 == null || bigInteger3.signum() < 0 || bigInteger3.compareTo(bigInteger) >= 0) {
                throw new IllegalArgumentException("x value invalid in Fp field element");
            }
            this.q = bigInteger;
            this.r = bigInteger2;
            this.x = bigInteger3;
        }

        static BigInteger calculateResidue(BigInteger bigInteger) {
            int bitLength = bigInteger.bitLength();
            return (bitLength < 96 || bigInteger.shiftRight(bitLength - 64).longValue() != -1) ? null : ONE.shiftLeft(bitLength).subtract(bigInteger);
        }

        private ECFieldElement checkSqrt(ECFieldElement eCFieldElement) {
            return eCFieldElement.square().equals(this) ? eCFieldElement : null;
        }

        private BigInteger[] lucasSequence(BigInteger bigInteger, BigInteger bigInteger2, BigInteger bigInteger3) {
            int bitLength = bigInteger3.bitLength();
            int lowestSetBit = bigInteger3.getLowestSetBit();
            BigInteger bigInteger4 = ECConstants.ONE;
            BigInteger bigInteger5 = ECConstants.TWO;
            BigInteger bigInteger6 = ECConstants.ONE;
            BigInteger bigInteger7 = ECConstants.ONE;
            BigInteger bigInteger8 = bigInteger;
            for (bitLength--; bitLength >= lowestSetBit + 1; bitLength--) {
                bigInteger6 = modMult(bigInteger6, bigInteger7);
                if (bigInteger3.testBit(bitLength)) {
                    bigInteger7 = modMult(bigInteger6, bigInteger2);
                    bigInteger4 = modMult(bigInteger4, bigInteger8);
                    bigInteger5 = modReduce(bigInteger8.multiply(bigInteger5).subtract(bigInteger.multiply(bigInteger6)));
                    bigInteger8 = modReduce(bigInteger8.multiply(bigInteger8).subtract(bigInteger7.shiftLeft(1)));
                } else {
                    bigInteger4 = modReduce(bigInteger4.multiply(bigInteger5).subtract(bigInteger6));
                    bigInteger7 = modReduce(bigInteger8.multiply(bigInteger5).subtract(bigInteger.multiply(bigInteger6)));
                    bigInteger5 = modReduce(bigInteger5.multiply(bigInteger5).subtract(bigInteger6.shiftLeft(1)));
                    bigInteger8 = bigInteger7;
                    bigInteger7 = bigInteger6;
                }
            }
            bigInteger3 = modMult(bigInteger6, bigInteger7);
            bigInteger2 = modMult(bigInteger3, bigInteger2);
            BigInteger modReduce = modReduce(bigInteger4.multiply(bigInteger5).subtract(bigInteger3));
            bigInteger = modReduce(bigInteger8.multiply(bigInteger5).subtract(bigInteger.multiply(bigInteger3)));
            bigInteger3 = modMult(bigInteger3, bigInteger2);
            bigInteger2 = bigInteger;
            for (int i = 1; i <= lowestSetBit; i++) {
                modReduce = modMult(modReduce, bigInteger2);
                bigInteger2 = modReduce(bigInteger2.multiply(bigInteger2).subtract(bigInteger3.shiftLeft(1)));
                bigInteger3 = modMult(bigInteger3, bigInteger3);
            }
            return new BigInteger[]{modReduce, bigInteger2};
        }

        public ECFieldElement add(ECFieldElement eCFieldElement) {
            return new Fp(this.q, this.r, modAdd(this.x, eCFieldElement.toBigInteger()));
        }

        public ECFieldElement addOne() {
            BigInteger add = this.x.add(ECConstants.ONE);
            if (add.compareTo(this.q) == 0) {
                add = ECConstants.ZERO;
            }
            return new Fp(this.q, this.r, add);
        }

        public ECFieldElement divide(ECFieldElement eCFieldElement) {
            return new Fp(this.q, this.r, modMult(this.x, modInverse(eCFieldElement.toBigInteger())));
        }

        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (!(obj instanceof Fp)) {
                return false;
            }
            Fp fp = (Fp) obj;
            return this.q.equals(fp.q) && this.x.equals(fp.x);
        }

        public String getFieldName() {
            return "Fp";
        }

        public int getFieldSize() {
            return this.q.bitLength();
        }

        public BigInteger getQ() {
            return this.q;
        }

        public int hashCode() {
            return this.q.hashCode() ^ this.x.hashCode();
        }

        public ECFieldElement invert() {
            return new Fp(this.q, this.r, modInverse(this.x));
        }

        protected BigInteger modAdd(BigInteger bigInteger, BigInteger bigInteger2) {
            bigInteger = bigInteger.add(bigInteger2);
            return bigInteger.compareTo(this.q) >= 0 ? bigInteger.subtract(this.q) : bigInteger;
        }

        protected BigInteger modDouble(BigInteger bigInteger) {
            bigInteger = bigInteger.shiftLeft(1);
            return bigInteger.compareTo(this.q) >= 0 ? bigInteger.subtract(this.q) : bigInteger;
        }

        protected BigInteger modHalf(BigInteger bigInteger) {
            if (bigInteger.testBit(0)) {
                bigInteger = this.q.add(bigInteger);
            }
            return bigInteger.shiftRight(1);
        }

        protected BigInteger modHalfAbs(BigInteger bigInteger) {
            if (bigInteger.testBit(0)) {
                bigInteger = this.q.subtract(bigInteger);
            }
            return bigInteger.shiftRight(1);
        }

        protected BigInteger modInverse(BigInteger bigInteger) {
            int fieldSize = getFieldSize();
            int i = (fieldSize + 31) >> 5;
            int[] fromBigInteger = Nat.fromBigInteger(fieldSize, this.q);
            int[] fromBigInteger2 = Nat.fromBigInteger(fieldSize, bigInteger);
            int[] create = Nat.create(i);
            Mod.invert(fromBigInteger, fromBigInteger2, create);
            return Nat.toBigInteger(i, create);
        }

        protected BigInteger modMult(BigInteger bigInteger, BigInteger bigInteger2) {
            return modReduce(bigInteger.multiply(bigInteger2));
        }

        protected BigInteger modReduce(BigInteger bigInteger) {
            if (this.r != null) {
                Object obj = bigInteger.signum() < 0 ? 1 : null;
                if (obj != null) {
                    bigInteger = bigInteger.abs();
                }
                int bitLength = this.q.bitLength();
                boolean equals = this.r.equals(ECConstants.ONE);
                while (bigInteger.bitLength() > bitLength + 1) {
                    BigInteger shiftRight = bigInteger.shiftRight(bitLength);
                    bigInteger = bigInteger.subtract(shiftRight.shiftLeft(bitLength));
                    if (!equals) {
                        shiftRight = shiftRight.multiply(this.r);
                    }
                    bigInteger = shiftRight.add(bigInteger);
                }
                while (bigInteger.compareTo(this.q) >= 0) {
                    bigInteger = bigInteger.subtract(this.q);
                }
                if (!(obj == null || bigInteger.signum() == 0)) {
                    return this.q.subtract(bigInteger);
                }
            }
            bigInteger = bigInteger.mod(this.q);
            return bigInteger;
        }

        protected BigInteger modSubtract(BigInteger bigInteger, BigInteger bigInteger2) {
            bigInteger = bigInteger.subtract(bigInteger2);
            return bigInteger.signum() < 0 ? bigInteger.add(this.q) : bigInteger;
        }

        public ECFieldElement multiply(ECFieldElement eCFieldElement) {
            return new Fp(this.q, this.r, modMult(this.x, eCFieldElement.toBigInteger()));
        }

        public ECFieldElement multiplyMinusProduct(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement eCFieldElement3) {
            BigInteger bigInteger = this.x;
            BigInteger toBigInteger = eCFieldElement.toBigInteger();
            BigInteger toBigInteger2 = eCFieldElement2.toBigInteger();
            BigInteger toBigInteger3 = eCFieldElement3.toBigInteger();
            return new Fp(this.q, this.r, modReduce(bigInteger.multiply(toBigInteger).subtract(toBigInteger2.multiply(toBigInteger3))));
        }

        public ECFieldElement multiplyPlusProduct(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement eCFieldElement3) {
            BigInteger bigInteger = this.x;
            BigInteger toBigInteger = eCFieldElement.toBigInteger();
            BigInteger toBigInteger2 = eCFieldElement2.toBigInteger();
            BigInteger toBigInteger3 = eCFieldElement3.toBigInteger();
            return new Fp(this.q, this.r, modReduce(bigInteger.multiply(toBigInteger).add(toBigInteger2.multiply(toBigInteger3))));
        }

        public ECFieldElement negate() {
            return this.x.signum() == 0 ? this : new Fp(this.q, this.r, this.q.subtract(this.x));
        }

        public ECFieldElement sqrt() {
            if (isZero() || isOne()) {
                return this;
            }
            BigInteger modPow;
            if (!this.q.testBit(0)) {
                throw new RuntimeException("not done yet");
            } else if (this.q.testBit(1)) {
                return checkSqrt(new Fp(this.q, this.r, this.x.modPow(this.q.shiftRight(2).add(ECConstants.ONE), this.q)));
            } else if (this.q.testBit(2)) {
                modPow = this.x.modPow(this.q.shiftRight(3), this.q);
                BigInteger modMult = modMult(modPow, this.x);
                if (modMult(modMult, modPow).equals(ECConstants.ONE)) {
                    return checkSqrt(new Fp(this.q, this.r, modMult));
                }
                return checkSqrt(new Fp(this.q, this.r, modMult(modMult, ECConstants.TWO.modPow(this.q.shiftRight(2), this.q))));
            } else {
                modPow = this.q.shiftRight(1);
                if (!this.x.modPow(modPow, this.q).equals(ECConstants.ONE)) {
                    return null;
                }
                BigInteger bigInteger = this.x;
                BigInteger modDouble = modDouble(modDouble(bigInteger));
                BigInteger add = modPow.add(ECConstants.ONE);
                BigInteger subtract = this.q.subtract(ECConstants.ONE);
                Random random = new Random();
                while (true) {
                    BigInteger bigInteger2 = new BigInteger(this.q.bitLength(), random);
                    if (bigInteger2.compareTo(this.q) < 0 && modReduce(bigInteger2.multiply(bigInteger2).subtract(modDouble)).modPow(modPow, this.q).equals(subtract)) {
                        BigInteger[] lucasSequence = lucasSequence(bigInteger2, bigInteger, add);
                        BigInteger bigInteger3 = lucasSequence[0];
                        bigInteger2 = lucasSequence[1];
                        if (modMult(bigInteger2, bigInteger2).equals(modDouble)) {
                            return new Fp(this.q, this.r, modHalfAbs(bigInteger2));
                        }
                        if (!(bigInteger3.equals(ECConstants.ONE) || bigInteger3.equals(subtract))) {
                            return null;
                        }
                    }
                }
            }
        }

        public ECFieldElement square() {
            return new Fp(this.q, this.r, modMult(this.x, this.x));
        }

        public ECFieldElement squareMinusProduct(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            BigInteger bigInteger = this.x;
            BigInteger toBigInteger = eCFieldElement.toBigInteger();
            BigInteger toBigInteger2 = eCFieldElement2.toBigInteger();
            return new Fp(this.q, this.r, modReduce(bigInteger.multiply(bigInteger).subtract(toBigInteger.multiply(toBigInteger2))));
        }

        public ECFieldElement squarePlusProduct(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
            BigInteger bigInteger = this.x;
            BigInteger toBigInteger = eCFieldElement.toBigInteger();
            BigInteger toBigInteger2 = eCFieldElement2.toBigInteger();
            return new Fp(this.q, this.r, modReduce(bigInteger.multiply(bigInteger).add(toBigInteger.multiply(toBigInteger2))));
        }

        public ECFieldElement subtract(ECFieldElement eCFieldElement) {
            return new Fp(this.q, this.r, modSubtract(this.x, eCFieldElement.toBigInteger()));
        }

        public BigInteger toBigInteger() {
            return this.x;
        }
    }

    public abstract ECFieldElement add(ECFieldElement eCFieldElement);

    public abstract ECFieldElement addOne();

    public int bitLength() {
        return toBigInteger().bitLength();
    }

    public abstract ECFieldElement divide(ECFieldElement eCFieldElement);

    public byte[] getEncoded() {
        return BigIntegers.asUnsignedByteArray((getFieldSize() + 7) / 8, toBigInteger());
    }

    public abstract String getFieldName();

    public abstract int getFieldSize();

    public abstract ECFieldElement invert();

    public boolean isOne() {
        return bitLength() == 1;
    }

    public boolean isZero() {
        return toBigInteger().signum() == 0;
    }

    public abstract ECFieldElement multiply(ECFieldElement eCFieldElement);

    public ECFieldElement multiplyMinusProduct(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement eCFieldElement3) {
        return multiply(eCFieldElement).subtract(eCFieldElement2.multiply(eCFieldElement3));
    }

    public ECFieldElement multiplyPlusProduct(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2, ECFieldElement eCFieldElement3) {
        return multiply(eCFieldElement).add(eCFieldElement2.multiply(eCFieldElement3));
    }

    public abstract ECFieldElement negate();

    public abstract ECFieldElement sqrt();

    public abstract ECFieldElement square();

    public ECFieldElement squareMinusProduct(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        return square().subtract(eCFieldElement.multiply(eCFieldElement2));
    }

    public ECFieldElement squarePlusProduct(ECFieldElement eCFieldElement, ECFieldElement eCFieldElement2) {
        return square().add(eCFieldElement.multiply(eCFieldElement2));
    }

    public ECFieldElement squarePow(int i) {
        ECFieldElement eCFieldElement = this;
        for (int i2 = 0; i2 < i; i2++) {
            eCFieldElement = eCFieldElement.square();
        }
        return eCFieldElement;
    }

    public abstract ECFieldElement subtract(ECFieldElement eCFieldElement);

    public boolean testBitZero() {
        return toBigInteger().testBit(0);
    }

    public abstract BigInteger toBigInteger();

    public String toString() {
        return toBigInteger().toString(16);
    }
}
