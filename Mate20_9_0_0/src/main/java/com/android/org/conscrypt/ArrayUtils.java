package com.android.org.conscrypt;

final class ArrayUtils {
    private ArrayUtils() {
    }

    static void checkOffsetAndCount(int arrayLength, int offset, int count) {
        if ((offset | count) < 0 || offset > arrayLength || arrayLength - offset < count) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("length=");
            stringBuilder.append(arrayLength);
            stringBuilder.append("; regionStart=");
            stringBuilder.append(offset);
            stringBuilder.append("; regionLength=");
            stringBuilder.append(count);
            throw new ArrayIndexOutOfBoundsException(stringBuilder.toString());
        }
    }
}
