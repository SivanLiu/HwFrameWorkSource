package android.media.session;

import android.annotation.SystemApi;
import android.content.ComponentName;
import android.content.Context;
import android.media.IRemoteVolumeController;
import android.media.ISessionTokensListener.Stub;
import android.media.SessionToken2;
import android.media.session.MediaSession.CallbackStub;
import android.media.session.MediaSession.Token;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.util.ArrayMap;
import android.util.Log;
import android.view.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;

public final class MediaSessionManager {
    public static final int RESULT_MEDIA_KEY_HANDLED = 1;
    public static final int RESULT_MEDIA_KEY_NOT_HANDLED = 0;
    private static final String TAG = "SessionManager";
    private CallbackImpl mCallback;
    private Context mContext;
    private final ArrayMap<OnActiveSessionsChangedListener, SessionsChangedWrapper> mListeners = new ArrayMap();
    private final Object mLock = new Object();
    private OnMediaKeyListenerImpl mOnMediaKeyListener;
    private OnVolumeKeyLongPressListenerImpl mOnVolumeKeyLongPressListener;
    private final ISessionManager mService;
    private final ArrayMap<OnSessionTokensChangedListener, SessionTokensChangedWrapper> mSessionTokensListener = new ArrayMap();

    public static abstract class Callback {
        public abstract void onAddressedPlayerChanged(ComponentName componentName);

        public abstract void onAddressedPlayerChanged(Token token);

        public abstract void onMediaKeyEventDispatched(KeyEvent keyEvent, ComponentName componentName);

        public abstract void onMediaKeyEventDispatched(KeyEvent keyEvent, Token token);
    }

    public interface OnActiveSessionsChangedListener {
        void onActiveSessionsChanged(List<MediaController> list);
    }

    @SystemApi
    public interface OnMediaKeyListener {
        boolean onMediaKey(KeyEvent keyEvent);
    }

    public interface OnSessionTokensChangedListener {
        void onSessionTokensChanged(List<SessionToken2> list);
    }

    @SystemApi
    public interface OnVolumeKeyLongPressListener {
        void onVolumeKeyLongPress(KeyEvent keyEvent);
    }

    public static final class RemoteUserInfo {
        private final IBinder mCallerBinder;
        private final String mPackageName;
        private final int mPid;
        private final int mUid;

        public RemoteUserInfo(String packageName, int pid, int uid) {
            this(packageName, pid, uid, null);
        }

        public RemoteUserInfo(String packageName, int pid, int uid, IBinder callerBinder) {
            this.mPackageName = packageName;
            this.mPid = pid;
            this.mUid = uid;
            this.mCallerBinder = callerBinder;
        }

        public String getPackageName() {
            return this.mPackageName;
        }

        public int getPid() {
            return this.mPid;
        }

        public int getUid() {
            return this.mUid;
        }

        public boolean equals(Object obj) {
            boolean z = false;
            if (!(obj instanceof RemoteUserInfo)) {
                return false;
            }
            if (this == obj) {
                return true;
            }
            RemoteUserInfo otherUserInfo = (RemoteUserInfo) obj;
            if (!(this.mCallerBinder == null || otherUserInfo.mCallerBinder == null)) {
                z = this.mCallerBinder.equals(otherUserInfo.mCallerBinder);
            }
            return z;
        }

        public int hashCode() {
            return Objects.hash(new Object[]{this.mPackageName, Integer.valueOf(this.mPid), Integer.valueOf(this.mUid)});
        }
    }

    private static final class SessionTokensChangedWrapper {
        private Context mContext;
        private Executor mExecutor;
        private OnSessionTokensChangedListener mListener;
        private final Stub mStub = new Stub() {
            public void onSessionTokensChanged(List<Bundle> bundles) {
                Executor executor = SessionTokensChangedWrapper.this.mExecutor;
                if (executor != null) {
                    executor.execute(new -$$Lambda$MediaSessionManager$SessionTokensChangedWrapper$1$wkYv3P0_Sdm0wRGnCFHp-AGf3Dw(this, bundles));
                }
            }

            public static /* synthetic */ void lambda$onSessionTokensChanged$0(AnonymousClass1 anonymousClass1, List bundles) {
                Context context = SessionTokensChangedWrapper.this.mContext;
                OnSessionTokensChangedListener listener = SessionTokensChangedWrapper.this.mListener;
                if (context != null && listener != null) {
                    listener.onSessionTokensChanged(MediaSessionManager.toTokenList(bundles));
                }
            }
        };

        public SessionTokensChangedWrapper(Context context, Executor executor, OnSessionTokensChangedListener listener) {
            this.mContext = context;
            this.mExecutor = executor;
            this.mListener = listener;
        }

        private void release() {
            this.mListener = null;
            this.mContext = null;
            this.mExecutor = null;
        }
    }

    private static final class SessionsChangedWrapper {
        private Context mContext;
        private Handler mHandler;
        private OnActiveSessionsChangedListener mListener;
        private final IActiveSessionsListener.Stub mStub = new IActiveSessionsListener.Stub() {
            public void onActiveSessionsChanged(final List<Token> tokens) {
                Handler handler = SessionsChangedWrapper.this.mHandler;
                if (handler != null) {
                    handler.post(new Runnable() {
                        public void run() {
                            Context context = SessionsChangedWrapper.this.mContext;
                            if (context != null) {
                                ArrayList<MediaController> controllers = new ArrayList();
                                int size = tokens.size();
                                for (int i = 0; i < size; i++) {
                                    controllers.add(new MediaController(context, (Token) tokens.get(i)));
                                }
                                OnActiveSessionsChangedListener listener = SessionsChangedWrapper.this.mListener;
                                if (listener != null) {
                                    listener.onActiveSessionsChanged(controllers);
                                }
                            }
                        }
                    });
                }
            }
        };

        public SessionsChangedWrapper(Context context, OnActiveSessionsChangedListener listener, Handler handler) {
            this.mContext = context;
            this.mListener = listener;
            this.mHandler = handler;
        }

        private void release() {
            this.mListener = null;
            this.mContext = null;
            this.mHandler = null;
        }
    }

    private static final class CallbackImpl extends ICallback.Stub {
        private final Callback mCallback;
        private final Handler mHandler;

        public CallbackImpl(Callback callback, Handler handler) {
            this.mCallback = callback;
            this.mHandler = handler;
        }

        public void onMediaKeyEventDispatchedToMediaSession(final KeyEvent event, final Token sessionToken) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    CallbackImpl.this.mCallback.onMediaKeyEventDispatched(event, sessionToken);
                }
            });
        }

        public void onMediaKeyEventDispatchedToMediaButtonReceiver(final KeyEvent event, final ComponentName mediaButtonReceiver) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    CallbackImpl.this.mCallback.onMediaKeyEventDispatched(event, mediaButtonReceiver);
                }
            });
        }

        public void onAddressedPlayerChangedToMediaSession(final Token sessionToken) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    CallbackImpl.this.mCallback.onAddressedPlayerChanged(sessionToken);
                }
            });
        }

        public void onAddressedPlayerChangedToMediaButtonReceiver(final ComponentName mediaButtonReceiver) {
            this.mHandler.post(new Runnable() {
                public void run() {
                    CallbackImpl.this.mCallback.onAddressedPlayerChanged(mediaButtonReceiver);
                }
            });
        }
    }

    private static final class OnMediaKeyListenerImpl extends IOnMediaKeyListener.Stub {
        private Handler mHandler;
        private OnMediaKeyListener mListener;

        public OnMediaKeyListenerImpl(OnMediaKeyListener listener, Handler handler) {
            this.mListener = listener;
            this.mHandler = handler;
        }

        public void onMediaKey(final KeyEvent event, final ResultReceiver result) {
            if (this.mListener == null || this.mHandler == null) {
                Log.w(MediaSessionManager.TAG, "Failed to call media key listener. Either mListener or mHandler is null");
            } else {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        boolean handled = OnMediaKeyListenerImpl.this.mListener.onMediaKey(event);
                        String str = MediaSessionManager.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("The media key listener is returned ");
                        stringBuilder.append(handled);
                        Log.d(str, stringBuilder.toString());
                        if (result != null) {
                            result.send(handled, null);
                        }
                    }
                });
            }
        }
    }

    private static final class OnVolumeKeyLongPressListenerImpl extends IOnVolumeKeyLongPressListener.Stub {
        private Handler mHandler;
        private OnVolumeKeyLongPressListener mListener;

        public OnVolumeKeyLongPressListenerImpl(OnVolumeKeyLongPressListener listener, Handler handler) {
            this.mListener = listener;
            this.mHandler = handler;
        }

        public void onVolumeKeyLongPress(final KeyEvent event) {
            if (this.mListener == null || this.mHandler == null) {
                Log.w(MediaSessionManager.TAG, "Failed to call volume key long-press listener. Either mListener or mHandler is null");
            } else {
                this.mHandler.post(new Runnable() {
                    public void run() {
                        OnVolumeKeyLongPressListenerImpl.this.mListener.onVolumeKeyLongPress(event);
                    }
                });
            }
        }
    }

    public MediaSessionManager(Context context) {
        this.mContext = context;
        this.mService = ISessionManager.Stub.asInterface(ServiceManager.getService(Context.MEDIA_SESSION_SERVICE));
    }

    public ISession createSession(CallbackStub cbStub, String tag, int userId) throws RemoteException {
        return this.mService.createSession(this.mContext.getPackageName(), cbStub, tag, userId);
    }

    public List<MediaController> getActiveSessions(ComponentName notificationListener) {
        return getActiveSessionsForUser(notificationListener, UserHandle.myUserId());
    }

    public List<MediaController> getActiveSessionsForUser(ComponentName notificationListener, int userId) {
        ArrayList<MediaController> controllers = new ArrayList();
        try {
            List<IBinder> binders = this.mService.getSessions(notificationListener, userId);
            int size = binders.size();
            for (int i = 0; i < size; i++) {
                controllers.add(new MediaController(this.mContext, ISessionController.Stub.asInterface((IBinder) binders.get(i))));
            }
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to get active sessions: ", e);
        }
        return controllers;
    }

    public void addOnActiveSessionsChangedListener(OnActiveSessionsChangedListener sessionListener, ComponentName notificationListener) {
        addOnActiveSessionsChangedListener(sessionListener, notificationListener, null);
    }

    public void addOnActiveSessionsChangedListener(OnActiveSessionsChangedListener sessionListener, ComponentName notificationListener, Handler handler) {
        addOnActiveSessionsChangedListener(sessionListener, notificationListener, UserHandle.myUserId(), handler);
    }

    public void addOnActiveSessionsChangedListener(OnActiveSessionsChangedListener sessionListener, ComponentName notificationListener, int userId, Handler handler) {
        if (sessionListener != null) {
            Handler handler2;
            if (handler == null) {
                handler2 = new Handler();
            } else {
                handler2 = handler;
            }
            synchronized (this.mLock) {
                if (this.mListeners.get(sessionListener) != null) {
                    Log.w(TAG, "Attempted to add session listener twice, ignoring.");
                    return;
                }
                SessionsChangedWrapper wrapper = new SessionsChangedWrapper(this.mContext, sessionListener, handler2);
                try {
                    this.mService.addSessionsListener(wrapper.mStub, notificationListener, userId);
                    this.mListeners.put(sessionListener, wrapper);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error in addOnActiveSessionsChangedListener.", e);
                }
            }
        } else {
            throw new IllegalArgumentException("listener may not be null");
        }
    }

    public void removeOnActiveSessionsChangedListener(OnActiveSessionsChangedListener listener) {
        if (listener != null) {
            synchronized (this.mLock) {
                SessionsChangedWrapper wrapper = (SessionsChangedWrapper) this.mListeners.remove(listener);
                if (wrapper != null) {
                    try {
                        this.mService.removeSessionsListener(wrapper.mStub);
                        wrapper.release();
                    } catch (RemoteException e) {
                        try {
                            Log.e(TAG, "Error in removeOnActiveSessionsChangedListener.", e);
                        } finally {
                            wrapper.release();
                        }
                    }
                }
            }
            return;
        }
        throw new IllegalArgumentException("listener may not be null");
    }

    public void setRemoteVolumeController(IRemoteVolumeController rvc) {
        try {
            this.mService.setRemoteVolumeController(rvc);
        } catch (RemoteException e) {
            Log.e(TAG, "Error in setRemoteVolumeController.", e);
        }
    }

    public void dispatchMediaKeyEvent(KeyEvent keyEvent) {
        dispatchMediaKeyEvent(keyEvent, false);
    }

    public void dispatchMediaKeyEvent(KeyEvent keyEvent, boolean needWakeLock) {
        dispatchMediaKeyEventInternal(false, keyEvent, needWakeLock);
    }

    public void dispatchMediaKeyEventAsSystemService(KeyEvent keyEvent) {
        dispatchMediaKeyEventInternal(true, keyEvent, false);
    }

    private void dispatchMediaKeyEventInternal(boolean asSystemService, KeyEvent keyEvent, boolean needWakeLock) {
        try {
            this.mService.dispatchMediaKeyEvent(this.mContext.getPackageName(), asSystemService, keyEvent, needWakeLock);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send key event.", e);
        }
    }

    public void dispatchVolumeKeyEvent(KeyEvent keyEvent, int stream, boolean musicOnly) {
        dispatchVolumeKeyEventInternal(false, keyEvent, stream, musicOnly);
    }

    public void dispatchVolumeKeyEventAsSystemService(KeyEvent keyEvent, int streamType) {
        dispatchVolumeKeyEventInternal(true, keyEvent, streamType, false);
    }

    private void dispatchVolumeKeyEventInternal(boolean asSystemService, KeyEvent keyEvent, int stream, boolean musicOnly) {
        try {
            this.mService.dispatchVolumeKeyEvent(this.mContext.getPackageName(), asSystemService, keyEvent, stream, musicOnly);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send volume key event.", e);
        }
    }

    public void dispatchAdjustVolume(int suggestedStream, int direction, int flags) {
        try {
            this.mService.dispatchAdjustVolume(this.mContext.getPackageName(), suggestedStream, direction, flags);
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to send adjust volume.", e);
        }
    }

    public boolean isTrustedForMediaControl(RemoteUserInfo userInfo) {
        if (userInfo == null) {
            throw new IllegalArgumentException("userInfo may not be null");
        } else if (userInfo.getPackageName() == null) {
            return false;
        } else {
            try {
                return this.mService.isTrusted(userInfo.getPackageName(), userInfo.getPid(), userInfo.getUid());
            } catch (RemoteException e) {
                Log.wtf(TAG, "Cannot communicate with the service.", e);
                return false;
            }
        }
    }

    public boolean createSession2(SessionToken2 token) {
        if (token == null) {
            return false;
        }
        try {
            return this.mService.createSession2(token.toBundle());
        } catch (RemoteException e) {
            Log.wtf(TAG, "Cannot communicate with the service.", e);
            return false;
        }
    }

    public void destroySession2(SessionToken2 token) {
        if (token != null) {
            try {
                this.mService.destroySession2(token.toBundle());
            } catch (RemoteException e) {
                Log.wtf(TAG, "Cannot communicate with the service.", e);
            }
        }
    }

    public List<SessionToken2> getActiveSessionTokens() {
        try {
            return toTokenList(this.mService.getSessionTokens(true, false, this.mContext.getPackageName()));
        } catch (RemoteException e) {
            Log.wtf(TAG, "Cannot communicate with the service.", e);
            return Collections.emptyList();
        }
    }

    public List<SessionToken2> getSessionServiceTokens() {
        try {
            return toTokenList(this.mService.getSessionTokens(false, true, this.mContext.getPackageName()));
        } catch (RemoteException e) {
            Log.wtf(TAG, "Cannot communicate with the service.", e);
            return Collections.emptyList();
        }
    }

    public List<SessionToken2> getAllSessionTokens() {
        try {
            return toTokenList(this.mService.getSessionTokens(false, false, this.mContext.getPackageName()));
        } catch (RemoteException e) {
            Log.wtf(TAG, "Cannot communicate with the service.", e);
            return Collections.emptyList();
        }
    }

    public void addOnSessionTokensChangedListener(Executor executor, OnSessionTokensChangedListener listener) {
        addOnSessionTokensChangedListener(UserHandle.myUserId(), executor, listener);
    }

    public void addOnSessionTokensChangedListener(int userId, Executor executor, OnSessionTokensChangedListener listener) {
        if (executor == null) {
            throw new IllegalArgumentException("executor may not be null");
        } else if (listener != null) {
            synchronized (this.mLock) {
                if (this.mSessionTokensListener.get(listener) != null) {
                    Log.w(TAG, "Attempted to add session listener twice, ignoring.");
                    return;
                }
                SessionTokensChangedWrapper wrapper = new SessionTokensChangedWrapper(this.mContext, executor, listener);
                try {
                    this.mService.addSessionTokensListener(wrapper.mStub, userId, this.mContext.getPackageName());
                    this.mSessionTokensListener.put(listener, wrapper);
                } catch (RemoteException e) {
                    Log.e(TAG, "Error in addSessionTokensListener.", e);
                }
            }
        } else {
            throw new IllegalArgumentException("listener may not be null");
        }
    }

    public void removeOnSessionTokensChangedListener(OnSessionTokensChangedListener listener) {
        if (listener != null) {
            synchronized (this.mLock) {
                SessionTokensChangedWrapper wrapper = (SessionTokensChangedWrapper) this.mSessionTokensListener.remove(listener);
                if (wrapper != null) {
                    try {
                        this.mService.removeSessionTokensListener(wrapper.mStub, this.mContext.getPackageName());
                        wrapper.release();
                    } catch (RemoteException e) {
                        try {
                            Log.e(TAG, "Error in removeSessionTokensListener.", e);
                        } finally {
                            wrapper.release();
                        }
                    }
                }
            }
            return;
        }
        throw new IllegalArgumentException("listener may not be null");
    }

    private static List<SessionToken2> toTokenList(List<Bundle> bundles) {
        List<SessionToken2> tokens = new ArrayList();
        if (bundles != null) {
            for (int i = 0; i < bundles.size(); i++) {
                SessionToken2 token = SessionToken2.fromBundle((Bundle) bundles.get(i));
                if (token != null) {
                    tokens.add(token);
                }
            }
        }
        return tokens;
    }

    public boolean isGlobalPriorityActive() {
        try {
            return this.mService.isGlobalPriorityActive();
        } catch (RemoteException e) {
            Log.e(TAG, "Failed to check if the global priority is active.", e);
            return false;
        }
    }

    @SystemApi
    public void setOnVolumeKeyLongPressListener(OnVolumeKeyLongPressListener listener, Handler handler) {
        synchronized (this.mLock) {
            if (listener == null) {
                try {
                    this.mOnVolumeKeyLongPressListener = null;
                    this.mService.setOnVolumeKeyLongPressListener(null);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to set volume key long press listener", e);
                } catch (Throwable th) {
                }
            } else {
                if (handler == null) {
                    handler = new Handler();
                }
                this.mOnVolumeKeyLongPressListener = new OnVolumeKeyLongPressListenerImpl(listener, handler);
                this.mService.setOnVolumeKeyLongPressListener(this.mOnVolumeKeyLongPressListener);
            }
        }
    }

    @SystemApi
    public void setOnMediaKeyListener(OnMediaKeyListener listener, Handler handler) {
        synchronized (this.mLock) {
            if (listener == null) {
                try {
                    this.mOnMediaKeyListener = null;
                    this.mService.setOnMediaKeyListener(null);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to set media key listener", e);
                } catch (Throwable th) {
                }
            } else {
                if (handler == null) {
                    handler = new Handler();
                }
                this.mOnMediaKeyListener = new OnMediaKeyListenerImpl(listener, handler);
                this.mService.setOnMediaKeyListener(this.mOnMediaKeyListener);
            }
        }
    }

    public void setCallback(Callback callback, Handler handler) {
        synchronized (this.mLock) {
            if (callback == null) {
                try {
                    this.mCallback = null;
                    this.mService.setCallback(null);
                } catch (RemoteException e) {
                    Log.e(TAG, "Failed to set media key callback", e);
                } catch (Throwable th) {
                }
            } else {
                if (handler == null) {
                    handler = new Handler();
                }
                this.mCallback = new CallbackImpl(callback, handler);
                this.mService.setCallback(this.mCallback);
            }
        }
    }
}
