package tmsdkobf;

import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import tmsdk.common.utils.s;
import tmsdk.fg.creator.BaseManagerF;
import tmsdk.fg.module.spacemanager.ISpaceScanListener;
import tmsdk.fg.module.spacemanager.PhotoScanResult;
import tmsdk.fg.module.spacemanager.PhotoScanResult.PhotoItem;
import tmsdk.fg.module.spacemanager.PhotoSimilarResult;
import tmsdk.fg.module.spacemanager.PhotoSimilarResult.PhotoSimilarBucketItem;
import tmsdkobf.ry.a;

public class ri extends BaseManagerF {
    sd Pn;
    private sb Po = sb.kq();

    public ri() {
        s.bW(16);
    }

    public static ArrayList<sa> H(List<PhotoItem> list) {
        Object -l_1_R = new ArrayList();
        int -l_2_I = list.size();
        for (int -l_3_I = 0; -l_3_I < -l_2_I; -l_3_I++) {
            PhotoItem -l_4_R = (PhotoItem) list.get(-l_3_I);
            -l_1_R.add(new sa(-l_4_R.mTime, -l_4_R.mSize, -l_4_R.mPath, -l_4_R.mDbId));
        }
        return -l_1_R;
    }

    public static List<PhotoSimilarResult> I(List<ry> list) {
        Object -l_1_R = new ArrayList();
        int -l_2_I = list.size();
        for (int -l_3_I = 0; -l_3_I < -l_2_I; -l_3_I++) {
            ry -l_4_R = (ry) list.get(-l_3_I);
            Object -l_5_R = new PhotoSimilarResult();
            -l_5_R.mItemList = new ArrayList();
            -l_5_R.mTime = -l_4_R.mTime;
            -l_5_R.mTimeString = -l_4_R.mTimeString;
            int -l_6_I = -l_4_R.mItemList.size();
            for (int -l_7_I = 0; -l_7_I < -l_6_I; -l_7_I++) {
                Object -l_8_R = new PhotoSimilarBucketItem();
                -l_8_R.mId = ((a) -l_4_R.mItemList.get(-l_7_I)).mDbId;
                -l_8_R.mPath = ((a) -l_4_R.mItemList.get(-l_7_I)).mPath;
                -l_8_R.mFileSize = ((a) -l_4_R.mItemList.get(-l_7_I)).mSize;
                -l_8_R.mSelected = ((a) -l_4_R.mItemList.get(-l_7_I)).mSelected;
                -l_5_R.mItemList.add(-l_8_R);
            }
            -l_1_R.add(-l_5_R);
        }
        return -l_1_R;
    }

    public static PhotoScanResult a(sb.a aVar) {
        Object -l_1_R = new PhotoScanResult();
        -l_1_R.mInnerPicSize = aVar.mInnerPicSize;
        -l_1_R.mOutPicSize = aVar.mOutPicSize;
        -l_1_R.mPhotoCountAndSize = new Pair(Integer.valueOf(((Integer) aVar.mPhotoCountAndSize.first).intValue()), Long.valueOf(((Long) aVar.mPhotoCountAndSize.second).longValue()));
        -l_1_R.mScreenShotCountAndSize = new Pair(Integer.valueOf(((Integer) aVar.mScreenShotCountAndSize.first).intValue()), Long.valueOf(((Long) aVar.mScreenShotCountAndSize.second).longValue()));
        -l_1_R.mResultList = new ArrayList();
        int -l_2_I = aVar.mResultList.size();
        for (int -l_3_I = 0; -l_3_I < -l_2_I; -l_3_I++) {
            sa -l_4_R = (sa) aVar.mResultList.get(-l_3_I);
            Object -l_5_R = new PhotoItem();
            -l_5_R.mDbId = -l_4_R.mDbId;
            -l_5_R.mIsOut = -l_4_R.mIsOut;
            -l_5_R.mIsScreenShot = -l_4_R.mIsScreenShot;
            -l_5_R.mPath = -l_4_R.mPath;
            -l_5_R.mSize = -l_4_R.mSize;
            -l_5_R.mTime = -l_4_R.mTime;
            -l_1_R.mResultList.add(-l_5_R);
        }
        return -l_1_R;
    }

    public boolean a(ISpaceScanListener iSpaceScanListener) {
        if (this.Pn != null) {
            return false;
        }
        this.Pn = new sd(this);
        if (!this.Pn.a(iSpaceScanListener)) {
            return false;
        }
        kt.saveActionData(29990);
        return true;
    }

    public boolean a(ISpaceScanListener iSpaceScanListener, List<PhotoItem> list) {
        this.Po.c(iSpaceScanListener);
        int -l_3_I = this.Po.w(H(list));
        kt.saveActionData(29993);
        return -l_3_I;
    }

    public boolean a(ISpaceScanListener iSpaceScanListener, String[] strArr) {
        this.Po.b(iSpaceScanListener);
        int -l_3_I = this.Po.b(strArr);
        kt.saveActionData(29992);
        return -l_3_I;
    }

    public void bX(int i) {
        switch (i) {
            case 0:
                this.Pn = null;
                return;
            default:
                return;
        }
    }

    public double detectBlur(String str) {
        if (TextUtils.isEmpty(str) || !new File(str).exists()) {
            return -1.0d;
        }
        Object -l_3_R = new rx();
        return rx.detectBlur(str);
    }

    public void kf() {
        if (this.Pn != null) {
            this.Pn.kv();
        }
    }

    public void kg() {
        if (this.Po != null) {
            this.Po.b(null);
            this.Po.c(null);
        }
    }

    public void onCreate(Context context) {
    }

    public void onDestory() {
        if (this.Pn != null) {
            this.Pn.kv();
        }
    }

    public int stopPhotoScan() {
        return this.Po.kr();
    }

    public int stopPhotoSimilarCategorise() {
        return this.Po.ks();
    }
}
