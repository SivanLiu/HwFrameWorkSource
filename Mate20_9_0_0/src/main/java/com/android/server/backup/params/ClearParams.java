package com.android.server.backup.params;

import android.content.pm.PackageInfo;
import com.android.server.backup.internal.OnTaskFinishedListener;
import com.android.server.backup.transport.TransportClient;

public class ClearParams {
    public OnTaskFinishedListener listener;
    public PackageInfo packageInfo;
    public TransportClient transportClient;

    public ClearParams(TransportClient transportClient, PackageInfo packageInfo, OnTaskFinishedListener listener) {
        this.transportClient = transportClient;
        this.packageInfo = packageInfo;
        this.listener = listener;
    }
}
