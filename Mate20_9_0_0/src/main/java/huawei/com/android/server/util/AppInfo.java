package huawei.com.android.server.util;

public class AppInfo {
    private String appName;
    private int count;
    private String packageName;

    public AppInfo(String packageName) {
        this.packageName = packageName;
    }

    public String getPackageName() {
        return this.packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public int getCount() {
        return this.count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public static String exitReson(String packageName, String backreson) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PN:");
        stringBuilder.append(packageName);
        stringBuilder.append(",REASON:");
        stringBuilder.append(backreson);
        return stringBuilder.toString();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PN:");
        stringBuilder.append(this.packageName);
        stringBuilder.append(",COUNT:");
        stringBuilder.append(this.count);
        return stringBuilder.toString();
    }
}
