package com.android.server.audio;

public interface IHwAudioServiceEx {
    boolean checkRecordActive(int i);

    int getRecordConcurrentType(String str);

    void hideHiResIconDueKilledAPP(boolean z, String str);

    boolean isHwKaraokeEffectEnable(String str);

    void notifyHiResIcon(int i);

    void onSetSoundEffectState(int i, int i2);

    void processAudioServerRestart();

    void sendAudioRecordStateChangedIntent(String str, int i, int i2, String str2);

    boolean setDolbyEffect(int i);

    int setSoundEffectState(boolean z, String str, boolean z2, String str2);

    void setSystemReady();

    void updateMicIcon();

    void updateTypeCNotify(int i, int i2);
}
