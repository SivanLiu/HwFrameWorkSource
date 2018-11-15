package com.android.server.rms.algorithm;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.rms.iaware.AwareLog;
import android.util.ArraySet;
import com.android.internal.os.BackgroundThread;
import java.util.Set;

public class AwareHSMListHandler {
    private static final int COL_PKGNAME = 0;
    private static final int COL_PROTECTED = 1;
    private static final int COL_USER_SELECTED = 2;
    private static final String SMCS_AUTHORITY_URI = "content://";
    private static final String ST_PROTECTED_PKGS_TABLE = "smcs/st_protected_pkgs_table";
    private static final String TAG = "AwareHSMListHandler";
    private static final int UPDATE_APP_DELAYTIME = 5000;
    private static final int UPDATE_PROTECT_INFO_FROM_DB = 1;
    private final Set<String> mAllProtectAppSet = new ArraySet();
    private final Set<String> mAllUnProtectAppSet = new ArraySet();
    private Context mContext = null;
    private int mCurUserId = 0;
    private final HSMHandler mHandler = new HSMHandler(BackgroundThread.get().getLooper());
    private final ContentObserver mProtectAppChangeObserver = new ContentObserver(null) {
        public void onChange(boolean selfChange) {
            AwareHSMListHandler.this.updateProtectAppSet(AwareHSMListHandler.UPDATE_APP_DELAYTIME);
        }
    };
    private final Set<String> mProtectAppSet = new ArraySet();
    private final Set<String> mUnProtectAppSet = new ArraySet();

    private class HSMHandler extends Handler {
        public HSMHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                updateProtectInfoFromDB();
            }
        }

        /* JADX WARNING: Missing block: B:19:0x009d, code:
            return;
     */
        /* JADX WARNING: Missing block: B:37:0x00dc, code:
            if (r0 != null) goto L_0x00de;
     */
        /* JADX WARNING: Missing block: B:39:?, code:
            r0.close();
     */
        /* JADX WARNING: Missing block: B:44:0x00ed, code:
            if (r0 == null) goto L_0x0106;
     */
        /* JADX WARNING: Missing block: B:47:0x00f8, code:
            if (r0 == null) goto L_0x0106;
     */
        /* JADX WARNING: Missing block: B:50:0x0103, code:
            if (r0 == null) goto L_0x0106;
     */
        /* JADX WARNING: Missing block: B:52:?, code:
            r2 = com.android.server.rms.algorithm.AwareHSMListHandler.TAG;
            r5 = new java.lang.StringBuilder();
            r5.append("get protect Set ");
            r5.append(com.android.server.rms.algorithm.AwareHSMListHandler.access$200(r11.this$0));
            r5.append(", and unprotected Set ");
            r5.append(com.android.server.rms.algorithm.AwareHSMListHandler.access$300(r11.this$0));
            android.rms.iaware.AwareLog.d(r2, r5.toString());
     */
        /* JADX WARNING: Missing block: B:54:0x0131, code:
            r2 = com.android.server.rms.algorithm.AwareHSMListHandler.access$500(r11.this$0);
     */
        /* JADX WARNING: Missing block: B:55:0x0137, code:
            monitor-enter(r2);
     */
        /* JADX WARNING: Missing block: B:57:?, code:
            com.android.server.rms.algorithm.AwareHSMListHandler.access$500(r11.this$0).clear();
            com.android.server.rms.algorithm.AwareHSMListHandler.access$500(r11.this$0).addAll(r8);
     */
        /* JADX WARNING: Missing block: B:58:0x014a, code:
            monitor-exit(r2);
     */
        /* JADX WARNING: Missing block: B:59:0x014b, code:
            r3 = com.android.server.rms.algorithm.AwareHSMListHandler.access$600(r11.this$0);
     */
        /* JADX WARNING: Missing block: B:60:0x0151, code:
            monitor-enter(r3);
     */
        /* JADX WARNING: Missing block: B:62:?, code:
            com.android.server.rms.algorithm.AwareHSMListHandler.access$600(r11.this$0).clear();
            com.android.server.rms.algorithm.AwareHSMListHandler.access$600(r11.this$0).addAll(r9);
     */
        /* JADX WARNING: Missing block: B:63:0x0164, code:
            monitor-exit(r3);
     */
        /* JADX WARNING: Missing block: B:64:0x0165, code:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void updateProtectInfoFromDB() {
            AwareLog.d(AwareHSMListHandler.TAG, "Get HSM list from DB.");
            if (AwareHSMListHandler.this.mContext != null) {
                Cursor cursor = null;
                ContentResolver resolver = AwareHSMListHandler.this.mContext.getContentResolver();
                if (resolver != null) {
                    ArraySet allProtectAppSet = new ArraySet();
                    ArraySet allUnProtectAppSet = new ArraySet();
                    synchronized (AwareHSMListHandler.this) {
                        AwareHSMListHandler.this.mProtectAppSet.clear();
                        AwareHSMListHandler.this.mUnProtectAppSet.clear();
                        String str = AwareHSMListHandler.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Updating HSM data with userid ");
                        stringBuilder.append(AwareHSMListHandler.this.mCurUserId);
                        AwareLog.d(str, stringBuilder.toString());
                        String[] projection = new String[]{"pkg_name", "is_checked", "userchanged"};
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append(AwareHSMListHandler.SMCS_AUTHORITY_URI);
                        stringBuilder2.append(AwareHSMListHandler.this.mCurUserId);
                        stringBuilder2.append("@");
                        stringBuilder2.append(AwareHSMListHandler.ST_PROTECTED_PKGS_TABLE);
                        try {
                            cursor = resolver.query(Uri.parse(stringBuilder2.toString()), projection, null, null, null);
                            if (cursor != null) {
                                while (cursor.moveToNext()) {
                                    str = cursor.getString(null);
                                    int isProtected = cursor.getInt(1);
                                    int isSelectByUser = cursor.getInt(2);
                                    if (str != null) {
                                        if (1 == isProtected) {
                                            allProtectAppSet.add(str);
                                        } else if (isProtected == 0) {
                                            allUnProtectAppSet.add(str);
                                        }
                                        if (1 == isSelectByUser) {
                                            if (1 == isProtected) {
                                                AwareHSMListHandler.this.mProtectAppSet.add(str);
                                            } else if (isProtected == 0) {
                                                AwareHSMListHandler.this.mUnProtectAppSet.add(str);
                                            }
                                        }
                                    }
                                }
                            } else if (cursor != null) {
                                cursor.close();
                            }
                        } catch (IllegalArgumentException e) {
                            AwareLog.e(AwareHSMListHandler.TAG, "Exception when getProtectAppFromDB.");
                        } catch (IllegalStateException e2) {
                            AwareLog.e(AwareHSMListHandler.TAG, "IllegalStateException: load HSM protectlist failed!");
                        } catch (SQLiteException e3) {
                            try {
                                AwareLog.e(AwareHSMListHandler.TAG, "Error: load HSM protectlist failed!");
                            } catch (Throwable th) {
                                if (cursor != null) {
                                    cursor.close();
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public AwareHSMListHandler(Context context) {
        this.mContext = context;
    }

    void init() {
        startObserver();
        updateProtectAppSet(0);
    }

    void deinit() {
        stopObserver();
        synchronized (this) {
            this.mProtectAppSet.clear();
            this.mUnProtectAppSet.clear();
        }
        synchronized (this.mAllProtectAppSet) {
            this.mAllProtectAppSet.clear();
        }
        synchronized (this.mAllUnProtectAppSet) {
            this.mAllUnProtectAppSet.clear();
        }
    }

    public void setUserId(int userId) {
        synchronized (this) {
            this.mCurUserId = userId;
        }
        updateProtectAppSet(UPDATE_APP_DELAYTIME);
    }

    private void startObserver() {
        AwareLog.i(TAG, "UserHabit HSM db provider observer started.");
        if (this.mContext != null) {
            this.mContext.getContentResolver().registerContentObserver(Uri.parse("content://smcs/st_protected_pkgs_table"), true, this.mProtectAppChangeObserver, -1);
        }
    }

    private void stopObserver() {
        if (this.mContext != null) {
            this.mContext.getContentResolver().unregisterContentObserver(this.mProtectAppChangeObserver);
            AwareLog.i(TAG, "UserHabit HSM db provider observer stopped.");
        }
    }

    Set<String> getProtectSet() {
        ArraySet<String> result = new ArraySet();
        synchronized (this) {
            result.addAll(this.mProtectAppSet);
        }
        return result;
    }

    Set<String> getAllProtectSet() {
        ArraySet<String> result = new ArraySet();
        synchronized (this.mAllProtectAppSet) {
            result.addAll(this.mAllProtectAppSet);
        }
        return result;
    }

    Set<String> getAllUnProtectSet() {
        ArraySet<String> result = new ArraySet();
        synchronized (this.mAllUnProtectAppSet) {
            result.addAll(this.mAllUnProtectAppSet);
        }
        return result;
    }

    Set<String> getUnProtectSet() {
        ArraySet<String> result = new ArraySet();
        synchronized (this) {
            result.addAll(this.mUnProtectAppSet);
        }
        return result;
    }

    private void updateProtectAppSet(int postTime) {
        if (this.mHandler.hasMessages(1)) {
            this.mHandler.removeMessages(1);
        }
        this.mHandler.sendEmptyMessageDelayed(1, (long) postTime);
    }
}
