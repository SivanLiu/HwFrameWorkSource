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

        /* JADX WARNING: Removed duplicated region for block: B:58:0x0138 A:{SYNTHETIC} */
        /* JADX WARNING: Removed duplicated region for block: B:63:0x0152 A:{SYNTHETIC} */
        /* JADX WARNING: Missing block: B:19:0x009d, code skipped:
            return;
     */
        /* JADX WARNING: Missing block: B:37:0x00dc, code skipped:
            if (r0 != null) goto L_0x00de;
     */
        /* JADX WARNING: Missing block: B:52:0x0103, code skipped:
            if (r0 == null) goto L_0x0106;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void updateProtectInfoFromDB() {
            StringBuilder stringBuilder;
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
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Updating HSM data with userid ");
                        stringBuilder2.append(AwareHSMListHandler.this.mCurUserId);
                        AwareLog.d(str, stringBuilder2.toString());
                        String[] projection = new String[]{"pkg_name", "is_checked", "userchanged"};
                        StringBuilder stringBuilder3 = new StringBuilder();
                        stringBuilder3.append(AwareHSMListHandler.SMCS_AUTHORITY_URI);
                        stringBuilder3.append(AwareHSMListHandler.this.mCurUserId);
                        stringBuilder3.append("@");
                        stringBuilder3.append(AwareHSMListHandler.ST_PROTECTED_PKGS_TABLE);
                        try {
                            cursor = resolver.query(Uri.parse(stringBuilder3.toString()), projection, null, null, null);
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
                            if (cursor != null) {
                                cursor.close();
                            }
                            str = AwareHSMListHandler.TAG;
                            stringBuilder = new StringBuilder();
                            stringBuilder.append("get protect Set ");
                            stringBuilder.append(AwareHSMListHandler.this.mProtectAppSet);
                            stringBuilder.append(", and unprotected Set ");
                            stringBuilder.append(AwareHSMListHandler.this.mUnProtectAppSet);
                            AwareLog.d(str, stringBuilder.toString());
                            synchronized (AwareHSMListHandler.this.mAllProtectAppSet) {
                                AwareHSMListHandler.this.mAllProtectAppSet.clear();
                                AwareHSMListHandler.this.mAllProtectAppSet.addAll(allProtectAppSet);
                            }
                            synchronized (AwareHSMListHandler.this.mAllUnProtectAppSet) {
                                AwareHSMListHandler.this.mAllUnProtectAppSet.clear();
                                AwareHSMListHandler.this.mAllUnProtectAppSet.addAll(allUnProtectAppSet);
                            }
                        } catch (SQLiteException e3) {
                            try {
                                AwareLog.e(AwareHSMListHandler.TAG, "Error: load HSM protectlist failed!");
                                if (cursor != null) {
                                    cursor.close();
                                }
                                str = AwareHSMListHandler.TAG;
                                stringBuilder = new StringBuilder();
                                stringBuilder.append("get protect Set ");
                                stringBuilder.append(AwareHSMListHandler.this.mProtectAppSet);
                                stringBuilder.append(", and unprotected Set ");
                                stringBuilder.append(AwareHSMListHandler.this.mUnProtectAppSet);
                                AwareLog.d(str, stringBuilder.toString());
                                synchronized (AwareHSMListHandler.this.mAllProtectAppSet) {
                                }
                                synchronized (AwareHSMListHandler.this.mAllUnProtectAppSet) {
                                }
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
