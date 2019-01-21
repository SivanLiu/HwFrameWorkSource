package org.bouncycastle.crypto.engines;

import java.io.PrintStream;
import java.math.BigInteger;
import java.util.Vector;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.params.NaccacheSternKeyParameters;
import org.bouncycastle.crypto.params.NaccacheSternPrivateKeyParameters;
import org.bouncycastle.crypto.params.ParametersWithRandom;
import org.bouncycastle.util.Arrays;

public class NaccacheSternEngine implements AsymmetricBlockCipher {
    private static BigInteger ONE = BigInteger.valueOf(1);
    private static BigInteger ZERO = BigInteger.valueOf(0);
    private boolean debug = false;
    private boolean forEncryption;
    private NaccacheSternKeyParameters key;
    private Vector[] lookup = null;

    private static BigInteger chineseRemainder(Vector vector, Vector vector2) {
        BigInteger bigInteger = ZERO;
        int i = 0;
        BigInteger bigInteger2 = ONE;
        for (int i2 = 0; i2 < vector2.size(); i2++) {
            bigInteger2 = bigInteger2.multiply((BigInteger) vector2.elementAt(i2));
        }
        while (i < vector2.size()) {
            BigInteger bigInteger3 = (BigInteger) vector2.elementAt(i);
            BigInteger divide = bigInteger2.divide(bigInteger3);
            bigInteger = bigInteger.add(divide.multiply(divide.modInverse(bigInteger3)).multiply((BigInteger) vector.elementAt(i)));
            i++;
        }
        return bigInteger.mod(bigInteger2);
    }

    public byte[] addCryptedBlocks(byte[] bArr, byte[] bArr2) throws InvalidCipherTextException {
        if (this.forEncryption) {
            if (bArr.length > getOutputBlockSize() || bArr2.length > getOutputBlockSize()) {
                throw new InvalidCipherTextException("BlockLength too large for simple addition.\n");
            }
        } else if (bArr.length > getInputBlockSize() || bArr2.length > getInputBlockSize()) {
            throw new InvalidCipherTextException("BlockLength too large for simple addition.\n");
        }
        BigInteger bigInteger = new BigInteger(1, bArr);
        BigInteger bigInteger2 = new BigInteger(1, bArr2);
        BigInteger mod = bigInteger.multiply(bigInteger2).mod(this.key.getModulus());
        if (this.debug) {
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("c(m1) as BigInteger:....... ");
            stringBuilder.append(bigInteger);
            printStream.println(stringBuilder.toString());
            PrintStream printStream2 = System.out;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("c(m2) as BigInteger:....... ");
            stringBuilder2.append(bigInteger2);
            printStream2.println(stringBuilder2.toString());
            PrintStream printStream3 = System.out;
            StringBuilder stringBuilder3 = new StringBuilder();
            stringBuilder3.append("c(m1)*c(m2)%n = c(m1+m2)%n: ");
            stringBuilder3.append(mod);
            printStream3.println(stringBuilder3.toString());
        }
        bArr = this.key.getModulus().toByteArray();
        Arrays.fill(bArr, (byte) 0);
        System.arraycopy(mod.toByteArray(), 0, bArr, bArr.length - mod.toByteArray().length, mod.toByteArray().length);
        return bArr;
    }

    public byte[] encrypt(BigInteger bigInteger) {
        byte[] toByteArray = this.key.getModulus().toByteArray();
        Arrays.fill(toByteArray, (byte) 0);
        byte[] toByteArray2 = this.key.getG().modPow(bigInteger, this.key.getModulus()).toByteArray();
        System.arraycopy(toByteArray2, 0, toByteArray, toByteArray.length - toByteArray2.length, toByteArray2.length);
        if (this.debug) {
            PrintStream printStream = System.out;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Encrypted value is:  ");
            stringBuilder.append(new BigInteger(toByteArray));
            printStream.println(stringBuilder.toString());
        }
        return toByteArray;
    }

    public int getInputBlockSize() {
        return this.forEncryption ? ((this.key.getLowerSigmaBound() + 7) / 8) - 1 : this.key.getModulus().toByteArray().length;
    }

    public int getOutputBlockSize() {
        return this.forEncryption ? this.key.getModulus().toByteArray().length : ((this.key.getLowerSigmaBound() + 7) / 8) - 1;
    }

    public void init(boolean z, CipherParameters cipherParameters) {
        this.forEncryption = z;
        if (cipherParameters instanceof ParametersWithRandom) {
            cipherParameters = ((ParametersWithRandom) cipherParameters).getParameters();
        }
        this.key = (NaccacheSternKeyParameters) cipherParameters;
        if (!this.forEncryption) {
            if (this.debug) {
                System.out.println("Constructing lookup Array");
            }
            NaccacheSternPrivateKeyParameters naccacheSternPrivateKeyParameters = (NaccacheSternPrivateKeyParameters) this.key;
            Vector smallPrimes = naccacheSternPrivateKeyParameters.getSmallPrimes();
            this.lookup = new Vector[smallPrimes.size()];
            for (int i = 0; i < smallPrimes.size(); i++) {
                BigInteger bigInteger = (BigInteger) smallPrimes.elementAt(i);
                int intValue = bigInteger.intValue();
                this.lookup[i] = new Vector();
                this.lookup[i].addElement(ONE);
                if (this.debug) {
                    PrintStream printStream = System.out;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Constructing lookup ArrayList for ");
                    stringBuilder.append(intValue);
                    printStream.println(stringBuilder.toString());
                }
                BigInteger bigInteger2 = ZERO;
                for (int i2 = 1; i2 < intValue; i2++) {
                    bigInteger2 = bigInteger2.add(naccacheSternPrivateKeyParameters.getPhi_n());
                    this.lookup[i].addElement(naccacheSternPrivateKeyParameters.getG().modPow(bigInteger2.divide(bigInteger), naccacheSternPrivateKeyParameters.getModulus()));
                }
            }
        }
    }

    public byte[] processBlock(byte[] bArr, int i, int i2) throws InvalidCipherTextException {
        if (this.key == null) {
            throw new IllegalStateException("NaccacheStern engine not initialised");
        } else if (i2 > getInputBlockSize() + 1) {
            throw new DataLengthException("input too large for Naccache-Stern cipher.\n");
        } else if (this.forEncryption || i2 >= getInputBlockSize()) {
            PrintStream printStream;
            int i3 = 0;
            if (!(i == 0 && i2 == bArr.length)) {
                byte[] bArr2 = new byte[i2];
                System.arraycopy(bArr, i, bArr2, 0, i2);
                bArr = bArr2;
            }
            BigInteger bigInteger = new BigInteger(1, bArr);
            if (this.debug) {
                printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("input as BigInteger: ");
                stringBuilder.append(bigInteger);
                printStream.println(stringBuilder.toString());
            }
            if (this.forEncryption) {
                return encrypt(bigInteger);
            }
            Vector vector = new Vector();
            NaccacheSternPrivateKeyParameters naccacheSternPrivateKeyParameters = (NaccacheSternPrivateKeyParameters) this.key;
            Vector smallPrimes = naccacheSternPrivateKeyParameters.getSmallPrimes();
            for (int i4 = 0; i4 < smallPrimes.size(); i4++) {
                BigInteger modPow = bigInteger.modPow(naccacheSternPrivateKeyParameters.getPhi_n().divide((BigInteger) smallPrimes.elementAt(i4)), naccacheSternPrivateKeyParameters.getModulus());
                Vector vector2 = this.lookup[i4];
                StringBuilder stringBuilder2;
                if (this.lookup[i4].size() != ((BigInteger) smallPrimes.elementAt(i4)).intValue()) {
                    if (this.debug) {
                        printStream = System.out;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Prime is ");
                        stringBuilder2.append(smallPrimes.elementAt(i4));
                        stringBuilder2.append(", lookup table has size ");
                        stringBuilder2.append(vector2.size());
                        printStream.println(stringBuilder2.toString());
                    }
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Error in lookup Array for ");
                    stringBuilder2.append(((BigInteger) smallPrimes.elementAt(i4)).intValue());
                    stringBuilder2.append(": Size mismatch. Expected ArrayList with length ");
                    stringBuilder2.append(((BigInteger) smallPrimes.elementAt(i4)).intValue());
                    stringBuilder2.append(" but found ArrayList of length ");
                    stringBuilder2.append(this.lookup[i4].size());
                    throw new InvalidCipherTextException(stringBuilder2.toString());
                }
                int indexOf = vector2.indexOf(modPow);
                if (indexOf == -1) {
                    if (this.debug) {
                        printStream = System.out;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Actual prime is ");
                        stringBuilder2.append(smallPrimes.elementAt(i4));
                        printStream.println(stringBuilder2.toString());
                        printStream = System.out;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Decrypted value is ");
                        stringBuilder2.append(modPow);
                        printStream.println(stringBuilder2.toString());
                        printStream = System.out;
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("LookupList for ");
                        stringBuilder2.append(smallPrimes.elementAt(i4));
                        stringBuilder2.append(" with size ");
                        stringBuilder2.append(this.lookup[i4].size());
                        stringBuilder2.append(" is: ");
                        printStream.println(stringBuilder2.toString());
                        while (i3 < this.lookup[i4].size()) {
                            System.out.println(this.lookup[i4].elementAt(i3));
                            i3++;
                        }
                    }
                    throw new InvalidCipherTextException("Lookup failed");
                }
                vector.addElement(BigInteger.valueOf((long) indexOf));
            }
            return chineseRemainder(vector, smallPrimes).toByteArray();
        } else {
            throw new InvalidCipherTextException("BlockLength does not match modulus for Naccache-Stern cipher.\n");
        }
    }

    public byte[] processData(byte[] bArr) throws InvalidCipherTextException {
        if (this.debug) {
            System.out.println();
        }
        if (bArr.length > getInputBlockSize()) {
            int inputBlockSize = getInputBlockSize();
            int outputBlockSize = getOutputBlockSize();
            if (this.debug) {
                PrintStream printStream = System.out;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Input blocksize is:  ");
                stringBuilder.append(inputBlockSize);
                stringBuilder.append(" bytes");
                printStream.println(stringBuilder.toString());
                printStream = System.out;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Output blocksize is: ");
                stringBuilder.append(outputBlockSize);
                stringBuilder.append(" bytes");
                printStream.println(stringBuilder.toString());
                printStream = System.out;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Data has length:.... ");
                stringBuilder.append(bArr.length);
                stringBuilder.append(" bytes");
                printStream.println(stringBuilder.toString());
            }
            byte[] bArr2 = new byte[(((bArr.length / inputBlockSize) + 1) * outputBlockSize)];
            int i = 0;
            int i2 = i;
            while (i < bArr.length) {
                Object processBlock;
                int i3 = i + inputBlockSize;
                if (i3 < bArr.length) {
                    int i4 = i3;
                    processBlock = processBlock(bArr, i, inputBlockSize);
                    i = i4;
                } else {
                    processBlock = processBlock(bArr, i, bArr.length - i);
                    i += bArr.length - i;
                }
                if (this.debug) {
                    PrintStream printStream2 = System.out;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("new datapos is ");
                    stringBuilder2.append(i);
                    printStream2.println(stringBuilder2.toString());
                }
                if (processBlock != null) {
                    System.arraycopy(processBlock, 0, bArr2, i2, processBlock.length);
                    i2 += processBlock.length;
                } else {
                    if (this.debug) {
                        System.out.println("cipher returned null");
                    }
                    throw new InvalidCipherTextException("cipher returned null");
                }
            }
            bArr = new byte[i2];
            System.arraycopy(bArr2, 0, bArr, 0, i2);
            if (this.debug) {
                PrintStream printStream3 = System.out;
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("returning ");
                stringBuilder3.append(bArr.length);
                stringBuilder3.append(" bytes");
                printStream3.println(stringBuilder3.toString());
            }
            return bArr;
        }
        if (this.debug) {
            System.out.println("data size is less then input block size, processing directly");
        }
        return processBlock(bArr, 0, bArr.length);
    }

    public void setDebug(boolean z) {
        this.debug = z;
    }
}
