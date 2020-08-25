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
    /* access modifiers changed from: private */
    public final Set<String> mAllProtectAppSet = new ArraySet();
    /* access modifiers changed from: private */
    public final Set<String> mAllUnProtectAppSet = new ArraySet();
    /* access modifiers changed from: private */
    public Context mContext = null;
    /* access modifiers changed from: private */
    public int mCurUserId = 0;
    private final HSMHandler mHandler = new HSMHandler(BackgroundThread.get().getLooper());
    private final ContentObserver mProtectAppChangeObserver = new ContentObserver(null) {
        /* class com.android.server.rms.algorithm.AwareHSMListHandler.AnonymousClass1 */

        public void onChange(boolean selfChange) {
            AwareHSMListHandler.this.updateProtectAppSet(AwareHSMListHandler.UPDATE_APP_DELAYTIME);
        }
    };
    /* access modifiers changed from: private */
    public final Set<String> mProtectAppSet = new ArraySet();
    /* access modifiers changed from: private */
    public final Set<String> mUnProtectAppSet = new ArraySet();

    public AwareHSMListHandler(Context context) {
        this.mContext = context;
    }

    /* access modifiers changed from: package-private */
    public void init() {
        startObserver();
        updateProtectAppSet(0);
    }

    /* access modifiers changed from: package-private */
    public void deinit() {
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

    private class HSMHandler extends Handler {
        public HSMHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                updateProtectInfoFromDB();
            }
        }

        /* JADX WARNING: Code restructure failed: missing block: B:46:0x0103, code lost:
            if (0 == 0) goto L_0x0106;
         */
        private void updateProtectInfoFromDB() {
            AwareLog.d(AwareHSMListHandler.TAG, "Get HSM list from DB.");
            if (AwareHSMListHandler.this.mContext != null) {
                Cursor cursor = null;
                ContentResolver resolver = AwareHSMListHandler.this.mContext.getContentResolver();
                if (resolver != null) {
                    ArraySet arraySet = new ArraySet();
                    ArraySet arraySet2 = new ArraySet();
                    synchronized (AwareHSMListHandler.this) {
                        AwareHSMListHandler.this.mProtectAppSet.clear();
                        AwareHSMListHandler.this.mUnProtectAppSet.clear();
                        AwareLog.d(AwareHSMListHandler.TAG, "Updating HSM data with userid " + AwareHSMListHandler.this.mCurUserId);
                        String[] projection = {"pkg_name", "is_checked", "userchanged"};
                        try {
                            cursor = resolver.query(Uri.parse(AwareHSMListHandler.SMCS_AUTHORITY_URI + AwareHSMListHandler.this.mCurUserId + "@" + AwareHSMListHandler.ST_PROTECTED_PKGS_TABLE), projection, null, null, null);
                            if (cursor == null) {
                                if (cursor != null) {
                                    cursor.close();
                                }
                                return;
                            }
                            while (cursor.moveToNext()) {
                                String pkgName = cursor.getString(0);
                                int isProtected = cursor.getInt(1);
                                int isSelectByUser = cursor.getInt(2);
                                if (pkgName != null) {
                                    if (1 == isProtected) {
                                        arraySet.add(pkgName);
                                    } else if (isProtected == 0) {
                                        arraySet2.add(pkgName);
                                    }
                                    if (1 == isSelectByUser) {
                                        if (1 == isProtected) {
                                            AwareHSMListHandler.this.mProtectAppSet.add(pkgName);
                                        } else if (isProtected == 0) {
                                            AwareHSMListHandler.this.mUnProtectAppSet.add(pkgName);
                                        }
                                    }
                                }
                            }
                            cursor.close();
                            AwareLog.d(AwareHSMListHandler.TAG, "get protect Set " + AwareHSMListHandler.this.mProtectAppSet + ", and unprotected Set " + AwareHSMListHandler.this.mUnProtectAppSet);
                            synchronized (AwareHSMListHandler.this.mAllProtectAppSet) {
                                AwareHSMListHandler.this.mAllProtectAppSet.clear();
                                AwareHSMListHandler.this.mAllProtectAppSet.addAll(arraySet);
                            }
                            synchronized (AwareHSMListHandler.this.mAllUnProtectAppSet) {
                                AwareHSMListHandler.this.mAllUnProtectAppSet.clear();
                                AwareHSMListHandler.this.mAllUnProtectAppSet.addAll(arraySet2);
                            }
                        } catch (IllegalArgumentException e) {
                            AwareLog.e(AwareHSMListHandler.TAG, "Exception when getProtectAppFromDB.");
                        } catch (IllegalStateException e2) {
                            AwareLog.e(AwareHSMListHandler.TAG, "IllegalStateException: load HSM protectlist failed!");
                            if (0 != 0) {
                            }
                        } catch (SQLiteException e3) {
                            AwareLog.e(AwareHSMListHandler.TAG, "Error: load HSM protectlist failed!");
                            if (0 != 0) {
                            }
                        } catch (Throwable th) {
                            if (0 != 0) {
                                cursor.close();
                            }
                            throw th;
                        }
                    }
                }
            }
        }
    }

    private void startObserver() {
        AwareLog.i(TAG, "UserHabit HSM db provider observer started.");
        Context context = this.mContext;
        if (context != null) {
            context.getContentResolver().registerContentObserver(Uri.parse("content://smcs/st_protected_pkgs_table"), true, this.mProtectAppChangeObserver, -1);
        }
    }

    private void stopObserver() {
        Context context = this.mContext;
        if (context != null) {
            context.getContentResolver().unregisterContentObserver(this.mProtectAppChangeObserver);
            AwareLog.i(TAG, "UserHabit HSM db provider observer stopped.");
        }
    }

    /* access modifiers changed from: package-private */
    public Set<String> getProtectSet() {
        ArraySet<String> result = new ArraySet<>();
        synchronized (this) {
            result.addAll(this.mProtectAppSet);
        }
        return result;
    }

    /* access modifiers changed from: package-private */
    public Set<String> getAllProtectSet() {
        ArraySet<String> result = new ArraySet<>();
        synchronized (this.mAllProtectAppSet) {
            result.addAll(this.mAllProtectAppSet);
        }
        return result;
    }

    /* access modifiers changed from: package-private */
    public Set<String> getAllUnProtectSet() {
        ArraySet<String> result = new ArraySet<>();
        synchronized (this.mAllUnProtectAppSet) {
            result.addAll(this.mAllUnProtectAppSet);
        }
        return result;
    }

    /* access modifiers changed from: package-private */
    public Set<String> getUnProtectSet() {
        ArraySet<String> result = new ArraySet<>();
        synchronized (this) {
            result.addAll(this.mUnProtectAppSet);
        }
        return result;
    }

    /* access modifiers changed from: private */
    public void updateProtectAppSet(int postTime) {
        if (this.mHandler.hasMessages(1)) {
            this.mHandler.removeMessages(1);
        }
        this.mHandler.sendEmptyMessageDelayed(1, (long) postTime);
    }
}
