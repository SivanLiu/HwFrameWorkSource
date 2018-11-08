package com.tencent.qqimagecompare;

import android.graphics.Bitmap;
import java.nio.ByteBuffer;

public class QQImageFeatureHSV extends QQImageNativeObject {
    private native int CompareC(long j, QQImageFeatureHSV qQImageFeatureHSV);

    private static native void FreeSerializationBufferC(ByteBuffer byteBuffer);

    private native int GetImageFeatureC(long j, Bitmap bitmap);

    private native ByteBuffer SerializationC(long j);

    private native int UnserializationC(long j, byte[] bArr);

    public int compare(QQImageFeatureHSV qQImageFeatureHSV) {
        return CompareC(this.mThisC, qQImageFeatureHSV);
    }

    protected native long createNativeObject();

    protected native void destroyNativeObject(long j);

    public int getImageFeature(Bitmap bitmap) {
        return GetImageFeatureC(this.mThisC, bitmap);
    }

    public byte[] serialization() {
        Object -l_1_R = SerializationC(this.mThisC);
        Object -l_2_R = new byte[-l_1_R.limit()];
        -l_1_R.get(-l_2_R);
        FreeSerializationBufferC(-l_1_R);
        return -l_2_R;
    }

    public int unserialization(byte[] bArr) {
        return UnserializationC(this.mThisC, bArr);
    }
}
