package com.huawei.android.view;

import android.view.AbsLayoutParams;
import android.view.WindowManager.LayoutParams;

public class LayoutParamsEx extends AbsLayoutParams {
    public static final int FLAG_MMI_TEST_DEFAULT_SHAPE = 16384;
    public static final int FLAG_NOTCH_SUPPORT = 65536;
    public static final int FLAG_SECURE_SCREENCAP = 8192;
    public static final int FLAG_SECURE_SCREENSHOT = 4096;
    LayoutParams attrs;

    private void setHwFlags(int r1, int r2) {
        /* JADX: method processing error */
/*
Error: jadx.core.utils.exceptions.DecodeException: Load method exception in method: com.huawei.android.view.LayoutParamsEx.setHwFlags(int, int):void
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:116)
	at jadx.core.dex.nodes.ClassNode.load(ClassNode.java:249)
	at jadx.core.ProcessClass.process(ProcessClass.java:31)
	at jadx.api.JadxDecompiler.processClass(JadxDecompiler.java:296)
	at jadx.api.JavaClass.decompile(JavaClass.java:62)
	at jadx.api.JadxDecompiler.lambda$appendSourcesSave$0(JadxDecompiler.java:199)
Caused by: jadx.core.utils.exceptions.DecodeException: Unknown instruction: not-int
	at jadx.core.dex.instructions.InsnDecoder.decode(InsnDecoder.java:568)
	at jadx.core.dex.instructions.InsnDecoder.process(InsnDecoder.java:56)
	at jadx.core.dex.nodes.MethodNode.load(MethodNode.java:102)
	... 5 more
*/
        /*
        // Can't load method instructions.
        */
        throw new UnsupportedOperationException("Method not decompiled: com.huawei.android.view.LayoutParamsEx.setHwFlags(int, int):void");
    }

    public LayoutParamsEx(LayoutParams lp) {
        this.attrs = lp;
    }

    public int getHwFlags() {
        return this.attrs.hwFlags;
    }

    public void addHwFlags(int hwFlags) {
        setHwFlags(hwFlags, hwFlags);
    }

    public void clearHwFlags(int hwFlags) {
        setHwFlags(0, hwFlags);
    }
}
