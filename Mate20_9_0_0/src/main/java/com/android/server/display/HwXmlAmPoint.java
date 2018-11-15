package com.android.server.display;

public class HwXmlAmPoint {
    final float x;
    final float y;
    final float z;

    public HwXmlAmPoint(float inx, float iny, float inz) {
        this.x = inx;
        this.y = iny;
        this.z = inz;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Point(");
        stringBuilder.append(this.x);
        stringBuilder.append(", ");
        stringBuilder.append(this.y);
        stringBuilder.append(", ");
        stringBuilder.append(this.z);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }
}
