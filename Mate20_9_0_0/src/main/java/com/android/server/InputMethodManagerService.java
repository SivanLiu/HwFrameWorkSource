package com.android.server;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerInternal;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.AppGlobals;
import android.app.AppOpsManager;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageManager;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.database.ContentObserver;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.IBinder;
import android.os.IInterface;
import android.os.LocaleList;
import android.os.Message;
import android.os.Parcel;
import android.os.Process;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.ShellCallback;
import android.os.ShellCommand;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import android.service.vr.IVrManager;
import android.service.vr.IVrStateCallbacks;
import android.service.vr.IVrStateCallbacks.Stub;
import android.text.TextUtils;
import android.text.style.SuggestionSpan;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.EventLog;
import android.util.HwPCUtils;
import android.util.Log;
import android.util.LruCache;
import android.util.Pair;
import android.util.PrintWriterPrinter;
import android.util.Printer;
import android.util.Slog;
import android.util.Xml;
import android.view.ContextThemeWrapper;
import android.view.IWindowManager;
import android.view.InputChannel;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputBinding;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManagerInternal;
import android.view.inputmethod.InputMethodSubtype;
import android.view.inputmethod.InputMethodSubtype.InputMethodSubtypeBuilder;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.android.internal.R;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.content.PackageMonitor;
import com.android.internal.inputmethod.IInputContentUriToken;
import com.android.internal.inputmethod.InputMethodSubtypeSwitchingController;
import com.android.internal.inputmethod.InputMethodSubtypeSwitchingController.ImeSubtypeListItem;
import com.android.internal.inputmethod.InputMethodUtils;
import com.android.internal.inputmethod.InputMethodUtils.InputMethodSettings;
import com.android.internal.os.HandlerCaller;
import com.android.internal.os.SomeArgs;
import com.android.internal.os.TransferPipe;
import com.android.internal.util.DumpUtils;
import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.IndentingPrintWriter;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethod;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodSession;
import com.android.internal.view.IInputSessionCallback;
import com.android.internal.view.InputBindResult;
import com.android.internal.view.InputMethodClient;
import com.android.server.HwServiceFactory.IHwInputMethodManagerService;
import com.android.server.imm.IHwInputMethodManagerInner;
import com.android.server.imm.IHwInputMethodManagerServiceEx;
import com.android.server.pm.DumpState;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.wm.WindowManagerInternal;
import com.android.server.wm.WindowManagerInternal.OnHardKeyboardStatusChangeListener;
import com.huawei.android.inputmethod.IHwInputMethodManager;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.nio.charset.StandardCharsets;
import java.security.InvalidParameterException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

public class InputMethodManagerService extends AbsInputMethodManagerService implements IHwInputMethodManagerInner, ServiceConnection, Callback {
    private static final String ACTION_INPUT_METHOD_PICKER = "huawei.settings.ACTION_SHOW_INPUT_METHOD_PICKER";
    private static final String ACTION_SHOW_INPUT_METHOD_PICKER = "com.android.server.InputMethodManagerService.SHOW_INPUT_METHOD_PICKER";
    static final boolean DEBUG = false;
    static final boolean DEBUG_FLOW = Log.HWINFO;
    static final boolean DEBUG_RESTORE = false;
    private static final String DEVELOPER_CHANNEL = "DEVELOPER";
    private static final int IME_CONNECTION_BIND_FLAGS = 1082130437;
    private static final int IME_VISIBLE_BIND_FLAGS = 738197505;
    static final int MSG_ATTACH_TOKEN = 1040;
    static final int MSG_BIND_CLIENT = 3010;
    static final int MSG_BIND_INPUT = 1010;
    static final int MSG_CREATE_SESSION = 1050;
    static final int MSG_HARD_KEYBOARD_SWITCH_CHANGED = 4000;
    static final int MSG_HIDE_CURRENT_INPUT_METHOD = 1035;
    static final int MSG_HIDE_SOFT_INPUT = 1030;
    static final int MSG_REPORT_FULLSCREEN_MODE = 3045;
    static final int MSG_SET_ACTIVE = 3020;
    static final int MSG_SET_INTERACTIVE = 3030;
    static final int MSG_SET_USER_ACTION_NOTIFICATION_SEQUENCE_NUMBER = 3040;
    static final int MSG_SHOW_IM_CONFIG = 3;
    static final int MSG_SHOW_IM_SUBTYPE_ENABLER = 2;
    static final int MSG_SHOW_IM_SUBTYPE_PICKER = 1;
    static final int MSG_SHOW_SOFT_INPUT = 1020;
    static final int MSG_START_INPUT = 2000;
    static final int MSG_START_VR_INPUT = 2010;
    static final int MSG_SWITCH_IME = 3050;
    static final int MSG_SYSTEM_UNLOCK_USER = 5000;
    static final int MSG_UNBIND_CLIENT = 3000;
    static final int MSG_UNBIND_INPUT = 1000;
    private static final int NOT_A_SUBTYPE_ID = -1;
    static final int SECURE_SUGGESTION_SPANS_MAX_SIZE = 20;
    static final String TAG = "InputMethodManagerService";
    private static final String TAG_TRY_SUPPRESSING_IME_SWITCHER = "TrySuppressingImeSwitcher";
    static final long TIME_TO_RECONNECT = 3000;
    boolean bFlag = false;
    private boolean mAccessibilityRequestingNoSoftKeyboard;
    private final AppOpsManager mAppOpsManager;
    int mBackDisposition = 0;
    private boolean mBindInstantServiceAllowed = false;
    boolean mBoundToMethod;
    final HandlerCaller mCaller;
    final HashMap<IBinder, ClientState> mClients = new HashMap();
    final Context mContext;
    EditorInfo mCurAttribute;
    ClientState mCurClient;
    private boolean mCurClientInKeyguard;
    IBinder mCurFocusedWindow;
    ClientState mCurFocusedWindowClient;
    int mCurFocusedWindowSoftInputMode;
    String mCurId;
    IInputContext mCurInputContext;
    int mCurInputContextMissingMethods;
    private String mCurInputId;
    Intent mCurIntent;
    IInputMethod mCurMethod;
    String mCurMethodId;
    int mCurSeq;
    IBinder mCurToken;
    int mCurUserActionNotificationSequenceNumber = 0;
    private InputMethodSubtype mCurrentSubtype;
    private Builder mDialogBuilder;
    HashMap<String, Boolean> mEnabledFileMap = new HashMap();
    SessionState mEnabledSession;
    private InputMethodFileManager mFileManager;
    final Handler mHandler;
    private final int mHardKeyboardBehavior;
    private final HardKeyboardListener mHardKeyboardListener;
    final boolean mHasFeature;
    boolean mHaveConnection;
    IHwInputMethodManagerServiceEx mHwIMMSEx = null;
    HwInnerInputMethodManagerService mHwInnerService = new HwInnerInputMethodManagerService(this);
    private final IPackageManager mIPackageManager;
    final IWindowManager mIWindowManager;
    private PendingIntent mImeSwitchPendingIntent;
    private Notification.Builder mImeSwitcherNotification;
    int mImeWindowVis;
    private InputMethodInfo[] mIms;
    boolean mInFullscreenMode;
    boolean mInputShown;
    boolean mIsDiffIME;
    boolean mIsInteractive = true;
    private KeyguardManager mKeyguardManager;
    long mLastBindTime;
    private String mLastIME = null;
    boolean mLastInputShown;
    private LocaleList mLastSystemLocales;
    boolean mLastUnBindInputMethodInPCMode = false;
    final ArrayList<InputMethodInfo> mMethodList = new ArrayList();
    final HashMap<String, InputMethodInfo> mMethodMap = new HashMap();
    @GuardedBy("mMethodMap")
    private int mMethodMapUpdateCount = 0;
    private final MyPackageMonitor mMyPackageMonitor = new MyPackageMonitor();
    private NotificationManager mNotificationManager;
    private boolean mNotificationShown;
    final Resources mRes;
    private final LruCache<SuggestionSpan, InputMethodInfo> mSecureSuggestionSpans = new LruCache(20);
    final InputMethodSettings mSettings;
    final SettingsObserver mSettingsObserver;
    private final HashMap<InputMethodInfo, ArrayList<InputMethodSubtype>> mShortcutInputMethodsAndSubtypes = new HashMap();
    boolean mShowExplicitlyRequested;
    boolean mShowForced;
    private boolean mShowImeWithHardKeyboard;
    private boolean mShowOngoingImeSwitcherForPhones;
    boolean mShowRequested;
    private final String mSlotIme;
    @GuardedBy("mMethodMap")
    private final StartInputHistory mStartInputHistory = new StartInputHistory();
    @GuardedBy("mMethodMap")
    private final WeakHashMap<IBinder, StartInputInfo> mStartInputMap = new WeakHashMap();
    private StatusBarManagerService mStatusBar;
    private int[] mSubtypeIds;
    private Toast mSubtypeSwitchedByShortCutToast;
    private final InputMethodSubtypeSwitchingController mSwitchingController;
    private AlertDialog mSwitchingDialog;
    private View mSwitchingDialogTitleView;
    private IBinder mSwitchingDialogToken = new Binder();
    boolean mSystemReady;
    private final UserManager mUserManager;
    boolean mVisibleBound = false;
    final ServiceConnection mVisibleConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName name, IBinder service) {
        }

        public void onServiceDisconnected(ComponentName name) {
        }
    };
    private final IVrStateCallbacks mVrStateCallbacks = new Stub() {
        public void onVrStateChanged(boolean enabled) {
            if (!enabled) {
                InputMethodManagerService.this.restoreNonVrImeFromSettingsNoCheck();
            }
        }
    };
    final WindowManagerInternal mWindowManagerInternal;

    static final class ClientState {
        final InputBinding binding = new InputBinding(null, this.inputContext.asBinder(), this.uid, this.pid);
        final IInputMethodClient client;
        SessionState curSession;
        final IInputContext inputContext;
        final int pid;
        boolean sessionRequested;
        final int uid;

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ClientState{");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(this)));
            stringBuilder.append(" uid ");
            stringBuilder.append(this.uid);
            stringBuilder.append(" pid ");
            stringBuilder.append(this.pid);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        ClientState(IInputMethodClient _client, IInputContext _inputContext, int _uid, int _pid) {
            this.client = _client;
            this.inputContext = _inputContext;
            this.uid = _uid;
            this.pid = _pid;
        }
    }

    private static final class DebugFlag {
        private static final Object LOCK = new Object();
        private final boolean mDefaultValue;
        private final String mKey;
        @GuardedBy("LOCK")
        private boolean mValue;

        public DebugFlag(String key, boolean defaultValue) {
            this.mKey = key;
            this.mDefaultValue = defaultValue;
            this.mValue = SystemProperties.getBoolean(key, defaultValue);
        }

        void refresh() {
            synchronized (LOCK) {
                this.mValue = SystemProperties.getBoolean(this.mKey, this.mDefaultValue);
            }
        }

        boolean value() {
            boolean z;
            synchronized (LOCK) {
                z = this.mValue;
            }
            return z;
        }
    }

    private static final class DebugFlags {
        static final DebugFlag FLAG_OPTIMIZE_START_INPUT = new DebugFlag("debug.optimize_startinput", false);

        private DebugFlags() {
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface HardKeyboardBehavior {
        public static final int WIRED_AFFORDANCE = 1;
        public static final int WIRELESS_AFFORDANCE = 0;
    }

    public class HwInnerInputMethodManagerService extends IHwInputMethodManager.Stub {
        HwInnerInputMethodManagerService(InputMethodManagerService imms) {
        }

        public void setDefaultIme(String imeId) {
            InputMethodManagerService.this.mHwIMMSEx.setDefaultIme(imeId);
        }

        public void setInputSource(boolean isFingerTouch) {
            InputMethodManagerService.this.mHwIMMSEx.setInputSource(isFingerTouch);
        }
    }

    private static class ImeSubtypeListAdapter extends ArrayAdapter<ImeSubtypeListItem> {
        public int mCheckedItem;
        private final LayoutInflater mInflater;
        private final List<ImeSubtypeListItem> mItemsList;
        private final int mTextColorPri;
        private final int mTextColorSec;
        private final int mTextViewResourceId;

        public ImeSubtypeListAdapter(Context context, int textViewResourceId, List<ImeSubtypeListItem> itemsList, int checkedItem) {
            super(context, textViewResourceId, itemsList);
            this.mTextViewResourceId = textViewResourceId;
            this.mItemsList = itemsList;
            this.mCheckedItem = checkedItem;
            this.mInflater = (LayoutInflater) context.getSystemService(LayoutInflater.class);
            TypedArray array = context.obtainStyledAttributes(null, R.styleable.Theme, 0, 0);
            this.mTextColorPri = array.getColor(6, 0);
            this.mTextColorSec = array.getColor(8, 0);
            array.recycle();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView != null ? convertView : this.mInflater.inflate(this.mTextViewResourceId, null);
            if (position < 0 || position >= this.mItemsList.size()) {
                return view;
            }
            ImeSubtypeListItem item = (ImeSubtypeListItem) this.mItemsList.get(position);
            CharSequence imeName = item.mImeName;
            CharSequence subtypeName = item.mSubtypeName;
            TextView firstTextView = (TextView) view.findViewById(16908308);
            TextView secondTextView = (TextView) view.findViewById(16908309);
            if (this.mTextColorPri != 0) {
                firstTextView.setTextColor(this.mTextColorPri);
            }
            if (this.mTextColorSec != 0) {
                secondTextView.setTextColor(this.mTextColorSec);
            }
            boolean z = false;
            if (TextUtils.isEmpty(subtypeName)) {
                firstTextView.setText(imeName);
                secondTextView.setVisibility(8);
            } else {
                secondTextView.setText(subtypeName);
                firstTextView.setText(imeName);
                secondTextView.setVisibility(0);
            }
            RadioButton radioButton = (RadioButton) view.findViewById(16909232);
            if (position == this.mCheckedItem) {
                z = true;
            }
            radioButton.setChecked(z);
            return view;
        }
    }

    class ImmsBroadcastReceiver extends BroadcastReceiver {
        ImmsBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.intent.action.CLOSE_SYSTEM_DIALOGS".equals(action)) {
                InputMethodManagerService.this.hideInputMethodMenu();
            } else if ("android.intent.action.USER_ADDED".equals(action) || "android.intent.action.USER_REMOVED".equals(action)) {
                InputMethodManagerService.this.updateCurrentProfileIds();
            } else {
                if ("android.intent.action.LOCALE_CHANGED".equals(action)) {
                    synchronized (InputMethodManagerService.this.mMethodMap) {
                        InputMethodManagerService.this.mSettings.putSelectedSubtype(-1);
                    }
                    InputMethodManagerService.this.onActionLocaleChanged();
                } else if (InputMethodManagerService.ACTION_SHOW_INPUT_METHOD_PICKER.equals(action)) {
                    InputMethodManagerService.this.mHandler.obtainMessage(1, 1, 0).sendToTarget();
                } else {
                    String str = InputMethodManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unexpected intent ");
                    stringBuilder.append(intent);
                    Slog.w(str, stringBuilder.toString());
                }
            }
        }
    }

    private static class InputMethodFileManager {
        private static final String ADDITIONAL_SUBTYPES_FILE_NAME = "subtypes.xml";
        private static final String ATTR_ICON = "icon";
        private static final String ATTR_ID = "id";
        private static final String ATTR_IME_SUBTYPE_EXTRA_VALUE = "imeSubtypeExtraValue";
        private static final String ATTR_IME_SUBTYPE_ID = "subtypeId";
        private static final String ATTR_IME_SUBTYPE_LANGUAGE_TAG = "languageTag";
        private static final String ATTR_IME_SUBTYPE_LOCALE = "imeSubtypeLocale";
        private static final String ATTR_IME_SUBTYPE_MODE = "imeSubtypeMode";
        private static final String ATTR_IS_ASCII_CAPABLE = "isAsciiCapable";
        private static final String ATTR_IS_AUXILIARY = "isAuxiliary";
        private static final String ATTR_LABEL = "label";
        private static final String INPUT_METHOD_PATH = "inputmethod";
        private static final String NODE_IMI = "imi";
        private static final String NODE_SUBTYPE = "subtype";
        private static final String NODE_SUBTYPES = "subtypes";
        private static final String SYSTEM_PATH = "system";
        private final AtomicFile mAdditionalInputMethodSubtypeFile;
        private final HashMap<String, List<InputMethodSubtype>> mAdditionalSubtypesMap = new HashMap();
        private final HashMap<String, InputMethodInfo> mMethodMap;

        public InputMethodFileManager(HashMap<String, InputMethodInfo> methodMap, int userId) {
            if (methodMap != null) {
                File systemDir;
                this.mMethodMap = methodMap;
                if (userId == 0) {
                    systemDir = new File(Environment.getDataDirectory(), SYSTEM_PATH);
                } else {
                    systemDir = Environment.getUserSystemDirectory(userId);
                }
                File inputMethodDir = new File(systemDir, INPUT_METHOD_PATH);
                if (!(inputMethodDir.exists() || inputMethodDir.mkdirs())) {
                    String str = InputMethodManagerService.TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Couldn't create dir.: ");
                    stringBuilder.append(inputMethodDir.getAbsolutePath());
                    Slog.w(str, stringBuilder.toString());
                }
                File subtypeFile = new File(inputMethodDir, ADDITIONAL_SUBTYPES_FILE_NAME);
                this.mAdditionalInputMethodSubtypeFile = new AtomicFile(subtypeFile, "input-subtypes");
                if (subtypeFile.exists()) {
                    readAdditionalInputMethodSubtypes(this.mAdditionalSubtypesMap, this.mAdditionalInputMethodSubtypeFile);
                    return;
                } else {
                    writeAdditionalInputMethodSubtypes(this.mAdditionalSubtypesMap, this.mAdditionalInputMethodSubtypeFile, methodMap);
                    return;
                }
            }
            throw new NullPointerException("methodMap is null");
        }

        private void deleteAllInputMethodSubtypes(String imiId) {
            synchronized (this.mMethodMap) {
                this.mAdditionalSubtypesMap.remove(imiId);
                writeAdditionalInputMethodSubtypes(this.mAdditionalSubtypesMap, this.mAdditionalInputMethodSubtypeFile, this.mMethodMap);
            }
        }

        public void addInputMethodSubtypes(InputMethodInfo imi, InputMethodSubtype[] additionalSubtypes) {
            synchronized (this.mMethodMap) {
                ArrayList<InputMethodSubtype> subtypes = new ArrayList();
                for (InputMethodSubtype subtype : additionalSubtypes) {
                    if (subtypes.contains(subtype)) {
                        String str = InputMethodManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("Duplicated subtype definition found: ");
                        stringBuilder.append(subtype.getLocale());
                        stringBuilder.append(", ");
                        stringBuilder.append(subtype.getMode());
                        Slog.w(str, stringBuilder.toString());
                    } else {
                        subtypes.add(subtype);
                    }
                }
                this.mAdditionalSubtypesMap.put(imi.getId(), subtypes);
                writeAdditionalInputMethodSubtypes(this.mAdditionalSubtypesMap, this.mAdditionalInputMethodSubtypeFile, this.mMethodMap);
            }
        }

        public HashMap<String, List<InputMethodSubtype>> getAllAdditionalInputMethodSubtypes() {
            HashMap hashMap;
            synchronized (this.mMethodMap) {
                hashMap = this.mAdditionalSubtypesMap;
            }
            return hashMap;
        }

        private static void writeAdditionalInputMethodSubtypes(HashMap<String, List<InputMethodSubtype>> allSubtypes, AtomicFile subtypesFile, HashMap<String, InputMethodInfo> methodMap) {
            boolean isSetMethodMap = methodMap != null && methodMap.size() > 0;
            FileOutputStream fos = null;
            try {
                fos = subtypesFile.startWrite();
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(fos, StandardCharsets.UTF_8.name());
                out.startDocument(null, Boolean.valueOf(true));
                out.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
                out.startTag(null, NODE_SUBTYPES);
                for (String imiId : allSubtypes.keySet()) {
                    if (!isSetMethodMap || methodMap.containsKey(imiId)) {
                        out.startTag(null, NODE_IMI);
                        out.attribute(null, ATTR_ID, imiId);
                        List<InputMethodSubtype> subtypesList = (List) allSubtypes.get(imiId);
                        int N = subtypesList.size();
                        for (int i = 0; i < N; i++) {
                            InputMethodSubtype subtype = (InputMethodSubtype) subtypesList.get(i);
                            out.startTag(null, NODE_SUBTYPE);
                            if (subtype.hasSubtypeId()) {
                                out.attribute(null, ATTR_IME_SUBTYPE_ID, String.valueOf(subtype.getSubtypeId()));
                            }
                            out.attribute(null, ATTR_ICON, String.valueOf(subtype.getIconResId()));
                            out.attribute(null, ATTR_LABEL, String.valueOf(subtype.getNameResId()));
                            out.attribute(null, ATTR_IME_SUBTYPE_LOCALE, subtype.getLocale());
                            out.attribute(null, ATTR_IME_SUBTYPE_LANGUAGE_TAG, subtype.getLanguageTag());
                            out.attribute(null, ATTR_IME_SUBTYPE_MODE, subtype.getMode());
                            out.attribute(null, ATTR_IME_SUBTYPE_EXTRA_VALUE, subtype.getExtraValue());
                            out.attribute(null, ATTR_IS_AUXILIARY, String.valueOf(subtype.isAuxiliary()));
                            out.attribute(null, ATTR_IS_ASCII_CAPABLE, String.valueOf(subtype.isAsciiCapable()));
                            out.endTag(null, NODE_SUBTYPE);
                        }
                        out.endTag(null, NODE_IMI);
                    } else {
                        String str = InputMethodManagerService.TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("IME uninstalled or not valid.: ");
                        stringBuilder.append(imiId);
                        Slog.w(str, stringBuilder.toString());
                    }
                }
                out.endTag(null, NODE_SUBTYPES);
                out.endDocument();
                subtypesFile.finishWrite(fos);
            } catch (IOException e) {
                Slog.w(InputMethodManagerService.TAG, "Error writing subtypes", e);
                if (fos != null) {
                    subtypesFile.failWrite(fos);
                }
            }
        }

        /* JADX WARNING: Removed duplicated region for block: B:60:0x0182 A:{ExcHandler: Throwable (r0_3 'th' java.lang.Throwable), Splitter:B:7:0x0011} */
        /* JADX WARNING: Removed duplicated region for block: B:76:0x0199 A:{ExcHandler: IOException | NumberFormatException | XmlPullParserException (r0_6 'e' java.lang.Exception), Splitter:B:4:0x000b} */
        /* JADX WARNING: Removed duplicated region for block: B:76:0x0199 A:{ExcHandler: IOException | NumberFormatException | XmlPullParserException (r0_6 'e' java.lang.Exception), Splitter:B:4:0x000b} */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Failed to process nested try/catch */
        /* JADX WARNING: Missing block: B:58:0x017e, code skipped:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:59:0x017f, code skipped:
            r1 = r0;
            r4 = null;
     */
        /* JADX WARNING: Missing block: B:60:0x0182, code skipped:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:61:0x0183, code skipped:
            r4 = r0;
     */
        /* JADX WARNING: Missing block: B:63:?, code skipped:
            throw r4;
     */
        /* JADX WARNING: Missing block: B:66:0x0187, code skipped:
            if (r3 != null) goto L_0x0189;
     */
        /* JADX WARNING: Missing block: B:67:0x0189, code skipped:
            if (r4 != null) goto L_0x018b;
     */
        /* JADX WARNING: Missing block: B:69:?, code skipped:
            r3.close();
     */
        /* JADX WARNING: Missing block: B:74:0x0195, code skipped:
            r3.close();
     */
        /* JADX WARNING: Missing block: B:75:0x0198, code skipped:
            throw r1;
     */
        /* JADX WARNING: Missing block: B:76:0x0199, code skipped:
            r0 = move-exception;
     */
        /* JADX WARNING: Missing block: B:77:0x019a, code skipped:
            android.util.Slog.w(com.android.server.InputMethodManagerService.TAG, "Error reading subtypes", r0);
     */
        /* JADX WARNING: Missing block: B:78:0x01a1, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private static void readAdditionalInputMethodSubtypes(HashMap<String, List<InputMethodSubtype>> allSubtypes, AtomicFile subtypesFile) {
            HashMap hashMap = allSubtypes;
            if (hashMap != null && subtypesFile != null) {
                allSubtypes.clear();
                try {
                    FileInputStream fis = subtypesFile.openRead();
                    String str = null;
                    try {
                        int i;
                        int i2;
                        String firstNodeName;
                        XmlPullParser parser = Xml.newPullParser();
                        parser.setInput(fis, StandardCharsets.UTF_8.name());
                        int type = parser.getEventType();
                        while (true) {
                            int next = parser.next();
                            type = next;
                            i = 1;
                            i2 = 2;
                            if (next == 2 || type == 1) {
                                firstNodeName = parser.getName();
                            }
                        }
                        firstNodeName = parser.getName();
                        String firstNodeName2;
                        if (NODE_SUBTYPES.equals(firstNodeName)) {
                            int depth = parser.getDepth();
                            String currentImiId = null;
                            type = 0;
                            while (true) {
                                int next2 = parser.next();
                                int type2 = next2;
                                if (next2 == 3) {
                                    if (parser.getDepth() <= depth) {
                                        break;
                                    }
                                }
                                if (type2 == i) {
                                    break;
                                }
                                int depth2;
                                if (type2 != i2) {
                                    firstNodeName2 = firstNodeName;
                                    depth2 = depth;
                                } else {
                                    String nodeName = parser.getName();
                                    if (NODE_IMI.equals(nodeName)) {
                                        currentImiId = parser.getAttributeValue(str, ATTR_ID);
                                        if (TextUtils.isEmpty(currentImiId)) {
                                            Slog.w(InputMethodManagerService.TAG, "Invalid imi id found in subtypes.xml");
                                        } else {
                                            type = new ArrayList();
                                            hashMap.put(currentImiId, type);
                                            firstNodeName2 = firstNodeName;
                                            depth2 = depth;
                                        }
                                    } else if (NODE_SUBTYPE.equals(nodeName)) {
                                        if (TextUtils.isEmpty(currentImiId)) {
                                            firstNodeName2 = firstNodeName;
                                            depth2 = depth;
                                        } else if (type == 0) {
                                            firstNodeName2 = firstNodeName;
                                            depth2 = depth;
                                        } else {
                                            int icon = Integer.parseInt(parser.getAttributeValue(str, ATTR_ICON));
                                            int label = Integer.parseInt(parser.getAttributeValue(str, ATTR_LABEL));
                                            String imeSubtypeLocale = parser.getAttributeValue(str, ATTR_IME_SUBTYPE_LOCALE);
                                            String languageTag = parser.getAttributeValue(str, ATTR_IME_SUBTYPE_LANGUAGE_TAG);
                                            String imeSubtypeMode = parser.getAttributeValue(str, ATTR_IME_SUBTYPE_MODE);
                                            str = parser.getAttributeValue(null, ATTR_IME_SUBTYPE_EXTRA_VALUE);
                                            firstNodeName2 = firstNodeName;
                                            boolean isAuxiliary = "1".equals(String.valueOf(parser.getAttributeValue(null, ATTR_IS_AUXILIARY)));
                                            depth2 = depth;
                                            firstNodeName = new InputMethodSubtypeBuilder().setSubtypeNameResId(label).setSubtypeIconResId(icon).setSubtypeLocale(imeSubtypeLocale).setLanguageTag(languageTag).setSubtypeMode(imeSubtypeMode).setSubtypeExtraValue(str).setIsAuxiliary(isAuxiliary).setIsAsciiCapable("1".equals(String.valueOf(parser.getAttributeValue(0, ATTR_IS_ASCII_CAPABLE))));
                                            depth = parser.getAttributeValue(false, ATTR_IME_SUBTYPE_ID);
                                            if (depth != 0) {
                                                firstNodeName.setSubtypeId(Integer.parseInt(depth));
                                            }
                                            type.add(firstNodeName.build());
                                        }
                                        String str2 = InputMethodManagerService.TAG;
                                        StringBuilder stringBuilder = new StringBuilder();
                                        stringBuilder.append("IME uninstalled or not valid.: ");
                                        stringBuilder.append(currentImiId);
                                        Slog.w(str2, stringBuilder.toString());
                                    } else {
                                        firstNodeName2 = firstNodeName;
                                        depth2 = depth;
                                    }
                                    firstNodeName = firstNodeName2;
                                    depth = depth2;
                                    hashMap = allSubtypes;
                                    str = null;
                                    i = 1;
                                    i2 = 2;
                                }
                                firstNodeName = firstNodeName2;
                                depth = depth2;
                                hashMap = allSubtypes;
                                str = null;
                                i = 1;
                                i2 = 2;
                            }
                            if (fis != null) {
                                fis.close();
                            }
                            return;
                        }
                        firstNodeName2 = firstNodeName;
                        throw new XmlPullParserException("Xml doesn't start with subtypes");
                    } catch (Throwable th) {
                        Throwable th2 = th;
                    }
                } catch (IOException | NumberFormatException | XmlPullParserException e) {
                } catch (Throwable th3) {
                    r4.addSuppressed(th3);
                }
            }
        }
    }

    private static final class LocalServiceImpl implements InputMethodManagerInternal {
        private final Handler mHandler;

        LocalServiceImpl(Handler handler) {
            this.mHandler = handler;
        }

        public void setInteractive(boolean interactive) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(InputMethodManagerService.MSG_SET_INTERACTIVE, interactive, 0));
        }

        public void switchInputMethod(boolean forwardDirection) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(3050, forwardDirection, 0));
        }

        public void hideCurrentInputMethod() {
            this.mHandler.removeMessages(InputMethodManagerService.MSG_HIDE_CURRENT_INPUT_METHOD);
            this.mHandler.sendEmptyMessage(InputMethodManagerService.MSG_HIDE_CURRENT_INPUT_METHOD);
        }

        public void startVrInputMethodNoCheck(ComponentName componentName) {
            this.mHandler.sendMessage(this.mHandler.obtainMessage(InputMethodManagerService.MSG_START_VR_INPUT, componentName));
        }
    }

    private static final class MethodCallback extends IInputSessionCallback.Stub {
        private final InputChannel mChannel;
        private final IInputMethod mMethod;
        private final InputMethodManagerService mParentIMMS;

        MethodCallback(InputMethodManagerService imms, IInputMethod method, InputChannel channel) {
            this.mParentIMMS = imms;
            this.mMethod = method;
            this.mChannel = channel;
        }

        public void sessionCreated(IInputMethodSession session) {
            long ident = Binder.clearCallingIdentity();
            try {
                this.mParentIMMS.onSessionCreated(this.mMethod, session, this.mChannel);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    final class MyPackageMonitor extends PackageMonitor {
        private final ArrayList<String> mChangedPackages = new ArrayList();
        private boolean mImePackageAppeared = false;
        @GuardedBy("mMethodMap")
        private final ArraySet<String> mKnownImePackageNames = new ArraySet();

        MyPackageMonitor() {
        }

        @GuardedBy("mMethodMap")
        void clearKnownImePackageNamesLocked() {
            this.mKnownImePackageNames.clear();
        }

        @GuardedBy("mMethodMap")
        final void addKnownImePackageNameLocked(String packageName) {
            this.mKnownImePackageNames.add(packageName);
        }

        @GuardedBy("mMethodMap")
        private boolean isChangingPackagesOfCurrentUserLocked() {
            return getChangingUserId() == InputMethodManagerService.this.mSettings.getCurrentUserId();
        }

        /* JADX WARNING: Missing block: B:26:0x0055, code skipped:
            return false;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean onHandleForceStop(Intent intent, String[] packages, int uid, boolean doit) {
            synchronized (InputMethodManagerService.this.mMethodMap) {
                if (isChangingPackagesOfCurrentUserLocked()) {
                    String curInputMethodId = InputMethodManagerService.this.mSettings.getSelectedInputMethod();
                    int N = InputMethodManagerService.this.mMethodList.size();
                    if (curInputMethodId != null) {
                        for (int i = 0; i < N; i++) {
                            InputMethodInfo imi = (InputMethodInfo) InputMethodManagerService.this.mMethodList.get(i);
                            if (imi.getId().equals(curInputMethodId)) {
                                int length = packages.length;
                                int i2 = 0;
                                while (i2 < length) {
                                    if (!imi.getPackageName().equals(packages[i2])) {
                                        i2++;
                                    } else if (doit) {
                                        return true;
                                    } else {
                                        return true;
                                    }
                                }
                                continue;
                            }
                        }
                    }
                } else {
                    return false;
                }
            }
        }

        public void onBeginPackageChanges() {
            clearPackageChangeState();
        }

        public void onPackageAppeared(String packageName, int reason) {
            if (!(this.mImePackageAppeared || InputMethodManagerService.this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent("android.view.InputMethod").setPackage(packageName), InputMethodManagerService.this.getComponentMatchingFlags(512), getChangingUserId()).isEmpty())) {
                this.mImePackageAppeared = true;
            }
            this.mChangedPackages.add(packageName);
        }

        public void onPackageDisappeared(String packageName, int reason) {
            this.mChangedPackages.add(packageName);
        }

        public void onPackageModified(String packageName) {
            this.mChangedPackages.add(packageName);
        }

        public void onPackagesSuspended(String[] packages) {
            for (String packageName : packages) {
                this.mChangedPackages.add(packageName);
            }
        }

        public void onPackagesUnsuspended(String[] packages) {
            for (String packageName : packages) {
                this.mChangedPackages.add(packageName);
            }
        }

        public void onFinishPackageChanges() {
            onFinishPackageChangesInternal();
            clearPackageChangeState();
        }

        private void clearPackageChangeState() {
            this.mChangedPackages.clear();
            this.mImePackageAppeared = false;
        }

        @GuardedBy("mMethodMap")
        private boolean shouldRebuildInputMethodListLocked() {
            if (this.mImePackageAppeared) {
                return true;
            }
            int N = this.mChangedPackages.size();
            for (int i = 0; i < N; i++) {
                if (this.mKnownImePackageNames.contains((String) this.mChangedPackages.get(i))) {
                    return true;
                }
            }
            return false;
        }

        /* JADX WARNING: Missing block: B:50:0x0114, code skipped:
            return;
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void onFinishPackageChangesInternal() {
            synchronized (InputMethodManagerService.this.mMethodMap) {
                if (!isChangingPackagesOfCurrentUserLocked()) {
                } else if (shouldRebuildInputMethodListLocked()) {
                    InputMethodInfo curIm = null;
                    String curInputMethodId = InputMethodManagerService.this.mSettings.getSelectedInputMethod();
                    int N = InputMethodManagerService.this.mMethodList.size();
                    if (curInputMethodId != null) {
                        InputMethodInfo curIm2 = null;
                        for (int i = 0; i < N; i++) {
                            InputMethodInfo imi = (InputMethodInfo) InputMethodManagerService.this.mMethodList.get(i);
                            String imiId = imi.getId();
                            if (imiId.equals(curInputMethodId)) {
                                curIm2 = imi;
                            }
                            int change = isPackageDisappearing(imi.getPackageName());
                            if (isPackageModified(imi.getPackageName())) {
                                InputMethodManagerService.this.mFileManager.deleteAllInputMethodSubtypes(imiId);
                            }
                            if (change == 2 || change == 3) {
                                String str = InputMethodManagerService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Input method uninstalled, disabling: ");
                                stringBuilder.append(imi.getComponent());
                                Slog.i(str, stringBuilder.toString());
                                InputMethodManagerService.this.setInputMethodEnabledLocked(imi.getId(), false);
                            }
                        }
                        curIm = curIm2;
                    }
                    InputMethodManagerService.this.buildInputMethodListLocked(false);
                    boolean changed = false;
                    if (curIm != null) {
                        int change2 = isPackageDisappearing(curIm.getPackageName());
                        if (change2 == 2 || change2 == 3) {
                            ServiceInfo si = null;
                            try {
                                si = InputMethodManagerService.this.mIPackageManager.getServiceInfo(curIm.getComponent(), 0, InputMethodManagerService.this.mSettings.getCurrentUserId());
                            } catch (RemoteException e) {
                            }
                            if (si == null) {
                                String str2 = InputMethodManagerService.TAG;
                                StringBuilder stringBuilder2 = new StringBuilder();
                                stringBuilder2.append("Current input method removed: ");
                                stringBuilder2.append(curInputMethodId);
                                Slog.i(str2, stringBuilder2.toString());
                                InputMethodManagerService.this.updateSystemUiLocked(InputMethodManagerService.this.mCurToken, 0, InputMethodManagerService.this.mBackDisposition);
                                if (!InputMethodManagerService.this.chooseNewDefaultIMELocked()) {
                                    changed = true;
                                    curIm = null;
                                    Slog.i(InputMethodManagerService.TAG, "Unsetting current input method");
                                    InputMethodManagerService.this.resetSelectedInputMethodAndSubtypeLocked(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                                }
                            }
                        }
                    }
                    if (curIm == null) {
                        changed = InputMethodManagerService.this.chooseNewDefaultIMELocked();
                    } else if (!changed && isPackageModified(curIm.getPackageName())) {
                        changed = true;
                    }
                    if (changed) {
                        InputMethodManagerService.this.updateFromSettingsLocked(false);
                    }
                }
            }
        }

        public boolean onPackageChanged(String packageName, int uid, String[] components) {
            onSomePackagesChanged();
            if (components != null) {
                for (String name : components) {
                    if (packageName.equals(name)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    static class SessionState {
        InputChannel channel;
        final ClientState client;
        final IInputMethod method;
        IInputMethodSession session;

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SessionState{uid ");
            stringBuilder.append(this.client.uid);
            stringBuilder.append(" pid ");
            stringBuilder.append(this.client.pid);
            stringBuilder.append(" method ");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(this.method)));
            stringBuilder.append(" session ");
            stringBuilder.append(Integer.toHexString(System.identityHashCode(this.session)));
            stringBuilder.append(" channel ");
            stringBuilder.append(this.channel);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }

        SessionState(ClientState _client, IInputMethod _method, IInputMethodSession _session, InputChannel _channel) {
            this.client = _client;
            this.method = _method;
            this.session = _session;
            this.channel = _channel;
        }
    }

    class SettingsObserver extends ContentObserver {
        String mLastEnabled = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        boolean mRegistered = false;
        int mUserId;

        SettingsObserver(Handler handler) {
            super(handler);
        }

        public void registerContentObserverLocked(int userId) {
            if (!this.mRegistered || this.mUserId != userId) {
                ContentResolver resolver = InputMethodManagerService.this.mContext.getContentResolver();
                if (this.mRegistered) {
                    InputMethodManagerService.this.mContext.getContentResolver().unregisterContentObserver(this);
                    this.mRegistered = false;
                }
                if (this.mUserId != userId) {
                    this.mLastEnabled = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    this.mUserId = userId;
                }
                resolver.registerContentObserver(Secure.getUriFor("default_input_method"), false, this, userId);
                resolver.registerContentObserver(Secure.getUriFor("enabled_input_methods"), false, this, userId);
                resolver.registerContentObserver(Secure.getUriFor("selected_input_method_subtype"), false, this, userId);
                resolver.registerContentObserver(Secure.getUriFor("show_ime_with_hard_keyboard"), false, this, userId);
                resolver.registerContentObserver(Secure.getUriFor("accessibility_soft_keyboard_mode"), false, this, userId);
                this.mRegistered = true;
            }
        }

        public void onChange(boolean selfChange, Uri uri) {
            Uri showImeUri = Secure.getUriFor("show_ime_with_hard_keyboard");
            Uri accessibilityRequestingNoImeUri = Secure.getUriFor("accessibility_soft_keyboard_mode");
            synchronized (InputMethodManagerService.this.mMethodMap) {
                boolean showRequested;
                if (showImeUri.equals(uri)) {
                    InputMethodManagerService.this.updateKeyboardFromSettingsLocked();
                } else if (accessibilityRequestingNoImeUri.equals(uri)) {
                    InputMethodManagerService.this.mAccessibilityRequestingNoSoftKeyboard = Secure.getIntForUser(InputMethodManagerService.this.mContext.getContentResolver(), "accessibility_soft_keyboard_mode", 0, this.mUserId) == 1;
                    if (InputMethodManagerService.this.mAccessibilityRequestingNoSoftKeyboard) {
                        showRequested = InputMethodManagerService.this.mShowRequested;
                        InputMethodManagerService.this.hideCurrentInputLocked(0, null);
                        InputMethodManagerService.this.mShowRequested = showRequested;
                    } else if (InputMethodManagerService.this.mShowRequested) {
                        InputMethodManagerService.this.showCurrentInputLocked(1, null);
                    }
                } else {
                    showRequested = false;
                    String newEnabled = InputMethodManagerService.this.mSettings.getEnabledInputMethodsStr();
                    if (this.mLastEnabled == null) {
                        this.mLastEnabled = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
                    }
                    if (!this.mLastEnabled.equals(newEnabled)) {
                        this.mLastEnabled = newEnabled;
                        showRequested = true;
                    }
                    InputMethodManagerService.this.updateInputMethodsFromSettingsLocked(showRequested);
                }
            }
        }

        public String toString() {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SettingsObserver{mUserId=");
            stringBuilder.append(this.mUserId);
            stringBuilder.append(" mRegistered=");
            stringBuilder.append(this.mRegistered);
            stringBuilder.append(" mLastEnabled=");
            stringBuilder.append(this.mLastEnabled);
            stringBuilder.append("}");
            return stringBuilder.toString();
        }
    }

    private static final class ShellCommandImpl extends ShellCommand {
        final InputMethodManagerService mService;

        ShellCommandImpl(InputMethodManagerService service) {
            this.mService = service;
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int onCommand(String cmd) {
            if ("refresh_debug_properties".equals(cmd)) {
                return refreshDebugProperties();
            }
            if ("set-bind-instant-service-allowed".equals(cmd)) {
                return setBindInstantServiceAllowed();
            }
            if (!"ime".equals(cmd)) {
                return handleDefaultCommands(cmd);
            }
            String imeCommand = getNextArg();
            if (imeCommand == null || "help".equals(imeCommand) || "-h".equals(imeCommand)) {
                onImeCommandHelp();
                return 0;
            }
            boolean z;
            switch (imeCommand.hashCode()) {
                case -1298848381:
                    if (imeCommand.equals("enable")) {
                        z = true;
                        break;
                    }
                case 113762:
                    if (imeCommand.equals("set")) {
                        z = true;
                        break;
                    }
                case 3322014:
                    if (imeCommand.equals("list")) {
                        z = false;
                        break;
                    }
                case 108404047:
                    if (imeCommand.equals("reset")) {
                        z = true;
                        break;
                    }
                case 1671308008:
                    if (imeCommand.equals("disable")) {
                        z = true;
                        break;
                    }
                default:
                    z = true;
                    break;
            }
            switch (z) {
                case false:
                    return this.mService.handleShellCommandListInputMethods(this);
                case true:
                    return this.mService.handleShellCommandEnableDisableInputMethod(this, true);
                case true:
                    return this.mService.handleShellCommandEnableDisableInputMethod(this, false);
                case true:
                    return this.mService.handleShellCommandSetInputMethod(this);
                case true:
                    return this.mService.handleShellCommandResetInputMethod(this);
                default:
                    PrintWriter outPrintWriter = getOutPrintWriter();
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Unknown command: ");
                    stringBuilder.append(imeCommand);
                    outPrintWriter.println(stringBuilder.toString());
                    return -1;
            }
        }

        private int setBindInstantServiceAllowed() {
            return this.mService.handleSetBindInstantServiceAllowed(this);
        }

        private int refreshDebugProperties() {
            DebugFlags.FLAG_OPTIMIZE_START_INPUT.refresh();
            return 0;
        }

        /* JADX WARNING: Missing block: B:9:0x003c, code skipped:
            if (r0 != null) goto L_0x003e;
     */
        /* JADX WARNING: Missing block: B:10:0x003e, code skipped:
            $closeResource(r1, r0);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void onHelp() {
            PrintWriter pw = getOutPrintWriter();
            pw.println("InputMethodManagerService commands:");
            pw.println("  help");
            pw.println("    Prints this help text.");
            pw.println("  dump [options]");
            pw.println("    Synonym of dumpsys.");
            pw.println("  ime <command> [options]");
            pw.println("    Manipulate IMEs.  Run \"ime help\" for details.");
            pw.println("  set-bind-instant-service-allowed true|false ");
            pw.println("    Set whether binding to services provided by instant apps is allowed.");
            if (pw != null) {
                $closeResource(null, pw);
            }
        }

        private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
            if (x0 != null) {
                try {
                    x1.close();
                    return;
                } catch (Throwable th) {
                    x0.addSuppressed(th);
                    return;
                }
            }
            x1.close();
        }

        /* JADX WARNING: Missing block: B:9:0x0088, code skipped:
            $closeResource(r1, r0);
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        private void onImeCommandHelp() {
            IndentingPrintWriter pw = new IndentingPrintWriter(getOutPrintWriter(), "  ", 100);
            pw.println("ime <command>:");
            pw.increaseIndent();
            pw.println("list [-a] [-s]");
            pw.increaseIndent();
            pw.println("prints all enabled input methods.");
            pw.increaseIndent();
            pw.println("-a: see all input methods");
            pw.println("-s: only a single summary line of each");
            pw.decreaseIndent();
            pw.decreaseIndent();
            pw.println("enable <ID>");
            pw.increaseIndent();
            pw.println("allows the given input method ID to be used.");
            pw.decreaseIndent();
            pw.println("disable <ID>");
            pw.increaseIndent();
            pw.println("disallows the given input method ID to be used.");
            pw.decreaseIndent();
            pw.println("set <ID>");
            pw.increaseIndent();
            pw.println("switches to the given input method ID.");
            pw.decreaseIndent();
            pw.println("reset");
            pw.increaseIndent();
            pw.println("reset currently selected/enabled IMEs to the default ones as if the device is initially booted with the current locale.");
            pw.decreaseIndent();
            pw.decreaseIndent();
            $closeResource(null, pw);
        }
    }

    @Retention(RetentionPolicy.SOURCE)
    private @interface ShellCommandResult {
        public static final int FAILURE = -1;
        public static final int SUCCESS = 0;
    }

    private static final class StartInputHistory {
        private static final int ENTRY_SIZE_FOR_HIGH_RAM_DEVICE = 16;
        private static final int ENTRY_SIZE_FOR_LOW_RAM_DEVICE = 5;
        private final Entry[] mEntries;
        private int mNextIndex;

        private static final class Entry {
            int mClientBindSequenceNumber;
            EditorInfo mEditorInfo;
            String mImeId;
            String mImeTokenString;
            boolean mRestarting;
            int mSequenceNumber;
            int mStartInputReason;
            int mTargetWindowSoftInputMode;
            String mTargetWindowString;
            long mTimestamp;
            long mWallTime;

            Entry(StartInputInfo original) {
                set(original);
            }

            void set(StartInputInfo original) {
                this.mSequenceNumber = original.mSequenceNumber;
                this.mTimestamp = original.mTimestamp;
                this.mWallTime = original.mWallTime;
                this.mImeTokenString = String.valueOf(original.mImeToken);
                this.mImeId = original.mImeId;
                this.mStartInputReason = original.mStartInputReason;
                this.mRestarting = original.mRestarting;
                this.mTargetWindowString = String.valueOf(original.mTargetWindow);
                this.mEditorInfo = original.mEditorInfo;
                this.mTargetWindowSoftInputMode = original.mTargetWindowSoftInputMode;
                this.mClientBindSequenceNumber = original.mClientBindSequenceNumber;
            }
        }

        private StartInputHistory() {
            this.mEntries = new Entry[getEntrySize()];
            this.mNextIndex = 0;
        }

        /* synthetic */ StartInputHistory(AnonymousClass1 x0) {
            this();
        }

        private static int getEntrySize() {
            if (ActivityManager.isLowRamDeviceStatic()) {
                return 5;
            }
            return 16;
        }

        void addEntry(StartInputInfo info) {
            int index = this.mNextIndex;
            if (this.mEntries[index] == null) {
                this.mEntries[index] = new Entry(info);
            } else {
                this.mEntries[index].set(info);
            }
            this.mNextIndex = (this.mNextIndex + 1) % this.mEntries.length;
        }

        void dump(PrintWriter pw, String prefix) {
            SimpleDateFormat dataFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
            for (int i = 0; i < this.mEntries.length; i++) {
                Entry entry = this.mEntries[(this.mNextIndex + i) % this.mEntries.length];
                if (entry != null) {
                    pw.print(prefix);
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("StartInput #");
                    stringBuilder.append(entry.mSequenceNumber);
                    stringBuilder.append(":");
                    pw.println(stringBuilder.toString());
                    pw.print(prefix);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" time=");
                    stringBuilder.append(dataFormat.format(new Date(entry.mWallTime)));
                    stringBuilder.append(" (timestamp=");
                    stringBuilder.append(entry.mTimestamp);
                    stringBuilder.append(") reason=");
                    stringBuilder.append(InputMethodClient.getStartInputReason(entry.mStartInputReason));
                    stringBuilder.append(" restarting=");
                    stringBuilder.append(entry.mRestarting);
                    pw.println(stringBuilder.toString());
                    pw.print(prefix);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" imeToken=");
                    stringBuilder.append(entry.mImeTokenString);
                    stringBuilder.append(" [");
                    stringBuilder.append(entry.mImeId);
                    stringBuilder.append("]");
                    pw.println(stringBuilder.toString());
                    pw.print(prefix);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" targetWin=");
                    stringBuilder.append(entry.mTargetWindowString);
                    stringBuilder.append(" [");
                    stringBuilder.append(entry.mEditorInfo.packageName);
                    stringBuilder.append("] clientBindSeq=");
                    stringBuilder.append(entry.mClientBindSequenceNumber);
                    pw.println(stringBuilder.toString());
                    pw.print(prefix);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" softInputMode=");
                    stringBuilder.append(InputMethodClient.softInputModeToString(entry.mTargetWindowSoftInputMode));
                    pw.println(stringBuilder.toString());
                    pw.print(prefix);
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(" inputType=0x");
                    stringBuilder.append(Integer.toHexString(entry.mEditorInfo.inputType));
                    stringBuilder.append(" imeOptions=0x");
                    stringBuilder.append(Integer.toHexString(entry.mEditorInfo.imeOptions));
                    stringBuilder.append(" fieldId=0x");
                    stringBuilder.append(Integer.toHexString(entry.mEditorInfo.fieldId));
                    stringBuilder.append(" fieldName=");
                    stringBuilder.append(entry.mEditorInfo.fieldName);
                    stringBuilder.append(" actionId=");
                    stringBuilder.append(entry.mEditorInfo.actionId);
                    stringBuilder.append(" actionLabel=");
                    stringBuilder.append(entry.mEditorInfo.actionLabel);
                    pw.println(stringBuilder.toString());
                }
            }
        }
    }

    private static class StartInputInfo {
        private static final AtomicInteger sSequenceNumber = new AtomicInteger(0);
        final int mClientBindSequenceNumber;
        final EditorInfo mEditorInfo;
        final String mImeId;
        final IBinder mImeToken;
        final boolean mRestarting;
        final int mSequenceNumber = sSequenceNumber.getAndIncrement();
        final int mStartInputReason;
        final IBinder mTargetWindow;
        final int mTargetWindowSoftInputMode;
        final long mTimestamp = SystemClock.uptimeMillis();
        final long mWallTime = System.currentTimeMillis();

        StartInputInfo(IBinder imeToken, String imeId, int startInputReason, boolean restarting, IBinder targetWindow, EditorInfo editorInfo, int targetWindowSoftInputMode, int clientBindSequenceNumber) {
            this.mImeToken = imeToken;
            this.mImeId = imeId;
            this.mStartInputReason = startInputReason;
            this.mRestarting = restarting;
            this.mTargetWindow = targetWindow;
            this.mEditorInfo = editorInfo;
            this.mTargetWindowSoftInputMode = targetWindowSoftInputMode;
            this.mClientBindSequenceNumber = clientBindSequenceNumber;
        }
    }

    private class HardKeyboardListener implements OnHardKeyboardStatusChangeListener {
        private HardKeyboardListener() {
        }

        /* synthetic */ HardKeyboardListener(InputMethodManagerService x0, AnonymousClass1 x1) {
            this();
        }

        public void onHardKeyboardStatusChange(boolean available) {
            InputMethodManagerService.this.mHandler.sendMessage(InputMethodManagerService.this.mHandler.obtainMessage(InputMethodManagerService.MSG_HARD_KEYBOARD_SWITCH_CHANGED, Integer.valueOf(available)));
        }

        public void handleHardKeyboardStatusChange(boolean available) {
            synchronized (InputMethodManagerService.this.mMethodMap) {
                if (!(InputMethodManagerService.this.mSwitchingDialog == null || InputMethodManagerService.this.mSwitchingDialogTitleView == null || !InputMethodManagerService.this.mSwitchingDialog.isShowing())) {
                    View switchSectionView = InputMethodManagerService.this.mSwitchingDialogTitleView.findViewById(34603134);
                    if (switchSectionView != null) {
                        switchSectionView.setVisibility(available ? 0 : 8);
                    }
                }
            }
        }
    }

    public static final class Lifecycle extends SystemService {
        private InputMethodManagerService mService;

        public Lifecycle(Context context) {
            super(context);
            IHwInputMethodManagerService iinputmethodMS = HwServiceFactory.getHwInputMethodManagerService();
            if (iinputmethodMS != null) {
                this.mService = iinputmethodMS.getInstance(context);
            } else {
                this.mService = new InputMethodManagerService(context);
            }
        }

        public void onStart() {
            LocalServices.addService(InputMethodManagerInternal.class, new LocalServiceImpl(this.mService.mHandler));
            publishBinderService("input_method", this.mService);
        }

        public void onSwitchUser(int userHandle) {
            this.mService.onSwitchUser(userHandle);
        }

        public void onBootPhase(int phase) {
            if (phase == 550) {
                this.mService.systemRunning((StatusBarManagerService) ServiceManager.getService("statusbar"));
            }
        }

        public void onUnlockUser(int userHandle) {
            this.mService.mHandler.sendMessage(this.mService.mHandler.obtainMessage(InputMethodManagerService.MSG_SYSTEM_UNLOCK_USER, userHandle, 0));
        }
    }

    private void restoreNonVrImeFromSettingsNoCheck() {
        synchronized (this.mMethodMap) {
            String lastInputId = this.mSettings.getSelectedInputMethod();
            setInputMethodLocked(lastInputId, this.mSettings.getSelectedInputMethodSubtypeId(lastInputId));
        }
    }

    private void startVrInputMethodNoCheck(ComponentName component) {
        if (component == null) {
            restoreNonVrImeFromSettingsNoCheck();
            return;
        }
        synchronized (this.mMethodMap) {
            String packageName = component.getPackageName();
            Iterator it = this.mMethodList.iterator();
            while (it.hasNext()) {
                InputMethodInfo info = (InputMethodInfo) it.next();
                if (TextUtils.equals(info.getPackageName(), packageName) && info.isVrOnly()) {
                    setInputMethodEnabledLocked(info.getId(), true);
                    setInputMethodLocked(info.getId(), -1);
                    break;
                }
            }
        }
    }

    void onActionLocaleChanged() {
        synchronized (this.mMethodMap) {
            LocaleList possibleNewLocale = this.mRes.getConfiguration().getLocales();
            if (possibleNewLocale == null || !possibleNewLocale.equals(this.mLastSystemLocales)) {
                buildInputMethodListLocked(true);
                resetDefaultImeLocked(this.mContext);
                updateFromSettingsLocked(true);
                this.mLastSystemLocales = possibleNewLocale;
                return;
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x0022, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void onUnlockUser(int userId) {
        synchronized (this.mMethodMap) {
            int currentUserId = this.mSettings.getCurrentUserId();
            if (userId != currentUserId) {
                return;
            }
            this.mSettings.switchCurrentUser(currentUserId, this.mSystemReady ^ 1);
            if (this.mSystemReady) {
                buildInputMethodListLocked(false);
                updateInputMethodsFromSettingsLocked(true);
            }
        }
    }

    void onSwitchUser(int userId) {
        synchronized (this.mMethodMap) {
            switchUserLocked(userId);
        }
    }

    public InputMethodManagerService(Context context) {
        Context context2 = context;
        this.mHwIMMSEx = HwServiceExFactory.getHwInputMethodManagerServiceEx(this, context);
        this.mIPackageManager = AppGlobals.getPackageManager();
        this.mContext = context2;
        this.mRes = context.getResources();
        this.mHandler = new Handler(this);
        this.mSettingsObserver = new SettingsObserver(this.mHandler);
        this.mIWindowManager = IWindowManager.Stub.asInterface(ServiceManager.getService("window"));
        this.mWindowManagerInternal = (WindowManagerInternal) LocalServices.getService(WindowManagerInternal.class);
        this.mCaller = new HandlerCaller(context2, null, new HandlerCaller.Callback() {
            public void executeMessage(Message msg) {
                InputMethodManagerService.this.handleMessage(msg);
            }
        }, true);
        this.mAppOpsManager = (AppOpsManager) this.mContext.getSystemService(AppOpsManager.class);
        this.mUserManager = (UserManager) this.mContext.getSystemService(UserManager.class);
        this.mHardKeyboardListener = new HardKeyboardListener(this, null);
        this.mHasFeature = context.getPackageManager().hasSystemFeature("android.software.input_methods");
        this.mSlotIme = this.mContext.getString(17041187);
        this.mHardKeyboardBehavior = this.mContext.getResources().getInteger(17694786);
        Bundle extras = new Bundle();
        extras.putBoolean("android.allowDuringSetup", true);
        this.mImeSwitcherNotification = new Notification.Builder(this.mContext, DEVELOPER_CHANNEL).setSmallIcon(33751156).setWhen(0).setOngoing(true).addExtras(extras).setCategory("sys");
        Intent intent = new Intent(ACTION_INPUT_METHOD_PICKER).setPackage(this.mContext.getPackageName());
        intent.setComponent(new ComponentName("com.android.settings", "com.android.settings.inputmethod.InputMethodDialogReceiver"));
        this.mImeSwitchPendingIntent = PendingIntent.getBroadcast(this.mContext, 0, intent, 0);
        this.mShowOngoingImeSwitcherForPhones = false;
        this.mNotificationShown = false;
        int userId = 0;
        try {
            userId = ActivityManager.getService().getCurrentUser().id;
        } catch (RemoteException e) {
            Slog.w(TAG, "Couldn't get current user ID; guessing it's 0", e);
        }
        this.mSettings = new InputMethodSettings(this.mRes, context.getContentResolver(), this.mMethodMap, this.mMethodList, userId, this.mSystemReady ^ 1);
        updateCurrentProfileIds();
        this.mFileManager = new InputMethodFileManager(this.mMethodMap, userId);
        this.mSwitchingController = InputMethodSubtypeSwitchingController.createInstanceLocked(this.mSettings, context2);
        IVrManager vrManager = (IVrManager) ServiceManager.getService("vrmanager");
        if (vrManager != null) {
            try {
                vrManager.registerListener(this.mVrStateCallbacks);
            } catch (RemoteException e2) {
                Slog.e(TAG, "Failed to register VR mode state listener.");
            }
        }
        createFlagIfNecessary(userId);
    }

    private void resetDefaultImeLocked(Context context) {
    }

    @GuardedBy("mMethodMap")
    private void switchUserLocked(int newUserId) {
        createFlagIfNecessary(newUserId);
        this.mSettingsObserver.registerContentObserverLocked(newUserId);
        boolean useCopyOnWriteSettings = (this.mSystemReady && this.mUserManager.isUserUnlockingOrUnlocked(newUserId)) ? false : true;
        this.mSettings.switchCurrentUser(newUserId, useCopyOnWriteSettings);
        updateCurrentProfileIds();
        this.mFileManager = new InputMethodFileManager(this.mMethodMap, newUserId);
        boolean initialUserSwitch = TextUtils.isEmpty(this.mSettings.getSelectedInputMethod());
        this.mLastSystemLocales = this.mRes.getConfiguration().getLocales();
        if (this.mSystemReady) {
            hideCurrentInputLocked(0, null);
            resetCurrentMethodAndClient(6);
            buildInputMethodListLocked(initialUserSwitch);
            if (TextUtils.isEmpty(this.mSettings.getSelectedInputMethod())) {
                resetDefaultImeLocked(this.mContext);
            }
            updateFromSettingsLocked(true);
            try {
                startInputInnerLocked();
            } catch (RuntimeException e) {
                Slog.w(TAG, "Unexpected exception", e);
            }
        }
        if (initialUserSwitch) {
            InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(this.mIPackageManager, this.mSettings.getEnabledInputMethodListLocked(), newUserId, this.mContext.getBasePackageName());
        }
        switchUserExtra(newUserId);
    }

    void updateCurrentProfileIds() {
        this.mSettings.setCurrentProfileIds(this.mUserManager.getProfileIdsWithDisabled(this.mSettings.getCurrentUserId()));
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        try {
            return super.onTransact(code, data, reply, flags);
        } catch (RuntimeException e) {
            if (!(e instanceof SecurityException)) {
                Slog.wtf(TAG, "Input Method Manager Crash", e);
            }
            throw e;
        }
    }

    public void systemRunning(StatusBarManagerService statusBar) {
        synchronized (this.mMethodMap) {
            if (!this.mSystemReady) {
                this.mSystemReady = true;
                this.mLastSystemLocales = this.mRes.getConfiguration().getLocales();
                int currentUserId = this.mSettings.getCurrentUserId();
                this.mSettings.switchCurrentUser(currentUserId, this.mUserManager.isUserUnlockingOrUnlocked(currentUserId) ^ 1);
                this.mKeyguardManager = (KeyguardManager) this.mContext.getSystemService(KeyguardManager.class);
                this.mNotificationManager = (NotificationManager) this.mContext.getSystemService(NotificationManager.class);
                this.mStatusBar = statusBar;
                boolean z = false;
                if (this.mStatusBar != null) {
                    this.mStatusBar.setIconVisibility(this.mSlotIme, false);
                }
                updateSystemUiLocked(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
                this.mShowOngoingImeSwitcherForPhones = this.mRes.getBoolean(17957108);
                if (this.mShowOngoingImeSwitcherForPhones) {
                    this.mWindowManagerInternal.setOnHardKeyboardStatusChangeListener(this.mHardKeyboardListener);
                }
                this.mMyPackageMonitor.register(this.mContext, null, UserHandle.ALL, true);
                this.mSettingsObserver.registerContentObserverLocked(currentUserId);
                IntentFilter broadcastFilter = new IntentFilter();
                broadcastFilter.addAction("android.intent.action.CLOSE_SYSTEM_DIALOGS");
                broadcastFilter.addAction("android.intent.action.USER_ADDED");
                broadcastFilter.addAction("android.intent.action.USER_REMOVED");
                broadcastFilter.addAction("android.intent.action.LOCALE_CHANGED");
                broadcastFilter.addAction(ACTION_SHOW_INPUT_METHOD_PICKER);
                this.mContext.registerReceiver(new ImmsBroadcastReceiver(), broadcastFilter);
                String[] imePkgName = new String[]{"com.baidu.input_huawei", "com.touchtype.swiftkey", "com.swiftkey.swiftkeyconfigurator"};
                for (String defaultImeEnable : imePkgName) {
                    setDefaultImeEnable(defaultImeEnable);
                }
                if (!(TextUtils.isEmpty(this.mSettings.getSelectedInputMethod()) ^ true)) {
                    z = true;
                }
                buildInputMethodListLocked(z);
                resetDefaultImeLocked(this.mContext);
                updateFromSettingsLocked(true);
                InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(this.mIPackageManager, this.mSettings.getEnabledInputMethodListLocked(), currentUserId, this.mContext.getBasePackageName());
                try {
                    startInputInnerLocked();
                } catch (RuntimeException e) {
                    Slog.w(TAG, "Unexpected exception", e);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:9:0x003f, code skipped:
            return true;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private boolean calledFromValidUser() {
        int uid = Binder.getCallingUid();
        int userId = UserHandle.getUserId(uid);
        if (uid == 1000 || this.mSettings.isCurrentProfile(userId) || this.mContext.checkCallingOrSelfPermission("android.permission.INTERACT_ACROSS_USERS_FULL") == 0) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("--- IPC called from background users. Ignore. callers=");
        stringBuilder.append(Debug.getCallers(10));
        Slog.w(str, stringBuilder.toString());
        return false;
    }

    private boolean calledWithValidToken(IBinder token) {
        if (token == null && Binder.getCallingPid() == Process.myPid()) {
            return false;
        }
        if (token != null && token == this.mCurToken) {
            return true;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ignoring ");
        stringBuilder.append(Debug.getCaller());
        stringBuilder.append(" due to an invalid token. uid:");
        stringBuilder.append(Binder.getCallingUid());
        stringBuilder.append(" token:");
        stringBuilder.append(token);
        Slog.e(str, stringBuilder.toString());
        return false;
    }

    @GuardedBy("mMethodMap")
    private boolean bindCurrentInputMethodServiceLocked(Intent service, ServiceConnection conn, int flags) {
        if (service == null || conn == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("--- bind failed: service = ");
            stringBuilder.append(service);
            stringBuilder.append(", conn = ");
            stringBuilder.append(conn);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
        if (this.mBindInstantServiceAllowed) {
            flags |= DumpState.DUMP_CHANGES;
        }
        return this.mContext.bindServiceAsUser(service, conn, flags, new UserHandle(this.mSettings.getCurrentUserId()));
    }

    public List<InputMethodInfo> getInputMethodList() {
        return getInputMethodList(false);
    }

    public List<InputMethodInfo> getVrInputMethodList() {
        return getInputMethodList(true);
    }

    private List<InputMethodInfo> getInputMethodList(boolean isVrOnly) {
        if (!calledFromValidUser()) {
            return Collections.emptyList();
        }
        ArrayList<InputMethodInfo> methodList;
        synchronized (this.mMethodMap) {
            methodList = new ArrayList();
            Iterator it = this.mMethodList.iterator();
            while (it.hasNext()) {
                InputMethodInfo info = (InputMethodInfo) it.next();
                if (info.isVrOnly() == isVrOnly) {
                    methodList.add(info);
                }
            }
        }
        return methodList;
    }

    public List<InputMethodInfo> getEnabledInputMethodList() {
        if (!calledFromValidUser()) {
            return Collections.emptyList();
        }
        ArrayList enabledInputMethodListLocked;
        synchronized (this.mMethodMap) {
            enabledInputMethodListLocked = this.mSettings.getEnabledInputMethodListLocked();
        }
        return enabledInputMethodListLocked;
    }

    /* JADX WARNING: Removed duplicated region for block: B:18:0x0031 A:{Catch:{ all -> 0x001f }} */
    /* JADX WARNING: Removed duplicated region for block: B:15:0x002b A:{Catch:{ all -> 0x001f }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List<InputMethodSubtype> getEnabledInputMethodSubtypeList(String imiId, boolean allowsImplicitlySelectedSubtypes) {
        if (!calledFromValidUser()) {
            return Collections.emptyList();
        }
        synchronized (this.mMethodMap) {
            InputMethodInfo imi;
            if (imiId == null) {
                try {
                    if (this.mCurMethodId != null) {
                        imi = (InputMethodInfo) this.mMethodMap.get(this.mCurMethodId);
                        List emptyList;
                        if (imi != null) {
                            emptyList = Collections.emptyList();
                            return emptyList;
                        }
                        emptyList = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi, allowsImplicitlySelectedSubtypes);
                        return emptyList;
                    }
                } finally {
                }
            }
            imi = (InputMethodInfo) this.mMethodMap.get(imiId);
            if (imi != null) {
            }
        }
    }

    public void addClient(IInputMethodClient client, IInputContext inputContext, int uid, int pid) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                this.mClients.put(client.asBinder(), new ClientState(client, inputContext, uid, pid));
            }
        }
    }

    public void removeClient(IInputMethodClient client) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                ClientState cs = (ClientState) this.mClients.remove(client.asBinder());
                if (cs != null) {
                    clearClientSessionLocked(cs);
                    if (this.mCurClient == cs) {
                        if (this.mBoundToMethod) {
                            this.mBoundToMethod = false;
                            if (this.mCurMethod != null) {
                                executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageO(1000, this.mCurMethod));
                            }
                        }
                        this.mCurClient = null;
                    }
                    if (this.mCurFocusedWindowClient == cs) {
                        this.mCurFocusedWindowClient = null;
                    }
                }
            }
        }
    }

    void executeOrSendMessage(IInterface target, Message msg) {
        if (target.asBinder() instanceof Binder) {
            this.mCaller.sendMessage(msg);
            return;
        }
        handleMessage(msg);
        msg.recycle();
    }

    void unbindCurrentClientLocked(int unbindClientReason) {
        if (this.mCurClient != null) {
            if (this.mBoundToMethod) {
                this.mBoundToMethod = false;
                if (this.mCurMethod != null) {
                    executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageO(1000, this.mCurMethod));
                }
            }
            executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIIO(MSG_SET_ACTIVE, 0, 0, this.mCurClient));
            executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIIO(MSG_UNBIND_CLIENT, this.mCurSeq, unbindClientReason, this.mCurClient.client));
            this.mCurClient.sessionRequested = false;
            this.mCurClient = null;
            hideInputMethodMenuLocked();
        }
    }

    private int getImeShowFlags() {
        if (this.mShowForced) {
            return 0 | 3;
        }
        if (this.mShowExplicitlyRequested) {
            return 0 | 1;
        }
        return 0;
    }

    private int getAppShowFlags() {
        if (this.mShowForced) {
            return 0 | 2;
        }
        if (this.mShowExplicitlyRequested) {
            return 0;
        }
        return 0 | 1;
    }

    @GuardedBy("mMethodMap")
    InputBindResult attachNewInputLocked(int startInputReason, boolean initial) {
        if (!this.mBoundToMethod) {
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOO(MSG_BIND_INPUT, this.mCurMethod, this.mCurClient.binding));
            this.mBoundToMethod = true;
        }
        Binder startInputToken = new Binder();
        StartInputInfo info = new StartInputInfo(this.mCurToken, this.mCurId, startInputReason, initial ^ 1, this.mCurFocusedWindow, this.mCurAttribute, this.mCurFocusedWindowSoftInputMode, this.mCurSeq);
        this.mStartInputMap.put(startInputToken, info);
        this.mStartInputHistory.addEntry(info);
        SessionState session = this.mCurClient.curSession;
        executeOrSendMessage(session.method, this.mCaller.obtainMessageIIOOOO(2000, this.mCurInputContextMissingMethods, initial ^ 1, startInputToken, session, this.mCurInputContext, this.mCurAttribute));
        ResultReceiver resultReceiver = null;
        if (this.mShowRequested) {
            if (DEBUG_FLOW) {
                Slog.v(TAG, "Attach new input asks to show input");
            }
            showCurrentInputLocked(getAppShowFlags(), null);
        }
        IInputMethodSession iInputMethodSession = session.session;
        if (session.channel != null) {
            resultReceiver = session.channel.dup();
        }
        return new InputBindResult(0, iInputMethodSession, resultReceiver, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
    }

    @GuardedBy("mMethodMap")
    InputBindResult startInputLocked(int startInputReason, IInputMethodClient client, IInputContext inputContext, int missingMethods, EditorInfo attribute, int controlFlags) {
        if (this.mCurMethodId == null) {
            return InputBindResult.NO_IME;
        }
        ClientState cs = (ClientState) this.mClients.get(client.asBinder());
        StringBuilder stringBuilder;
        String str;
        if (cs == null) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("unknown client ");
            stringBuilder.append(client.asBinder());
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (attribute == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring startInput with null EditorInfo. uid=");
            stringBuilder.append(cs.uid);
            stringBuilder.append(" pid=");
            stringBuilder.append(cs.pid);
            Slog.w(str, stringBuilder.toString());
            return InputBindResult.NULL_EDITOR_INFO;
        } else {
            try {
                if (!this.mIWindowManager.inputMethodClientHasFocus(cs.client)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Starting input on non-focused client ");
                    stringBuilder.append(cs.client);
                    stringBuilder.append(" (uid=");
                    stringBuilder.append(cs.uid);
                    stringBuilder.append(" pid=");
                    stringBuilder.append(cs.pid);
                    stringBuilder.append(")");
                    Slog.w(str, stringBuilder.toString());
                    return InputBindResult.NOT_IME_TARGET_WINDOW;
                }
            } catch (RemoteException e) {
            }
            return startInputUncheckedLocked(cs, inputContext, missingMethods, attribute, controlFlags, startInputReason);
        }
    }

    @GuardedBy("mMethodMap")
    InputBindResult startInputUncheckedLocked(ClientState cs, IInputContext inputContext, int missingMethods, EditorInfo attribute, int controlFlags, int startInputReason) {
        ClientState clientState = cs;
        EditorInfo editorInfo = attribute;
        int i = controlFlags;
        if (this.mCurMethodId == null) {
            return InputBindResult.NO_IME;
        }
        if (InputMethodUtils.checkIfPackageBelongsToUid(this.mAppOpsManager, clientState.uid, editorInfo.packageName)) {
            if (this.mCurClient != clientState) {
                this.mCurClientInKeyguard = isKeyguardLocked();
                unbindCurrentClientLocked(1);
                if (this.mIsInteractive) {
                    executeOrSendMessage(clientState.client, this.mCaller.obtainMessageIO(MSG_SET_ACTIVE, this.mIsInteractive, clientState));
                }
            }
            this.mCurSeq++;
            if (this.mCurSeq <= 0) {
                this.mCurSeq = 1;
            }
            this.mCurClient = clientState;
            this.mCurInputContext = inputContext;
            this.mCurInputContextMissingMethods = missingMethods;
            this.mCurAttribute = editorInfo;
            int i2;
            if (this.mCurId == null || !this.mCurId.equals(this.mCurMethodId)) {
                i2 = startInputReason;
            } else {
                boolean z = false;
                if (clientState.curSession != null) {
                    if ((65536 & i) != 0) {
                        this.mShowRequested = true;
                    }
                    if ((i & 256) != 0) {
                        z = true;
                    }
                    return attachNewInputLocked(startInputReason, z);
                }
                i2 = startInputReason;
                if (this.mHaveConnection) {
                    if (this.mCurMethod != null) {
                        requestClientSessionLocked(cs);
                        return new InputBindResult(1, null, null, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
                    } else if (SystemClock.uptimeMillis() < this.mLastBindTime + TIME_TO_RECONNECT) {
                        return new InputBindResult(2, null, null, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
                    } else {
                        EventLog.writeEvent(EventLogTags.IMF_FORCE_RECONNECT_IME, new Object[]{this.mCurMethodId, Long.valueOf(SystemClock.uptimeMillis() - this.mLastBindTime), Integer.valueOf(0)});
                    }
                }
            }
            try {
                return startInputInnerLocked();
            } catch (RuntimeException e) {
                RuntimeException runtimeException = e;
                Slog.w(TAG, "Unexpected exception", e);
                return null;
            }
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Rejecting this client as it reported an invalid package name. uid=");
        stringBuilder.append(clientState.uid);
        stringBuilder.append(" package=");
        stringBuilder.append(editorInfo.packageName);
        Slog.e(str, stringBuilder.toString());
        return InputBindResult.INVALID_PACKAGE_NAME;
    }

    InputBindResult startInputInnerLocked() {
        if (this.mCurMethodId == null || !this.mMethodMap.containsKey(this.mCurMethodId)) {
            return InputBindResult.NO_IME;
        }
        if (!this.mSystemReady) {
            return new InputBindResult(7, null, null, this.mCurMethodId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
        }
        InputMethodInfo info = (InputMethodInfo) this.mMethodMap.get(this.mCurMethodId);
        String str;
        StringBuilder stringBuilder;
        if (info == null) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("info == null id: ");
            stringBuilder.append(this.mCurMethodId);
            Slog.w(str, stringBuilder.toString());
            return InputBindResult.NO_IME;
        }
        unbindCurrentMethodLocked(true);
        this.mCurIntent = new Intent("android.view.InputMethod");
        this.mCurIntent.setComponent(info.getComponent());
        this.mCurIntent.putExtra("android.intent.extra.client_label", 17040222);
        this.mCurIntent.putExtra("android.intent.extra.client_intent", PendingIntent.getActivity(this.mContext, 0, new Intent("android.settings.INPUT_METHOD_SETTINGS"), 0));
        if (bindCurrentInputMethodServiceLocked(this.mCurIntent, this, IME_CONNECTION_BIND_FLAGS)) {
            this.mLastBindTime = SystemClock.uptimeMillis();
            this.mHaveConnection = true;
            this.mCurId = info.getId();
            this.mCurToken = new Binder();
            try {
                this.mIWindowManager.addWindowToken(this.mCurToken, 2011, 0);
            } catch (RemoteException e) {
            }
            return new InputBindResult(2, null, null, this.mCurId, this.mCurSeq, this.mCurUserActionNotificationSequenceNumber);
        }
        this.mCurIntent = null;
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("Failure connecting to input method service: ");
        stringBuilder.append(this.mCurIntent);
        Slog.w(str, stringBuilder.toString());
        return InputBindResult.IME_NOT_CONNECTED;
    }

    protected InputBindResult startInput(int startInputReason, IInputMethodClient client, IInputContext inputContext, int missingMethods, EditorInfo attribute, int controlFlags) {
        if (!calledFromValidUser()) {
            return InputBindResult.INVALID_USER;
        }
        InputBindResult startInputLocked;
        synchronized (this.mMethodMap) {
            long ident = Binder.clearCallingIdentity();
            try {
                startInputLocked = startInputLocked(startInputReason, client, inputContext, missingMethods, attribute, controlFlags);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
        return startInputLocked;
    }

    public void finishInput(IInputMethodClient client) {
    }

    /* JADX WARNING: Missing block: B:19:0x0066, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void onServiceConnected(ComponentName name, IBinder service) {
        synchronized (this.mMethodMap) {
            if (this.mCurIntent != null && name.equals(this.mCurIntent.getComponent())) {
                this.mCurMethod = IInputMethod.Stub.asInterface(service);
                if (this.mCurToken == null) {
                    Slog.w(TAG, "Service connected without a token!");
                    unbindCurrentMethodLocked(false);
                    return;
                }
                if (DEBUG_FLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Initiating attach with token: ");
                    stringBuilder.append(this.mCurToken);
                    Slog.v(str, stringBuilder.toString());
                }
                executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOO(MSG_ATTACH_TOKEN, this.mCurMethod, this.mCurToken));
                if (this.mCurClient != null) {
                    clearClientSessionLocked(this.mCurClient);
                    requestClientSessionLocked(this.mCurClient);
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0053, code skipped:
            return;
     */
    /* JADX WARNING: Missing block: B:19:0x0055, code skipped:
            r9.dispose();
     */
    /* JADX WARNING: Missing block: B:20:0x0058, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    void onSessionCreated(IInputMethod method, IInputMethodSession session, InputChannel channel) {
        synchronized (this.mMethodMap) {
            if (this.mCurMethod == null || method == null || this.mCurMethod.asBinder() != method.asBinder() || this.mCurClient == null) {
            } else {
                if (DEBUG_FLOW) {
                    Slog.v(TAG, "IME session created");
                }
                clearClientSessionLocked(this.mCurClient);
                this.mCurClient.curSession = new SessionState(this.mCurClient, method, session, channel);
                InputBindResult res = attachNewInputLocked(9, true);
                if (res.method != null) {
                    executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageOO(3010, this.mCurClient.client, res));
                }
            }
        }
    }

    void unbindCurrentMethodLocked(boolean savePosition) {
        if (this.mVisibleBound) {
            this.mContext.unbindService(this.mVisibleConnection);
            this.mVisibleBound = false;
        }
        if (this.mHaveConnection) {
            this.mContext.unbindService(this);
            this.mHaveConnection = false;
        }
        if (this.mCurToken != null) {
            try {
                if ((this.mImeWindowVis & 1) != 0 && savePosition) {
                    this.mWindowManagerInternal.saveLastInputMethodWindowForTransition();
                }
                this.mIWindowManager.removeWindowToken(this.mCurToken, 0);
            } catch (RemoteException e) {
            }
            this.mCurToken = null;
        }
        this.mCurId = null;
        clearCurMethodLocked();
    }

    void resetCurrentMethodAndClient(int unbindClientReason) {
        this.mCurMethodId = null;
        unbindCurrentMethodLocked(false);
        unbindCurrentClientLocked(unbindClientReason);
    }

    void requestClientSessionLocked(ClientState cs) {
        if (!cs.sessionRequested) {
            InputChannel[] channels = InputChannel.openInputChannelPair(cs.toString());
            cs.sessionRequested = true;
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOOO(MSG_CREATE_SESSION, this.mCurMethod, channels[1], new MethodCallback(this, this.mCurMethod, channels[0])));
        }
    }

    void clearClientSessionLocked(ClientState cs) {
        finishSessionLocked(cs.curSession);
        cs.curSession = null;
        cs.sessionRequested = false;
    }

    private void finishSessionLocked(SessionState sessionState) {
        if (sessionState != null) {
            if (sessionState.session != null) {
                try {
                    sessionState.session.finishSession();
                } catch (RemoteException e) {
                    Slog.w(TAG, "Session failed to close due to remote exception", e);
                    updateSystemUiLocked(this.mCurToken, 0, this.mBackDisposition);
                }
                sessionState.session = null;
            }
            if (sessionState.channel != null) {
                sessionState.channel.dispose();
                sessionState.channel = null;
            }
        }
    }

    void clearCurMethodLocked() {
        if (this.mCurMethod != null) {
            for (ClientState cs : this.mClients.values()) {
                clearClientSessionLocked(cs);
            }
            finishSessionLocked(this.mEnabledSession);
            this.mEnabledSession = null;
            this.mCurMethod = null;
        }
        if (this.mStatusBar != null) {
            this.mStatusBar.setIconVisibility(this.mSlotIme, false);
        }
        this.mInFullscreenMode = false;
    }

    public void onServiceDisconnected(ComponentName name) {
        synchronized (this.mMethodMap) {
            if (!(this.mCurMethod == null || this.mCurIntent == null || !name.equals(this.mCurIntent.getComponent()))) {
                clearCurMethodLocked();
                this.mLastBindTime = SystemClock.uptimeMillis();
                this.mShowRequested = this.mInputShown;
                this.mInputShown = false;
                unbindCurrentClientLocked(3);
                if (HwPCUtils.isPcCastModeInServer()) {
                    unbindCurrentMethodLocked(false);
                    this.mLastUnBindInputMethodInPCMode = true;
                }
                if (HwPCUtils.enabledInPad() && !HwPCUtils.isPcCastModeInServer() && this.mLastUnBindInputMethodInPCMode) {
                    unbindCurrentMethodLocked(false);
                    this.mLastUnBindInputMethodInPCMode = false;
                }
            }
        }
    }

    public void updateStatusIcon(IBinder token, String packageName, int iconId) {
        synchronized (this.mMethodMap) {
            if (calledWithValidToken(token)) {
                long ident = Binder.clearCallingIdentity();
                if (iconId == 0) {
                    try {
                        if (this.mStatusBar != null) {
                            this.mStatusBar.setIconVisibility(this.mSlotIme, false);
                        }
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(ident);
                    }
                } else if (packageName != null) {
                    String str = null;
                    CharSequence contentDescription = null;
                    try {
                        contentDescription = this.mContext.getPackageManager().getApplicationLabel(this.mIPackageManager.getApplicationInfo(packageName, 0, this.mSettings.getCurrentUserId()));
                    } catch (RemoteException e) {
                    }
                    if (this.mStatusBar != null) {
                        StatusBarManagerService statusBarManagerService = this.mStatusBar;
                        String str2 = this.mSlotIme;
                        if (contentDescription != null) {
                            str = contentDescription.toString();
                        }
                        statusBarManagerService.setIcon(str2, packageName, iconId, 0, str);
                        this.mStatusBar.setIconVisibility(this.mSlotIme, true);
                    }
                }
                Binder.restoreCallingIdentity(ident);
                return;
            }
        }
    }

    private boolean shouldShowImeSwitcherLocked(int visibility) {
        if (!this.mShowOngoingImeSwitcherForPhones || this.mSwitchingDialog != null) {
            return false;
        }
        if ((this.mWindowManagerInternal.isKeyguardShowingAndNotOccluded() && this.mKeyguardManager != null && this.mKeyguardManager.isKeyguardSecure()) || (visibility & 1) == 0) {
            return false;
        }
        if (this.mWindowManagerInternal.isHardKeyboardAvailable()) {
            if (this.mHardKeyboardBehavior == 0) {
                return true;
            }
        } else if ((visibility & 2) == 0) {
            return false;
        }
        List<InputMethodInfo> imis = this.mSettings.getEnabledInputMethodListLocked();
        int N = imis.size();
        if (N > 2) {
            return true;
        }
        if (N < 1) {
            return false;
        }
        InputMethodSubtype auxSubtype = null;
        InputMethodSubtype nonAuxSubtype = null;
        int auxCount = 0;
        int nonAuxCount = 0;
        for (int i = 0; i < N; i++) {
            List<InputMethodSubtype> subtypes = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, (InputMethodInfo) imis.get(i), true);
            int subtypeCount = subtypes.size();
            if (subtypeCount == 0) {
                nonAuxCount++;
            } else {
                InputMethodSubtype auxSubtype2 = auxSubtype;
                auxSubtype = nonAuxSubtype;
                int auxCount2 = auxCount;
                auxCount = nonAuxCount;
                for (nonAuxCount = 0; nonAuxCount < subtypeCount; nonAuxCount++) {
                    InputMethodSubtype subtype = (InputMethodSubtype) subtypes.get(nonAuxCount);
                    if (subtype.isAuxiliary()) {
                        auxCount2++;
                        auxSubtype2 = subtype;
                    } else {
                        auxCount++;
                        auxSubtype = subtype;
                    }
                }
                nonAuxCount = auxCount;
                auxCount = auxCount2;
                nonAuxSubtype = auxSubtype;
                auxSubtype = auxSubtype2;
            }
        }
        if (nonAuxCount > 1 || auxCount > 1) {
            return true;
        }
        if (nonAuxCount != 1 || auxCount != 1) {
            return false;
        }
        if (nonAuxSubtype == null || auxSubtype == null || ((!nonAuxSubtype.getLocale().equals(auxSubtype.getLocale()) && !auxSubtype.overridesImplicitlyEnabledSubtype() && !nonAuxSubtype.overridesImplicitlyEnabledSubtype()) || !nonAuxSubtype.containsExtraValueKey(TAG_TRY_SUPPRESSING_IME_SWITCHER))) {
            return true;
        }
        return false;
    }

    private boolean isKeyguardLocked() {
        return this.mKeyguardManager != null && this.mKeyguardManager.isKeyguardLocked();
    }

    public void setImeWindowStatus(IBinder token, IBinder startInputToken, int vis, int backDisposition) {
        if (calledWithValidToken(token)) {
            StartInputInfo info;
            boolean dismissImeOnBackKeyPressed;
            synchronized (this.mMethodMap) {
                info = (StartInputInfo) this.mStartInputMap.get(startInputToken);
                this.mImeWindowVis = vis;
                this.mBackDisposition = backDisposition;
                updateSystemUiLocked(token, vis, backDisposition);
            }
            boolean z = false;
            switch (backDisposition) {
                case 1:
                    dismissImeOnBackKeyPressed = false;
                    break;
                case 2:
                    dismissImeOnBackKeyPressed = true;
                    break;
                default:
                    if ((vis & 2) == 0) {
                        dismissImeOnBackKeyPressed = false;
                        break;
                    } else {
                        dismissImeOnBackKeyPressed = true;
                        break;
                    }
            }
            WindowManagerInternal windowManagerInternal = this.mWindowManagerInternal;
            if ((vis & 2) != 0) {
                z = true;
            }
            windowManagerInternal.updateInputMethodWindowStatus(token, z, dismissImeOnBackKeyPressed, info != null ? info.mTargetWindow : null);
        }
    }

    private void updateSystemUi(IBinder token, int vis, int backDisposition) {
        synchronized (this.mMethodMap) {
            updateSystemUiLocked(token, vis, backDisposition);
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:26:0x0045  */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x0030 A:{SYNTHETIC, Splitter:B:17:0x0030} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void updateSystemUiLocked(IBinder token, int vis, int backDisposition) {
        Throwable th;
        IBinder iBinder;
        int i;
        if (calledWithValidToken(token)) {
            int vis2;
            boolean needsToShowImeSwitcher;
            InputMethodInfo imi;
            int i2;
            long ident = Binder.clearCallingIdentity();
            if (vis != 0) {
                try {
                    if (isKeyguardLocked() && !this.mCurClientInKeyguard) {
                        vis2 = 0;
                        needsToShowImeSwitcher = shouldShowImeSwitcherLocked(vis2);
                        if (this.mStatusBar == null) {
                            try {
                                try {
                                    this.mStatusBar.setImeWindowStatus(token, vis2, backDisposition, needsToShowImeSwitcher);
                                } catch (Throwable th2) {
                                    th = th2;
                                }
                            } catch (Throwable th3) {
                                th = th3;
                                iBinder = token;
                                i = backDisposition;
                                Binder.restoreCallingIdentity(ident);
                                throw th;
                            }
                        }
                        iBinder = token;
                        i = backDisposition;
                        try {
                            imi = (InputMethodInfo) this.mMethodMap.get(this.mCurMethodId);
                            if (imi == null && needsToShowImeSwitcher) {
                                this.mImeSwitcherNotification.setContentTitle(this.mRes.getText(17041060)).setContentText(InputMethodUtils.getImeAndSubtypeDisplayName(this.mContext, imi, this.mCurrentSubtype)).setContentIntent(this.mImeSwitchPendingIntent);
                                try {
                                    boolean isEnableNavBar = System.getIntForUser(this.mContext.getContentResolver(), "enable_navbar", getNaviBarEnabledDefValue(), -2) != 0;
                                    String str = TAG;
                                    StringBuilder stringBuilder = new StringBuilder();
                                    stringBuilder.append("--- show notification config: mIWindowManager.hasNavigationBar() =  ");
                                    stringBuilder.append(this.mIWindowManager.hasNavigationBar());
                                    stringBuilder.append(" ,isEnableNavBar = ");
                                    stringBuilder.append(isEnableNavBar);
                                    Slog.i(str, stringBuilder.toString());
                                    if (this.mNotificationManager != null) {
                                        if (this.mIWindowManager.hasNavigationBar()) {
                                            if (isEnableNavBar) {
                                                i2 = vis2;
                                            }
                                        }
                                        i2 = vis2;
                                        try {
                                            this.mNotificationManager.notifyAsUser(null, 8, this.mImeSwitcherNotification.build(), UserHandle.ALL);
                                            this.mNotificationShown = true;
                                        } catch (RemoteException e) {
                                        }
                                    }
                                } catch (RemoteException e2) {
                                    i2 = vis2;
                                }
                            } else {
                                try {
                                    if (this.mNotificationShown && this.mNotificationManager != null) {
                                        this.mNotificationManager.cancelAsUser(null, 8, UserHandle.ALL);
                                        this.mNotificationShown = false;
                                    }
                                } catch (Throwable th4) {
                                    th = th4;
                                    Binder.restoreCallingIdentity(ident);
                                    throw th;
                                }
                            }
                            Binder.restoreCallingIdentity(ident);
                        } catch (Throwable th5) {
                            th = th5;
                            i2 = vis2;
                            Binder.restoreCallingIdentity(ident);
                            throw th;
                        }
                    }
                } catch (Throwable th6) {
                    th = th6;
                    iBinder = token;
                    i2 = vis;
                    i = backDisposition;
                    Binder.restoreCallingIdentity(ident);
                    throw th;
                }
            }
            vis2 = vis;
            try {
                needsToShowImeSwitcher = shouldShowImeSwitcherLocked(vis2);
                if (this.mStatusBar == null) {
                }
                imi = (InputMethodInfo) this.mMethodMap.get(this.mCurMethodId);
                if (imi == null) {
                }
                this.mNotificationManager.cancelAsUser(null, 8, UserHandle.ALL);
                this.mNotificationShown = false;
                Binder.restoreCallingIdentity(ident);
            } catch (Throwable th7) {
                th = th7;
                iBinder = token;
                i = backDisposition;
                i2 = vis2;
                Binder.restoreCallingIdentity(ident);
                throw th;
            }
        }
    }

    public void registerSuggestionSpansForNotification(SuggestionSpan[] spans) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                InputMethodInfo currentImi = (InputMethodInfo) this.mMethodMap.get(this.mCurMethodId);
                for (SuggestionSpan ss : spans) {
                    if (!TextUtils.isEmpty(ss.getNotificationTargetClassName())) {
                        this.mSecureSuggestionSpans.put(ss, currentImi);
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:23:0x0061, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean notifySuggestionPicked(SuggestionSpan span, String originalString, int index) {
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            InputMethodInfo targetImi = (InputMethodInfo) this.mSecureSuggestionSpans.get(span);
            if (targetImi != null) {
                String[] suggestions = span.getSuggestions();
                if (index >= 0) {
                    if (index < suggestions.length) {
                        String className = span.getNotificationTargetClassName();
                        Intent intent = new Intent();
                        intent.setClassName(targetImi.getPackageName(), className);
                        intent.setAction("android.text.style.SUGGESTION_PICKED");
                        intent.putExtra("before", originalString);
                        intent.putExtra("after", suggestions[index]);
                        intent.putExtra("hashcode", span.hashCode());
                        long ident = Binder.clearCallingIdentity();
                        try {
                            this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
                            return true;
                        } finally {
                            Binder.restoreCallingIdentity(ident);
                        }
                    }
                }
            } else {
                return false;
            }
        }
    }

    void updateFromSettingsLocked(boolean enabledMayChange) {
        updateInputMethodsFromSettingsLocked(enabledMayChange);
        updateKeyboardFromSettingsLocked();
    }

    void updateInputMethodsFromSettingsLocked(boolean enabledMayChange) {
        if (enabledMayChange) {
            List<InputMethodInfo> enabled = this.mSettings.getEnabledInputMethodListLocked();
            for (int i = 0; i < enabled.size(); i++) {
                InputMethodInfo imm = (InputMethodInfo) enabled.get(i);
                try {
                    ApplicationInfo ai = this.mIPackageManager.getApplicationInfo(imm.getPackageName(), 32768, this.mSettings.getCurrentUserId());
                    if (ai != null && ai.enabledSetting == 4) {
                        this.mIPackageManager.setApplicationEnabledSetting(imm.getPackageName(), 0, 1, this.mSettings.getCurrentUserId(), this.mContext.getBasePackageName());
                    }
                } catch (RemoteException e) {
                }
            }
        }
        String id = this.mSettings.getSelectedInputMethod();
        if (TextUtils.isEmpty(id) && chooseNewDefaultIMELocked()) {
            id = this.mSettings.getSelectedInputMethod();
        }
        if (TextUtils.isEmpty(id)) {
            resetCurrentMethodAndClient(4);
        } else {
            try {
                setInputMethodLocked(id, this.mSettings.getSelectedInputMethodSubtypeId(id));
            } catch (IllegalArgumentException e2) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("Unknown input method from prefs: ");
                stringBuilder.append(id);
                Slog.w(str, stringBuilder.toString(), e2);
                resetCurrentMethodAndClient(5);
            }
            this.mShortcutInputMethodsAndSubtypes.clear();
        }
        this.mSwitchingController.resetCircularListLocked(this.mContext);
    }

    public void updateKeyboardFromSettingsLocked() {
        this.mShowImeWithHardKeyboard = this.mSettings.isShowImeWithHardKeyboardEnabled();
        if (this.mSwitchingDialog != null && this.mSwitchingDialogTitleView != null && this.mSwitchingDialog.isShowing()) {
            Switch hardKeySwitch = (Switch) this.mSwitchingDialogTitleView.findViewById(16908949);
            if (hardKeySwitch != null) {
                hardKeySwitch.setChecked(this.mShowImeWithHardKeyboard);
            }
        }
    }

    void setInputMethodLocked(String id, int subtypeId) {
        InputMethodInfo info = (InputMethodInfo) this.mMethodMap.get(id);
        if (info == null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown id: ");
            stringBuilder.append(id);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (id.equals(this.mCurMethodId)) {
            this.mIsDiffIME = false;
            int subtypeCount = info.getSubtypeCount();
            if (subtypeCount > 0) {
                InputMethodSubtype newSubtype;
                InputMethodSubtype oldSubtype = this.mCurrentSubtype;
                if (subtypeId < 0 || subtypeId >= subtypeCount) {
                    newSubtype = getCurrentInputMethodSubtypeLocked();
                } else {
                    newSubtype = info.getSubtypeAt(subtypeId);
                }
                if (newSubtype == null || oldSubtype == null) {
                    String str = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Illegal subtype state: old subtype = ");
                    stringBuilder2.append(oldSubtype);
                    stringBuilder2.append(", new subtype = ");
                    stringBuilder2.append(newSubtype);
                    Slog.w(str, stringBuilder2.toString());
                    return;
                }
                if (newSubtype != oldSubtype) {
                    setSelectedInputMethodAndSubtypeLocked(info, subtypeId, true);
                    if (this.mCurMethod != null) {
                        try {
                            updateSystemUiLocked(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
                            this.mCurMethod.changeInputMethodSubtype(newSubtype);
                        } catch (RemoteException e) {
                            Slog.w(TAG, "Failed to call changeInputMethodSubtype");
                        }
                    }
                }
            }
        } else {
            this.mIsDiffIME = true;
            this.mLastIME = this.mCurMethodId;
            long ident = Binder.clearCallingIdentity();
            try {
                setSelectedInputMethodAndSubtypeLocked(info, subtypeId, false);
                this.mCurMethodId = id;
                if (((ActivityManagerInternal) LocalServices.getService(ActivityManagerInternal.class)).isSystemReady()) {
                    Intent intent = new Intent("android.intent.action.INPUT_METHOD_CHANGED");
                    intent.addFlags(536870912);
                    intent.putExtra("input_method_id", id);
                    this.mContext.sendBroadcastAsUser(intent, UserHandle.CURRENT);
                }
                unbindCurrentClientLocked(2);
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }
    }

    public boolean showSoftInput(IInputMethodClient client, int flags, ResultReceiver resultReceiver) {
        if (!calledFromValidUser()) {
            return false;
        }
        int uid = Binder.getCallingUid();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mMethodMap) {
                if (this.mCurClient == null || client == null || this.mCurClient.client.asBinder() != client.asBinder()) {
                    try {
                        if (!this.mIWindowManager.inputMethodClientHasFocus(client)) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Ignoring showSoftInput of uid ");
                            stringBuilder.append(uid);
                            stringBuilder.append(": ");
                            stringBuilder.append(client);
                            Slog.w(str, stringBuilder.toString());
                            Binder.restoreCallingIdentity(ident);
                            return false;
                        }
                    } catch (RemoteException e) {
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                }
                if (DEBUG_FLOW) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Client requesting input be shown, requestedUid=");
                    stringBuilder2.append(uid);
                    Slog.v(str2, stringBuilder2.toString());
                }
                boolean showCurrentInputLocked = showCurrentInputLocked(flags, resultReceiver);
                Binder.restoreCallingIdentity(ident);
                return showCurrentInputLocked;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    boolean showCurrentInputLocked(int flags, ResultReceiver resultReceiver) {
        this.mLastInputShown = false;
        this.mShowRequested = true;
        if (this.mAccessibilityRequestingNoSoftKeyboard) {
            return false;
        }
        if ((flags & 2) != 0) {
            this.mShowExplicitlyRequested = true;
            this.mShowForced = true;
        } else if ((flags & 1) == 0) {
            this.mShowExplicitlyRequested = true;
        }
        if (!this.mSystemReady) {
            return false;
        }
        boolean res = false;
        if (this.mCurMethod != null) {
            if (DEBUG_FLOW) {
                String str = TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("showCurrentInputLocked: mCurToken=");
                stringBuilder.append(this.mCurToken);
                Slog.d(str, stringBuilder.toString());
            }
            executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageIOO(MSG_SHOW_SOFT_INPUT, getImeShowFlags(), this.mCurMethod, resultReceiver));
            this.mInputShown = true;
            if (this.mHaveConnection && !this.mVisibleBound) {
                bindCurrentInputMethodServiceLocked(this.mCurIntent, this.mVisibleConnection, IME_VISIBLE_BIND_FLAGS);
                this.mVisibleBound = true;
            }
            res = true;
        } else if (this.mHaveConnection && SystemClock.uptimeMillis() >= this.mLastBindTime + TIME_TO_RECONNECT) {
            EventLog.writeEvent(EventLogTags.IMF_FORCE_RECONNECT_IME, new Object[]{this.mCurMethodId, Long.valueOf(SystemClock.uptimeMillis() - this.mLastBindTime), Integer.valueOf(1)});
            Slog.w(TAG, "Force disconnect/connect to the IME in showCurrentInputLocked()");
            this.mContext.unbindService(this);
            bindCurrentInputMethodServiceLocked(this.mCurIntent, this, IME_CONNECTION_BIND_FLAGS);
        }
        return res;
    }

    public boolean hideSoftInput(IInputMethodClient client, int flags, ResultReceiver resultReceiver) {
        if (!calledFromValidUser()) {
            return false;
        }
        int uid = Binder.getCallingUid();
        int pid = Binder.getCallingPid();
        long ident = Binder.clearCallingIdentity();
        try {
            synchronized (this.mMethodMap) {
                if (this.mCurClient == null || client == null || this.mCurClient.client.asBinder() != client.asBinder()) {
                    try {
                        if (!this.mIWindowManager.inputMethodClientHasFocus(client)) {
                            String str = TAG;
                            StringBuilder stringBuilder = new StringBuilder();
                            stringBuilder.append("Ignoring hideSoftInput of uid ");
                            stringBuilder.append(uid);
                            stringBuilder.append(": ");
                            stringBuilder.append(client);
                            Slog.w(str, stringBuilder.toString());
                            Binder.restoreCallingIdentity(ident);
                            return false;
                        }
                    } catch (RemoteException e) {
                        Binder.restoreCallingIdentity(ident);
                        return false;
                    }
                }
                if (DEBUG_FLOW) {
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("Client requesting input be hidden, pid=");
                    stringBuilder2.append(pid);
                    Slog.v(str2, stringBuilder2.toString());
                }
                boolean hideCurrentInputLocked = hideCurrentInputLocked(flags, resultReceiver);
                Binder.restoreCallingIdentity(ident);
                return hideCurrentInputLocked;
            }
        } catch (Throwable th) {
            Binder.restoreCallingIdentity(ident);
        }
    }

    boolean hideCurrentInputLocked(int flags, ResultReceiver resultReceiver) {
        if (this.mLastInputShown && this.mIsDiffIME) {
            Slog.i(TAG, "cancel hide ");
            this.mLastInputShown = false;
            return false;
        } else if ((flags & 1) != 0 && (this.mShowExplicitlyRequested || this.mShowForced)) {
            return false;
        } else {
            if (this.mShowForced && (flags & 2) != 0) {
                return false;
            }
            boolean z = true;
            if (this.mCurMethod == null || (!this.mInputShown && (this.mImeWindowVis & 1) == 0)) {
                z = false;
            }
            if (z) {
                executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageOO(MSG_HIDE_SOFT_INPUT, this.mCurMethod, resultReceiver));
                z = true;
            } else {
                z = false;
            }
            if (this.mHaveConnection && this.mVisibleBound) {
                this.mContext.unbindService(this.mVisibleConnection);
                this.mVisibleBound = false;
            }
            this.mInputShown = false;
            this.mShowRequested = false;
            this.mShowExplicitlyRequested = false;
            this.mShowForced = false;
            return z;
        }
    }

    public InputBindResult startInputOrWindowGainedFocus(int startInputReason, IInputMethodClient client, IBinder windowToken, int controlFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext, int missingMethods, int unverifiedTargetSdkVersion) {
        InputBindResult result;
        if (windowToken != null) {
            result = windowGainedFocus(startInputReason, client, windowToken, controlFlags, softInputMode, windowFlags, attribute, inputContext, missingMethods, unverifiedTargetSdkVersion);
        } else {
            result = startInput(startInputReason, client, inputContext, missingMethods, attribute, controlFlags);
        }
        if (result == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("InputBindResult is @NonNull. startInputReason=");
            stringBuilder.append(InputMethodClient.getStartInputReason(startInputReason));
            stringBuilder.append(" windowFlags=#");
            stringBuilder.append(Integer.toHexString(windowFlags));
            stringBuilder.append(" editorInfo=");
            stringBuilder.append(attribute);
            Slog.wtf(str, stringBuilder.toString());
            return InputBindResult.NULL;
        }
        EditorInfo editorInfo = attribute;
        return result;
    }

    /* JADX WARNING: Removed duplicated region for block: B:57:0x00f8  */
    /* JADX WARNING: Removed duplicated region for block: B:56:0x00f6  */
    /* JADX WARNING: Removed duplicated region for block: B:60:0x0102  */
    /* JADX WARNING: Removed duplicated region for block: B:105:0x01cb A:{Catch:{ all -> 0x0149, all -> 0x01a8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:104:0x01c6 A:{Catch:{ all -> 0x0149, all -> 0x01a8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x01ad A:{Catch:{ all -> 0x0149, all -> 0x01a8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:92:0x0193 A:{Catch:{ all -> 0x0149, all -> 0x01a8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:81:0x0156 A:{Catch:{ all -> 0x0149, all -> 0x01a8 }} */
    /* JADX WARNING: Removed duplicated region for block: B:61:0x0108 A:{SYNTHETIC, Splitter:B:61:0x0108} */
    /* JADX WARNING: Removed duplicated region for block: B:127:0x021e A:{Catch:{ all -> 0x0149, all -> 0x01a8 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    protected InputBindResult windowGainedFocus(int startInputReason, IInputMethodClient client, IBinder windowToken, int controlFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext, int missingMethods, int unverifiedTargetSdkVersion) {
        Throwable th;
        HashMap hashMap;
        long ident;
        boolean z;
        long ident2;
        IBinder iBinder = windowToken;
        int i = controlFlags;
        int i2 = softInputMode;
        int i3 = unverifiedTargetSdkVersion;
        boolean calledFromValidUser = calledFromValidUser();
        InputBindResult res = null;
        long ident3 = Binder.clearCallingIdentity();
        int i4;
        try {
            HashMap hashMap2 = this.mMethodMap;
            synchronized (hashMap2) {
                try {
                    ClientState cs = (ClientState) this.mClients.get(client.asBinder());
                    if (cs != null) {
                        InputBindResult inputBindResult;
                        try {
                            if (!this.mIWindowManager.inputMethodClientHasFocus(cs.client)) {
                                String str = TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("Focus gain on non-focused client ");
                                stringBuilder.append(cs.client);
                                stringBuilder.append(" (uid=");
                                stringBuilder.append(cs.uid);
                                stringBuilder.append(" pid=");
                                stringBuilder.append(cs.pid);
                                stringBuilder.append(")");
                                Slog.w(str, stringBuilder.toString());
                                inputBindResult = InputBindResult.NOT_IME_TARGET_WINDOW;
                                try {
                                    Binder.restoreCallingIdentity(ident3);
                                    return inputBindResult;
                                } catch (Throwable th2) {
                                    th = th2;
                                    hashMap = hashMap2;
                                    ident = ident3;
                                    i4 = i3;
                                    z = calledFromValidUser;
                                    try {
                                        throw th;
                                    } catch (Throwable th3) {
                                        th = th3;
                                    }
                                }
                            }
                        } catch (RemoteException e) {
                        }
                        long ident4;
                        if (!calledFromValidUser) {
                            Slog.w(TAG, "A background user is requesting window. Hiding IME.");
                            Slog.w(TAG, "If you want to interect with IME, you need android.permission.INTERACT_ACROSS_USERS_FULL");
                            hideCurrentInputLocked(0, null);
                            inputBindResult = InputBindResult.INVALID_USER;
                            Binder.restoreCallingIdentity(ident3);
                            return inputBindResult;
                        } else if (this.mCurFocusedWindow != iBinder) {
                            hashMap = hashMap2;
                            ident4 = ident3;
                            z = calledFromValidUser;
                            calledFromValidUser = cs;
                            try {
                                boolean z2;
                                boolean doAutoShow;
                                boolean isTextEditor;
                                boolean didStart;
                                this.mCurFocusedWindow = iBinder;
                                this.mCurFocusedWindowSoftInputMode = i2;
                                this.mCurFocusedWindowClient = calledFromValidUser;
                                if ((i2 & 240) != 16) {
                                    if (!this.mRes.getConfiguration().isLayoutSizeAtLeast(3)) {
                                        z2 = false;
                                        doAutoShow = z2;
                                        isTextEditor = (i & 2) == 0;
                                        didStart = false;
                                        int i5;
                                        ResultReceiver resultReceiver;
                                        ResultReceiver resultReceiver2;
                                        switch (i2 & 15) {
                                            case 0:
                                                i5 = 1;
                                                ident2 = ident4;
                                                ident4 = unverifiedTargetSdkVersion;
                                                resultReceiver = null;
                                                if (isTextEditor) {
                                                    if (doAutoShow) {
                                                        if (isTextEditor && doAutoShow && (i2 & 256) != 0) {
                                                            if (DEBUG_FLOW) {
                                                                Slog.v(TAG, "Unspecified window will show input");
                                                            }
                                                            if (attribute != null) {
                                                                resultReceiver2 = resultReceiver;
                                                                res = startInputUncheckedLocked(calledFromValidUser, inputContext, missingMethods, attribute, i, startInputReason);
                                                                didStart = true;
                                                            } else {
                                                                resultReceiver2 = resultReceiver;
                                                            }
                                                            showCurrentInputLocked(1, resultReceiver2);
                                                            break;
                                                        }
                                                    }
                                                }
                                                resultReceiver2 = resultReceiver;
                                                if (LayoutParams.mayUseInputMethod(windowFlags)) {
                                                    if (DEBUG_FLOW) {
                                                        Slog.v(TAG, "Unspecified window will hide input");
                                                    }
                                                    hideCurrentInputLocked(2, resultReceiver2);
                                                    break;
                                                }
                                                break;
                                            case 1:
                                                ident2 = ident4;
                                                ident4 = unverifiedTargetSdkVersion;
                                                break;
                                            case 2:
                                                resultReceiver = null;
                                                ident2 = ident4;
                                                ident4 = unverifiedTargetSdkVersion;
                                                if ((i2 & 256) != 0) {
                                                    if (DEBUG_FLOW) {
                                                        Slog.v(TAG, "Window asks to hide input going forward");
                                                    }
                                                    hideCurrentInputLocked(0, resultReceiver);
                                                    break;
                                                }
                                                break;
                                            case 3:
                                                resultReceiver = null;
                                                ident2 = ident4;
                                                ident4 = unverifiedTargetSdkVersion;
                                                if (DEBUG_FLOW) {
                                                    Slog.v(TAG, "Window asks to hide input");
                                                }
                                                hideCurrentInputLocked(0, resultReceiver);
                                                break;
                                            case 4:
                                                i5 = 1;
                                                ident2 = ident4;
                                                i4 = unverifiedTargetSdkVersion;
                                                if ((i2 & 256) != 0) {
                                                    if (DEBUG_FLOW) {
                                                        Slog.v(TAG, "Window asks to show input going forward");
                                                    }
                                                    if (!InputMethodUtils.isSoftInputModeStateVisibleAllowed(i4, i)) {
                                                        Slog.e(TAG, "SOFT_INPUT_STATE_VISIBLE is ignored because there is no focused view that also returns true from View#onCheckIsTextEditor()");
                                                        break;
                                                    }
                                                    if (attribute != null) {
                                                        didStart = true;
                                                        res = startInputUncheckedLocked(calledFromValidUser, inputContext, missingMethods, attribute, i, startInputReason);
                                                    }
                                                    showCurrentInputLocked(i5, null);
                                                    break;
                                                }
                                                break;
                                            case 5:
                                                long ident5;
                                                try {
                                                    if (DEBUG_FLOW) {
                                                        Slog.v(TAG, "Window asks to always show input");
                                                    }
                                                    ident5 = ident4;
                                                } catch (Throwable th4) {
                                                    th = th4;
                                                    ident2 = ident4;
                                                    ident4 = unverifiedTargetSdkVersion;
                                                    ident = ident2;
                                                    throw th;
                                                }
                                                try {
                                                    if (!InputMethodUtils.isSoftInputModeStateVisibleAllowed(unverifiedTargetSdkVersion, i)) {
                                                        ident2 = ident5;
                                                        Slog.e(TAG, "SOFT_INPUT_STATE_ALWAYS_VISIBLE is ignored because there is no focused view that also returns true from View#onCheckIsTextEditor()");
                                                        break;
                                                    }
                                                    if (attribute != null) {
                                                        resultReceiver2 = null;
                                                        ident2 = ident5;
                                                        i5 = 1;
                                                        didStart = true;
                                                        res = startInputUncheckedLocked(calledFromValidUser, inputContext, missingMethods, attribute, i, startInputReason);
                                                    } else {
                                                        ident2 = ident5;
                                                        i5 = 1;
                                                    }
                                                    showCurrentInputLocked(i5, null);
                                                    break;
                                                } catch (Throwable th5) {
                                                    th = th5;
                                                    ident = ident2;
                                                    throw th;
                                                }
                                            default:
                                                ident2 = ident4;
                                                ident4 = unverifiedTargetSdkVersion;
                                                break;
                                        }
                                        if (!didStart) {
                                            if (attribute != null) {
                                                if (DebugFlags.FLAG_OPTIMIZE_START_INPUT.value()) {
                                                    if ((i & 2) == 0) {
                                                        res = InputBindResult.NO_EDITOR;
                                                    }
                                                }
                                                res = startInputUncheckedLocked(calledFromValidUser, inputContext, missingMethods, attribute, i, startInputReason);
                                            } else {
                                                res = InputBindResult.NULL_EDITOR_INFO;
                                            }
                                        }
                                        Binder.restoreCallingIdentity(ident2);
                                        return res;
                                    }
                                }
                                z2 = true;
                                doAutoShow = z2;
                                if ((i & 2) == 0) {
                                }
                                isTextEditor = (i & 2) == 0;
                                didStart = false;
                                switch (i2 & 15) {
                                    case 0:
                                        break;
                                    case 1:
                                        break;
                                    case 2:
                                        break;
                                    case 3:
                                        break;
                                    case 4:
                                        break;
                                    case 5:
                                        break;
                                    default:
                                        break;
                                }
                                if (didStart) {
                                }
                                try {
                                    Binder.restoreCallingIdentity(ident2);
                                    return res;
                                } catch (Throwable th6) {
                                    th = th6;
                                    ident = ident2;
                                    throw th;
                                }
                            } catch (Throwable th7) {
                                th = th7;
                                ident = ident4;
                                ident4 = unverifiedTargetSdkVersion;
                                throw th;
                            }
                        } else if (attribute != null) {
                            calledFromValidUser = cs;
                            hashMap = hashMap2;
                            ident4 = ident3;
                            try {
                                inputBindResult = startInputUncheckedLocked(cs, inputContext, missingMethods, attribute, i, startInputReason);
                                Binder.restoreCallingIdentity(ident4);
                                return inputBindResult;
                            } catch (Throwable th8) {
                                th = th8;
                                ident = ident4;
                                ident4 = unverifiedTargetSdkVersion;
                                throw th;
                            }
                        } else {
                            hashMap = hashMap2;
                            ident4 = ident3;
                            z = calledFromValidUser;
                            calledFromValidUser = cs;
                            InputBindResult inputBindResult2 = new InputBindResult(3, null, null, null, -1, -1);
                            Binder.restoreCallingIdentity(ident4);
                            return inputBindResult2;
                        }
                    }
                    hashMap = hashMap2;
                    ident = ident3;
                    i4 = i3;
                    z = calledFromValidUser;
                    calledFromValidUser = cs;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("unknown client ");
                    stringBuilder2.append(client.asBinder());
                    throw new IllegalArgumentException(stringBuilder2.toString());
                } catch (Throwable th9) {
                    th = th9;
                    throw th;
                }
            }
        } catch (Throwable th10) {
            th = th10;
            ident = ident3;
            i4 = i3;
            z = calledFromValidUser;
            Binder.restoreCallingIdentity(ident);
            throw th;
        }
    }

    private boolean canShowInputMethodPickerLocked(IInputMethodClient client) {
        int uid = Binder.getCallingUid();
        if (UserHandle.getAppId(uid) == 1000) {
            return true;
        }
        if (this.mCurFocusedWindowClient != null && client != null && this.mCurFocusedWindowClient.client.asBinder() == client.asBinder()) {
            return true;
        }
        if ((this.mCurIntent == null || !InputMethodUtils.checkIfPackageBelongsToUid(this.mAppOpsManager, uid, this.mCurIntent.getComponent().getPackageName())) && this.mContext.checkCallingPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
            return false;
        }
        return true;
    }

    public void showInputMethodPickerFromClient(IInputMethodClient client, int auxiliarySubtypeMode) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                String str;
                StringBuilder stringBuilder;
                if (canShowInputMethodPickerLocked(client)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append(Binder.getCallingPid());
                    stringBuilder.append(":request to show input method dialog");
                    Slog.d(str, stringBuilder.toString());
                    this.mHandler.sendMessage(this.mCaller.obtainMessageII(1, auxiliarySubtypeMode, Binder.getCallingPid()));
                    return;
                }
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("Ignoring showInputMethodPickerFromClient of uid ");
                stringBuilder.append(Binder.getCallingUid());
                stringBuilder.append(": ");
                stringBuilder.append(client);
                Slog.w(str, stringBuilder.toString());
            }
        }
    }

    public boolean isInputMethodPickerShownForTest() {
        synchronized (this.mMethodMap) {
            if (this.mSwitchingDialog == null) {
                return false;
            }
            boolean isShowing = this.mSwitchingDialog.isShowing();
            return isShowing;
        }
    }

    public void setInputMethod(IBinder token, String id) {
        if (calledFromValidUser()) {
            setInputMethodWithSubtypeId(token, id, -1);
        }
    }

    public void setInputMethodAndSubtype(IBinder token, String id, InputMethodSubtype subtype) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                if (subtype != null) {
                    try {
                        setInputMethodWithSubtypeIdLocked(token, id, InputMethodUtils.getSubtypeIdFromHashCode((InputMethodInfo) this.mMethodMap.get(id), subtype.hashCode()));
                    } catch (Throwable th) {
                    }
                } else {
                    setInputMethod(token, id);
                }
            }
        }
    }

    public void showInputMethodAndSubtypeEnablerFromClient(IInputMethodClient client, String inputMethodId) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                executeOrSendMessage(this.mCurMethod, this.mCaller.obtainMessageO(2, inputMethodId));
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:50:0x00d7 A:{Catch:{ all -> 0x00db, all -> 0x00e0 }} */
    /* JADX WARNING: Removed duplicated region for block: B:45:0x00d0  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean switchToPreviousInputMethod(IBinder token) {
        Throwable th;
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            IBinder iBinder;
            try {
                InputMethodInfo lastImi;
                int currentSubtypeHash;
                int subtypeId;
                String targetLastImiId;
                Pair<String, String> lastIme = this.mSettings.getLastInputMethodAndSubtypeLocked();
                if (lastIme != null) {
                    lastImi = (InputMethodInfo) this.mMethodMap.get(lastIme.first);
                } else {
                    lastImi = null;
                }
                String targetLastImiId2 = null;
                int subtypeId2 = -1;
                if (!(lastIme == null || lastImi == null)) {
                    boolean imiIdIsSame = lastImi.getId().equals(this.mCurMethodId);
                    int lastSubtypeHash = Integer.parseInt((String) lastIme.second);
                    if (this.mCurrentSubtype == null) {
                        currentSubtypeHash = -1;
                    } else {
                        currentSubtypeHash = this.mCurrentSubtype.hashCode();
                    }
                    if (!(imiIdIsSame && lastSubtypeHash == currentSubtypeHash)) {
                        targetLastImiId2 = (String) lastIme.first;
                        subtypeId2 = InputMethodUtils.getSubtypeIdFromHashCode(lastImi, lastSubtypeHash);
                    }
                }
                if (TextUtils.isEmpty(targetLastImiId2) && !InputMethodUtils.canAddToLastInputMethod(this.mCurrentSubtype)) {
                    List<InputMethodInfo> enabled = this.mSettings.getEnabledInputMethodListLocked();
                    if (enabled != null) {
                        String locale;
                        currentSubtypeHash = enabled.size();
                        if (this.mCurrentSubtype == null) {
                            locale = this.mRes.getConfiguration().locale.toString();
                        } else {
                            locale = this.mCurrentSubtype.getLocale();
                        }
                        subtypeId = subtypeId2;
                        targetLastImiId = targetLastImiId2;
                        for (targetLastImiId2 = null; targetLastImiId2 < currentSubtypeHash; targetLastImiId2++) {
                            InputMethodInfo imi = (InputMethodInfo) enabled.get(targetLastImiId2);
                            if (imi.getSubtypeCount() > 0 && InputMethodUtils.isSystemIme(imi)) {
                                InputMethodSubtype keyboardSubtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, InputMethodUtils.getSubtypes(imi), "keyboard", locale, true);
                                if (keyboardSubtype != null) {
                                    targetLastImiId = imi.getId();
                                    subtypeId = InputMethodUtils.getSubtypeIdFromHashCode(imi, keyboardSubtype.hashCode());
                                    if (keyboardSubtype.getLocale().equals(locale)) {
                                        break;
                                    }
                                } else {
                                    continue;
                                }
                            }
                        }
                        if (TextUtils.isEmpty(targetLastImiId)) {
                            setInputMethodWithSubtypeIdLocked(token, targetLastImiId, subtypeId);
                            return true;
                        }
                        iBinder = token;
                        return false;
                    }
                }
                subtypeId = subtypeId2;
                targetLastImiId = targetLastImiId2;
                if (TextUtils.isEmpty(targetLastImiId)) {
                }
            } catch (Throwable th2) {
                th = th2;
                throw th;
            }
        }
    }

    public boolean switchToNextInputMethod(IBinder token, boolean onlyCurrentIme) {
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            if (calledWithValidToken(token)) {
                ImeSubtypeListItem nextSubtype = this.mSwitchingController.getNextInputMethodLocked(onlyCurrentIme, (InputMethodInfo) this.mMethodMap.get(this.mCurMethodId), this.mCurrentSubtype, true);
                if (nextSubtype == null) {
                    return false;
                }
                setInputMethodWithSubtypeIdLocked(token, nextSubtype.mImi.getId(), nextSubtype.mSubtypeId);
                return true;
            }
            return false;
        }
    }

    public boolean shouldOfferSwitchingToNextInputMethod(IBinder token) {
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            if (!calledWithValidToken(token)) {
                return false;
            } else if (this.mSwitchingController.getNextInputMethodLocked(false, (InputMethodInfo) this.mMethodMap.get(this.mCurMethodId), this.mCurrentSubtype, true) == null) {
                return false;
            } else {
                return true;
            }
        }
    }

    /* JADX WARNING: Missing block: B:28:0x0053, code skipped:
            return null;
     */
    /* JADX WARNING: Missing block: B:33:0x0058, code skipped:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public InputMethodSubtype getLastInputMethodSubtype() {
        if (!calledFromValidUser()) {
            return null;
        }
        synchronized (this.mMethodMap) {
            Pair<String, String> lastIme = this.mSettings.getLastInputMethodAndSubtypeLocked();
            if (!(lastIme == null || TextUtils.isEmpty((CharSequence) lastIme.first))) {
                if (!TextUtils.isEmpty((CharSequence) lastIme.second)) {
                    InputMethodInfo lastImi = (InputMethodInfo) this.mMethodMap.get(lastIme.first);
                    if (lastImi == null) {
                        return null;
                    }
                    try {
                        int lastSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(lastImi, Integer.parseInt((String) lastIme.second));
                        if (lastSubtypeId >= 0) {
                            if (lastSubtypeId < lastImi.getSubtypeCount()) {
                                InputMethodSubtype subtypeAt = lastImi.getSubtypeAt(lastSubtypeId);
                                return subtypeAt;
                            }
                        }
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:36:0x005f, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void setAdditionalInputMethodSubtypes(String imiId, InputMethodSubtype[] subtypes) {
        if (calledFromValidUser() && !TextUtils.isEmpty(imiId) && subtypes != null) {
            synchronized (this.mMethodMap) {
                if (this.mSystemReady) {
                    InputMethodInfo imi = (InputMethodInfo) this.mMethodMap.get(imiId);
                    if (imi == null) {
                        return;
                    }
                    try {
                        String[] packageInfos = this.mIPackageManager.getPackagesForUid(Binder.getCallingUid());
                        if (packageInfos != null) {
                            int packageNum = packageInfos.length;
                            int i = 0;
                            while (i < packageNum) {
                                if (packageInfos[i].equals(imi.getPackageName())) {
                                    this.mFileManager.addInputMethodSubtypes(imi, subtypes);
                                    long ident = Binder.clearCallingIdentity();
                                    try {
                                        buildInputMethodListLocked(false);
                                        return;
                                    } finally {
                                        Binder.restoreCallingIdentity(ident);
                                    }
                                } else {
                                    i++;
                                }
                            }
                        }
                    } catch (RemoteException e) {
                        Slog.e(TAG, "Failed to get package infos");
                        return;
                    }
                }
            }
        }
    }

    public int getInputMethodWindowVisibleHeight() {
        return this.mWindowManagerInternal.getInputMethodWindowVisibleHeight();
    }

    public void clearLastInputMethodWindowForTransition(IBinder token) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                if (calledWithValidToken(token)) {
                    this.mWindowManagerInternal.clearLastInputMethodWindowForTransition();
                    return;
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:11:0x001d, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void notifyUserAction(int sequenceNumber) {
        synchronized (this.mMethodMap) {
            if (this.mCurUserActionNotificationSequenceNumber != sequenceNumber) {
                return;
            }
            InputMethodInfo imi = (InputMethodInfo) this.mMethodMap.get(this.mCurMethodId);
            if (imi != null) {
                this.mSwitchingController.onUserActionLocked(imi, this.mCurrentSubtype);
            }
        }
    }

    private void setInputMethodWithSubtypeId(IBinder token, String id, int subtypeId) {
        synchronized (this.mMethodMap) {
            setInputMethodWithSubtypeIdLocked(token, id, subtypeId);
        }
    }

    private void setInputMethodWithSubtypeIdLocked(IBinder token, String id, int subtypeId) {
        if (token == null) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") != 0) {
                throw new SecurityException("Using null token requires permission android.permission.WRITE_SECURE_SETTINGS");
            }
        } else if (this.mCurToken != token) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Ignoring setInputMethod of uid ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(" token: ");
            stringBuilder.append(token);
            Slog.w(str, stringBuilder.toString());
            return;
        }
        long ident = Binder.clearCallingIdentity();
        try {
            setInputMethodLocked(id, subtypeId);
        } finally {
            Binder.restoreCallingIdentity(ident);
        }
    }

    public void hideMySoftInput(IBinder token, int flags) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                if (calledWithValidToken(token)) {
                    if (DEBUG_FLOW) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("hideMySoftInput, pid=");
                        stringBuilder.append(Binder.getCallingPid());
                        stringBuilder.append(", token=");
                        stringBuilder.append(token);
                        Slog.v(str, stringBuilder.toString());
                    }
                    long ident = Binder.clearCallingIdentity();
                    try {
                        hideCurrentInputLocked(flags, null);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }
        }
    }

    public void showMySoftInput(IBinder token, int flags) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                if (calledWithValidToken(token)) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        showCurrentInputLocked(flags, null);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                }
            }
        }
    }

    void setEnabledSessionInMainThread(SessionState session) {
        if (this.mEnabledSession != session) {
            if (!(this.mEnabledSession == null || this.mEnabledSession.session == null)) {
                try {
                    this.mEnabledSession.method.setSessionEnabled(this.mEnabledSession.session, false);
                } catch (RemoteException e) {
                }
            }
            this.mEnabledSession = session;
            if (this.mEnabledSession != null && this.mEnabledSession.session != null) {
                try {
                    this.mEnabledSession.method.setSessionEnabled(this.mEnabledSession.session, true);
                } catch (RemoteException e2) {
                }
            }
        }
    }

    /* JADX WARNING: Missing block: B:53:0x00fd, code skipped:
            if (android.os.Binder.isProxy(r1) != false) goto L_0x00ff;
     */
    /* JADX WARNING: Missing block: B:54:0x00ff, code skipped:
            r3.channel.dispose();
     */
    /* JADX WARNING: Missing block: B:62:0x0128, code skipped:
            if (android.os.Binder.isProxy(r1) != false) goto L_0x00ff;
     */
    /* JADX WARNING: Missing block: B:92:0x01a0, code skipped:
            if (android.os.Binder.isProxy(r1) != false) goto L_0x01b9;
     */
    /* JADX WARNING: Missing block: B:102:0x01b7, code skipped:
            if (android.os.Binder.isProxy(r1) != false) goto L_0x01b9;
     */
    /* JADX WARNING: Missing block: B:103:0x01b9, code skipped:
            r3.dispose();
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean handleMessage(Message msg) {
        String str;
        StringBuilder stringBuilder;
        boolean z = false;
        boolean showAuxSubtypes;
        SomeArgs args;
        int missingMethods;
        ClientState clientState;
        switch (msg.what) {
            case 1:
                switch (msg.arg1) {
                    case 0:
                        showAuxSubtypes = this.mInputShown;
                        break;
                    case 1:
                        showAuxSubtypes = true;
                        break;
                    case 2:
                        showAuxSubtypes = false;
                        break;
                    default:
                        String str2 = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unknown subtype picker mode = ");
                        stringBuilder2.append(msg.arg1);
                        Slog.e(str2, stringBuilder2.toString());
                        return false;
                }
                showInputMethodMenu(showAuxSubtypes, msg.arg2);
                return true;
            case 2:
                showInputMethodAndSubtypeEnabler((String) msg.obj);
                return true;
            case 3:
                showConfigureInputMethods();
                return true;
            case 1000:
                try {
                    ((IInputMethod) msg.obj).unbindInput();
                } catch (RemoteException e) {
                }
                return true;
            case MSG_BIND_INPUT /*1010*/:
                args = (SomeArgs) msg.obj;
                try {
                    ((IInputMethod) args.arg1).bindInput((InputBinding) args.arg2);
                } catch (RemoteException e2) {
                }
                args.recycle();
                return true;
            case MSG_SHOW_SOFT_INPUT /*1020*/:
                args = (SomeArgs) msg.obj;
                try {
                    ((IInputMethod) args.arg1).showSoftInput(msg.arg1, (ResultReceiver) args.arg2);
                } catch (RemoteException e3) {
                }
                args.recycle();
                return true;
            case MSG_HIDE_SOFT_INPUT /*1030*/:
                args = (SomeArgs) msg.obj;
                try {
                    ((IInputMethod) args.arg1).hideSoftInput(0, (ResultReceiver) args.arg2);
                } catch (RemoteException e4) {
                }
                args.recycle();
                return true;
            case MSG_HIDE_CURRENT_INPUT_METHOD /*1035*/:
                synchronized (this.mMethodMap) {
                    hideCurrentInputLocked(0, null);
                }
                return true;
            case MSG_ATTACH_TOKEN /*1040*/:
                args = (SomeArgs) msg.obj;
                try {
                    ((IInputMethod) args.arg1).attachToken((IBinder) args.arg2);
                } catch (RemoteException e5) {
                }
                args.recycle();
                return true;
            case MSG_CREATE_SESSION /*1050*/:
                args = msg.obj;
                IInputMethod method = args.arg1;
                InputChannel channel = args.arg2;
                try {
                    method.createSession(channel, (IInputSessionCallback) args.arg3);
                    if (channel != null) {
                        break;
                    }
                } catch (RemoteException e6) {
                    if (channel != null) {
                        break;
                    }
                } catch (Throwable th) {
                    if (channel != null && Binder.isProxy(method)) {
                        channel.dispose();
                    }
                }
                args.recycle();
                return true;
            case 2000:
                missingMethods = msg.arg1;
                boolean restarting = msg.arg2 != 0;
                SomeArgs args2 = msg.obj;
                IBinder startInputToken = args2.arg1;
                SessionState session = args2.arg2;
                IInputContext inputContext = args2.arg3;
                EditorInfo editorInfo = args2.arg4;
                try {
                    setEnabledSessionInMainThread(session);
                    session.method.startInput(startInputToken, inputContext, missingMethods, editorInfo, restarting);
                } catch (RemoteException e7) {
                }
                args2.recycle();
                return true;
            case MSG_START_VR_INPUT /*2010*/:
                startVrInputMethodNoCheck((ComponentName) msg.obj);
                return true;
            case MSG_UNBIND_CLIENT /*3000*/:
                try {
                    ((IInputMethodClient) msg.obj).onUnbindMethod(msg.arg1, msg.arg2);
                } catch (RemoteException e8) {
                }
                return true;
            case 3010:
                args = msg.obj;
                IInputMethodClient client = args.arg1;
                InputBindResult res = args.arg2;
                try {
                    client.onBindMethod(res);
                    if (res.channel != null) {
                        break;
                    }
                } catch (RemoteException e9) {
                    String str3 = TAG;
                    StringBuilder stringBuilder3 = new StringBuilder();
                    stringBuilder3.append("Client died receiving input method ");
                    stringBuilder3.append(args.arg2);
                    Slog.w(str3, stringBuilder3.toString());
                    if (res.channel != null) {
                        break;
                    }
                } catch (Throwable th2) {
                    if (res.channel != null && Binder.isProxy(client)) {
                        res.channel.dispose();
                    }
                }
                args.recycle();
                return true;
            case MSG_SET_ACTIVE /*3020*/:
                try {
                    IInputMethodClient iInputMethodClient = ((ClientState) msg.obj).client;
                    boolean z2 = msg.arg1 != 0;
                    if (msg.arg2 != 0) {
                        z = true;
                    }
                    iInputMethodClient.setActive(z2, z);
                } catch (RemoteException e10) {
                    String str4 = TAG;
                    StringBuilder stringBuilder4 = new StringBuilder();
                    stringBuilder4.append("Got RemoteException sending setActive(false) notification to pid ");
                    stringBuilder4.append(((ClientState) msg.obj).pid);
                    stringBuilder4.append(" uid ");
                    stringBuilder4.append(((ClientState) msg.obj).uid);
                    Slog.w(str4, stringBuilder4.toString());
                }
                return true;
            case MSG_SET_INTERACTIVE /*3030*/:
                if (msg.arg1 != 0) {
                    z = true;
                }
                handleSetInteractive(z);
                return true;
            case 3040:
                missingMethods = msg.arg1;
                clientState = (ClientState) msg.obj;
                try {
                    clientState.client.setUserActionNotificationSequenceNumber(missingMethods);
                } catch (RemoteException e11) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Got RemoteException sending setUserActionNotificationSequenceNumber(");
                    stringBuilder.append(missingMethods);
                    stringBuilder.append(") notification to pid ");
                    stringBuilder.append(clientState.pid);
                    stringBuilder.append(" uid ");
                    stringBuilder.append(clientState.uid);
                    Slog.w(str, stringBuilder.toString());
                }
                return true;
            case MSG_REPORT_FULLSCREEN_MODE /*3045*/:
                if (msg.arg1 != 0) {
                    z = true;
                }
                showAuxSubtypes = z;
                clientState = msg.obj;
                try {
                    clientState.client.reportFullscreenMode(showAuxSubtypes);
                } catch (RemoteException e12) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Got RemoteException sending reportFullscreen(");
                    stringBuilder.append(showAuxSubtypes);
                    stringBuilder.append(") notification to pid=");
                    stringBuilder.append(clientState.pid);
                    stringBuilder.append(" uid=");
                    stringBuilder.append(clientState.uid);
                    Slog.w(str, stringBuilder.toString());
                }
                return true;
            case 3050:
                if (msg.arg1 != 0) {
                    z = true;
                }
                handleSwitchInputMethod(z);
                return true;
            case MSG_HARD_KEYBOARD_SWITCH_CHANGED /*4000*/:
                HardKeyboardListener hardKeyboardListener = this.mHardKeyboardListener;
                if (msg.arg1 == 1) {
                    z = true;
                }
                hardKeyboardListener.handleHardKeyboardStatusChange(z);
                return true;
            case MSG_SYSTEM_UNLOCK_USER /*5000*/:
                onUnlockUser(msg.arg1);
                return true;
            default:
                return false;
        }
    }

    private void handleSetInteractive(boolean interactive) {
        synchronized (this.mMethodMap) {
            this.mIsInteractive = interactive;
            updateSystemUiLocked(this.mCurToken, interactive ? this.mImeWindowVis : 0, this.mBackDisposition);
            if (!(this.mCurClient == null || this.mCurClient.client == null)) {
                executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIIO(MSG_SET_ACTIVE, this.mIsInteractive, this.mInFullscreenMode, this.mCurClient));
            }
        }
    }

    /* JADX WARNING: Missing block: B:19:0x0059, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void handleSwitchInputMethod(boolean forwardDirection) {
        synchronized (this.mMethodMap) {
            ImeSubtypeListItem nextSubtype = this.mSwitchingController.getNextInputMethodLocked(false, (InputMethodInfo) this.mMethodMap.get(this.mCurMethodId), this.mCurrentSubtype, forwardDirection);
            if (nextSubtype == null) {
                return;
            }
            setInputMethodLocked(nextSubtype.mImi.getId(), nextSubtype.mSubtypeId);
            InputMethodInfo newInputMethodInfo = (InputMethodInfo) this.mMethodMap.get(this.mCurMethodId);
            if (newInputMethodInfo == null) {
                return;
            }
            CharSequence toastText = InputMethodUtils.getImeAndSubtypeDisplayName(this.mContext, newInputMethodInfo, this.mCurrentSubtype);
            if (!TextUtils.isEmpty(toastText)) {
                if (this.mSubtypeSwitchedByShortCutToast == null) {
                    this.mSubtypeSwitchedByShortCutToast = Toast.makeText(this.mContext, toastText, 0);
                } else {
                    this.mSubtypeSwitchedByShortCutToast.setText(toastText);
                }
                this.mSubtypeSwitchedByShortCutToast.show();
            }
        }
    }

    private boolean chooseNewDefaultIMELocked() {
        if (this.mLastIME == null || !this.mMethodMap.containsKey(this.mLastIME)) {
            InputMethodInfo imi = InputMethodUtils.getMostApplicableDefaultIME(this.mSettings.getEnabledInputMethodListLocked());
            if (imi != null) {
                if (DEBUG_FLOW) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("New default IME was selected: ");
                    stringBuilder.append(imi.getId());
                    Slog.d(str, stringBuilder.toString());
                }
                this.mLastIME = imi.getId();
                resetSelectedInputMethodAndSubtypeLocked(imi.getId());
                return true;
            }
            if (imi == null) {
                Slog.w(TAG, "NO default IME was selected: ");
            }
            return false;
        }
        resetSelectedInputMethodAndSubtypeLocked(this.mLastIME);
        return true;
    }

    private int getComponentMatchingFlags(int baseFlags) {
        synchronized (this.mMethodMap) {
            if (this.mBindInstantServiceAllowed) {
                baseFlags |= DumpState.DUMP_VOLUMES;
            }
        }
        return baseFlags;
    }

    @GuardedBy("mMethodMap")
    void buildInputMethodListLocked(boolean resetDefaultEnabledIme) {
        if (this.mSystemReady) {
            int i;
            this.mMethodList.clear();
            this.mMethodMap.clear();
            this.mMethodMapUpdateCount++;
            this.mMyPackageMonitor.clearKnownImePackageNamesLocked();
            PackageManager pm = this.mContext.getPackageManager();
            List<ResolveInfo> services = pm.queryIntentServicesAsUser(new Intent("android.view.InputMethod"), getComponentMatchingFlags(32896), this.mSettings.getCurrentUserId());
            if (services.size() == 0) {
                Slog.e(TAG, "There is no input method available in the system");
            }
            HashMap<String, List<InputMethodSubtype>> additionalSubtypeMap = this.mFileManager.getAllAdditionalInputMethodSubtypes();
            int i2 = 0;
            for (int i3 = 0; i3 < services.size(); i3++) {
                ResolveInfo ri = (ResolveInfo) services.get(i3);
                ServiceInfo si = ri.serviceInfo;
                String imeId = InputMethodInfo.computeId(ri);
                String str;
                StringBuilder stringBuilder;
                if (!"android.permission.BIND_INPUT_METHOD".equals(si.permission)) {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Skipping input method ");
                    stringBuilder.append(imeId);
                    stringBuilder.append(": it does not require the permission ");
                    stringBuilder.append("android.permission.BIND_INPUT_METHOD");
                    Slog.w(str, stringBuilder.toString());
                } else if (shouldBuildInputMethodList(si.packageName)) {
                    String id;
                    try {
                        InputMethodInfo p = new InputMethodInfo(this.mContext, ri, (List) additionalSubtypeMap.get(imeId));
                        this.mMethodList.add(p);
                        id = p.getId();
                        this.mMethodMap.put(id, p);
                        ensureEnableSystemIME(id, p, this.mContext, this.mSettings.getCurrentUserId());
                    } catch (Exception e) {
                        id = TAG;
                        StringBuilder stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("Unable to load input method ");
                        stringBuilder2.append(imeId);
                        Slog.wtf(id, stringBuilder2.toString(), e);
                    }
                } else {
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("buildInputMethodListLocked: Skipping IME ");
                    stringBuilder.append(si.packageName);
                    Slog.w(str, stringBuilder.toString());
                }
            }
            HwPCUtils.setInputMethodList(new ArrayList(this.mMethodList));
            updateSecureIMEStatus();
            List<ResolveInfo> allInputMethodServices = pm.queryIntentServicesAsUser(new Intent("android.view.InputMethod"), getComponentMatchingFlags(512), this.mSettings.getCurrentUserId());
            int N = allInputMethodServices.size();
            for (i = 0; i < N; i++) {
                ServiceInfo si2 = ((ResolveInfo) allInputMethodServices.get(i)).serviceInfo;
                if ("android.permission.BIND_INPUT_METHOD".equals(si2.permission)) {
                    this.mMyPackageMonitor.addKnownImePackageNameLocked(si2.packageName);
                }
            }
            boolean reenableMinimumNonAuxSystemImes = false;
            if (!resetDefaultEnabledIme) {
                boolean enabledNonAuxImeFound = false;
                List<InputMethodInfo> enabledImes = this.mSettings.getEnabledInputMethodListLocked();
                int N2 = enabledImes.size();
                boolean enabledImeFound = false;
                for (N = 0; N < N2; N++) {
                    InputMethodInfo imi = (InputMethodInfo) enabledImes.get(N);
                    if (this.mMethodList.contains(imi)) {
                        enabledImeFound = true;
                        if (!imi.isAuxiliaryIme()) {
                            enabledNonAuxImeFound = true;
                            break;
                        }
                    }
                }
                if (!enabledImeFound) {
                    resetDefaultEnabledIme = true;
                    resetSelectedInputMethodAndSubtypeLocked(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                } else if (!enabledNonAuxImeFound) {
                    reenableMinimumNonAuxSystemImes = true;
                }
            }
            if (resetDefaultEnabledIme || reenableMinimumNonAuxSystemImes) {
                ArrayList<InputMethodInfo> defaultEnabledIme = InputMethodUtils.getDefaultEnabledImes(this.mContext, this.mMethodList, reenableMinimumNonAuxSystemImes);
                i = defaultEnabledIme.size();
                while (i2 < i) {
                    setInputMethodEnabledLocked(((InputMethodInfo) defaultEnabledIme.get(i2)).getId(), true);
                    i2++;
                }
            }
            String defaultImiId = this.mSettings.getSelectedInputMethod();
            if (!TextUtils.isEmpty(defaultImiId)) {
                if (this.mMethodMap.containsKey(defaultImiId)) {
                    setInputMethodEnabledLocked(defaultImiId, true);
                } else {
                    Slog.w(TAG, "Default IME is uninstalled. Choose new default IME.");
                    if (chooseNewDefaultIMELocked()) {
                        updateInputMethodsFromSettingsLocked(true);
                    }
                }
            }
            this.mSwitchingController.resetCircularListLocked(this.mContext);
            return;
        }
        Slog.e(TAG, "buildInputMethodListLocked is not allowed until system is ready");
    }

    private void showInputMethodAndSubtypeEnabler(String inputMethodId) {
        int userId;
        Intent intent = new Intent("android.settings.INPUT_METHOD_SUBTYPE_SETTINGS");
        intent.setFlags(337641472);
        if (!TextUtils.isEmpty(inputMethodId)) {
            intent.putExtra("input_method_id", inputMethodId);
        }
        synchronized (this.mMethodMap) {
            userId = this.mSettings.getCurrentUserId();
        }
        this.mContext.startActivityAsUser(intent, null, UserHandle.of(userId));
    }

    private void showConfigureInputMethods() {
        Intent intent = new Intent("android.settings.INPUT_METHOD_SETTINGS");
        intent.setFlags(337641472);
        this.mContext.startActivityAsUser(intent, null, UserHandle.CURRENT);
    }

    private boolean isScreenLocked() {
        return this.mKeyguardManager != null && this.mKeyguardManager.isKeyguardLocked() && this.mKeyguardManager.isKeyguardSecure();
    }

    /* JADX WARNING: Missing block: B:82:0x0204, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void showInputMethodMenu(boolean showAuxSubtypes, int pID) {
        Throwable mSwitchSectionView;
        Context context = this.mContext;
        boolean isScreenLocked = isScreenLocked();
        String lastInputMethodId = this.mSettings.getSelectedInputMethod();
        int lastInputMethodSubtypeId = this.mSettings.getSelectedInputMethodSubtypeId(lastInputMethodId);
        synchronized (this.mMethodMap) {
            boolean z;
            Context context2;
            boolean z2;
            String str;
            try {
                HashMap<InputMethodInfo, List<InputMethodSubtype>> immis = this.mSettings.getExplicitlyOrImplicitlyEnabledInputMethodsAndSubtypeListLocked(this.mContext);
                HashMap<InputMethodInfo, List<InputMethodSubtype>> hashMap;
                if (immis == null) {
                    z = showAuxSubtypes;
                    hashMap = immis;
                    context2 = context;
                    z2 = isScreenLocked;
                    str = lastInputMethodId;
                } else if (immis.size() == 0) {
                    z = showAuxSubtypes;
                    hashMap = immis;
                    context2 = context;
                    z2 = isScreenLocked;
                } else {
                    List<ImeSubtypeListItem> imList;
                    int checkedItem;
                    int subtypeId;
                    ContextThemeWrapper themeContext;
                    Context dialogContext;
                    hideInputMethodMenuLocked();
                    try {
                        imList = this.mSwitchingController.getSortedInputMethodAndSubtypeListLocked(showAuxSubtypes, isScreenLocked);
                        if (lastInputMethodSubtypeId == -1) {
                            try {
                                InputMethodSubtype currentSubtype = getCurrentInputMethodSubtypeLocked();
                                if (currentSubtype != null) {
                                    lastInputMethodSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode((InputMethodInfo) this.mMethodMap.get(this.mCurMethodId), currentSubtype.hashCode());
                                }
                            } catch (Throwable th) {
                                mSwitchSectionView = th;
                                throw mSwitchSectionView;
                            }
                        }
                        int N = imList.size();
                        this.mIms = new InputMethodInfo[N];
                        this.mSubtypeIds = new int[N];
                        checkedItem = -1;
                        for (int i = 0; i < N; i++) {
                            ImeSubtypeListItem item = (ImeSubtypeListItem) imList.get(i);
                            this.mIms[i] = item.mImi;
                            this.mSubtypeIds[i] = item.mSubtypeId;
                            if (this.mIms[i].getId().equals(lastInputMethodId)) {
                                subtypeId = this.mSubtypeIds[i];
                                if (subtypeId == -1 || ((lastInputMethodSubtypeId == -1 && subtypeId == 0) || subtypeId == lastInputMethodSubtypeId)) {
                                    checkedItem = i;
                                }
                            }
                        }
                        int themeID = context.getResources().getIdentifier("androidhwext:style/Theme.Emui.Dialog.Alert", null, null);
                        themeContext = new ContextThemeWrapper(context, themeID);
                        this.mDialogBuilder = new Builder(themeContext, themeID);
                        this.mDialogBuilder.setOnCancelListener(new OnCancelListener() {
                            public void onCancel(DialogInterface dialog) {
                                InputMethodManagerService.this.hideInputMethodMenu();
                            }
                        });
                        dialogContext = this.mDialogBuilder.getContext();
                        context2 = context;
                    } catch (Throwable th2) {
                        mSwitchSectionView = th2;
                        context2 = context;
                        z2 = isScreenLocked;
                        str = lastInputMethodId;
                        throw mSwitchSectionView;
                    }
                    try {
                        TypedArray a = dialogContext.obtainStyledAttributes(null, R.styleable.DialogPreference, 16842845, null);
                        Drawable dialogIcon = a.getDrawable(2);
                        a.recycle();
                        this.mDialogBuilder.setIcon(dialogIcon);
                        LayoutInflater inflater = (LayoutInflater) dialogContext.getSystemService(LayoutInflater.class);
                        View tv = inflater.inflate(34013191, null);
                        this.mDialogBuilder.setCustomTitle(tv);
                        this.mSwitchingDialogTitleView = tv;
                        tv = this.mSwitchingDialogTitleView.findViewById(34603134);
                        if (tv == null) {
                            try {
                                Slog.e(TAG, "mSwitchSectionView is null");
                                return;
                            } catch (Throwable th3) {
                                mSwitchSectionView = th3;
                                throw mSwitchSectionView;
                            }
                        }
                        LayoutInflater layoutInflater = inflater;
                        if (this.mWindowManagerInternal.isHardKeyboardAvailable()) {
                            subtypeId = 0;
                        } else {
                            subtypeId = 8;
                        }
                        tv.setVisibility(subtypeId);
                        Switch hardKeySwitch = (Switch) this.mSwitchingDialogTitleView.findViewById(34603135);
                        hardKeySwitch.setChecked(this.mShowImeWithHardKeyboard);
                        hardKeySwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                                InputMethodManagerService.this.mSettings.setShowImeWithHardKeyboard(isChecked);
                                InputMethodManagerService.this.hideInputMethodMenu();
                            }
                        });
                        final ImeSubtypeListAdapter adapter = new ImeSubtypeListAdapter(themeContext, 17367285, imList, checkedItem);
                        this.mDialogBuilder.setSingleChoiceItems(adapter, checkedItem, new OnClickListener() {
                            /* JADX WARNING: Missing block: B:23:0x00a1, code skipped:
            return;
     */
                            /* JADX WARNING: Missing block: B:25:0x00a3, code skipped:
            return;
     */
                            /* Code decompiled incorrectly, please refer to instructions dump. */
                            public void onClick(DialogInterface dialog, int which) {
                                synchronized (InputMethodManagerService.this.mMethodMap) {
                                    if (!(InputMethodManagerService.this.mIms == null || InputMethodManagerService.this.mIms.length <= which || InputMethodManagerService.this.mSubtypeIds == null)) {
                                        if (InputMethodManagerService.this.mSubtypeIds.length > which) {
                                            InputMethodInfo im = InputMethodManagerService.this.mIms[which];
                                            int subtypeId = InputMethodManagerService.this.mSubtypeIds[which];
                                            adapter.mCheckedItem = which;
                                            adapter.notifyDataSetChanged();
                                            InputMethodManagerService.this.hideInputMethodMenu();
                                            if (im != null) {
                                                if (subtypeId < 0 || subtypeId >= im.getSubtypeCount()) {
                                                    subtypeId = -1;
                                                }
                                                InputMethodManagerService.this.mLastInputShown = InputMethodManagerService.this.mInputShown;
                                                if (im.getId() != null) {
                                                    String str = InputMethodManagerService.TAG;
                                                    StringBuilder stringBuilder = new StringBuilder();
                                                    stringBuilder.append("ime choosed, issame: ");
                                                    stringBuilder.append(im.getId().equals(InputMethodManagerService.this.mCurMethodId));
                                                    stringBuilder.append(",lastInputShown: ");
                                                    stringBuilder.append(InputMethodManagerService.this.mLastInputShown);
                                                    Slog.i(str, stringBuilder.toString());
                                                }
                                                InputMethodManagerService.this.mCurInputId = im.getId();
                                                InputMethodManagerService.this.setInputMethodLocked(im.getId(), subtypeId);
                                            }
                                        }
                                    }
                                }
                            }
                        });
                        String packageName = getPackageName(pID);
                        String str2 = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        str = lastInputMethodId;
                        try {
                            stringBuilder.append("showInputMethodMenu packageName: ");
                            lastInputMethodId = packageName;
                            stringBuilder.append(lastInputMethodId);
                            Slog.i(str2, stringBuilder.toString());
                            if (isScreenLocked) {
                            } else {
                                if (lastInputMethodId != null) {
                                    try {
                                        if (lastInputMethodId.equals("com.android.settings")) {
                                            z2 = isScreenLocked;
                                        }
                                    } catch (Throwable th4) {
                                        mSwitchSectionView = th4;
                                        z2 = isScreenLocked;
                                        throw mSwitchSectionView;
                                    }
                                }
                                z2 = isScreenLocked;
                                this.mDialogBuilder.setPositiveButton(true, new OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        InputMethodManagerService.this.showConfigureInputMethods();
                                    }
                                });
                            }
                            this.mSwitchingDialog = this.mDialogBuilder.create();
                            this.mSwitchingDialog.setCanceledOnTouchOutside(true);
                            Window w = this.mSwitchingDialog.getWindow();
                            LayoutParams attrs = w.getAttributes();
                            w.setType(2012);
                            attrs.token = this.mSwitchingDialogToken;
                            attrs.privateFlags |= 16;
                            attrs.setTitle("Select input method");
                            w.setAttributes(attrs);
                            updateSystemUi(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
                            this.mSwitchingDialog.show();
                        } catch (Throwable th5) {
                            mSwitchSectionView = th5;
                            throw mSwitchSectionView;
                        }
                    } catch (Throwable th6) {
                        mSwitchSectionView = th6;
                        z2 = isScreenLocked;
                        str = lastInputMethodId;
                        throw mSwitchSectionView;
                    }
                }
            } catch (Throwable th7) {
                mSwitchSectionView = th7;
                z = showAuxSubtypes;
                context2 = context;
                z2 = isScreenLocked;
                str = lastInputMethodId;
                throw mSwitchSectionView;
            }
        }
    }

    protected String getPackageName(int pID) {
        String processName = BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS;
        List<RunningAppProcessInfo> appProcessList = ((ActivityManager) this.mContext.getSystemService("activity")).getRunningAppProcesses();
        if (appProcessList == null) {
            return null;
        }
        for (RunningAppProcessInfo appProcess : appProcessList) {
            if (appProcess.pid == pID) {
                return appProcess.processName;
            }
        }
        return null;
    }

    void hideInputMethodMenu() {
        synchronized (this.mMethodMap) {
            hideInputMethodMenuLocked();
        }
    }

    void hideInputMethodMenuLocked() {
        if (this.mSwitchingDialog != null) {
            this.mSwitchingDialog.dismiss();
            this.mSwitchingDialog = null;
            this.mSwitchingDialogTitleView = null;
        }
        updateSystemUiLocked(this.mCurToken, this.mImeWindowVis, this.mBackDisposition);
        this.mDialogBuilder = null;
        this.mIms = null;
    }

    boolean setInputMethodEnabledLocked(String id, boolean enabled) {
        if (((InputMethodInfo) this.mMethodMap.get(id)) != null) {
            List<Pair<String, ArrayList<String>>> enabledInputMethodsList = this.mSettings.getEnabledInputMethodsAndSubtypeListLocked();
            if (enabled) {
                for (Pair<String, ArrayList<String>> pair : enabledInputMethodsList) {
                    if (((String) pair.first).equals(id)) {
                        return true;
                    }
                }
                this.mSettings.appendAndPutEnabledInputMethodLocked(id, false);
                return false;
            }
            if (!this.mSettings.buildAndPutEnabledInputMethodsStrRemovingIdLocked(new StringBuilder(), enabledInputMethodsList, id)) {
                return false;
            }
            if (id.equals(this.mSettings.getSelectedInputMethod()) && !chooseNewDefaultIMELocked()) {
                Slog.i(TAG, "Can't find new IME, unsetting the current input method.");
                resetSelectedInputMethodAndSubtypeLocked(BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
            }
            return true;
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Unknown id: ");
        stringBuilder.append(this.mCurMethodId);
        throw new IllegalArgumentException(stringBuilder.toString());
    }

    private void setSelectedInputMethodAndSubtypeLocked(InputMethodInfo imi, int subtypeId, boolean setSubtypeOnly) {
        if (imi == null || !isSecureIME(imi.getPackageName())) {
            boolean isVrInput = imi != null && imi.isVrOnly();
            if (!isVrInput) {
                this.mSettings.saveCurrentInputMethodAndSubtypeToHistory(this.mCurMethodId, this.mCurrentSubtype);
            }
            this.mCurUserActionNotificationSequenceNumber = Math.max(this.mCurUserActionNotificationSequenceNumber + 1, 1);
            if (!(this.mCurClient == null || this.mCurClient.client == null)) {
                executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIO(3040, this.mCurUserActionNotificationSequenceNumber, this.mCurClient));
            }
            if (!isVrInput) {
                if (imi == null || subtypeId < 0) {
                    this.mSettings.putSelectedSubtype(-1);
                    this.mCurrentSubtype = null;
                } else if (subtypeId < imi.getSubtypeCount()) {
                    InputMethodSubtype subtype = imi.getSubtypeAt(subtypeId);
                    this.mSettings.putSelectedSubtype(subtype.hashCode());
                    this.mCurrentSubtype = subtype;
                } else {
                    this.mSettings.putSelectedSubtype(-1);
                    this.mCurrentSubtype = getCurrentInputMethodSubtypeLocked();
                }
                if (!setSubtypeOnly) {
                    this.mSettings.putSelectedInputMethod(imi != null ? imi.getId() : BackupManagerConstants.DEFAULT_BACKUP_FINISHED_NOTIFICATION_RECEIVERS);
                }
                return;
            }
            return;
        }
        Slog.d(TAG, "setSelectedInputMethodAndSubtypeLocked: Skipping SecureIME");
    }

    private void resetSelectedInputMethodAndSubtypeLocked(String newDefaultIme) {
        InputMethodInfo imi = (InputMethodInfo) this.mMethodMap.get(newDefaultIme);
        int lastSubtypeId = -1;
        if (!(imi == null || TextUtils.isEmpty(newDefaultIme))) {
            String subtypeHashCode = this.mSettings.getLastSubtypeForInputMethodLocked(newDefaultIme);
            if (subtypeHashCode != null) {
                try {
                    lastSubtypeId = InputMethodUtils.getSubtypeIdFromHashCode(imi, Integer.parseInt(subtypeHashCode));
                } catch (NumberFormatException e) {
                    String str = TAG;
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("HashCode for subtype looks broken: ");
                    stringBuilder.append(subtypeHashCode);
                    Slog.w(str, stringBuilder.toString(), e);
                }
            }
        }
        setSelectedInputMethodAndSubtypeLocked(imi, lastSubtypeId, false);
    }

    private Pair<InputMethodInfo, InputMethodSubtype> findLastResortApplicableShortcutInputMethodAndSubtypeLocked(String mode) {
        String str = mode;
        InputMethodInfo mostApplicableIMI = null;
        InputMethodSubtype mostApplicableSubtype = null;
        boolean foundInSystemIME = false;
        for (InputMethodInfo imi : this.mSettings.getEnabledInputMethodListLocked()) {
            String imiId = imi.getId();
            if (!foundInSystemIME || imiId.equals(this.mCurMethodId)) {
                ArrayList<InputMethodSubtype> subtypesForSearch;
                InputMethodSubtype subtype = null;
                List<InputMethodSubtype> enabledSubtypes = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi, true);
                if (this.mCurrentSubtype != null) {
                    subtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, enabledSubtypes, str, this.mCurrentSubtype.getLocale(), false);
                }
                if (subtype == null) {
                    subtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, enabledSubtypes, str, null, true);
                }
                ArrayList<InputMethodSubtype> overridingImplicitlyEnabledSubtypes = InputMethodUtils.getOverridingImplicitlyEnabledSubtypes(imi, str);
                if (overridingImplicitlyEnabledSubtypes.isEmpty()) {
                    subtypesForSearch = InputMethodUtils.getSubtypes(imi);
                } else {
                    subtypesForSearch = overridingImplicitlyEnabledSubtypes;
                }
                if (subtype == null && this.mCurrentSubtype != null) {
                    subtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, subtypesForSearch, str, this.mCurrentSubtype.getLocale(), false);
                }
                if (subtype == null) {
                    subtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, subtypesForSearch, str, null, true);
                }
                if (subtype != null) {
                    if (imiId.equals(this.mCurMethodId)) {
                        mostApplicableIMI = imi;
                        mostApplicableSubtype = subtype;
                        break;
                    } else if (!foundInSystemIME) {
                        mostApplicableIMI = imi;
                        mostApplicableSubtype = subtype;
                        if ((imi.getServiceInfo().applicationInfo.flags & 1) != 0) {
                            foundInSystemIME = true;
                        }
                    }
                }
            }
        }
        if (mostApplicableIMI != null) {
            return new Pair(mostApplicableIMI, mostApplicableSubtype);
        }
        return null;
    }

    public InputMethodSubtype getCurrentInputMethodSubtype() {
        if (!calledFromValidUser()) {
            return null;
        }
        InputMethodSubtype currentInputMethodSubtypeLocked;
        synchronized (this.mMethodMap) {
            currentInputMethodSubtypeLocked = getCurrentInputMethodSubtypeLocked();
        }
        return currentInputMethodSubtypeLocked;
    }

    private InputMethodSubtype getCurrentInputMethodSubtypeLocked() {
        if (this.mCurMethodId == null) {
            return null;
        }
        boolean subtypeIsSelected = this.mSettings.isSubtypeSelected();
        InputMethodInfo imi = (InputMethodInfo) this.mMethodMap.get(this.mCurMethodId);
        if (imi == null || imi.getSubtypeCount() == 0) {
            return null;
        }
        if (!(subtypeIsSelected && this.mCurrentSubtype != null && InputMethodUtils.isValidSubtypeId(imi, this.mCurrentSubtype.hashCode()))) {
            int subtypeId = this.mSettings.getSelectedInputMethodSubtypeId(this.mCurMethodId);
            if (subtypeId == -1) {
                List<InputMethodSubtype> explicitlyOrImplicitlyEnabledSubtypes = this.mSettings.getEnabledInputMethodSubtypeListLocked(this.mContext, imi, true);
                if (explicitlyOrImplicitlyEnabledSubtypes.size() == 1) {
                    this.mCurrentSubtype = (InputMethodSubtype) explicitlyOrImplicitlyEnabledSubtypes.get(0);
                } else if (explicitlyOrImplicitlyEnabledSubtypes.size() > 1) {
                    this.mCurrentSubtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, explicitlyOrImplicitlyEnabledSubtypes, "keyboard", null, true);
                    if (this.mCurrentSubtype == null) {
                        this.mCurrentSubtype = InputMethodUtils.findLastResortApplicableSubtypeLocked(this.mRes, explicitlyOrImplicitlyEnabledSubtypes, null, null, true);
                    }
                }
            } else {
                this.mCurrentSubtype = (InputMethodSubtype) InputMethodUtils.getSubtypes(imi).get(subtypeId);
            }
        }
        return this.mCurrentSubtype;
    }

    /* JADX WARNING: Missing block: B:9:0x0024, code skipped:
            return r1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public List getShortcutInputMethodsAndSubtypes() {
        synchronized (this.mMethodMap) {
            ArrayList<Object> ret = new ArrayList();
            if (this.mShortcutInputMethodsAndSubtypes.size() == 0) {
                Pair<InputMethodInfo, InputMethodSubtype> info = findLastResortApplicableShortcutInputMethodAndSubtypeLocked("voice");
                if (info != null) {
                    ret.add(info.first);
                    ret.add(info.second);
                }
            } else {
                for (InputMethodInfo imi : this.mShortcutInputMethodsAndSubtypes.keySet()) {
                    ret.add(imi);
                    Iterator it = ((ArrayList) this.mShortcutInputMethodsAndSubtypes.get(imi)).iterator();
                    while (it.hasNext()) {
                        ret.add((InputMethodSubtype) it.next());
                    }
                }
                return ret;
            }
        }
    }

    /* JADX WARNING: Missing block: B:17:0x0031, code skipped:
            return false;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean setCurrentInputMethodSubtype(InputMethodSubtype subtype) {
        if (!calledFromValidUser()) {
            return false;
        }
        synchronized (this.mMethodMap) {
            if (subtype != null) {
                try {
                    if (this.mCurMethodId != null) {
                        int subtypeId = InputMethodUtils.getSubtypeIdFromHashCode((InputMethodInfo) this.mMethodMap.get(this.mCurMethodId), subtype.hashCode());
                        if (subtypeId != -1) {
                            setInputMethodLocked(this.mCurMethodId, subtypeId);
                            return true;
                        }
                    }
                } catch (Throwable th) {
                }
            }
        }
    }

    private static String imeWindowStatusToString(int imeWindowVis) {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        if ((imeWindowVis & 1) != 0) {
            sb.append("Active");
            first = false;
        }
        if ((imeWindowVis & 2) != 0) {
            if (!first) {
                sb.append("|");
            }
            sb.append("Visible");
        }
        return sb.toString();
    }

    public IInputContentUriToken createInputContentUriToken(IBinder token, Uri contentUri, String packageName) {
        if (!calledFromValidUser()) {
            return null;
        }
        if (token == null) {
            throw new NullPointerException("token");
        } else if (packageName == null) {
            throw new NullPointerException("packageName");
        } else if (contentUri != null) {
            if ("content".equals(contentUri.getScheme())) {
                synchronized (this.mMethodMap) {
                    int uid = Binder.getCallingUid();
                    String str;
                    StringBuilder stringBuilder;
                    if (this.mCurMethodId == null) {
                        return null;
                    } else if (this.mCurToken != token) {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Ignoring createInputContentUriToken mCurToken=");
                        stringBuilder.append(this.mCurToken);
                        stringBuilder.append(" token=");
                        stringBuilder.append(token);
                        Slog.e(str, stringBuilder.toString());
                        return null;
                    } else if (TextUtils.equals(this.mCurAttribute.packageName, packageName)) {
                        int imeUserId = UserHandle.getUserId(uid);
                        int appUserId = UserHandle.getUserId(this.mCurClient.uid);
                        InputContentUriTokenHandler inputContentUriTokenHandler = new InputContentUriTokenHandler(ContentProvider.getUriWithoutUserId(contentUri), uid, packageName, ContentProvider.getUserIdFromUri(contentUri, imeUserId), appUserId);
                        return inputContentUriTokenHandler;
                    } else {
                        str = TAG;
                        stringBuilder = new StringBuilder();
                        stringBuilder.append("Ignoring createInputContentUriToken mCurAttribute.packageName=");
                        stringBuilder.append(this.mCurAttribute.packageName);
                        stringBuilder.append(" packageName=");
                        stringBuilder.append(packageName);
                        Slog.e(str, stringBuilder.toString());
                        return null;
                    }
                }
            }
            throw new InvalidParameterException("contentUri must have content scheme");
        } else {
            throw new NullPointerException("contentUri");
        }
    }

    /* JADX WARNING: Missing block: B:16:0x0030, code skipped:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void reportFullscreenMode(IBinder token, boolean fullscreen) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                if (!calledWithValidToken(token)) {
                } else if (!(this.mCurClient == null || this.mCurClient.client == null)) {
                    this.mInFullscreenMode = fullscreen;
                    executeOrSendMessage(this.mCurClient.client, this.mCaller.obtainMessageIO(MSG_REPORT_FULLSCREEN_MODE, fullscreen, this.mCurClient));
                }
            }
        }
    }

    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        StringBuilder stringBuilder;
        if (DumpUtils.checkDumpPermission(this.mContext, TAG, pw)) {
            ClientState ci;
            ClientState client;
            IInputMethod method;
            Printer p = new PrintWriterPrinter(pw);
            synchronized (this.mMethodMap) {
                StringBuilder stringBuilder2;
                p.println("Current Input Method Manager state:");
                int N = this.mMethodList.size();
                StringBuilder stringBuilder3 = new StringBuilder();
                stringBuilder3.append("  Input Methods: mMethodMapUpdateCount=");
                stringBuilder3.append(this.mMethodMapUpdateCount);
                stringBuilder3.append(" mBindInstantServiceAllowed=");
                stringBuilder3.append(this.mBindInstantServiceAllowed);
                p.println(stringBuilder3.toString());
                for (int i = 0; i < N; i++) {
                    InputMethodInfo info = (InputMethodInfo) this.mMethodList.get(i);
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  InputMethod #");
                    stringBuilder2.append(i);
                    stringBuilder2.append(":");
                    p.println(stringBuilder2.toString());
                    info.dump(p, "    ");
                }
                p.println("  Clients:");
                for (ClientState ci2 : this.mClients.values()) {
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("  Client ");
                    stringBuilder2.append(ci2);
                    stringBuilder2.append(":");
                    p.println(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("    client=");
                    stringBuilder2.append(ci2.client);
                    p.println(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("    inputContext=");
                    stringBuilder2.append(ci2.inputContext);
                    p.println(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("    sessionRequested=");
                    stringBuilder2.append(ci2.sessionRequested);
                    p.println(stringBuilder2.toString());
                    stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("    curSession=");
                    stringBuilder2.append(ci2.curSession);
                    p.println(stringBuilder2.toString());
                }
                stringBuilder3 = new StringBuilder();
                stringBuilder3.append("  mCurMethodId=");
                stringBuilder3.append(this.mCurMethodId);
                p.println(stringBuilder3.toString());
                client = this.mCurClient;
                StringBuilder stringBuilder4 = new StringBuilder();
                stringBuilder4.append("  mCurClient=");
                stringBuilder4.append(client);
                stringBuilder4.append(" mCurSeq=");
                stringBuilder4.append(this.mCurSeq);
                p.println(stringBuilder4.toString());
                stringBuilder4 = new StringBuilder();
                stringBuilder4.append("  mCurFocusedWindow=");
                stringBuilder4.append(this.mCurFocusedWindow);
                stringBuilder4.append(" softInputMode=");
                stringBuilder4.append(InputMethodClient.softInputModeToString(this.mCurFocusedWindowSoftInputMode));
                stringBuilder4.append(" client=");
                stringBuilder4.append(this.mCurFocusedWindowClient);
                p.println(stringBuilder4.toString());
                ci2 = this.mCurFocusedWindowClient;
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  mCurId=");
                stringBuilder2.append(this.mCurId);
                stringBuilder2.append(" mHaveConnect=");
                stringBuilder2.append(this.mHaveConnection);
                stringBuilder2.append(" mBoundToMethod=");
                stringBuilder2.append(this.mBoundToMethod);
                p.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  mCurToken=");
                stringBuilder2.append(this.mCurToken);
                p.println(stringBuilder2.toString());
                stringBuilder2 = new StringBuilder();
                stringBuilder2.append("  mCurIntent=");
                stringBuilder2.append(this.mCurIntent);
                p.println(stringBuilder2.toString());
                method = this.mCurMethod;
                StringBuilder stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  mCurMethod=");
                stringBuilder5.append(this.mCurMethod);
                p.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  mEnabledSession=");
                stringBuilder5.append(this.mEnabledSession);
                p.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  mImeWindowVis=");
                stringBuilder5.append(imeWindowStatusToString(this.mImeWindowVis));
                p.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  mShowRequested=");
                stringBuilder5.append(this.mShowRequested);
                stringBuilder5.append(" mShowExplicitlyRequested=");
                stringBuilder5.append(this.mShowExplicitlyRequested);
                stringBuilder5.append(" mShowForced=");
                stringBuilder5.append(this.mShowForced);
                stringBuilder5.append(" mInputShown=");
                stringBuilder5.append(this.mInputShown);
                p.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  mInFullscreenMode=");
                stringBuilder5.append(this.mInFullscreenMode);
                p.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  mCurUserActionNotificationSequenceNumber=");
                stringBuilder5.append(this.mCurUserActionNotificationSequenceNumber);
                p.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  mSystemReady=");
                stringBuilder5.append(this.mSystemReady);
                stringBuilder5.append(" mInteractive=");
                stringBuilder5.append(this.mIsInteractive);
                p.println(stringBuilder5.toString());
                stringBuilder5 = new StringBuilder();
                stringBuilder5.append("  mSettingsObserver=");
                stringBuilder5.append(this.mSettingsObserver);
                p.println(stringBuilder5.toString());
                p.println("  mSwitchingController:");
                this.mSwitchingController.dump(p);
                p.println("  mSettings:");
                this.mSettings.dumpLocked(p, "    ");
                p.println("  mStartInputHistory:");
                this.mStartInputHistory.dump(pw, "   ");
            }
            p.println(" ");
            if (client != null) {
                pw.flush();
                try {
                    TransferPipe.dumpAsync(client.client.asBinder(), fd, args);
                } catch (RemoteException | IOException e) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to dump input method client: ");
                    stringBuilder.append(e);
                    p.println(stringBuilder.toString());
                }
            } else {
                p.println("No input method client.");
            }
            if (!(ci2 == null || client == ci2)) {
                p.println(" ");
                p.println("Warning: Current input method client doesn't match the last focused. window.");
                p.println("Dumping input method client in the last focused window just in case.");
                p.println(" ");
                pw.flush();
                try {
                    TransferPipe.dumpAsync(ci2.client.asBinder(), fd, args);
                } catch (RemoteException | IOException e2) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to dump input method client in focused window: ");
                    stringBuilder.append(e2);
                    p.println(stringBuilder.toString());
                }
            }
            p.println(" ");
            if (method != null) {
                pw.flush();
                try {
                    TransferPipe.dumpAsync(method.asBinder(), fd, args);
                } catch (RemoteException | IOException e22) {
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("Failed to dump input method service: ");
                    stringBuilder.append(e22);
                    p.println(stringBuilder.toString());
                }
            } else {
                p.println("No input method service.");
            }
        }
    }

    public void onShellCommand(FileDescriptor in, FileDescriptor out, FileDescriptor err, String[] args, ShellCallback callback, ResultReceiver resultReceiver) throws RemoteException {
        new ShellCommandImpl(this).exec(this, in, out, err, args, callback, resultReceiver);
    }

    private int handleSetBindInstantServiceAllowed(ShellCommand shellCommand) {
        String allowedString = shellCommand.getNextArgRequired();
        if (allowedString == null) {
            shellCommand.getErrPrintWriter().println("Error: no true/false specified");
            return -1;
        }
        boolean allowed = Boolean.parseBoolean(allowedString);
        synchronized (this.mMethodMap) {
            if (this.mContext.checkCallingOrSelfPermission("android.permission.MANAGE_BIND_INSTANT_SERVICE") != 0) {
                shellCommand.getErrPrintWriter().print("Caller must have MANAGE_BIND_INSTANT_SERVICE permission");
                return -1;
            } else if (this.mBindInstantServiceAllowed == allowed) {
                return 0;
            } else {
                this.mBindInstantServiceAllowed = allowed;
                long ident = Binder.clearCallingIdentity();
                try {
                    resetSelectedInputMethodAndSubtypeLocked(null);
                    this.mSettings.putSelectedInputMethod(null);
                    buildInputMethodListLocked(false);
                    updateInputMethodsFromSettingsLocked(true);
                    return 0;
                } finally {
                    Binder.restoreCallingIdentity(ident);
                }
            }
        }
    }

    private int handleShellCommandListInputMethods(ShellCommand shellCommand) {
        boolean all = false;
        boolean brief = false;
        while (true) {
            String nextOption = shellCommand.getNextOption();
            if (nextOption != null) {
                int i = -1;
                int hashCode = nextOption.hashCode();
                if (hashCode != 1492) {
                    if (hashCode == 1510 && nextOption.equals("-s")) {
                        i = 1;
                    }
                } else if (nextOption.equals("-a")) {
                    i = 0;
                }
                switch (i) {
                    case 0:
                        all = true;
                        break;
                    case 1:
                        brief = true;
                        break;
                    default:
                        break;
                }
            }
            nextOption = all ? getInputMethodList() : getEnabledInputMethodList();
            PrintWriter pr = shellCommand.getOutPrintWriter();
            Printer printer = new -$$Lambda$InputMethodManagerService$87vt08aKi27xQgvHZ-wOyJeb5jo(pr);
            int N = nextOption.size();
            for (int i2 = 0; i2 < N; i2++) {
                if (brief) {
                    pr.println(((InputMethodInfo) nextOption.get(i2)).getId());
                } else {
                    pr.print(((InputMethodInfo) nextOption.get(i2)).getId());
                    pr.println(":");
                    ((InputMethodInfo) nextOption.get(i2)).dump(printer, "  ");
                }
            }
            return 0;
        }
    }

    private int handleShellCommandEnableDisableInputMethod(ShellCommand shellCommand, boolean enabled) {
        if (calledFromValidUser()) {
            boolean previouslyEnabled;
            String id = shellCommand.getNextArgRequired();
            synchronized (this.mMethodMap) {
                if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        previouslyEnabled = setInputMethodEnabledLocked(id, enabled);
                    } finally {
                        Binder.restoreCallingIdentity(ident);
                    }
                } else {
                    shellCommand.getErrPrintWriter().print("Caller must have WRITE_SECURE_SETTINGS permission");
                    throw new SecurityException("Requires permission android.permission.WRITE_SECURE_SETTINGS");
                }
            }
            PrintWriter pr = shellCommand.getOutPrintWriter();
            pr.print("Input method ");
            pr.print(id);
            pr.print(": ");
            pr.print(enabled == previouslyEnabled ? "already " : "now ");
            pr.println(enabled ? "enabled" : "disabled");
            return 0;
        }
        shellCommand.getErrPrintWriter().print("Must be called from the foreground user or with INTERACT_ACROSS_USERS_FULL");
        return -1;
    }

    private int handleShellCommandSetInputMethod(ShellCommand shellCommand) {
        String id = shellCommand.getNextArgRequired();
        setInputMethod(null, id);
        PrintWriter pr = shellCommand.getOutPrintWriter();
        pr.print("Input method ");
        pr.print(id);
        pr.println("  selected");
        return 0;
    }

    private int handleShellCommandResetInputMethod(ShellCommand shellCommand) {
        if (calledFromValidUser()) {
            synchronized (this.mMethodMap) {
                if (this.mContext.checkCallingOrSelfPermission("android.permission.WRITE_SECURE_SETTINGS") == 0) {
                    long ident = Binder.clearCallingIdentity();
                    try {
                        int i;
                        String nextIme;
                        List<InputMethodInfo> nextEnabledImes;
                        synchronized (this.mMethodMap) {
                            hideCurrentInputLocked(0, null);
                            unbindCurrentMethodLocked(false);
                            resetSelectedInputMethodAndSubtypeLocked(null);
                            this.mSettings.putSelectedInputMethod(null);
                            ArrayList<InputMethodInfo> enabledImes = this.mSettings.getEnabledInputMethodListLocked();
                            int N = enabledImes.size();
                            for (i = 0; i < N; i++) {
                                setInputMethodEnabledLocked(((InputMethodInfo) enabledImes.get(i)).getId(), false);
                            }
                            enabledImes = InputMethodUtils.getDefaultEnabledImes(this.mContext, this.mMethodList);
                            N = enabledImes.size();
                            for (i = 0; i < N; i++) {
                                setInputMethodEnabledLocked(((InputMethodInfo) enabledImes.get(i)).getId(), true);
                            }
                            updateInputMethodsFromSettingsLocked(true);
                            InputMethodUtils.setNonSelectedSystemImesDisabledUntilUsed(this.mIPackageManager, this.mSettings.getEnabledInputMethodListLocked(), this.mSettings.getCurrentUserId(), this.mContext.getBasePackageName());
                            nextIme = this.mSettings.getSelectedInputMethod();
                            nextEnabledImes = getEnabledInputMethodList();
                        }
                        Binder.restoreCallingIdentity(ident);
                        PrintWriter pr = shellCommand.getOutPrintWriter();
                        pr.println("Reset current and enabled IMEs");
                        pr.println("Newly selected IME:");
                        pr.print("  ");
                        pr.println(nextIme);
                        pr.println("Newly enabled IMEs:");
                        i = nextEnabledImes.size();
                        for (int i2 = 0; i2 < i; i2++) {
                            pr.print("  ");
                            pr.println(((InputMethodInfo) nextEnabledImes.get(i2)).getId());
                        }
                    } catch (Throwable th) {
                        Binder.restoreCallingIdentity(ident);
                    }
                } else {
                    shellCommand.getErrPrintWriter().print("Caller must have WRITE_SECURE_SETTINGS permission");
                    throw new SecurityException("Requires permission android.permission.WRITE_SECURE_SETTINGS");
                }
            }
            return 0;
        }
        shellCommand.getErrPrintWriter().print("Must be called from the foreground user or with INTERACT_ACROSS_USERS_FULL");
        return -1;
    }

    private void setDefaultImeEnable(String pkgImeName) {
        try {
            PackageManager pm = this.mContext.getPackageManager();
            if (!(pm.getApplicationEnabledSetting(pkgImeName) == 1)) {
                Slog.i(TAG, "current default input disable,enable it");
                pm.setApplicationEnabledSetting(pkgImeName, 1, 0);
            }
        } catch (Exception e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unexpected exception");
            stringBuilder.append(e.getMessage());
            Slog.w(str, stringBuilder.toString());
        }
    }

    public IBinder getHwInnerService() {
        return this.mHwInnerService;
    }

    public void setInputMethodLockedByInner(String imeId) {
        int uid = Binder.getCallingUid();
        if (uid == 1000 || uid == 0) {
            synchronized (this.mMethodMap) {
                if (this.mSettings.getIsWriteInputEnable()) {
                    if (TextUtils.isEmpty(imeId)) {
                        if (TextUtils.isEmpty(this.mCurInputId)) {
                            this.mCurInputId = this.mSettings.getSelectedInputMethod();
                        }
                        setInputMethodLocked(this.mCurInputId, this.mSettings.getSelectedInputMethodSubtypeId(this.mCurInputId));
                    } else {
                        setInputMethodLocked(imeId, this.mSettings.getSelectedInputMethodSubtypeId(imeId));
                    }
                }
            }
            return;
        }
        throw new SecurityException("has no permssion to use");
    }

    public void setInputSource(boolean isFingerTouch) {
        int uid = Binder.getCallingUid();
        if (uid != 1000 && uid != 0) {
            throw new SecurityException("has no permssion to use");
        } else if (this.mCurMethod == null) {
        } else {
            if (isFingerTouch) {
                Secure.putString(this.mContext.getContentResolver(), "input_source", "0");
            } else {
                Secure.putString(this.mContext.getContentResolver(), "input_source", "1");
            }
        }
    }
}
