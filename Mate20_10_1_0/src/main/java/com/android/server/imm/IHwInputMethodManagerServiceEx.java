package com.android.server.imm;

import android.content.Context;
import android.view.inputmethod.CursorAnchorInfo;
import com.huawei.android.inputmethod.IHwInputContentListener;
import com.huawei.android.inputmethod.IHwInputMethodListener;

public interface IHwInputMethodManagerServiceEx {
    boolean isTriNavigationBar(Context context);

    void onContentChanged(String str);

    void onFinishInput();

    void onReceivedComposingText(String str);

    void onReceivedInputContent(String str);

    void onShowInputRequested();

    void onStartInput();

    void onUpdateCursorAnchorInfo(CursorAnchorInfo cursorAnchorInfo);

    void registerInputContentListener(IHwInputContentListener iHwInputContentListener);

    void registerInputMethodListener(IHwInputMethodListener iHwInputMethodListener);

    void unregisterInputContentListener();

    void unregisterInputMethodListener();
}
