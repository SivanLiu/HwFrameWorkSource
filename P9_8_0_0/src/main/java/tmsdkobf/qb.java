package tmsdkobf;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Process;
import tmsdk.common.OfflineVideo;
import tmsdk.common.utils.f;

public class qb {
    public static final String TAG = qb.class.getSimpleName();

    private static void a(OfflineVideo offlineVideo) {
        f.h(TAG, offlineVideo.mPath);
        try {
            if ("qiyi".equals(offlineVideo.mAdapter)) {
                b(offlineVideo);
                return;
            }
            if ("qqlive".equals(offlineVideo.mAdapter)) {
                c(offlineVideo);
                if (offlineVideo.mTitle != null) {
                    if (!"".equals(offlineVideo.mTitle)) {
                        return;
                    }
                }
                offlineVideo.mTitle = ct(offlineVideo.mPath);
                return;
            }
            if ("sohu".equals(offlineVideo.mAdapter)) {
                d(offlineVideo);
            }
        } catch (Object -l_1_R) {
            -l_1_R.printStackTrace();
            f.h(TAG, -l_1_R.getMessage());
        }
    }

    public static void a(String[] strArr) {
        if (strArr.length >= 1) {
            f.h(TAG, "uid " + Process.myUid());
            Object -l_1_R = strArr[0];
            Object<OfflineVideo> -l_2_R = OfflineVideo.readOfflineVideos(-l_1_R);
            if (-l_2_R != null && -l_2_R.size() != 0) {
                for (OfflineVideo -l_4_R : -l_2_R) {
                    a(-l_4_R);
                }
                OfflineVideo.dumpToFile(-l_2_R, -l_1_R);
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void b(OfflineVideo offlineVideo) {
        if (offlineVideo.mPath != null) {
            Object -l_2_R = ct(offlineVideo.mPath).split("_");
            if (-l_2_R.length == 2) {
                Object -l_3_R = -l_2_R[0];
                Object -l_4_R = -l_2_R[1];
                Object -l_5_R = SQLiteDatabase.openDatabase("/data/data/com.qiyi.video/databases/qyvideo.db", null, 0);
                if (-l_5_R != null) {
                    Cursor -l_7_R = -l_5_R.query("rc_tbl", null, "tvId = ?", new String[]{-l_4_R}, null, null, null);
                    if (-l_7_R != null) {
                        try {
                            if (-l_7_R.moveToFirst()) {
                                int -l_8_I = -l_7_R.getColumnIndex("videoPlayTime");
                                int -l_9_I = -l_7_R.getColumnIndex("videoDuration");
                                -l_7_R.moveToFirst();
                                int -l_10_I = -l_7_R.getInt(-l_8_I);
                                int -l_11_I = -l_7_R.getInt(-l_9_I);
                                if (-l_10_I != 0) {
                                    offlineVideo.mPlayProgress = -l_11_I > 0 ? (-l_10_I * 100) / -l_11_I : -1;
                                } else {
                                    offlineVideo.mPlayProgress = 100;
                                }
                            } else {
                                offlineVideo.mPlayProgress = 0;
                            }
                            -l_7_R.close();
                        } catch (Exception e) {
                        } catch (Throwable th) {
                            -l_7_R.close();
                        }
                    }
                    -l_5_R.close();
                }
            }
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void c(OfflineVideo offlineVideo) {
        if (offlineVideo.mPath != null) {
            Object -l_1_R = ct(offlineVideo.mPath);
            Object -l_2_R = SQLiteDatabase.openDatabase("/data/data/com.tencent.qqlive/databases/download_db", null, 0);
            if (-l_2_R != null) {
                Object -l_3_R = -l_2_R.query("download_db", null, "recordid = ?", new String[]{-l_1_R}, null, null, null);
                if (-l_3_R != null) {
                    Object -l_4_R;
                    try {
                        if (-l_3_R.moveToFirst()) {
                            -l_4_R = -l_3_R.getString(-l_3_R.getColumnIndex("covername"));
                            Object -l_5_R = -l_3_R.getString(-l_3_R.getColumnIndex("episodename"));
                            long -l_6_J = -l_3_R.getLong(-l_3_R.getColumnIndex("videosize"));
                            StringBuilder stringBuilder = new StringBuilder();
                            if (-l_4_R == null) {
                                -l_4_R = "";
                            }
                            stringBuilder = stringBuilder.append(-l_4_R);
                            if (-l_5_R == null) {
                                -l_5_R = "";
                            }
                            offlineVideo.mTitle = stringBuilder.append(-l_5_R).toString();
                            if ((-l_6_J <= 0 ? 1 : null) == null) {
                                offlineVideo.mDownProgress = (int) ((offlineVideo.mSize * 100) / -l_6_J);
                            } else {
                                offlineVideo.mDownProgress = -1;
                            }
                        }
                        -l_3_R.close();
                    } catch (Object -l_4_R2) {
                        f.h(TAG, "fillQQLiveVideoInfo " + -l_4_R2.getMessage());
                    } catch (Throwable th) {
                        -l_3_R.close();
                    }
                }
                -l_2_R.close();
            }
        }
    }

    private static String ct(String str) {
        int -l_1_I = str.lastIndexOf("/");
        return (-l_1_I <= 0 && -l_1_I < str.length() - 1) ? null : str.substring(-l_1_I + 1);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void d(OfflineVideo offlineVideo) {
        if (ct(offlineVideo.mPath) != null) {
            Object -l_5_R;
            Object obj = null;
            int -l_3_I = 0;
            Object -l_4_R = SQLiteDatabase.openDatabase("/data/data/com.sohu.sohuvideo/files/databases/sohutv.db", null, 0);
            if (-l_4_R != null) {
                -l_5_R = -l_4_R.query("t_videodownload", null, "save_filename = ?", new String[]{-l_1_R}, null, null, null);
                if (-l_5_R != null) {
                    try {
                        if (-l_5_R.moveToFirst()) {
                            obj = -l_5_R.getString(-l_5_R.getColumnIndex("play_id"));
                            -l_3_I = -l_5_R.getInt(-l_5_R.getColumnIndex("time_length"));
                            offlineVideo.mTitle = -l_5_R.getString(-l_5_R.getColumnIndex("vd_titile"));
                            offlineVideo.mDownProgress = -l_5_R.getInt(-l_5_R.getColumnIndex("download_percent"));
                            f.h(TAG, "" + obj + " " + -l_3_I + " " + offlineVideo.mTitle + " " + offlineVideo.mDownProgress);
                        }
                        -l_5_R.close();
                    } catch (Object -l_6_R) {
                        f.h(TAG, "fillSohuVideoInfo " + -l_6_R.getMessage());
                    } catch (Throwable th) {
                        -l_5_R.close();
                    }
                }
                -l_4_R.close();
            }
            if (obj != null && -l_3_I > 0) {
                -l_4_R = SQLiteDatabase.openDatabase("/data/data/com.sohu.sohuvideo/files/databases/other.db", null, 0);
                if (-l_4_R != null) {
                    -l_5_R = -l_4_R.query("sohu_video_history", null, "playId = ?", new String[]{obj}, null, null, null);
                    if (-l_5_R != null) {
                        try {
                            if (-l_5_R.moveToFirst()) {
                                int -l_6_I = -l_5_R.getInt(-l_5_R.getColumnIndex("playedTime"));
                                offlineVideo.mPlayProgress = (-l_6_I * 100) / -l_3_I;
                                f.h(TAG, "play time " + -l_6_I);
                            } else {
                                offlineVideo.mPlayProgress = 0;
                            }
                            -l_5_R.close();
                        } catch (Object -l_6_R2) {
                            f.h(TAG, "fillSohuVideoInfo " + -l_6_R2.getMessage());
                        } catch (Throwable th2) {
                            -l_5_R.close();
                        }
                    }
                    -l_4_R.close();
                }
            }
        }
    }
}
