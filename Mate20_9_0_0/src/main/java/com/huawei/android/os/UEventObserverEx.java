package com.huawei.android.os;

import android.os.UEventObserver;

public abstract class UEventObserverEx {
    private ObserverEx mObserver = new ObserverEx();

    private class ObserverEx extends UEventObserver {
        private ObserverEx() {
        }

        public void onUEvent(android.os.UEventObserver.UEvent event) {
            UEventObserverEx.this.onUEvent(new UEvent(event));
        }
    }

    public static final class UEvent {
        private android.os.UEventObserver.UEvent mEvent;

        public UEvent(android.os.UEventObserver.UEvent event) {
            this.mEvent = event;
        }

        public String get(String key) {
            return this.mEvent.get(key);
        }
    }

    public abstract void onUEvent(UEvent uEvent);

    public final void startObserving(String match) {
        this.mObserver.startObserving(match);
    }

    public final void stopObserving() {
        this.mObserver.stopObserving();
    }
}
