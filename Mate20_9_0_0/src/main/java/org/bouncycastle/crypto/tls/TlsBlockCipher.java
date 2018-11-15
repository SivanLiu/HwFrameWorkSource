package org.bouncycastle.crypto.tls;

import java.io.IOException;
import java.security.SecureRandom;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.util.Arrays;

public class TlsBlockCipher implements TlsCipher {
    protected TlsContext context;
    protected BlockCipher decryptCipher;
    protected BlockCipher encryptCipher;
    protected boolean encryptThenMAC;
    protected byte[] randomData = new byte[256];
    protected TlsMac readMac;
    protected boolean useExplicitIV;
    protected TlsMac writeMac;

    public TlsBlockCipher(TlsContext tlsContext, BlockCipher blockCipher, BlockCipher blockCipher2, Digest digest, Digest digest2, int i) throws IOException {
        byte[] bArr;
        TlsContext tlsContext2 = tlsContext;
        BlockCipher blockCipher3 = blockCipher;
        BlockCipher blockCipher4 = blockCipher2;
        int i2 = i;
        this.context = tlsContext2;
        tlsContext.getNonceRandomGenerator().nextBytes(this.randomData);
        this.useExplicitIV = TlsUtils.isTLSv11(tlsContext);
        this.encryptThenMAC = tlsContext.getSecurityParameters().encryptThenMAC;
        int digestSize = ((2 * i2) + digest.getDigestSize()) + digest2.getDigestSize();
        if (!this.useExplicitIV) {
            digestSize += blockCipher.getBlockSize() + blockCipher2.getBlockSize();
        }
        int i3 = digestSize;
        byte[] calculateKeyBlock = TlsUtils.calculateKeyBlock(tlsContext2, i3);
        TlsContext tlsContext3 = tlsContext2;
        byte[] bArr2 = calculateKeyBlock;
        TlsMac tlsMac = new TlsMac(tlsContext3, digest, bArr2, 0, digest.getDigestSize());
        int digestSize2 = 0 + digest.getDigestSize();
        TlsMac tlsMac2 = tlsMac;
        tlsMac = new TlsMac(tlsContext3, digest2, bArr2, digestSize2, digest2.getDigestSize());
        digestSize2 += digest2.getDigestSize();
        CipherParameters keyParameter = new KeyParameter(calculateKeyBlock, digestSize2, i2);
        digestSize2 += i2;
        CipherParameters keyParameter2 = new KeyParameter(calculateKeyBlock, digestSize2, i2);
        digestSize2 += i2;
        if (this.useExplicitIV) {
            bArr = new byte[blockCipher.getBlockSize()];
            bArr2 = new byte[blockCipher2.getBlockSize()];
        } else {
            bArr = Arrays.copyOfRange(calculateKeyBlock, digestSize2, blockCipher.getBlockSize() + digestSize2);
            digestSize2 += blockCipher.getBlockSize();
            bArr2 = Arrays.copyOfRange(calculateKeyBlock, digestSize2, blockCipher2.getBlockSize() + digestSize2);
            digestSize2 += blockCipher2.getBlockSize();
        }
        if (digestSize2 == i3) {
            CipherParameters parametersWithIV;
            if (tlsContext.isServer()) {
                this.writeMac = tlsMac2;
                this.readMac = tlsMac;
                this.encryptCipher = blockCipher4;
                this.decryptCipher = blockCipher3;
                parametersWithIV = new ParametersWithIV(keyParameter2, bArr2);
                keyParameter = new ParametersWithIV(keyParameter, bArr);
            } else {
                this.writeMac = tlsMac;
                this.readMac = tlsMac2;
                this.encryptCipher = blockCipher3;
                this.decryptCipher = blockCipher4;
                parametersWithIV = new ParametersWithIV(keyParameter, bArr);
                keyParameter = new ParametersWithIV(keyParameter2, bArr2);
            }
            this.encryptCipher.init(true, parametersWithIV);
            this.decryptCipher.init(false, keyParameter);
            return;
        }
        throw new TlsFatalAlert((short) 80);
    }

    protected int checkPaddingConstantTime(byte[] bArr, int i, int i2, int i3, int i4) {
        int i5;
        i += i2;
        byte b = bArr[i - 1];
        int i6 = (b & 255) + 1;
        if ((!TlsUtils.isSSL(this.context) || i6 <= i3) && i4 + i6 <= i2) {
            i3 = i - i6;
            i2 = 0;
            while (true) {
                i4 = i3 + 1;
                i2 = (byte) (i2 | (bArr[i3] ^ b));
                if (i4 >= i) {
                    break;
                }
                i3 = i4;
            }
            i5 = i2 != 0 ? 0 : i6;
        } else {
            i5 = 0;
            i2 = i5;
            i6 = i2;
        }
        byte[] bArr2 = this.randomData;
        for (i6 = 
/*
Method generation error in method: org.bouncycastle.crypto.tls.TlsBlockCipher.checkPaddingConstantTime(byte[], int, int, int, int):int, dex: 
jadx.core.utils.exceptions.CodegenException: Error generate insn: PHI: (r1_3 'i6' int) = (r1_1 'i6' int), (r1_1 'i6' int), (r1_2 'i6' int) binds: {(r1_1 'i6' int)=B:10:0x002a, (r1_1 'i6' int)=B:11:0x002c, (r1_2 'i6' int)=B:5:0x0017} in method: org.bouncycastle.crypto.tls.TlsBlockCipher.checkPaddingConstantTime(byte[], int, int, int, int):int, dex: 
	at jadx.core.codegen.InsnGen.makeInsn(InsnGen.java:228)
	at jadx.core.codegen.RegionGen.makeLoop(RegionGen.java:183)
	at jadx.core.codegen.RegionGen.makeRegion(RegionGen.java:61)
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
	... 15 more

*/

    protected int chooseExtraPadBlocks(SecureRandom secureRandom, int i) {
        return Math.min(lowestBitSet(secureRandom.nextInt()), i);
    }

    public byte[] decodeCiphertext(long j, short s, byte[] bArr, int i, int i2) throws IOException {
        byte[] bArr2 = bArr;
        int i3 = i;
        int i4 = i2;
        int blockSize = this.decryptCipher.getBlockSize();
        boolean size = this.readMac.getSize();
        int max = this.encryptThenMAC ? blockSize + size : Math.max(blockSize, size + 1);
        if (this.useExplicitIV) {
            max += blockSize;
        }
        if (i4 >= max) {
            max = this.encryptThenMAC ? i4 - size : i4;
            if (max % blockSize == 0) {
                int i5;
                if (this.encryptThenMAC) {
                    int i6 = i3 + i4;
                    if ((Arrays.constantTimeAreEqual(this.readMac.calculateMac(j, s, bArr2, i3, i4 - size), Arrays.copyOfRange(bArr2, i6 - size, i6)) ^ 1) != 0) {
                        throw new TlsFatalAlert((short) 20);
                    }
                }
                if (this.useExplicitIV) {
                    this.decryptCipher.init(false, new ParametersWithIV(null, bArr2, i3, blockSize));
                    i3 += blockSize;
                    max -= blockSize;
                }
                int i7 = i3;
                int i8 = max;
                for (i3 = 0; i3 < i8; i3 += blockSize) {
                    max = i7 + i3;
                    this.decryptCipher.processBlock(bArr2, max, bArr2, max);
                }
                short s2 = (short) 20;
                i3 = checkPaddingConstantTime(bArr2, i7, i8, blockSize, this.encryptThenMAC ? false : size);
                int i9 = i3 == 0 ? 1 : 0;
                i3 = i8 - i3;
                if (this.encryptThenMAC) {
                    i5 = i3;
                } else {
                    i5 = i3 - size;
                    i3 = i7 + i5;
                    i9 |= Arrays.constantTimeAreEqual(this.readMac.calculateMacConstantTime(j, s, bArr2, i7, i5, i8 - size, this.randomData), Arrays.copyOfRange(bArr2, i3, i3 + size)) ^ 1;
                }
                if (i9 == 0) {
                    return Arrays.copyOfRange(bArr2, i7, i5 + i7);
                }
                throw new TlsFatalAlert(s2);
            }
            throw new TlsFatalAlert((short) 21);
        }
        throw new TlsFatalAlert((short) 50);
    }

    public byte[] encodePlaintext(long j, short s, byte[] bArr, int i, int i2) {
        Object obj;
        Object obj2;
        int i3;
        int i4;
        int i5 = i2;
        int blockSize = this.encryptCipher.getBlockSize();
        int size = this.writeMac.getSize();
        ProtocolVersion serverVersion = this.context.getServerVersion();
        int i6 = (blockSize - 1) - ((!this.encryptThenMAC ? i5 + size : i5) % blockSize);
        if (!((!this.encryptThenMAC && this.context.getSecurityParameters().truncatedHMac) || serverVersion.isDTLS() || serverVersion.isSSL())) {
            i6 += chooseExtraPadBlocks(this.context.getSecureRandom(), (255 - i6) / blockSize) * blockSize;
        }
        int i7 = i6;
        size = ((size + i5) + i7) + 1;
        if (this.useExplicitIV) {
            size += blockSize;
        }
        Object obj3 = new byte[size];
        if (this.useExplicitIV) {
            obj = new byte[blockSize];
            this.context.getNonceRandomGenerator().nextBytes(obj);
            this.encryptCipher.init(true, new ParametersWithIV(null, obj));
            System.arraycopy(obj, 0, obj3, 0, blockSize);
            obj2 = bArr;
            i3 = i;
            i4 = 0 + blockSize;
        } else {
            obj2 = bArr;
            i3 = i;
            i4 = 0;
        }
        System.arraycopy(obj2, i3, obj3, i4, i5);
        int i8 = i4 + i5;
        if (!this.encryptThenMAC) {
            obj = this.writeMac.calculateMac(j, s, obj2, i3, i5);
            System.arraycopy(obj, 0, obj3, i8, obj.length);
            i8 += obj.length;
        }
        int i9 = i8;
        size = 0;
        while (size <= i7) {
            int i10 = i9 + 1;
            obj3[i9] = (byte) i7;
            size++;
            i9 = i10;
        }
        while (i4 < i9) {
            this.encryptCipher.processBlock(obj3, i4, obj3, i4);
            i4 += blockSize;
        }
        if (!this.encryptThenMAC) {
            return obj3;
        }
        size = 0;
        Object obj4 = obj3;
        Object calculateMac = this.writeMac.calculateMac(j, s, obj3, 0, i9);
        System.arraycopy(calculateMac, size, obj4, i9, calculateMac.length);
        int length = calculateMac.length;
        return obj4;
    }

    public int getPlaintextLimit(int i) {
        int blockSize = this.encryptCipher.getBlockSize();
        int size = this.writeMac.getSize();
        if (this.useExplicitIV) {
            i -= blockSize;
        }
        if (this.encryptThenMAC) {
            i -= size;
            i -= i % blockSize;
        } else {
            i = (i - (i % blockSize)) - size;
        }
        return i - 1;
    }

    public TlsMac getReadMac() {
        return this.readMac;
    }

    public TlsMac getWriteMac() {
        return this.writeMac;
    }

    protected int lowestBitSet(int i) {
        if (i == 0) {
            return 32;
        }
        int i2 = 0;
        while ((i & 1) == 0) {
            i2++;
            i >>= 1;
        }
        return i2;
    }
}
