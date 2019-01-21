package android.os;

import android.os.IBinder.DeathRecipient;
import android.util.Log;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.WeakHashMap;

public abstract class TokenWatcher {
    private volatile boolean mAcquired = false;
    private Handler mHandler;
    private int mNotificationQueue = -1;
    private Runnable mNotificationTask = new Runnable() {
        public void run() {
            int value;
            synchronized (TokenWatcher.this.mTokens) {
                value = TokenWatcher.this.mNotificationQueue;
                TokenWatcher.this.mNotificationQueue = -1;
            }
            if (value == 1) {
                TokenWatcher.this.acquired();
            } else if (value == 0) {
                TokenWatcher.this.released();
            }
        }
    };
    private String mTag;
    private WeakHashMap<IBinder, Death> mTokens = new WeakHashMap();

    private class Death implements DeathRecipient {
        String tag;
        IBinder token;

        Death(IBinder token, String tag) {
            this.token = token;
            this.tag = tag;
        }

        public void binderDied() {
            TokenWatcher.this.cleanup(this.token, false);
        }

        protected void finalize() throws Throwable {
            try {
                if (this.token != null) {
                    String access$200 = TokenWatcher.this.mTag;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("cleaning up leaked reference: ");
                    stringBuilder.append(this.tag);
                    Log.w(access$200, stringBuilder.toString());
                    TokenWatcher.this.release(this.token);
                }
                super.finalize();
            } catch (Throwable th) {
                super.finalize();
            }
        }
    }

    public abstract void acquired();

    public abstract void released();

    public TokenWatcher(Handler h, String tag) {
        this.mHandler = h;
        this.mTag = tag != null ? tag : "TokenWatcher";
    }

    /* JADX WARNING: Missing block: B:18:0x0058, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void acquire(IBinder token, String tag) {
        synchronized (this.mTokens) {
            if (this.mTokens.containsKey(token)) {
                return;
            }
            int oldSize = this.mTokens.size();
            Death d = new Death(token, tag);
            try {
                token.linkToDeath(d, 0);
                this.mTokens.put(token, d);
                String str = this.mTag;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("put token = ");
                stringBuilder.append(token.toString());
                stringBuilder.append(",mTokens.size() = ");
                stringBuilder.append(this.mTokens.size());
                Log.d(str, stringBuilder.toString());
                if (oldSize == 0 && !this.mAcquired) {
                    sendNotificationLocked(true);
                    this.mAcquired = true;
                }
            } catch (RemoteException e) {
            }
        }
    }

    public void cleanup(IBinder token, boolean unlink) {
        synchronized (this.mTokens) {
            Death d = (Death) this.mTokens.remove(token);
            String str = this.mTag;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("remove token = ");
            stringBuilder.append(token.toString());
            stringBuilder.append(",mTokens.size() = ");
            stringBuilder.append(this.mTokens.size());
            Log.d(str, stringBuilder.toString());
            if (unlink && d != null) {
                d.token.unlinkToDeath(d, 0);
                d.token = null;
            }
            if (this.mTokens.size() == 0 && this.mAcquired) {
                sendNotificationLocked(false);
                this.mAcquired = false;
            }
        }
    }

    public void release(IBinder token) {
        cleanup(token, true);
    }

    public boolean isAcquired() {
        boolean z;
        synchronized (this.mTokens) {
            z = this.mAcquired;
        }
        return z;
    }

    public void dump() {
        Iterator it = dumpInternal().iterator();
        while (it.hasNext()) {
            Log.i(this.mTag, (String) it.next());
        }
    }

    public void dump(PrintWriter pw) {
        Iterator it = dumpInternal().iterator();
        while (it.hasNext()) {
            pw.println((String) it.next());
        }
    }

    private ArrayList<String> dumpInternal() {
        ArrayList<String> a = new ArrayList();
        synchronized (this.mTokens) {
            Set<IBinder> keys = this.mTokens.keySet();
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Token count: ");
            stringBuilder.append(this.mTokens.size());
            a.add(stringBuilder.toString());
            int i = 0;
            for (IBinder b : keys) {
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("[");
                stringBuilder2.append(i);
                stringBuilder2.append("] ");
                stringBuilder2.append(((Death) this.mTokens.get(b)).tag);
                stringBuilder2.append(" - ");
                stringBuilder2.append(b);
                a.add(stringBuilder2.toString());
                i++;
            }
        }
        return a;
    }

    private void sendNotificationLocked(boolean on) {
        boolean value = on;
        if (this.mNotificationQueue == -1) {
            this.mNotificationQueue = value;
            this.mHandler.post(this.mNotificationTask);
        } else if (this.mNotificationQueue != value) {
            this.mNotificationQueue = -1;
            this.mHandler.removeCallbacks(this.mNotificationTask);
        }
    }
}
