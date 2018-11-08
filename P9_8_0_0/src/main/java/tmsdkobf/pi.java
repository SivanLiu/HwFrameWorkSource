package tmsdkobf;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import java.util.LinkedList;
import tmsdk.common.TMSService;
import tmsdk.common.creator.BaseManagerC;

final class pi extends BaseManagerC {
    private LinkedList<b> Jy = new LinkedList();
    private id Jz;
    private Context mContext;

    interface a {
        void cp(String str);
    }

    static final class b implements pg {
        private pg JA;

        public b(pg pgVar) {
            this.JA = pgVar;
        }

        public final void aQ(final String str) {
            im.bJ().newFreeThread(new Runnable(this) {
                final /* synthetic */ b JC;

                public void run() {
                    this.JC.JA.aQ(str);
                }
            }, "onPackageAddedThread").start();
        }

        public void aR(final String str) {
            im.bJ().newFreeThread(new Runnable(this) {
                final /* synthetic */ b JC;

                public void run() {
                    this.JC.JA.aR(str);
                }
            }, "onPackageReinstallThread").start();
        }

        public final void aS(final String str) {
            im.bJ().newFreeThread(new Runnable(this) {
                final /* synthetic */ b JC;

                public void run() {
                    this.JC.JA.aS(str);
                }
            }, "onPackageRemovedThread").start();
        }

        public boolean equals(Object obj) {
            if (obj == null || !(obj instanceof b)) {
                return false;
            }
            return this.JA.getClass().equals(((b) obj).JA.getClass());
        }
    }

    private final class c extends id {
        private d JD;
        final /* synthetic */ pi JE;

        private c(pi piVar) {
            this.JE = piVar;
        }

        public IBinder onBind() {
            return null;
        }

        public void onCreate(Context context) {
            super.onCreate(context);
            this.JD = new d();
            this.JD.register();
        }

        public void onDestory() {
            this.JD.hK();
            super.onDestory();
        }
    }

    private final class d extends if {
        final /* synthetic */ pi JE;
        private a JF;
        private a JG;
        private a JH;

        private d(pi piVar) {
            this.JE = piVar;
            this.JF = new a(this) {
                final /* synthetic */ d JI;

                {
                    this.JI = r1;
                }

                public void cp(String str) {
                    Object -l_2_R = this.JI.JE.Jy.iterator();
                    while (-l_2_R.hasNext()) {
                        ((b) -l_2_R.next()).aQ(str);
                    }
                }
            };
            this.JG = new a(this) {
                final /* synthetic */ d JI;

                {
                    this.JI = r1;
                }

                public void cp(String str) {
                    Object -l_2_R = this.JI.JE.Jy.iterator();
                    while (-l_2_R.hasNext()) {
                        ((b) -l_2_R.next()).aS(str);
                    }
                }
            };
            this.JH = new a(this) {
                final /* synthetic */ d JI;

                {
                    this.JI = r1;
                }

                public void cp(String str) {
                    Object -l_2_R = this.JI.JE.Jy.iterator();
                    while (-l_2_R.hasNext()) {
                        ((b) -l_2_R.next()).aR(str);
                    }
                }
            };
        }

        private void a(final a aVar, final String str) {
            im.bJ().newFreeThread(new Runnable(this) {
                final /* synthetic */ d JI;

                public void run() {
                    synchronized (this.JI.JE.Jy) {
                        aVar.cp(str);
                    }
                }
            }, "handlePackageChangeThread").start();
        }

        public void doOnRecv(Context context, Intent intent) {
            Object -l_3_R = intent.getAction();
            Object -l_4_R = intent.getExtras();
            int -l_5_I = -1;
            if (-l_4_R != null && -l_4_R.containsKey("android.intent.extra.REPLACING")) {
                -l_5_I = !-l_4_R.getBoolean("android.intent.extra.REPLACING") ? 1 : 0;
            }
            if (-l_3_R.equals("android.intent.action.PACKAGE_ADDED") && -l_5_I != 0) {
                a(this.JF, intent.getDataString().substring(8));
            } else if (-l_3_R.equals("android.intent.action.PACKAGE_REMOVED") && -l_5_I != 0) {
                a(this.JG, intent.getDataString().substring(8));
            } else if (-l_3_R.equals("android.intent.action.PACKAGE_REPLACED")) {
                a(this.JH, intent.getDataString().substring(8));
            }
        }

        public void hK() {
            this.JE.mContext.unregisterReceiver(this);
        }

        public void register() {
            Object -l_1_R = new IntentFilter();
            -l_1_R.addAction("android.intent.action.PACKAGE_REPLACED");
            -l_1_R.addAction("android.intent.action.PACKAGE_ADDED");
            -l_1_R.addAction("android.intent.action.PACKAGE_REMOVED");
            -l_1_R.setPriority(Integer.MAX_VALUE);
            -l_1_R.addDataScheme("package");
            this.JE.mContext.registerReceiver(this, -l_1_R);
        }
    }

    pi() {
    }

    public pg c(pg pgVar) {
        synchronized (this.Jy) {
            Object -l_3_R = pgVar == null ? null : new b(pgVar);
            if (-l_3_R != null) {
                if (!this.Jy.contains(-l_3_R)) {
                    this.Jy.add(-l_3_R);
                    return pgVar;
                }
            }
            return null;
        }
    }

    public int getSingletonType() {
        return 1;
    }

    public void onCreate(Context context) {
        this.mContext = context;
        this.Jz = new c();
        TMSService.startService(this.Jz, null);
    }
}
