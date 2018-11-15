package com.android.server.wm;

import android.content.res.Configuration;

class WindowContainerController<E extends WindowContainer, I extends WindowContainerListener> implements ConfigurationContainerListener {
    E mContainer;
    final I mListener;
    final RootWindowContainer mRoot;
    final WindowManagerService mService;
    final WindowHashMap mWindowMap;

    WindowContainerController(I listener, WindowManagerService service) {
        this.mListener = listener;
        this.mService = service;
        WindowHashMap windowHashMap = null;
        this.mRoot = this.mService != null ? this.mService.mRoot : null;
        if (this.mService != null) {
            windowHashMap = this.mService.mWindowMap;
        }
        this.mWindowMap = windowHashMap;
    }

    void setContainer(E container) {
        if (this.mContainer == null || container == null) {
            this.mContainer = container;
            if (this.mContainer != null && this.mListener != null) {
                this.mListener.registerConfigurationChangeListener(this);
                return;
            }
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can't set container=");
        stringBuilder.append(container);
        stringBuilder.append(" for controller=");
        stringBuilder.append(this);
        stringBuilder.append(" Already set to=");
        stringBuilder.append(this.mContainer);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    void removeContainer() {
        if (this.mContainer != null) {
            this.mContainer.setController(null);
            this.mContainer = null;
            if (this.mListener != null) {
                this.mListener.unregisterConfigurationChangeListener(this);
            }
        }
    }

    public void onOverrideConfigurationChanged(Configuration overrideConfiguration) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                } else {
                    this.mContainer.onOverrideConfigurationChanged(overrideConfiguration);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }
}
