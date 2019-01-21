package com.android.internal.telephony.cat;

public class ImageDescriptor {
    static final int CODING_SCHEME_BASIC = 17;
    static final int CODING_SCHEME_COLOUR = 33;
    int mCodingScheme = 0;
    int mHeight = 0;
    int mHighOffset = 0;
    int mImageId = 0;
    int mLength = 0;
    int mLowOffset = 0;
    int mWidth = 0;

    ImageDescriptor() {
    }

    static ImageDescriptor parse(byte[] rawData, int valueIndex) {
        ImageDescriptor d = new ImageDescriptor();
        int valueIndex2 = valueIndex + 1;
        int valueIndex3;
        try {
            d.mWidth = rawData[valueIndex] & 255;
            valueIndex = valueIndex2 + 1;
            try {
                d.mHeight = rawData[valueIndex2] & 255;
                valueIndex2 = valueIndex + 1;
                d.mCodingScheme = rawData[valueIndex] & 255;
                valueIndex = valueIndex2 + 1;
                d.mImageId = (rawData[valueIndex2] & 255) << 8;
                valueIndex3 = valueIndex + 1;
                try {
                    d.mImageId = (rawData[valueIndex] & 255) | d.mImageId;
                    valueIndex2 = valueIndex3 + 1;
                    d.mHighOffset = rawData[valueIndex3] & 255;
                    valueIndex = valueIndex2 + 1;
                    d.mLowOffset = rawData[valueIndex2] & 255;
                    valueIndex2 = valueIndex + 1;
                    valueIndex3 = valueIndex2 + 1;
                    d.mLength = ((rawData[valueIndex] & 255) << 8) | (rawData[valueIndex2] & 255);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("parse; Descriptor : ");
                    stringBuilder.append(d.mWidth);
                    stringBuilder.append(", ");
                    stringBuilder.append(d.mHeight);
                    stringBuilder.append(", ");
                    stringBuilder.append(d.mCodingScheme);
                    stringBuilder.append(", 0x");
                    stringBuilder.append(Integer.toHexString(d.mImageId));
                    stringBuilder.append(", ");
                    stringBuilder.append(d.mHighOffset);
                    stringBuilder.append(", ");
                    stringBuilder.append(d.mLowOffset);
                    stringBuilder.append(", ");
                    stringBuilder.append(d.mLength);
                    CatLog.d("ImageDescriptor", stringBuilder.toString());
                    return d;
                } catch (IndexOutOfBoundsException e) {
                }
            } catch (IndexOutOfBoundsException e2) {
                valueIndex3 = valueIndex;
                valueIndex = e2;
                CatLog.d("ImageDescriptor", "parse; failed parsing image descriptor");
                return null;
            }
        } catch (IndexOutOfBoundsException e3) {
            valueIndex3 = valueIndex2;
            CatLog.d("ImageDescriptor", "parse; failed parsing image descriptor");
            return null;
        }
    }
}
