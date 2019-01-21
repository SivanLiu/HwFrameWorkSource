package android.graphics;

import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class PointF implements Parcelable {
    public static final Creator<PointF> CREATOR = new Creator<PointF>() {
        public PointF createFromParcel(Parcel in) {
            PointF r = new PointF();
            r.readFromParcel(in);
            return r;
        }

        public PointF[] newArray(int size) {
            return new PointF[size];
        }
    };
    public float x;
    public float y;

    public PointF(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public PointF(Point p) {
        this.x = (float) p.x;
        this.y = (float) p.y;
    }

    public final void set(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public final void set(PointF p) {
        this.x = p.x;
        this.y = p.y;
    }

    public final void negate() {
        this.x = -this.x;
        this.y = -this.y;
    }

    public final void offset(float dx, float dy) {
        this.x += dx;
        this.y += dy;
    }

    public final boolean equals(float x, float y) {
        return this.x == x && this.y == y;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PointF pointF = (PointF) o;
        if (Float.compare(pointF.x, this.x) == 0 && Float.compare(pointF.y, this.y) == 0) {
            return true;
        }
        return false;
    }

    public int hashCode() {
        int i = 0;
        int result = 31 * (this.x != 0.0f ? Float.floatToIntBits(this.x) : 0);
        if (this.y != 0.0f) {
            i = Float.floatToIntBits(this.y);
        }
        return result + i;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PointF(");
        stringBuilder.append(this.x);
        stringBuilder.append(", ");
        stringBuilder.append(this.y);
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    public final float length() {
        return length(this.x, this.y);
    }

    public static float length(float x, float y) {
        return (float) Math.hypot((double) x, (double) y);
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeFloat(this.x);
        out.writeFloat(this.y);
    }

    public void readFromParcel(Parcel in) {
        this.x = in.readFloat();
        this.y = in.readFloat();
    }
}
