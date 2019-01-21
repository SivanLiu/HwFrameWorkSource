package android.os;

import android.opengl.EGL14;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$GraphicsEnvironment$U4RqBlx5-Js31-71IFOgvpvoAFg implements Runnable {
    public static final /* synthetic */ -$$Lambda$GraphicsEnvironment$U4RqBlx5-Js31-71IFOgvpvoAFg INSTANCE = new -$$Lambda$GraphicsEnvironment$U4RqBlx5-Js31-71IFOgvpvoAFg();

    private /* synthetic */ -$$Lambda$GraphicsEnvironment$U4RqBlx5-Js31-71IFOgvpvoAFg() {
    }

    public final void run() {
        EGL14.eglGetDisplay(0);
    }
}
