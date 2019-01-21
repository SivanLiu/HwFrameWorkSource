package android.support.v4.content;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public final class LocalBroadcastManager {
    private static final boolean DEBUG = false;
    static final int MSG_EXEC_PENDING_BROADCASTS = 1;
    private static final String TAG = "LocalBroadcastManager";
    private static LocalBroadcastManager mInstance;
    private static final Object mLock = new Object();
    private final HashMap<String, ArrayList<ReceiverRecord>> mActions = new HashMap();
    private final Context mAppContext;
    private final Handler mHandler;
    private final ArrayList<BroadcastRecord> mPendingBroadcasts = new ArrayList();
    private final HashMap<BroadcastReceiver, ArrayList<ReceiverRecord>> mReceivers = new HashMap();

    private static final class BroadcastRecord {
        final Intent intent;
        final ArrayList<ReceiverRecord> receivers;

        BroadcastRecord(Intent _intent, ArrayList<ReceiverRecord> _receivers) {
            this.intent = _intent;
            this.receivers = _receivers;
        }
    }

    private static final class ReceiverRecord {
        boolean broadcasting;
        boolean dead;
        final IntentFilter filter;
        final BroadcastReceiver receiver;

        ReceiverRecord(IntentFilter _filter, BroadcastReceiver _receiver) {
            this.filter = _filter;
            this.receiver = _receiver;
        }

        public String toString() {
            StringBuilder builder = new StringBuilder(128);
            builder.append("Receiver{");
            builder.append(this.receiver);
            builder.append(" filter=");
            builder.append(this.filter);
            if (this.dead) {
                builder.append(" DEAD");
            }
            builder.append("}");
            return builder.toString();
        }
    }

    @NonNull
    public static LocalBroadcastManager getInstance(@NonNull Context context) {
        LocalBroadcastManager localBroadcastManager;
        synchronized (mLock) {
            if (mInstance == null) {
                mInstance = new LocalBroadcastManager(context.getApplicationContext());
            }
            localBroadcastManager = mInstance;
        }
        return localBroadcastManager;
    }

    private LocalBroadcastManager(Context context) {
        this.mAppContext = context;
        this.mHandler = new Handler(context.getMainLooper()) {
            public void handleMessage(Message msg) {
                if (msg.what != 1) {
                    super.handleMessage(msg);
                } else {
                    LocalBroadcastManager.this.executePendingBroadcasts();
                }
            }
        };
    }

    public void registerReceiver(@NonNull BroadcastReceiver receiver, @NonNull IntentFilter filter) {
        synchronized (this.mReceivers) {
            ReceiverRecord entry = new ReceiverRecord(filter, receiver);
            ArrayList<ReceiverRecord> filters = (ArrayList) this.mReceivers.get(receiver);
            if (filters == null) {
                filters = new ArrayList(1);
                this.mReceivers.put(receiver, filters);
            }
            filters.add(entry);
            for (int i = 0; i < filter.countActions(); i++) {
                String action = filter.getAction(i);
                ArrayList<ReceiverRecord> entries = (ArrayList) this.mActions.get(action);
                if (entries == null) {
                    entries = new ArrayList(1);
                    this.mActions.put(action, entries);
                }
                entries.add(entry);
            }
        }
    }

    public void unregisterReceiver(@NonNull BroadcastReceiver receiver) {
        synchronized (this.mReceivers) {
            ArrayList<ReceiverRecord> filters = (ArrayList) this.mReceivers.remove(receiver);
            if (filters == null) {
                return;
            }
            for (int i = filters.size() - 1; i >= 0; i--) {
                ReceiverRecord filter = (ReceiverRecord) filters.get(i);
                filter.dead = true;
                for (int j = 0; j < filter.filter.countActions(); j++) {
                    String action = filter.filter.getAction(j);
                    ArrayList<ReceiverRecord> receivers = (ArrayList) this.mActions.get(action);
                    if (receivers != null) {
                        for (int k = receivers.size() - 1; k >= 0; k--) {
                            ReceiverRecord rec = (ReceiverRecord) receivers.get(k);
                            if (rec.receiver == receiver) {
                                rec.dead = true;
                                receivers.remove(k);
                            }
                        }
                        if (receivers.size() <= 0) {
                            this.mActions.remove(action);
                        }
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:53:0x0160, code skipped:
            return true;
     */
    /* JADX WARNING: Missing block: B:56:0x0163, code skipped:
            return DEBUG;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean sendBroadcast(@NonNull Intent intent) {
        Intent intent2 = intent;
        synchronized (this.mReceivers) {
            String str;
            StringBuilder stringBuilder;
            String action = intent.getAction();
            String type = intent2.resolveTypeIfNeeded(this.mAppContext.getContentResolver());
            Uri data = intent.getData();
            String scheme = intent.getScheme();
            Set<String> categories = intent.getCategories();
            boolean debug = (intent.getFlags() & 8) != 0 ? true : DEBUG;
            if (debug) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Resolving type ");
                stringBuilder.append(type);
                stringBuilder.append(" scheme ");
                stringBuilder.append(scheme);
                stringBuilder.append(" of intent ");
                stringBuilder.append(intent2);
                Log.v(str, stringBuilder.toString());
            }
            ArrayList<ReceiverRecord> entries = (ArrayList) this.mActions.get(intent.getAction());
            if (entries != null) {
                if (debug) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Action list: ");
                    stringBuilder.append(entries);
                    Log.v(str, stringBuilder.toString());
                }
                ArrayList<ReceiverRecord> receivers = null;
                int i = 0;
                while (true) {
                    int i2 = i;
                    ArrayList<ReceiverRecord> receivers2;
                    if (i2 < entries.size()) {
                        String type2;
                        int i3;
                        ReceiverRecord receiver = (ReceiverRecord) entries.get(i2);
                        if (debug) {
                            str = TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Matching against filter ");
                            stringBuilder2.append(receiver.filter);
                            Log.v(str, stringBuilder2.toString());
                        }
                        if (receiver.broadcasting) {
                            if (debug) {
                                Log.v(TAG, "  Filter's target already added");
                            }
                            type2 = type;
                            i3 = i2;
                            type = receivers;
                        } else {
                            ReceiverRecord receiver2 = receiver;
                            i3 = i2;
                            type2 = type;
                            type = receivers;
                            i = receiver.filter.match(action, type, scheme, data, categories, TAG);
                            String str2;
                            if (i >= 0) {
                                if (debug) {
                                    str2 = TAG;
                                    StringBuilder stringBuilder3 = new StringBuilder();
                                    stringBuilder3.append("  Filter matched!  match=0x");
                                    stringBuilder3.append(Integer.toHexString(i));
                                    Log.v(str2, stringBuilder3.toString());
                                }
                                if (type == null) {
                                    receivers2 = new ArrayList();
                                }
                                receivers2.add(receiver2);
                                receiver2.broadcasting = true;
                                receivers = receivers2;
                                i = i3 + 1;
                                type = type2;
                            } else if (debug) {
                                switch (i) {
                                    case -4:
                                        str2 = "category";
                                        break;
                                    case -3:
                                        str2 = "action";
                                        break;
                                    case -2:
                                        str2 = "data";
                                        break;
                                    case -1:
                                        str2 = "type";
                                        break;
                                    default:
                                        str2 = "unknown reason";
                                        break;
                                }
                                String str3 = TAG;
                                StringBuilder stringBuilder4 = new StringBuilder();
                                stringBuilder4.append("  Filter did not match: ");
                                stringBuilder4.append(str2);
                                Log.v(str3, stringBuilder4.toString());
                            }
                        }
                        receivers = type;
                        i = i3 + 1;
                        type = type2;
                    } else {
                        receivers2 = receivers;
                        if (receivers2 != null) {
                            for (i = 0; i < receivers2.size(); i++) {
                                ((ReceiverRecord) receivers2.get(i)).broadcasting = DEBUG;
                            }
                            this.mPendingBroadcasts.add(new BroadcastRecord(intent2, receivers2));
                            if (!this.mHandler.hasMessages(1)) {
                                this.mHandler.sendEmptyMessage(1);
                            }
                        }
                    }
                }
            }
        }
    }

    public void sendBroadcastSync(@NonNull Intent intent) {
        if (sendBroadcast(intent)) {
            executePendingBroadcasts();
        }
    }

    /* JADX WARNING: Missing block: B:10:0x001c, code skipped:
            r2 = 0;
     */
    /* JADX WARNING: Missing block: B:12:0x001f, code skipped:
            if (r2 >= r0.length) goto L_0x0047;
     */
    /* JADX WARNING: Missing block: B:13:0x0021, code skipped:
            r3 = r0[r2];
            r4 = r3.receivers.size();
            r5 = 0;
     */
    /* JADX WARNING: Missing block: B:14:0x002a, code skipped:
            if (r5 >= r4) goto L_0x0044;
     */
    /* JADX WARNING: Missing block: B:15:0x002c, code skipped:
            r6 = (android.support.v4.content.LocalBroadcastManager.ReceiverRecord) r3.receivers.get(r5);
     */
    /* JADX WARNING: Missing block: B:16:0x0036, code skipped:
            if (r6.dead != false) goto L_0x0041;
     */
    /* JADX WARNING: Missing block: B:17:0x0038, code skipped:
            r6.receiver.onReceive(r10.mAppContext, r3.intent);
     */
    /* JADX WARNING: Missing block: B:18:0x0041, code skipped:
            r5 = r5 + 1;
     */
    /* JADX WARNING: Missing block: B:19:0x0044, code skipped:
            r2 = r2 + 1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void executePendingBroadcasts() {
        while (true) {
            synchronized (this.mReceivers) {
                int N = this.mPendingBroadcasts.size();
                if (N <= 0) {
                    return;
                }
                BroadcastRecord[] brs = new BroadcastRecord[N];
                this.mPendingBroadcasts.toArray(brs);
                this.mPendingBroadcasts.clear();
            }
        }
        while (true) {
        }
    }
}
