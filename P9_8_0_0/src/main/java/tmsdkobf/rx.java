package tmsdkobf;

import android.graphics.Bitmap;
import com.tencent.qqimagecompare.QQImageBlur;
import com.tencent.qqimagecompare.QQImageFeatureHSV;
import com.tencent.qqimagecompare.QQImageLoader;
import tmsdk.common.utils.f;
import tmsdkobf.ry.a;

public class rx {
    private static boolean isLoaded = false;

    public static int a(QQImageFeatureHSV qQImageFeatureHSV, QQImageFeatureHSV qQImageFeatureHSV2) {
        if (!isLoaded) {
            loadLib();
        }
        try {
            return qQImageFeatureHSV.compare(qQImageFeatureHSV2);
        } catch (Object -l_2_R) {
            f.g("ImageFeatureCenter", -l_2_R);
            return 0;
        }
    }

    public static QQImageFeatureHSV a(a aVar) {
        return x(dz(aVar.mPath));
    }

    private static byte[] a(Bitmap bitmap) {
        byte[] bArr = null;
        Object -l_2_R;
        try {
            -l_2_R = new QQImageFeatureHSV();
            -l_2_R.init();
            -l_2_R.getImageFeature(bitmap);
            bitmap.recycle();
            bArr = -l_2_R.serialization();
            -l_2_R.finish();
            return bArr;
        } catch (Object -l_2_R2) {
            f.g("ImageFeatureCenter", -l_2_R2);
            return bArr;
        }
    }

    public static double detectBlur(String str) {
        if (!isLoaded) {
            loadLib();
        }
        return new QQImageBlur().detectBlur(str);
    }

    private static byte[] dz(String str) {
        if (!isLoaded) {
            loadLib();
        }
        if (isLoaded) {
            Bitmap -l_1_R;
            try {
                -l_1_R = QQImageLoader.loadBitmap100x100FromFile(str);
            } catch (Object -l_2_R) {
                f.g("ImageFeatureCenter", -l_2_R);
                -l_1_R = null;
            }
            if (-l_1_R != null) {
                return a(-l_1_R);
            }
        }
        return null;
    }

    private static synchronized void loadLib() {
        synchronized (rx.class) {
            try {
                System.loadLibrary("QQImageCompare-1.5-mfr");
                isLoaded = true;
            } catch (Object -l_1_R) {
                f.g("ImageFeatureCenter", -l_1_R);
            }
        }
    }

    public static QQImageFeatureHSV x(byte[] bArr) {
        Object -l_1_R;
        if (!isLoaded) {
            loadLib();
        }
        if (bArr != null) {
            try {
                -l_1_R = new QQImageFeatureHSV();
                -l_1_R.init();
                return -l_1_R.unserialization(bArr) == 0 ? -l_1_R : null;
            } catch (Object -l_1_R2) {
                f.g("ImageFeatureCenter", -l_1_R2);
            }
        }
        return null;
    }
}
