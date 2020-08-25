package com.android.server.wm;

import java.util.function.Consumer;

/* renamed from: com.android.server.wm.-$$Lambda$WindowManagerService$QGTApvQkj7JVfTvOVrLJ6s24-v8  reason: invalid class name */
/* compiled from: lambda */
public final /* synthetic */ class $$Lambda$WindowManagerService$QGTApvQkj7JVfTvOVrLJ6s24v8 implements Consumer {
    public static final /* synthetic */ $$Lambda$WindowManagerService$QGTApvQkj7JVfTvOVrLJ6s24v8 INSTANCE = new $$Lambda$WindowManagerService$QGTApvQkj7JVfTvOVrLJ6s24v8();

    private /* synthetic */ $$Lambda$WindowManagerService$QGTApvQkj7JVfTvOVrLJ6s24v8() {
    }

    @Override // java.util.function.Consumer
    public final void accept(Object obj) {
        ((DisplayContent) obj).getInputMonitor().updateInputWindowsImmediately();
    }
}
