package org.simalliance.openmobileapi;

import android.os.RemoteException;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import org.simalliance.openmobileapi.service.ISmartcardServiceReader;
import org.simalliance.openmobileapi.service.ISmartcardServiceSession;
import org.simalliance.openmobileapi.service.SmartcardError;

public class Reader {
    private static final String TAG = "Reader";
    private final ArrayList<EventCallBack> mEventCallBackList = new ArrayList();
    private final Object mLock = new Object();
    private final String mName;
    private ISmartcardServiceReader mReader;
    private final SEService mService;

    public static class Event {
        private int mEventType;
        private Reader mReader;

        public Event(Reader reader, int type) {
            this.mReader = reader;
            this.mEventType = type;
        }

        public Reader getReader() {
            return this.mReader;
        }

        public int getEventType() {
            return this.mEventType;
        }
    }

    public interface EventCallBack {
        void notify(Event event);
    }

    Reader(SEService service, String name) {
        this.mName = name;
        this.mService = service;
        this.mReader = null;
    }

    public String getName() {
        return this.mName;
    }

    public Session openSession() throws IOException {
        Log.d(TAG, "Reader to openSession");
        if (this.mService == null || !this.mService.isConnected()) {
            throw new IllegalStateException("service is not connected");
        }
        Session session;
        if (this.mReader == null) {
            try {
                this.mReader = this.mService.getReader(this.mName);
            } catch (Exception e) {
                throw new IOException("service reader cannot be accessed.");
            }
        }
        synchronized (this.mLock) {
            SmartcardError error = new SmartcardError();
            ISmartcardServiceSession iSmartcardServiceSession = null;
            try {
                if (this.mReader != null) {
                    if (this.mReader.isSecureElementPresent(error)) {
                        iSmartcardServiceSession = this.mReader.openSession(error);
                    } else {
                        throw new IOException("Secure Element is not presented.");
                    }
                }
                SEService.checkForException(error);
                if (iSmartcardServiceSession == null) {
                    throw new IOException("service session is null.");
                }
                session = new Session(this.mService, iSmartcardServiceSession, this);
            } catch (RemoteException e2) {
                throw new IOException(e2.getMessage());
            }
        }
        return session;
    }

    public boolean isSecureElementPresent() {
        Log.d(TAG, "Reader to isSecureElementPresent");
        if (this.mService == null || !this.mService.isConnected()) {
            throw new IllegalStateException("service is not connected");
        }
        if (this.mReader == null) {
            try {
                this.mReader = this.mService.getReader(this.mName);
            } catch (Exception e) {
                throw new IllegalStateException("service reader cannot be accessed. " + e.getLocalizedMessage());
            }
        }
        SmartcardError error = new SmartcardError();
        boolean flag = false;
        try {
            if (this.mReader != null) {
                flag = this.mReader.isSecureElementPresent(error);
            }
            SEService.checkForException(error);
            return flag;
        } catch (RemoteException e2) {
            throw new IllegalStateException(e2.getMessage());
        }
    }

    public SEService getSEService() {
        Log.d(TAG, "Reader to getSEService");
        return this.mService;
    }

    public void closeSessions() {
        Log.d(TAG, "Reader to closeSessions");
        if (this.mService == null || !this.mService.isConnected()) {
            throw new IllegalStateException("service is not connected");
        } else if (this.mReader != null) {
            synchronized (this.mLock) {
                SmartcardError error = new SmartcardError();
                try {
                    this.mReader.closeSessions(error);
                    SEService.checkForException(error);
                } catch (RemoteException e) {
                    throw new IllegalStateException(e.getMessage());
                }
            }
        }
    }

    public void registerReaderEventCallback(EventCallBack callBack) {
        synchronized (this.mEventCallBackList) {
            if (callBack == null) {
                Log.v(TAG, "The callback is null");
            } else if (this.mEventCallBackList.contains(callBack)) {
                Log.v(TAG, "The callback has been already registered (" + callBack + ")");
            } else {
                this.mEventCallBackList.add(callBack);
            }
        }
    }

    public boolean unregisterReaderEventCallback(EventCallBack callBack) {
        synchronized (this.mEventCallBackList) {
            if (callBack == null) {
                Log.v(TAG, "The callback is null");
                return false;
            }
            boolean remove = this.mEventCallBackList.remove(callBack);
            return remove;
        }
    }

    void notifyEvent(final int eventType) {
        Log.v(TAG, "notify eventType=" + eventType + " name=" + this.mName + " this=" + this);
        synchronized (this.mEventCallBackList) {
            for (final EventCallBack c : this.mEventCallBackList) {
                int type = eventType;
                EventCallBack callback = c;
                new Thread(new Runnable() {
                    public void run() {
                        c.notify(new Event(Reader.this, eventType));
                    }
                }).start();
            }
        }
    }
}
