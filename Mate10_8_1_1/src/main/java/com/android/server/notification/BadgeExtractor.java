package com.android.server.notification;

import android.content.Context;

public class BadgeExtractor implements NotificationSignalExtractor {
    private static final boolean DBG = false;
    private static final String TAG = "BadgeExtractor";
    private RankingConfig mConfig;

    public void initialize(Context ctx, NotificationUsageStats usageStats) {
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public RankingReconsideration process(NotificationRecord record) {
        if (record == null || record.getNotification() == null || this.mConfig == null) {
            return null;
        }
        boolean userWantsBadges = this.mConfig.badgingEnabled(record.sbn.getUser());
        boolean appCanShowBadge = this.mConfig.canShowBadge(record.sbn.getPackageName(), record.sbn.getUid());
        if (!userWantsBadges || (appCanShowBadge ^ 1) != 0) {
            record.setShowBadge(false);
        } else if (record.getChannel() != null) {
            if (!record.getChannel().canShowBadge()) {
                appCanShowBadge = false;
            }
            record.setShowBadge(appCanShowBadge);
        } else {
            record.setShowBadge(appCanShowBadge);
        }
        return null;
    }

    public void setConfig(RankingConfig config) {
        this.mConfig = config;
    }
}
