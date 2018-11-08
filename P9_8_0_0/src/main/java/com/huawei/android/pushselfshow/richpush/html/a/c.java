package com.huawei.android.pushselfshow.richpush.html.a;

import android.media.AudioManager;
import android.media.SoundPool;
import android.media.SoundPool.OnLoadCompleteListener;

class c implements OnLoadCompleteListener {
    final /* synthetic */ a a;

    c(a aVar) {
        this.a = aVar;
    }

    public void onLoadComplete(SoundPool soundPool, int i, int i2) {
        com.huawei.android.pushagent.a.a.c.b("PushSelfShowLog", "onSensorChanged and SoundPool onLoadComplete" + i);
        AudioManager -l_4_R = (AudioManager) this.a.e.getSystemService("audio");
        String str = "PushSelfShowLog";
        com.huawei.android.pushagent.a.a.c.a(str, "actualVolume is " + ((float) -l_4_R.getStreamVolume(3)));
        float -l_6_F = (float) -l_4_R.getStreamMaxVolume(3);
        -l_4_R.setStreamVolume(3, (((int) -l_6_F) * 2) / 3, 0);
        com.huawei.android.pushagent.a.a.c.a("PushSelfShowLog", "maxVolume is " + -l_6_F);
        soundPool.play(i, 1.0f, 1.0f, 1, 0, 1.0f);
    }
}
