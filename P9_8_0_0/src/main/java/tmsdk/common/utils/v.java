package tmsdk.common.utils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public final class v {
    private byte[] Mh = new byte[16384];

    private void a(ZipFile zipFile, ZipEntry zipEntry) throws IOException {
        InputStream inputStream = null;
        try {
            Object -l_3_R = zipFile.getInputStream(zipEntry);
            do {
            } while (-1 != -l_3_R.read(this.Mh));
            if (-l_3_R != null) {
                -l_3_R.close();
            }
        } catch (Throwable th) {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    private void cP(String str) throws IOException {
        Object -l_11_R;
        ZipFile zipFile = null;
        try {
            long -l_3_J = new File(str).length();
            ZipFile -l_2_R = new ZipFile(str);
            try {
                Object -l_5_R = -l_2_R.entries();
                while (-l_5_R.hasMoreElements()) {
                    ZipEntry -l_6_R = (ZipEntry) -l_5_R.nextElement();
                    if (!-l_6_R.isDirectory()) {
                        long -l_7_J = -l_6_R.getCompressedSize();
                        long -l_9_J = -l_6_R.getSize();
                        if ((-l_7_J < -1 ? 1 : null) == null) {
                            if ((-l_7_J > -l_3_J ? 1 : null) == null) {
                                if ((-l_9_J < -1 ? 1 : null) == null) {
                                    if ((-l_9_J <= 2500 * -l_7_J ? 1 : null) != null) {
                                        if (-l_6_R.getName() != null && -l_6_R.getName().contains("AndroidManifest.xml")) {
                                            if (-l_7_J == 0 || -l_9_J == 0) {
                                                throw new RuntimeException("Invalid AndroidManifest!");
                                            }
                                            try {
                                                a(-l_2_R, -l_6_R);
                                                if (-l_2_R != null) {
                                                    -l_2_R.close();
                                                }
                                                return;
                                            } catch (Throwable th) {
                                                -l_11_R = th;
                                                zipFile = -l_2_R;
                                                if (zipFile != null) {
                                                    zipFile.close();
                                                }
                                                throw -l_11_R;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        f.e("ZipChecker", " fileName :: " + str);
                        throw new RuntimeException("Invalid entry size!");
                    }
                }
                if (-l_2_R != null) {
                    -l_2_R.close();
                }
            } catch (Throwable th2) {
                -l_11_R = th2;
                zipFile = -l_2_R;
            }
        } catch (Throwable th3) {
            -l_11_R = th3;
            if (zipFile != null) {
                zipFile.close();
            }
            throw -l_11_R;
        }
    }

    public synchronized boolean cO(String str) {
        try {
            cP(str);
        } catch (Object -l_2_R) {
            f.b("ZipChecker", "check", -l_2_R);
            return false;
        }
        return true;
    }
}
