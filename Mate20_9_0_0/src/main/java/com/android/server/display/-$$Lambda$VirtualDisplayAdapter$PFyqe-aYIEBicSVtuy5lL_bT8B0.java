package com.android.server.display;

import android.os.IBinder;
import android.view.SurfaceControl;
import com.android.server.display.VirtualDisplayAdapter.SurfaceControlDisplayFactory;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$VirtualDisplayAdapter$PFyqe-aYIEBicSVtuy5lL_bT8B0 implements SurfaceControlDisplayFactory {
    public static final /* synthetic */ -$$Lambda$VirtualDisplayAdapter$PFyqe-aYIEBicSVtuy5lL_bT8B0 INSTANCE = new -$$Lambda$VirtualDisplayAdapter$PFyqe-aYIEBicSVtuy5lL_bT8B0();

    private /* synthetic */ -$$Lambda$VirtualDisplayAdapter$PFyqe-aYIEBicSVtuy5lL_bT8B0() {
    }

    public final IBinder createDisplay(String str, boolean z) {
        return SurfaceControl.createDisplay(str, z);
    }
}
