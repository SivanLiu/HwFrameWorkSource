package tmsdkobf;

import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import tmsdk.common.OfflineVideo;
import tmsdk.common.utils.f;

public class ru extends rn {
    final boolean PY;
    private Pattern PZ = null;

    public ru(boolean z) {
        this.PY = z;
    }

    private void b(String str, List<OfflineVideo> list) {
        Object -l_13_R;
        Object -l_4_R = dx(rh.di(str));
        if (!TextUtils.isEmpty(-l_4_R)) {
            Object -l_5_R = new File(str).list();
            if (-l_5_R != null) {
                Object -l_6_R = -l_5_R;
                for (Object -l_9_R : -l_5_R) {
                    Object -l_10_R = dy(-l_9_R);
                    if (-l_10_R != null) {
                        Object -l_11_R = new OfflineVideo();
                        -l_11_R.mPath = str + "/" + -l_9_R;
                        -l_11_R.mTitle = !this.PY ? -l_4_R + "(第" + -l_10_R + "集)" : -l_4_R + "(Episode " + -l_10_R + ")";
                        -l_11_R.mSize = rq.dq(-l_11_R.mPath);
                        Object -l_12_R = new File(-l_11_R.mPath).list();
                        if (-l_12_R != null) {
                            -l_13_R = -l_12_R;
                            for (Object -l_16_R : -l_12_R) {
                                if (-l_16_R.endsWith(".storm")) {
                                    -l_11_R.mThumnbailPath = -l_11_R.mPath + "/" + -l_16_R;
                                    break;
                                }
                            }
                        }
                        try {
                            f(-l_11_R);
                        } catch (Object -l_13_R2) {
                            f.h("PiDeepClean", -l_13_R2.getMessage());
                        }
                        list.add(-l_11_R);
                    }
                }
            }
        }
    }

    private String dx(String str) {
        if (this.PZ == null) {
            this.PZ = Pattern.compile("[0-9]+-[0-9]+-(.*)");
        }
        Object -l_2_R = this.PZ.matcher(str);
        return !-l_2_R.find() ? null : -l_2_R.group(1);
    }

    private String dy(String str) {
        String str2 = null;
        Object -l_2_R = str.split("-");
        if (-l_2_R == null || -l_2_R.length != 2) {
            return null;
        }
        if (-l_2_R[0].length() != 0) {
            str2 = -l_2_R[0];
        }
        return str2;
    }

    private void f(OfflineVideo offlineVideo) {
        Object -l_3_R = SQLiteDatabase.openDatabase(offlineVideo.mPath.replaceFirst("\\.download/.*", ".database/bfdownload.db"), null, 0);
        if (-l_3_R != null) {
            Object -l_4_R = -l_3_R.query("downloadtable", null, "local_file_path = ?", new String[]{offlineVideo.mPath}, null, null, null);
            if (-l_4_R != null) {
                try {
                    if (-l_4_R.moveToFirst()) {
                        long -l_5_J = -l_4_R.getLong(-l_4_R.getColumnIndex("total_size"));
                        long -l_7_J = -l_4_R.getLong(-l_4_R.getColumnIndex("downloaded_size"));
                        if ((-l_5_J <= 0 ? 1 : null) == null) {
                            offlineVideo.mDownProgress = (int) ((100 * -l_7_J) / -l_5_J);
                        } else {
                            offlineVideo.mDownProgress = -1;
                        }
                    }
                    if (-l_4_R != null) {
                        -l_4_R.close();
                    }
                } catch (Exception e) {
                    if (-l_4_R != null) {
                        -l_4_R.close();
                    }
                } catch (Throwable th) {
                    if (-l_4_R != null) {
                        -l_4_R.close();
                    }
                }
            }
            -l_3_R.close();
        }
    }

    public List<OfflineVideo> a(ro roVar) {
        Object<String> -l_2_R = rq.dp(roVar.Ok);
        if (-l_2_R.size() == 0) {
            return null;
        }
        Object -l_3_R = new ArrayList();
        for (String -l_5_R : -l_2_R) {
            b(-l_5_R, -l_3_R);
        }
        return -l_3_R;
    }
}
