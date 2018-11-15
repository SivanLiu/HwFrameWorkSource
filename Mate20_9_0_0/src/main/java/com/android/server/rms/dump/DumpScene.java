package com.android.server.rms.dump;

import android.content.Context;
import android.util.Log;
import com.android.server.rms.IScene;
import com.android.server.rms.scene.DownloadScene;
import com.android.server.rms.scene.MediaScene;
import com.android.server.rms.scene.NonIdleScene;
import com.android.server.rms.scene.PhoneScene;

public final class DumpScene {
    public static final void dumpDownloadScene(Context context) {
        IScene scene = new DownloadScene(context);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("DownloadScene state is ");
        stringBuilder.append(scene.identify(null));
        Log.d("RMS.dump", stringBuilder.toString());
    }

    public static final void dumpMediaScene(Context context) {
        IScene scene = new MediaScene(context);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("MediaScene state is ");
        stringBuilder.append(scene.identify(null));
        Log.d("RMS.dump", stringBuilder.toString());
    }

    public static final void dumpNonIdleScene(Context context) {
        IScene scene = new NonIdleScene(context);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NonIdleScene state is ");
        stringBuilder.append(scene.identify(null));
        Log.d("RMS.dump", stringBuilder.toString());
    }

    public static final void dumpPhoneScene(Context context) {
        IScene scene = new PhoneScene(context);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("PhoneScene state is ");
        stringBuilder.append(scene.identify(null));
        Log.d("RMS.dump", stringBuilder.toString());
    }
}
