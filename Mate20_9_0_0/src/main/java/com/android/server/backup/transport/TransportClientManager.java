package com.android.server.backup.transport;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import com.android.server.backup.TransportManager;
import java.io.PrintWriter;
import java.util.Map;
import java.util.WeakHashMap;

public class TransportClientManager {
    private static final String TAG = "TransportClientManager";
    private final Context mContext;
    private Map<TransportClient, String> mTransportClientsCallerMap = new WeakHashMap();
    private int mTransportClientsCreated = 0;
    private final Object mTransportClientsLock = new Object();
    private final TransportStats mTransportStats;

    public TransportClientManager(Context context, TransportStats transportStats) {
        this.mContext = context;
        this.mTransportStats = transportStats;
    }

    public TransportClient getTransportClient(ComponentName transportComponent, String caller) {
        return getTransportClient(transportComponent, caller, new Intent(TransportManager.SERVICE_ACTION_TRANSPORT_HOST).setComponent(transportComponent));
    }

    public TransportClient getTransportClient(ComponentName transportComponent, Bundle extras, String caller) {
        Intent bindIntent = new Intent(TransportManager.SERVICE_ACTION_TRANSPORT_HOST).setComponent(transportComponent);
        bindIntent.putExtras(extras);
        return getTransportClient(transportComponent, caller, bindIntent);
    }

    private TransportClient getTransportClient(ComponentName transportComponent, String caller, Intent bindIntent) {
        TransportClient transportClient;
        synchronized (this.mTransportClientsLock) {
            transportClient = new TransportClient(this.mContext, this.mTransportStats, bindIntent, transportComponent, Integer.toString(this.mTransportClientsCreated), caller);
            this.mTransportClientsCallerMap.put(transportClient, caller);
            this.mTransportClientsCreated++;
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Retrieving ");
            stringBuilder.append(transportClient);
            TransportUtils.log(3, str, TransportUtils.formatMessage(null, caller, stringBuilder.toString()));
        }
        return transportClient;
    }

    public void disposeOfTransportClient(TransportClient transportClient, String caller) {
        transportClient.unbind(caller);
        transportClient.markAsDisposed();
        synchronized (this.mTransportClientsLock) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Disposing of ");
            stringBuilder.append(transportClient);
            TransportUtils.log(3, str, TransportUtils.formatMessage(null, caller, stringBuilder.toString()));
            this.mTransportClientsCallerMap.remove(transportClient);
        }
    }

    public void dump(PrintWriter pw) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Transport clients created: ");
        stringBuilder.append(this.mTransportClientsCreated);
        pw.println(stringBuilder.toString());
        synchronized (this.mTransportClientsLock) {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Current transport clients: ");
            stringBuilder2.append(this.mTransportClientsCallerMap.size());
            pw.println(stringBuilder2.toString());
            for (TransportClient transportClient : this.mTransportClientsCallerMap.keySet()) {
                String caller = (String) this.mTransportClientsCallerMap.get(transportClient);
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("    ");
                stringBuilder3.append(transportClient);
                stringBuilder3.append(" [");
                stringBuilder3.append(caller);
                stringBuilder3.append("]");
                pw.println(stringBuilder3.toString());
                for (String logEntry : transportClient.getLogBuffer()) {
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("        ");
                    stringBuilder4.append(logEntry);
                    pw.println(stringBuilder4.toString());
                }
            }
        }
    }
}
