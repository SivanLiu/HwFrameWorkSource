package tmsdk.bg.module.network;

import android.content.Context;
import java.util.ArrayList;
import tmsdk.common.TMSDKContext;
import tmsdk.common.TMServiceFactory;
import tmsdk.common.creator.ManagerCreatorC;
import tmsdk.common.module.network.TrafficEntity;
import tmsdk.common.utils.i;
import tmsdkobf.eb;
import tmsdkobf.jd;
import tmsdkobf.jx;
import tmsdkobf.pg;
import tmsdkobf.ph;

final class a {
    private Context mContext = TMSDKContext.getApplicaionContext();
    private jx un = jd.b(this.mContext, "traffic_xml");
    private k uo = new k();

    public a() {
        ((ph) ManagerCreatorC.getManager(ph.class)).c(new pg(this) {
            final /* synthetic */ a up;

            {
                this.up = r1;
            }

            private void ba(String str) {
                this.up.clearTrafficInfo(new String[]{str});
            }

            public void aQ(String str) {
                ba(str);
            }

            public void aR(String str) {
            }

            public void aS(String str) {
            }
        });
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private TrafficEntity a(TrafficEntity trafficEntity, String str, String str2) {
        int -l_4_I = aZ(str);
        int -l_5_I = 0;
        if (trafficEntity == null && -l_4_I != -1) {
            trafficEntity = new TrafficEntity();
            trafficEntity.mPkg = str;
            -l_5_I = 1;
        }
        if (trafficEntity != null) {
            if (str2 == null) {
                str2 = this.un.getString("last_connection_type", null);
            }
            long -l_6_J = this.uo.getUidTxBytes(-l_4_I);
            long -l_8_J = this.uo.getUidRxBytes(-l_4_I);
            if (-l_5_I != 0) {
                trafficEntity.mLastDownValue = -l_8_J;
                trafficEntity.mLastUpValue = -l_6_J;
            }
            if (-l_6_J == -1) {
                if ((trafficEntity.mMobileUpValue > 0 ? 1 : null) == null) {
                }
                -l_6_J = 0;
            }
            if (-l_8_J == -1) {
                if ((trafficEntity.mMobileDownValue > 0 ? 1 : null) == null) {
                }
                -l_8_J = 0;
            }
            long -l_10_J = -l_6_J - trafficEntity.mLastUpValue;
            long -l_12_J = -l_8_J - trafficEntity.mLastDownValue;
            if ((-l_10_J >= 0 ? 1 : null) == null) {
                -l_10_J = -l_6_J;
            }
            if ((-l_12_J >= 0 ? 1 : null) == null) {
                -l_12_J = -l_6_J;
            }
            if (-l_6_J == -1 && -l_8_J == -1) {
                trafficEntity.mMobileDownValue = 0;
                trafficEntity.mMobileUpValue = 0;
                trafficEntity.mWIFIDownValue = 0;
                trafficEntity.mWIFIUpValue = 0;
            } else if (-l_6_J != -1 || -l_8_J == -1) {
                long j;
                if (-l_6_J == -1 || -l_8_J != -1) {
                    if (str2.equals("mobile")) {
                        trafficEntity.mMobileDownValue += -l_12_J;
                        j = trafficEntity.mMobileUpValue;
                        trafficEntity.mMobileUpValue = j + -l_10_J;
                    } else {
                        if (str2.equals("wifi")) {
                            trafficEntity.mWIFIDownValue += -l_12_J;
                            j = trafficEntity.mWIFIUpValue;
                        }
                    }
                } else {
                    trafficEntity.mMobileDownValue = 0;
                    trafficEntity.mWIFIDownValue = 0;
                    if (str2.equals("mobile")) {
                        j = trafficEntity.mMobileUpValue;
                        trafficEntity.mMobileUpValue = j + -l_10_J;
                    } else {
                        if (str2.equals("wifi")) {
                            j = trafficEntity.mWIFIUpValue;
                        }
                    }
                }
                trafficEntity.mWIFIUpValue = j + -l_10_J;
            } else {
                trafficEntity.mMobileUpValue = 0;
                trafficEntity.mWIFIUpValue = 0;
                if (str2.equals("mobile")) {
                    trafficEntity.mMobileDownValue += -l_12_J;
                } else {
                    if (str2.equals("wifi")) {
                        trafficEntity.mWIFIDownValue += -l_12_J;
                    }
                }
            }
            trafficEntity.mLastUpValue = -l_6_J;
            trafficEntity.mLastDownValue = -l_8_J;
            this.un.putString(str, trafficEntity.toString());
        }
        return trafficEntity;
    }

    private int aZ(String str) {
        Object -l_2_R = TMServiceFactory.getSystemInfoService().a(str, 1);
        return -l_2_R == null ? -1 : -l_2_R.getUid();
    }

    public void clearTrafficInfo(String[] -l_2_R) {
        for (Object -l_5_R : -l_2_R) {
            int -l_6_I = aZ(-l_5_R);
            Object -l_7_R = getTrafficEntity(-l_5_R);
            if (-l_6_I == -1 || -l_7_R == null) {
                this.un.putString(-l_5_R, "EMPTY");
            } else {
                -l_7_R.mLastUpValue = this.uo.getUidTxBytes(-l_6_I);
                -l_7_R.mLastDownValue = this.uo.getUidRxBytes(-l_6_I);
                -l_7_R.mMobileDownValue = 0;
                -l_7_R.mMobileUpValue = 0;
                -l_7_R.mWIFIDownValue = 0;
                -l_7_R.mWIFIUpValue = 0;
                this.un.putString(-l_5_R, -l_7_R.toString());
            }
        }
    }

    public long getMobileRxBytes(String str) {
        Object -l_2_R = getTrafficEntity(str);
        return -l_2_R == null ? -1 : -l_2_R.mMobileDownValue;
    }

    public long getMobileTxBytes(String str) {
        Object -l_2_R = getTrafficEntity(str);
        return -l_2_R == null ? -1 : -l_2_R.mMobileUpValue;
    }

    public TrafficEntity getTrafficEntity(String str) {
        int -l_3_I = aZ(str);
        Object -l_4_R = this.un.getString(str, null);
        return (-l_3_I == -1 || -l_4_R == null || "EMPTY".equals(-l_4_R)) ? null : TrafficEntity.fromString(-l_4_R);
    }

    public long getWIFIRxBytes(String str) {
        Object -l_2_R = getTrafficEntity(str);
        return -l_2_R == null ? -1 : -l_2_R.mWIFIDownValue;
    }

    public long getWIFITxBytes(String str) {
        Object -l_2_R = getTrafficEntity(str);
        return -l_2_R == null ? -1 : -l_2_R.mWIFIUpValue;
    }

    public boolean isSupportTrafficState() {
        return this.uo.isSupportTrafficState();
    }

    public ArrayList<TrafficEntity> refreshTrafficInfo(String[] strArr, boolean z) {
        Object -l_3_R = new ArrayList();
        eb -l_4_R = i.iG();
        Object -l_5_R = -l_4_R != eb.iJ ? -l_4_R != eb.iH ? "mobile" : "none" : "wifi";
        Object -l_6_R = this.un.getString("last_connection_type", null);
        if (-l_6_R != null) {
            Object -l_7_R = -l_5_R;
            if (-l_5_R.equals(-l_6_R) && !z) {
                return -l_3_R;
            }
        }
        -l_6_R = -l_5_R;
        for (int -l_7_I = 0; -l_7_I < strArr.length; -l_7_I++) {
            Object -l_8_R = a(getTrafficEntity(strArr[-l_7_I]), strArr[-l_7_I], -l_6_R);
            if (-l_8_R != null) {
                -l_3_R.add(-l_8_R);
            }
        }
        this.un.putString("last_connection_type", -l_5_R);
        return -l_3_R;
    }

    public void refreshTrafficInfo(ArrayList<TrafficEntity> arrayList) {
        if (arrayList != null && !arrayList.isEmpty()) {
            eb -l_2_R = i.iG();
            Object -l_3_R = -l_2_R != eb.iJ ? -l_2_R != eb.iH ? "mobile" : "none" : "wifi";
            Object -l_4_R = this.un.getString("last_connection_type", null);
            if (-l_4_R == null) {
                -l_4_R = -l_3_R;
            }
            Object -l_5_R = arrayList.iterator();
            while (-l_5_R.hasNext()) {
                TrafficEntity -l_6_R = (TrafficEntity) -l_5_R.next();
                a(-l_6_R, -l_6_R.mPkg, -l_4_R);
            }
            this.un.putString("last_connection_type", -l_3_R);
        }
    }

    public void refreshTrafficInfo(TrafficEntity trafficEntity) {
        if (trafficEntity != null) {
            ArrayList -l_2_R = new ArrayList();
            -l_2_R.add(trafficEntity);
            refreshTrafficInfo(-l_2_R);
        }
    }
}
