package tmsdkobf;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import tmsdk.bg.creator.BaseManagerB;
import tmsdk.bg.creator.ManagerCreatorB;
import tmsdk.bg.module.aresengine.AresEngineFactor;
import tmsdk.bg.module.aresengine.AresEngineManager;
import tmsdk.bg.module.aresengine.DataInterceptor;
import tmsdk.bg.module.aresengine.DataInterceptorBuilder;
import tmsdk.bg.module.aresengine.ISmsReportCallBack;
import tmsdk.bg.module.aresengine.IncomingSmsFilter;
import tmsdk.bg.module.aresengine.IntelliSmsChecker;
import tmsdk.common.DataEntity;
import tmsdk.common.TMSDKContext;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.aresengine.SmsEntity;
import tmsdk.common.module.aresengine.TelephonyEntity;
import tmsdk.common.module.intelli_sms.IntelliSmsCheckResult;
import tmsdk.common.module.intelli_sms.MMatchSysResult;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.utils.f;

public final class hh extends BaseManagerB {
    private final HashMap<String, DataInterceptor<? extends TelephonyEntity>> pB = new HashMap();
    private b pC;
    private AresEngineFactor pD;
    private a pE;

    final class a implements ii {
        final /* synthetic */ hh pF;

        a(hh hhVar) {
            this.pF = hhVar;
        }

        private void a(DataEntity dataEntity, DataEntity dataEntity2) {
            int -l_3_I = 0;
            Object -l_5_R = SmsEntity.unmarshall(dataEntity.bundle().getByteArray("sms"));
            Object -l_6_R = this.pF.findInterceptor(DataInterceptorBuilder.TYPE_INCOMING_SMS);
            Object -l_7_R = -l_6_R == null ? null : -l_6_R.dataFilter();
            if (-l_6_R != null && (-l_7_R instanceof IncomingSmsFilter)) {
                Object -l_9_R = -l_6_R.dataHandler();
                -l_7_R.unbind();
                Object -l_10_R = -l_7_R.filter(-l_5_R, Integer.valueOf(1), null);
                -l_7_R.a(-l_9_R);
                if (-l_10_R != null) {
                    -l_3_I = -l_10_R.isBlocked;
                    hp -l_8_R = new hp();
                    -l_8_R.mPkg = TMSDKContext.getApplicaionContext().getPackageName();
                    -l_8_R.qh = -l_10_R.mFilterfiled;
                    -l_8_R.mState = -l_10_R.mState;
                    -l_8_R.qi = -l_10_R.isBlocked;
                    dataEntity2.bundle().putString("information", hp.a(-l_8_R));
                }
            }
            dataEntity2.bundle().putBoolean("blocked", -l_3_I);
        }

        private void b(DataEntity dataEntity, DataEntity dataEntity2) {
            Object -l_3_R = dataEntity.bundle();
            Object -l_4_R = SmsEntity.unmarshall(-l_3_R.getByteArray("sms"));
            Object -l_5_R = hp.aD(-l_3_R.getString("informations"));
            Object -l_6_R = this.pF.findInterceptor(DataInterceptorBuilder.TYPE_INCOMING_SMS);
            Object -l_7_R = -l_3_R.getString("event_sender");
            if (-l_6_R != null) {
                Object -l_8_R = -l_6_R.dataMonitor();
                if (-l_8_R instanceof a) {
                    ((a) -l_8_R).a(-l_4_R, Integer.valueOf(2), -l_7_R, -l_5_R);
                    return;
                }
                -l_8_R.notifyDataReached(-l_4_R, new Object[0]);
            }
        }

        private void c(DataEntity dataEntity, DataEntity dataEntity2) {
            Object -l_3_R = dataEntity.bundle();
            Object -l_4_R = -l_3_R.getString("command");
            Object -l_5_R = -l_3_R.getString("data");
            if (-l_4_R != null && -l_5_R != null) {
                if (-l_4_R.equals("add")) {
                    hi.bi().ax(-l_5_R);
                } else {
                    hi.bi().ay(-l_5_R);
                }
            }
        }

        private void d(DataEntity dataEntity, DataEntity dataEntity2) {
            dataEntity2.bundle().putBoolean("support_this_phone", ((AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class)).getAresEngineFactor().getSysDao().supportThisPhone());
            dataEntity2.bundle().putString("pkg", TMSDKContext.getApplicaionContext().getPackageName());
        }

        public boolean isMatch(int i) {
            if (i >= 1) {
                if (i <= 4) {
                    return true;
                }
            }
            return false;
        }

        public DataEntity onProcessing(DataEntity dataEntity) {
            int -l_2_I = dataEntity.what();
            Object -l_3_R = new DataEntity(-l_2_I);
            switch (-l_2_I) {
                case 1:
                    b(dataEntity, -l_3_R);
                    break;
                case 2:
                    a(dataEntity, -l_3_R);
                    break;
                case 3:
                    c(dataEntity, -l_3_R);
                    break;
                case 4:
                    d(dataEntity, -l_3_R);
                    break;
                default:
                    -l_3_R.bundle().putBoolean("has_exceprtion", true);
                    break;
            }
            return -l_3_R;
        }
    }

    static final class b extends IntelliSmsChecker {
        private mm pG = null;

        private MMatchSysResult a(SmsEntity smsEntity, Boolean bool) {
            Object -l_3_R = b.class;
            synchronized (b.class) {
                if (this.pG == null) {
                    this.pG = mm.eV();
                    this.pG.eT();
                }
                if (smsEntity.protocolType < 0 || smsEntity.protocolType > 2) {
                    smsEntity.protocolType = 0;
                }
                SmsCheckResult -l_3_R2 = this.pG.b(smsEntity, bool);
                if (-l_3_R2 == null) {
                    return new MMatchSysResult(1, 1, 0, 0, 1, null);
                }
                Object -l_4_R = new MMatchSysResult(-l_3_R2);
                -l_4_R.contentType = this.pG.aU(-l_4_R.contentType);
                return -l_4_R;
            }
        }

        public IntelliSmsCheckResult check(SmsEntity smsEntity) {
            return check(smsEntity, Boolean.valueOf(false));
        }

        public IntelliSmsCheckResult check(SmsEntity smsEntity, Boolean bool) {
            int -l_4_I = 1;
            Object -l_3_R = a(smsEntity, bool);
            if (smsEntity.protocolType != 1) {
                -l_4_I = MMatchSysResult.getSuggestion(-l_3_R);
            }
            return new IntelliSmsCheckResult(-l_4_I, -l_3_R);
        }

        public boolean isChargingSms(SmsEntity smsEntity) {
            Object -l_2_R = b.class;
            synchronized (b.class) {
                if (this.pG == null) {
                    this.pG = mm.eV();
                    this.pG.eT();
                }
                return this.pG.t(smsEntity.phonenum, smsEntity.body) != null;
            }
        }
    }

    public void addInterceptor(DataInterceptorBuilder<? extends TelephonyEntity> dataInterceptorBuilder) {
        if (this.pB.containsKey(dataInterceptorBuilder.getName())) {
            throw new RuntimeException("the interceptor named " + dataInterceptorBuilder.getName() + " had exist");
        }
        this.pB.put(dataInterceptorBuilder.getName(), dataInterceptorBuilder.create());
    }

    public IntelliSmsChecker bh() {
        return this.pC;
    }

    protected void finalize() throws Throwable {
        super.finalize();
        f.h("QQPimSecure", "AresEngineManagerImpl finalize()");
    }

    public DataInterceptor<? extends TelephonyEntity> findInterceptor(String str) {
        return (DataInterceptor) this.pB.get(str);
    }

    public AresEngineFactor getAresEngineFactor() {
        if (this.pD != null) {
            return this.pD;
        }
        throw new NullPointerException("The AresEngineManager's Factor can not be null.");
    }

    public int getSingletonType() {
        return 1;
    }

    public List<DataInterceptor<? extends TelephonyEntity>> interceptors() {
        return new ArrayList(this.pB.values());
    }

    public void onCreate(Context context) {
        this.pC = new b();
        this.pE = new a(this);
        ik.a(this.pE);
        iu.bY();
    }

    public void reportRecoverSms(LinkedHashMap<SmsEntity, Integer> linkedHashMap, ISmsReportCallBack iSmsReportCallBack) {
        hy.reportRecoverSms(linkedHashMap, iSmsReportCallBack);
    }

    public final boolean reportSms(List<SmsEntity> list) {
        Object -l_2_R = new ArrayList();
        Object -l_3_R = bh();
        for (SmsEntity -l_5_R : list) {
            MMatchSysResult -l_6_R = (MMatchSysResult) -l_3_R.check(-l_5_R).getSysResult();
            Object -l_7_R = new es();
            -l_7_R.setComment(null);
            -l_7_R.q((int) (System.currentTimeMillis() / 1000));
            -l_7_R.t(-l_5_R.phonenum);
            -l_7_R.u(-l_5_R.body);
            if (-l_5_R.protocolType < 0 || -l_5_R.protocolType > 2) {
                -l_5_R.protocolType = 0;
            }
            -l_7_R.v(mm.Ai[-l_5_R.protocolType][0]);
            if (-l_6_R != null) {
                -l_7_R.r(-l_6_R.finalAction);
                -l_7_R.s(-l_6_R.actionReason);
                -l_7_R.u(-l_6_R.minusMark);
                -l_7_R.t(-l_6_R.contentType);
                -l_7_R.fo = new ArrayList();
                if (-l_6_R.ruleTypeID != null) {
                    for (Object -l_11_R : -l_6_R.ruleTypeID) {
                        -l_7_R.fo.add(new en(-l_11_R.fg, -l_11_R.fh));
                    }
                }
            }
            -l_2_R.add(-l_7_R);
        }
        return ((pq) ManagerCreatorC.getManager(pq.class)).u(-l_2_R) == 0;
    }

    public void setAresEngineFactor(AresEngineFactor aresEngineFactor) {
        this.pD = aresEngineFactor;
    }
}
