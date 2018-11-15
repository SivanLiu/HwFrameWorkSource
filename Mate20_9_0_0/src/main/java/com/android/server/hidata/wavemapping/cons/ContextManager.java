package com.android.server.hidata.wavemapping.cons;

import android.content.Context;

public class ContextManager {
    private static final String TAG;
    private static ContextManager instance = null;
    private Context context = null;

    static {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WMapping.");
        stringBuilder.append(ContextManager.class.getSimpleName());
        TAG = stringBuilder.toString();
    }

    public static synchronized ContextManager getInstance() {
        ContextManager contextManager;
        synchronized (ContextManager.class) {
            if (instance == null) {
                instance = new ContextManager();
            }
            contextManager = instance;
        }
        return contextManager;
    }

    public Context getContext() {
        return this.context;
    }

    public void setContext(Context context) {
        if (this.context == null) {
            this.context = context;
        }
    }
}
