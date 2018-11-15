package com.android.server.rms.shrinker;

import android.os.Bundle;
import android.util.Log;
import com.android.server.am.ProcessList;
import com.android.server.rms.IShrinker;

public class SystemShrinker implements IShrinker {
    static final String TAG = "RMS.SystemShrinker";

    public int reclaim(String reason, Bundle extras) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(reason);
        stringBuilder.append(" ertras:");
        stringBuilder.append(extras);
        Log.w(str, stringBuilder.toString());
        ProcessList.callMemoryShrinker(1);
        return 1;
    }

    public void interrupt() {
    }
}
