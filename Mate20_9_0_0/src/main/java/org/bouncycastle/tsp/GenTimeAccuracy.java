package org.bouncycastle.tsp;

import org.bouncycastle.asn1.ASN1Integer;
import org.bouncycastle.asn1.tsp.Accuracy;

public class GenTimeAccuracy {
    private Accuracy accuracy;

    public GenTimeAccuracy(Accuracy accuracy) {
        this.accuracy = accuracy;
    }

    private String format(int i) {
        StringBuilder stringBuilder;
        String str;
        if (i < 10) {
            stringBuilder = new StringBuilder();
            str = "00";
        } else if (i >= 100) {
            return Integer.toString(i);
        } else {
            stringBuilder = new StringBuilder();
            str = "0";
        }
        stringBuilder.append(str);
        stringBuilder.append(i);
        return stringBuilder.toString();
    }

    private int getTimeComponent(ASN1Integer aSN1Integer) {
        return aSN1Integer != null ? aSN1Integer.getValue().intValue() : 0;
    }

    public int getMicros() {
        return getTimeComponent(this.accuracy.getMicros());
    }

    public int getMillis() {
        return getTimeComponent(this.accuracy.getMillis());
    }

    public int getSeconds() {
        return getTimeComponent(this.accuracy.getSeconds());
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getSeconds());
        stringBuilder.append(".");
        stringBuilder.append(format(getMillis()));
        stringBuilder.append(format(getMicros()));
        return stringBuilder.toString();
    }
}
