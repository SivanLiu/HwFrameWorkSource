package com.android.server.security.tsmagent.server.card.impl;

import android.content.Context;
import com.android.server.security.tsmagent.constant.ServiceConfig;
import com.android.server.security.tsmagent.server.CardServerBaseResponse;
import com.android.server.security.tsmagent.server.card.request.TsmParamQueryRequest;
import com.android.server.security.tsmagent.server.card.response.TsmParamQueryResponse;
import com.android.server.security.tsmagent.server.wallet.impl.DicsQueryTask;
import com.android.server.security.tsmagent.server.wallet.request.QueryDicsRequset;
import com.android.server.security.tsmagent.server.wallet.response.QueryDicsResponse;
import com.android.server.security.tsmagent.utils.HwLog;
import com.android.server.security.tsmagent.utils.PackageUtil;

public class CardServer {
    private static volatile CardServer sInstance;
    private final Context mContext;
    private final String serverTotalUrl;

    public static CardServer getInstance(Context context) {
        if (sInstance == null) {
            synchronized (CardServer.class) {
                if (sInstance == null) {
                    sInstance = new CardServer(context);
                }
            }
        }
        return sInstance;
    }

    public CardServer(Context context) {
        this.mContext = context;
        int versionCode = PackageUtil.getVersionCode(this.mContext);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(ServiceConfig.CARD_INFO_MANAGE_SERVER_URL);
        stringBuilder.append("?clientVersion=");
        stringBuilder.append(versionCode);
        this.serverTotalUrl = stringBuilder.toString();
    }

    public TsmParamQueryResponse queryDeleteSSDTsmParam(TsmParamQueryRequest request) {
        HwLog.i("queryDeleteSSDTsmParam begin.");
        CardServerBaseResponse response = new TsmParamQueryTask(this.mContext, this.serverTotalUrl, "nfc.get.del.SSD").processTask(request);
        HwLog.i("queryDeleteSSDTsmParam end.");
        if (response instanceof TsmParamQueryResponse) {
            return (TsmParamQueryResponse) response;
        }
        return null;
    }

    public QueryDicsResponse queryDics(QueryDicsRequset request) {
        HwLog.i("queryDics begin.");
        CardServerBaseResponse response = new DicsQueryTask(this.mContext, this.serverTotalUrl).processTask(request);
        if (response instanceof QueryDicsResponse) {
            return (QueryDicsResponse) response;
        }
        HwLog.i("queryDics end.");
        return null;
    }

    public TsmParamQueryResponse queryCreateSSDTsmParam(TsmParamQueryRequest request) {
        HwLog.i("queryCreateSSDTsmParam begin.");
        CardServerBaseResponse response = new TsmParamQueryTask(this.mContext, this.serverTotalUrl, "nfc.get.create.SSD").processTask(request);
        HwLog.i("queryCreateSSDTsmParam end.");
        if (response instanceof TsmParamQueryResponse) {
            return (TsmParamQueryResponse) response;
        }
        return null;
    }

    public TsmParamQueryResponse queryInfoInitTsmParam(TsmParamQueryRequest request) {
        HwLog.i("queryInfoInitTsmParam begin.");
        CardServerBaseResponse response = new TsmParamQueryTask(this.mContext, this.serverTotalUrl, "nfc.get.NotifyEseInfoSync").processTask(request);
        HwLog.i("queryInfoInitTsmParam end.");
        if (response instanceof TsmParamQueryResponse) {
            return (TsmParamQueryResponse) response;
        }
        return null;
    }
}
