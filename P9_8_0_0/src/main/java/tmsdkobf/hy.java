package tmsdkobf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import tmsdk.bg.module.aresengine.ISmsReportCallBack;
import tmsdk.common.module.aresengine.SmsEntity;
import tmsdk.common.utils.f;
import tmsdk.common.utils.i;
import tmsdk.common.utils.q;

public class hy {
    private static cp a(SmsEntity smsEntity) {
        Object -l_1_R = new cp();
        -l_1_R.fr = null;
        -l_1_R.fk = (int) (System.currentTimeMillis() / 1000);
        -l_1_R.sender = q.cI(smsEntity.getAddress());
        -l_1_R.sms = q.cI(smsEntity.getBody());
        -l_1_R.fs = smsEntity.protocolType;
        -l_1_R.fm = -1;
        -l_1_R.fn = -1;
        -l_1_R.fl = -1;
        -l_1_R.fp = -1;
        -l_1_R.fq = new ArrayList();
        -l_1_R.fo = new ArrayList();
        -l_1_R.ft = 0;
        -l_1_R.fu = null;
        return -l_1_R;
    }

    private static void a(ArrayList<cp> arrayList, ISmsReportCallBack iSmsReportCallBack) {
        if (arrayList.size() > 0 && i.hm()) {
            Object -l_2_R = new ck();
            -l_2_R.eZ = arrayList;
            im.bK().a(801, -l_2_R, null, 0, iSmsReportCallBack, 180000);
            return;
        }
        f.h("SmsReport", "not connected!");
        iSmsReportCallBack.onReprotFinish(-52);
    }

    public static void reportRecoverSms(LinkedHashMap<SmsEntity, Integer> linkedHashMap, ISmsReportCallBack iSmsReportCallBack) {
        Object -l_2_R = new ArrayList();
        for (SmsEntity -l_5_R : linkedHashMap.keySet()) {
            Object -l_6_R = a(-l_5_R);
            Object -l_7_R = new cs();
            if (((Integer) linkedHashMap.get(-l_5_R)).intValue() != 0) {
                -l_7_R.fP = 12;
            } else {
                -l_7_R.fP = 24;
            }
            -l_7_R.time = (int) (System.currentTimeMillis() / 1000);
            -l_2_R.add(-l_6_R);
        }
        a(-l_2_R, iSmsReportCallBack);
    }
}
