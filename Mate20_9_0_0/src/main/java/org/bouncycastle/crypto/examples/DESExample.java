package org.bouncycastle.crypto.examples;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.security.SecureRandom;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.KeyGenerationParameters;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.generators.DESedeKeyGenerator;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;

public class DESExample {
    private PaddedBufferedBlockCipher cipher = null;
    private boolean encrypt = true;
    private BufferedInputStream in = null;
    private byte[] key = null;
    private BufferedOutputStream out = null;

    public DESExample(String str, String str2, String str3, boolean z) {
        PrintStream printStream;
        KeyGenerationParameters keyGenerationParameters;
        DESedeKeyGenerator dESedeKeyGenerator;
        BufferedOutputStream bufferedOutputStream;
        byte[] encode;
        StringBuilder stringBuilder;
        String str4;
        this.encrypt = z;
        try {
            this.in = new BufferedInputStream(new FileInputStream(str));
        } catch (FileNotFoundException e) {
            PrintStream printStream2 = System.err;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Input file not found [");
            stringBuilder2.append(str);
            stringBuilder2.append("]");
            printStream2.println(stringBuilder2.toString());
            System.exit(1);
        }
        try {
            this.out = new BufferedOutputStream(new FileOutputStream(str2));
        } catch (IOException e2) {
            printStream = System.err;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("Output file not created [");
            stringBuilder3.append(str2);
            stringBuilder3.append("]");
            printStream.println(stringBuilder3.toString());
            System.exit(1);
        }
        if (z) {
            SecureRandom secureRandom;
            try {
                secureRandom = new SecureRandom();
                try {
                    secureRandom.setSeed("www.bouncycastle.org".getBytes());
                } catch (Exception e3) {
                }
            } catch (Exception e4) {
                secureRandom = null;
                try {
                    System.err.println("Hmmm, no SHA1PRNG, you need the Sun implementation");
                    System.exit(1);
                    keyGenerationParameters = new KeyGenerationParameters(secureRandom, 192);
                    dESedeKeyGenerator = new DESedeKeyGenerator();
                    dESedeKeyGenerator.init(keyGenerationParameters);
                    this.key = dESedeKeyGenerator.generateKey();
                    bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(str3));
                    encode = Hex.encode(this.key);
                    bufferedOutputStream.write(encode, 0, encode.length);
                    bufferedOutputStream.flush();
                    bufferedOutputStream.close();
                    return;
                } catch (IOException e5) {
                    printStream = System.err;
                    stringBuilder = new StringBuilder();
                    str4 = "Could not decryption create key file [";
                    stringBuilder.append(str4);
                    stringBuilder.append(str3);
                    stringBuilder.append("]");
                    printStream.println(stringBuilder.toString());
                    System.exit(1);
                }
            }
            keyGenerationParameters = new KeyGenerationParameters(secureRandom, 192);
            dESedeKeyGenerator = new DESedeKeyGenerator();
            dESedeKeyGenerator.init(keyGenerationParameters);
            this.key = dESedeKeyGenerator.generateKey();
            bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(str3));
            encode = Hex.encode(this.key);
            bufferedOutputStream.write(encode, 0, encode.length);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            return;
        }
        try {
            BufferedInputStream bufferedInputStream = new BufferedInputStream(new FileInputStream(str3));
            int available = bufferedInputStream.available();
            byte[] bArr = new byte[available];
            bufferedInputStream.read(bArr, 0, available);
            this.key = Hex.decode(bArr);
        } catch (IOException e6) {
            printStream = System.err;
            stringBuilder = new StringBuilder();
            str4 = "Decryption key file not found, or not valid [";
            stringBuilder.append(str4);
            stringBuilder.append(str3);
            stringBuilder.append("]");
            printStream.println(stringBuilder.toString());
            System.exit(1);
        }
    }

    public static void main(String[] strArr) {
        boolean z = true;
        if (strArr.length < 2) {
            DESExample dESExample = new DESExample();
            PrintStream printStream = System.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Usage: java ");
            stringBuilder.append(dESExample.getClass().getName());
            stringBuilder.append(" infile outfile [keyfile]");
            printStream.println(stringBuilder.toString());
            System.exit(1);
        }
        String str = "deskey.dat";
        String str2 = strArr[0];
        String str3 = strArr[1];
        if (strArr.length > 2) {
            str = strArr[2];
            z = false;
        }
        new DESExample(str2, str3, str, z).process();
    }

    private void performDecrypt(byte[] bArr) {
        this.cipher.init(false, new KeyParameter(bArr));
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(this.in));
        byte[] bArr2 = null;
        while (true) {
            try {
                String readLine = bufferedReader.readLine();
                if (readLine != null) {
                    byte[] decode = Hex.decode(readLine);
                    bArr2 = new byte[this.cipher.getOutputSize(decode.length)];
                    int processBytes = this.cipher.processBytes(decode, 0, decode.length, bArr2, 0);
                    if (processBytes > 0) {
                        this.out.write(bArr2, 0, processBytes);
                    }
                } else {
                    try {
                        break;
                    } catch (CryptoException e) {
                        return;
                    }
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        int doFinal = this.cipher.doFinal(bArr2, 0);
        if (doFinal > 0) {
            this.out.write(bArr2, 0, doFinal);
        }
    }

    private void performEncrypt(byte[] bArr) {
        this.cipher.init(true, new KeyParameter(bArr));
        byte[] bArr2 = new byte[47];
        bArr = new byte[this.cipher.getOutputSize(47)];
        while (true) {
            try {
                int read = this.in.read(bArr2, 0, 47);
                if (read > 0) {
                    int processBytes = this.cipher.processBytes(bArr2, 0, read, bArr, 0);
                    if (processBytes > 0) {
                        byte[] encode = Hex.encode(bArr, 0, processBytes);
                        this.out.write(encode, 0, encode.length);
                        this.out.write(10);
                    }
                } else {
                    try {
                        break;
                    } catch (CryptoException e) {
                        return;
                    }
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }
        int doFinal = this.cipher.doFinal(bArr, 0);
        if (doFinal > 0) {
            bArr = Hex.encode(bArr, 0, doFinal);
            this.out.write(bArr, 0, bArr.length);
            this.out.write(10);
        }
    }

    private void process() {
        this.cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new DESedeEngine()));
        if (this.encrypt) {
            performEncrypt(this.key);
        } else {
            performDecrypt(this.key);
        }
        try {
            this.in.close();
            this.out.flush();
            this.out.close();
        } catch (IOException e) {
            PrintStream printStream = System.err;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("exception closing resources: ");
            stringBuilder.append(e.getMessage());
            printStream.println(stringBuilder.toString());
        }
    }
}
