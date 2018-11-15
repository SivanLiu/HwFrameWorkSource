package org.bouncycastle.crypto.generators;

import java.math.BigInteger;
import java.security.SecureRandom;
import org.bouncycastle.asn1.cmp.PKIFailureInfo;
import org.bouncycastle.crypto.params.GOST3410Parameters;
import org.bouncycastle.crypto.params.GOST3410ValidationParameters;

public class GOST3410ParametersGenerator {
    private static final BigInteger ONE = BigInteger.valueOf(1);
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private SecureRandom init_random;
    private int size;
    private int typeproc;

    private int procedure_A(int i, int i2, BigInteger[] bigIntegerArr, int i3) {
        int i4 = i;
        while (true) {
            if (i4 >= 0 && i4 <= PKIFailureInfo.notAuthorized) {
                break;
            }
            i4 = this.init_random.nextInt() / 32768;
        }
        int i5 = i2;
        while (true) {
            if (i5 >= 0 && i5 <= PKIFailureInfo.notAuthorized && i5 / 2 != 0) {
                break;
            }
            i5 = (this.init_random.nextInt() / 32768) + 1;
        }
        BigInteger bigInteger = new BigInteger(Integer.toString(i5));
        BigInteger bigInteger2 = new BigInteger("19381");
        Object obj = new BigInteger[1];
        BigInteger bigInteger3 = new BigInteger(Integer.toString(i4));
        i4 = 0;
        obj[0] = bigInteger3;
        Object obj2 = new int[]{i3};
        int i6 = 0;
        int i7 = i6;
        while (obj2[i6] >= 17) {
            Object obj3 = new int[(obj2.length + 1)];
            System.arraycopy(obj2, 0, obj3, 0, obj2.length);
            obj2 = new int[obj3.length];
            System.arraycopy(obj3, 0, obj2, 0, obj3.length);
            i7 = i6 + 1;
            obj2[i7] = obj2[i6] / 2;
            i6 = i7;
        }
        BigInteger[] bigIntegerArr2 = new BigInteger[(i7 + 1)];
        int i8 = 16;
        bigIntegerArr2[i7] = new BigInteger("8003", 16);
        int i9 = i7 - 1;
        Object obj4 = obj;
        int i10 = 0;
        while (i10 < i7) {
            Object obj5;
            BigInteger bigInteger4;
            int i11 = obj2[i9] / i8;
            while (true) {
                int i12;
                Object obj6 = new BigInteger[obj4.length];
                System.arraycopy(obj4, i4, obj6, i4, obj4.length);
                obj5 = new BigInteger[(i11 + 1)];
                System.arraycopy(obj6, i4, obj5, i4, obj6.length);
                int i13 = i4;
                while (i13 < i11) {
                    int i14 = i13 + 1;
                    obj5[i14] = obj5[i13].multiply(bigInteger2).add(bigInteger).mod(TWO.pow(i8));
                    i13 = i14;
                }
                BigInteger bigInteger5 = new BigInteger("0");
                for (i13 = i4; i13 < i11; i13++) {
                    bigInteger5 = bigInteger5.add(obj5[i13].multiply(TWO.pow(i8 * i13)));
                }
                obj5[0] = obj5[i11];
                int i15 = i9 + 1;
                bigInteger4 = bigInteger;
                bigInteger = TWO.pow(obj2[i9] - 1).divide(bigIntegerArr2[i15]).add(TWO.pow(obj2[i9] - 1).multiply(bigInteger5).divide(bigIntegerArr2[i15].multiply(TWO.pow(16 * i11))));
                if (bigInteger.mod(TWO).compareTo(ONE) == 0) {
                    bigInteger = bigInteger.add(ONE);
                }
                i4 = 0;
                while (true) {
                    i12 = i11;
                    long j = (long) i4;
                    bigIntegerArr2[i9] = bigIntegerArr2[i15].multiply(bigInteger.add(BigInteger.valueOf(j))).add(ONE);
                    if (bigIntegerArr2[i9].compareTo(TWO.pow(obj2[i9])) != 1) {
                        if (TWO.modPow(bigIntegerArr2[i15].multiply(bigInteger.add(BigInteger.valueOf(j))), bigIntegerArr2[i9]).compareTo(ONE) == 0 && TWO.modPow(bigInteger.add(BigInteger.valueOf(j)), bigIntegerArr2[i9]).compareTo(ONE) != 0) {
                            break;
                        }
                        i4 += 2;
                        i11 = i12;
                    } else {
                        break;
                    }
                }
                i15 = 1;
                obj4 = obj5;
                bigInteger = bigInteger4;
                i11 = i12;
                i4 = 0;
                i8 = 16;
            }
            i9--;
            if (i9 < 0) {
                bigIntegerArr[0] = bigIntegerArr2[0];
                bigIntegerArr[1] = bigIntegerArr2[1];
                bigInteger = obj5[0];
                break;
            }
            i10++;
            obj4 = obj5;
            bigInteger = bigInteger4;
            i4 = 0;
            i8 = 16;
        }
        bigInteger = obj4[i4];
        return bigInteger.intValue();
    }

    private long procedure_Aa(long j, long j2, BigInteger[] bigIntegerArr, int i) {
        long j3 = j;
        while (true) {
            if (j3 >= 0 && j3 <= 4294967296L) {
                break;
            }
            j3 = (long) (this.init_random.nextInt() * 2);
        }
        long j4 = j2;
        while (true) {
            if (j4 >= 0 && j4 <= 4294967296L && j4 / 2 != 0) {
                break;
            }
            j4 = (long) ((this.init_random.nextInt() * 2) + 1);
        }
        BigInteger bigInteger = new BigInteger(Long.toString(j4));
        BigInteger bigInteger2 = new BigInteger("97781173");
        Object obj = new BigInteger[1];
        BigInteger bigInteger3 = new BigInteger(Long.toString(j3));
        int i2 = 0;
        obj[0] = bigInteger3;
        Object obj2 = new int[]{i};
        int i3 = 0;
        int i4 = i3;
        while (obj2[i3] >= 33) {
            Object obj3 = new int[(obj2.length + 1)];
            System.arraycopy(obj2, 0, obj3, 0, obj2.length);
            obj2 = new int[obj3.length];
            System.arraycopy(obj3, 0, obj2, 0, obj3.length);
            i4 = i3 + 1;
            obj2[i4] = obj2[i3] / 2;
            i3 = i4;
        }
        BigInteger[] bigIntegerArr2 = new BigInteger[(i4 + 1)];
        bigIntegerArr2[i4] = new BigInteger("8000000B", 16);
        int i5 = i4 - 1;
        Object obj4 = obj;
        int i6 = 0;
        while (i6 < i4) {
            Object obj5;
            BigInteger bigInteger4;
            BigInteger bigInteger5;
            int i7 = 32;
            int i8 = obj2[i5] / 32;
            while (true) {
                Object obj6 = new BigInteger[obj4.length];
                System.arraycopy(obj4, i2, obj6, i2, obj4.length);
                obj5 = new BigInteger[(i8 + 1)];
                System.arraycopy(obj6, i2, obj5, i2, obj6.length);
                int i9 = i2;
                while (i9 < i8) {
                    int i10 = i9 + 1;
                    obj5[i10] = obj5[i9].multiply(bigInteger2).add(bigInteger).mod(TWO.pow(i7));
                    i9 = i10;
                }
                BigInteger bigInteger6 = new BigInteger("0");
                for (i9 = i2; i9 < i8; i9++) {
                    bigInteger6 = bigInteger6.add(obj5[i9].multiply(TWO.pow(i7 * i9)));
                }
                obj5[0] = obj5[i8];
                int i11 = i5 + 1;
                bigInteger4 = bigInteger;
                bigInteger = TWO.pow(obj2[i5] - 1).divide(bigIntegerArr2[i11]).add(TWO.pow(obj2[i5] - 1).multiply(bigInteger6).divide(bigIntegerArr2[i11].multiply(TWO.pow(32 * i8))));
                if (bigInteger.mod(TWO).compareTo(ONE) == 0) {
                    bigInteger = bigInteger.add(ONE);
                }
                i2 = 0;
                while (true) {
                    long j5 = (long) i2;
                    bigIntegerArr2[i5] = bigIntegerArr2[i11].multiply(bigInteger.add(BigInteger.valueOf(j5))).add(ONE);
                    bigInteger5 = bigInteger2;
                    if (bigIntegerArr2[i5].compareTo(TWO.pow(obj2[i5])) != 1) {
                        if (TWO.modPow(bigIntegerArr2[i11].multiply(bigInteger.add(BigInteger.valueOf(j5))), bigIntegerArr2[i5]).compareTo(ONE) == 0 && TWO.modPow(bigInteger.add(BigInteger.valueOf(j5)), bigIntegerArr2[i5]).compareTo(ONE) != 0) {
                            break;
                        }
                        i2 += 2;
                        bigInteger2 = bigInteger5;
                    } else {
                        break;
                    }
                }
                int i12 = 1;
                obj4 = obj5;
                bigInteger = bigInteger4;
                bigInteger2 = bigInteger5;
                i2 = 0;
                i7 = 32;
            }
            i5--;
            if (i5 < 0) {
                bigIntegerArr[0] = bigIntegerArr2[0];
                bigIntegerArr[1] = bigIntegerArr2[1];
                bigInteger = obj5[0];
                break;
            }
            i6++;
            obj4 = obj5;
            bigInteger = bigInteger4;
            bigInteger2 = bigInteger5;
            i2 = 0;
        }
        bigInteger = obj4[i2];
        return bigInteger.longValue();
    }

    private void procedure_B(int i, int i2, BigInteger[] bigIntegerArr) {
        int i3 = i;
        while (true) {
            if (i3 >= 0 && i3 <= PKIFailureInfo.notAuthorized) {
                break;
            }
            i3 = this.init_random.nextInt() / 32768;
        }
        int i4 = i2;
        while (true) {
            if (i4 >= 0 && i4 <= PKIFailureInfo.notAuthorized && i4 / 2 != 0) {
                break;
            }
            i4 = (this.init_random.nextInt() / 32768) + 1;
        }
        BigInteger[] bigIntegerArr2 = new BigInteger[2];
        BigInteger bigInteger = new BigInteger(Integer.toString(i4));
        BigInteger bigInteger2 = new BigInteger("19381");
        i3 = procedure_A(i3, i4, bigIntegerArr2, 256);
        BigInteger bigInteger3 = bigIntegerArr2[0];
        int procedure_A = procedure_A(i3, i4, bigIntegerArr2, 512);
        BigInteger bigInteger4 = bigIntegerArr2[0];
        BigInteger[] bigIntegerArr3 = new BigInteger[65];
        bigIntegerArr3[0] = new BigInteger(Integer.toString(procedure_A));
        while (true) {
            procedure_A = 0;
            while (procedure_A < 64) {
                int i5 = procedure_A + 1;
                bigIntegerArr3[i5] = bigIntegerArr3[procedure_A].multiply(bigInteger2).add(bigInteger).mod(TWO.pow(16));
                procedure_A = i5;
            }
            BigInteger bigInteger5 = new BigInteger("0");
            for (procedure_A = 0; procedure_A < 64; procedure_A++) {
                bigInteger5 = bigInteger5.add(bigIntegerArr3[procedure_A].multiply(TWO.pow(16 * procedure_A)));
            }
            bigIntegerArr3[0] = bigIntegerArr3[64];
            int i6 = 1024;
            BigInteger add = TWO.pow(1023).divide(bigInteger3.multiply(bigInteger4)).add(TWO.pow(1023).multiply(bigInteger5).divide(bigInteger3.multiply(bigInteger4).multiply(TWO.pow(1024))));
            if (add.mod(TWO).compareTo(ONE) == 0) {
                add = add.add(ONE);
            }
            BigInteger bigInteger6 = add;
            procedure_A = 0;
            while (true) {
                long j = (long) procedure_A;
                BigInteger add2 = bigInteger3.multiply(bigInteger4).multiply(bigInteger6.add(BigInteger.valueOf(j))).add(ONE);
                if (add2.compareTo(TWO.pow(i6)) == 1) {
                    break;
                } else if (TWO.modPow(bigInteger3.multiply(bigInteger4).multiply(bigInteger6.add(BigInteger.valueOf(j))), add2).compareTo(ONE) != 0 || TWO.modPow(bigInteger3.multiply(bigInteger6.add(BigInteger.valueOf(j))), add2).compareTo(ONE) == 0) {
                    procedure_A += 2;
                    i6 = 1024;
                } else {
                    bigIntegerArr[0] = add2;
                    bigIntegerArr[1] = bigInteger3;
                    return;
                }
            }
        }
    }

    private void procedure_Bb(long j, long j2, BigInteger[] bigIntegerArr) {
        long j3 = j;
        while (true) {
            if (j3 >= 0 && j3 <= 4294967296L) {
                break;
            }
            j3 = (long) (this.init_random.nextInt() * 2);
        }
        long j4 = j2;
        while (true) {
            if (j4 >= 0 && j4 <= 4294967296L && j4 / 2 != 0) {
                break;
            }
            j4 = (long) ((this.init_random.nextInt() * 2) + 1);
        }
        BigInteger[] bigIntegerArr2 = new BigInteger[2];
        BigInteger bigInteger = new BigInteger(Long.toString(j4));
        BigInteger bigInteger2 = new BigInteger("97781173");
        long j5 = j4;
        BigInteger[] bigIntegerArr3 = bigIntegerArr2;
        j3 = procedure_Aa(j3, j5, bigIntegerArr3, 256);
        BigInteger bigInteger3 = bigIntegerArr2[0];
        long procedure_Aa = procedure_Aa(j3, j5, bigIntegerArr3, 512);
        BigInteger bigInteger4 = bigIntegerArr2[0];
        BigInteger[] bigIntegerArr4 = new BigInteger[33];
        bigIntegerArr4[0] = new BigInteger(Long.toString(procedure_Aa));
        while (true) {
            int i = 0;
            while (i < 32) {
                int i2 = i + 1;
                bigIntegerArr4[i2] = bigIntegerArr4[i].multiply(bigInteger2).add(bigInteger).mod(TWO.pow(32));
                i = i2;
            }
            BigInteger bigInteger5 = new BigInteger("0");
            for (i = 0; i < 32; i++) {
                bigInteger5 = bigInteger5.add(bigIntegerArr4[i].multiply(TWO.pow(32 * i)));
            }
            bigIntegerArr4[0] = bigIntegerArr4[32];
            BigInteger add = TWO.pow(1023).divide(bigInteger3.multiply(bigInteger4)).add(TWO.pow(1023).multiply(bigInteger5).divide(bigInteger3.multiply(bigInteger4).multiply(TWO.pow(1024))));
            if (add.mod(TWO).compareTo(ONE) == 0) {
                add = add.add(ONE);
            }
            int i3 = 0;
            while (true) {
                j5 = (long) i3;
                bigInteger5 = bigInteger3.multiply(bigInteger4).multiply(add.add(BigInteger.valueOf(j5))).add(ONE);
                if (bigInteger5.compareTo(TWO.pow(1024)) == 1) {
                    break;
                } else if (TWO.modPow(bigInteger3.multiply(bigInteger4).multiply(add.add(BigInteger.valueOf(j5))), bigInteger5).compareTo(ONE) != 0 || TWO.modPow(bigInteger3.multiply(add.add(BigInteger.valueOf(j5))), bigInteger5).compareTo(ONE) == 0) {
                    i3 += 2;
                } else {
                    bigIntegerArr[0] = bigInteger5;
                    bigIntegerArr[1] = bigInteger3;
                    return;
                }
            }
        }
    }

    private BigInteger procedure_C(BigInteger bigInteger, BigInteger bigInteger2) {
        BigInteger subtract = bigInteger.subtract(ONE);
        bigInteger2 = subtract.divide(bigInteger2);
        int bitLength = bigInteger.bitLength();
        while (true) {
            BigInteger bigInteger3 = new BigInteger(bitLength, this.init_random);
            if (bigInteger3.compareTo(ONE) > 0 && bigInteger3.compareTo(subtract) < 0) {
                bigInteger3 = bigInteger3.modPow(bigInteger2, bigInteger);
                if (bigInteger3.compareTo(ONE) != 0) {
                    return bigInteger3;
                }
            }
        }
    }

    public GOST3410Parameters generateParameters() {
        BigInteger[] bigIntegerArr = new BigInteger[2];
        int nextInt;
        BigInteger bigInteger;
        if (this.typeproc == 1) {
            nextInt = this.init_random.nextInt();
            int nextInt2 = this.init_random.nextInt();
            int i = this.size;
            if (i == 512) {
                procedure_A(nextInt, nextInt2, bigIntegerArr, 512);
            } else if (i == 1024) {
                procedure_B(nextInt, nextInt2, bigIntegerArr);
            } else {
                throw new IllegalArgumentException("Ooops! key size 512 or 1024 bit.");
            }
            BigInteger bigInteger2 = bigIntegerArr[0];
            bigInteger = bigIntegerArr[1];
            return new GOST3410Parameters(bigInteger2, bigInteger, procedure_C(bigInteger2, bigInteger), new GOST3410ValidationParameters(nextInt, nextInt2));
        }
        long nextLong = this.init_random.nextLong();
        long nextLong2 = this.init_random.nextLong();
        nextInt = this.size;
        if (nextInt == 512) {
            procedure_Aa(nextLong, nextLong2, bigIntegerArr, 512);
        } else if (nextInt == 1024) {
            procedure_Bb(nextLong, nextLong2, bigIntegerArr);
        } else {
            throw new IllegalStateException("Ooops! key size 512 or 1024 bit.");
        }
        BigInteger bigInteger3 = bigIntegerArr[0];
        bigInteger = bigIntegerArr[1];
        return new GOST3410Parameters(bigInteger3, bigInteger, procedure_C(bigInteger3, bigInteger), new GOST3410ValidationParameters(nextLong, nextLong2));
    }

    public void init(int i, int i2, SecureRandom secureRandom) {
        this.size = i;
        this.typeproc = i2;
        this.init_random = secureRandom;
    }
}
