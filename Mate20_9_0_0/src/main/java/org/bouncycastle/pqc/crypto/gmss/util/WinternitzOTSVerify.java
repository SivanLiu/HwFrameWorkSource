package org.bouncycastle.pqc.crypto.gmss.util;

import org.bouncycastle.crypto.Digest;

public class WinternitzOTSVerify {
    private Digest messDigestOTS;
    private int w;

    public WinternitzOTSVerify(Digest digest, int i) {
        this.w = i;
        this.messDigestOTS = digest;
    }

    public byte[] Verify(byte[] bArr, byte[] bArr2) {
        byte[] bArr3 = bArr;
        Object obj = bArr2;
        int digestSize = this.messDigestOTS.getDigestSize();
        byte[] bArr4 = new byte[digestSize];
        int i = 0;
        this.messDigestOTS.update(bArr3, 0, bArr3.length);
        bArr3 = new byte[this.messDigestOTS.getDigestSize()];
        this.messDigestOTS.doFinal(bArr3, 0);
        int i2 = digestSize << 3;
        int i3 = ((this.w - 1) + i2) / this.w;
        int log = getLog((i3 << this.w) + 1);
        int i4 = ((((this.w + log) - 1) / this.w) + i3) * digestSize;
        if (i4 != obj.length) {
            return null;
        }
        byte[] bArr5 = new byte[i4];
        int i5 = 8;
        int i6;
        Object obj2;
        int i7;
        int i8;
        int i9;
        int i10;
        int i11;
        int i12;
        if (8 % this.w == 0) {
            i5 = 8 / this.w;
            i2 = (1 << this.w) - 1;
            int i13 = 0;
            i6 = i13;
            obj2 = new byte[digestSize];
            i7 = i6;
            while (i7 < bArr3.length) {
                Object obj3 = obj2;
                int i14 = i6;
                i6 = i13;
                i13 = 0;
                while (i13 < i5) {
                    i8 = bArr3[i7] & i2;
                    i6 += i8;
                    i9 = i5;
                    i5 = i14 * digestSize;
                    System.arraycopy(obj, i5, obj3, 0, digestSize);
                    while (i8 < i2) {
                        i10 = i6;
                        this.messDigestOTS.update(obj3, 0, obj3.length);
                        obj3 = new byte[this.messDigestOTS.getDigestSize()];
                        this.messDigestOTS.doFinal(obj3, 0);
                        i8++;
                        i6 = i10;
                        byte[] bArr6 = bArr2;
                    }
                    i10 = i6;
                    System.arraycopy(obj3, 0, bArr5, i5, digestSize);
                    bArr3[i7] = (byte) (bArr3[i7] >>> this.w);
                    i14++;
                    i13++;
                    i5 = i9;
                    obj = bArr2;
                }
                i9 = i5;
                i7++;
                i13 = i6;
                i6 = i14;
                obj2 = obj3;
                obj = bArr2;
            }
            i11 = (i3 << this.w) - i13;
            for (i12 = 0; i12 < log; i12 += this.w) {
                i7 = i6 * digestSize;
                System.arraycopy(bArr2, i7, obj2, 0, digestSize);
                for (i3 = i11 & i2; i3 < i2; i3++) {
                    this.messDigestOTS.update(obj2, 0, obj2.length);
                    obj2 = new byte[this.messDigestOTS.getDigestSize()];
                    this.messDigestOTS.doFinal(obj2, 0);
                }
                System.arraycopy(obj2, 0, bArr5, i7, digestSize);
                i11 >>>= this.w;
                i6++;
            }
        } else {
            Object obj4 = obj;
            long j;
            if (this.w < 8) {
                int i15;
                i11 = digestSize / this.w;
                i2 = (1 << this.w) - 1;
                i8 = 0;
                i9 = i8;
                i10 = i9;
                Object obj5 = new byte[digestSize];
                int i16 = i10;
                while (i16 < i11) {
                    int i17 = i8;
                    long j2 = 0;
                    for (i8 = 0; i8 < this.w; i8++) {
                        j2 ^= (long) ((bArr3[i17] & 255) << (i8 << 3));
                        i17++;
                    }
                    i6 = 0;
                    obj2 = obj5;
                    for (i5 = 
/*
Method generation error in method: org.bouncycastle.pqc.crypto.gmss.util.WinternitzOTSVerify.Verify(byte[], byte[]):byte[], dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r11_11 'i5' int) = (r11_0 'i5' int), (r11_19 'i5' int) binds: {(r11_0 'i5' int)=B:24:0x00e8, (r11_19 'i5' int)=B:36:0x0153} in method: org.bouncycastle.pqc.crypto.gmss.util.WinternitzOTSVerify.Verify(byte[], byte[]):byte[], dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:185)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:220)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:63)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:120)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeRegionIndent(RegionGen.java:95)
	at jadx.core.codegen.RegionGen.makeIf(RegionGen.java:130)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:59)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.RegionGen.makeSimpleRegion(RegionGen.java:89)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:55)
	at jadx.core.codegen.MethodGen.addInstructions(MethodGen.java:183)
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
	... 34 more

*/

    public int getLog(int i) {
        int i2 = 1;
        int i3 = 2;
        while (i3 < i) {
            i3 <<= 1;
            i2++;
        }
        return i2;
    }

    public int getSignatureLength() {
        int digestSize = this.messDigestOTS.getDigestSize();
        int i = ((digestSize << 3) + (this.w - 1)) / this.w;
        return digestSize * (i + (((getLog((i << this.w) + 1) + this.w) - 1) / this.w));
    }
}
