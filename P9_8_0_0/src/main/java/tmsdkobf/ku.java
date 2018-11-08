package tmsdkobf;

import com.qq.taf.jce.JceStruct;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;

public class ku {
    private static volatile boolean xT = false;
    private static List<byte[]> xU = null;
    private static List<Long> xV = null;

    private static int a(RandomAccessFile randomAccessFile) throws Exception {
        if (randomAccessFile.readInt() == 712365948) {
            return randomAccessFile.readInt();
        }
        throw new Exception();
    }

    private static int a(RandomAccessFile randomAccessFile, int -l_5_I, int i) throws Exception {
        Object -l_3_R = new ArrayList();
        Object -l_4_R = new ArrayList();
        do {
            int -l_6_I = a(randomAccessFile);
            long -l_9_J = randomAccessFile.readLong();
            if (-l_6_I > 255) {
                i = 0;
                break;
            }
            Object -l_7_R = new byte[-l_6_I];
            if (randomAccessFile.read(-l_7_R, 0, -l_6_I) != -l_6_I) {
                i = 0;
                break;
            }
            if (-l_5_I <= i) {
                -l_3_R.add(-l_7_R);
                -l_4_R.add(Long.valueOf(-l_9_J));
            }
            -l_5_I--;
        } while (-l_5_I > 0);
        randomAccessFile.setLength(0);
        if (i != 0) {
            a(randomAccessFile, 0);
            i = a(randomAccessFile, null, 0, -l_3_R, -l_4_R);
            long -l_11_J = randomAccessFile.getFilePointer();
            randomAccessFile.seek(0);
            a(randomAccessFile, i);
            randomAccessFile.seek(-l_11_J);
            return i;
        }
        a(randomAccessFile, 0);
        return i;
    }

    private static int a(RandomAccessFile randomAccessFile, byte[] bArr, long j, List<byte[]> list, List<Long> list2) throws Exception {
        int -l_6_I = 0;
        if (bArr != null && bArr.length > 0) {
            -l_6_I = 1;
            a(randomAccessFile, bArr.length);
            randomAccessFile.writeLong(j);
            randomAccessFile.write(bArr);
        }
        if (list != null && list.size() > 0) {
            int -l_7_I = list.size();
            for (int -l_8_I = 0; -l_8_I < -l_7_I; -l_8_I++) {
                byte[] -l_9_R = (byte[]) list.get(-l_8_I);
                if (-l_9_R != null && -l_9_R.length > 0) {
                    -l_6_I++;
                    byte[] bArr2 = (byte[]) list.get(-l_8_I);
                    a(randomAccessFile, bArr2.length);
                    randomAccessFile.writeLong(((Long) list2.get(-l_8_I)).longValue());
                    randomAccessFile.write(bArr2);
                }
            }
        }
        return -l_6_I;
    }

    private static void a(RandomAccessFile randomAccessFile, int i) throws Exception {
        randomAccessFile.writeInt(712365948);
        randomAccessFile.writeInt(i);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void a(byte[] bArr, long j, List<byte[]> list, List<Long> list2) {
        Object -l_9_R;
        Object obj = null;
        Object -l_6_R = new File(TMSDKContext.getApplicaionContext().getFilesDir() + File.separator + ".bufflocache001");
        int -l_7_I = 0;
        Object randomAccessFile;
        try {
            if (!-l_6_R.exists()) {
                -l_6_R.createNewFile();
            }
            randomAccessFile = new RandomAccessFile(-l_6_R, "rws");
            try {
                if (randomAccessFile.length() <= 0) {
                    obj = 1;
                }
                if (obj == null) {
                    -l_7_I = a(randomAccessFile);
                    if (-l_7_I <= 20) {
                        randomAccessFile.seek(randomAccessFile.length());
                    } else {
                        -l_7_I = a(randomAccessFile, -l_7_I, 15);
                    }
                } else {
                    a(randomAccessFile, 0);
                }
                -l_7_I += a(randomAccessFile, bArr, j, list, list2);
                randomAccessFile.seek(0);
                a(randomAccessFile, -l_7_I);
                if (randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (IOException e) {
                    }
                }
            } catch (IOException e2) {
            } catch (Throwable th) {
                -l_9_R = th;
                if (randomAccessFile != null) {
                    try {
                        randomAccessFile.close();
                    } catch (IOException e3) {
                    }
                }
                throw -l_9_R;
            }
        } catch (Throwable th2) {
            -l_9_R = th2;
            randomAccessFile = null;
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
            throw -l_9_R;
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static boolean b(ArrayList<String> arrayList, ArrayList<String> arrayList2) {
        Object -l_11_R;
        RandomAccessFile randomAccessFile = null;
        Object -l_3_R = new File(TMSDKContext.getApplicaionContext().getFilesDir() + File.separator + ".bufflocache001");
        if (!-l_3_R.exists()) {
            return false;
        }
        int -l_4_I = 0;
        try {
            RandomAccessFile -l_2_R = new RandomAccessFile(-l_3_R, "rws");
            try {
                if ((-l_2_R.length() <= 0 ? 1 : null) == null) {
                    try {
                        int -l_6_I = a(-l_2_R);
                        if (-l_6_I > 0) {
                            for (int -l_10_I = -l_6_I; -l_10_I > 0; -l_10_I--) {
                                int -l_7_I = a(-l_2_R);
                                if (-l_7_I > IncomingSmsFilterConsts.PAY_SMS) {
                                    break;
                                }
                                long -l_8_J = -l_2_R.readLong();
                                Object -l_5_R = new byte[-l_7_I];
                                if (-l_7_I != -l_2_R.read(-l_5_R, 0, -l_7_I)) {
                                    break;
                                }
                                arrayList2.add(new String(-l_5_R));
                                arrayList.add(String.valueOf(-l_8_J));
                            }
                            if (arrayList.size() > 0) {
                                -l_4_I = 1;
                            }
                        }
                    } catch (Throwable th) {
                        -l_11_R = th;
                        randomAccessFile = -l_2_R;
                        if (randomAccessFile != null) {
                            try {
                                randomAccessFile.close();
                            } catch (IOException e) {
                            }
                        }
                        throw -l_11_R;
                    }
                }
                if (-l_2_R != null) {
                    try {
                        -l_2_R.close();
                    } catch (IOException e2) {
                    }
                }
                randomAccessFile = -l_2_R;
            } catch (Throwable th2) {
                -l_11_R = th2;
                randomAccessFile = -l_2_R;
                if (randomAccessFile != null) {
                    randomAccessFile.close();
                }
                throw -l_11_R;
            }
        } catch (Throwable th3) {
            -l_11_R = th3;
            if (randomAccessFile != null) {
                randomAccessFile.close();
            }
            throw -l_11_R;
        }
        return -l_4_I;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static synchronized void bt(String str) {
        synchronized (ku.class) {
            if (gf.S().ab().booleanValue()) {
                Object -l_1_R = str.getBytes();
                long -l_2_J = System.currentTimeMillis();
                if (xT) {
                    if (xU == null) {
                        xU = new ArrayList();
                        xV = new ArrayList();
                    }
                    xU.add(-l_1_R);
                    xV.add(Long.valueOf(-l_2_J));
                } else {
                    a(-l_1_R, -l_2_J, null, null);
                }
            }
        }
    }

    public static synchronized void dO() {
        synchronized (ku.class) {
            if (!gf.S().ab().booleanValue()) {
            } else if (xT) {
            } else {
                xT = true;
                Object -l_0_R = new ArrayList();
                Object -l_1_R = new ArrayList();
                if (b(-l_0_R, -l_1_R) != 0) {
                    Object -l_3_R = new aq();
                    -l_3_R.bC = 54;
                    -l_3_R.bI = new HashMap();
                    -l_3_R.bI.put(Integer.valueOf(2), -l_0_R);
                    -l_3_R.bI.put(Integer.valueOf(3), -l_1_R);
                    im.bK().a(3122, -l_3_R, null, 0, new jy() {
                        public void onFinish(int i, int i2, int i3, int i4, JceStruct jceStruct) {
                            switch (ne.bg(i3)) {
                                case 0:
                                    ku.r(true);
                                    break;
                                default:
                                    ku.r(false);
                                    break;
                            }
                            ku.xU = null;
                            ku.xV = null;
                            ku.xT = false;
                        }
                    });
                    return;
                }
                xT = false;
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void r(boolean z) {
        Object -l_8_R;
        Object -l_2_R = new File(TMSDKContext.getApplicaionContext().getFilesDir() + File.separator + ".bufflocache001");
        int -l_3_I = 0;
        Object -l_1_R;
        try {
            if (!-l_2_R.exists()) {
                -l_2_R.createNewFile();
            }
            -l_1_R = new RandomAccessFile(-l_2_R, "rws");
            try {
                long -l_4_J = -l_1_R.length();
                if ((-l_4_J <= 0 ? 1 : null) != null) {
                    a(-l_1_R, 0);
                } else if (z) {
                    -l_1_R.setLength(0);
                    a(-l_1_R, 0);
                } else {
                    -l_3_I = a(-l_1_R);
                    -l_1_R.seek(-l_4_J);
                }
                if (xU != null) {
                    if (xU.size() > 0) {
                        -l_3_I += a(-l_1_R, null, 0, new ArrayList(xU), new ArrayList(xV));
                        -l_1_R.seek(0);
                        a(-l_1_R, -l_3_I);
                        if (-l_1_R == null) {
                            try {
                                -l_1_R.close();
                            } catch (IOException e) {
                            }
                        }
                    }
                }
                if (-l_3_I == 0) {
                    -l_1_R.setLength(0);
                }
                if (-l_1_R == null) {
                    -l_1_R.close();
                }
            } catch (Throwable th) {
                -l_8_R = th;
                if (-l_1_R != null) {
                    try {
                        -l_1_R.close();
                    } catch (IOException e2) {
                    }
                }
                throw -l_8_R;
            }
        } catch (Throwable th2) {
            -l_8_R = th2;
            -l_1_R = null;
            if (-l_1_R != null) {
                -l_1_R.close();
            }
            throw -l_8_R;
        }
    }
}
