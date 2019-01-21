package com.huawei.zxing.oned.rss;

public class DataCharacter {
    private final int checksumPortion;
    private final int value;

    public DataCharacter(int value, int checksumPortion) {
        this.value = value;
        this.checksumPortion = checksumPortion;
    }

    public final int getValue() {
        return this.value;
    }

    public final int getChecksumPortion() {
        return this.checksumPortion;
    }

    public final String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.value);
        stringBuilder.append("(");
        stringBuilder.append(this.checksumPortion);
        stringBuilder.append(')');
        return stringBuilder.toString();
    }

    public final boolean equals(Object o) {
        boolean z = false;
        if (!(o instanceof DataCharacter)) {
            return false;
        }
        DataCharacter that = (DataCharacter) o;
        if (this.value == that.value && this.checksumPortion == that.checksumPortion) {
            z = true;
        }
        return z;
    }

    public final int hashCode() {
        return this.value ^ this.checksumPortion;
    }
}
