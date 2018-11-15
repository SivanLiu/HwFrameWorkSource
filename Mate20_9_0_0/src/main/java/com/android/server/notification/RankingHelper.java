package com.android.server.notification;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager.Policy;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ParceledListSlice;
import android.metrics.LogMaker;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.service.notification.NotificationListenerService.Ranking;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.Slog;
import android.util.SparseBooleanArray;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.util.Preconditions;
import com.android.internal.util.XmlUtils;
import com.android.server.job.JobSchedulerShellCommand;
import com.android.server.notification.NotificationManagerService.DumpFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class RankingHelper implements RankingConfig {
    private static final String ATT_APP_USER_LOCKED_FIELDS = "app_user_locked_fields";
    private static final String ATT_ID = "id";
    private static final String ATT_IMPORTANCE = "importance";
    private static final String ATT_NAME = "name";
    private static final String ATT_PRIORITY = "priority";
    private static final String ATT_SHOW_BADGE = "show_badge";
    private static final String ATT_UID = "uid";
    private static final String ATT_VERSION = "version";
    private static final String ATT_VISIBILITY = "visibility";
    private static final int DEFAULT_IMPORTANCE = -1000;
    private static final int DEFAULT_LOCKED_APP_FIELDS = 0;
    private static final int DEFAULT_PRIORITY = 0;
    private static final boolean DEFAULT_SHOW_BADGE = true;
    private static final int DEFAULT_VISIBILITY = -1000;
    private static final String TAG = "RankingHelper";
    private static final String TAG_CHANNEL = "channel";
    private static final String TAG_GROUP = "channelGroup";
    private static final String TAG_PACKAGE = "package";
    static final String TAG_RANKING = "ranking";
    private static final int XML_VERSION = 1;
    private boolean mAreChannelsBypassingDnd;
    private SparseBooleanArray mBadgingEnabled;
    private final Context mContext;
    private final GlobalSortKeyComparator mFinalComparator = new GlobalSortKeyComparator();
    private final PackageManager mPm;
    private final NotificationComparator mPreliminaryComparator;
    private final ArrayMap<String, NotificationRecord> mProxyByGroupTmp = new ArrayMap();
    private final RankingHandler mRankingHandler;
    private final ArrayMap<String, Record> mRecords = new ArrayMap();
    private final ArrayMap<String, Record> mRestoredWithoutUids = new ArrayMap();
    private final NotificationSignalExtractor[] mSignalExtractors;
    private final ArrayMap<String, NotificationSysMgrCfg> mSysMgrCfgMap = new ArrayMap();
    private ZenModeHelper mZenModeHelper;

    public @interface LockableAppFields {
        public static final int USER_LOCKED_IMPORTANCE = 1;
    }

    public static class NotificationSysMgrCfg {
        int smc_bypassDND;
        int smc_iconBadge;
        int smc_importance;
        String smc_packageName;
        int smc_userId;
        int smc_visilibity;
    }

    private static class Record {
        static int UNKNOWN_UID = -10000;
        ArrayMap<String, NotificationChannel> channels;
        Map<String, NotificationChannelGroup> groups;
        int importance;
        int lockedAppFields;
        String pkg;
        int priority;
        boolean showBadge;
        int uid;
        int visibility;

        private Record() {
            this.uid = UNKNOWN_UID;
            this.importance = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            this.priority = 0;
            this.visibility = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            this.showBadge = true;
            this.lockedAppFields = 0;
            this.channels = new ArrayMap();
            this.groups = new ConcurrentHashMap();
        }
    }

    public RankingHelper(Context context, PackageManager pm, RankingHandler rankingHandler, ZenModeHelper zenHelper, NotificationUsageStats usageStats, String[] extractorNames) {
        String str;
        StringBuilder stringBuilder;
        this.mContext = context;
        this.mRankingHandler = rankingHandler;
        this.mPm = pm;
        this.mZenModeHelper = zenHelper;
        this.mPreliminaryComparator = new NotificationComparator(this.mContext);
        updateBadgingEnabled();
        int N = extractorNames.length;
        this.mSignalExtractors = new NotificationSignalExtractor[N];
        boolean z = false;
        for (int i = 0; i < N; i++) {
            try {
                NotificationSignalExtractor extractor = (NotificationSignalExtractor) this.mContext.getClassLoader().loadClass(extractorNames[i]).newInstance();
                extractor.initialize(this.mContext, usageStats);
                extractor.setConfig(this);
                extractor.setZenHelper(zenHelper);
                this.mSignalExtractors[i] = extractor;
            } catch (ClassNotFoundException e) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't find extractor ");
                stringBuilder.append(extractorNames[i]);
                stringBuilder.append(".");
                Slog.w(str, stringBuilder.toString(), e);
            } catch (InstantiationException e2) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Couldn't instantiate extractor ");
                stringBuilder.append(extractorNames[i]);
                stringBuilder.append(".");
                Slog.w(str, stringBuilder.toString(), e2);
            } catch (IllegalAccessException e3) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Problem accessing extractor ");
                stringBuilder.append(extractorNames[i]);
                stringBuilder.append(".");
                Slog.w(str, stringBuilder.toString(), e3);
            }
        }
        if ((this.mZenModeHelper.getNotificationPolicy().state & 1) == 1) {
            z = true;
        }
        this.mAreChannelsBypassingDnd = z;
        updateChannelsBypassingDnd();
    }

    public <T extends NotificationSignalExtractor> T findExtractor(Class<T> extractorClass) {
        for (NotificationSignalExtractor extractor : this.mSignalExtractors) {
            if (extractorClass.equals(extractor.getClass())) {
                return extractor;
            }
        }
        return null;
    }

    public void extractSignals(NotificationRecord r) {
        for (NotificationSignalExtractor extractor : this.mSignalExtractors) {
            try {
                RankingReconsideration recon = extractor.process(r);
                if (recon != null) {
                    this.mRankingHandler.requestReconsideration(recon);
                }
            } catch (Throwable t) {
                Slog.w(TAG, "NotificationSignalExtractor failed.", t);
            }
        }
    }

    public void readXml(XmlPullParser parser, boolean forRestore) throws XmlPullParserException, IOException {
        XmlPullParser xmlPullParser = parser;
        int i = 2;
        if (parser.getEventType() == 2) {
            if (TAG_RANKING.equals(parser.getName())) {
                this.mRestoredWithoutUids.clear();
                while (true) {
                    int next = parser.next();
                    int type = next;
                    if (next != 1) {
                        String tag = parser.getName();
                        if (type != 3 || !TAG_RANKING.equals(tag)) {
                            if (type == i && "package".equals(tag)) {
                                int uid = XmlUtils.readIntAttribute(xmlPullParser, "uid", Record.UNKNOWN_UID);
                                String name = xmlPullParser.getAttributeValue(null, "name");
                                if (!TextUtils.isEmpty(name)) {
                                    int uid2;
                                    int readIntAttribute;
                                    int readIntAttribute2;
                                    int readIntAttribute3;
                                    boolean readBooleanAttribute;
                                    int i2;
                                    int i3;
                                    Record r;
                                    if (forRestore) {
                                        try {
                                            uid2 = this.mPm.getPackageUidAsUser(name, 0);
                                        } catch (NameNotFoundException e) {
                                        }
                                        readIntAttribute = XmlUtils.readIntAttribute(xmlPullParser, ATT_IMPORTANCE, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                                        readIntAttribute2 = XmlUtils.readIntAttribute(xmlPullParser, ATT_PRIORITY, 0);
                                        readIntAttribute3 = XmlUtils.readIntAttribute(xmlPullParser, ATT_VISIBILITY, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                                        readBooleanAttribute = XmlUtils.readBooleanAttribute(xmlPullParser, ATT_SHOW_BADGE, true);
                                        i = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                        i2 = 0;
                                        i3 = readIntAttribute2;
                                        r = getOrCreateRecord(name, uid2, readIntAttribute, i3, readIntAttribute3, readBooleanAttribute);
                                        r.importance = XmlUtils.readIntAttribute(xmlPullParser, ATT_IMPORTANCE, i);
                                        r.priority = XmlUtils.readIntAttribute(xmlPullParser, ATT_PRIORITY, i2);
                                        r.visibility = XmlUtils.readIntAttribute(xmlPullParser, ATT_VISIBILITY, i);
                                        r.showBadge = XmlUtils.readBooleanAttribute(xmlPullParser, ATT_SHOW_BADGE, true);
                                        r.lockedAppFields = XmlUtils.readIntAttribute(xmlPullParser, ATT_APP_USER_LOCKED_FIELDS, i2);
                                        readIntAttribute = parser.getDepth();
                                        while (true) {
                                            next = readIntAttribute;
                                            readIntAttribute = parser.next();
                                            type = readIntAttribute;
                                            if (readIntAttribute != 1 || (type == 3 && parser.getDepth() <= next)) {
                                                try {
                                                    deleteDefaultChannelIfNeeded(r);
                                                    break;
                                                } catch (NameNotFoundException e2) {
                                                    NameNotFoundException nameNotFoundException = e2;
                                                    String str = TAG;
                                                    StringBuilder stringBuilder = new StringBuilder();
                                                    stringBuilder.append("deleteDefaultChannelIfNeeded - Exception: ");
                                                    stringBuilder.append(e2);
                                                    Slog.e(str, stringBuilder.toString());
                                                }
                                            } else {
                                                if (type != 3 && type != 4) {
                                                    String str2;
                                                    String id;
                                                    String tagName = parser.getName();
                                                    if (TAG_GROUP.equals(tagName)) {
                                                        str2 = null;
                                                        id = xmlPullParser.getAttributeValue(null, ATT_ID);
                                                        CharSequence groupName = xmlPullParser.getAttributeValue(null, "name");
                                                        if (!TextUtils.isEmpty(id)) {
                                                            NotificationChannelGroup group = new NotificationChannelGroup(id, groupName);
                                                            group.populateFromXml(xmlPullParser);
                                                            r.groups.put(id, group);
                                                        }
                                                    } else {
                                                        str2 = null;
                                                    }
                                                    if (TAG_CHANNEL.equals(tagName)) {
                                                        id = xmlPullParser.getAttributeValue(str2, ATT_ID);
                                                        name = xmlPullParser.getAttributeValue(str2, "name");
                                                        int channelImportance = XmlUtils.readIntAttribute(xmlPullParser, ATT_IMPORTANCE, i);
                                                        if (!(TextUtils.isEmpty(id) || TextUtils.isEmpty(name))) {
                                                            NotificationChannel channel = new NotificationChannel(id, name, channelImportance);
                                                            if (forRestore) {
                                                                channel.populateFromXmlForRestore(xmlPullParser, this.mContext);
                                                            } else {
                                                                channel.populateFromXml(xmlPullParser);
                                                            }
                                                            r.channels.put(id, channel);
                                                        }
                                                    }
                                                }
                                                readIntAttribute = next;
                                            }
                                        }
                                    }
                                    uid2 = uid;
                                    readIntAttribute = XmlUtils.readIntAttribute(xmlPullParser, ATT_IMPORTANCE, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                                    readIntAttribute2 = XmlUtils.readIntAttribute(xmlPullParser, ATT_PRIORITY, 0);
                                    readIntAttribute3 = XmlUtils.readIntAttribute(xmlPullParser, ATT_VISIBILITY, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                                    readBooleanAttribute = XmlUtils.readBooleanAttribute(xmlPullParser, ATT_SHOW_BADGE, true);
                                    i = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
                                    i2 = 0;
                                    i3 = readIntAttribute2;
                                    r = getOrCreateRecord(name, uid2, readIntAttribute, i3, readIntAttribute3, readBooleanAttribute);
                                    r.importance = XmlUtils.readIntAttribute(xmlPullParser, ATT_IMPORTANCE, i);
                                    r.priority = XmlUtils.readIntAttribute(xmlPullParser, ATT_PRIORITY, i2);
                                    r.visibility = XmlUtils.readIntAttribute(xmlPullParser, ATT_VISIBILITY, i);
                                    r.showBadge = XmlUtils.readBooleanAttribute(xmlPullParser, ATT_SHOW_BADGE, true);
                                    r.lockedAppFields = XmlUtils.readIntAttribute(xmlPullParser, ATT_APP_USER_LOCKED_FIELDS, i2);
                                    readIntAttribute = parser.getDepth();
                                    while (true) {
                                        next = readIntAttribute;
                                        readIntAttribute = parser.next();
                                        type = readIntAttribute;
                                        if (readIntAttribute != 1) {
                                        }
                                        deleteDefaultChannelIfNeeded(r);
                                        readIntAttribute = next;
                                    }
                                }
                            }
                            i = 2;
                        } else {
                            return;
                        }
                    }
                    throw new IllegalStateException("Failed to reach END_DOCUMENT");
                }
            }
        }
    }

    private static String recordKey(String pkg, int uid) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(pkg);
        stringBuilder.append("|");
        stringBuilder.append(uid);
        return stringBuilder.toString();
    }

    private Record getRecord(String pkg, int uid) {
        Record record;
        String key = recordKey(pkg, uid);
        synchronized (this.mRecords) {
            record = (Record) this.mRecords.get(key);
        }
        return record;
    }

    private Record getOrCreateRecord(String pkg, int uid) {
        return getOrCreateRecord(pkg, uid, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE, 0, JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE, true);
    }

    private Record getOrCreateRecord(String pkg, int uid, int importance, int priority, int visibility, boolean showBadge) {
        Record r;
        String key = recordKey(pkg, uid);
        synchronized (this.mRecords) {
            r = uid == Record.UNKNOWN_UID ? this.mRestoredWithoutUids.get(pkg) : this.mRecords.get(key);
            if (r == null) {
                r = new Record();
                r.pkg = pkg;
                r.uid = uid;
                r.importance = importance;
                r.priority = priority;
                r.visibility = visibility;
                r.showBadge = showBadge;
                NotificationSysMgrCfg cfg = (NotificationSysMgrCfg) this.mSysMgrCfgMap.get(key);
                if (cfg != null) {
                    r.importance = cfg.smc_importance;
                    r.visibility = cfg.smc_visilibity;
                    int i = 0;
                    r.showBadge = cfg.smc_iconBadge != 0;
                    if (cfg.smc_bypassDND != 0) {
                        i = 2;
                    }
                    r.priority = i;
                }
                try {
                    createDefaultChannelIfNeeded(r);
                } catch (NameNotFoundException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("createDefaultChannelIfNeeded - Exception: ");
                    stringBuilder.append(e);
                    Slog.e(str, stringBuilder.toString());
                }
                if (r.uid == Record.UNKNOWN_UID) {
                    this.mRestoredWithoutUids.put(pkg, r);
                } else if (r.uid >= 0) {
                    this.mRecords.put(key, r);
                } else {
                    Log.e(TAG, "record uid is below zero", new Throwable());
                }
            }
        }
        return r;
    }

    private boolean shouldHaveDefaultChannel(Record r) throws NameNotFoundException {
        if (this.mPm.getApplicationInfoAsUser(r.pkg, 0, UserHandle.getUserId(r.uid)).targetSdkVersion >= 26) {
            return false;
        }
        return true;
    }

    private void deleteDefaultChannelIfNeeded(Record r) throws NameNotFoundException {
        if (r.channels.containsKey("miscellaneous") && !shouldHaveDefaultChannel(r)) {
            r.channels.remove("miscellaneous");
        }
    }

    private void createDefaultChannelIfNeeded(Record r) throws NameNotFoundException {
        if (r.channels.containsKey("miscellaneous")) {
            ((NotificationChannel) r.channels.get("miscellaneous")).setName(this.mContext.getString(17039921));
        } else if (shouldHaveDefaultChannel(r)) {
            NotificationChannel channel = new NotificationChannel("miscellaneous", this.mContext.getString(17039921), r.importance);
            channel.setBypassDnd(r.priority == 2);
            channel.setLockscreenVisibility(r.visibility);
            if (r.importance != JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
                channel.lockFields(4);
            }
            if (r.priority != 0) {
                channel.lockFields(1);
            }
            if (r.visibility != JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
                channel.lockFields(2);
            }
            r.channels.put(channel.getId(), channel);
        }
    }

    public void writeXml(XmlSerializer out, boolean forBackup) throws IOException {
        out.startTag(null, TAG_RANKING);
        out.attribute(null, ATT_VERSION, Integer.toString(1));
        synchronized (this.mRecords) {
            int N = this.mRecords.size();
            for (int i = 0; i < N; i++) {
                Record r = (Record) this.mRecords.valueAt(i);
                if (r != null && (!forBackup || UserHandle.getUserId(r.uid) == 0)) {
                    boolean hasNonDefaultSettings = (r.importance == JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE && r.priority == 0 && r.visibility == JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE && r.showBadge && r.lockedAppFields == 0 && r.channels.size() <= 0 && r.groups.size() <= 0) ? false : true;
                    if (hasNonDefaultSettings) {
                        out.startTag(null, "package");
                        out.attribute(null, "name", r.pkg);
                        if (r.importance != JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
                            out.attribute(null, ATT_IMPORTANCE, Integer.toString(r.importance));
                        }
                        if (r.priority != 0) {
                            out.attribute(null, ATT_PRIORITY, Integer.toString(r.priority));
                        }
                        if (r.visibility != JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
                            out.attribute(null, ATT_VISIBILITY, Integer.toString(r.visibility));
                        }
                        out.attribute(null, ATT_SHOW_BADGE, Boolean.toString(r.showBadge));
                        out.attribute(null, ATT_APP_USER_LOCKED_FIELDS, Integer.toString(r.lockedAppFields));
                        if (!forBackup) {
                            out.attribute(null, "uid", Integer.toString(r.uid));
                        }
                        for (NotificationChannelGroup group : r.groups.values()) {
                            group.writeXml(out);
                        }
                        for (NotificationChannel channel : r.channels.values()) {
                            if (channel != null) {
                                if (!forBackup) {
                                    channel.writeXml(out);
                                } else if (!channel.isDeleted()) {
                                    channel.writeXmlForBackup(out, this.mContext);
                                }
                            }
                        }
                        out.endTag(null, "package");
                    }
                }
            }
        }
        out.endTag(null, TAG_RANKING);
    }

    private void updateConfig() {
        for (NotificationSignalExtractor config : this.mSignalExtractors) {
            config.setConfig(this);
        }
        this.mRankingHandler.requestSort();
    }

    public void sort(ArrayList<NotificationRecord> notificationList) {
        int i;
        ArrayList<NotificationRecord> arrayList = notificationList;
        int N = notificationList.size();
        for (i = N - 1; i >= 0; i--) {
            ((NotificationRecord) arrayList.get(i)).setGlobalSortKey(null);
        }
        Collections.sort(arrayList, this.mPreliminaryComparator);
        synchronized (this.mProxyByGroupTmp) {
            for (i = N - 1; i >= 0; i--) {
                NotificationRecord record = (NotificationRecord) arrayList.get(i);
                record.setAuthoritativeRank(i);
                String groupKey = record.getGroupKey();
                if (((NotificationRecord) this.mProxyByGroupTmp.get(groupKey)) == null) {
                    this.mProxyByGroupTmp.put(groupKey, record);
                }
            }
            for (int i2 = 0; i2 < N; i2++) {
                String groupSortKeyPortion;
                NotificationRecord record2 = (NotificationRecord) arrayList.get(i2);
                NotificationRecord groupProxy = (NotificationRecord) this.mProxyByGroupTmp.get(record2.getGroupKey());
                String groupSortKey = record2.getNotification().getSortKey();
                if (groupSortKey == null) {
                    groupSortKeyPortion = "nsk";
                } else if (groupSortKey.equals(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS)) {
                    groupSortKeyPortion = "esk";
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("gsk=");
                    stringBuilder.append(groupSortKey);
                    groupSortKeyPortion = stringBuilder.toString();
                }
                boolean isGroupSummary = record2.getNotification().isGroupSummary();
                String str = "intrsv=%c:grnk=0x%04x:gsmry=%c:%s:rnk=0x%04x";
                Object[] objArr = new Object[5];
                char c = '0';
                char c2 = (!record2.isRecentlyIntrusive() || record2.getImportance() <= 1) ? '1' : '0';
                objArr[0] = Character.valueOf(c2);
                objArr[1] = Integer.valueOf(groupProxy != null ? groupProxy.getAuthoritativeRank() : i2);
                if (!isGroupSummary) {
                    c = '1';
                }
                objArr[2] = Character.valueOf(c);
                objArr[3] = groupSortKeyPortion;
                objArr[4] = Integer.valueOf(record2.getAuthoritativeRank());
                record2.setGlobalSortKey(String.format(str, objArr));
            }
            this.mProxyByGroupTmp.clear();
        }
        Collections.sort(arrayList, this.mFinalComparator);
    }

    public int indexOf(ArrayList<NotificationRecord> notificationList, NotificationRecord target) {
        return Collections.binarySearch(notificationList, target, this.mFinalComparator);
    }

    public int getImportance(String packageName, int uid) {
        return getOrCreateRecord(packageName, uid).importance;
    }

    public boolean getIsAppImportanceLocked(String packageName, int uid) {
        return (getOrCreateRecord(packageName, uid).lockedAppFields & 1) != 0;
    }

    public boolean canShowBadge(String packageName, int uid) {
        return getOrCreateRecord(packageName, uid).showBadge;
    }

    public void setShowBadge(String packageName, int uid, boolean showBadge) {
        getOrCreateRecord(packageName, uid).showBadge = showBadge;
        updateConfig();
    }

    public boolean isGroupBlocked(String packageName, int uid, String groupId) {
        if (groupId == null) {
            return false;
        }
        NotificationChannelGroup group = (NotificationChannelGroup) getOrCreateRecord(packageName, uid).groups.get(groupId);
        if (group == null) {
            return false;
        }
        return group.isBlocked();
    }

    int getPackagePriority(String pkg, int uid) {
        return getOrCreateRecord(pkg, uid).priority;
    }

    int getPackageVisibility(String pkg, int uid) {
        return getOrCreateRecord(pkg, uid).visibility;
    }

    public void createNotificationChannelGroup(String pkg, int uid, NotificationChannelGroup group, boolean fromTargetApp) {
        Preconditions.checkNotNull(pkg);
        Preconditions.checkNotNull(group);
        Preconditions.checkNotNull(group.getId());
        Preconditions.checkNotNull(Boolean.valueOf(TextUtils.isEmpty(group.getName()) ^ 1));
        Record r = getOrCreateRecord(pkg, uid);
        if (r != null) {
            NotificationChannelGroup oldGroup = (NotificationChannelGroup) r.groups.get(group.getId());
            if (!group.equals(oldGroup)) {
                MetricsLogger.action(getChannelGroupLog(group.getId(), pkg));
            }
            if (oldGroup != null) {
                group.setChannels(oldGroup.getChannels());
                if (fromTargetApp) {
                    group.setBlocked(oldGroup.isBlocked());
                }
            }
            r.groups.put(group.getId(), group);
            return;
        }
        throw new IllegalArgumentException("Invalid package");
    }

    public void createNotificationChannel(String pkg, int uid, NotificationChannel channel, boolean fromTargetApp, boolean hasDndAccess) {
        Preconditions.checkNotNull(pkg);
        Preconditions.checkNotNull(channel);
        Preconditions.checkNotNull(channel.getId());
        Preconditions.checkArgument(TextUtils.isEmpty(channel.getName()) ^ true);
        Record r = getOrCreateRecord(pkg, uid);
        if (r == null) {
            throw new IllegalArgumentException("Invalid package");
        } else if (channel.getGroup() != null && !r.groups.containsKey(channel.getGroup())) {
            throw new IllegalArgumentException("NotificationChannelGroup doesn't exist");
        } else if ("miscellaneous".equals(channel.getId())) {
            throw new IllegalArgumentException("Reserved id");
        } else {
            NotificationChannel existing = (NotificationChannel) r.channels.get(channel.getId());
            if (existing != null && fromTargetApp) {
                if (existing.isDeleted()) {
                    existing.setDeleted(false);
                    MetricsLogger.action(getChannelLog(channel, pkg).setType(1));
                }
                existing.setName(channel.getName().toString());
                existing.setDescription(channel.getDescription());
                existing.setBlockableSystem(channel.isBlockableSystem());
                if (existing.getGroup() == null) {
                    existing.setGroup(channel.getGroup());
                }
                if (existing.getUserLockedFields() == 0 && channel.getImportance() < existing.getImportance()) {
                    existing.setImportance(channel.getImportance());
                }
                if (existing.getUserLockedFields() == 0 && hasDndAccess) {
                    boolean bypassDnd = channel.canBypassDnd();
                    existing.setBypassDnd(bypassDnd);
                    if (bypassDnd != this.mAreChannelsBypassingDnd) {
                        updateChannelsBypassingDnd();
                    }
                }
                updateConfig();
            } else if (channel.getImportance() < 0 || channel.getImportance() > 5) {
                throw new IllegalArgumentException("Invalid importance level");
            } else {
                if (fromTargetApp && !hasDndAccess) {
                    channel.setBypassDnd(r.priority == 2);
                }
                if (fromTargetApp) {
                    channel.setLockscreenVisibility(r.visibility);
                }
                clearLockedFields(channel);
                if (channel.getLockscreenVisibility() == 1) {
                    channel.setLockscreenVisibility(JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
                }
                if (!r.showBadge) {
                    channel.setShowBadge(false);
                }
                r.channels.put(channel.getId(), channel);
                if (channel.canBypassDnd() != this.mAreChannelsBypassingDnd) {
                    updateChannelsBypassingDnd();
                }
                MetricsLogger.action(getChannelLog(channel, pkg).setType(1));
            }
        }
    }

    void clearLockedFields(NotificationChannel channel) {
        channel.unlockFields(channel.getUserLockedFields());
    }

    public void updateNotificationChannel(String pkg, int uid, NotificationChannel updatedChannel, boolean fromUser) {
        Preconditions.checkNotNull(updatedChannel);
        Preconditions.checkNotNull(updatedChannel.getId());
        Record r = getOrCreateRecord(pkg, uid);
        if (r != null) {
            NotificationChannel channel = (NotificationChannel) r.channels.get(updatedChannel.getId());
            if (channel == null || channel.isDeleted()) {
                throw new IllegalArgumentException("Channel does not exist");
            }
            if (updatedChannel.getLockscreenVisibility() == 1) {
                updatedChannel.setLockscreenVisibility(JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE);
            }
            if (!fromUser) {
                updatedChannel.unlockFields(updatedChannel.getUserLockedFields());
            }
            if (fromUser) {
                updatedChannel.lockFields(channel.getUserLockedFields());
                lockFieldsForUpdate(channel, updatedChannel);
            }
            r.channels.put(updatedChannel.getId(), updatedChannel);
            if ("miscellaneous".equals(updatedChannel.getId()) && r.channels.size() == 1) {
                Slog.d(TAG, "only modify pkg when there is single channel");
                r.importance = updatedChannel.getImportance();
                r.priority = updatedChannel.canBypassDnd() ? 2 : 0;
                r.visibility = updatedChannel.getLockscreenVisibility();
                r.showBadge = updatedChannel.canShowBadge();
            }
            if (!channel.equals(updatedChannel)) {
                MetricsLogger.action(getChannelLog(updatedChannel, pkg));
            }
            if (updatedChannel.canBypassDnd() != this.mAreChannelsBypassingDnd) {
                updateChannelsBypassingDnd();
            }
            updateConfig();
            return;
        }
        throw new IllegalArgumentException("Invalid package");
    }

    public NotificationChannel getNotificationChannel(String pkg, int uid, String channelId, boolean includeDeleted) {
        Preconditions.checkNotNull(pkg);
        Record r = getOrCreateRecord(pkg, uid);
        if (r == null) {
            return null;
        }
        if (channelId == null) {
            channelId = "miscellaneous";
        }
        NotificationChannel nc = (NotificationChannel) r.channels.get(channelId);
        if (nc == null || (!includeDeleted && nc.isDeleted())) {
            return null;
        }
        return nc;
    }

    public void deleteNotificationChannel(String pkg, int uid, String channelId) {
        Record r = getRecord(pkg, uid);
        if (r != null) {
            NotificationChannel channel = (NotificationChannel) r.channels.get(channelId);
            if (channel != null) {
                channel.setDeleted(true);
                LogMaker lm = getChannelLog(channel, pkg);
                lm.setType(2);
                MetricsLogger.action(lm);
                if (this.mAreChannelsBypassingDnd && channel.canBypassDnd()) {
                    updateChannelsBypassingDnd();
                }
            }
        }
    }

    @VisibleForTesting
    public void permanentlyDeleteNotificationChannel(String pkg, int uid, String channelId) {
        Preconditions.checkNotNull(pkg);
        Preconditions.checkNotNull(channelId);
        Record r = getRecord(pkg, uid);
        if (r != null) {
            r.channels.remove(channelId);
        }
    }

    public void permanentlyDeleteNotificationChannels(String pkg, int uid) {
        Preconditions.checkNotNull(pkg);
        Record r = getRecord(pkg, uid);
        if (r != null) {
            for (int i = r.channels.size() - 1; i >= 0; i--) {
                String key = (String) r.channels.keyAt(i);
                if (!"miscellaneous".equals(key)) {
                    r.channels.remove(key);
                }
            }
        }
    }

    public NotificationChannelGroup getNotificationChannelGroupWithChannels(String pkg, int uid, String groupId, boolean includeDeleted) {
        Preconditions.checkNotNull(pkg);
        Record r = getRecord(pkg, uid);
        if (r == null || groupId == null || !r.groups.containsKey(groupId)) {
            return null;
        }
        NotificationChannelGroup group = ((NotificationChannelGroup) r.groups.get(groupId)).clone();
        group.setChannels(new ArrayList());
        int N = r.channels.size();
        for (int i = 0; i < N; i++) {
            NotificationChannel nc = (NotificationChannel) r.channels.valueAt(i);
            if ((includeDeleted || !nc.isDeleted()) && groupId.equals(nc.getGroup())) {
                group.addChannel(nc);
            }
        }
        return group;
    }

    public NotificationChannelGroup getNotificationChannelGroup(String groupId, String pkg, int uid) {
        Preconditions.checkNotNull(pkg);
        Record r = getRecord(pkg, uid);
        if (r == null) {
            return null;
        }
        return (NotificationChannelGroup) r.groups.get(groupId);
    }

    public ParceledListSlice<NotificationChannelGroup> getNotificationChannelGroups(String pkg, int uid, boolean includeDeleted, boolean includeNonGrouped) {
        Preconditions.checkNotNull(pkg);
        Map<String, NotificationChannelGroup> groups = new ArrayMap();
        Record r = getRecord(pkg, uid);
        if (r == null) {
            return ParceledListSlice.emptyList();
        }
        NotificationChannelGroup nonGrouped = new NotificationChannelGroup(null, null);
        int N = r.channels.size();
        for (int i = 0; i < N; i++) {
            NotificationChannel nc = (NotificationChannel) r.channels.valueAt(i);
            if (includeDeleted || !nc.isDeleted()) {
                if (nc.getGroup() == null) {
                    nonGrouped.addChannel(nc);
                } else if (r.groups.get(nc.getGroup()) != null) {
                    NotificationChannelGroup ncg = (NotificationChannelGroup) groups.get(nc.getGroup());
                    if (ncg == null) {
                        ncg = ((NotificationChannelGroup) r.groups.get(nc.getGroup())).clone();
                        ncg.setChannels(new ArrayList());
                        groups.put(nc.getGroup(), ncg);
                    }
                    ncg.addChannel(nc);
                }
            }
        }
        if (includeNonGrouped && nonGrouped.getChannels().size() > 0) {
            groups.put(null, nonGrouped);
        }
        return new ParceledListSlice(new ArrayList(groups.values()));
    }

    public List<NotificationChannel> deleteNotificationChannelGroup(String pkg, int uid, String groupId) {
        List<NotificationChannel> deletedChannels = new ArrayList();
        Record r = getRecord(pkg, uid);
        if (r == null || TextUtils.isEmpty(groupId)) {
            return deletedChannels;
        }
        r.groups.remove(groupId);
        int N = r.channels.size();
        for (int i = 0; i < N; i++) {
            NotificationChannel nc = (NotificationChannel) r.channels.valueAt(i);
            if (groupId.equals(nc.getGroup())) {
                nc.setDeleted(true);
                deletedChannels.add(nc);
            }
        }
        return deletedChannels;
    }

    public Collection<NotificationChannelGroup> getNotificationChannelGroups(String pkg, int uid) {
        Record r = getRecord(pkg, uid);
        if (r == null) {
            return new ArrayList();
        }
        return r.groups.values();
    }

    public ParceledListSlice<NotificationChannel> getNotificationChannels(String pkg, int uid, boolean includeDeleted) {
        Preconditions.checkNotNull(pkg);
        List<NotificationChannel> channels = new ArrayList();
        Record r = getRecord(pkg, uid);
        if (r == null) {
            return ParceledListSlice.emptyList();
        }
        int N = r.channels.size();
        for (int i = 0; i < N; i++) {
            NotificationChannel nc = (NotificationChannel) r.channels.valueAt(i);
            if (includeDeleted || !nc.isDeleted()) {
                channels.add(nc);
            }
        }
        return new ParceledListSlice(channels);
    }

    public boolean onlyHasDefaultChannel(String pkg, int uid) {
        Record r = getOrCreateRecord(pkg, uid);
        if (r.channels.size() == 1 && r.channels.containsKey("miscellaneous")) {
            return true;
        }
        return false;
    }

    public int getDeletedChannelCount(String pkg, int uid) {
        Preconditions.checkNotNull(pkg);
        int deletedCount = 0;
        Record r = getRecord(pkg, uid);
        if (r == null) {
            return 0;
        }
        int N = r.channels.size();
        for (int i = 0; i < N; i++) {
            if (((NotificationChannel) r.channels.valueAt(i)).isDeleted()) {
                deletedCount++;
            }
        }
        return deletedCount;
    }

    public int getBlockedChannelCount(String pkg, int uid) {
        Preconditions.checkNotNull(pkg);
        int blockedCount = 0;
        Record r = getRecord(pkg, uid);
        if (r == null) {
            return 0;
        }
        int N = r.channels.size();
        for (int i = 0; i < N; i++) {
            NotificationChannel nc = (NotificationChannel) r.channels.valueAt(i);
            if (!nc.isDeleted() && nc.getImportance() == 0) {
                blockedCount++;
            }
        }
        return blockedCount;
    }

    public int getBlockedAppCount(int userId) {
        int count = 0;
        synchronized (this.mRecords) {
            int N = this.mRecords.size();
            for (int i = 0; i < N; i++) {
                Record r = (Record) this.mRecords.valueAt(i);
                if (userId == UserHandle.getUserId(r.uid) && r.importance == 0) {
                    count++;
                }
            }
        }
        return count;
    }

    /* JADX WARNING: Missing block: B:15:0x003d, code:
            return;
     */
    /* JADX WARNING: Missing block: B:20:0x0047, code:
            if (r9.mAreChannelsBypassingDnd == false) goto L_0x004e;
     */
    /* JADX WARNING: Missing block: B:21:0x0049, code:
            r9.mAreChannelsBypassingDnd = false;
            updateZenPolicy(false);
     */
    /* JADX WARNING: Missing block: B:22:0x004e, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void updateChannelsBypassingDnd() {
        synchronized (this.mRecords) {
            int numRecords = this.mRecords.size();
            for (int recordIndex = 0; recordIndex < numRecords; recordIndex++) {
                Record r = (Record) this.mRecords.valueAt(recordIndex);
                int numChannels = r.channels.size();
                int channelIndex = 0;
                while (channelIndex < numChannels) {
                    NotificationChannel channel = (NotificationChannel) r.channels.valueAt(channelIndex);
                    if (channel.isDeleted() || !channel.canBypassDnd()) {
                        channelIndex++;
                    } else if (!this.mAreChannelsBypassingDnd) {
                        this.mAreChannelsBypassingDnd = true;
                        updateZenPolicy(true);
                    }
                }
            }
        }
    }

    public void updateZenPolicy(boolean areChannelsBypassingDnd) {
        int i;
        Policy policy = this.mZenModeHelper.getNotificationPolicy();
        ZenModeHelper zenModeHelper = this.mZenModeHelper;
        int i2 = policy.priorityCategories;
        int i3 = policy.priorityCallSenders;
        int i4 = policy.priorityMessageSenders;
        int i5 = policy.suppressedVisualEffects;
        if (areChannelsBypassingDnd) {
            i = 1;
        } else {
            i = 0;
        }
        zenModeHelper.setNotificationPolicy(new Policy(i2, i3, i4, i5, i));
    }

    public boolean areChannelsBypassingDnd() {
        return this.mAreChannelsBypassingDnd;
    }

    public void setImportance(String pkgName, int uid, int importance) {
        getOrCreateRecord(pkgName, uid).importance = importance;
        updateConfig();
    }

    public void setEnabled(String packageName, int uid, boolean enabled) {
        int i = 0;
        if ((getImportance(packageName, uid) != 0) != enabled) {
            if (enabled) {
                i = JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE;
            }
            setImportance(packageName, uid, i);
        }
    }

    public void setAppImportanceLocked(String packageName, int uid) {
        Record record = getOrCreateRecord(packageName, uid);
        if ((record.lockedAppFields & 1) == 0) {
            record.lockedAppFields |= 1;
            updateConfig();
        }
    }

    @VisibleForTesting
    void lockFieldsForUpdate(NotificationChannel original, NotificationChannel update) {
        if (original.canBypassDnd() != update.canBypassDnd()) {
            update.lockFields(1);
        }
        if (original.getLockscreenVisibility() != update.getLockscreenVisibility()) {
            update.lockFields(2);
        }
        if (original.getImportance() != update.getImportance()) {
            update.lockFields(4);
        }
        if (!(original.shouldShowLights() == update.shouldShowLights() && original.getLightColor() == update.getLightColor())) {
            update.lockFields(8);
        }
        if (!Objects.equals(original.getSound(), update.getSound())) {
            update.lockFields(32);
        }
        if (!(Arrays.equals(original.getVibrationPattern(), update.getVibrationPattern()) && original.shouldVibrate() == update.shouldVibrate())) {
            update.lockFields(16);
        }
        if (original.canShowBadge() != update.canShowBadge()) {
            update.lockFields(128);
        }
    }

    public void dump(PrintWriter pw, String prefix, DumpFilter filter) {
        pw.print(prefix);
        pw.print("mSignalExtractors.length = ");
        pw.println(N);
        for (Object obj : this.mSignalExtractors) {
            pw.print(prefix);
            pw.print("  ");
            pw.println(obj.getClass().getSimpleName());
        }
        pw.print(prefix);
        pw.println("per-package config:");
        pw.println("Records:");
        synchronized (this.mRecords) {
            dumpRecords(pw, prefix, filter, this.mRecords);
        }
        pw.println("Restored without uid:");
        dumpRecords(pw, prefix, filter, this.mRestoredWithoutUids);
    }

    public void dump(ProtoOutputStream proto, DumpFilter filter) {
        for (Object obj : this.mSignalExtractors) {
            proto.write(2237677961217L, obj.getClass().getSimpleName());
        }
        synchronized (this.mRecords) {
            dumpRecords(proto, 2246267895810L, filter, this.mRecords);
        }
        dumpRecords(proto, 2246267895811L, filter, this.mRestoredWithoutUids);
    }

    private static void dumpRecords(ProtoOutputStream proto, long fieldId, DumpFilter filter, ArrayMap<String, Record> records) {
        int N = records.size();
        for (int i = 0; i < N; i++) {
            Record r = (Record) records.valueAt(i);
            if (filter.matches(r.pkg)) {
                long fToken = proto.start(fieldId);
                proto.write(1138166333441L, r.pkg);
                proto.write(1120986464258L, r.uid);
                proto.write(1172526071811L, r.importance);
                proto.write(1120986464260L, r.priority);
                proto.write(1172526071813L, r.visibility);
                proto.write(1133871366150L, r.showBadge);
                for (NotificationChannel channel : r.channels.values()) {
                    channel.writeToProto(proto, 2246267895815L);
                }
                for (NotificationChannelGroup group : r.groups.values()) {
                    group.writeToProto(proto, 2246267895816L);
                }
                proto.end(fToken);
            }
        }
    }

    private static void dumpRecords(PrintWriter pw, String prefix, DumpFilter filter, ArrayMap<String, Record> records) {
        int N = records.size();
        for (int i = 0; i < N; i++) {
            Record r = (Record) records.valueAt(i);
            if (filter.matches(r.pkg)) {
                pw.print(prefix);
                pw.print("  AppSettings: ");
                pw.print(r.pkg);
                pw.print(" (");
                pw.print(r.uid == Record.UNKNOWN_UID ? "UNKNOWN_UID" : Integer.toString(r.uid));
                pw.print(')');
                if (r.importance != JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
                    pw.print(" importance=");
                    pw.print(Ranking.importanceToString(r.importance));
                }
                if (r.priority != 0) {
                    pw.print(" priority=");
                    pw.print(Notification.priorityToString(r.priority));
                }
                if (r.visibility != JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
                    pw.print(" visibility=");
                    pw.print(Notification.visibilityToString(r.visibility));
                }
                pw.print(" showBadge=");
                pw.print(Boolean.toString(r.showBadge));
                pw.println();
                for (NotificationChannel channel : r.channels.values()) {
                    pw.print(prefix);
                    pw.print("  ");
                    pw.print("  ");
                    pw.println(channel);
                }
                for (NotificationChannelGroup group : r.groups.values()) {
                    pw.print(prefix);
                    pw.print("  ");
                    pw.print("  ");
                    pw.println(group);
                }
            }
        }
    }

    public JSONObject dumpJson(DumpFilter filter) {
        JSONObject ranking = new JSONObject();
        JSONArray records = new JSONArray();
        try {
            ranking.put("noUid", this.mRestoredWithoutUids.size());
        } catch (JSONException e) {
        }
        synchronized (this.mRecords) {
            int N = this.mRecords.size();
            for (int i = 0; i < N; i++) {
                Record r = (Record) this.mRecords.valueAt(i);
                if (filter == null || filter.matches(r.pkg)) {
                    JSONObject record = new JSONObject();
                    try {
                        record.put("userId", UserHandle.getUserId(r.uid));
                        record.put("packageName", r.pkg);
                        if (r.importance != JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
                            record.put(ATT_IMPORTANCE, Ranking.importanceToString(r.importance));
                        }
                        if (r.priority != 0) {
                            record.put(ATT_PRIORITY, Notification.priorityToString(r.priority));
                        }
                        if (r.visibility != JobSchedulerShellCommand.CMD_ERR_NO_PACKAGE) {
                            record.put(ATT_VISIBILITY, Notification.visibilityToString(r.visibility));
                        }
                        if (!r.showBadge) {
                            record.put("showBadge", Boolean.valueOf(r.showBadge));
                        }
                        JSONArray channels = new JSONArray();
                        for (NotificationChannel channel : r.channels.values()) {
                            channels.put(channel.toJson());
                        }
                        record.put("channels", channels);
                        JSONArray groups = new JSONArray();
                        for (NotificationChannelGroup group : r.groups.values()) {
                            groups.put(group.toJson());
                        }
                        record.put("groups", groups);
                    } catch (JSONException e2) {
                    }
                    records.put(record);
                }
            }
        }
        try {
            ranking.put("records", records);
        } catch (JSONException e3) {
        }
        return ranking;
    }

    public JSONArray dumpBansJson(DumpFilter filter) {
        JSONArray bans = new JSONArray();
        for (Entry<Integer, String> ban : getPackageBans().entrySet()) {
            int userId = UserHandle.getUserId(((Integer) ban.getKey()).intValue());
            String packageName = (String) ban.getValue();
            if (filter == null || filter.matches(packageName)) {
                JSONObject banJson = new JSONObject();
                try {
                    banJson.put("userId", userId);
                    banJson.put("packageName", packageName);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                bans.put(banJson);
            }
        }
        return bans;
    }

    public Map<Integer, String> getPackageBans() {
        ArrayMap<Integer, String> packageBans;
        synchronized (this.mRecords) {
            int N = this.mRecords.size();
            packageBans = new ArrayMap(N);
            for (int i = 0; i < N; i++) {
                Record r = (Record) this.mRecords.valueAt(i);
                if (r.importance == 0) {
                    packageBans.put(Integer.valueOf(r.uid), r.pkg);
                }
            }
        }
        return packageBans;
    }

    public JSONArray dumpChannelsJson(DumpFilter filter) {
        JSONArray channels = new JSONArray();
        for (Entry<String, Integer> channelCount : getPackageChannels().entrySet()) {
            String packageName = (String) channelCount.getKey();
            if (filter == null || filter.matches(packageName)) {
                JSONObject channelCountJson = new JSONObject();
                try {
                    channelCountJson.put("packageName", packageName);
                    channelCountJson.put("channelCount", channelCount.getValue());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                channels.put(channelCountJson);
            }
        }
        return channels;
    }

    private Map<String, Integer> getPackageChannels() {
        ArrayMap<String, Integer> packageChannels = new ArrayMap();
        synchronized (this.mRecords) {
            for (int i = 0; i < this.mRecords.size(); i++) {
                Record r = (Record) this.mRecords.valueAt(i);
                int channelCount = 0;
                for (int j = 0; j < r.channels.size(); j++) {
                    if (!((NotificationChannel) r.channels.valueAt(j)).isDeleted()) {
                        channelCount++;
                    }
                }
                packageChannels.put(r.pkg, Integer.valueOf(channelCount));
            }
        }
        return packageChannels;
    }

    public void onUserRemoved(int userId) {
        synchronized (this.mRecords) {
            for (int i = this.mRecords.size() - 1; i >= 0; i--) {
                if (UserHandle.getUserId(((Record) this.mRecords.valueAt(i)).uid) == userId) {
                    this.mRecords.removeAt(i);
                }
            }
        }
    }

    protected void onLocaleChanged(Context context, int userId) {
        synchronized (this.mRecords) {
            int N = this.mRecords.size();
            for (int i = 0; i < N; i++) {
                Record record = (Record) this.mRecords.valueAt(i);
                if (UserHandle.getUserId(record.uid) == userId && record.channels.containsKey("miscellaneous")) {
                    ((NotificationChannel) record.channels.get("miscellaneous")).setName(context.getResources().getString(17039921));
                }
            }
        }
    }

    public void onPackagesChanged(boolean removingPackage, int changeUserId, String[] pkgList, int[] uidList) {
        if (pkgList != null && pkgList.length != 0) {
            boolean updated = false;
            int i = 0;
            int size;
            String pkg;
            if (removingPackage) {
                size = Math.min(pkgList.length, uidList.length);
                while (i < size) {
                    pkg = pkgList[i];
                    int uid = uidList[i];
                    synchronized (this.mRecords) {
                        this.mRecords.remove(recordKey(pkg, uid));
                    }
                    this.mRestoredWithoutUids.remove(pkg);
                    updated = true;
                    i++;
                }
            } else {
                size = pkgList.length;
                while (i < size) {
                    pkg = pkgList[i];
                    Record r = (Record) this.mRestoredWithoutUids.get(pkg);
                    if (r != null) {
                        try {
                            r.uid = this.mPm.getPackageUidAsUser(r.pkg, changeUserId);
                            this.mRestoredWithoutUids.remove(pkg);
                            synchronized (this.mRecords) {
                                this.mRecords.put(recordKey(r.pkg, r.uid), r);
                            }
                            updated = true;
                        } catch (NameNotFoundException e) {
                        }
                    }
                    try {
                        Record fullRecord = getRecord(pkg, this.mPm.getPackageUidAsUser(pkg, changeUserId));
                        if (fullRecord != null) {
                            createDefaultChannelIfNeeded(fullRecord);
                            deleteDefaultChannelIfNeeded(fullRecord);
                        }
                    } catch (NameNotFoundException e2) {
                    }
                    i++;
                }
            }
            if (updated) {
                updateConfig();
            }
        }
    }

    private LogMaker getChannelLog(NotificationChannel channel, String pkg) {
        return new LogMaker(856).setType(6).setPackageName(pkg).addTaggedData(857, channel.getId()).addTaggedData(858, Integer.valueOf(channel.getImportance()));
    }

    private LogMaker getChannelGroupLog(String groupId, String pkg) {
        return new LogMaker(859).setType(6).addTaggedData(860, groupId).setPackageName(pkg);
    }

    public void updateBadgingEnabled() {
        if (this.mBadgingEnabled == null) {
            this.mBadgingEnabled = new SparseBooleanArray();
        }
        boolean changed = false;
        for (int index = 0; index < this.mBadgingEnabled.size(); index++) {
            int userId = this.mBadgingEnabled.keyAt(index);
            boolean oldValue = this.mBadgingEnabled.get(userId);
            int i = 1;
            boolean newValue = Secure.getIntForUser(this.mContext.getContentResolver(), "notification_badging", 1, userId) != 0;
            this.mBadgingEnabled.put(userId, newValue);
            if (oldValue == newValue) {
                i = 0;
            }
            changed |= i;
        }
        if (changed) {
            updateConfig();
        }
    }

    public boolean badgingEnabled(UserHandle userHandle) {
        int userId = userHandle.getIdentifier();
        boolean z = false;
        if (userId == -1) {
            return false;
        }
        if (this.mBadgingEnabled.indexOfKey(userId) < 0) {
            SparseBooleanArray sparseBooleanArray = this.mBadgingEnabled;
            if (Secure.getIntForUser(this.mContext.getContentResolver(), "notification_badging", 1, userId) != 0) {
                z = true;
            }
            sparseBooleanArray.put(userId, z);
        }
        return this.mBadgingEnabled.get(userId, true);
    }

    public void setSysMgrCfgMap(ArrayList<NotificationSysMgrCfg> cfgList) {
        synchronized (this.mSysMgrCfgMap) {
            this.mSysMgrCfgMap.clear();
            if (cfgList == null) {
                Slog.w(TAG, "RankingHelper: setSysMgrCfgMap: get default channel cfg is null:");
                return;
            }
            int size = cfgList.size();
            for (int i = 0; i < size; i++) {
                NotificationSysMgrCfg cfg = (NotificationSysMgrCfg) cfgList.get(i);
                this.mSysMgrCfgMap.put(recordKey(cfg.smc_packageName, cfg.smc_userId), cfg);
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("RankingHelper: setSysMgrCfgMap: get default channel cfg size:");
            stringBuilder.append(this.mSysMgrCfgMap.size());
            Slog.d(str, stringBuilder.toString());
        }
    }
}
