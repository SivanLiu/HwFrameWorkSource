package android_maps_conflict_avoidance.com.google.common.geom;

public final class Point {
    public int x;
    public int y;

    public boolean equals(Object o) {
        boolean z = false;
        if (o == null || !o.getClass().equals(getClass())) {
            return false;
        }
        Point p = (Point) o;
        if (this.x == p.x && this.y == p.y) {
            z = true;
        }
        return z;
    }

    public int hashCode() {
        return this.x + this.y;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getClass().getName());
        stringBuilder.append("[");
        stringBuilder.append(this.x);
        stringBuilder.append(",");
        stringBuilder.append(this.y);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
