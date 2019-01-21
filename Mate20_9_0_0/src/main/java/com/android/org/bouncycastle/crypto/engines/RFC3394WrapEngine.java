package com.android.org.bouncycastle.crypto.engines;

import com.android.org.bouncycastle.crypto.BlockCipher;
import com.android.org.bouncycastle.crypto.CipherParameters;
import com.android.org.bouncycastle.crypto.DataLengthException;
import com.android.org.bouncycastle.crypto.InvalidCipherTextException;
import com.android.org.bouncycastle.crypto.Wrapper;
import com.android.org.bouncycastle.crypto.params.KeyParameter;
import com.android.org.bouncycastle.crypto.params.ParametersWithIV;
import com.android.org.bouncycastle.crypto.params.ParametersWithRandom;

public class RFC3394WrapEngine implements Wrapper {
    private BlockCipher engine;
    private boolean forWrapping;
    private byte[] iv;
    private KeyParameter param;
    private boolean wrapCipherMode;

    public RFC3394WrapEngine(BlockCipher engine) {
        this(engine, false);
    }

    public RFC3394WrapEngine(BlockCipher engine, boolean useReverseDirection) {
        this.iv = new byte[]{(byte) -90, (byte) -90, (byte) -90, (byte) -90, (byte) -90, (byte) -90, (byte) -90, (byte) -90};
        this.engine = engine;
        this.wrapCipherMode = useReverseDirection ^ 1;
    }

    public void init(boolean forWrapping, CipherParameters param) {
        this.forWrapping = forWrapping;
        if (param instanceof ParametersWithRandom) {
            param = ((ParametersWithRandom) param).getParameters();
        }
        if (param instanceof KeyParameter) {
            this.param = (KeyParameter) param;
        } else if (param instanceof ParametersWithIV) {
            this.iv = ((ParametersWithIV) param).getIV();
            this.param = (KeyParameter) ((ParametersWithIV) param).getParameters();
            if (this.iv.length != 8) {
                throw new IllegalArgumentException("IV not equal to 8");
            }
        }
    }

    public String getAlgorithmName() {
        return this.engine.getAlgorithmName();
    }

    public byte[] wrap(byte[] in, int inOff, int inLen) {
        int i = inLen;
        byte[] bArr;
        int i2;
        if (this.forWrapping) {
            int n = i / 8;
            if (n * 8 == i) {
                byte[] block = new byte[(this.iv.length + i)];
                byte[] buf = new byte[(this.iv.length + 8)];
                System.arraycopy(this.iv, 0, block, 0, this.iv.length);
                System.arraycopy(in, inOff, block, this.iv.length, i);
                this.engine.init(this.wrapCipherMode, this.param);
                for (int j = 0; j != 6; j++) {
                    for (int i3 = 1; i3 <= n; i3++) {
                        System.arraycopy(block, 0, buf, 0, this.iv.length);
                        System.arraycopy(block, 8 * i3, buf, this.iv.length, 8);
                        this.engine.processBlock(buf, 0, buf, 0);
                        int t = (n * j) + i3;
                        int k = 1;
                        while (t != 0) {
                            int length = this.iv.length - k;
                            buf[length] = (byte) (buf[length] ^ ((byte) t));
                            t >>>= 8;
                            k++;
                        }
                        System.arraycopy(buf, 0, block, 0, 8);
                        System.arraycopy(buf, 8, block, 8 * i3, 8);
                    }
                }
                return block;
            }
            bArr = in;
            i2 = inOff;
            throw new DataLengthException("wrap data must be a multiple of 8 bytes");
        }
        bArr = in;
        i2 = inOff;
        throw new IllegalStateException("not set for wrapping");
    }

    public byte[] unwrap(byte[] in, int inOff, int inLen) throws InvalidCipherTextException {
        Object obj = in;
        int i = inOff;
        int i2 = inLen;
        if (this.forWrapping) {
            throw new IllegalStateException("not set for unwrapping");
        }
        int n = i2 / 8;
        if (n * 8 == i2) {
            byte[] block = new byte[(i2 - this.iv.length)];
            byte[] a = new byte[this.iv.length];
            int i3 = 8;
            byte[] buf = new byte[(this.iv.length + 8)];
            System.arraycopy(obj, i, a, 0, this.iv.length);
            System.arraycopy(obj, this.iv.length + i, block, 0, i2 - this.iv.length);
            int i4 = 1;
            this.engine.init(this.wrapCipherMode ^ 1, this.param);
            n--;
            int j = 5;
            while (j >= 0) {
                int i5 = n;
                for (i4 = 
/*
Method generation error in method: com.android.org.bouncycastle.crypto.engines.RFC3394WrapEngine.unwrap(byte[], int, int):byte[], dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r12_1 'i4' int) = (r12_0 'i4' int), (r12_10 'i4' int) binds: {(r12_0 'i4' int)=B:4:0x0012, (r12_10 'i4' int)=B:12:0x0089} in method: com.android.org.bouncycastle.crypto.engines.RFC3394WrapEngine.unwrap(byte[], int, int):byte[], dex: 
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
	... 31 more

*/
}
