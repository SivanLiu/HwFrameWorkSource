package com.huawei.nb.searchmanager.client;

import android.os.RemoteException;
import com.huawei.nb.searchmanager.client.model.IndexData;
import com.huawei.nb.searchmanager.client.model.Recommendation;
import com.huawei.nb.searchmanager.client.model.Token;
import com.huawei.nb.utils.logger.DSLog;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchSession {
    private static final String TAG = "SearchSession";
    private ISearchSession searchSessionProxy;

    SearchSession(ISearchSession searchSessionProxy2) {
        this.searchSessionProxy = searchSessionProxy2;
    }

    /* access modifiers changed from: package-private */
    public ISearchSession getSearchSessionProxy() {
        return this.searchSessionProxy;
    }

    public List<String> getTopFieldValues(String fieldName, int limit) {
        try {
            return this.searchSessionProxy.getTopFieldValues(fieldName, limit);
        } catch (RemoteException e) {
            DSLog.et(TAG, "getTopFieldValues remote exception: %s", e.getMessage());
            return null;
        }
    }

    public int getSearchHitCount(String queryJsonStr) {
        try {
            return this.searchSessionProxy.getSearchHitCount(queryJsonStr);
        } catch (RemoteException e) {
            DSLog.et(TAG, "getSearchHitCount remote exception: %s", e.getMessage());
            return 0;
        }
    }

    public List<IndexData> search(String queryJsonStr, int start, int limit) {
        try {
            return this.searchSessionProxy.search(queryJsonStr, start, limit);
        } catch (RemoteException e) {
            DSLog.et(TAG, "search remote exception: %s", e.getMessage());
            return null;
        }
    }

    public List<Recommendation> groupSearch(String queryJsonStr, int groupLimit) {
        try {
            return this.searchSessionProxy.groupSearch(queryJsonStr, groupLimit);
        } catch (RemoteException e) {
            DSLog.et(TAG, "group search remote exception: %s", e.getMessage());
            return null;
        }
    }

    public List<Recommendation> groupTimeline(String queryJsonStr, String field) {
        List<Recommendation> result = new ArrayList<>();
        Token token = new Token();
        while (!token.isFinish()) {
            List<Recommendation> part = doGroupTimeline(queryJsonStr, field, token);
            if (part != null && !part.isEmpty()) {
                result.addAll(part);
            }
        }
        DSLog.it(TAG, "groupTimeline total size: " + result.size(), new Object[0]);
        return result;
    }

    private List<Recommendation> doGroupTimeline(String queryJsonStr, String field, Token token) {
        try {
            return this.searchSessionProxy.groupTimeline(queryJsonStr, field, token);
        } catch (RemoteException e) {
            token.setFinish(true);
            DSLog.et(TAG, "group Timeline remote exception: %s", e.getMessage());
            return null;
        }
    }

    public Map<String, List<Recommendation>> coverSearch(String queryJsonStr, List<String> groupFields, int groupLimit) {
        try {
            return this.searchSessionProxy.coverSearch(queryJsonStr, groupFields, groupLimit);
        } catch (RemoteException e) {
            DSLog.et(TAG, "cover search remote exception: %s", e.getMessage());
            return null;
        }
    }
}
