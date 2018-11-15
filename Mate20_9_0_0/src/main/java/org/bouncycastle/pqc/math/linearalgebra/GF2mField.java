package org.bouncycastle.pqc.math.linearalgebra;

import java.security.SecureRandom;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;

public class GF2mField {
    private int degree = 0;
    private int polynomial;

    public GF2mField(int i) {
        if (i >= 32) {
            throw new IllegalArgumentException(" Error: the degree of field is too large ");
        } else if (i >= 1) {
            this.degree = i;
            this.polynomial = PolynomialRingGF2.getIrreduciblePolynomial(i);
        } else {
            throw new IllegalArgumentException(" Error: the degree of field is non-positive ");
        }
    }

    public GF2mField(int i, int i2) {
        if (i != PolynomialRingGF2.degree(i2)) {
            throw new IllegalArgumentException(" Error: the degree is not correct");
        } else if (PolynomialRingGF2.isIrreducible(i2)) {
            this.degree = i;
            this.polynomial = i2;
        } else {
            throw new IllegalArgumentException(" Error: given polynomial is reducible");
        }
    }

    public GF2mField(GF2mField gF2mField) {
        this.degree = gF2mField.degree;
        this.polynomial = gF2mField.polynomial;
    }

    public GF2mField(byte[] bArr) {
        if (bArr.length == 4) {
            this.polynomial = LittleEndianConversions.OS2IP(bArr);
            if (PolynomialRingGF2.isIrreducible(this.polynomial)) {
                this.degree = PolynomialRingGF2.degree(this.polynomial);
                return;
            }
            throw new IllegalArgumentException("byte array is not an encoded finite field");
        }
        throw new IllegalArgumentException("byte array is not an encoded finite field");
    }

    private static String polyToString(int i) {
        String str = "";
        if (i == 0) {
            return "0";
        }
        if (((byte) (i & 1)) == (byte) 1) {
            str = "1";
        }
        i >>>= 1;
        int i2 = 1;
        while (i != 0) {
            if (((byte) (i & 1)) == (byte) 1) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append(str);
                stringBuilder.append("+x^");
                stringBuilder.append(i2);
                str = stringBuilder.toString();
            }
            i >>>= 1;
            i2++;
        }
        return str;
    }

    public int add(int i, int i2) {
        return i ^ i2;
    }

    public String elementToStr(int i) {
        String str = "";
        for (int i2 = 0; i2 < this.degree; i2++) {
            StringBuilder stringBuilder;
            String str2;
            if ((((byte) i) & 1) == 0) {
                stringBuilder = new StringBuilder();
                str2 = "0";
            } else {
                stringBuilder = new StringBuilder();
                str2 = "1";
            }
            stringBuilder.append(str2);
            stringBuilder.append(str);
            str = stringBuilder.toString();
            i >>>= 1;
        }
        return str;
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof GF2mField)) {
            return false;
        }
        GF2mField gF2mField = (GF2mField) obj;
        if (this.degree == gF2mField.degree && this.polynomial == gF2mField.polynomial) {
            return true;
        }
        return false;
    }

    public int exp(int i, int i2) {
        if (i2 == 0) {
            return 1;
        }
        if (i == 0) {
            return 0;
        }
        if (i == 1) {
            return 1;
        }
        if (i2 < 0) {
            i = inverse(i);
            i2 = -i2;
        }
        int i3 = i;
        i = 1;
        for (i2 = 
/*
Method generation error in method: org.bouncycastle.pqc.math.linearalgebra.GF2mField.exp(int, int):int, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r5_2 'i2' int) = (r5_0 'i2' int), (r5_1 'i2' int) binds: {(r5_0 'i2' int)=B:8:0x000b, (r5_1 'i2' int)=B:9:0x000d} in method: org.bouncycastle.pqc.math.linearalgebra.GF2mField.exp(int, int):int, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:183)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:61)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:87)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:53)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:173)
	at jadx.core.codegen.ClassGen.addMethod(ClassGen.java:321)
	at jadx.core.codegen.ClassGen.addMethods(ClassGen.java:259)
	at jadx.core.codegen.ClassGen.addClassBody(ClassGen.java:221)
	at jadx.core.codegen.ClassGen.addClassCode(ClassGen.java:111)
	at jadx.core.codegen.ClassGen.makeClass(ClassGen.java:77)
	at jadx.core.codegen.CodeGen.visit(CodeGen.java:10)
	at jadx.core.ProcessClass.process(ProcessClass.java:38)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:292)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:200)
Caused by: jadx.core.utils.exceptions.CodegenException: PHI can be used only in fallback mode
	at jadx.core.codegen.InsnGen.fallbackOnlyInsn(InsnGen.java:539)
	at jadx.core.codegen.InsnGen.makeInsnBody(InsnGen.java:511)
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:222)
	... 27 more

*/

    public int getDegree() {
        return this.degree;
    }

    public byte[] getEncoded() {
        return LittleEndianConversions.I2OSP(this.polynomial);
    }

    public int getPolynomial() {
        return this.polynomial;
    }

    public int getRandomElement(SecureRandom secureRandom) {
        return RandUtils.nextInt(secureRandom, 1 << this.degree);
    }

    public int getRandomNonZeroElement() {
        return getRandomNonZeroElement(new SecureRandom());
    }

    public int getRandomNonZeroElement(SecureRandom secureRandom) {
        int nextInt = RandUtils.nextInt(secureRandom, 1 << this.degree);
        int i = 0;
        while (nextInt == 0 && i < PKIFailureInfo.badCertTemplate) {
            nextInt = RandUtils.nextInt(secureRandom, 1 << this.degree);
            i++;
        }
        return i == PKIFailureInfo.badCertTemplate ? 1 : nextInt;
    }

    public int hashCode() {
        return this.polynomial;
    }

    public int inverse(int i) {
        return exp(i, (1 << this.degree) - 2);
    }

    public boolean isElementOfThisField(int i) {
        boolean z = false;
        if (this.degree == 31) {
            if (i >= 0) {
                z = true;
            }
            return z;
        }
        if (i >= 0 && i < (1 << this.degree)) {
            z = true;
        }
        return z;
    }

    public int mult(int i, int i2) {
        return PolynomialRingGF2.modMultiply(i, i2, this.polynomial);
    }

    public int sqRoot(int i) {
        for (int i2 = 1; i2 < this.degree; i2++) {
            i = mult(i, i);
        }
        return i;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Finite Field GF(2^");
        stringBuilder.append(this.degree);
        stringBuilder.append(") = GF(2)[X]/<");
        stringBuilder.append(polyToString(this.polynomial));
        stringBuilder.append("> ");
        return stringBuilder.toString();
    }
}
