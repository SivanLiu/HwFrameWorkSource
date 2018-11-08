package tmsdk.bg.module.aresengine;

import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import tmsdk.bg.creator.BaseManagerB;
import tmsdk.common.ErrorCode;
import tmsdk.common.module.aresengine.SmsEntity;
import tmsdk.common.module.aresengine.TelephonyEntity;
import tmsdkobf.hh;
import tmsdkobf.ic;
import tmsdkobf.kt;

public final class AresEngineManager extends BaseManagerB {
    private hh tP;
    private Map<String, DataInterceptor<? extends TelephonyEntity>> tQ;
    private b tR;

    public void addInterceptor(DataInterceptorBuilder<? extends TelephonyEntity> dataInterceptorBuilder) throws RuntimeException {
        if (!ic.bE()) {
            this.tP.addInterceptor(dataInterceptorBuilder);
        }
    }

    public DataInterceptor<? extends TelephonyEntity> findInterceptor(String str) {
        if (!ic.bE()) {
            return this.tP.findInterceptor(str);
        }
        if (this.tQ == null) {
            this.tQ = new HashMap();
            Object -l_2_R = new String[]{DataInterceptorBuilder.TYPE_INCOMING_CALL, DataInterceptorBuilder.TYPE_INCOMING_SMS, DataInterceptorBuilder.TYPE_OUTGOING_SMS, DataInterceptorBuilder.TYPE_SYSTEM_CALL};
            Object -l_3_R = -l_2_R;
            int -l_4_I = -l_2_R.length;
            for (int -l_5_I = 0; -l_5_I < -l_4_I; -l_5_I++) {
                Object -l_6_R = -l_3_R[-l_5_I];
                this.tQ.put(-l_6_R, new a(-l_6_R));
            }
        }
        return (DataInterceptor) this.tQ.get(str);
    }

    public AresEngineFactor getAresEngineFactor() {
        return this.tP.getAresEngineFactor();
    }

    public IntelliSmsChecker getIntelligentSmsChecker() {
        if (ic.bE()) {
            if (this.tR == null) {
                this.tR = new b();
            }
            return this.tR;
        }
        kt.saveActionData(29947);
        return this.tP.bh();
    }

    public List<DataInterceptor<? extends TelephonyEntity>> interceptors() {
        return !ic.bE() ? this.tP.interceptors() : new ArrayList();
    }

    public void onCreate(Context context) {
        this.tP = new hh();
        this.tP.onCreate(context);
        a(this.tP);
    }

    public void reportRecoverSms(LinkedHashMap<SmsEntity, Integer> linkedHashMap, ISmsReportCallBack iSmsReportCallBack) {
        if (iSmsReportCallBack == null) {
            return;
        }
        if (ic.bE()) {
            iSmsReportCallBack.onReprotFinish(ErrorCode.ERR_LICENSE_EXPIRED);
        } else if (linkedHashMap != null && linkedHashMap.size() > 0) {
            this.tP.reportRecoverSms(linkedHashMap, iSmsReportCallBack);
        } else {
            iSmsReportCallBack.onReprotFinish(-6);
        }
    }

    public final boolean reportSms(List<SmsEntity> list) {
        if (ic.bE()) {
            return false;
        }
        kt.saveActionData(29946);
        return this.tP.reportSms(list);
    }

    public void setAresEngineFactor(AresEngineFactor aresEngineFactor) {
        this.tP.setAresEngineFactor(aresEngineFactor);
    }
}
