package tmsdkobf;

import android.content.Context;
import android.provider.MediaStore.Audio.Media;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import tmsdk.common.tcc.DeepCleanEngine;
import tmsdk.common.tcc.DeepCleanEngine.Callback;
import tmsdk.common.tcc.QFile;
import tmsdk.common.tcc.SdcardScannerFactory;
import tmsdk.common.utils.q;
import tmsdk.fg.module.spacemanager.FileInfo;
import tmsdk.fg.module.spacemanager.FileMedia;
import tmsdk.fg.module.spacemanager.FileScanResult;

public class sf implements Callback {
    private DeepCleanEngine Pa;
    boolean QS = false;
    private int QT = 0;
    int QU = 0;
    int QV = 0;
    HashMap<String, qt> QW = new HashMap();
    HashMap<String, qt> QX = new HashMap();
    HashMap<String, qt> QY = new HashMap();
    List<String> QZ = new ArrayList();
    List<qt> Ra = new ArrayList();
    FileScanResult Rb = new FileScanResult();
    a Rc;
    int Rd = 7;
    Context mContext;

    public interface a {
        void a(long j, Object obj);

        void onProgressChanged(int i);
    }

    public sf(Context context, a aVar, int i) {
        this.mContext = context;
        this.Rc = aVar;
        this.Rd = i;
    }

    private static void R(List<FileMedia> list) {
        if (list != null) {
            Collections.sort(list, new Comparator<FileMedia>() {
                public int a(FileMedia fileMedia, FileMedia fileMedia2) {
                    int i = 1;
                    int i2 = 0;
                    if (fileMedia.mSize == fileMedia2.mSize) {
                        return 0;
                    }
                    if (fileMedia.mSize >= fileMedia2.mSize) {
                        i2 = 1;
                    }
                    if (i2 != 0) {
                        i = -1;
                    }
                    return i;
                }

                public /* synthetic */ int compare(Object obj, Object obj2) {
                    return a((FileMedia) obj, (FileMedia) obj2);
                }
            });
        }
    }

    private static void S(List<FileMedia> list) {
        if (list != null) {
            R(list);
            Object -l_1_R = new ArrayList();
            Object -l_2_R = new HashMap();
            for (FileMedia -l_4_R : list) {
                if (q.cK(-l_4_R.pkg)) {
                    -l_1_R.add(-l_4_R);
                } else {
                    Object -l_5_R = (ArrayList) -l_2_R.get(-l_4_R.pkg);
                    if (-l_5_R == null) {
                        -l_5_R = new ArrayList();
                        -l_2_R.put(-l_4_R.pkg, -l_5_R);
                    }
                    -l_5_R.add(-l_4_R);
                }
            }
            Object -l_3_R = new ArrayList();
            -l_3_R.addAll(-l_2_R.values());
            Collections.sort(-l_3_R, new Comparator<ArrayList<FileMedia>>() {
                public int c(ArrayList<FileMedia> arrayList, ArrayList<FileMedia> arrayList2) {
                    if (arrayList.size() == arrayList2.size()) {
                        return 0;
                    }
                    return arrayList.size() <= arrayList2.size() ? 1 : -1;
                }

                public /* synthetic */ int compare(Object obj, Object obj2) {
                    return c((ArrayList) obj, (ArrayList) obj2);
                }
            });
            list.clear();
            Object -l_4_R2 = -l_3_R.iterator();
            while (-l_4_R2.hasNext()) {
                list.addAll((ArrayList) -l_4_R2.next());
            }
            list.addAll(-l_1_R);
        }
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private static void a(FileMedia fileMedia, Context context) {
        Object -l_2_R = new String[]{"title", "artist", "album"};
        Object -l_4_R = new String[]{fileMedia.mPath};
        Object -l_5_R = context.getContentResolver().query(Media.EXTERNAL_CONTENT_URI, -l_2_R, "_data = ?", -l_4_R, "");
        if (-l_5_R != null) {
            try {
                if (-l_5_R.moveToFirst()) {
                    fileMedia.title = -l_5_R.getString(-l_5_R.getColumnIndex("title"));
                    fileMedia.artist = -l_5_R.getString(-l_5_R.getColumnIndex("artist"));
                    fileMedia.album = -l_5_R.getString(-l_5_R.getColumnIndex("album"));
                    if (fileMedia.artist != null) {
                        if (fileMedia.artist.contains("unknown")) {
                            fileMedia.artist = null;
                        }
                    }
                    if (fileMedia.album != null && fileMedia.album.contains("unknown")) {
                        fileMedia.album = null;
                    }
                }
                -l_5_R.close();
            } catch (Exception e) {
            } catch (Throwable th) {
                -l_5_R.close();
            }
        }
    }

    public static void a(FileScanResult fileScanResult, Context context, boolean z) {
        Object -l_3_R = new rl();
        -l_3_R.kj();
        if (fileScanResult.mBigFiles != null) {
            for (FileInfo -l_5_R : fileScanResult.mBigFiles) {
                -l_5_R.mSrcName = -l_3_R.a(rk.a(-l_5_R.mPath, rl.Ps).toLowerCase(), null, z);
            }
        }
        if (fileScanResult.mRadioFiles != null) {
            for (FileMedia -l_5_R2 : fileScanResult.mRadioFiles) {
                Object -l_6_R = rk.a(-l_5_R2.mPath, rl.Ps).toLowerCase();
                -l_5_R2.pkg = -l_3_R.k(-l_6_R, z);
                -l_5_R2.mSrcName = -l_3_R.l(-l_6_R, z);
                a(-l_5_R2, context);
            }
            S(fileScanResult.mRadioFiles);
        }
        if (fileScanResult.mVideoFiles != null) {
            for (FileMedia -l_5_R22 : fileScanResult.mVideoFiles) {
                -l_6_R = rk.a(-l_5_R22.mPath, rl.Ps).toLowerCase();
                -l_5_R22.pkg = -l_3_R.k(-l_6_R, z);
                -l_5_R22.mSrcName = -l_3_R.l(-l_6_R, z);
            }
            S(fileScanResult.mVideoFiles);
        }
    }

    private void b(long j, Object obj) {
        if (this.Rc != null) {
            this.Rc.a(j, obj);
        }
    }

    private boolean kx() {
        Object -l_1_R = new se();
        if (-l_1_R.U(this.mContext) == 0) {
            return false;
        }
        Object -l_4_R;
        if (-l_1_R.QO != null) {
            this.QZ.addAll(-l_1_R.QO);
        }
        Object<tmsdkobf.se.a> -l_3_R = -l_1_R.QR;
        if (-l_3_R != null) {
            for (tmsdkobf.se.a -l_5_R : -l_3_R) {
                if (q.cJ(-l_5_R.Ok)) {
                    this.QZ.add(-l_5_R.Ok);
                }
            }
        }
        if (!((this.Rd & 1) == 0 || -l_1_R.QP == null)) {
            for (qt -l_5_R2 : -l_1_R.QP) {
                this.Ra.add(-l_5_R2);
                this.QW.put(-l_5_R2.Oj, -l_5_R2);
            }
        }
        if (!((this.Rd & 2) == 0 || -l_1_R.QQ == null)) {
            for (qt -l_5_R22 : -l_1_R.QQ) {
                this.Ra.add(-l_5_R22);
                this.QX.put(-l_5_R22.Oj, -l_5_R22);
            }
        }
        if ((this.Rd & 4) != 0) {
            -l_4_R = new qt();
            -l_4_R.Ol = "10240,-";
            -l_4_R.Op = "0";
            if ((this.Rd & 256) != 0) {
                -l_4_R.mFileName = "/\\.(zip|rar|pdf|doc|apk|ppt|txt|log|chm|docx|pptx|iso|7z|tar|gz)";
            }
            this.Ra.add(-l_4_R);
            this.QY.put(-l_4_R.Oj, -l_4_R);
        }
        return true;
    }

    private void ky() {
        this.QZ.addAll(rk.kh());
        Object -l_1_R = new String[this.QZ.size()];
        this.QZ.toArray(-l_1_R);
        this.Pa.setWhitePaths(-l_1_R);
        Object -l_2_R = new String[this.Ra.size()];
        Object -l_3_R = new StringBuilder();
        for (int -l_4_I = 0; -l_4_I < -l_2_R.length; -l_4_I++) {
            qt.a(-l_3_R, (qt) this.Ra.get(-l_4_I));
            -l_2_R[-l_4_I] = -l_3_R.toString();
            -l_3_R.setLength(0);
        }
        this.Pa.setComRubRule(-l_2_R);
        Object<String> -l_4_R = rk.jZ();
        Object -l_5_R = rk.ki();
        if (-l_5_R != null) {
            this.QT = -l_5_R.size();
            if (-l_4_R != null) {
                for (String -l_7_R : -l_4_R) {
                    if (!kA()) {
                        this.Pa.scanPath(-l_7_R, "/");
                        this.QU += this.QV;
                    }
                }
            }
        }
    }

    public FileScanResult Z(boolean z) {
        if (!kx() || this.Ra.size() == 0) {
            return this.Rb;
        }
        this.Pa = SdcardScannerFactory.getDeepCleanEngine(this, 2);
        if (this.Pa != null) {
            ky();
            this.Pa.release();
            this.Pa = null;
        }
        a(this.Rb, this.mContext, z);
        return this.Rb;
    }

    public void a(String str, String str2, long j) {
        Object -l_5_R = new FileInfo();
        -l_5_R.type = 3;
        -l_5_R.mPath = str2;
        -l_5_R.mSize = j;
        this.Rb.mBigFiles.add(-l_5_R);
        b(j, -l_5_R);
    }

    public void a(String str, String str2, long j, tmsdkobf.se.a aVar) {
        Object -l_6_R = new FileMedia();
        -l_6_R.mPath = str2;
        -l_6_R.type = 1;
        -l_6_R.mSize = j;
        -l_6_R.mPlayers = aVar.mPlayers;
        this.Rb.mRadioFiles.add(-l_6_R);
        b(j, -l_6_R);
    }

    public void b(String str, String str2, long j, tmsdkobf.se.a aVar) {
        Object -l_6_R = new FileMedia();
        -l_6_R.mPath = str2;
        -l_6_R.type = 2;
        -l_6_R.mSize = j;
        -l_6_R.mPlayers = aVar.mPlayers;
        this.Rb.mVideoFiles.add(-l_6_R);
        b(j, -l_6_R);
    }

    public String getDetailRule(String str) {
        return "";
    }

    public boolean kA() {
        return this.QS;
    }

    public void kz() {
        this.QS = true;
        if (this.Pa != null) {
            this.Pa.cancel();
        }
    }

    public void onDirectoryChange(String str, int i) {
    }

    public void onFoundComRubbish(String str, String str2, long j) {
        qt -l_5_R = (qt) this.QW.get(str);
        if (-l_5_R == null) {
            -l_5_R = (qt) this.QX.get(str);
            if (-l_5_R == null) {
                if (((qt) this.QY.get(str)) != null) {
                    a(str, str2, j);
                }
                return;
            }
            b(str, str2, j, (tmsdkobf.se.a) -l_5_R);
            return;
        }
        a(str, str2, j, (tmsdkobf.se.a) -l_5_R);
    }

    public void onFoundEmptyDir(String str, long j) {
    }

    public void onFoundKeySoftRubbish(String str, String[] strArr, long j) {
    }

    public void onFoundSoftRubbish(String str, String str2, String str3, long j) {
    }

    public void onProcessChange(int i) {
        if (this.QT != 0) {
            this.QV = i;
            int -l_3_I = (int) ((((float) (this.QV + this.QU)) * 100.0f) / ((float) this.QT));
            if (-l_3_I == 100) {
                -l_3_I--;
            }
            if (this.Rc != null) {
                this.Rc.onProgressChanged(-l_3_I);
            }
        }
    }

    public void onVisit(QFile qFile) {
        int i = qFile.type;
    }
}
