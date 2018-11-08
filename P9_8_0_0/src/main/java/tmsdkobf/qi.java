package tmsdkobf;

import android.os.Environment;
import android.util.Log;
import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import tmsdk.common.TMSDKContext;
import tmsdk.fg.module.cleanV2.IScanTaskCallBack;
import tmsdk.fg.module.cleanV2.RubbishEntity;
import tmsdk.fg.module.cleanV2.RubbishHolder;

public class qi {
    static b MJ;
    public static long startTime;
    private final int MC;
    private final int MD;
    List<String> ME;
    List<a> MF;
    private qh MG;
    final String MH;
    final String MI;
    private AtomicBoolean MK;

    public interface a {
        void a(File file, qj qjVar);
    }

    public interface b {
        void close();

        void println(String str);
    }

    public qi() {
        this.MC = Runtime.getRuntime().availableProcessors();
        this.MD = 50000;
        this.MF = null;
        this.MG = null;
        this.MH = Environment.getExternalStorageDirectory().getAbsolutePath();
        this.MI = "fgtScanRule.txt";
        this.MK = null;
        this.MG = new qh(false);
        this.MK = new AtomicBoolean(false);
        MJ = i(this.MH + File.separator + "fgtProfile.txt", false);
    }

    private static String cS(String str) {
        Object -l_1_R = TMSDKContext.getApplicaionContext().getPackageManager();
        try {
            return (String) -l_1_R.getApplicationInfo(str, 1).loadLabel(-l_1_R);
        } catch (Object -l_2_R) {
            -l_2_R.printStackTrace();
            return null;
        }
    }

    public static b i(final String str, final boolean z) {
        try {
            return new b() {
                private PrintWriter Na;

                public void close() {
                    if (this.Na != null) {
                        this.Na.close();
                    }
                }

                public void println(String str) {
                    if (this.Na != null) {
                        this.Na.println(str);
                    }
                }
            };
        } catch (Object -l_3_R) {
            -l_3_R.printStackTrace();
            return null;
        } catch (Object -l_3_R2) {
            -l_3_R2.printStackTrace();
            return null;
        }
    }

    public static long jp() {
        return System.currentTimeMillis() - startTime;
    }

    public void a(List<ql> list, RubbishHolder rubbishHolder) {
        for (ql -l_4_R : list) {
            -l_4_R.a(rubbishHolder);
        }
    }

    public void a(IScanTaskCallBack iScanTaskCallBack, String str, boolean z) {
        startTime = System.currentTimeMillis();
        this.MK.set(false);
        this.MG.U(z);
        this.ME = rh.jZ();
        this.MF = this.MG.cQ(str);
        if (this.MF != null && this.MF.size() >= 1) {
            Object -l_4_R = cS(str);
            final int -l_5_I = -l_4_R == null ? 0 : 1;
            final Object -l_6_R = -l_4_R == null ? ((a) this.MF.get(0)).mAppName : -l_4_R;
            final Object -l_7_R = Executors.newSingleThreadExecutor();
            final IScanTaskCallBack iScanTaskCallBack2 = iScanTaskCallBack;
            final String str2 = str;
            a -l_8_R = new a(this) {
                long ML = 0;
                long MM = 0;
                String MN;
                final /* synthetic */ qi MT;

                public void a(final File file, final qj qjVar) {
                    -l_7_R.execute(new Runnable(this) {
                        final /* synthetic */ AnonymousClass1 MW;

                        public void run() {
                            Object -l_1_R = file.getAbsolutePath();
                            IScanTaskCallBack iScanTaskCallBack = iScanTaskCallBack2;
                            String absolutePath = file.getAbsolutePath();
                            AnonymousClass1 anonymousClass1 = this.MW;
                            long j = anonymousClass1.ML + 1;
                            anonymousClass1.ML = j;
                            iScanTaskCallBack.onDirectoryChange(absolutePath, (int) j);
                            int -l_2_I = !-l_5_I ? 4 : 0;
                            List -l_3_R = new LinkedList();
                            -l_3_R.add(-l_1_R);
                            if (qjVar != null) {
                                boolean -l_5_I = -l_2_I != 0 ? 3 != qjVar.Nt : 1 == qjVar.Nt;
                                Object -l_4_R;
                                if (qjVar.mDescription.equals(this.MW.MN)) {
                                    if ((System.currentTimeMillis() - this.MW.MM <= 1000 ? 1 : null) == null) {
                                        this.MW.MM = System.currentTimeMillis();
                                        -l_4_R = new RubbishEntity(-l_2_I, -l_3_R, -l_5_I, qjVar.No, -l_6_R, str2, qjVar.mDescription);
                                        iScanTaskCallBack2.onRubbishFound(-l_4_R);
                                        -l_4_R.setExtendData(qjVar.Nu, qjVar.Ne, qjVar.Nw);
                                        return;
                                    }
                                    return;
                                }
                                this.MW.MN = qjVar.mDescription;
                                this.MW.MM = System.currentTimeMillis();
                                -l_4_R = new RubbishEntity(-l_2_I, -l_3_R, -l_5_I, qjVar.No, -l_6_R, str2, qjVar.mDescription);
                                -l_4_R.setExtendData(qjVar.Nu, qjVar.Ne, qjVar.Nw);
                                iScanTaskCallBack2.onRubbishFound(-l_4_R);
                            }
                        }
                    });
                }
            };
            qj.Nm = 0;
            MJ.println("paserRootPath\t" + jp());
            List -l_9_R = new ArrayList();
            Log.d("fgtScan", "core size:" + this.MC);
            ExecutorService -l_10_R = new ThreadPoolExecutor(this.MC << 1, this.MC << 1, 60, TimeUnit.SECONDS, new ArrayBlockingQueue(50000));
            final RubbishHolder -l_11_R = new RubbishHolder();
            final IScanTaskCallBack iScanTaskCallBack3 = iScanTaskCallBack;
            -l_7_R.execute(new Runnable(this) {
                final /* synthetic */ qi MT;

                public void run() {
                    iScanTaskCallBack3.onScanStarted();
                }
            });
            for (a -l_13_R : this.MF) {
                String -l_14_R = ((String) this.ME.get(0)) + -l_13_R.MB;
                Log.d("fgtScan", "root path:" + -l_14_R);
                File file = new File(-l_14_R);
                if (file.exists() && file.isDirectory()) {
                    ql qlVar = new ql(str, -l_6_R, -l_13_R.MB, -l_5_I);
                    qlVar.a(file, -l_10_R);
                    ql -l_17_R = this.MG.a(-l_13_R.MB, qlVar);
                    if (-l_17_R != null) {
                        MJ.println("paserDetailRule-" + -l_14_R + "\t" + jp());
                        -l_9_R.add(-l_17_R);
                        Log.d("fgtScan", "resolving:" + -l_14_R);
                        -l_17_R.a("", MJ, -l_8_R, this.MK);
                        if (this.MK.get()) {
                            -l_10_R.shutdownNow();
                            try {
                                -l_10_R.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                            } catch (Object -l_18_R) {
                                -l_18_R.printStackTrace();
                            }
                            qk.jq();
                            final List list = -l_9_R;
                            final RubbishHolder rubbishHolder = -l_11_R;
                            final IScanTaskCallBack iScanTaskCallBack4 = iScanTaskCallBack;
                            -l_7_R.execute(new Runnable(this) {
                                final /* synthetic */ qi MT;

                                public void run() {
                                    this.MT.a(list, rubbishHolder);
                                    iScanTaskCallBack4.onScanCanceled(rubbishHolder);
                                }
                            });
                            return;
                        }
                        Log.d("fgtScan", "resolve over:" + -l_14_R);
                        MJ.println("after-" + -l_14_R + "-resolved\t" + jp());
                    } else {
                        Log.d("fgtScan", "can not parser rule!!!");
                        iScanTaskCallBack.onScanError(-18, null);
                        return;
                    }
                }
                Log.d("fgtScan", "root path:" + -l_14_R + "  is not exist or not a directory skiped!");
            }
            -l_10_R.shutdown();
            try {
                -l_10_R.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (Object -l_12_R) {
                -l_12_R.printStackTrace();
            }
            final long -l_12_J = jp();
            final List list2 = -l_9_R;
            final IScanTaskCallBack iScanTaskCallBack5 = iScanTaskCallBack;
            -l_7_R.execute(new Runnable(this) {
                final /* synthetic */ qi MT;

                public void run() {
                    this.MT.a(list2, -l_11_R);
                    iScanTaskCallBack5.onScanFinished(-l_11_R);
                }
            });
            Log.d("fgtScan", "scan all over\t" + -l_12_J);
            MJ.println("scan all over\t" + -l_12_J);
            MJ.close();
            qk.jq();
            return;
        }
        Log.d("fgtScan", "can not get root path!!!");
        iScanTaskCallBack.onScanError(-17, null);
    }

    public void cancel() {
        Log.d("fgtScan", "cancel is called");
        this.MK.set(true);
    }
}
