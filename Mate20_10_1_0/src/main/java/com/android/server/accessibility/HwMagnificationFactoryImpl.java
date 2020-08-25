package com.android.server.accessibility;

import android.content.Context;
import com.android.server.accessibility.HwMagnificationFactory;

public class HwMagnificationFactoryImpl implements HwMagnificationFactory.Factory {
    private static final String TAG = "HwMagnificationFactoryImpl";

    public HwMagnificationFactory.IMagnificationGestureHandler getHwMagnificationGestureHandler() {
        return new HwMagnificationGestureHandlerImpl();
    }

    public static class HwMagnificationGestureHandlerImpl implements HwMagnificationFactory.IMagnificationGestureHandler {
        public MagnificationGestureHandler getInstance(Context context, MagnificationController magnificationController, boolean detectControlGestures, boolean triggerable, int displayId) {
            return new HwMagnificationGestureHandler(context, magnificationController, detectControlGestures, triggerable, displayId);
        }
    }
}
