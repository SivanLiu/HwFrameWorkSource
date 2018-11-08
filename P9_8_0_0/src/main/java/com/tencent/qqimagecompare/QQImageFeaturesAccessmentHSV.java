package com.tencent.qqimagecompare;

import java.util.ArrayList;

public class QQImageFeaturesAccessmentHSV extends QQImageNativeObject {

    public enum eDimensionType {
        Sharpness,
        Lightness
    }

    private static native void AddDimensionC(long j, int i, int i2);

    private static native int GetFeaturesRankC(long j, long[] jArr, int[] iArr);

    public void addDimension(eDimensionType edimensiontype, int i) {
        int -l_3_I = 0;
        switch (edimensiontype) {
            case Sharpness:
                -l_3_I = 1;
                break;
            case Lightness:
                -l_3_I = 2;
                break;
        }
        AddDimensionC(this.mThisC, -l_3_I, i);
    }

    protected native long createNativeObject();

    protected native void destroyNativeObject(long j);

    public int[] getFeaturesRanks(ArrayList<QQImageFeatureHSV> arrayList) {
        int -l_2_I = arrayList.size();
        Object -l_3_R = new int[-l_2_I];
        Object -l_4_R = new long[-l_2_I];
        for (int -l_5_I = 0; -l_5_I < -l_2_I; -l_5_I++) {
            -l_4_R[-l_5_I] = ((QQImageFeatureHSV) arrayList.get(-l_5_I)).mThisC;
        }
        GetFeaturesRankC(this.mThisC, -l_4_R, -l_3_R);
        return -l_3_R;
    }
}
