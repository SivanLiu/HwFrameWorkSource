package tmsdkobf;

import java.util.HashMap;

public class ov extends ib {
    private HashMap<String, Object> Jb = new HashMap();

    private String e(Object obj) {
        if (obj != null) {
            return !(obj instanceof String) ? obj.toString() : (String) obj;
        } else {
            return null;
        }
    }

    public void P(boolean z) {
        this.Jb.put("isApk", Boolean.valueOf(z));
    }

    public void cm(String str) {
        this.Jb.put("pkgName", str);
    }

    public void cn(String str) {
        this.Jb.put("apkPath", str);
    }

    public Object get(String str) {
        return this.Jb.get(str);
    }

    public String getAppName() {
        return e(this.Jb.get("appName"));
    }

    public String getPackageName() {
        return e(this.Jb.get("pkgName"));
    }

    public long getSize() {
        Object -l_1_R = this.Jb.get("size");
        return -l_1_R == null ? 0 : ((Long) -l_1_R).longValue();
    }

    public int getUid() {
        Object -l_1_R = this.Jb.get("uid");
        return -l_1_R == null ? 0 : ((Integer) -l_1_R).intValue();
    }

    public String getVersion() {
        return e(this.Jb.get("version"));
    }

    public int getVersionCode() {
        Object -l_1_R = this.Jb.get("versionCode");
        return -l_1_R == null ? 0 : ((Integer) -l_1_R).intValue();
    }

    public String[] hA() {
        Object -l_1_R = this.Jb.get("permissions");
        return -l_1_R == null ? null : (String[]) -l_1_R;
    }

    public String hB() {
        return e(this.Jb.get("apkPath"));
    }

    public boolean hC() {
        Object -l_1_R = this.Jb.get("isApk");
        return -l_1_R == null ? false : ((Boolean) -l_1_R).booleanValue();
    }

    public boolean hD() {
        Object -l_1_R = this.Jb.get("installedOnSdcard");
        return -l_1_R == null ? false : ((Boolean) -l_1_R).booleanValue();
    }

    public boolean hx() {
        Object -l_1_R = this.Jb.get("isSystem");
        return -l_1_R == null ? false : ((Boolean) -l_1_R).booleanValue();
    }

    public long hy() {
        Object -l_1_R = this.Jb.get("lastModified");
        return -l_1_R == null ? 0 : ((Long) -l_1_R).longValue();
    }

    public String hz() {
        return e(this.Jb.get("signatureCermMD5"));
    }

    public void put(String str, Object obj) {
        this.Jb.put(str, obj);
    }

    public void setAppName(String str) {
        this.Jb.put("appName", str);
    }
}
