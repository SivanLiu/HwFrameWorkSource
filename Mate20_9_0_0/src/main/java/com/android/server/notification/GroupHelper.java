package com.android.server.notification;

import android.service.notification.StatusBarNotification;
import android.util.Log;
import android.util.Slog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class GroupHelper {
    protected static final int AUTOGROUP_AT_COUNT = 2;
    protected static final String AUTOGROUP_KEY = "ranker_group";
    private static final boolean DEBUG = Log.isLoggable(TAG, 3);
    private static final int N_MUSIC = 2;
    private static final int N_NORMAL = 1;
    private static final String TAG = "GroupHelper";
    private final Callback mCallback;
    Map<Integer, Map<String, LinkedHashSet<String>>> mUngroupedNotifications = new HashMap();

    protected interface Callback {
        void addAutoGroup(String str);

        void addAutoGroupSummary(int i, String str, String str2);

        void removeAutoGroup(String str);

        void removeAutoGroupSummary(int i, String str);

        void removeAutoGroupSummary(int i, String str, int i2);
    }

    public GroupHelper(Callback callback) {
        this.mCallback = callback;
    }

    public void onNotificationPosted(StatusBarNotification sbn, boolean autogroupSummaryExists, int notifyType) {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("POSTED ");
            stringBuilder.append(sbn.getKey());
            Log.i(str, stringBuilder.toString());
        }
        try {
            List<String> notificationsToGroup = new ArrayList();
            if (sbn.getNotification().extras.containsKey("hw_btw") || !sbn.isAppGroup()) {
                synchronized (this.mUngroupedNotifications) {
                    Map<String, LinkedHashSet<String>> ungroupedNotificationsByUser = (Map) this.mUngroupedNotifications.get(Integer.valueOf(sbn.getUserId()));
                    if (ungroupedNotificationsByUser == null) {
                        ungroupedNotificationsByUser = new HashMap();
                    }
                    this.mUngroupedNotifications.put(Integer.valueOf(sbn.getUserId()), ungroupedNotificationsByUser);
                    String notifyKey = getUnGroupKey(sbn.getPackageName(), notifyType);
                    LinkedHashSet<String> notificationsForPackage = (LinkedHashSet) ungroupedNotificationsByUser.get(notifyKey);
                    if (notificationsForPackage == null) {
                        notificationsForPackage = new LinkedHashSet();
                    }
                    notificationsForPackage.add(sbn.getKey());
                    ungroupedNotificationsByUser.put(notifyKey, notificationsForPackage);
                    if (notificationsForPackage.size() >= 2 || autogroupSummaryExists) {
                        notificationsToGroup.addAll(notificationsForPackage);
                    }
                }
                if (notificationsToGroup.size() > 0) {
                    adjustAutogroupingSummary(sbn.getUserId(), sbn.getPackageName(), (String) notificationsToGroup.get(0), true, notifyType);
                    adjustNotificationBundling(notificationsToGroup, true);
                }
                return;
            }
            maybeUngroup(sbn, false, sbn.getUserId(), notifyType);
            String str2 = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("onNotificationPosted ");
            stringBuilder2.append(notifyType);
            Log.i(str2, stringBuilder2.toString());
        } catch (Exception e) {
            Slog.e(TAG, "Failure processing new notification", e);
        }
    }

    public void onNotificationRemoved(StatusBarNotification sbn) {
        onNotificationRemoved(sbn, 1);
    }

    public void onNotificationRemoved(StatusBarNotification sbn, int notifyType) {
        try {
            maybeUngroup(sbn, true, sbn.getUserId(), notifyType);
        } catch (Exception e) {
            Slog.e(TAG, "Error processing canceled notification", e);
        }
    }

    /* JADX WARNING: Missing block: B:29:0x007b, code skipped:
            if (r1 == false) goto L_0x0089;
     */
    /* JADX WARNING: Missing block: B:30:0x007d, code skipped:
            adjustAutogroupingSummary(r12, r10.getPackageName(), null, false, r13);
     */
    /* JADX WARNING: Missing block: B:32:0x008d, code skipped:
            if (r0.size() <= 0) goto L_0x0093;
     */
    /* JADX WARNING: Missing block: B:33:0x008f, code skipped:
            adjustNotificationBundling(r0, false);
     */
    /* JADX WARNING: Missing block: B:34:0x0093, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:39:0x0097, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void maybeUngroup(StatusBarNotification sbn, boolean notificationGone, int userId, int notifyType) {
        List<String> notificationsToUnAutogroup = new ArrayList();
        boolean removeSummary = false;
        synchronized (this.mUngroupedNotifications) {
            Map<String, LinkedHashSet<String>> ungroupedNotificationsByUser = (Map) this.mUngroupedNotifications.get(Integer.valueOf(sbn.getUserId()));
            if (ungroupedNotificationsByUser != null) {
                if (ungroupedNotificationsByUser.size() != 0) {
                    String notifyKey = getUnGroupKey(sbn.getPackageName(), notifyType);
                    LinkedHashSet<String> notificationsForPackage = (LinkedHashSet) ungroupedNotificationsByUser.get(notifyKey);
                    if (notificationsForPackage != null) {
                        if (notificationsForPackage.size() != 0) {
                            if (notificationsForPackage.remove(sbn.getKey()) && !notificationGone) {
                                notificationsToUnAutogroup.add(sbn.getKey());
                            }
                            if (notificationsForPackage.size() == 0) {
                                ungroupedNotificationsByUser.remove(notifyKey);
                                removeSummary = true;
                            }
                            if (notificationsForPackage.size() == 1) {
                                Iterator<String> linkedSet = notificationsForPackage.iterator();
                                while (linkedSet.hasNext()) {
                                    if (((String) linkedSet.next()).contains(AUTOGROUP_KEY)) {
                                        removeSummary = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void adjustAutogroupingSummary(int userId, String packageName, String triggeringKey, boolean summaryNeeded, int notifyType) {
        if (summaryNeeded) {
            this.mCallback.addAutoGroupSummary(userId, packageName, triggeringKey);
        } else {
            this.mCallback.removeAutoGroupSummary(userId, packageName, notifyType);
        }
    }

    private void adjustNotificationBundling(List<String> keys, boolean group) {
        for (String key : keys) {
            if (DEBUG) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Sending grouping adjustment for: ");
                stringBuilder.append(key);
                stringBuilder.append(" group? ");
                stringBuilder.append(group);
                Log.i(str, stringBuilder.toString());
            }
            if (group) {
                this.mCallback.addAutoGroup(key);
            } else {
                this.mCallback.removeAutoGroup(key);
            }
        }
    }

    public String getAutoGroupKey(int notificationType, int id) {
        StringBuilder stringBuilder;
        if (notificationType == 2) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(AUTOGROUP_KEY);
            stringBuilder.append(notificationType);
            stringBuilder.append(id);
            return stringBuilder.toString();
        }
        stringBuilder = new StringBuilder();
        stringBuilder.append(AUTOGROUP_KEY);
        stringBuilder.append(notificationType);
        return stringBuilder.toString();
    }

    public String getUnGroupKey(String pkgName, int notificationType) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pkgName);
        stringBuilder.append(notificationType);
        return stringBuilder.toString();
    }
}
