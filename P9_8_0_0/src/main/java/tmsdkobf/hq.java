package tmsdkobf;

import android.content.BroadcastReceiver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import java.util.ArrayList;
import java.util.List;
import tmsdk.bg.creator.ManagerCreatorB;
import tmsdk.bg.module.aresengine.AresEngineManager;
import tmsdk.bg.module.aresengine.DataFilter;
import tmsdk.bg.module.aresengine.DataHandler;
import tmsdk.bg.module.aresengine.DataInterceptorBuilder;
import tmsdk.bg.module.aresengine.DataMonitor;
import tmsdk.bg.module.aresengine.ISpecialSmsChecker;
import tmsdk.bg.module.aresengine.IncomingSmsFilter;
import tmsdk.bg.module.aresengine.IntelliSmsChecker;
import tmsdk.bg.module.aresengine.IntelligentSmsHandler;
import tmsdk.common.DataEntity;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.aresengine.FilterConfig;
import tmsdk.common.module.aresengine.FilterResult;
import tmsdk.common.module.aresengine.ISmsDao;
import tmsdk.common.module.aresengine.IncomingSmsFilterConsts;
import tmsdk.common.module.aresengine.SmsEntity;
import tmsdk.common.module.aresengine.TelephonyEntity;
import tmsdk.common.module.intelli_sms.IntelliSmsCheckResult;
import tmsdk.common.module.intelli_sms.SmsCheckResult;
import tmsdk.common.utils.f;

public final class hq extends DataInterceptorBuilder<SmsEntity> {
    private Context mContext = TMSDKContext.getApplicaionContext();

    static final class a extends DataMonitor<SmsEntity> {
        private ht qj;
        private c qk = new c();

        public a() {
            register();
        }

        private void register() {
            this.qj = ht.h(TMSDKContext.getApplicaionContext());
            kt.saveActionData(29945);
            this.qj.a(new hu(this) {
                final /* synthetic */ a ql;

                {
                    this.ql = r1;
                }

                void a(SmsEntity smsEntity, BroadcastReceiver broadcastReceiver) {
                    if (smsEntity != null) {
                        if (this.ql.qk.c(smsEntity, broadcastReceiver)) {
                            this.ql.notifyDataReached(smsEntity, Integer.valueOf(0), broadcastReceiver);
                        }
                        ll.aM(5);
                    }
                }
            });
            this.qj.a(null);
        }

        private void unregister() {
            this.qj.unregister();
        }

        void a(SmsEntity smsEntity, Object... objArr) {
            if (smsEntity != null) {
                notifyDataReached(smsEntity, objArr);
            }
        }

        protected void a(boolean z, SmsEntity smsEntity, Object... objArr) {
            super.a(z, smsEntity, objArr);
            if (z && objArr != null && objArr.length >= 2 && (objArr[1] instanceof BroadcastReceiver)) {
                try {
                    ((BroadcastReceiver) objArr[1]).abortBroadcast();
                } catch (Object -l_5_R) {
                    f.e("abortBroadcast", -l_5_R);
                }
            }
        }

        protected void finalize() throws Throwable {
            unregister();
            super.finalize();
        }

        public void setRegisterState(boolean z) {
            if (z != this.qj.bw()) {
                if (z) {
                    register();
                } else {
                    unregister();
                }
            }
        }
    }

    private static final class b extends IncomingSmsFilter {
        private mm pG;
        private hm qb;
        private AresEngineManager qc;
        private IntelligentSmsHandler qm;
        private ISpecialSmsChecker qn;
        private IntelliSmsChecker qo;

        b(Context context) {
            this.pG = null;
            this.qc = (AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class);
            this.pG = mm.eV();
            this.pG.eT();
            this.qo = ((AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class)).getIntelligentSmsChecker();
            this.qb = new hm();
            this.qb.a(256, 1, 2, 4, 8, 16, 32, 64, IncomingSmsFilterConsts.PAY_SMS, 128);
            this.qb.a(256, new a(this) {
                final /* synthetic */ b qp;

                {
                    this.qp = r1;
                }

                boolean br() {
                    return (bn() == 2 && this.qp.qn != null) ? this.qp.qn.isMatch((SmsEntity) bm()) : false;
                }

                void bs() {
                    SmsEntity -l_1_R = (SmsEntity) bm();
                    Object -l_2_R = new FilterResult();
                    -l_2_R.mData = bm();
                    -l_2_R.mFilterfiled = bp();
                    -l_2_R.mState = bn();
                    -l_2_R.mParams = new Object[]{this.qp.a(bo()), Boolean.valueOf(this.qp.qn.isBlocked(-l_1_R))};
                    Object -l_3_R = this.qp.qc.getAresEngineFactor();
                    if (((Boolean) -l_2_R.mParams[1]).booleanValue()) {
                        -l_2_R.isBlocked = true;
                        -l_3_R.getPhoneDeviceController().blockSms(bo());
                        -l_2_R.mDotos.add(this.qp.a(-l_1_R, -l_3_R.getSmsDao(), -l_2_R));
                    } else {
                        -l_3_R.getPhoneDeviceController().unBlockSms(-l_1_R, bo());
                    }
                    a((FilterResult) -l_2_R);
                }
            });
            this.qb.a(1, new a(this) {
                final /* synthetic */ b qp;

                {
                    this.qp = r1;
                }

                public boolean br() {
                    SmsEntity -l_1_R = (SmsEntity) bm();
                    if (bn() == 2 && -l_1_R.protocolType != 2) {
                        if (this.qp.qc.getAresEngineFactor().getPrivateListDao().contains(-l_1_R.phonenum, 1)) {
                            return true;
                        }
                    }
                    return false;
                }

                public void bs() {
                    SmsEntity -l_1_R = (SmsEntity) bm();
                    Object -l_2_R = new FilterResult();
                    -l_2_R.mData = bm();
                    -l_2_R.mFilterfiled = bp();
                    -l_2_R.mState = bn();
                    -l_2_R.mParams = new Object[]{this.qp.a(bo())};
                    Object -l_3_R = this.qp.qc.getAresEngineFactor();
                    -l_2_R.isBlocked = true;
                    -l_3_R.getPhoneDeviceController().blockSms(bo());
                    -l_2_R.mDotos.add(this.qp.a(-l_1_R, -l_3_R.getPrivateSmsDao(), -l_2_R));
                    a((FilterResult) -l_2_R);
                }
            });
            this.qb.a(2, new a(this) {
                final /* synthetic */ b qp;

                {
                    this.qp = r1;
                }

                public boolean br() {
                    SmsEntity -l_1_R = (SmsEntity) bm();
                    if (bn() != 2) {
                        if (this.qp.qc.getAresEngineFactor().getWhiteListDao().contains(-l_1_R.phonenum, 1)) {
                            return true;
                        }
                    }
                    return false;
                }

                public void bs() {
                    this.qp.b((a) this);
                }
            });
            this.qb.a(4, new a(this) {
                final /* synthetic */ b qp;

                {
                    this.qp = r1;
                }

                public boolean br() {
                    SmsEntity -l_1_R = (SmsEntity) bm();
                    if (bn() != 2) {
                        if (this.qp.qc.getAresEngineFactor().getBlackListDao().contains(-l_1_R.phonenum, 1)) {
                            return true;
                        }
                    }
                    return false;
                }

                public void bs() {
                    this.qp.b((a) this);
                }
            });
            this.qb.a(8, new a(this) {
                final /* synthetic */ b qp;

                {
                    this.qp = r1;
                }

                public boolean br() {
                    return bn() != 2 && this.qp.qc.getAresEngineFactor().getSysDao().contains(((SmsEntity) bm()).phonenum);
                }

                public void bs() {
                    this.qp.b((a) this);
                }
            });
            this.qb.a(16, new a(this) {
                final /* synthetic */ b qp;

                {
                    this.qp = r1;
                }

                public boolean br() {
                    return bn() != 2 && this.qp.qc.getAresEngineFactor().getLastCallLogDao().contains(((SmsEntity) bm()).phonenum);
                }

                public void bs() {
                    this.qp.b((a) this);
                }
            });
            this.qb.a(32, new a(this) {
                final /* synthetic */ b qp;

                {
                    this.qp = r1;
                }

                public boolean br() {
                    return bn() != 2 && this.qp.qc.getAresEngineFactor().getKeyWordDao().contains(((SmsEntity) bm()).body);
                }

                public void bs() {
                    this.qp.b((a) this);
                }
            });
            this.qb.a(64, new a(this) {
                final /* synthetic */ b qp;

                {
                    this.qp = r1;
                }

                public boolean br() {
                    int -l_3_I = 0;
                    Object -l_2_R = this.qp.qo.check((SmsEntity) bm());
                    if (bn() == 2 && -l_2_R.suggestion != 4) {
                        -l_3_I = 1;
                    }
                    if (-l_3_I != 0) {
                        a(-l_2_R);
                    }
                    return -l_3_I;
                }

                public void bs() {
                    Object -l_1_R = new FilterResult();
                    SmsEntity -l_2_R = (SmsEntity) bm();
                    IntelliSmsCheckResult -l_3_R = (IntelliSmsCheckResult) bq();
                    -l_1_R.mData = bm();
                    -l_1_R.mFilterfiled = bp();
                    -l_1_R.mState = bn();
                    int -l_4_I = 0;
                    int -l_5_I = 0;
                    Object -l_6_R = null;
                    ISmsDao -l_7_R = null;
                    Object -l_8_R = this.qp.qc.getAresEngineFactor();
                    if (IntelliSmsCheckResult.shouldBeBlockedOrNot(-l_3_R)) {
                        -l_4_I = 1;
                    } else if (-l_3_R.suggestion == 1) {
                        SmsCheckResult -l_9_R = null;
                        -l_7_R = -l_8_R.getPaySmsDao();
                        if (-l_7_R != null) {
                            -l_9_R = this.qp.pG.t(-l_2_R.getAddress(), -l_2_R.getBody());
                        }
                        if (-l_9_R == null) {
                            -l_4_I = 0;
                        } else {
                            -l_5_I = 1;
                            -l_4_I = 1;
                            -l_6_R = -l_9_R.sRule;
                        }
                    }
                    if (this.qp.qm != null) {
                        -l_4_I = this.qp.qm.handleCheckResult(-l_2_R, -l_3_R, -l_4_I);
                    }
                    if (-l_4_I == 0) {
                        -l_8_R.getPhoneDeviceController().unBlockSms(-l_2_R, bo());
                    } else {
                        -l_1_R.isBlocked = true;
                        -l_8_R.getPhoneDeviceController().blockSms(bo());
                        ArrayList arrayList = -l_1_R.mDotos;
                        b bVar = this.qp;
                        SmsEntity smsEntity = (SmsEntity) bm();
                        if (-l_5_I == 0) {
                            -l_7_R = -l_8_R.getSmsDao();
                        }
                        arrayList.add(bVar.a(smsEntity, -l_7_R, -l_1_R));
                    }
                    -l_1_R.mParams = new Object[]{this.qp.a(bo()), -l_3_R, Boolean.valueOf(-l_4_I), Boolean.valueOf(-l_5_I), -l_6_R};
                    a((FilterResult) -l_1_R);
                }
            });
            this.qb.a(IncomingSmsFilterConsts.PAY_SMS, new a(this) {
                final /* synthetic */ b qp;

                {
                    this.qp = r1;
                }

                public boolean br() {
                    if (this.qp.qc.getAresEngineFactor().getPaySmsDao() == null) {
                        return false;
                    }
                    SmsEntity -l_4_R = (SmsEntity) bm();
                    Object -l_5_R = this.qp.pG.t(-l_4_R.getAddress(), -l_4_R.getBody());
                    if (-l_5_R == null) {
                        return false;
                    }
                    a(-l_5_R);
                    return true;
                }

                public void bs() {
                    SmsEntity -l_1_R = (SmsEntity) bm();
                    SmsCheckResult -l_2_R = (SmsCheckResult) bq();
                    Object -l_3_R = new FilterResult();
                    -l_3_R.mData = bm();
                    -l_3_R.mFilterfiled = bp();
                    -l_3_R.mState = bn();
                    Object -l_4_R = null;
                    if (-l_2_R != null) {
                        -l_4_R = -l_2_R.sRule;
                    }
                    -l_3_R.mParams = new Object[]{this.qp.a(bo()), -l_4_R};
                    Object -l_5_R = this.qp.qc.getAresEngineFactor();
                    -l_3_R.isBlocked = true;
                    -l_5_R.getPhoneDeviceController().blockSms(bo());
                    -l_3_R.mDotos.add(this.qp.a(-l_1_R, -l_5_R.getPaySmsDao(), -l_3_R));
                    a((FilterResult) -l_3_R);
                }
            });
            this.qb.a(128, new a(this) {
                final /* synthetic */ b qp;

                {
                    this.qp = r1;
                }

                public boolean br() {
                    return true;
                }

                public void bs() {
                    this.qp.b((a) this);
                }
            });
        }

        private final Runnable a(final SmsEntity smsEntity, final ISmsDao<? extends SmsEntity> iSmsDao, final FilterResult filterResult) {
            return new Runnable(this) {
                final /* synthetic */ b qp;

                public void run() {
                    Object -l_1_R = this.qp.qc.getAresEngineFactor().getEntityConverter();
                    long -l_2_J = iSmsDao.insert(-l_1_R != null ? -l_1_R.convert(smsEntity) : smsEntity, filterResult);
                    if ((-l_2_J <= 0 ? 1 : null) == null) {
                        smsEntity.id = (int) -l_2_J;
                    }
                }
            };
        }

        private ArrayList<hp> a(Object... objArr) {
            return (objArr != null && objArr.length > 2 && ((Integer) objArr[0]).intValue() == 2) ? (ArrayList) objArr[2] : null;
        }

        private final void b(a aVar) {
            FilterResult -l_2_R = new FilterResult();
            SmsEntity -l_3_R = (SmsEntity) aVar.bm();
            -l_2_R.mData = aVar.bm();
            -l_2_R.mFilterfiled = aVar.bp();
            -l_2_R.mState = aVar.bn();
            -l_2_R.mParams = new Object[]{a(aVar.bo())};
            Object -l_4_R = this.qc.getAresEngineFactor();
            if (aVar.bn() == 0) {
                -l_4_R.getPhoneDeviceController().unBlockSms(-l_3_R, aVar.bo());
            } else if (aVar.bn() == 1) {
                -l_2_R.isBlocked = true;
                -l_4_R.getPhoneDeviceController().blockSms(aVar.bo());
                if (-l_4_R.getSmsDao() != null) {
                    -l_2_R.mDotos.add(a((SmsEntity) aVar.bm(), -l_4_R.getSmsDao(), -l_2_R));
                }
            }
            aVar.a(-l_2_R);
        }

        protected /* synthetic */ FilterResult a(TelephonyEntity telephonyEntity, Object[] objArr) {
            return b((SmsEntity) telephonyEntity, objArr);
        }

        protected FilterResult b(SmsEntity smsEntity, Object... objArr) {
            Object -l_4_R;
            Object obj;
            Object -l_8_R;
            int -l_4_I;
            Object -l_3_R = this.qb.a(smsEntity, getConfig(), objArr);
            if (ht.i(TMSDKContext.getApplicaionContext())) {
                if (-l_3_R == null || !-l_3_R.isBlocked) {
                    -l_4_R = ((AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class)).getAresEngineFactor().getSysDao();
                    obj = null;
                    try {
                        obj = -l_4_R.insert(smsEntity);
                        smsEntity.id = (int) ContentUris.parseId(obj);
                    } catch (Object -l_9_R) {
                        f.e("IncomingSmsInterceptorBuilder", -l_9_R);
                    }
                    -l_3_R = new FilterResult();
                    -l_3_R.mData = smsEntity;
                    -l_3_R.mFilterfiled = 512;
                    -l_3_R.mState = 0;
                }
            }
            if (-l_3_R == null) {
                -l_3_R = new FilterResult();
                -l_3_R.mData = smsEntity;
                -l_3_R.mFilterfiled = -1;
                -l_3_R.mState = 0;
                if (objArr != null && objArr.length >= 2) {
                    -l_4_I = ((Integer) objArr[0]).intValue();
                    if (-l_4_I != 1) {
                        final SmsEntity smsEntity2 = smsEntity;
                        final Object[] objArr2 = objArr;
                        obj = new Runnable(this) {
                            final /* synthetic */ b qp;

                            public void run() {
                                this.qp.qc.getAresEngineFactor().getPhoneDeviceController().unBlockSms(smsEntity2, objArr2);
                            }
                        };
                        if (-l_4_I == 0) {
                            -l_3_R.mDotos.add(obj);
                        } else {
                            obj.run();
                        }
                    }
                }
                -l_3_R.mParams = new Object[]{a(objArr)};
            }
            return -l_3_R;
            me.a(new Thread(), -l_6_R, -l_8_R.toString(), null);
            f.e("IncomingSmsInterceptorBuilder", -l_6_R + -l_8_R.toString());
            smsEntity.id = (int) ContentUris.parseId(-l_4_R.insert(smsEntity, true));
            -l_3_R = new FilterResult();
            -l_3_R.mData = smsEntity;
            -l_3_R.mFilterfiled = 512;
            -l_3_R.mState = 0;
            if (-l_3_R == null) {
                -l_3_R = new FilterResult();
                -l_3_R.mData = smsEntity;
                -l_3_R.mFilterfiled = -1;
                -l_3_R.mState = 0;
                -l_4_I = ((Integer) objArr[0]).intValue();
                if (-l_4_I != 1) {
                    final SmsEntity smsEntity22 = smsEntity;
                    final Object[] objArr22 = objArr;
                    obj = /* anonymous class already generated */;
                    if (-l_4_I == 0) {
                        obj.run();
                    } else {
                        -l_3_R.mDotos.add(obj);
                    }
                }
                -l_3_R.mParams = new Object[]{a(objArr)};
            }
            return -l_3_R;
        }

        public FilterConfig defalutFilterConfig() {
            Object -l_1_R = new FilterConfig();
            -l_1_R.set(256, 3);
            -l_1_R.set(1, 2);
            -l_1_R.set(2, 0);
            -l_1_R.set(4, 1);
            -l_1_R.set(8, 0);
            -l_1_R.set(16, 0);
            -l_1_R.set(32, 1);
            -l_1_R.set(64, 2);
            -l_1_R.set(128, 1);
            return -l_1_R;
        }

        protected void finalize() throws Throwable {
            if (this.pG != null) {
                this.pG.eU();
            }
            super.finalize();
        }

        public void setIntelligentSmsHandler(IntelligentSmsHandler intelligentSmsHandler) {
            this.qm = intelligentSmsHandler;
        }

        public void setSpecialSmsChecker(ISpecialSmsChecker iSpecialSmsChecker) {
            this.qn = iSpecialSmsChecker;
        }
    }

    private static final class c {
        private hi qv;

        private c() {
            this.qv = hi.bi();
        }

        private ArrayList<ih> a(SmsEntity smsEntity, List<hp> list) {
            Object -l_3_R = this.qv.bj();
            Object -l_4_R = new ArrayList();
            Object -l_6_R = new DataEntity(2);
            -l_6_R.bundle().putByteArray("sms", SmsEntity.marshall(smsEntity));
            int -l_7_I = -l_3_R.size() - 1;
            while (-l_7_I >= 0) {
                try {
                    ih -l_8_R = (ih) -l_3_R.get(-l_7_I);
                    Object -l_9_R = -l_8_R.sendMessage(-l_6_R);
                    if (-l_9_R == null) {
                        -l_3_R.remove(-l_8_R);
                    } else {
                        int -l_10_I = -l_9_R.bundle().getBoolean("blocked");
                        Object -l_11_R = hp.aC(-l_9_R.bundle().getString("information"));
                        if (-l_11_R != null) {
                            list.add(-l_11_R);
                        }
                        if (-l_10_I != 0) {
                            -l_4_R.add(-l_8_R);
                        }
                    }
                    -l_7_I--;
                } catch (Object -l_7_R) {
                    -l_7_R.printStackTrace();
                }
            }
            return -l_4_R.size() != 0 ? -l_4_R : -l_3_R;
        }

        private void a(BroadcastReceiver broadcastReceiver) {
            if (broadcastReceiver != null) {
                try {
                    broadcastReceiver.abortBroadcast();
                } catch (Object -l_2_R) {
                    f.e("abortBroadcast", -l_2_R);
                }
            }
        }

        private void a(List<ih> list, SmsEntity smsEntity, ArrayList<hp> arrayList) {
            Object -l_4_R = new DataEntity(1);
            Object -l_5_R = -l_4_R.bundle();
            -l_5_R.putByteArray("sms", SmsEntity.marshall(smsEntity));
            -l_5_R.putString("event_sender", b(list));
            -l_5_R.putString("informations", hp.a((List) arrayList));
            try {
                for (ih -l_8_R : list) {
                    -l_8_R.sendMessage(-l_4_R);
                }
            } catch (Object -l_7_R) {
                -l_7_R.printStackTrace();
            }
        }

        private String b(List<ih> list) {
            Object -l_2_R = TMSDKContext.getApplicaionContext().getPackageName();
            Object -l_3_R = new DataEntity(4);
            try {
                for (ih -l_5_R : list) {
                    Object -l_6_R = -l_5_R.sendMessage(-l_3_R);
                    if (-l_6_R != null && -l_6_R.bundle().getBoolean("support_this_phone")) {
                        -l_2_R = -l_6_R.bundle().getString("pkg");
                        break;
                    }
                }
            } catch (Object -l_4_R) {
                -l_4_R.printStackTrace();
            }
            return -l_2_R;
        }

        public boolean c(final SmsEntity smsEntity, Object... objArr) {
            BroadcastReceiver -l_3_R = (BroadcastReceiver) objArr[0];
            if (this.qv.bk() < 2) {
                return true;
            }
            a(-l_3_R);
            im.bJ().a(new Runnable(this) {
                final /* synthetic */ c qw;

                public void run() {
                    List -l_1_R = new ArrayList();
                    this.qw.a((List) this.qw.a(smsEntity, -l_1_R), smsEntity, (ArrayList) -l_1_R);
                }
            }, "onCallingNotifyDataReachedThread");
            return false;
        }
    }

    private static StringBuffer f(Context context) {
        Object -l_1_R = new StringBuffer();
        Cursor cursor = null;
        try {
            Object -l_3_R = context.getContentResolver().query(Uri.parse("content://sms"), null, null, null, "_id limit 0,1");
            if (-l_3_R != null) {
                int -l_4_I = -l_3_R.getColumnCount();
                for (int -l_5_I = 0; -l_5_I < -l_4_I; -l_5_I++) {
                    -l_1_R.append(-l_5_I).append("=").append(-l_3_R.getColumnName(-l_5_I)).append(",");
                }
            }
            if (-l_3_R != null) {
                try {
                    -l_3_R.close();
                } catch (Object -l_4_R) {
                    f.e("getColumnInfo", -l_4_R);
                }
            }
        } catch (Object -l_4_R2) {
            f.e("getColumnInfo", -l_4_R2);
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_4_R22) {
                    f.e("getColumnInfo", -l_4_R22);
                }
            }
        } catch (Throwable th) {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Object -l_8_R) {
                    f.e("getColumnInfo", -l_8_R);
                }
            }
        }
        return -l_1_R;
    }

    public DataFilter<SmsEntity> getDataFilter() {
        return new b(this.mContext);
    }

    public DataHandler getDataHandler() {
        return new DataHandler();
    }

    public DataMonitor<SmsEntity> getDataMonitor() {
        return new a();
    }

    public String getName() {
        return DataInterceptorBuilder.TYPE_INCOMING_SMS;
    }
}
