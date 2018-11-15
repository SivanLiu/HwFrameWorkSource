package com.android.server.wm;

import android.view.SurfaceControl.Builder;
import android.view.SurfaceSession;

interface SurfaceBuilderFactory {
    Builder make(SurfaceSession surfaceSession);
}
