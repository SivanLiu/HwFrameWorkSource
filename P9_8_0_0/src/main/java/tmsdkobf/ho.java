package tmsdkobf;

import android.content.Context;
import android.text.TextUtils;
import tmsdk.bg.creator.ManagerCreatorB;
import tmsdk.bg.module.aresengine.AresEngineManager;
import tmsdk.bg.module.aresengine.DataFilter;
import tmsdk.bg.module.aresengine.DataHandler;
import tmsdk.bg.module.aresengine.DataInterceptorBuilder;
import tmsdk.bg.module.aresengine.DataMonitor;
import tmsdk.bg.module.aresengine.IncomingCallFilter;
import tmsdk.common.DualSimTelephonyManager;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.aresengine.AbsSysDao;
import tmsdk.common.module.aresengine.CallLogEntity;
import tmsdk.common.module.aresengine.FilterConfig;
import tmsdk.common.module.aresengine.FilterResult;
import tmsdk.common.module.aresengine.IContactDao;
import tmsdk.common.module.aresengine.ILastCallLogDao;
import tmsdk.common.utils.f;

public final class ho extends DataInterceptorBuilder<CallLogEntity> {
    public static long pZ = 0;
    private Context mContext;

    private static class a {
        static ho qa = new ho();
    }

    private static final class b extends IncomingCallFilter {
        private hm qb = new hm();
        private AresEngineManager qc = ((AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class));

        b(Context context) {
            this.qb.a(64, 1, 2, 4, 8, 16, 32);
            this.qb.a(64, ac(64));
            this.qb.a(1, ac(1));
            this.qb.a(2, ac(2));
            this.qb.a(4, ac(4));
            this.qb.a(8, ac(8));
            this.qb.a(16, ac(16));
            this.qb.a(32, ac(32));
        }

        private a ac(final int i) {
            return new a(this) {
                final /* synthetic */ b qe;

                boolean br() {
                    if (bn() != 0 && bn() != 1) {
                        return false;
                    }
                    Object -l_1_R = null;
                    switch (i) {
                        case 1:
                            -l_1_R = this.qe.qc.getAresEngineFactor().getPrivateListDao();
                            break;
                        case 2:
                            -l_1_R = this.qe.qc.getAresEngineFactor().getWhiteListDao();
                            break;
                        case 4:
                            -l_1_R = this.qe.qc.getAresEngineFactor().getBlackListDao();
                            break;
                        case 8:
                            -l_1_R = this.qe.qc.getAresEngineFactor().getSysDao();
                            break;
                        case 16:
                            -l_1_R = this.qe.qc.getAresEngineFactor().getLastCallLogDao();
                            break;
                        case 32:
                            -l_1_R = null;
                            break;
                    }
                    if (i == 64) {
                        return TextUtils.isEmpty(bm().phonenum);
                    }
                    if (i == 32) {
                        return true;
                    }
                    if (-l_1_R instanceof IContactDao) {
                        return ((IContactDao) -l_1_R).contains(bm().phonenum, 0);
                    }
                    if (-l_1_R instanceof ILastCallLogDao) {
                        return ((ILastCallLogDao) -l_1_R).contains(bm().phonenum);
                    }
                    return !(-l_1_R instanceof AbsSysDao) ? false : ((AbsSysDao) -l_1_R).contains(bm().phonenum);
                }

                void bs() {
                    Object -l_1_R = new FilterResult();
                    -l_1_R.mData = bm();
                    -l_1_R.mParams = bo();
                    -l_1_R.mState = bn();
                    -l_1_R.mFilterfiled = bp();
                    if (bn() != 0 && bn() == 1) {
                        -l_1_R.isBlocked = true;
                        CallLogEntity -l_2_R = (CallLogEntity) -l_1_R.mData;
                        Object -l_3_R = im.rE;
                        Object -l_4_R = null;
                        if (-l_3_R != null) {
                            if (-l_2_R.fromCard == null || -l_2_R.fromCard.equals(-l_3_R.bT(0))) {
                                -l_4_R = DualSimTelephonyManager.getDefaultTelephony();
                            } else if (-l_2_R.fromCard.equals(-l_3_R.bT(1))) {
                                -l_4_R = DualSimTelephonyManager.getSecondTelephony();
                            }
                        }
                        int -l_5_I = 0;
                        if (-l_4_R != null) {
                            try {
                                -l_5_I = -l_4_R.endCall();
                            } catch (Object -l_6_R) {
                                f.b("IncomingCallInterceptorBuilder", "endCall", -l_6_R);
                            }
                        }
                        f.f("IncomingCallInterceptorBuilder", "endCall1 " + -l_5_I);
                        if (-l_5_I == 0) {
                            -l_5_I = this.qe.qc.getAresEngineFactor().getPhoneDeviceController().hangup();
                            f.f("IncomingCallInterceptorBuilder", "endCall2 " + -l_5_I);
                        }
                        if (-l_5_I == 0) {
                            long -l_6_J = System.currentTimeMillis();
                            f.f("IncomingCallInterceptorBuilder", "now-lastCallEndTime" + (-l_6_J - ho.pZ));
                            f.f("IncomingCallInterceptorBuilder", "now" + -l_6_J);
                            f.f("IncomingCallInterceptorBuilder", "lastCallEndTime" + ho.pZ);
                            if (!(ho.pZ <= 0) && -l_6_J > ho.pZ) {
                                int i = 0;
                            }
                        }
                    }
                    a((FilterResult) -l_1_R);
                }
            };
        }

        protected FilterResult a(CallLogEntity callLogEntity, Object... objArr) {
            return this.qb.a(callLogEntity, getConfig(), objArr);
        }

        public FilterConfig defalutFilterConfig() {
            Object -l_1_R = new FilterConfig();
            -l_1_R.set(1, 0);
            -l_1_R.set(2, 0);
            -l_1_R.set(4, 1);
            -l_1_R.set(8, 0);
            -l_1_R.set(16, 0);
            -l_1_R.set(32, 0);
            -l_1_R.set(64, 0);
            return -l_1_R;
        }
    }

    private static final class c extends DataMonitor<CallLogEntity> {
        private tmsdkobf.hw.b qf = new tmsdkobf.hw.b(this) {
            final /* synthetic */ c qg;

            {
                this.qg = r1;
            }

            public void aA(String str) {
                ho.pZ = System.currentTimeMillis();
            }

            public void aB(String str) {
                ho.pZ = 0;
            }

            public void az(String str) {
            }

            public void g(String str, String str2) {
                ho.pZ = 0;
                Object -l_3_R = new CallLogEntity();
                -l_3_R.phonenum = str;
                -l_3_R.type = 1;
                -l_3_R.date = System.currentTimeMillis();
                -l_3_R.fromCard = str2;
                this.qg.notifyDataReached(-l_3_R, new Object[0]);
            }
        };

        public c(Context context) {
            hw.bx().a(this.qf);
        }

        protected void finalize() throws Throwable {
            hw.bx().b(this.qf);
            super.finalize();
        }
    }

    private ho() {
        this.mContext = TMSDKContext.getApplicaionContext();
    }

    public static ho bu() {
        return a.qa;
    }

    public DataFilter<CallLogEntity> getDataFilter() {
        return new b(this.mContext);
    }

    public DataHandler getDataHandler() {
        return new DataHandler();
    }

    public DataMonitor<CallLogEntity> getDataMonitor() {
        return new c(this.mContext);
    }

    public String getName() {
        return DataInterceptorBuilder.TYPE_INCOMING_CALL;
    }
}
