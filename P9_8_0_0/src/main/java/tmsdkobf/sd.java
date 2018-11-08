package tmsdkobf;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import java.util.ArrayList;
import tmsdk.common.OfflineVideo;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.lang.MultiLangManager;
import tmsdk.fg.module.spacemanager.FileMedia;
import tmsdk.fg.module.spacemanager.FileScanResult;
import tmsdk.fg.module.spacemanager.ISpaceScanListener;
import tmsdkobf.sf.a;

public class sd implements Runnable {
    ri Pm;
    sf QG;
    public ISpaceScanListener QH;
    a QI;
    Context mContext = TMSDKContext.getApplicaionContext();
    Handler vW = new Handler(Looper.getMainLooper());
    boolean wO = false;

    public sd(ri riVar) {
        this.Pm = riVar;
    }

    private void a(a aVar, FileScanResult fileScanResult, boolean z) {
        Object<OfflineVideo> -l_5_R = new rp(null, z).ko();
        if (-l_5_R != null && -l_5_R.size() != 0) {
            long -l_6_J = 0;
            Object -l_8_R = new ArrayList();
            Object -l_9_R = null;
            for (OfflineVideo -l_11_R : -l_5_R) {
                Object -l_12_R;
                if (-l_9_R == null) {
                    -l_9_R = new rl();
                    -l_9_R.kj();
                }
                if (TextUtils.isEmpty(-l_11_R.mAppName)) {
                    -l_12_R = rk.a(-l_11_R.mPath, rl.Ps).toLowerCase();
                    -l_11_R.mPackage = -l_9_R.k(-l_12_R, z);
                    -l_11_R.mAppName = -l_9_R.l(-l_12_R, z);
                }
                -l_12_R = new FileMedia();
                -l_12_R.type = 2;
                -l_12_R.mPath = -l_11_R.mPath;
                -l_12_R.title = -l_11_R.mTitle;
                -l_12_R.pkg = -l_11_R.mPackage;
                -l_12_R.mSrcName = -l_11_R.mAppName;
                -l_12_R.mSize = -l_11_R.mSize;
                -l_12_R.mOfflineVideo = -l_11_R;
                -l_6_J += -l_12_R.mSize;
                -l_8_R.add(-l_12_R);
                if (aVar != null) {
                    aVar.a(-l_6_J, -l_12_R);
                }
            }
            if (fileScanResult.mVideoFiles != null) {
                -l_8_R.addAll(fileScanResult.mVideoFiles);
            }
            fileScanResult.mVideoFiles = -l_8_R;
        }
    }

    private void kw() {
        if (this.QI == null) {
            this.QI = new a(this) {
                final /* synthetic */ sd QJ;

                {
                    this.QJ = r1;
                }

                public void a(long j, final Object -l_4_R) {
                    if (this.QJ.QH != null) {
                        this.QJ.vW.post(new Runnable(this) {
                            final /* synthetic */ AnonymousClass5 QM;

                            public void run() {
                                this.QM.QJ.QH.onFound(-l_4_R);
                            }
                        });
                    }
                }

                public void onProgressChanged(int -l_2_I) {
                    if (this.QJ.QH != null) {
                        if (-l_2_I >= 100) {
                            -l_2_I = 99;
                        }
                        final int -l_3_I = -l_2_I;
                        this.QJ.vW.post(new Runnable(this) {
                            final /* synthetic */ AnonymousClass5 QM;

                            public void run() {
                                this.QM.QJ.QH.onProgressChanged(-l_3_I);
                            }
                        });
                    }
                }
            };
        }
    }

    public boolean a(ISpaceScanListener iSpaceScanListener) {
        if (this.QH != null) {
            return false;
        }
        this.QH = iSpaceScanListener;
        im.bJ().addTask(this, "scanbigfile");
        return true;
    }

    public void kv() {
        this.wO = true;
        if (this.QG != null) {
            this.QG.kz();
        }
    }

    public void run() {
        kw();
        int -l_2_I = ((MultiLangManager) ManagerCreatorC.getManager(MultiLangManager.class)).isENG();
        if (this.QH != null) {
            this.vW.post(new Runnable(this) {
                final /* synthetic */ sd QJ;

                {
                    this.QJ = r1;
                }

                public void run() {
                    this.QJ.QH.onStart();
                }
            });
            int -l_4_I = ((1 | 2) | 4) | 256;
            if (this.wO) {
                this.vW.post(new Runnable(this) {
                    final /* synthetic */ sd QJ;

                    {
                        this.QJ = r1;
                    }

                    public void run() {
                        this.QJ.QH.onCancelFinished();
                        this.QJ.QH = null;
                        this.QJ.wO = false;
                        this.QJ.Pm.bX(0);
                    }
                });
                return;
            }
            this.QG = new sf(this.mContext, this.QI, -l_4_I);
            final Object -l_3_R = this.QG.Z(-l_2_I);
            a(this.QI, -l_3_R, -l_2_I);
            Object -l_5_R = -l_3_R;
            if (this.wO) {
                this.vW.post(new Runnable(this) {
                    final /* synthetic */ sd QJ;

                    public void run() {
                        this.QJ.QH.onCancelFinished();
                        this.QJ.QH.onFinish(0, -l_3_R);
                        this.QJ.QH = null;
                        this.QJ.wO = false;
                        this.QJ.Pm.bX(0);
                    }
                });
            } else {
                this.vW.post(new Runnable(this) {
                    final /* synthetic */ sd QJ;

                    public void run() {
                        this.QJ.QH.onFinish(0, -l_3_R);
                        this.QJ.QH = null;
                        this.QJ.Pm.bX(0);
                    }
                });
            }
            this.QG = null;
        }
    }
}
