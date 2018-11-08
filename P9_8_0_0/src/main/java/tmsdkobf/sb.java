package tmsdkobf;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Pair;
import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import tmsdk.common.utils.f;
import tmsdk.fg.module.spacemanager.ISpaceScanListener;
import tmsdk.fg.module.spacemanager.SpaceManager;

public class sb {
    private static int QA = 4;
    private static sb QC = null;
    private static final String[] Qq = new String[]{"_id", "_data", "datetaken", "_size"};
    private static final int Qr = dH("_id");
    private static final int Qs = dH("datetaken");
    private static final int Qt = dH("_data");
    private static final int Qu = dH("_size");
    private static int Qx = 1;
    private static int Qy = 2;
    private static int Qz = 3;
    private sc QB;
    private ISpaceScanListener Qo;
    private ISpaceScanListener Qp;
    private int Qv;
    private AtomicBoolean Qw;
    private int mState;
    byte[] mg;
    private Handler vW;

    public static class a {
        public long QE;
        public long QF;
        public long mInnerPicSize;
        public long mOutPicSize;
        public Pair<Integer, Long> mPhotoCountAndSize;
        public ArrayList<sa> mResultList;
        public Pair<Integer, Long> mScreenShotCountAndSize;
    }

    private sb() {
        this.mg = new byte[0];
        this.Qo = null;
        this.Qp = null;
        this.mState = 0;
        this.Qv = 0;
        this.Qw = new AtomicBoolean();
        this.mState = Qx;
        this.Qv = QA;
        this.Qw.set(false);
        this.vW = new Handler(this, Looper.getMainLooper()) {
            final /* synthetic */ sb QD;

            public void handleMessage(Message message) {
                switch (message.what) {
                    case 4097:
                        if (this.QD.Qo != null) {
                            this.QD.Qo.onStart();
                            return;
                        }
                        return;
                    case 4098:
                        if (this.QD.Qo != null) {
                            this.QD.Qo.onFound(message.obj);
                            return;
                        }
                        return;
                    case 4099:
                        if (this.QD.Qo != null) {
                            this.QD.Qo.onProgressChanged(message.arg1);
                            return;
                        }
                        return;
                    case 4100:
                        if (this.QD.Qo != null) {
                            this.QD.Qo.onCancelFinished();
                            this.QD.Qo = null;
                            this.QD.mState = sb.Qx;
                            return;
                        }
                        return;
                    case 4101:
                        if (this.QD.Qo != null) {
                            this.QD.Qo.onFinish(message.arg1, message.obj);
                            this.QD.Qo = null;
                            this.QD.mState = sb.Qx;
                            return;
                        }
                        return;
                    case 4353:
                        if (this.QD.Qp != null) {
                            this.QD.Qp.onStart();
                            return;
                        }
                        return;
                    case 4354:
                        if (this.QD.Qp != null) {
                            this.QD.Qp.onFound(message.obj);
                            return;
                        }
                        return;
                    case 4355:
                        if (this.QD.Qp != null) {
                            this.QD.Qp.onProgressChanged(message.arg1);
                            return;
                        }
                        return;
                    case 4356:
                        if (this.QD.Qp != null) {
                            this.QD.Qp.onCancelFinished();
                            this.QD.Qp = null;
                            this.QD.Qv = sb.QA;
                            return;
                        }
                        return;
                    case 4357:
                        if (this.QD.Qp != null) {
                            this.QD.Qp.onFinish(message.arg1, message.obj);
                            this.QD.Qp = null;
                            this.QD.Qv = sb.QA;
                            return;
                        }
                        return;
                    default:
                        return;
                }
            }
        };
    }

    private static long a(long -l_3_J, String str) {
        Object obj = null;
        if (-l_3_J <= 0) {
            obj = 1;
        }
        if (obj == null) {
            return -l_3_J;
        }
        if (rz.dD(str)) {
            -l_3_J = rz.dG(str);
        }
        if (-l_3_J == 0) {
            -l_3_J = new File(str).lastModified();
        }
        return -l_3_J;
    }

    private ArrayList<sa> a(ContentResolver contentResolver, Uri uri, String[] strArr, String[] strArr2) {
        Cursor -l_6_R;
        ArrayList -l_5_R = new ArrayList();
        Object -l_6_R2 = null;
        if (strArr2 != null) {
            try {
                Object -l_7_R = c(strArr2);
                Object -l_8_R = "bucket_id=?";
                for (int -l_9_I = 1; -l_9_I < strArr2.length; -l_9_I++) {
                    -l_8_R = -l_8_R + " OR bucket_id=?";
                }
                -l_6_R = contentResolver.query(uri, strArr, -l_8_R + " AND _size>30720", -l_7_R, null);
            } catch (Throwable th) {
                if (-l_6_R2 != null) {
                    try {
                        -l_6_R2.close();
                    } catch (Exception e) {
                    }
                }
            }
        } else {
            -l_6_R = contentResolver.query(uri, strArr, "_size>30720", null, null);
        }
        if (-l_6_R == null) {
            f.e("PhotoManager", "cursor is null!");
        } else {
            -l_6_R.moveToFirst();
            int -l_7_I = -l_6_R.getCount();
            int -l_8_I = 0;
            while (!-l_6_R.isAfterLast() && !this.Qw.get()) {
                String -l_9_R = -l_6_R.getString(Qt);
                -l_8_I++;
                int -l_10_I = (-l_8_I * 100) / -l_7_I;
                Message -l_11_R = this.vW.obtainMessage(4098);
                -l_11_R.obj = -l_9_R;
                this.vW.sendMessage(-l_11_R);
                Message -l_12_R = this.vW.obtainMessage(4099);
                -l_12_R.arg1 = -l_10_I;
                this.vW.sendMessage(-l_12_R);
                if (dI(-l_9_R)) {
                    if (rz.dB(-l_9_R)) {
                        ArrayList arrayList = -l_5_R;
                        arrayList.add(new sa(a(-l_6_R.getLong(Qs), -l_9_R), b(-l_6_R.getLong(Qu), -l_9_R), -l_9_R, -l_6_R.getLong(Qr)));
                        -l_6_R.moveToNext();
                    }
                }
                f.g("PhotoManager", "media file not exist : " + -l_9_R);
                -l_6_R.moveToNext();
            }
        }
        if (-l_6_R != null) {
            try {
                -l_6_R.close();
            } catch (Exception e2) {
                return -l_5_R;
            }
        }
        return -l_5_R;
    }

    private static long b(long -l_3_J, String str) {
        return ((-l_3_J > 0 ? 1 : (-l_3_J == 0 ? 0 : -1)) <= 0 ? 1 : null) == null ? -l_3_J : new File(str).length();
    }

    private void b(a aVar) {
        long -l_2_J = 0;
        long -l_4_J = 0;
        int -l_6_I = 0;
        int -l_7_I = 0;
        if (aVar.mResultList != null) {
            aVar.mInnerPicSize = 0;
            aVar.QE = 0;
            aVar.mOutPicSize = 0;
            aVar.QF = 0;
            Object -l_8_R = aVar.mResultList.iterator();
            while (-l_8_R.hasNext()) {
                sa -l_9_R = (sa) -l_8_R.next();
                if (-l_9_R.mIsScreenShot) {
                    -l_4_J += -l_9_R.mSize;
                    -l_7_I++;
                }
                -l_2_J += -l_9_R.mSize;
                -l_6_I++;
                if (-l_9_R.mIsOut) {
                    aVar.mOutPicSize += -l_9_R.mSize;
                } else {
                    aVar.mInnerPicSize += -l_9_R.mSize;
                }
            }
            aVar.mPhotoCountAndSize = new Pair(Integer.valueOf(-l_6_I), Long.valueOf(-l_2_J));
            aVar.mScreenShotCountAndSize = new Pair(Integer.valueOf(-l_7_I), Long.valueOf(-l_4_J));
        }
    }

    private String[] c(String[] strArr) {
        Object -l_2_R = new String[strArr.length];
        for (int -l_3_I = 0; -l_3_I < strArr.length; -l_3_I++) {
            -l_2_R[-l_3_I] = String.valueOf(strArr[-l_3_I].toLowerCase().hashCode());
        }
        return -l_2_R;
    }

    private static int dH(String str) {
        int -l_1_I = 0;
        for (Object -l_5_R : Qq) {
            if (-l_5_R.equals(str)) {
                return -l_1_I;
            }
            -l_1_I++;
        }
        return -1;
    }

    private static boolean dI(String str) {
        return TextUtils.isEmpty(str) ? false : new File(str).exists();
    }

    public static sb kq() {
        if (QC == null) {
            QC = new sb();
        }
        return QC;
    }

    public void b(ISpaceScanListener iSpaceScanListener) {
        this.Qo = iSpaceScanListener;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean b(String[] strArr) {
        sb -l_2_R = this;
        synchronized (this) {
            if (this.mState == Qx) {
                this.Qw.set(false);
                this.mState = Qy;
            } else {
                return false;
            }
        }
    }

    public void c(ISpaceScanListener iSpaceScanListener) {
        this.Qp = iSpaceScanListener;
    }

    public int kr() {
        if (Qy != this.mState) {
            return -1;
        }
        this.Qw.set(true);
        return 1;
    }

    public int ks() {
        if (Qz != this.Qv) {
            return -1;
        }
        this.QB.cancel();
        this.Qv = QA;
        return 1;
    }

    public boolean w(ArrayList<sa> arrayList) {
        Object -l_2_R = SpaceManager.class;
        synchronized (SpaceManager.class) {
            if (this.Qv == QA) {
                if (this.QB == null) {
                    this.QB = new sc();
                }
                this.Qv = Qz;
                this.vW.sendMessage(this.vW.obtainMessage(4353));
                long -l_3_J = System.currentTimeMillis();
                Object -l_5_R = this.QB.a((ArrayList) arrayList, this.vW);
                String str = "PhotoManager";
                f.g(str, "Similar time consume : " + (System.currentTimeMillis() - -l_3_J) + "ms");
                return true;
            }
            return false;
        }
    }
}
