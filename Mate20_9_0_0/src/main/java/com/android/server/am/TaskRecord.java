package com.android.server.am;

import android.app.ActivityManager;
import android.app.ActivityManager.TaskDescription;
import android.app.ActivityManager.TaskSnapshot;
import android.app.ActivityOptions;
import android.app.AppGlobals;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Debug;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.service.voice.IVoiceInteractionSession;
import android.util.HwPCUtils;
import android.util.Slog;
import android.util.proto.ProtoOutputStream;
import com.android.internal.annotations.VisibleForTesting;
import com.android.internal.app.IVoiceInteractor;
import com.android.internal.util.XmlUtils;
import com.android.server.HwServiceFactory;
import com.android.server.backup.internal.BackupHandler;
import com.android.server.os.HwBootFail;
import com.android.server.pm.DumpState;
import com.android.server.wm.AppWindowContainerController;
import com.android.server.wm.ConfigurationContainer;
import com.android.server.wm.TaskWindowContainerController;
import com.android.server.wm.TaskWindowContainerListener;
import com.android.server.wm.WindowManagerService;
import com.android.server.wm.WindowManagerService.H;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Objects;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class TaskRecord extends AbsTaskRecord implements TaskWindowContainerListener {
    private static final String ATTR_AFFINITY = "affinity";
    private static final String ATTR_ASKEDCOMPATMODE = "asked_compat_mode";
    private static final String ATTR_AUTOREMOVERECENTS = "auto_remove_recents";
    private static final String ATTR_CALLING_PACKAGE = "calling_package";
    private static final String ATTR_CALLING_UID = "calling_uid";
    private static final String ATTR_EFFECTIVE_UID = "effective_uid";
    private static final String ATTR_LASTDESCRIPTION = "last_description";
    private static final String ATTR_LASTTIMEMOVED = "last_time_moved";
    private static final String ATTR_MIN_HEIGHT = "min_height";
    private static final String ATTR_MIN_WIDTH = "min_width";
    private static final String ATTR_NEVERRELINQUISH = "never_relinquish_identity";
    private static final String ATTR_NEXT_AFFILIATION = "next_affiliation";
    private static final String ATTR_NON_FULLSCREEN_BOUNDS = "non_fullscreen_bounds";
    private static final String ATTR_ORIGACTIVITY = "orig_activity";
    private static final String ATTR_PERSIST_TASK_VERSION = "persist_task_version";
    private static final String ATTR_PREV_AFFILIATION = "prev_affiliation";
    private static final String ATTR_REALACTIVITY = "real_activity";
    private static final String ATTR_REALACTIVITY_SUSPENDED = "real_activity_suspended";
    private static final String ATTR_RESIZE_MODE = "resize_mode";
    private static final String ATTR_ROOTHASRESET = "root_has_reset";
    private static final String ATTR_ROOT_AFFINITY = "root_affinity";
    private static final String ATTR_SUPPORTS_PICTURE_IN_PICTURE = "supports_picture_in_picture";
    private static final String ATTR_TASKID = "task_id";
    @Deprecated
    private static final String ATTR_TASKTYPE = "task_type";
    private static final String ATTR_TASK_AFFILIATION = "task_affiliation";
    private static final String ATTR_TASK_AFFILIATION_COLOR = "task_affiliation_color";
    private static final String ATTR_USERID = "user_id";
    private static final String ATTR_USER_SETUP_COMPLETE = "user_setup_complete";
    private static final int INVALID_MIN_SIZE = -1;
    static final int INVALID_TASK_ID = -1;
    private static final int LAND_ROTATE_VALUE = 270;
    static final int LOCK_TASK_AUTH_DONT_LOCK = 0;
    static final int LOCK_TASK_AUTH_LAUNCHABLE = 2;
    static final int LOCK_TASK_AUTH_LAUNCHABLE_PRIV = 4;
    static final int LOCK_TASK_AUTH_PINNABLE = 1;
    static final int LOCK_TASK_AUTH_WHITELISTED = 3;
    private static final int PERSIST_TASK_VERSION = 1;
    static final int REPARENT_KEEP_STACK_AT_FRONT = 1;
    static final int REPARENT_LEAVE_STACK_IN_PLACE = 2;
    static final int REPARENT_MOVE_STACK_TO_FRONT = 0;
    private static final String TAG = "ActivityManager";
    private static final String TAG_ACTIVITY = "activity";
    private static final String TAG_ADD_REMOVE = "ActivityManager";
    private static final String TAG_AFFINITYINTENT = "affinity_intent";
    private static final String TAG_INTENT = "intent";
    private static final String TAG_LOCKTASK = "ActivityManager";
    private static final String TAG_RECENTS = "ActivityManager";
    private static final String TAG_TASKS = "ActivityManager";
    private static TaskRecordFactory sTaskRecordFactory;
    String affinity;
    Intent affinityIntent;
    boolean askedCompatMode;
    boolean autoRemoveRecents;
    int effectiveUid;
    boolean hasBeenVisible;
    boolean inRecents;
    Intent intent;
    boolean isAvailable;
    boolean isLaunching;
    boolean isPersistable = false;
    long lastActiveTime;
    CharSequence lastDescription;
    TaskDescription lastTaskDescription = new TaskDescription();
    final ArrayList<ActivityRecord> mActivities;
    int mAffiliatedTaskColor;
    int mAffiliatedTaskId;
    String mCallingPackage;
    int mCallingUid;
    int mDefaultMinSize;
    Rect mLastNonFullscreenBounds = null;
    long mLastTimeMoved = System.currentTimeMillis();
    int mLayerRank = -1;
    int mLockTaskAuth = 1;
    int mLockTaskUid = -1;
    int mMinHeight;
    int mMinWidth;
    private boolean mNeverRelinquishIdentity = true;
    TaskRecord mNextAffiliate;
    int mNextAffiliateTaskId = -1;
    TaskRecord mPrevAffiliate;
    int mPrevAffiliateTaskId = -1;
    int mResizeMode;
    private boolean mReuseTask = false;
    protected ActivityInfo mRootActivityInfo;
    private ProcessRecord mRootProcess;
    final ActivityManagerService mService;
    protected ActivityStack mStack;
    private boolean mSupportsPictureInPicture;
    private Configuration mTmpConfig = new Configuration();
    private final Rect mTmpNonDecorBounds = new Rect();
    protected final Rect mTmpRect = new Rect();
    private final Rect mTmpStableBounds = new Rect();
    boolean mUserSetupComplete;
    private TaskWindowContainerController mWindowContainerController;
    int maxRecents;
    int numFullscreen;
    ComponentName origActivity;
    ComponentName realActivity;
    boolean realActivitySuspended;
    String rootAffinity;
    boolean rootWasReset;
    String stringName;
    final int taskId;
    int userId;
    final IVoiceInteractor voiceInteractor;
    final IVoiceInteractionSession voiceSession;

    @Retention(RetentionPolicy.SOURCE)
    @interface ReparentMoveStackMode {
    }

    static class TaskActivitiesReport {
        ActivityRecord base;
        int numActivities;
        int numRunning;
        ActivityRecord top;

        TaskActivitiesReport() {
        }

        void reset() {
            this.numActivities = 0;
            this.numRunning = 0;
            this.base = null;
            this.top = null;
        }
    }

    static class TaskRecordFactory {
        TaskRecordFactory() {
        }

        TaskRecord create(ActivityManagerService service, int taskId, ActivityInfo info, Intent intent, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor) {
            return HwServiceFactory.createTaskRecord(service, taskId, info, intent, voiceSession, voiceInteractor);
        }

        TaskRecord create(ActivityManagerService service, int taskId, ActivityInfo info, Intent intent, TaskDescription taskDescription) {
            return HwServiceFactory.createTaskRecord(service, taskId, info, intent, taskDescription);
        }

        TaskRecord create(ActivityManagerService service, int taskId, Intent intent, Intent affinityIntent, String affinity, String rootAffinity, ComponentName realActivity, ComponentName origActivity, boolean rootWasReset, boolean autoRemoveRecents, boolean askedCompatMode, int userId, int effectiveUid, String lastDescription, ArrayList<ActivityRecord> activities, long lastTimeMoved, boolean neverRelinquishIdentity, TaskDescription lastTaskDescription, int taskAffiliation, int prevTaskId, int nextTaskId, int taskAffiliationColor, int callingUid, String callingPackage, int resizeMode, boolean supportsPictureInPicture, boolean realActivitySuspended, boolean userSetupComplete, int minWidth, int minHeight) {
            return HwServiceFactory.createTaskRecord(service, taskId, intent, affinityIntent, affinity, rootAffinity, realActivity, origActivity, rootWasReset, autoRemoveRecents, askedCompatMode, userId, effectiveUid, lastDescription, activities, lastTimeMoved, neverRelinquishIdentity, lastTaskDescription, taskAffiliation, prevTaskId, nextTaskId, taskAffiliationColor, callingUid, callingPackage, resizeMode, supportsPictureInPicture, realActivitySuspended, userSetupComplete, minWidth, minHeight);
        }

        /* JADX WARNING: Removed duplicated region for block: B:185:0x0405 A:{LOOP_END, LOOP:2: B:184:0x0403->B:185:0x0405} */
        /* JADX WARNING: Removed duplicated region for block: B:188:0x0415  */
        /* JADX WARNING: Removed duplicated region for block: B:172:0x038b  */
        /* JADX WARNING: Removed duplicated region for block: B:159:0x033c  */
        /* JADX WARNING: Removed duplicated region for block: B:179:0x0397  */
        /* JADX WARNING: Removed duplicated region for block: B:175:0x0390 A:{SKIP} */
        /* JADX WARNING: Removed duplicated region for block: B:185:0x0405 A:{LOOP_END, LOOP:2: B:184:0x0403->B:185:0x0405} */
        /* JADX WARNING: Removed duplicated region for block: B:188:0x0415  */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        TaskRecord restoreFromXml(XmlPullParser in, ActivityStackSupervisor stackSupervisor) throws IOException, XmlPullParserException {
            XmlPullParser xmlPullParser = in;
            ArrayList<ActivityRecord> activities = new ArrayList();
            String rootAffinity = null;
            int effectiveUid = -1;
            Intent affinityIntent = null;
            int outerDepth = in.getDepth();
            Intent intent = null;
            TaskDescription taskDescription = new TaskDescription();
            boolean supportsPictureInPicture = false;
            Object obj = 1;
            int attrNdx = in.getAttributeCount() - 1;
            int taskAffiliation = -1;
            ComponentName realActivity = null;
            boolean realActivitySuspended = false;
            ComponentName origActivity = null;
            String affinity = null;
            boolean hasRootAffinity = false;
            boolean rootHasReset = false;
            boolean autoRemoveRecents = false;
            boolean askedCompatMode = false;
            int taskType = 0;
            int userId = 0;
            String lastDescription = null;
            long lastTimeOnTop = 0;
            int taskId = -1;
            int taskAffiliationColor = 0;
            int prevTaskId = -1;
            int nextTaskId = -1;
            int callingUid = -1;
            String callingPackage = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
            int resizeMode = 4;
            Rect lastNonFullscreenBounds = null;
            int minWidth = -1;
            int minHeight = -1;
            int persistTaskVersion = 0;
            boolean userSetupComplete = true;
            boolean neverRelinquishIdentity = true;
            while (true) {
                int attrNdx2 = attrNdx;
                TaskDescription taskDescription2;
                String str;
                if (attrNdx2 >= 0) {
                    Object obj2;
                    String attrName = xmlPullParser.getAttributeName(attrNdx2);
                    String attrValue = xmlPullParser.getAttributeValue(attrNdx2);
                    switch (attrName.hashCode()) {
                        case -2134816935:
                            if (attrName.equals(TaskRecord.ATTR_ASKEDCOMPATMODE)) {
                                obj2 = 8;
                                break;
                            }
                        case -1556983798:
                            if (attrName.equals(TaskRecord.ATTR_LASTTIMEMOVED)) {
                                obj2 = 14;
                                break;
                            }
                        case -1537240555:
                            if (attrName.equals(TaskRecord.ATTR_TASKID)) {
                                obj2 = null;
                                break;
                            }
                        case -1494902876:
                            if (attrName.equals(TaskRecord.ATTR_NEXT_AFFILIATION)) {
                                obj2 = 18;
                                break;
                            }
                        case -1292777190:
                            if (attrName.equals(TaskRecord.ATTR_TASK_AFFILIATION_COLOR)) {
                                obj2 = 19;
                                break;
                            }
                        case -1138503444:
                            if (attrName.equals(TaskRecord.ATTR_REALACTIVITY_SUSPENDED)) {
                                obj2 = 2;
                                break;
                            }
                        case -1124927690:
                            if (attrName.equals(TaskRecord.ATTR_TASK_AFFILIATION)) {
                                obj2 = 16;
                                break;
                            }
                        case -974080081:
                            if (attrName.equals(TaskRecord.ATTR_USER_SETUP_COMPLETE)) {
                                obj2 = 10;
                                break;
                            }
                        case -929566280:
                            if (attrName.equals(TaskRecord.ATTR_EFFECTIVE_UID)) {
                                obj2 = 11;
                                break;
                            }
                        case -865458610:
                            if (attrName.equals(TaskRecord.ATTR_RESIZE_MODE)) {
                                obj2 = 22;
                                break;
                            }
                        case -826243148:
                            if (attrName.equals(TaskRecord.ATTR_MIN_HEIGHT)) {
                                obj2 = 26;
                                break;
                            }
                        case -707249465:
                            if (attrName.equals(TaskRecord.ATTR_NON_FULLSCREEN_BOUNDS)) {
                                obj2 = 24;
                                break;
                            }
                        case -705269939:
                            if (attrName.equals(TaskRecord.ATTR_ORIGACTIVITY)) {
                                obj2 = 3;
                                break;
                            }
                        case -502399667:
                            if (attrName.equals(TaskRecord.ATTR_AUTOREMOVERECENTS)) {
                                obj2 = 7;
                                break;
                            }
                        case -360792224:
                            if (attrName.equals(TaskRecord.ATTR_SUPPORTS_PICTURE_IN_PICTURE)) {
                                obj2 = 23;
                                break;
                            }
                        case -162744347:
                            if (attrName.equals(TaskRecord.ATTR_ROOT_AFFINITY)) {
                                obj2 = 5;
                                break;
                            }
                        case -147132913:
                            if (attrName.equals(TaskRecord.ATTR_USERID)) {
                                obj2 = 9;
                                break;
                            }
                        case -132216235:
                            if (attrName.equals(TaskRecord.ATTR_CALLING_UID)) {
                                obj2 = 20;
                                break;
                            }
                        case 180927924:
                            if (attrName.equals(TaskRecord.ATTR_TASKTYPE)) {
                                obj2 = 12;
                                break;
                            }
                        case 331206372:
                            if (attrName.equals(TaskRecord.ATTR_PREV_AFFILIATION)) {
                                obj2 = 17;
                                break;
                            }
                        case 541503897:
                            if (attrName.equals(TaskRecord.ATTR_MIN_WIDTH)) {
                                obj2 = 25;
                                break;
                            }
                        case 605497640:
                            if (attrName.equals(TaskRecord.ATTR_AFFINITY)) {
                                obj2 = 4;
                                break;
                            }
                        case 869221331:
                            if (attrName.equals(TaskRecord.ATTR_LASTDESCRIPTION)) {
                                obj2 = 13;
                                break;
                            }
                        case 1007873193:
                            if (attrName.equals(TaskRecord.ATTR_PERSIST_TASK_VERSION)) {
                                obj2 = 27;
                                break;
                            }
                        case 1081438155:
                            if (attrName.equals(TaskRecord.ATTR_CALLING_PACKAGE)) {
                                obj2 = 21;
                                break;
                            }
                        case 1457608782:
                            if (attrName.equals(TaskRecord.ATTR_NEVERRELINQUISH)) {
                                obj2 = 15;
                                break;
                            }
                        case 1539554448:
                            if (attrName.equals(TaskRecord.ATTR_REALACTIVITY)) {
                                obj2 = obj;
                                break;
                            }
                        case 2023391309:
                            if (attrName.equals(TaskRecord.ATTR_ROOTHASRESET)) {
                                obj2 = 6;
                                break;
                            }
                        default:
                            obj2 = -1;
                            break;
                    }
                    switch (obj2) {
                        case null:
                            if (taskId == -1) {
                                taskId = Integer.parseInt(attrValue);
                                break;
                            }
                            break;
                        case 1:
                            realActivity = ComponentName.unflattenFromString(attrValue);
                            break;
                        case 2:
                            realActivitySuspended = Boolean.valueOf(attrValue).booleanValue();
                            break;
                        case 3:
                            origActivity = ComponentName.unflattenFromString(attrValue);
                            break;
                        case 4:
                            affinity = attrValue;
                            break;
                        case 5:
                            rootAffinity = attrValue;
                            hasRootAffinity = true;
                            break;
                        case 6:
                            rootHasReset = Boolean.parseBoolean(attrValue);
                            break;
                        case 7:
                            autoRemoveRecents = Boolean.parseBoolean(attrValue);
                            break;
                        case 8:
                            askedCompatMode = Boolean.parseBoolean(attrValue);
                            break;
                        case 9:
                            userId = Integer.parseInt(attrValue);
                            break;
                        case 10:
                            userSetupComplete = Boolean.parseBoolean(attrValue);
                            break;
                        case 11:
                            effectiveUid = Integer.parseInt(attrValue);
                            break;
                        case 12:
                            taskType = Integer.parseInt(attrValue);
                            break;
                        case 13:
                            lastDescription = attrValue;
                            break;
                        case 14:
                            lastTimeOnTop = Long.parseLong(attrValue);
                            break;
                        case 15:
                            neverRelinquishIdentity = Boolean.parseBoolean(attrValue);
                            break;
                        case 16:
                            taskAffiliation = Integer.parseInt(attrValue);
                            break;
                        case 17:
                            prevTaskId = Integer.parseInt(attrValue);
                            break;
                        case 18:
                            nextTaskId = Integer.parseInt(attrValue);
                            break;
                        case H.REPORT_WINDOWS_CHANGE /*19*/:
                            taskAffiliationColor = Integer.parseInt(attrValue);
                            break;
                        case 20:
                            callingUid = Integer.parseInt(attrValue);
                            break;
                        case BackupHandler.MSG_OP_COMPLETE /*21*/:
                            callingPackage = attrValue;
                            break;
                        case H.REPORT_HARD_KEYBOARD_STATUS_CHANGE /*22*/:
                            resizeMode = Integer.parseInt(attrValue);
                            break;
                        case H.BOOT_TIMEOUT /*23*/:
                            supportsPictureInPicture = Boolean.parseBoolean(attrValue);
                            break;
                        case 24:
                            lastNonFullscreenBounds = Rect.unflattenFromString(attrValue);
                            break;
                        case H.SHOW_STRICT_MODE_VIOLATION /*25*/:
                            minWidth = Integer.parseInt(attrValue);
                            break;
                        case H.DO_ANIMATION_CALLBACK /*26*/:
                            minHeight = Integer.parseInt(attrValue);
                            break;
                        case 27:
                            persistTaskVersion = Integer.parseInt(attrValue);
                            break;
                        default:
                            TaskDescription taskDescription3;
                            if (attrName.startsWith("task_description_")) {
                                taskDescription3 = taskDescription;
                                taskDescription3.restoreFromXml(attrName, attrValue);
                                taskDescription2 = taskDescription3;
                                break;
                            }
                            taskDescription3 = taskDescription;
                            str = ActivityManagerService.TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            taskDescription2 = taskDescription3;
                            stringBuilder.append("TaskRecord: Unknown attribute=");
                            stringBuilder.append(attrName);
                            Slog.w(str, stringBuilder.toString());
                            continue;
                    }
                    taskDescription2 = taskDescription;
                    attrNdx = attrNdx2 - 1;
                    taskDescription = taskDescription2;
                    obj = 1;
                } else {
                    String name;
                    String rootAffinity2;
                    int resizeMode2;
                    boolean supportsPictureInPicture2;
                    Rect lastNonFullscreenBounds2;
                    int i;
                    TaskRecord task;
                    int activityNdx;
                    taskDescription2 = taskDescription;
                    while (true) {
                        attrNdx2 = in.next();
                        int event = attrNdx2;
                        if (attrNdx2 == 1 || (event == 3 && in.getDepth() < outerDepth)) {
                        } else if (event == 2) {
                            name = in.getName();
                            if (TaskRecord.TAG_AFFINITYINTENT.equals(name)) {
                                affinityIntent = Intent.restoreFromXml(in);
                            } else if ("intent".equals(name)) {
                                intent = Intent.restoreFromXml(in);
                            } else if (TaskRecord.TAG_ACTIVITY.equals(name)) {
                                ActivityRecord activity = ActivityRecord.restoreFromXml(in, stackSupervisor);
                                if (activity != null) {
                                    activities.add(activity);
                                }
                            } else {
                                handleUnknownTag(name, xmlPullParser);
                            }
                        }
                    }
                    if (!hasRootAffinity) {
                        name = affinity;
                    } else if ("@".equals(rootAffinity)) {
                        name = null;
                    } else {
                        rootAffinity2 = rootAffinity;
                        if (effectiveUid > 0) {
                            Intent checkIntent = intent != null ? intent : affinityIntent;
                            int effectiveUid2 = 0;
                            if (checkIntent != null) {
                                try {
                                    ApplicationInfo ai = AppGlobals.getPackageManager().getApplicationInfo(checkIntent.getComponent().getPackageName(), 8704, userId);
                                    if (ai != null) {
                                        effectiveUid2 = ai.uid;
                                    }
                                } catch (RemoteException e) {
                                }
                            }
                            name = ActivityManagerService.TAG;
                            StringBuilder stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("Updating task #");
                            stringBuilder2.append(taskId);
                            stringBuilder2.append(" for ");
                            stringBuilder2.append(checkIntent);
                            stringBuilder2.append(": effectiveUid=");
                            stringBuilder2.append(effectiveUid2);
                            Slog.w(name, stringBuilder2.toString());
                            attrNdx2 = effectiveUid2;
                        } else {
                            attrNdx2 = effectiveUid;
                        }
                        if (persistTaskVersion >= 1) {
                            if (taskType == 1 && resizeMode == 2) {
                                resizeMode = 1;
                            }
                        } else if (resizeMode == 3) {
                            resizeMode2 = 2;
                            supportsPictureInPicture2 = true;
                            lastNonFullscreenBounds2 = lastNonFullscreenBounds;
                            i = 1;
                            task = create(stackSupervisor.mService, taskId, intent, affinityIntent, affinity, rootAffinity2, realActivity, origActivity, rootHasReset, autoRemoveRecents, askedCompatMode, userId, attrNdx2, lastDescription, activities, lastTimeOnTop, neverRelinquishIdentity, taskDescription2, taskAffiliation, prevTaskId, nextTaskId, taskAffiliationColor, callingUid, callingPackage, resizeMode2, supportsPictureInPicture2, realActivitySuspended, userSetupComplete, minWidth, minHeight);
                            task.mLastNonFullscreenBounds = lastNonFullscreenBounds2;
                            task.setBounds(lastNonFullscreenBounds2);
                            for (activityNdx = activities.size() - 1; activityNdx >= 0; activityNdx--) {
                                ((ActivityRecord) activities.get(activityNdx)).setTask(task);
                            }
                            if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                                str = ActivityManagerService.TAG;
                                StringBuilder stringBuilder3 = new StringBuilder();
                                stringBuilder3.append("Restored task=");
                                stringBuilder3.append(task);
                                Slog.d(str, stringBuilder3.toString());
                            }
                            return task;
                        }
                        resizeMode2 = resizeMode;
                        supportsPictureInPicture2 = supportsPictureInPicture;
                        lastNonFullscreenBounds2 = lastNonFullscreenBounds;
                        i = 1;
                        task = create(stackSupervisor.mService, taskId, intent, affinityIntent, affinity, rootAffinity2, realActivity, origActivity, rootHasReset, autoRemoveRecents, askedCompatMode, userId, attrNdx2, lastDescription, activities, lastTimeOnTop, neverRelinquishIdentity, taskDescription2, taskAffiliation, prevTaskId, nextTaskId, taskAffiliationColor, callingUid, callingPackage, resizeMode2, supportsPictureInPicture2, realActivitySuspended, userSetupComplete, minWidth, minHeight);
                        task.mLastNonFullscreenBounds = lastNonFullscreenBounds2;
                        task.setBounds(lastNonFullscreenBounds2);
                        while (activityNdx >= 0) {
                        }
                        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                        }
                        return task;
                    }
                    rootAffinity2 = name;
                    if (effectiveUid > 0) {
                    }
                    if (persistTaskVersion >= 1) {
                    }
                    resizeMode2 = resizeMode;
                    supportsPictureInPicture2 = supportsPictureInPicture;
                    lastNonFullscreenBounds2 = lastNonFullscreenBounds;
                    i = 1;
                    task = create(stackSupervisor.mService, taskId, intent, affinityIntent, affinity, rootAffinity2, realActivity, origActivity, rootHasReset, autoRemoveRecents, askedCompatMode, userId, attrNdx2, lastDescription, activities, lastTimeOnTop, neverRelinquishIdentity, taskDescription2, taskAffiliation, prevTaskId, nextTaskId, taskAffiliationColor, callingUid, callingPackage, resizeMode2, supportsPictureInPicture2, realActivitySuspended, userSetupComplete, minWidth, minHeight);
                    task.mLastNonFullscreenBounds = lastNonFullscreenBounds2;
                    task.setBounds(lastNonFullscreenBounds2);
                    while (activityNdx >= 0) {
                    }
                    if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
                    }
                    return task;
                }
            }
        }

        void handleUnknownTag(String name, XmlPullParser in) throws IOException, XmlPullParserException {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("restoreTask: Unexpected name=");
            stringBuilder.append(name);
            Slog.e(str, stringBuilder.toString());
            XmlUtils.skipCurrentTag(in);
        }
    }

    public TaskRecord(ActivityManagerService service, int _taskId, ActivityInfo info, Intent _intent, IVoiceInteractionSession _voiceSession, IVoiceInteractor _voiceInteractor) {
        this.mService = service;
        this.userId = UserHandle.getUserId(info.applicationInfo.uid);
        this.taskId = _taskId;
        this.lastActiveTime = SystemClock.elapsedRealtime();
        this.mAffiliatedTaskId = _taskId;
        this.voiceSession = _voiceSession;
        this.voiceInteractor = _voiceInteractor;
        this.isAvailable = true;
        this.mActivities = new ArrayList();
        this.mCallingUid = info.applicationInfo.uid;
        this.mCallingPackage = info.packageName;
        setIntent(_intent, info);
        setMinDimensions(info);
        touchActiveTime();
        this.mService.mTaskChangeNotificationController.notifyTaskCreated(_taskId, this.realActivity);
    }

    public TaskRecord(ActivityManagerService service, int _taskId, ActivityInfo info, Intent _intent, TaskDescription _taskDescription) {
        this.mService = service;
        this.userId = UserHandle.getUserId(info.applicationInfo.uid);
        this.taskId = _taskId;
        this.lastActiveTime = SystemClock.elapsedRealtime();
        this.mAffiliatedTaskId = _taskId;
        this.voiceSession = null;
        this.voiceInteractor = null;
        this.isAvailable = true;
        this.mActivities = new ArrayList();
        this.mCallingUid = info.applicationInfo.uid;
        this.mCallingPackage = info.packageName;
        setIntent(_intent, info);
        setMinDimensions(info);
        this.isPersistable = true;
        this.maxRecents = Math.min(Math.max(info.maxRecents, 1), ActivityManager.getMaxAppRecentsLimitStatic());
        this.lastTaskDescription = _taskDescription;
        touchActiveTime();
        this.mService.mTaskChangeNotificationController.notifyTaskCreated(_taskId, this.realActivity);
    }

    public TaskRecord(ActivityManagerService service, int _taskId, Intent _intent, Intent _affinityIntent, String _affinity, String _rootAffinity, ComponentName _realActivity, ComponentName _origActivity, boolean _rootWasReset, boolean _autoRemoveRecents, boolean _askedCompatMode, int _userId, int _effectiveUid, String _lastDescription, ArrayList<ActivityRecord> activities, long lastTimeMoved, boolean neverRelinquishIdentity, TaskDescription _lastTaskDescription, int taskAffiliation, int prevTaskId, int nextTaskId, int taskAffiliationColor, int callingUid, String callingPackage, int resizeMode, boolean supportsPictureInPicture, boolean _realActivitySuspended, boolean userSetupComplete, int minWidth, int minHeight) {
        int i = _taskId;
        this.mService = service;
        this.taskId = i;
        this.intent = _intent;
        this.affinityIntent = _affinityIntent;
        this.affinity = _affinity;
        this.rootAffinity = _rootAffinity;
        this.voiceSession = null;
        this.voiceInteractor = null;
        this.realActivity = _realActivity;
        this.realActivitySuspended = _realActivitySuspended;
        this.origActivity = _origActivity;
        this.rootWasReset = _rootWasReset;
        this.isAvailable = true;
        this.autoRemoveRecents = _autoRemoveRecents;
        this.askedCompatMode = _askedCompatMode;
        this.userId = _userId;
        this.mUserSetupComplete = userSetupComplete;
        this.effectiveUid = _effectiveUid;
        this.lastActiveTime = SystemClock.elapsedRealtime();
        this.lastDescription = _lastDescription;
        this.mActivities = activities;
        this.mLastTimeMoved = lastTimeMoved;
        this.mNeverRelinquishIdentity = neverRelinquishIdentity;
        this.lastTaskDescription = _lastTaskDescription;
        this.mAffiliatedTaskId = taskAffiliation;
        this.mAffiliatedTaskColor = taskAffiliationColor;
        this.mPrevAffiliateTaskId = prevTaskId;
        this.mNextAffiliateTaskId = nextTaskId;
        this.mCallingUid = callingUid;
        this.mCallingPackage = callingPackage;
        this.mResizeMode = resizeMode;
        this.mSupportsPictureInPicture = supportsPictureInPicture;
        this.mMinWidth = minWidth;
        this.mMinHeight = minHeight;
        this.mService.mTaskChangeNotificationController.notifyTaskCreated(i, this.realActivity);
    }

    TaskWindowContainerController getWindowContainerController() {
        return this.mWindowContainerController;
    }

    void createWindowContainer(boolean onTop, boolean showForAllUsers) {
        if (this.mWindowContainerController == null) {
            setWindowContainerController(new TaskWindowContainerController(this.taskId, this, getStack().getWindowContainerController(), this.userId, updateOverrideConfigurationFromLaunchBounds(), this.mResizeMode, this.mSupportsPictureInPicture, onTop, showForAllUsers, this.lastTaskDescription));
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Window container=");
        stringBuilder.append(this.mWindowContainerController);
        stringBuilder.append(" already created for task=");
        stringBuilder.append(this);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    @VisibleForTesting
    protected void setWindowContainerController(TaskWindowContainerController controller) {
        if (this.mWindowContainerController == null) {
            this.mWindowContainerController = controller;
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Window container=");
        stringBuilder.append(this.mWindowContainerController);
        stringBuilder.append(" already created for task=");
        stringBuilder.append(this);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    void removeWindowContainer() {
        this.mService.getLockTaskController().clearLockedTask(this);
        this.mWindowContainerController.removeContainer();
        if (!getWindowConfiguration().persistTaskBounds()) {
            updateOverrideConfiguration(null);
        }
        this.mService.mTaskChangeNotificationController.notifyTaskRemoved(this.taskId);
        this.mWindowContainerController = null;
    }

    public void onSnapshotChanged(TaskSnapshot snapshot) {
        this.mService.mTaskChangeNotificationController.notifyTaskSnapshotChanged(this.taskId, snapshot);
    }

    void setResizeMode(int resizeMode) {
        if (this.mResizeMode != resizeMode) {
            this.mResizeMode = resizeMode;
            if (this.mWindowContainerController != null) {
                this.mWindowContainerController.setResizeable(resizeMode);
            }
            this.mService.mStackSupervisor.ensureActivitiesVisibleLocked(null, 0, false);
            this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
        }
    }

    void setTaskDockedResizing(boolean resizing) {
        if (this.mWindowContainerController != null) {
            this.mWindowContainerController.setTaskDockedResizing(resizing);
        }
    }

    public void requestResize(Rect bounds, int resizeMode) {
        this.mService.resizeTask(this.taskId, bounds, resizeMode);
    }

    boolean resize(Rect bounds, int resizeMode, boolean preserveWindow, boolean deferResume) {
        this.mService.mWindowManager.deferSurfaceLayout();
        boolean z = true;
        StringBuilder stringBuilder;
        if (isResizeable()) {
            boolean forced = (resizeMode & 2) != 0;
            try {
                if (equivalentOverrideBounds(bounds) && !forced) {
                    this.mService.mWindowManager.continueSurfaceLayout();
                    return true;
                } else if (this.mWindowContainerController == null) {
                    updateOverrideConfiguration(bounds);
                    if (!inFreeformWindowingMode()) {
                        this.mService.mStackSupervisor.restoreRecentTaskLocked(this, null, false);
                    }
                    this.mService.mWindowManager.continueSurfaceLayout();
                    return true;
                } else if (canResizeToBounds(bounds)) {
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("am.resizeTask_");
                    stringBuilder2.append(this.taskId);
                    Trace.traceBegin(64, stringBuilder2.toString());
                    boolean kept = true;
                    if (updateOverrideConfiguration(bounds)) {
                        ActivityRecord r = topRunningActivityLocked();
                        if (!(r == null || deferResume)) {
                            kept = r.ensureActivityConfiguration(0, preserveWindow);
                            this.mService.mStackSupervisor.ensureActivitiesVisibleLocked(r, 0, false);
                            if (!kept) {
                                this.mService.mStackSupervisor.resumeFocusedStackTopActivityLocked();
                            }
                        }
                    }
                    this.mWindowContainerController.resize(kept, forced);
                    Trace.traceEnd(64);
                    this.mService.mWindowManager.continueSurfaceLayout();
                    return kept;
                } else {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("resizeTask: Can not resize task=");
                    stringBuilder.append(this);
                    stringBuilder.append(" to bounds=");
                    stringBuilder.append(bounds);
                    stringBuilder.append(" resizeMode=");
                    stringBuilder.append(this.mResizeMode);
                    throw new IllegalArgumentException(stringBuilder.toString());
                }
            } finally {
                z = this.mService.mWindowManager;
                z.continueSurfaceLayout();
            }
        } else {
            String str = ActivityManagerService.TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("resizeTask: task ");
            stringBuilder.append(this);
            stringBuilder.append(" not resizeable.");
            Slog.w(str, stringBuilder.toString());
            return z;
        }
    }

    void resizeWindowContainer() {
        if (this.mWindowContainerController != null) {
            this.mWindowContainerController.resize(false, false);
        }
    }

    void getWindowContainerBounds(Rect bounds) {
        if (this.mWindowContainerController != null) {
            this.mWindowContainerController.getBounds(bounds);
        }
    }

    boolean reparent(ActivityStack preferredStack, boolean toTop, int moveStackMode, boolean animate, boolean deferResume, String reason) {
        return reparent(preferredStack, toTop ? HwBootFail.STAGE_BOOT_SUCCESS : 0, moveStackMode, animate, deferResume, true, reason);
    }

    boolean reparent(ActivityStack preferredStack, boolean toTop, int moveStackMode, boolean animate, boolean deferResume, boolean schedulePictureInPictureModeChange, String reason) {
        return reparent(preferredStack, toTop ? HwBootFail.STAGE_BOOT_SUCCESS : 0, moveStackMode, animate, deferResume, schedulePictureInPictureModeChange, reason);
    }

    boolean reparent(ActivityStack preferredStack, int position, int moveStackMode, boolean animate, boolean deferResume, String reason) {
        return reparent(preferredStack, position, moveStackMode, animate, deferResume, true, reason);
    }

    /* JADX WARNING: Removed duplicated region for block: B:82:0x00ee A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x00eb A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x00f9 A:{SYNTHETIC, Splitter:B:85:0x00f9} */
    /* JADX WARNING: Removed duplicated region for block: B:95:0x010f A:{SYNTHETIC, Splitter:B:95:0x010f} */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x012e  */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x011c  */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x014f  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0138 A:{SYNTHETIC, Splitter:B:105:0x0138} */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x015b  */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x0159  */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x0176 A:{Catch:{ all -> 0x01ff }} */
    /* JADX WARNING: Removed duplicated region for block: B:129:0x0175 A:{Catch:{ all -> 0x01ff }} */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x01d6  */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x01f0  */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x01e2  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x01fc  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00d5  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x00d3  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x00e5 A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00db A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x00eb A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x00ee A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x00f9 A:{SYNTHETIC, Splitter:B:85:0x00f9} */
    /* JADX WARNING: Removed duplicated region for block: B:95:0x010f A:{SYNTHETIC, Splitter:B:95:0x010f} */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x011c  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x012e  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0138 A:{SYNTHETIC, Splitter:B:105:0x0138} */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x014f  */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x0159  */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x015b  */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x0162 A:{Catch:{ all -> 0x01ff }} */
    /* JADX WARNING: Removed duplicated region for block: B:129:0x0175 A:{Catch:{ all -> 0x01ff }} */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x0176 A:{Catch:{ all -> 0x01ff }} */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x01d6  */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x01e2  */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x01f0  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x01fc  */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x00a7 A:{SYNTHETIC, Splitter:B:50:0x00a7} */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x00d3  */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00d5  */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00db A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x00e5 A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x00ee A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x00eb A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x00f9 A:{SYNTHETIC, Splitter:B:85:0x00f9} */
    /* JADX WARNING: Removed duplicated region for block: B:95:0x010f A:{SYNTHETIC, Splitter:B:95:0x010f} */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x012e  */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x011c  */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x014f  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0138 A:{SYNTHETIC, Splitter:B:105:0x0138} */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x015b  */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x0159  */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x0162 A:{Catch:{ all -> 0x01ff }} */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x0176 A:{Catch:{ all -> 0x01ff }} */
    /* JADX WARNING: Removed duplicated region for block: B:129:0x0175 A:{Catch:{ all -> 0x01ff }} */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x01d6  */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x01f0  */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x01e2  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x01fc  */
    /* JADX WARNING: Removed duplicated region for block: B:41:0x008f A:{SYNTHETIC, Splitter:B:41:0x008f} */
    /* JADX WARNING: Removed duplicated region for block: B:50:0x00a7 A:{SYNTHETIC, Splitter:B:50:0x00a7} */
    /* JADX WARNING: Removed duplicated region for block: B:67:0x00d5  */
    /* JADX WARNING: Removed duplicated region for block: B:66:0x00d3  */
    /* JADX WARNING: Removed duplicated region for block: B:77:0x00e5 A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:71:0x00db A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x00eb A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:82:0x00ee A:{Catch:{ all -> 0x0208 }} */
    /* JADX WARNING: Removed duplicated region for block: B:85:0x00f9 A:{SYNTHETIC, Splitter:B:85:0x00f9} */
    /* JADX WARNING: Removed duplicated region for block: B:95:0x010f A:{SYNTHETIC, Splitter:B:95:0x010f} */
    /* JADX WARNING: Removed duplicated region for block: B:99:0x011c  */
    /* JADX WARNING: Removed duplicated region for block: B:103:0x012e  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x0138 A:{SYNTHETIC, Splitter:B:105:0x0138} */
    /* JADX WARNING: Removed duplicated region for block: B:113:0x014f  */
    /* JADX WARNING: Removed duplicated region for block: B:118:0x0159  */
    /* JADX WARNING: Removed duplicated region for block: B:119:0x015b  */
    /* JADX WARNING: Removed duplicated region for block: B:123:0x0162 A:{Catch:{ all -> 0x01ff }} */
    /* JADX WARNING: Removed duplicated region for block: B:129:0x0175 A:{Catch:{ all -> 0x01ff }} */
    /* JADX WARNING: Removed duplicated region for block: B:130:0x0176 A:{Catch:{ all -> 0x01ff }} */
    /* JADX WARNING: Removed duplicated region for block: B:160:0x01d6  */
    /* JADX WARNING: Removed duplicated region for block: B:166:0x01e2  */
    /* JADX WARNING: Removed duplicated region for block: B:170:0x01f0  */
    /* JADX WARNING: Removed duplicated region for block: B:173:0x01fc  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    boolean reparent(ActivityStack preferredStack, int position, int moveStackMode, boolean animate, boolean deferResume, boolean schedulePictureInPictureModeChange, String reason) {
        ActivityRecord r;
        ConfigurationContainer topActivity;
        boolean toStackWindowingMode;
        Throwable th;
        ConfigurationContainer configurationContainer;
        ActivityStack activityStack;
        ActivityStack activityStack2 = preferredStack;
        int i = position;
        int i2 = moveStackMode;
        boolean z = animate;
        boolean z2 = deferResume;
        String str = reason;
        ActivityStackSupervisor supervisor = this.mService.mStackSupervisor;
        WindowManagerService windowManager = this.mService.mWindowManager;
        ActivityStack sourceStack = getStack();
        ActivityStack toStack = supervisor.getReparentTargetStack(this, activityStack2, i == HwBootFail.STAGE_BOOT_SUCCESS);
        if (toStack == sourceStack || !canBeLaunchedOnDisplay(toStack.mDisplayId)) {
            return false;
        }
        boolean toStackWindowingMode2 = toStack.getWindowingMode();
        ConfigurationContainer topActivity2 = getTopActivity();
        boolean z3 = topActivity2 != null && replaceWindowsOnTaskMove(getWindowingMode(), toStackWindowingMode2);
        boolean mightReplaceWindow = z3;
        if (mightReplaceWindow) {
            windowManager.setWillReplaceWindow(topActivity2.appToken, z);
        }
        windowManager.deferSurfaceLayout();
        boolean kept = true;
        int i3;
        ActivityStack activityStack3;
        boolean z4;
        try {
            boolean wasFocused;
            ActivityRecord activityRecord;
            ActivityRecord topActivity3;
            int i4;
            int toStackWindowingMode3;
            boolean wasFront;
            int i5;
            boolean moveStackToFront;
            boolean position2;
            Rect configBounds;
            Rect overrideBounds;
            r = topRunningActivityLocked();
            if (r != null) {
                try {
                    if (supervisor.isFocusedStack(sourceStack) && topRunningActivityLocked() == r) {
                        z3 = true;
                        wasFocused = z3;
                        activityRecord = (r == null && sourceStack.getResumedActivity() == r) ? 1 : null;
                        topActivity = topActivity2;
                        topActivity3 = activityRecord;
                        if (r != null) {
                            try {
                                if (sourceStack.mPausingActivity == r) {
                                    i4 = 1;
                                    toStackWindowingMode = toStackWindowingMode2;
                                    toStackWindowingMode3 = i4;
                                    if (r != null) {
                                        try {
                                            if (sourceStack.isTopStackOnDisplay() && sourceStack.topRunningActivityLocked() == r) {
                                                z3 = true;
                                                wasFront = z3;
                                                i = toStack.getAdjustedPositionForTask(this, i, null);
                                                this.mWindowContainerController.reparent(toStack.getWindowContainerController(), i, i2 != 0);
                                                if (i2 == 0) {
                                                    i5 = 1;
                                                    if (i2 == 1) {
                                                        if (!wasFocused) {
                                                            if (wasFront) {
                                                            }
                                                        }
                                                    }
                                                    z3 = false;
                                                    moveStackToFront = z3;
                                                    sourceStack.removeTask(this, str, moveStackToFront ? 2 : i5);
                                                    toStack.addTask(this, i, false, str);
                                                    if (schedulePictureInPictureModeChange) {
                                                        try {
                                                            supervisor.scheduleUpdatePictureInPictureModeIfNeeded(this, sourceStack);
                                                        } catch (Throwable th2) {
                                                            th = th2;
                                                        }
                                                    }
                                                    if (this.voiceSession != null) {
                                                        try {
                                                            this.voiceSession.taskStarted(this.intent, this.taskId);
                                                        } catch (RemoteException e) {
                                                        }
                                                    }
                                                    if (r != null) {
                                                        activityStack2 = toStack;
                                                        i3 = i;
                                                        position2 = true;
                                                        activityStack3 = sourceStack;
                                                        try {
                                                            toStack.moveToFrontAndResumeStateIfNeeded(r, moveStackToFront, topActivity3, toStackWindowingMode3, str);
                                                        } catch (Throwable th3) {
                                                            th = th3;
                                                        }
                                                    } else {
                                                        activityStack2 = toStack;
                                                        ActivityRecord activityRecord2 = r;
                                                        activityStack3 = sourceStack;
                                                        position2 = true;
                                                    }
                                                    if (z) {
                                                        toStack = topActivity;
                                                    } else {
                                                        try {
                                                            toStack = topActivity;
                                                            try {
                                                                this.mService.mStackSupervisor.mNoAnimActivities.add(toStack);
                                                            } catch (Throwable th4) {
                                                                th = th4;
                                                            }
                                                        } catch (Throwable th5) {
                                                            th = th5;
                                                            configurationContainer = topActivity;
                                                            z4 = toStackWindowingMode;
                                                            activityStack = preferredStack;
                                                            windowManager.continueSurfaceLayout();
                                                            throw th;
                                                        }
                                                    }
                                                    activityStack2.prepareFreezingTaskBounds();
                                                    z4 = toStackWindowingMode;
                                                    z3 = z4 ? position2 : false;
                                                    configBounds = getOverrideBounds();
                                                    if ((z4 != position2 || z4) && !Objects.equals(configBounds, activityStack2.getOverrideBounds())) {
                                                        overrideBounds = activityStack2.getOverrideBounds();
                                                        if (mightReplaceWindow) {
                                                            position2 = false;
                                                        }
                                                        kept = resize(overrideBounds, 0, position2, z2);
                                                    } else if (z4) {
                                                        Rect bounds = getLaunchBounds();
                                                        if (bounds == null) {
                                                            this.mService.mStackSupervisor.getLaunchParamsController().layoutTask(this, null);
                                                            bounds = configBounds;
                                                        }
                                                        kept = resize(bounds, 2, !mightReplaceWindow, z2);
                                                    } else if (z3 || z4) {
                                                        if (z3) {
                                                            z = true;
                                                            if (i2 == 1 && !str.contains("swapDockedAndFullscreenStack")) {
                                                                this.mService.mStackSupervisor.moveRecentsStackToFront(str);
                                                            }
                                                        } else {
                                                            z = true;
                                                        }
                                                        kept = resize(activityStack2.getOverrideBounds(), 0, !mightReplaceWindow ? z : false, z2);
                                                    }
                                                    windowManager.continueSurfaceLayout();
                                                    if (mightReplaceWindow) {
                                                        windowManager.scheduleClearWillReplaceWindows(toStack.appToken, !kept);
                                                    }
                                                    if (z2) {
                                                        i = 0;
                                                    } else {
                                                        i = 0;
                                                        supervisor.ensureActivitiesVisibleLocked(null, 0, !mightReplaceWindow);
                                                        supervisor.resumeFocusedStackTopActivityLocked();
                                                    }
                                                    activityStack = preferredStack;
                                                    supervisor.handleNonResizableTaskIfNeeded(this, preferredStack.getWindowingMode(), i, activityStack2);
                                                    if (activityStack == activityStack2) {
                                                        i = 1;
                                                    }
                                                    return i;
                                                }
                                                i5 = 1;
                                                z3 = i5;
                                                moveStackToFront = z3;
                                                if (moveStackToFront) {
                                                }
                                                sourceStack.removeTask(this, str, moveStackToFront ? 2 : i5);
                                                toStack.addTask(this, i, false, str);
                                                if (schedulePictureInPictureModeChange) {
                                                }
                                                if (this.voiceSession != null) {
                                                }
                                                if (r != null) {
                                                }
                                                if (z) {
                                                }
                                                activityStack2.prepareFreezingTaskBounds();
                                                z4 = toStackWindowingMode;
                                                if (z4) {
                                                }
                                                try {
                                                    configBounds = getOverrideBounds();
                                                    if (z4 != position2) {
                                                    }
                                                    overrideBounds = activityStack2.getOverrideBounds();
                                                    if (mightReplaceWindow) {
                                                    }
                                                    kept = resize(overrideBounds, 0, position2, z2);
                                                    windowManager.continueSurfaceLayout();
                                                    if (mightReplaceWindow) {
                                                    }
                                                    if (z2) {
                                                    }
                                                    activityStack = preferredStack;
                                                    supervisor.handleNonResizableTaskIfNeeded(this, preferredStack.getWindowingMode(), i, activityStack2);
                                                    if (activityStack == activityStack2) {
                                                    }
                                                    return i;
                                                } catch (Throwable th6) {
                                                    th = th6;
                                                    activityStack = preferredStack;
                                                    windowManager.continueSurfaceLayout();
                                                    throw th;
                                                }
                                            }
                                        } catch (Throwable th7) {
                                            th = th7;
                                            activityStack = activityStack2;
                                            i3 = i;
                                            activityStack2 = toStack;
                                            activityStack3 = sourceStack;
                                            configurationContainer = topActivity;
                                            windowManager.continueSurfaceLayout();
                                            throw th;
                                        }
                                    }
                                    z3 = false;
                                    wasFront = z3;
                                    i = toStack.getAdjustedPositionForTask(this, i, null);
                                    if (i2 != 0) {
                                    }
                                    try {
                                        this.mWindowContainerController.reparent(toStack.getWindowContainerController(), i, i2 != 0);
                                        if (i2 == 0) {
                                        }
                                        z3 = i5;
                                        moveStackToFront = z3;
                                        if (moveStackToFront) {
                                        }
                                        sourceStack.removeTask(this, str, moveStackToFront ? 2 : i5);
                                        toStack.addTask(this, i, false, str);
                                        if (schedulePictureInPictureModeChange) {
                                        }
                                        if (this.voiceSession != null) {
                                        }
                                        if (r != null) {
                                        }
                                        if (z) {
                                        }
                                    } catch (Throwable th8) {
                                        th = th8;
                                        i3 = i;
                                        activityStack2 = toStack;
                                        activityStack3 = sourceStack;
                                        configurationContainer = topActivity;
                                        z4 = toStackWindowingMode;
                                        activityStack = preferredStack;
                                        windowManager.continueSurfaceLayout();
                                        throw th;
                                    }
                                    try {
                                        activityStack2.prepareFreezingTaskBounds();
                                        z4 = toStackWindowingMode;
                                        if (z4) {
                                        }
                                        configBounds = getOverrideBounds();
                                        if (z4 != position2) {
                                        }
                                        overrideBounds = activityStack2.getOverrideBounds();
                                        if (mightReplaceWindow) {
                                        }
                                        kept = resize(overrideBounds, 0, position2, z2);
                                        windowManager.continueSurfaceLayout();
                                        if (mightReplaceWindow) {
                                        }
                                        if (z2) {
                                        }
                                        activityStack = preferredStack;
                                        supervisor.handleNonResizableTaskIfNeeded(this, preferredStack.getWindowingMode(), i, activityStack2);
                                        if (activityStack == activityStack2) {
                                        }
                                        return i;
                                    } catch (Throwable th9) {
                                        th = th9;
                                        z4 = toStackWindowingMode;
                                        activityStack = preferredStack;
                                        windowManager.continueSurfaceLayout();
                                        throw th;
                                    }
                                }
                            } catch (Throwable th10) {
                                th = th10;
                                activityStack = activityStack2;
                                i3 = i;
                                activityStack2 = toStack;
                                z4 = toStackWindowingMode2;
                                activityStack3 = sourceStack;
                                configurationContainer = topActivity;
                                windowManager.continueSurfaceLayout();
                                throw th;
                            }
                        }
                        i4 = 0;
                        toStackWindowingMode = toStackWindowingMode2;
                        toStackWindowingMode3 = i4;
                        if (r != null) {
                        }
                        z3 = false;
                        wasFront = z3;
                        i = toStack.getAdjustedPositionForTask(this, i, null);
                        try {
                            if (i2 != 0) {
                            }
                            this.mWindowContainerController.reparent(toStack.getWindowContainerController(), i, i2 != 0);
                            if (i2 == 0) {
                            }
                            z3 = i5;
                            moveStackToFront = z3;
                            if (moveStackToFront) {
                            }
                            sourceStack.removeTask(this, str, moveStackToFront ? 2 : i5);
                            toStack.addTask(this, i, false, str);
                            if (schedulePictureInPictureModeChange) {
                            }
                            if (this.voiceSession != null) {
                            }
                            if (r != null) {
                            }
                            if (z) {
                            }
                            activityStack2.prepareFreezingTaskBounds();
                            z4 = toStackWindowingMode;
                            if (z4) {
                            }
                            configBounds = getOverrideBounds();
                            if (z4 != position2) {
                            }
                            overrideBounds = activityStack2.getOverrideBounds();
                            if (mightReplaceWindow) {
                            }
                            kept = resize(overrideBounds, 0, position2, z2);
                            windowManager.continueSurfaceLayout();
                            if (mightReplaceWindow) {
                            }
                            if (z2) {
                            }
                            activityStack = preferredStack;
                            supervisor.handleNonResizableTaskIfNeeded(this, preferredStack.getWindowingMode(), i, activityStack2);
                            if (activityStack == activityStack2) {
                            }
                            return i;
                        } catch (Throwable th11) {
                            th = th11;
                            activityStack = activityStack2;
                            i3 = i;
                            activityStack2 = toStack;
                            activityStack3 = sourceStack;
                            configurationContainer = topActivity;
                            z4 = toStackWindowingMode;
                            windowManager.continueSurfaceLayout();
                            throw th;
                        }
                    }
                } catch (Throwable th12) {
                    th = th12;
                    activityStack = activityStack2;
                    i3 = i;
                    activityStack2 = toStack;
                    configurationContainer = topActivity2;
                    z4 = toStackWindowingMode2;
                    activityStack3 = sourceStack;
                    windowManager.continueSurfaceLayout();
                    throw th;
                }
            }
            z3 = false;
            wasFocused = z3;
            if (r == null) {
            }
            topActivity = topActivity2;
            topActivity3 = activityRecord;
            if (r != null) {
            }
            i4 = 0;
            toStackWindowingMode = toStackWindowingMode2;
            toStackWindowingMode3 = i4;
            if (r != null) {
            }
            z3 = false;
            wasFront = z3;
            try {
                i = toStack.getAdjustedPositionForTask(this, i, null);
                if (i2 != 0) {
                }
                this.mWindowContainerController.reparent(toStack.getWindowContainerController(), i, i2 != 0);
                if (i2 == 0) {
                }
                z3 = i5;
                moveStackToFront = z3;
                if (moveStackToFront) {
                }
                sourceStack.removeTask(this, str, moveStackToFront ? 2 : i5);
                toStack.addTask(this, i, false, str);
                if (schedulePictureInPictureModeChange) {
                }
                if (this.voiceSession != null) {
                }
                if (r != null) {
                }
                if (z) {
                }
                activityStack2.prepareFreezingTaskBounds();
                z4 = toStackWindowingMode;
                if (z4) {
                }
                configBounds = getOverrideBounds();
                if (z4 != position2) {
                }
                overrideBounds = activityStack2.getOverrideBounds();
                if (mightReplaceWindow) {
                }
                kept = resize(overrideBounds, 0, position2, z2);
                windowManager.continueSurfaceLayout();
                if (mightReplaceWindow) {
                }
                if (z2) {
                }
                activityStack = preferredStack;
                supervisor.handleNonResizableTaskIfNeeded(this, preferredStack.getWindowingMode(), i, activityStack2);
                if (activityStack == activityStack2) {
                }
                return i;
            } catch (Throwable th13) {
                th = th13;
                activityStack = activityStack2;
                activityStack2 = toStack;
                activityStack3 = sourceStack;
                configurationContainer = topActivity;
                z4 = toStackWindowingMode;
                i3 = i;
                windowManager.continueSurfaceLayout();
                throw th;
            }
        } catch (Throwable th14) {
            th = th14;
            activityStack = activityStack2;
            activityStack2 = toStack;
            z4 = toStackWindowingMode2;
            activityStack3 = sourceStack;
            i3 = i;
            windowManager.continueSurfaceLayout();
            throw th;
        }
        configurationContainer = topActivity;
        r = toStackWindowingMode;
        activityStack = preferredStack;
        windowManager.continueSurfaceLayout();
        throw th;
        r = toStackWindowingMode;
        activityStack = preferredStack;
        windowManager.continueSurfaceLayout();
        throw th;
    }

    private static boolean replaceWindowsOnTaskMove(int sourceWindowingMode, int targetWindowingMode) {
        return sourceWindowingMode == 5 || targetWindowingMode == 5;
    }

    void cancelWindowTransition() {
        if (this.mWindowContainerController != null) {
            this.mWindowContainerController.cancelWindowTransition();
        }
    }

    TaskSnapshot getSnapshot(boolean reducedResolution) {
        return this.mService.mWindowManager.getTaskSnapshot(this.taskId, this.userId, reducedResolution);
    }

    void touchActiveTime() {
        this.lastActiveTime = SystemClock.elapsedRealtime();
    }

    long getInactiveDuration() {
        return SystemClock.elapsedRealtime() - this.lastActiveTime;
    }

    void setIntent(ActivityRecord r) {
        this.mCallingUid = r.launchedFromUid;
        this.mCallingPackage = r.launchedFromPackage;
        setIntent(r.intent, r.info);
        setLockTaskAuth(r);
    }

    private void setIntent(Intent _intent, ActivityInfo info) {
        if (this.intent == null) {
            this.mNeverRelinquishIdentity = (info.flags & 4096) == 0;
        } else if (this.mNeverRelinquishIdentity) {
            return;
        }
        this.affinity = info.taskAffinity;
        if (this.intent == null) {
            this.rootAffinity = this.affinity;
        }
        this.effectiveUid = info.applicationInfo.uid;
        this.stringName = null;
        if (info.targetActivity == null) {
            if (!(_intent == null || (_intent.getSelector() == null && _intent.getSourceBounds() == null))) {
                _intent = new Intent(_intent);
                _intent.setSelector(null);
                _intent.setSourceBounds(null);
            }
            if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Setting Intent of ");
                stringBuilder.append(this);
                stringBuilder.append(" to ");
                stringBuilder.append(_intent);
                Slog.v(str, stringBuilder.toString());
            }
            this.intent = _intent;
            this.realActivity = _intent != null ? _intent.getComponent() : null;
            this.origActivity = null;
        } else {
            ComponentName targetComponent = new ComponentName(info.packageName, info.targetActivity);
            if (_intent != null) {
                Intent targetIntent = new Intent(_intent);
                targetIntent.setComponent(targetComponent);
                targetIntent.setSelector(null);
                targetIntent.setSourceBounds(null);
                if (ActivityManagerDebugConfig.DEBUG_TASKS) {
                    String str2 = ActivityManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Setting Intent of ");
                    stringBuilder2.append(this);
                    stringBuilder2.append(" to target ");
                    stringBuilder2.append(targetIntent);
                    Slog.v(str2, stringBuilder2.toString());
                }
                this.intent = targetIntent;
                this.realActivity = targetComponent;
                this.origActivity = _intent.getComponent();
            } else {
                this.intent = null;
                this.realActivity = targetComponent;
                this.origActivity = new ComponentName(info.packageName, info.name);
            }
        }
        int intentFlags = this.intent == null ? 0 : this.intent.getFlags();
        if ((DumpState.DUMP_COMPILER_STATS & intentFlags) != 0) {
            this.rootWasReset = true;
        }
        this.userId = UserHandle.getUserId(info.applicationInfo.uid);
        this.mUserSetupComplete = Secure.getIntForUser(this.mService.mContext.getContentResolver(), ATTR_USER_SETUP_COMPLETE, 0, this.userId) != 0;
        if ((info.flags & 8192) != 0) {
            this.autoRemoveRecents = true;
        } else if ((532480 & intentFlags) != DumpState.DUMP_FROZEN) {
            this.autoRemoveRecents = false;
        } else if (info.documentLaunchMode != 0) {
            this.autoRemoveRecents = false;
        } else {
            this.autoRemoveRecents = true;
        }
        this.mResizeMode = info.resizeMode;
        this.mSupportsPictureInPicture = info.supportsPictureInPicture();
    }

    private void setMinDimensions(ActivityInfo info) {
        if (info == null || info.windowLayout == null) {
            this.mMinWidth = -1;
            this.mMinHeight = -1;
            return;
        }
        this.mMinWidth = info.windowLayout.minWidth;
        this.mMinHeight = info.windowLayout.minHeight;
    }

    boolean isSameIntentFilter(ActivityRecord r) {
        Intent intent = new Intent(r.intent);
        intent.setComponent(r.realActivity);
        return intent.filterEquals(this.intent);
    }

    boolean returnsToHomeStack() {
        return this.intent != null && (this.intent.getFlags() & 268451840) == 268451840;
    }

    void setPrevAffiliate(TaskRecord prevAffiliate) {
        this.mPrevAffiliate = prevAffiliate;
        this.mPrevAffiliateTaskId = prevAffiliate == null ? -1 : prevAffiliate.taskId;
    }

    void setNextAffiliate(TaskRecord nextAffiliate) {
        this.mNextAffiliate = nextAffiliate;
        this.mNextAffiliateTaskId = nextAffiliate == null ? -1 : nextAffiliate.taskId;
    }

    <T extends ActivityStack> T getStack() {
        return this.mStack;
    }

    void setStack(ActivityStack stack) {
        if (stack == null || stack.isInStackLocked(this)) {
            ActivityStack oldStack = this.mStack;
            this.mStack = stack;
            if (oldStack != this.mStack) {
                for (int i = getChildCount() - 1; i >= 0; i--) {
                    ActivityRecord activity = getChildAt(i);
                    if (oldStack != null) {
                        oldStack.onActivityRemovedFromStack(activity);
                    }
                    if (this.mStack != null) {
                        stack.onActivityAddedToStack(activity);
                    }
                }
            }
            onParentChanged();
            return;
        }
        throw new IllegalStateException("Task must be added as a Stack child first.");
    }

    int getStackId() {
        return this.mStack != null ? this.mStack.mStackId : -1;
    }

    protected int getChildCount() {
        return this.mActivities.size();
    }

    protected ActivityRecord getChildAt(int index) {
        return (ActivityRecord) this.mActivities.get(index);
    }

    protected ConfigurationContainer getParent() {
        return this.mStack;
    }

    protected void onParentChanged() {
        super.onParentChanged();
        this.mService.mStackSupervisor.updateUIDsPresentOnDisplay();
    }

    private void closeRecentsChain() {
        if (this.mPrevAffiliate != null) {
            this.mPrevAffiliate.setNextAffiliate(this.mNextAffiliate);
        }
        if (this.mNextAffiliate != null) {
            this.mNextAffiliate.setPrevAffiliate(this.mPrevAffiliate);
        }
        setPrevAffiliate(null);
        setNextAffiliate(null);
    }

    void removedFromRecents() {
        closeRecentsChain();
        if (this.inRecents) {
            this.inRecents = false;
            this.mService.notifyTaskPersisterLocked(this, false);
        }
        clearRootProcess();
        this.mService.mWindowManager.notifyTaskRemovedFromRecents(this.taskId, this.userId);
    }

    void setTaskToAffiliateWith(TaskRecord taskToAffiliateWith) {
        closeRecentsChain();
        this.mAffiliatedTaskId = taskToAffiliateWith.mAffiliatedTaskId;
        this.mAffiliatedTaskColor = taskToAffiliateWith.mAffiliatedTaskColor;
        while (taskToAffiliateWith.mNextAffiliate != null) {
            TaskRecord nextRecents = taskToAffiliateWith.mNextAffiliate;
            if (nextRecents.mAffiliatedTaskId != this.mAffiliatedTaskId) {
                String str = ActivityManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("setTaskToAffiliateWith: nextRecents=");
                stringBuilder.append(nextRecents);
                stringBuilder.append(" affilTaskId=");
                stringBuilder.append(nextRecents.mAffiliatedTaskId);
                stringBuilder.append(" should be ");
                stringBuilder.append(this.mAffiliatedTaskId);
                Slog.e(str, stringBuilder.toString());
                if (nextRecents.mPrevAffiliate == taskToAffiliateWith) {
                    nextRecents.setPrevAffiliate(null);
                }
                taskToAffiliateWith.setNextAffiliate(null);
                taskToAffiliateWith.setNextAffiliate(this);
                setPrevAffiliate(taskToAffiliateWith);
                setNextAffiliate(null);
            }
            taskToAffiliateWith = nextRecents;
        }
        taskToAffiliateWith.setNextAffiliate(this);
        setPrevAffiliate(taskToAffiliateWith);
        setNextAffiliate(null);
    }

    Intent getBaseIntent() {
        return this.intent != null ? this.intent : this.affinityIntent;
    }

    ActivityRecord getRootActivity() {
        for (int i = 0; i < this.mActivities.size(); i++) {
            ActivityRecord r = (ActivityRecord) this.mActivities.get(i);
            if (!r.finishing) {
                return r;
            }
        }
        return null;
    }

    ActivityRecord getTopActivity() {
        return getTopActivity(true);
    }

    ActivityRecord getTopActivity(boolean includeOverlays) {
        for (int i = this.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = (ActivityRecord) this.mActivities.get(i);
            if (!r.finishing && (includeOverlays || !r.mTaskOverlay)) {
                return r;
            }
        }
        return null;
    }

    ActivityRecord topRunningActivityLocked() {
        if (this.mStack != null) {
            for (int activityNdx = this.mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) this.mActivities.get(activityNdx);
                if (!r.finishing && r.okToShowLocked()) {
                    return r;
                }
            }
        }
        return null;
    }

    boolean isVisible() {
        for (int i = this.mActivities.size() - 1; i >= 0; i--) {
            if (((ActivityRecord) this.mActivities.get(i)).visible) {
                return true;
            }
        }
        return false;
    }

    void getAllRunningVisibleActivitiesLocked(ArrayList<ActivityRecord> outActivities) {
        if (this.mStack != null) {
            for (int activityNdx = this.mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) this.mActivities.get(activityNdx);
                if (!r.finishing && r.okToShowLocked() && r.visibleIgnoringKeyguard) {
                    outActivities.add(r);
                }
            }
        }
    }

    ActivityRecord topRunningActivityWithStartingWindowLocked() {
        if (this.mStack != null) {
            for (int activityNdx = this.mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
                ActivityRecord r = (ActivityRecord) this.mActivities.get(activityNdx);
                if (r.mStartingWindowState == 1 && !r.finishing && r.okToShowLocked()) {
                    return r;
                }
            }
        }
        return null;
    }

    void getNumRunningActivities(TaskActivitiesReport reportOut) {
        reportOut.reset();
        for (int i = this.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = (ActivityRecord) this.mActivities.get(i);
            if (!r.finishing) {
                reportOut.base = r;
                reportOut.numActivities++;
                if (reportOut.top == null || reportOut.top.isState(ActivityState.INITIALIZING)) {
                    reportOut.top = r;
                    reportOut.numRunning = 0;
                }
                if (!(r.app == null || r.app.thread == null)) {
                    reportOut.numRunning++;
                }
            }
        }
    }

    boolean okToShowLocked() {
        return this.mService.mStackSupervisor.isCurrentProfileLocked(this.userId) || topRunningActivityLocked() != null;
    }

    final void setFrontOfTask() {
        int numActivities = this.mActivities.size();
        boolean foundFront = false;
        for (int activityNdx = 0; activityNdx < numActivities; activityNdx++) {
            ActivityRecord r = (ActivityRecord) this.mActivities.get(activityNdx);
            if (foundFront || r.finishing) {
                r.frontOfTask = false;
            } else {
                r.frontOfTask = true;
                foundFront = true;
            }
        }
        if (!foundFront && numActivities > 0) {
            ((ActivityRecord) this.mActivities.get(0)).frontOfTask = true;
        }
    }

    final void moveActivityToFrontLocked(ActivityRecord newTop) {
        if (ActivityManagerDebugConfig.DEBUG_ADD_REMOVE) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Removing and adding activity ");
            stringBuilder.append(newTop);
            stringBuilder.append(" to stack at top callers=");
            stringBuilder.append(Debug.getCallers(4));
            Slog.i(str, stringBuilder.toString());
        }
        this.mActivities.remove(newTop);
        this.mActivities.add(newTop);
        this.mWindowContainerController.positionChildAtTop(newTop.mWindowContainerController);
        updateEffectiveIntent();
        setFrontOfTask();
    }

    void addActivityAtBottom(ActivityRecord r) {
        addActivityAtIndex(0, r);
    }

    void addActivityToTop(ActivityRecord r) {
        addActivityAtIndex(this.mActivities.size(), r);
    }

    public int getActivityType() {
        int applicationType = super.getActivityType();
        if (applicationType != 0 || this.mActivities.isEmpty()) {
            return applicationType;
        }
        return ((ActivityRecord) this.mActivities.get(0)).getActivityType();
    }

    void addActivityAtIndex(int index, ActivityRecord r) {
        TaskRecord task = r.getTask();
        if (task == null || task == this) {
            r.setTask(this);
            if (!this.mActivities.remove(r) && r.fullscreen) {
                this.numFullscreen++;
            }
            if (this.mActivities.isEmpty()) {
                if (r.getActivityType() == 0) {
                    r.setActivityType(1);
                }
                setActivityType(r.getActivityType());
                this.isPersistable = r.isPersistable();
                this.mCallingUid = r.launchedFromUid;
                this.mCallingPackage = r.launchedFromPackage;
                this.maxRecents = Math.min(Math.max(r.info.maxRecents, 1), ActivityManager.getMaxAppRecentsLimitStatic());
            } else {
                r.setActivityType(getActivityType());
            }
            int size = this.mActivities.size();
            if (index == size && size > 0 && ((ActivityRecord) this.mActivities.get(size - 1)).mTaskOverlay) {
                index--;
            }
            index = Math.min(size, index);
            this.mActivities.add(index, r);
            updateEffectiveIntent();
            if (r.isPersistable()) {
                this.mService.notifyTaskPersisterLocked(this, false);
            }
            updateOverrideConfigurationFromLaunchBounds();
            AppWindowContainerController appController = r.getWindowContainerController();
            if (appController != null) {
                this.mWindowContainerController.positionChildAt(appController, index);
            }
            this.mService.mStackSupervisor.updateUIDsPresentOnDisplay();
            return;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Can not add r= to task=");
        stringBuilder.append(this);
        stringBuilder.append(" current parent=");
        stringBuilder.append(task);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    boolean removeActivity(ActivityRecord r) {
        return removeActivity(r, false);
    }

    boolean removeActivity(ActivityRecord r, boolean reparenting) {
        if (r.getTask() == this) {
            r.setTask(null, reparenting);
            if (this.mActivities.remove(r) && r.fullscreen) {
                this.numFullscreen--;
            }
            if (r.isPersistable()) {
                this.mService.notifyTaskPersisterLocked(this, false);
            }
            if (inPinnedWindowingMode()) {
                this.mService.mTaskChangeNotificationController.notifyTaskStackChanged();
            }
            if (this.mActivities.isEmpty()) {
                return this.mReuseTask ^ 1;
            }
            updateEffectiveIntent();
            return false;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Activity=");
        stringBuilder.append(r);
        stringBuilder.append(" does not belong to task=");
        stringBuilder.append(this);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    boolean onlyHasTaskOverlayActivities(boolean excludeFinishing) {
        int count = 0;
        boolean z = true;
        for (int i = this.mActivities.size() - 1; i >= 0; i--) {
            ActivityRecord r = (ActivityRecord) this.mActivities.get(i);
            if (!excludeFinishing || !r.finishing) {
                if (!r.mTaskOverlay) {
                    return false;
                }
                count++;
            }
        }
        if (count <= 0) {
            z = false;
        }
        return z;
    }

    boolean autoRemoveFromRecents() {
        return this.autoRemoveRecents || (this.mActivities.isEmpty() && !this.hasBeenVisible);
    }

    final void performClearTaskAtIndexLocked(int activityNdx, boolean pauseImmediately, String reason) {
        int numActivities = this.mActivities.size();
        while (activityNdx < numActivities) {
            try {
                ActivityRecord r = (ActivityRecord) this.mActivities.get(activityNdx);
                if (!r.finishing) {
                    if (this.mStack == null) {
                        r.takeFromHistory();
                        this.mActivities.remove(activityNdx);
                        activityNdx--;
                        numActivities--;
                    } else if (this.mStack.finishActivityLocked(r, 0, null, reason, false, pauseImmediately)) {
                        activityNdx--;
                        numActivities--;
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                Slog.e(ActivityManagerService.TAG, "performClearTaskAtIndexLocked: IndexOutOfBoundsException!");
            }
            activityNdx++;
        }
    }

    void performClearTaskLocked() {
        this.mReuseTask = true;
        performClearTaskAtIndexLocked(0, false, "clear-task-all");
        this.mReuseTask = false;
    }

    ActivityRecord performClearTaskForReuseLocked(ActivityRecord newR, int launchFlags) {
        this.mReuseTask = true;
        ActivityRecord result = performClearTaskLocked(newR, launchFlags);
        this.mReuseTask = false;
        return result;
    }

    final ActivityRecord performClearTaskLocked(ActivityRecord newR, int launchFlags) {
        int numActivities = this.mActivities.size();
        int activityNdx = numActivities - 1;
        while (activityNdx >= 0) {
            ActivityRecord r = (ActivityRecord) this.mActivities.get(activityNdx);
            if (!r.finishing && r.realActivity.equals(newR.realActivity)) {
                ActivityRecord ret = r;
                while (true) {
                    activityNdx++;
                    if (activityNdx >= numActivities) {
                        break;
                    }
                    r = (ActivityRecord) this.mActivities.get(activityNdx);
                    if (!r.finishing) {
                        ActivityOptions opts = r.takeOptionsLocked();
                        if (opts != null) {
                            ret.updateOptionsLocked(opts);
                        }
                        if (this.mStack != null) {
                            if (this.mStack.finishActivityLocked(r, 0, null, "clear-task-stack", false)) {
                                activityNdx--;
                                numActivities--;
                            }
                        }
                    }
                }
                if (ret.launchMode != 0 || (536870912 & launchFlags) != 0 || ActivityStarter.isDocumentLaunchesIntoExisting(launchFlags) || ret.finishing) {
                    return ret;
                }
                if (this.mStack != null) {
                    this.mStack.finishActivityLocked(ret, 0, null, "clear-task-top", false);
                }
                return null;
            }
            activityNdx--;
        }
        return null;
    }

    void removeTaskActivitiesLocked(boolean pauseImmediately, String reason) {
        performClearTaskAtIndexLocked(0, pauseImmediately, reason);
    }

    String lockTaskAuthToString() {
        switch (this.mLockTaskAuth) {
            case 0:
                return "LOCK_TASK_AUTH_DONT_LOCK";
            case 1:
                return "LOCK_TASK_AUTH_PINNABLE";
            case 2:
                return "LOCK_TASK_AUTH_LAUNCHABLE";
            case 3:
                return "LOCK_TASK_AUTH_WHITELISTED";
            case 4:
                return "LOCK_TASK_AUTH_LAUNCHABLE_PRIV";
            default:
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("unknown=");
                stringBuilder.append(this.mLockTaskAuth);
                return stringBuilder.toString();
        }
    }

    void setLockTaskAuth() {
        setLockTaskAuth(getRootActivity());
    }

    private void setLockTaskAuth(ActivityRecord r) {
        int i = 1;
        if (r == null) {
            this.mLockTaskAuth = 1;
            return;
        }
        String pkg = this.realActivity != null ? this.realActivity.getPackageName() : null;
        LockTaskController lockTaskController = this.mService.getLockTaskController();
        switch (r.lockTaskLaunchMode) {
            case 0:
                if (lockTaskController.isPackageWhitelisted(this.userId, pkg)) {
                    i = 3;
                }
                this.mLockTaskAuth = i;
                break;
            case 1:
                this.mLockTaskAuth = 0;
                break;
            case 2:
                this.mLockTaskAuth = 4;
                break;
            case 3:
                if (lockTaskController.isPackageWhitelisted(this.userId, pkg)) {
                    i = 2;
                }
                this.mLockTaskAuth = i;
                break;
        }
        if (ActivityManagerDebugConfig.DEBUG_LOCKTASK) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("setLockTaskAuth: task=");
            stringBuilder.append(this);
            stringBuilder.append(" mLockTaskAuth=");
            stringBuilder.append(lockTaskAuthToString());
            Slog.d(str, stringBuilder.toString());
        }
    }

    protected boolean isResizeable(boolean checkSupportsPip) {
        return this.mService.mForceResizableActivities || ActivityInfo.isResizeableMode(this.mResizeMode) || (checkSupportsPip && this.mSupportsPictureInPicture);
    }

    boolean isResizeable() {
        return isResizeable(true);
    }

    public boolean supportsSplitScreenWindowingMode() {
        if (!super.supportsSplitScreenWindowingMode() || !this.mService.mSupportsSplitScreenMultiWindow) {
            return false;
        }
        if (this.mService.mForceResizableActivities || (isResizeable(false) && !ActivityInfo.isPreserveOrientationMode(this.mResizeMode))) {
            return true;
        }
        return false;
    }

    boolean canBeLaunchedOnDisplay(int displayId) {
        return this.mService.mStackSupervisor.canPlaceEntityOnDisplay(displayId, isResizeable(false), -1, -1, null);
    }

    private boolean canResizeToBounds(Rect bounds) {
        boolean z = true;
        if (bounds == null || !inFreeformWindowingMode()) {
            return true;
        }
        boolean landscape = bounds.width() > bounds.height();
        Rect configBounds = getOverrideBounds();
        if (this.mResizeMode == 7) {
            if (!configBounds.isEmpty()) {
                if (landscape != (configBounds.width() > configBounds.height())) {
                    z = false;
                }
            }
            return z;
        }
        if ((this.mResizeMode == 6 && landscape) || (this.mResizeMode == 5 && !landscape)) {
            z = false;
        }
        return z;
    }

    boolean isClearingToReuseTask() {
        return this.mReuseTask;
    }

    final ActivityRecord findActivityInHistoryLocked(ActivityRecord r) {
        ComponentName realActivity = r.realActivity;
        for (int activityNdx = this.mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
            ActivityRecord candidate = (ActivityRecord) this.mActivities.get(activityNdx);
            if (!candidate.finishing && candidate.realActivity.equals(realActivity)) {
                return candidate;
            }
        }
        return null;
    }

    void updateTaskDescription() {
        ActivityRecord r;
        int numActivities = this.mActivities.size();
        boolean relinquish = false;
        if (!(numActivities == 0 || (((ActivityRecord) this.mActivities.get(0)).info.flags & 4096) == 0)) {
            relinquish = true;
        }
        int activityNdx = Math.min(numActivities, 1);
        while (activityNdx < numActivities) {
            r = (ActivityRecord) this.mActivities.get(activityNdx);
            if (!relinquish || (r.info.flags & 4096) != 0) {
                if (r.intent != null && (r.intent.getFlags() & DumpState.DUMP_FROZEN) != 0) {
                    break;
                }
                activityNdx++;
            } else {
                activityNdx++;
                break;
            }
        }
        if (activityNdx > 0) {
            String label = null;
            String iconFilename = null;
            int iconResource = -1;
            int colorPrimary = 0;
            int colorBackground = 0;
            int statusBarColor = 0;
            int navigationBarColor = 0;
            boolean topActivity = true;
            for (activityNdx--; activityNdx >= 0; activityNdx--) {
                r = (ActivityRecord) this.mActivities.get(activityNdx);
                if (!r.mTaskOverlay) {
                    if (r.taskDescription != null) {
                        if (label == null) {
                            label = r.taskDescription.getLabel();
                        }
                        if (iconResource == -1) {
                            iconResource = r.taskDescription.getIconResource();
                        }
                        if (iconFilename == null) {
                            iconFilename = r.taskDescription.getIconFilename();
                        }
                        if (colorPrimary == 0) {
                            colorPrimary = r.taskDescription.getPrimaryColor();
                        }
                        if (topActivity) {
                            colorBackground = r.taskDescription.getBackgroundColor();
                            statusBarColor = r.taskDescription.getStatusBarColor();
                            navigationBarColor = r.taskDescription.getNavigationBarColor();
                        }
                    }
                    topActivity = false;
                }
            }
            TaskDescription taskDescription = r4;
            TaskDescription taskDescription2 = new TaskDescription(label, null, iconResource, iconFilename, colorPrimary, colorBackground, statusBarColor, navigationBarColor);
            this.lastTaskDescription = taskDescription;
            if (this.mWindowContainerController != null) {
                this.mWindowContainerController.setTaskDescription(this.lastTaskDescription);
            }
            if (this.taskId == this.mAffiliatedTaskId) {
                this.mAffiliatedTaskColor = this.lastTaskDescription.getPrimaryColor();
                return;
            }
            return;
        }
    }

    int findEffectiveRootIndex() {
        int effectiveNdx = 0;
        int topActivityNdx = this.mActivities.size() - 1;
        for (int activityNdx = 0; activityNdx <= topActivityNdx; activityNdx++) {
            ActivityRecord r = (ActivityRecord) this.mActivities.get(activityNdx);
            if (!r.finishing) {
                effectiveNdx = activityNdx;
                if ((r.info.flags & 4096) == 0) {
                    break;
                }
            }
        }
        return effectiveNdx;
    }

    void updateEffectiveIntent() {
        setIntent((ActivityRecord) this.mActivities.get(findEffectiveRootIndex()));
        updateTaskDescription();
    }

    protected void adjustForMinimalTaskDimensions(Rect bounds) {
        if (bounds != null) {
            int minWidth = this.mMinWidth;
            int minHeight = this.mMinHeight;
            if (!inPinnedWindowingMode()) {
                if (minWidth == -1) {
                    minWidth = this.mDefaultMinSize;
                }
                if (minHeight == -1) {
                    minHeight = this.mDefaultMinSize;
                }
            }
            boolean adjustHeight = false;
            boolean adjustWidth = minWidth > bounds.width();
            if (minHeight > bounds.height()) {
                adjustHeight = true;
            }
            if (adjustWidth || adjustHeight) {
                Rect configBounds = getOverrideBounds();
                if (adjustWidth) {
                    if (configBounds.isEmpty() || bounds.right != configBounds.right) {
                        bounds.right = bounds.left + minWidth;
                    } else {
                        bounds.left = bounds.right - minWidth;
                    }
                }
                if (adjustHeight) {
                    if (configBounds.isEmpty() || bounds.bottom != configBounds.bottom) {
                        bounds.bottom = bounds.top + minHeight;
                    } else {
                        bounds.top = bounds.bottom - minHeight;
                    }
                }
            }
        }
    }

    Configuration computeNewOverrideConfigurationForBounds(Rect bounds, Rect insetBounds) {
        Configuration newOverrideConfig = new Configuration();
        if (bounds != null) {
            newOverrideConfig.setTo(getOverrideConfiguration());
            this.mTmpRect.set(bounds);
            adjustForMinimalTaskDimensions(this.mTmpRect);
            computeOverrideConfiguration(newOverrideConfig, this.mTmpRect, insetBounds, this.mTmpRect.right != bounds.right, this.mTmpRect.bottom != bounds.bottom);
        }
        return newOverrideConfig;
    }

    boolean updateOverrideConfiguration(Rect bounds) {
        return updateOverrideConfiguration(bounds, null);
    }

    protected void updateHwOverrideConfiguration(Rect bounds) {
    }

    void activityResumedInTop() {
    }

    boolean updateOverrideConfiguration(Rect bounds, Rect insetBounds) {
        if (equivalentOverrideBounds(bounds) && (this.mStack == null || !HwPCUtils.isExtDynamicStack(this.mStack.getStackId()))) {
            return false;
        }
        Rect currentBounds = getOverrideBounds();
        this.mTmpConfig.setTo(getOverrideConfiguration());
        Configuration newConfig = getOverrideConfiguration();
        boolean z = bounds == null || bounds.isEmpty();
        boolean matchParentBounds = z;
        boolean persistBounds = getWindowConfiguration().persistTaskBounds();
        if (matchParentBounds) {
            if (!currentBounds.isEmpty() && persistBounds) {
                this.mLastNonFullscreenBounds = currentBounds;
            }
            setBounds(null);
            newConfig.unset();
        } else {
            this.mTmpRect.set(bounds);
            adjustForMinimalTaskDimensions(this.mTmpRect);
            setBounds(this.mTmpRect);
            if (this.mStack == null || persistBounds) {
                this.mLastNonFullscreenBounds = getOverrideBounds();
            }
            computeOverrideConfiguration(newConfig, this.mTmpRect, insetBounds, this.mTmpRect.right != bounds.right, this.mTmpRect.bottom != bounds.bottom);
            if (this.mRootActivityInfo != null && isMaximizedPortraitAppOnPCMode(this.mRootActivityInfo.packageName)) {
                newConfig.orientation = 1;
                this.mTmpConfig.orientation = 1;
            }
        }
        onOverrideConfigurationChanged(newConfig);
        updateHwOverrideConfiguration(bounds);
        return this.mTmpConfig.equals(newConfig) ^ 1;
    }

    void onActivityStateChanged(ActivityRecord record, ActivityState state, String reason) {
        ActivityStack parent = getStack();
        if (parent != null) {
            parent.onActivityStateChanged(record, state, reason);
        }
    }

    public void onConfigurationChanged(Configuration newParentConfig) {
        boolean wasInMultiWindowMode = inMultiWindowMode();
        super.onConfigurationChanged(newParentConfig);
        if (wasInMultiWindowMode != inMultiWindowMode()) {
            this.mService.mStackSupervisor.scheduleUpdateMultiWindowMode(this);
        }
    }

    void computeOverrideConfiguration(Configuration config, Rect bounds, Rect insetBounds, boolean overrideWidth, boolean overrideHeight) {
        Configuration configuration = config;
        Rect rect = bounds;
        if (getParent() == null) {
            Slog.w(ActivityManagerService.TAG, "computeOverrideConfiguration: getParent return null!");
            return;
        }
        this.mTmpNonDecorBounds.set(rect);
        this.mTmpStableBounds.set(rect);
        config.unset();
        Configuration parentConfig = getParent().getConfiguration();
        float density = ((float) parentConfig.densityDpi) * 0.00625f;
        if (this.mStack != null) {
            int i;
            this.mStack.getWindowContainerController().adjustConfigurationForBounds(rect, insetBounds, this.mTmpNonDecorBounds, this.mTmpStableBounds, overrideWidth, overrideHeight, density, configuration, parentConfig, getWindowingMode());
            if (configuration.screenWidthDp <= configuration.screenHeightDp) {
                i = 1;
            } else {
                i = 2;
            }
            configuration.orientation = i;
            overrideConfigOrienForFreeForm(config);
            i = (int) (((float) this.mTmpNonDecorBounds.width()) / density);
            int compatScreenHeightDp = (int) (((float) this.mTmpNonDecorBounds.height()) / density);
            configuration.screenLayout = Configuration.reduceScreenLayout(36, Math.max(compatScreenHeightDp, i), Math.min(compatScreenHeightDp, i));
            return;
        }
        throw new IllegalArgumentException("Expected stack when calculating override config");
    }

    Rect updateOverrideConfigurationFromLaunchBounds() {
        Rect bounds = getLaunchBounds();
        updateOverrideConfiguration(bounds);
        if (!(bounds == null || bounds.isEmpty())) {
            bounds.set(getOverrideBounds());
        }
        return bounds;
    }

    void updateOverrideConfigurationForStack(ActivityStack inStack) {
        if (this.mStack == null || this.mStack != inStack) {
            if (!inStack.inFreeformWindowingMode()) {
                updateOverrideConfiguration(inStack.getOverrideBounds());
            } else if (!isResizeable()) {
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Can not position non-resizeable task=");
                stringBuilder.append(this);
                stringBuilder.append(" in stack=");
                stringBuilder.append(inStack);
                throw new IllegalArgumentException(stringBuilder.toString());
            } else if (!matchParentBounds()) {
            } else {
                if (this.mLastNonFullscreenBounds != null) {
                    updateOverrideConfiguration(this.mLastNonFullscreenBounds);
                } else {
                    this.mService.mStackSupervisor.getLaunchParamsController().layoutTask(this, null);
                }
            }
        }
    }

    protected Rect getLaunchBounds() {
        Rect rect = null;
        if (this.mStack == null) {
            return null;
        }
        int windowingMode = getWindowingMode();
        if (!isActivityTypeStandardOrUndefined() || windowingMode == 1 || (windowingMode == 3 && !isResizeable())) {
            if (isResizeable()) {
                rect = this.mStack.getOverrideBounds();
            }
            return rect;
        } else if (getWindowConfiguration().persistTaskBounds()) {
            return this.mLastNonFullscreenBounds;
        } else {
            return this.mStack.getOverrideBounds();
        }
    }

    void addStartingWindowsForVisibleActivities(boolean taskSwitch) {
        for (int activityNdx = this.mActivities.size() - 1; activityNdx >= 0; activityNdx--) {
            ActivityRecord r = (ActivityRecord) this.mActivities.get(activityNdx);
            if (r.visible) {
                r.showStartingWindow(null, false, taskSwitch);
            }
        }
    }

    void setRootProcess(ProcessRecord proc) {
        clearRootProcess();
        if (this.intent != null && (this.intent.getFlags() & DumpState.DUMP_VOLUMES) == 0) {
            this.mRootProcess = proc;
            proc.recentTasks.add(this);
        }
    }

    void clearRootProcess() {
        if (this.mRootProcess != null) {
            this.mRootProcess.recentTasks.remove(this);
            this.mRootProcess = null;
        }
    }

    void clearAllPendingOptions() {
        for (int i = getChildCount() - 1; i >= 0; i--) {
            getChildAt(i).clearOptionsLocked(false);
        }
    }

    void dump(PrintWriter pw, String prefix) {
        StringBuilder sb;
        pw.print(prefix);
        pw.print("userId=");
        pw.print(this.userId);
        pw.print(" effectiveUid=");
        UserHandle.formatUid(pw, this.effectiveUid);
        pw.print(" mCallingUid=");
        UserHandle.formatUid(pw, this.mCallingUid);
        pw.print(" mUserSetupComplete=");
        pw.print(this.mUserSetupComplete);
        pw.print(" mCallingPackage=");
        pw.println(this.mCallingPackage);
        if (!(this.affinity == null && this.rootAffinity == null)) {
            pw.print(prefix);
            pw.print("affinity=");
            pw.print(this.affinity);
            if (this.affinity == null || !this.affinity.equals(this.rootAffinity)) {
                pw.print(" root=");
                pw.println(this.rootAffinity);
            } else {
                pw.println();
            }
        }
        if (!(this.voiceSession == null && this.voiceInteractor == null)) {
            pw.print(prefix);
            pw.print("VOICE: session=0x");
            pw.print(Integer.toHexString(System.identityHashCode(this.voiceSession)));
            pw.print(" interactor=0x");
            pw.println(Integer.toHexString(System.identityHashCode(this.voiceInteractor)));
        }
        if (this.intent != null) {
            sb = new StringBuilder(128);
            sb.append(prefix);
            sb.append("intent={");
            this.intent.toShortString(sb, true, true, false, true);
            sb.append('}');
            pw.println(sb.toString());
        }
        if (this.affinityIntent != null) {
            sb = new StringBuilder(128);
            sb.append(prefix);
            sb.append("affinityIntent={");
            this.affinityIntent.toShortString(sb, true, true, false, true);
            sb.append('}');
            pw.println(sb.toString());
        }
        if (this.origActivity != null) {
            pw.print(prefix);
            pw.print("origActivity=");
            pw.println(this.origActivity.flattenToShortString());
        }
        if (this.realActivity != null) {
            pw.print(prefix);
            pw.print("realActivity=");
            pw.println(this.realActivity.flattenToShortString());
        }
        if (this.autoRemoveRecents || this.isPersistable || !isActivityTypeStandard() || this.numFullscreen != 0) {
            pw.print(prefix);
            pw.print("autoRemoveRecents=");
            pw.print(this.autoRemoveRecents);
            pw.print(" isPersistable=");
            pw.print(this.isPersistable);
            pw.print(" numFullscreen=");
            pw.print(this.numFullscreen);
            pw.print(" activityType=");
            pw.println(getActivityType());
        }
        if (this.rootWasReset || this.mNeverRelinquishIdentity || this.mReuseTask || this.mLockTaskAuth != 1) {
            pw.print(prefix);
            pw.print("rootWasReset=");
            pw.print(this.rootWasReset);
            pw.print(" mNeverRelinquishIdentity=");
            pw.print(this.mNeverRelinquishIdentity);
            pw.print(" mReuseTask=");
            pw.print(this.mReuseTask);
            pw.print(" mLockTaskAuth=");
            pw.println(lockTaskAuthToString());
        }
        if (!(this.mAffiliatedTaskId == this.taskId && this.mPrevAffiliateTaskId == -1 && this.mPrevAffiliate == null && this.mNextAffiliateTaskId == -1 && this.mNextAffiliate == null)) {
            pw.print(prefix);
            pw.print("affiliation=");
            pw.print(this.mAffiliatedTaskId);
            pw.print(" prevAffiliation=");
            pw.print(this.mPrevAffiliateTaskId);
            pw.print(" (");
            if (this.mPrevAffiliate == null) {
                pw.print("null");
            } else {
                pw.print(Integer.toHexString(System.identityHashCode(this.mPrevAffiliate)));
            }
            pw.print(") nextAffiliation=");
            pw.print(this.mNextAffiliateTaskId);
            pw.print(" (");
            if (this.mNextAffiliate == null) {
                pw.print("null");
            } else {
                pw.print(Integer.toHexString(System.identityHashCode(this.mNextAffiliate)));
            }
            pw.println(")");
        }
        pw.print(prefix);
        pw.print("Activities=");
        pw.println(this.mActivities);
        if (!(this.askedCompatMode && this.inRecents && this.isAvailable)) {
            pw.print(prefix);
            pw.print("askedCompatMode=");
            pw.print(this.askedCompatMode);
            pw.print(" inRecents=");
            pw.print(this.inRecents);
            pw.print(" isAvailable=");
            pw.println(this.isAvailable);
        }
        if (this.lastDescription != null) {
            pw.print(prefix);
            pw.print("lastDescription=");
            pw.println(this.lastDescription);
        }
        if (this.mRootProcess != null) {
            pw.print(prefix);
            pw.print("mRootProcess=");
            pw.println(this.mRootProcess);
        }
        pw.print(prefix);
        pw.print("stackId=");
        pw.println(getStackId());
        sb = new StringBuilder();
        sb.append(prefix);
        sb.append("hasBeenVisible=");
        sb.append(this.hasBeenVisible);
        pw.print(sb.toString());
        sb = new StringBuilder();
        sb.append(" mResizeMode=");
        sb.append(ActivityInfo.resizeModeToString(this.mResizeMode));
        pw.print(sb.toString());
        sb = new StringBuilder();
        sb.append(" mSupportsPictureInPicture=");
        sb.append(this.mSupportsPictureInPicture);
        pw.print(sb.toString());
        sb = new StringBuilder();
        sb.append(" isResizeable=");
        sb.append(isResizeable());
        pw.print(sb.toString());
        sb = new StringBuilder();
        sb.append(" lastActiveTime=");
        sb.append(this.lastActiveTime);
        pw.print(sb.toString());
        sb = new StringBuilder();
        sb.append(" (inactive for ");
        sb.append(getInactiveDuration() / 1000);
        sb.append("s)");
        pw.println(sb.toString());
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        if (this.stringName != null) {
            sb.append(this.stringName);
            sb.append(" U=");
            sb.append(this.userId);
            sb.append(" StackId=");
            sb.append(getStackId());
            sb.append(" sz=");
            sb.append(this.mActivities.size());
            sb.append('}');
            return sb.toString();
        }
        sb.append("TaskRecord{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" #");
        sb.append(this.taskId);
        if (this.affinity != null) {
            sb.append(" A=");
            sb.append(this.affinity);
        } else if (this.intent != null) {
            sb.append(" I=");
            sb.append(this.intent.getComponent().flattenToShortString());
        } else if (this.affinityIntent == null || this.affinityIntent.getComponent() == null) {
            sb.append(" ??");
        } else {
            sb.append(" aI=");
            sb.append(this.affinityIntent.getComponent().flattenToShortString());
        }
        this.stringName = sb.toString();
        return toString();
    }

    public void writeToProto(ProtoOutputStream proto, long fieldId) {
        long token = proto.start(fieldId);
        super.writeToProto(proto, 1146756268033L, false);
        proto.write(1120986464258L, this.taskId);
        for (int i = this.mActivities.size() - 1; i >= 0; i--) {
            ((ActivityRecord) this.mActivities.get(i)).writeToProto(proto, 2246267895811L);
        }
        proto.write(1120986464260L, this.mStack.mStackId);
        if (this.mLastNonFullscreenBounds != null) {
            this.mLastNonFullscreenBounds.writeToProto(proto, 1146756268037L);
        }
        if (this.realActivity != null) {
            proto.write(1138166333446L, this.realActivity.flattenToShortString());
        }
        if (this.origActivity != null) {
            proto.write(1138166333447L, this.origActivity.flattenToShortString());
        }
        proto.write(1120986464264L, getActivityType());
        proto.write(1120986464265L, this.mResizeMode);
        proto.write(1133871366154L, matchParentBounds());
        if (!matchParentBounds()) {
            getOverrideBounds().writeToProto(proto, 1146756268043L);
        }
        proto.write(1120986464268L, this.mMinWidth);
        proto.write(1120986464269L, this.mMinHeight);
        proto.end(token);
    }

    void saveToXml(XmlSerializer out) throws IOException, XmlPullParserException {
        if (ActivityManagerDebugConfig.DEBUG_RECENTS) {
            String str = ActivityManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Saving task=");
            stringBuilder.append(this);
            Slog.i(str, stringBuilder.toString());
        }
        out.attribute(null, ATTR_TASKID, String.valueOf(this.taskId));
        if (this.realActivity != null) {
            out.attribute(null, ATTR_REALACTIVITY, this.realActivity.flattenToShortString());
        }
        out.attribute(null, ATTR_REALACTIVITY_SUSPENDED, String.valueOf(this.realActivitySuspended));
        if (this.origActivity != null) {
            out.attribute(null, ATTR_ORIGACTIVITY, this.origActivity.flattenToShortString());
        }
        if (this.affinity != null) {
            out.attribute(null, ATTR_AFFINITY, this.affinity);
            if (!this.affinity.equals(this.rootAffinity)) {
                out.attribute(null, ATTR_ROOT_AFFINITY, this.rootAffinity != null ? this.rootAffinity : "@");
            }
        } else if (this.rootAffinity != null) {
            out.attribute(null, ATTR_ROOT_AFFINITY, this.rootAffinity != null ? this.rootAffinity : "@");
        }
        out.attribute(null, ATTR_ROOTHASRESET, String.valueOf(this.rootWasReset));
        out.attribute(null, ATTR_AUTOREMOVERECENTS, String.valueOf(this.autoRemoveRecents));
        out.attribute(null, ATTR_ASKEDCOMPATMODE, String.valueOf(this.askedCompatMode));
        out.attribute(null, ATTR_USERID, String.valueOf(this.userId));
        out.attribute(null, ATTR_USER_SETUP_COMPLETE, String.valueOf(this.mUserSetupComplete));
        out.attribute(null, ATTR_EFFECTIVE_UID, String.valueOf(this.effectiveUid));
        out.attribute(null, ATTR_LASTTIMEMOVED, String.valueOf(this.mLastTimeMoved));
        out.attribute(null, ATTR_NEVERRELINQUISH, String.valueOf(this.mNeverRelinquishIdentity));
        if (this.lastDescription != null) {
            out.attribute(null, ATTR_LASTDESCRIPTION, this.lastDescription.toString());
        }
        if (this.lastTaskDescription != null) {
            this.lastTaskDescription.saveToXml(out);
        }
        out.attribute(null, ATTR_TASK_AFFILIATION_COLOR, String.valueOf(this.mAffiliatedTaskColor));
        out.attribute(null, ATTR_TASK_AFFILIATION, String.valueOf(this.mAffiliatedTaskId));
        out.attribute(null, ATTR_PREV_AFFILIATION, String.valueOf(this.mPrevAffiliateTaskId));
        out.attribute(null, ATTR_NEXT_AFFILIATION, String.valueOf(this.mNextAffiliateTaskId));
        out.attribute(null, ATTR_CALLING_UID, String.valueOf(this.mCallingUid));
        out.attribute(null, ATTR_CALLING_PACKAGE, this.mCallingPackage == null ? BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS : this.mCallingPackage);
        out.attribute(null, ATTR_RESIZE_MODE, String.valueOf(this.mResizeMode));
        out.attribute(null, ATTR_SUPPORTS_PICTURE_IN_PICTURE, String.valueOf(this.mSupportsPictureInPicture));
        if (this.mLastNonFullscreenBounds != null) {
            out.attribute(null, ATTR_NON_FULLSCREEN_BOUNDS, this.mLastNonFullscreenBounds.flattenToString());
        }
        out.attribute(null, ATTR_MIN_WIDTH, String.valueOf(this.mMinWidth));
        out.attribute(null, ATTR_MIN_HEIGHT, String.valueOf(this.mMinHeight));
        out.attribute(null, ATTR_PERSIST_TASK_VERSION, String.valueOf(1));
        if (this.affinityIntent != null) {
            out.startTag(null, TAG_AFFINITYINTENT);
            this.affinityIntent.saveToXml(out);
            out.endTag(null, TAG_AFFINITYINTENT);
        }
        if (this.intent != null) {
            out.startTag(null, "intent");
            this.intent.saveToXml(out);
            out.endTag(null, "intent");
        }
        ArrayList<ActivityRecord> activities = this.mActivities;
        int numActivities = activities.size();
        int activityNdx = 0;
        while (activityNdx < numActivities) {
            ActivityRecord r = (ActivityRecord) activities.get(activityNdx);
            if (r.info.persistableMode != 0 && r.isPersistable()) {
                if (((r.intent.getFlags() & DumpState.DUMP_FROZEN) | 8192) != DumpState.DUMP_FROZEN || activityNdx <= 0) {
                    out.startTag(null, TAG_ACTIVITY);
                    r.saveToXml(out);
                    out.endTag(null, TAG_ACTIVITY);
                    activityNdx++;
                } else {
                    return;
                }
            }
            return;
        }
    }

    @VisibleForTesting
    static TaskRecordFactory getTaskRecordFactory() {
        if (sTaskRecordFactory == null) {
            setTaskRecordFactory(new TaskRecordFactory());
        }
        return sTaskRecordFactory;
    }

    static void setTaskRecordFactory(TaskRecordFactory factory) {
        sTaskRecordFactory = factory;
    }

    static TaskRecord create(ActivityManagerService service, int taskId, ActivityInfo info, Intent intent, IVoiceInteractionSession voiceSession, IVoiceInteractor voiceInteractor) {
        return getTaskRecordFactory().create(service, taskId, info, intent, voiceSession, voiceInteractor);
    }

    static TaskRecord create(ActivityManagerService service, int taskId, ActivityInfo info, Intent intent, TaskDescription taskDescription) {
        return getTaskRecordFactory().create(service, taskId, info, intent, taskDescription);
    }

    static TaskRecord restoreFromXml(XmlPullParser in, ActivityStackSupervisor stackSupervisor) throws IOException, XmlPullParserException {
        return getTaskRecordFactory().restoreFromXml(in, stackSupervisor);
    }
}
