package tmsdkobf;

import java.lang.reflect.Array;
import java.security.SecureRandom;
import tmsdk.common.exception.BadExpiryDataException;
import tmsdk.common.module.intelli_sms.SmsCheckResult;

final class io {
    private int rK;
    private long rL;
    private long rM;

    io(boolean z) {
        int -l_3_I;
        lo.a(ir.class);
        SecureRandom -l_2_R = new SecureRandom();
        do {
            -l_3_I = -l_2_R.nextInt(900) + SmsCheckResult.ESCT_NORMAL;
        } while (ae(-l_3_I));
        this.rK = -l_3_I;
        int -l_4_I = this.rK;
        Object -l_5_R = new int[100];
        double -l_8_D = Math.sqrt((double) -l_4_I);
        int -l_10_I = ((int) -l_8_D) - 1;
        while ((-l_10_I + 1) * (-l_10_I + 1) <= -l_4_I) {
            -l_10_I++;
        }
        -l_5_R[0] = -l_10_I;
        if (-l_10_I * -l_10_I != -l_4_I) {
            int -l_12_I = 0;
            double -l_13_D = -l_8_D;
            int -l_11_I = 1;
            Object -l_15_R = new int[5];
            -l_15_R[1] = 1;
            -l_15_R[2] = null;
            -l_15_R[3] = --l_5_R[0];
            -l_15_R[4] = 1;
            int[][] -l_16_R = (int[][]) Array.newInstance(Integer.TYPE, new int[]{100, 5});
            int -l_17_I = 0;
            while (-l_11_I < 100) {
                if (-l_15_R[1] < null) {
                    -l_15_R[1] = --l_15_R[1];
                    -l_15_R[2] = --l_15_R[2];
                    -l_15_R[3] = --l_15_R[3];
                    -l_15_R[4] = --l_15_R[4];
                }
                int -l_18_I = k(k(k(-l_15_R[1], Math.abs(-l_15_R[2])), Math.abs(-l_15_R[3])), Math.abs(-l_15_R[4]));
                if (-l_18_I > 1) {
                    -l_15_R[1] = -l_15_R[1] / -l_18_I;
                    -l_15_R[2] = -l_15_R[2] / -l_18_I;
                    -l_15_R[3] = -l_15_R[3] / -l_18_I;
                    -l_15_R[4] = -l_15_R[4] / -l_18_I;
                }
                -l_12_I = 0;
                while (-l_12_I < -l_17_I) {
                    Object -l_19_R = -l_16_R[-l_12_I];
                    if (-l_19_R[1] == -l_15_R[1] && -l_19_R[2] == -l_15_R[2] && -l_19_R[3] == -l_15_R[3]) {
                        if (-l_19_R[4] == -l_15_R[4]) {
                            break;
                        }
                    }
                    -l_12_I++;
                }
                if (-l_12_I < -l_17_I) {
                    break;
                }
                -l_16_R[-l_17_I][1] = -l_15_R[1];
                -l_16_R[-l_17_I][2] = -l_15_R[2];
                -l_16_R[-l_17_I][3] = -l_15_R[3];
                -l_16_R[-l_17_I][4] = -l_15_R[4];
                -l_17_I++;
                -l_5_R[-l_11_I] = (int) Math.floor(a(-l_8_D, -l_15_R));
                a(-l_4_I, -l_15_R, -l_5_R[-l_11_I]);
                -l_11_I++;
            }
            int -l_6_I = -l_11_I - 1;
            int -l_7_I = -l_12_I;
            long -l_18_J = 0;
            long -l_20_J = 1;
            int -l_22_I = 1;
            int -l_23_I = 0;
            while (true) {
                if ((-l_18_J < 200 ? 1 : null) != null || -l_23_I == 0) {
                    -l_22_I++;
                    -l_18_J = 0;
                    -l_20_J = 1;
                    -l_11_I = -l_22_I - 1;
                    while (-l_11_I >= 0) {
                        long -l_27_J = -l_18_J;
                        long -l_29_J = -l_20_J + (((long) (-l_22_I > -l_6_I ? -l_5_R[((-l_11_I - -l_7_I) % -l_6_I) + -l_7_I] : -l_5_R[-l_11_I])) * -l_18_J);
                        -l_20_J = -l_18_J;
                        -l_18_J = -l_29_J;
                        -l_11_I--;
                    }
                    if ((-l_18_J < 1000000 ? 1 : null) == null) {
                        throw new RuntimeException();
                    }
                    long -l_26_J = -l_18_J * -l_18_J;
                    long -l_28_J = -l_20_J * -l_20_J;
                    long -l_30_J = ((long) -l_4_I) * -l_28_J;
                    long -l_32_J = -l_26_J * 1000;
                    if ((-l_18_J < 200 ? 1 : null) == null) {
                        if ((999 * -l_30_J >= -l_32_J ? 1 : null) == null) {
                            if ((-l_32_J >= 1001 * -l_30_J ? 1 : null) == null && -l_23_I == 0) {
                                if (z) {
                                    if ((-l_26_J < ((long) -l_4_I) * -l_28_J ? 1 : null) == null) {
                                    }
                                    -l_23_I = 1;
                                }
                                if (!z) {
                                    if ((-l_26_J <= ((long) -l_4_I) * -l_28_J ? 1 : null) != null) {
                                    }
                                    -l_23_I = 1;
                                }
                            }
                        }
                    }
                } else {
                    b(-l_18_J, -l_20_J);
                    return;
                }
            }
        }
    }

    private static final double a(double d, int[] iArr) {
        return (((double) iArr[1]) + (((double) iArr[2]) * d)) / (((double) iArr[3]) + (((double) iArr[4]) * d));
    }

    private static final void a(int i, int[] iArr, int i2) {
        iArr[1] = iArr[1] - (iArr[3] * i2);
        iArr[2] = iArr[2] - (iArr[4] * i2);
        int -l_4_I = (iArr[1] * iArr[4]) - (iArr[2] * iArr[3]);
        int -l_5_I = (iArr[1] * iArr[1]) - ((iArr[2] * iArr[2]) * i);
        iArr[1] = (iArr[1] * iArr[3]) - ((iArr[2] * iArr[4]) * i);
        iArr[2] = -l_4_I;
        iArr[3] = -l_5_I;
        iArr[4] = 0;
    }

    private static final boolean ae(int i) {
        int -l_3_I = ((int) Math.sqrt((double) i)) - 1;
        while ((-l_3_I + 1) * (-l_3_I + 1) <= i) {
            -l_3_I++;
        }
        return -l_3_I * -l_3_I == i;
    }

    private void bR() throws BadExpiryDataException {
        long -l_1_J = (long) this.rK;
        long -l_3_J = this.rL;
        long -l_5_J = this.rM;
        if ((-l_3_J >= 200 ? 1 : null) == null) {
            throw new BadExpiryDataException();
        }
        long -l_11_J = -l_1_J * (-l_5_J * -l_5_J);
        long -l_13_J = -l_11_J * 1000;
        long -l_15_J = (-l_3_J * -l_3_J) * 1000;
        if ((-l_15_J <= -l_13_J - -l_11_J ? 1 : null) == null) {
            if ((-l_15_J < -l_13_J + -l_11_J ? 1 : null) != null) {
                return;
            }
        }
        throw new BadExpiryDataException();
    }

    private static final int k(int i, int i2) {
        if (i == 0) {
            return i2;
        }
        if (i2 == 0) {
            return i;
        }
        if (i < i2) {
            return k(i2, i);
        }
        while (true) {
            int -l_2_I = i % i2;
            if (-l_2_I == 0) {
                return i2;
            }
            i = i2;
            i2 = -l_2_I;
        }
    }

    final void b(long j, long j2) {
        lo.a(getClass(), ir.class);
        this.rL = j;
        this.rM = j2;
        bR();
    }
}
