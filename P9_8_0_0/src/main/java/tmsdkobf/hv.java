package tmsdkobf;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Telephony.MmsSms;
import tmsdk.bg.creator.ManagerCreatorB;
import tmsdk.bg.module.aresengine.AresEngineManager;
import tmsdk.bg.module.aresengine.DataFilter;
import tmsdk.bg.module.aresengine.DataHandler;
import tmsdk.bg.module.aresengine.DataInterceptorBuilder;
import tmsdk.bg.module.aresengine.DataMonitor;
import tmsdk.bg.module.aresengine.OutgoingSmsFilter;
import tmsdk.common.TMSDKContext;
import tmsdk.common.module.aresengine.FilterConfig;
import tmsdk.common.module.aresengine.FilterResult;
import tmsdk.common.module.aresengine.SmsEntity;
import tmsdk.common.module.aresengine.TelephonyEntity;

public final class hv extends DataInterceptorBuilder<SmsEntity> {
    private Context mContext = TMSDKContext.getApplicaionContext();

    private static final class a extends OutgoingSmsFilter {
        private hm qb = new hm();
        private AresEngineManager qc = ((AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class));

        public a(Context context) {
            this.qb.a(1);
            this.qb.a(1, new a(this) {
                final /* synthetic */ a qI;

                {
                    this.qI = r1;
                }

                boolean br() {
                    if (bn() == 2) {
                        if (this.qI.qc.getAresEngineFactor().getPrivateListDao().contains(bm().phonenum, 1)) {
                            return true;
                        }
                    }
                    return false;
                }

                void bs() {
                    final Object -l_1_R = new FilterResult();
                    -l_1_R.mFilterfiled = bp();
                    -l_1_R.mState = bn();
                    -l_1_R.mData = bm();
                    -l_1_R.mDotos.add(new Runnable(this) {
                        final /* synthetic */ AnonymousClass1 qJ;

                        public void run() {
                            Object -l_1_R;
                            SmsEntity -l_1_R2 = (SmsEntity) -l_1_R.mData;
                            Object -l_2_R = this.qJ.qI.qc.getAresEngineFactor();
                            -l_2_R.getSysDao().remove(-l_1_R2);
                            Object -l_3_R = -l_2_R.getEntityConverter();
                            if (-l_3_R != null) {
                                -l_1_R = -l_3_R.convert(-l_1_R2);
                            }
                            -l_2_R.getPrivateSmsDao().insert(-l_1_R, -l_1_R);
                        }
                    });
                    a((FilterResult) -l_1_R);
                }
            });
        }

        protected /* synthetic */ FilterResult a(TelephonyEntity telephonyEntity, Object[] objArr) {
            return b((SmsEntity) telephonyEntity, objArr);
        }

        protected FilterResult b(SmsEntity smsEntity, Object... objArr) {
            return this.qb.a(smsEntity, getConfig(), objArr);
        }

        public FilterConfig defalutFilterConfig() {
            Object -l_1_R = new FilterConfig();
            -l_1_R.set(1, 2);
            return -l_1_R;
        }
    }

    private static final class b extends DataMonitor<SmsEntity> {
        private Context mContext;
        private ContentObserver qK;

        public b(Context context) {
            this.mContext = context;
            register();
        }

        private void register() {
            this.qK = new ContentObserver(this, new Handler()) {
                final /* synthetic */ b qL;

                public void onChange(boolean z) {
                    super.onChange(z);
                    try {
                        Object -l_3_R = ((AresEngineManager) ManagerCreatorB.getManager(AresEngineManager.class)).getAresEngineFactor().getSysDao().getLastSentSms(10);
                        if (-l_3_R != null) {
                            Object -l_4_R = this.qL.mContext.getContentResolver();
                            -l_4_R.unregisterContentObserver(this);
                            this.qL.notifyDataReached(-l_3_R, new Object[0]);
                            -l_4_R.registerContentObserver(MmsSms.CONTENT_CONVERSATIONS_URI, true, this);
                        }
                    } catch (NullPointerException e) {
                    }
                }
            };
            this.mContext.getContentResolver().registerContentObserver(MmsSms.CONTENT_CONVERSATIONS_URI, true, this.qK);
        }

        private void unregister() {
            if (this.qK != null) {
                this.mContext.getContentResolver().unregisterContentObserver(this.qK);
            }
        }

        protected void finalize() throws Throwable {
            unregister();
            super.finalize();
        }
    }

    public DataFilter<SmsEntity> getDataFilter() {
        return new a(this.mContext);
    }

    public DataHandler getDataHandler() {
        return new DataHandler();
    }

    public DataMonitor<SmsEntity> getDataMonitor() {
        return new b(this.mContext);
    }

    public String getName() {
        return DataInterceptorBuilder.TYPE_OUTGOING_SMS;
    }
}
