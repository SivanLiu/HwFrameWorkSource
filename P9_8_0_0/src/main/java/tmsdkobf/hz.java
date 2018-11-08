package tmsdkobf;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.os.Build;
import android.os.Handler;
import android.provider.CallLog;
import android.telephony.PhoneNumberUtils;
import android.telephony.PhoneStateListener;
import android.text.TextUtils;
import java.util.concurrent.ConcurrentLinkedQueue;
import tmsdk.bg.creator.ManagerCreatorB;
import tmsdk.bg.module.aresengine.AresEngineManager;
import tmsdk.bg.module.aresengine.DataFilter;
import tmsdk.bg.module.aresengine.DataHandler;
import tmsdk.bg.module.aresengine.DataInterceptorBuilder;
import tmsdk.bg.module.aresengine.DataMonitor;
import tmsdk.bg.module.aresengine.IShortCallChecker;
import tmsdk.bg.module.aresengine.SystemCallLogFilter;
import tmsdk.common.DualSimTelephonyManager;
import tmsdk.common.TMSDKContext;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.module.aresengine.CallLogEntity;
import tmsdk.common.module.aresengine.FilterConfig;
import tmsdk.common.module.aresengine.FilterResult;
import tmsdk.common.module.aresengine.ICallLogDao;
import tmsdk.common.utils.f;
import tmsdk.common.utils.q;

public final class hz extends DataInterceptorBuilder<CallLogEntity> {
    private Context mContext;
    private b qX;
    private c qY;

    private static class a {
        static hz qZ = new hz();
    }

    public static final class b extends DataMonitor<CallLogEntity> {
        private static final boolean rc = Build.BRAND.contains("Xiaomi");
        private static CallLogEntity rd;
        private static long re = 0;
        private Context mContext;
        private ContentObserver ra;
        private BroadcastReceiver rb;
        private final long rf = 10000;
        private final ConcurrentLinkedQueue<String> rg = new ConcurrentLinkedQueue();
        private final ConcurrentLinkedQueue<String> rh = new ConcurrentLinkedQueue();
        private PhoneStateListener ri;

        public b(Context context) {
            this.mContext = context;
            register();
        }

        private void a(ContentObserver contentObserver, CallLogEntity callLogEntity, ConcurrentLinkedQueue<String> concurrentLinkedQueue) {
            f.f("MMM", "recoreds.size: " + concurrentLinkedQueue.size() + " lastcalllog.phonenum:" + callLogEntity.phonenum);
            if (!concurrentLinkedQueue.isEmpty() && concurrentLinkedQueue.contains(callLogEntity.phonenum)) {
                f.f("MMM", "match =" + callLogEntity.phonenum);
                long -l_4_J = System.currentTimeMillis();
                callLogEntity.phonenum = PhoneNumberUtils.stripSeparators(callLogEntity.phonenum);
                notifyDataReached(callLogEntity, Long.valueOf(-l_4_J));
                concurrentLinkedQueue.clear();
                f.f("MMM", "clear ");
            }
        }

        private void register() {
            Object -l_2_R;
            this.rb = new if(this) {
                final /* synthetic */ b rj;

                {
                    this.rj = r1;
                }

                private String b(Intent intent) {
                    Object -l_2_R = intent.getStringExtra("android.intent.extra.PHONE_NUMBER");
                    return -l_2_R == null ? getResultData() : -l_2_R;
                }

                private String c(Intent intent) {
                    Object -l_2_R = intent.getStringExtra("incoming_number");
                    if (-l_2_R == null) {
                        -l_2_R = getResultData();
                    }
                    return PhoneNumberUtils.stripSeparators(-l_2_R);
                }

                public void doOnRecv(Context context, Intent intent) {
                    Object -l_3_R;
                    if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
                        -l_3_R = b(intent);
                        this.rj.rh.add(-l_3_R != null ? -l_3_R : "null");
                    } else if (it.a(context, intent) == 1 && !b.rc) {
                        -l_3_R = c(intent);
                        this.rj.rg.add(-l_3_R != null ? -l_3_R : "null");
                    }
                }
            };
            it.a(this.mContext, this.rb);
            Object -l_1_R = new IntentFilter("android.intent.action.NEW_OUTGOING_CALL");
            -l_1_R.setPriority(Integer.MAX_VALUE);
            -l_1_R.addCategory("android.intent.category.DEFAULT");
            this.mContext.registerReceiver(this.rb, -l_1_R);
            if (rc) {
                this.ri = new PhoneStateListener(this) {
                    final /* synthetic */ b rj;

                    {
                        this.rj = r1;
                    }

                    public void onCallStateChanged(int i, String str) {
                        if (i == 1) {
                            ConcurrentLinkedQueue b = this.rj.rg;
                            if (TextUtils.isEmpty(str)) {
                                str = "null";
                            }
                            b.add(str);
                        }
                    }
                };
                -l_2_R = DualSimTelephonyManager.getInstance();
                -l_2_R.listenPhonesState(0, this.ri, 32);
                -l_2_R.listenPhonesState(1, this.ri, 32);
            }
            -l_2_R = new Handler();
            this.ra = new ContentObserver(this, -l_2_R) {
                final /* synthetic */ b rj;

                public synchronized void onChange(boolean z) {
                    super.onChange(z);
                    final Object -l_2_R = ((AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class)).getAresEngineFactor().getSysDao();
                    final Object -l_3_R = -l_2_R.getLastCallLog();
                    if (-l_3_R != null) {
                        -l_2_R.post(new Runnable(this) {
                            final /* synthetic */ AnonymousClass3 rn;

                            /* JADX WARNING: inconsistent code. */
                            /* Code decompiled incorrectly, please refer to instructions dump. */
                            public void run() {
                                Object obj = null;
                                if (-l_3_R.type != 2) {
                                    int -l_1_I = 0;
                                    long -l_2_J = System.currentTimeMillis();
                                    if (b.rd != null) {
                                        if (-l_2_J - b.re >= 10000) {
                                            obj = 1;
                                        }
                                        if (obj == null) {
                                            if (TextUtils.isEmpty(b.rd.phonenum)) {
                                                if (!"null".endsWith(-l_3_R.phonenum)) {
                                                }
                                            }
                                            -l_1_I = 1;
                                        }
                                    }
                                    f.f("SystemCallLogInterceptorBuilder", "needDel" + -l_1_I);
                                    if (-l_1_I == 0) {
                                        this.rn.rj.a(this.rn.rj.ra, -l_3_R, this.rn.rj.rg);
                                    } else {
                                        -l_2_R.remove(-l_3_R);
                                        b.rd = null;
                                        b.re = 0;
                                        this.rn.rj.rg.clear();
                                    }
                                    this.rn.rj.rh.clear();
                                    return;
                                }
                                this.rn.rj.a(this.rn.rj.ra, -l_3_R, this.rn.rj.rh);
                                this.rn.rj.rg.clear();
                            }
                        });
                    }
                }
            };
            this.mContext.getContentResolver().registerContentObserver(CallLog.CONTENT_URI, true, this.ra);
        }

        private void unregister() {
            this.mContext.getContentResolver().unregisterContentObserver(this.ra);
            if (this.ri != null) {
                Object -l_1_R = DualSimTelephonyManager.getInstance();
                -l_1_R.listenPhonesState(0, this.ri, 0);
                -l_1_R.listenPhonesState(1, this.ri, 0);
            }
            this.ra = null;
            this.mContext.unregisterReceiver(this.rb);
            this.rb = null;
        }

        protected void finalize() throws Throwable {
            unregister();
            super.finalize();
        }
    }

    private static final class c extends SystemCallLogFilter {
        private Context mContext;
        private hm qb;
        private AresEngineManager qc = ((AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class));
        private IShortCallChecker ro;
        private boolean rp;

        public c(Context context) {
            this.mContext = context;
            this.rp = bD();
            this.qb = new hm();
            this.qb.a(512, 1, 2, 4, 8, 16, 32, 128, 64, 256);
            this.qb.a(512, new a(this) {
                final /* synthetic */ c rq;

                {
                    this.rq = r1;
                }

                boolean br() {
                    return (bn() == 0 || bn() == 1) ? q.cL(bm().phonenum) : false;
                }

                void bs() {
                    this.rq.a(this, this.rq.qc.getAresEngineFactor().getCallLogDao(), bn() == 1, true);
                }
            });
            this.qb.a(1, new a(this) {
                final /* synthetic */ c rq;

                {
                    this.rq = r1;
                }

                boolean br() {
                    return bn() == 2 && this.rq.qc.getAresEngineFactor().getPrivateListDao().contains(((CallLogEntity) bm()).phonenum, 0);
                }

                void bs() {
                    CallLogEntity -l_1_R = (CallLogEntity) bm();
                    if (-l_1_R.type == 3) {
                        -l_1_R.duration = ((Long) bo()[0]).longValue() - -l_1_R.date;
                    }
                    this.rq.a(this, this.rq.qc.getAresEngineFactor().getPrivateCallLogDao(), true, false);
                }
            });
            this.qb.a(2, new a(this) {
                final /* synthetic */ c rq;

                {
                    this.rq = r1;
                }

                boolean br() {
                    CallLogEntity -l_1_R = (CallLogEntity) bm();
                    return (bn() == 3 || -l_1_R.type == 2 || !this.rq.qc.getAresEngineFactor().getWhiteListDao().contains(-l_1_R.phonenum, 0)) ? false : true;
                }

                void bs() {
                    this.rq.a(this, this.rq.qc.getAresEngineFactor().getCallLogDao(), bn() == 1, true);
                }
            });
            this.qb.a(4, new a(this) {
                final /* synthetic */ c rq;

                {
                    this.rq = r1;
                }

                boolean br() {
                    CallLogEntity -l_1_R = (CallLogEntity) bm();
                    return (bn() == 3 || -l_1_R.type == 2 || !this.rq.qc.getAresEngineFactor().getBlackListDao().contains(-l_1_R.phonenum, 0)) ? false : true;
                }

                void bs() {
                    this.rq.a(this, this.rq.qc.getAresEngineFactor().getCallLogDao(), bn() == 1, true);
                }
            });
            this.qb.a(8, new a(this) {
                final /* synthetic */ c rq;

                {
                    this.rq = r1;
                }

                boolean br() {
                    CallLogEntity -l_1_R = (CallLogEntity) bm();
                    return (bn() == 3 || -l_1_R.type == 2 || !this.rq.qc.getAresEngineFactor().getSysDao().contains(-l_1_R.phonenum)) ? false : true;
                }

                void bs() {
                    this.rq.a(this, this.rq.qc.getAresEngineFactor().getCallLogDao(), bn() == 1, true);
                }
            });
            this.qb.a(16, new a(this) {
                final /* synthetic */ c rq;

                {
                    this.rq = r1;
                }

                boolean br() {
                    CallLogEntity -l_1_R = (CallLogEntity) bm();
                    return (bn() == 3 || -l_1_R.type == 2 || !this.rq.qc.getAresEngineFactor().getLastCallLogDao().contains(-l_1_R.phonenum)) ? false : true;
                }

                void bs() {
                    this.rq.a(this, this.rq.qc.getAresEngineFactor().getCallLogDao(), bn() == 1, true);
                }
            });
            this.qb.a(32, new a(this) {
                final /* synthetic */ c rq;

                {
                    this.rq = r1;
                }

                boolean br() {
                    return (((CallLogEntity) bm()).type == 2 || bn() == 3) ? false : true;
                }

                void bs() {
                    this.rq.a(this, this.rq.qc.getAresEngineFactor().getCallLogDao(), bn() == 1, true);
                }
            });
            this.qb.a(64, new a(this) {
                final /* synthetic */ c rq;

                {
                    this.rq = r1;
                }

                boolean br() {
                    int i = 1;
                    CallLogEntity -l_2_R = (CallLogEntity) bm();
                    Object -l_3_R = -l_2_R.phonenum;
                    if (-l_3_R == null || -l_3_R.length() <= 2) {
                        return false;
                    }
                    int -l_1_I = !this.rq.rp ? -l_2_R.type != 1 ? 0 : 1 : 0;
                    if ((-l_2_R.duration > 5 ? 1 : 0) != 0) {
                        i = 0;
                    }
                    return -l_1_I & i;
                }

                void bs() {
                    this.rq.a(this, null, false, false);
                }
            });
            this.qb.a(128, new a(this) {
                final /* synthetic */ c rq;
                private final int rr = 8000;

                {
                    this.rq = r2;
                }

                boolean br() {
                    long -l_1_J = ((Long) bo()[0]).longValue();
                    CallLogEntity -l_3_R = (CallLogEntity) bm();
                    long -l_4_J = -l_1_J - -l_3_R.date;
                    if (this.rq.ro != null) {
                        return this.rq.ro.isShortCall(-l_3_R, -l_4_J);
                    }
                    boolean z;
                    if (!this.rq.rp && bn() == 2 && -l_3_R.type == 3) {
                        if ((-l_3_R.duration > 8000 ? 1 : 0) == 0) {
                            if ((-l_1_J - -l_3_R.date > 8000 ? 1 : 0) == 0) {
                                z = true;
                                return z;
                            }
                        }
                    }
                    z = false;
                    return z;
                }

                void bs() {
                    CallLogEntity -l_1_R = (CallLogEntity) bm();
                    -l_1_R.duration = ((Long) bo()[0]).longValue() - -l_1_R.date;
                    Object -l_4_R = this.rq.qc.getAresEngineFactor();
                    -l_4_R.getPhoneDeviceController().cancelMissCall();
                    this.rq.a(this, -l_4_R.getCallLogDao(), true, false);
                }
            });
            this.qb.a(256, new a(this) {
                final /* synthetic */ c rq;

                {
                    this.rq = r1;
                }

                boolean br() {
                    return ((CallLogEntity) bm()).type != 2 && bn() == 2;
                }

                void bs() {
                    this.rq.a(this, this.rq.qc.getAresEngineFactor().getCallLogDao(), false, true);
                }
            });
        }

        private void a(a aVar, ICallLogDao<? extends CallLogEntity> iCallLogDao, boolean z, boolean z2) {
            FilterResult -l_5_R = new FilterResult();
            -l_5_R.mParams = aVar.bo();
            -l_5_R.mData = aVar.bm();
            -l_5_R.mFilterfiled = aVar.bp();
            -l_5_R.mState = aVar.bn();
            -l_5_R.isBlocked = z;
            aVar.a(-l_5_R);
            if (iCallLogDao != null && z) {
                CallLogEntity -l_6_R = (CallLogEntity) aVar.bm();
                if (z2) {
                    -l_6_R.type = 1;
                }
                Object -l_7_R = this.qc.getAresEngineFactor();
                Object -l_8_R = -l_7_R.getEntityConverter();
                if (iCallLogDao.insert(-l_8_R == null ? -l_6_R : -l_8_R.convert(-l_6_R), -l_5_R) != -1) {
                    -l_7_R.getSysDao().remove(-l_6_R);
                }
            }
        }

        private boolean bD() {
            return TMServiceFactory.getSystemInfoService().ai("com.htc.launcher");
        }

        protected FilterResult a(CallLogEntity callLogEntity, Object... objArr) {
            return this.qb.a(callLogEntity, getConfig(), objArr);
        }

        protected void a(CallLogEntity callLogEntity, FilterResult filterResult, Object... objArr) {
            super.a(callLogEntity, filterResult, new Object[0]);
            if (callLogEntity.type == 2) {
                this.qc.getAresEngineFactor().getLastCallLogDao().update(callLogEntity);
            }
        }

        public FilterConfig defalutFilterConfig() {
            Object -l_1_R = new FilterConfig();
            -l_1_R.set(512, 0);
            -l_1_R.set(1, 2);
            -l_1_R.set(2, 0);
            -l_1_R.set(4, 1);
            -l_1_R.set(8, 0);
            -l_1_R.set(16, 0);
            -l_1_R.set(32, 3);
            -l_1_R.set(128, 2);
            -l_1_R.set(64, 2);
            -l_1_R.set(256, 2);
            return -l_1_R;
        }

        public void setShortCallChecker(IShortCallChecker iShortCallChecker) {
            this.ro = iShortCallChecker;
        }
    }

    private hz() {
        this.mContext = TMSDKContext.getApplicaionContext();
    }

    public static hz bz() {
        return a.qZ;
    }

    public DataFilter<CallLogEntity> getDataFilter() {
        if (this.qY == null) {
            this.qY = new c(this.mContext);
        }
        return this.qY;
    }

    public DataHandler getDataHandler() {
        return new DataHandler();
    }

    public DataMonitor<CallLogEntity> getDataMonitor() {
        if (this.qX == null) {
            this.qX = new b(this.mContext);
        }
        return this.qX;
    }

    public String getName() {
        return DataInterceptorBuilder.TYPE_SYSTEM_CALL;
    }
}
