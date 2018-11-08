package tmsdkobf;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;

public class gd {
    public static void b(String str, String str2) throws Exception {
        Object -l_2_R = new ZipFile(str);
        Object -l_3_R = -l_2_R.entries();
        Object -l_10_R = new byte[IncomingSmsFilterConsts.PAY_SMS];
        while (-l_3_R.hasMoreElements()) {
            ZipEntry -l_9_R = (ZipEntry) -l_3_R.nextElement();
            if (-l_9_R.isDirectory()) {
                new File(str2, -l_9_R.getName()).mkdirs();
            } else {
                Object -l_4_R = new BufferedInputStream(-l_2_R.getInputStream(-l_9_R));
                Object -l_7_R = new File(str2, -l_9_R.getName());
                Object -l_8_R = -l_7_R.getParentFile();
                if (!(-l_8_R == null || -l_8_R.exists())) {
                    -l_8_R.mkdirs();
                }
                gc.c("ZipUtils", "file " + -l_7_R.getAbsolutePath());
                Object -l_5_R = new FileOutputStream(-l_7_R);
                Object -l_6_R = new BufferedOutputStream(-l_5_R, IncomingSmsFilterConsts.PAY_SMS);
                while (true) {
                    int -l_11_I = -l_4_R.read(-l_10_R, 0, IncomingSmsFilterConsts.PAY_SMS);
                    if (-l_11_I == -1) {
                        break;
                    }
                    -l_5_R.write(-l_10_R, 0, -l_11_I);
                }
                -l_6_R.flush();
                -l_6_R.close();
                -l_5_R.close();
                -l_4_R.close();
            }
        }
        -l_2_R.close();
    }
}
