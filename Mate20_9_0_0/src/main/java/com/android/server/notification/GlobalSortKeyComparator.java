package com.android.server.notification;

import android.util.Slog;
import java.util.Comparator;

public class GlobalSortKeyComparator implements Comparator<NotificationRecord> {
    private static final String TAG = "GlobalSortComp";

    public int compare(NotificationRecord left, NotificationRecord right) {
        String str;
        StringBuilder stringBuilder;
        if (left.getGlobalSortKey() == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Missing left global sort key: ");
            stringBuilder.append(left);
            Slog.wtf(str, stringBuilder.toString());
            return 1;
        } else if (right.getGlobalSortKey() != null) {
            return left.getGlobalSortKey().compareTo(right.getGlobalSortKey());
        } else {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Missing right global sort key: ");
            stringBuilder.append(right);
            Slog.wtf(str, stringBuilder.toString());
            return -1;
        }
    }
}
