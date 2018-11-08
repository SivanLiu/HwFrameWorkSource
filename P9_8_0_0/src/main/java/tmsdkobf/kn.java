package tmsdkobf;

import android.os.Environment;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

public class kn {
    private static final String[] tJ = new String[]{"MI 2"};

    private static boolean a(ArrayList<String> arrayList, String str) {
        Object -l_2_R = arrayList.iterator();
        while (-l_2_R.hasNext()) {
            String -l_3_R = (String) -l_2_R.next();
            if (str.equals(-l_3_R)) {
                return true;
            }
            int -l_4_I = 0;
            Object -l_5_R;
            try {
                -l_5_R = new File(-l_3_R).getCanonicalPath();
                Object -l_6_R = new File(str).getCanonicalPath();
                if (!(-l_5_R == null || -l_6_R == null)) {
                    -l_4_I = -l_5_R.equals(-l_6_R);
                    continue;
                }
            } catch (Object -l_5_R2) {
                -l_5_R2.printStackTrace();
                continue;
            }
            if (-l_4_I != 0) {
                return -l_4_I;
            }
        }
        return false;
    }

    public static ArrayList<String> cM() {
        Object -l_2_R;
        Object -l_5_R;
        Object -l_0_R = new ArrayList();
        if (kl.cK() == 0) {
            Object -l_1_R = Environment.getExternalStorageDirectory().getAbsolutePath();
            if (-l_1_R != null) {
                -l_0_R.add(-l_1_R);
            }
        }
        BufferedReader bufferedReader = null;
        try {
            Object -l_3_R;
            BufferedReader -l_1_R2 = new BufferedReader(new FileReader("/proc/mounts"));
            while (true) {
                try {
                    -l_2_R = -l_1_R2.readLine();
                    if (-l_2_R == null) {
                        break;
                    }
                    if (!-l_2_R.contains("vfat")) {
                        if (!(-l_2_R.contains("exfat") || -l_2_R.contains("/mnt") || -l_2_R.contains("fuse"))) {
                        }
                    }
                    -l_3_R = -l_2_R.split("\\s+");
                    if (-l_3_R[1].equals(Environment.getExternalStorageDirectory().getPath())) {
                        if (!a(-l_0_R, -l_3_R[1])) {
                            -l_0_R.add(-l_3_R[1]);
                        }
                    } else if (!(!-l_2_R.contains("/dev/block/vold") || -l_2_R.contains("/mnt/secure") || -l_2_R.contains("/mnt/asec") || -l_2_R.contains("/mnt/obb") || -l_2_R.contains("/dev/mapper") || -l_2_R.contains("tmpfs") || a(-l_0_R, -l_3_R[1]))) {
                        -l_0_R.add(-l_3_R[1]);
                    }
                } catch (Exception e) {
                    -l_2_R = e;
                    bufferedReader = -l_1_R2;
                } catch (Throwable th) {
                    -l_5_R = th;
                    bufferedReader = -l_1_R2;
                }
            }
            n(-l_0_R);
            -l_3_R = -l_0_R;
            if (-l_1_R2 != null) {
                try {
                    -l_1_R2.close();
                } catch (Object -l_4_R) {
                    -l_4_R.printStackTrace();
                }
            }
            return -l_0_R;
        } catch (Exception e2) {
            -l_2_R = e2;
            try {
                -l_2_R.printStackTrace();
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (Object -l_2_R2) {
                        -l_2_R2.printStackTrace();
                    }
                }
                return -l_0_R;
            } catch (Throwable th2) {
                -l_5_R = th2;
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (Object -l_6_R) {
                        -l_6_R.printStackTrace();
                    }
                }
                throw -l_5_R;
            }
        }
    }

    private static void n(ArrayList<String> arrayList) {
        if (arrayList != null && arrayList.size() > 0) {
            for (int -l_1_I = 0; -l_1_I < arrayList.size(); -l_1_I++) {
                while (((String) arrayList.get(-l_1_I)).endsWith("/")) {
                    arrayList.set(-l_1_I, ((String) arrayList.get(-l_1_I)).substring(0, ((String) arrayList.get(-l_1_I)).length() - 1));
                }
            }
        }
    }
}
