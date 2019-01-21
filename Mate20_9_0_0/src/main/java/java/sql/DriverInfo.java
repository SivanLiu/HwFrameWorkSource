package java.sql;

/* compiled from: DriverManager */
class DriverInfo {
    final Driver driver;

    DriverInfo(Driver driver) {
        this.driver = driver;
    }

    public boolean equals(Object other) {
        return (other instanceof DriverInfo) && this.driver == ((DriverInfo) other).driver;
    }

    public int hashCode() {
        return this.driver.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("driver[className=");
        stringBuilder.append(this.driver);
        stringBuilder.append("]");
        return stringBuilder.toString();
    }
}
