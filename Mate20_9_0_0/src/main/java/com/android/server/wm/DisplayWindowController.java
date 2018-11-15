package com.android.server.wm;

import android.content.res.Configuration;
import android.os.Binder;
import android.util.Slog;
import android.view.Display;

public class DisplayWindowController extends WindowContainerController<DisplayContent, WindowContainerListener> {
    private final int mDisplayId;

    public DisplayWindowController(Display display, WindowContainerListener listener) {
        super(listener, WindowManagerService.getInstance());
        this.mDisplayId = display.getDisplayId();
        synchronized (this.mWindowMap) {
            long callingIdentity;
            try {
                WindowManagerService.boostPriorityForLockedSection();
                callingIdentity = Binder.clearCallingIdentity();
                this.mRoot.createDisplayContent(display, this);
                Binder.restoreCallingIdentity(callingIdentity);
                if (this.mContainer != null) {
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Trying to add display=");
                    stringBuilder.append(display);
                    stringBuilder.append(" dc=");
                    stringBuilder.append(this.mRoot.getDisplayContent(this.mDisplayId));
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } catch (Throwable th) {
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
        WindowManagerService.resetPriorityAfterLockedSection();
    }

    public void removeContainer() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer != null) {
                    ((DisplayContent) this.mContainer).removeIfPossible();
                    super.removeContainer();
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else if (WindowManagerDebugConfig.DEBUG_DISPLAY) {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("removeDisplay: could not find displayId=");
                    stringBuilder.append(this.mDisplayId);
                    Slog.i("WindowManager", stringBuilder.toString());
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void onOverrideConfigurationChanged(Configuration overrideConfiguration) {
    }

    public void positionChildAt(StackWindowController child, int position) {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                if (this.mContainer == null) {
                } else if (child == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else if (child.mContainer == null) {
                    WindowManagerService.resetPriorityAfterLockedSection();
                } else {
                    ((DisplayContent) this.mContainer).positionStackAt(position, (TaskStack) child.mContainer);
                    WindowManagerService.resetPriorityAfterLockedSection();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void deferUpdateImeTarget() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(this.mDisplayId);
                if (dc != null) {
                    dc.deferUpdateImeTarget();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public void continueUpdateImeTarget() {
        synchronized (this.mWindowMap) {
            try {
                WindowManagerService.boostPriorityForLockedSection();
                DisplayContent dc = this.mRoot.getDisplayContent(this.mDisplayId);
                if (dc != null) {
                    dc.continueUpdateImeTarget();
                }
            } finally {
                while (true) {
                }
                WindowManagerService.resetPriorityAfterLockedSection();
            }
        }
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{DisplayWindowController displayId=");
        stringBuilder.append(this.mDisplayId);
        stringBuilder.append("}");
        return stringBuilder.toString();
    }
}
