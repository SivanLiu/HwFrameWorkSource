package com.android.server;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources.NotFoundException;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.provider.Settings.Secure;
import android.util.Slog;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodSubtype;
import com.android.internal.view.IInputContext;
import com.android.internal.view.IInputMethodClient;
import com.android.internal.view.IInputMethodManager;
import com.android.internal.view.IInputMethodManager.Stub;
import com.android.internal.view.InputBindResult;
import com.android.server.statusbar.StatusBarManagerService;
import com.android.server.wifipro.WifiProCHRManager;
import com.android.server.wifipro.WifiProCommonUtils;
import huawei.android.view.inputmethod.HwSecImmHelper;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;

public class HwInputMethodManagerService extends InputMethodManagerService {
    public static final String ACTION_SECURE_IME = "com.huawei.secime.SoftKeyboard";
    private static final Locale CHINA_LOCALE = new Locale("zh");
    private static final String DESCRIPTOR = "android.view.inputmethod.InputMethodManager";
    private static final Locale ENGLISH_LOCALE = new Locale("en");
    public static final int FLAG_SHOW_INPUT = 65536;
    private static final String INPUT_METHOD_ENABLED_FILE = "bflag";
    private static final boolean IS_CHINA_AREA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    public static final int SECURE_IME_NO_HIDE_FLAG = 4096;
    public static final String SECURE_IME_PACKAGENAME = "com.huawei.secime";
    public static final String SECURITY_INPUT_METHOD_ID = "com.huawei.secime/.SoftKeyboard";
    public static final String SECURITY_INPUT_SERVICE_NAME = "input_method_secure";
    public static final String SETTINGS_SECURE_KEYBOARD_CONTROL = "secure_keyboard";
    static final String TAG = "HwInputMethodManagerService";
    private static final int TRANSACTION_isUseSecureIME = 1001;
    private static final int UNBIND_SECIME_IF_SHOULD = 10000;
    private static final boolean isSupportedSecIme = IS_CHINA_AREA;
    private boolean isSecMethodUsing = false;
    private boolean isSecureIMEEnabled = false;
    private boolean isSecureIMEExist = false;
    private SecureSettingsObserver mSecureSettingsObserver;
    private IInputMethodManager mSecurityInputMethodService;

    private class SecureSettingsObserver extends ContentObserver {
        boolean mRegistered = false;
        int mUserId;

        public SecureSettingsObserver() {
            super(new Handler());
        }

        public void registerContentObserverInner(int userId) {
            String str = HwInputMethodManagerService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("SecureSettingsObserver mRegistered=");
            stringBuilder.append(this.mRegistered);
            stringBuilder.append(" new user=");
            stringBuilder.append(userId);
            stringBuilder.append(" current user=");
            stringBuilder.append(this.mUserId);
            Slog.d(str, stringBuilder.toString());
            if (!this.mRegistered || this.mUserId != userId) {
                ContentResolver resolver = HwInputMethodManagerService.this.mContext.getContentResolver();
                if (this.mRegistered) {
                    resolver.unregisterContentObserver(this);
                    this.mRegistered = false;
                }
                this.mUserId = userId;
                resolver.registerContentObserver(Secure.getUriFor("secure_keyboard"), false, this, userId);
                this.mRegistered = true;
            }
        }

        public void onChange(boolean selfChange, Uri uri) {
            if (uri != null) {
                String str = HwInputMethodManagerService.TAG;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("SecureSettingsObserver onChange, uri = ");
                stringBuilder.append(uri.toString());
                Slog.i(str, stringBuilder.toString());
                if (Secure.getUriFor("secure_keyboard").equals(uri)) {
                    HwInputMethodManagerService.this.isSecureIMEEnabled = HwInputMethodManagerService.this.isSecureIMEEnable();
                    String str2 = HwInputMethodManagerService.TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("isSecureIMEEnabled = ");
                    stringBuilder2.append(HwInputMethodManagerService.this.isSecureIMEEnabled);
                    Slog.i(str2, stringBuilder2.toString());
                }
            }
        }
    }

    public HwInputMethodManagerService(Context context) {
        super(context);
    }

    protected void createFlagIfNecessary(int newUserId) {
        File flagFile = new File(Environment.getUserSystemDirectory(newUserId), INPUT_METHOD_ENABLED_FILE);
        boolean existFlag = false;
        try {
            if (flagFile.exists()) {
                existFlag = true;
            } else {
                flagFile.createNewFile();
            }
            synchronized (this.mEnabledFileMap) {
                this.mEnabledFileMap.put(String.valueOf(newUserId), Boolean.valueOf(existFlag));
            }
        } catch (IOException e) {
            Slog.e(TAG, "Unable to create flag file!");
        }
    }

    protected boolean isFlagExists(int userId) {
        boolean value;
        synchronized (this.mEnabledFileMap) {
            value = ((Boolean) this.mEnabledFileMap.get(String.valueOf(userId))).booleanValue();
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isFlagExists  value = ");
        stringBuilder.append(value);
        stringBuilder.append(", userId = ");
        stringBuilder.append(userId);
        Slog.i(str, stringBuilder.toString());
        return value;
    }

    protected void ensureEnableSystemIME(String id, InputMethodInfo p, Context context, int userId) {
        if (!isFlagExists(userId)) {
            if (isValidSystemDefaultIme(p, context) || isSystemImeThatHasEnglishSubtype(p) || isSystemImeThatHasChinaSubtype(p)) {
                Slog.i(TAG, "ensureEnableSystemIME will setInputMethodEnabledLocked");
                setInputMethodEnabledLocked(id, true);
            }
        }
    }

    private static boolean isSystemIme(InputMethodInfo inputMethod) {
        return (inputMethod.getServiceInfo().applicationInfo.flags & 1) != 0;
    }

    private boolean isValidSystemDefaultIme(InputMethodInfo imi, Context context) {
        if (!this.mSystemReady || !isSystemIme(imi)) {
            return false;
        }
        if (imi.getIsDefaultResourceId() != 0) {
            try {
                if (context.createPackageContext(imi.getPackageName(), 0).getResources().getBoolean(imi.getIsDefaultResourceId()) && containsSubtypeOf(imi, context.getResources().getConfiguration().locale.getLanguage())) {
                    return true;
                }
            } catch (NameNotFoundException | NotFoundException e) {
            }
        }
        if (imi.getSubtypeCount() == 0) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Found no subtypes in a system IME: ");
            stringBuilder.append(imi.getPackageName());
            Slog.w(str, stringBuilder.toString());
        }
        return false;
    }

    private static boolean isSystemImeThatHasEnglishSubtype(InputMethodInfo imi) {
        if (isSystemIme(imi)) {
            return containsSubtypeOf(imi, ENGLISH_LOCALE.getLanguage());
        }
        return false;
    }

    private static boolean isSystemImeThatHasChinaSubtype(InputMethodInfo imi) {
        if (isSystemIme(imi)) {
            return containsSubtypeOf(imi, CHINA_LOCALE.getLanguage());
        }
        return false;
    }

    private static boolean containsSubtypeOf(InputMethodInfo imi, String language) {
        int N = imi.getSubtypeCount();
        for (int i = 0; i < N; i++) {
            if (imi.getSubtypeAt(i).getLocale().startsWith(language)) {
                return true;
            }
        }
        return false;
    }

    public void systemRunning(StatusBarManagerService statusBar) {
        if (isSupportedSecIme) {
            this.mSecureSettingsObserver = new SecureSettingsObserver();
            this.mSecureSettingsObserver.registerContentObserverInner(this.mSettings.getCurrentUserId());
            this.isSecureIMEEnabled = isSecureIMEEnable();
            this.isSecureIMEExist = existSecureIME();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("systemRunning isSecureIMEEnabled = ");
            stringBuilder.append(this.isSecureIMEEnabled);
            stringBuilder.append(", isSecureIMEExist = ");
            stringBuilder.append(this.isSecureIMEExist);
            Slog.i(str, stringBuilder.toString());
        }
        super.systemRunning(statusBar);
    }

    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code != 1001) {
            return super.onTransact(code, data, reply, flags);
        }
        data.enforceInterface(DESCRIPTOR);
        boolean result = isUseSecureIME();
        reply.writeNoException();
        reply.writeInt(result);
        return true;
    }

    private IInputMethodManager getSecurityInputMethodService() {
        if (this.mSecurityInputMethodService == null) {
            this.mSecurityInputMethodService = Stub.asInterface(ServiceManager.getService(SECURITY_INPUT_SERVICE_NAME));
        }
        return this.mSecurityInputMethodService;
    }

    private boolean isSecureIMEEnable() {
        return Secure.getIntForUser(this.mContext.getContentResolver(), "secure_keyboard", 1, this.mSettings.getCurrentUserId()) == 1;
    }

    private boolean isUseSecureIME() {
        return this.isSecureIMEExist && this.isSecureIMEEnabled && isSupportedSecIme && getSecurityInputMethodService() != null;
    }

    public void addClient(IInputMethodClient client, IInputContext inputContext, int uid, int pid) {
        super.addClient(client, inputContext, uid, pid);
        if (getSecurityInputMethodService() != null) {
            try {
                this.mSecurityInputMethodService.addClient(client, inputContext, uid, pid);
            } catch (RemoteException e) {
            }
        }
    }

    public void removeClient(IInputMethodClient client) {
        super.removeClient(client);
        if (getSecurityInputMethodService() != null) {
            try {
                this.mSecurityInputMethodService.removeClient(client);
            } catch (RemoteException e) {
            }
        }
    }

    public void setImeWindowStatus(IBinder token, IBinder startInputToken, int vis, int backDisposition) {
        if (!isUseSecureIME() || (token != null && this.mCurToken == token)) {
            super.setImeWindowStatus(token, startInputToken, vis, backDisposition);
            return;
        }
        try {
            this.mSecurityInputMethodService.setImeWindowStatus(token, startInputToken, vis, backDisposition);
        } catch (RemoteException e) {
            Slog.e(TAG, "setImeWindowStatus, remote exception");
        }
    }

    public InputBindResult startInputOrWindowGainedFocus(int startInputReason, IInputMethodClient client, IBinder windowToken, int controlFlags, int softInputMode, int windowFlags, EditorInfo attribute, IInputContext inputContext, int missingMethods, int unverifiedTargetSdkVersion) {
        int controlFlags2;
        IInputMethodClient iInputMethodClient = client;
        EditorInfo editorInfo = attribute;
        boolean z;
        if (windowToken == null) {
            z = false;
            ResultReceiver resultReceiver = null;
            Slog.d(TAG, "------------startInput--------------");
            if (isUseSecureIME()) {
                if (isPasswordType(editorInfo)) {
                    Slog.d(TAG, "isPasswordType(attribute) == true, using SecurityIMMS");
                    if (this.mInputShown) {
                        z = super.hideSoftInput(iInputMethodClient, z, resultReceiver);
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("hideNormalSoftInput = ");
                        stringBuilder.append(z);
                        Slog.d(str, stringBuilder.toString());
                        if (z) {
                            controlFlags2 = controlFlags | 65536;
                            this.isSecMethodUsing = true;
                            return this.mSecurityInputMethodService.startInputOrWindowGainedFocus(startInputReason, iInputMethodClient, windowToken, controlFlags2, softInputMode, windowFlags, editorInfo, inputContext, missingMethods, unverifiedTargetSdkVersion);
                        }
                    }
                    controlFlags2 = controlFlags;
                    this.isSecMethodUsing = true;
                    try {
                        return this.mSecurityInputMethodService.startInputOrWindowGainedFocus(startInputReason, iInputMethodClient, windowToken, controlFlags2, softInputMode, windowFlags, editorInfo, inputContext, missingMethods, unverifiedTargetSdkVersion);
                    } catch (RemoteException e) {
                    }
                } else {
                    boolean isHide = z;
                    try {
                        isHide = this.mSecurityInputMethodService.hideSoftInput(iInputMethodClient, z, resultReceiver);
                    } catch (RemoteException e2) {
                    }
                    String str2 = TAG;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("hideSecSoftInput = ");
                    stringBuilder2.append(isHide);
                    Slog.d(str2, stringBuilder2.toString());
                    if (isHide) {
                        controlFlags2 = controlFlags | 65536;
                    } else {
                        controlFlags2 = controlFlags;
                    }
                    this.isSecMethodUsing = z;
                    return super.startInputOrWindowGainedFocus(startInputReason, iInputMethodClient, windowToken, controlFlags2, softInputMode, windowFlags, editorInfo, inputContext, missingMethods, unverifiedTargetSdkVersion);
                }
            }
        } else if (isUseSecureIME()) {
            ResultReceiver resultReceiver2;
            if (isPasswordType(editorInfo)) {
                Slog.d(TAG, "windowGainedFocus  isPasswordType(attribute) = true");
                this.isSecMethodUsing = true;
                resultReceiver2 = null;
                super.startInputOrWindowGainedFocus(startInputReason, iInputMethodClient, windowToken, controlFlags, 1, windowFlags, editorInfo, inputContext, missingMethods, unverifiedTargetSdkVersion);
                super.hideSoftInput(iInputMethodClient, 0, resultReceiver2);
                try {
                    return this.mSecurityInputMethodService.startInputOrWindowGainedFocus(startInputReason, iInputMethodClient, windowToken, controlFlags, softInputMode, windowFlags, editorInfo, inputContext, missingMethods, unverifiedTargetSdkVersion);
                } catch (RemoteException e3) {
                }
            } else {
                z = false;
                resultReceiver2 = null;
                this.isSecMethodUsing = z;
                try {
                    this.mSecurityInputMethodService.hideSoftInput(iInputMethodClient, z, resultReceiver2);
                    InputBindResult inputBindRes = super.startInputOrWindowGainedFocus(startInputReason, client, windowToken, controlFlags, softInputMode, windowFlags, attribute, inputContext, missingMethods, unverifiedTargetSdkVersion);
                    this.mSecurityInputMethodService.startInputOrWindowGainedFocus(10000, iInputMethodClient, windowToken, controlFlags, softInputMode, windowFlags, editorInfo, inputContext, missingMethods, unverifiedTargetSdkVersion);
                    return inputBindRes;
                } catch (RemoteException e4) {
                }
            }
        }
        controlFlags2 = controlFlags;
        return super.startInputOrWindowGainedFocus(startInputReason, iInputMethodClient, windowToken, controlFlags2, softInputMode, windowFlags, editorInfo, inputContext, missingMethods, unverifiedTargetSdkVersion);
    }

    public void setInputMethod(IBinder token, String id) {
        if (!SECURITY_INPUT_METHOD_ID.equals(id)) {
            super.setInputMethod(token, id);
        }
    }

    public void setInputMethodAndSubtype(IBinder token, String id, InputMethodSubtype subtype) {
        if (!isSupportedSecIme || !SECURITY_INPUT_METHOD_ID.equals(id)) {
            super.setInputMethodAndSubtype(token, id, subtype);
        }
    }

    boolean setInputMethodEnabledLocked(String id, boolean enabled) {
        if (isSupportedSecIme && SECURITY_INPUT_METHOD_ID.equals(id)) {
            return false;
        }
        return super.setInputMethodEnabledLocked(id, enabled);
    }

    void setInputMethodLocked(String id, int subtypeId) {
        if (!isSupportedSecIme || !SECURITY_INPUT_METHOD_ID.equals(id)) {
            super.setInputMethodLocked(id, subtypeId);
        }
    }

    public void showInputMethodPickerFromClient(IInputMethodClient client, int auxiliarySubtypeMode) {
        if (isUseSecureIME() && this.isSecMethodUsing) {
            Slog.w(TAG, "SecIME is using, don't show the input method chooser dialog!");
        } else {
            super.showInputMethodPickerFromClient(client, auxiliarySubtypeMode);
        }
    }

    public void hideMySoftInput(IBinder token, int flags) {
        if (!isUseSecureIME() || (token != null && this.mCurToken == token)) {
            super.hideMySoftInput(token, flags);
        } else {
            try {
                this.mSecurityInputMethodService.hideMySoftInput(token, flags);
            } catch (RemoteException e) {
            }
        }
    }

    public boolean showSoftInput(IInputMethodClient client, int flags, ResultReceiver resultReceiver) {
        if (isUseSecureIME() && this.isSecMethodUsing) {
            try {
                hideSoftInput(client, 4096, resultReceiver);
                return this.mSecurityInputMethodService.showSoftInput(client, flags, resultReceiver);
            } catch (RemoteException e) {
            }
        }
        return super.showSoftInput(client, flags, resultReceiver);
    }

    public void showMySoftInput(IBinder token, int flags) {
        if (!isUseSecureIME() || (token != null && this.mCurToken == token)) {
            super.showMySoftInput(token, flags);
        } else {
            try {
                this.mSecurityInputMethodService.showMySoftInput(token, flags);
            } catch (RemoteException e) {
            }
        }
    }

    private boolean isPasswordType(EditorInfo attribute) {
        if (attribute != null) {
            return HwSecImmHelper.isPasswordInputType(attribute.inputType, true);
        }
        if (this.isSecMethodUsing) {
            return true;
        }
        return false;
    }

    protected boolean isSecureIME(String packageName) {
        if (SECURE_IME_PACKAGENAME.equals(packageName)) {
            return true;
        }
        return false;
    }

    protected boolean shouldBuildInputMethodList(String packageName) {
        if (isSupportedSecIme && SECURE_IME_PACKAGENAME.equals(packageName)) {
            return false;
        }
        return true;
    }

    protected void updateSecureIMEStatus() {
        this.isSecureIMEExist = existSecureIME();
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSecureIMEExist = ");
        stringBuilder.append(this.isSecureIMEExist);
        Slog.i(str, stringBuilder.toString());
    }

    private boolean existSecureIME() {
        List<ResolveInfo> packages = this.mContext.getPackageManager().queryIntentServicesAsUser(new Intent(ACTION_SECURE_IME), 0, this.mSettings.getCurrentUserId());
        if (packages == null || packages.size() <= 0) {
            return false;
        }
        return true;
    }

    public boolean hideSoftInput(IInputMethodClient client, int flags, ResultReceiver resultReceiver) {
        if (isUseSecureIME() && this.isSecMethodUsing) {
            if (4096 != flags) {
                try {
                    this.mSecurityInputMethodService.hideSoftInput(client, flags, resultReceiver);
                } catch (RemoteException e) {
                }
            } else {
                flags = 0;
            }
        }
        return super.hideSoftInput(client, flags, resultReceiver);
    }

    protected void switchUserExtra(int userId) {
        if (isSupportedSecIme) {
            if (this.mSecureSettingsObserver != null) {
                this.mSecureSettingsObserver.registerContentObserverInner(userId);
            }
            updateSecureIMEStatus();
            this.isSecureIMEEnabled = isSecureIMEEnable();
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("isSecureIMEEnabled = ");
            stringBuilder.append(this.isSecureIMEEnabled);
            Slog.i(str, stringBuilder.toString());
            if (isUseSecureIME()) {
                HwSecureInputMethodManagerInternal mLocalServices = (HwSecureInputMethodManagerInternal) LocalServices.getService(HwSecureInputMethodManagerInternal.class);
                if (mLocalServices != null) {
                    mLocalServices.setClientActiveFlag();
                } else {
                    Slog.w(TAG, "HwSecureInputMethodManagerInternal is not exist !");
                }
            }
        }
    }

    protected int getNaviBarEnabledDefValue() {
        int defValue;
        boolean FRONT_FINGERPRINT_NAVIGATION = SystemProperties.getBoolean("ro.config.hw_front_fp_navi", false);
        int FRONT_FINGERPRINT_NAVIGATION_TRIKEY = SystemProperties.getInt("ro.config.hw_front_fp_trikey", 0);
        if (FRONT_FINGERPRINT_NAVIGATION) {
            boolean isTrikeyExist = isTrikeyExist();
            if (FRONT_FINGERPRINT_NAVIGATION_TRIKEY == 1 && isTrikeyExist) {
                defValue = 0;
            } else if (SystemProperties.get("ro.config.hw_optb", "0").equals("156")) {
                defValue = 0;
            } else {
                defValue = 1;
            }
        } else {
            defValue = 1;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("NaviBar defValue = ");
        stringBuilder.append(defValue);
        Slog.i(str, stringBuilder.toString());
        return defValue;
    }

    private boolean isTrikeyExist() {
        String str;
        StringBuilder stringBuilder;
        try {
            Class clazz = Class.forName("huawei.android.os.HwGeneralManager");
            return ((Boolean) clazz.getDeclaredMethod("isSupportTrikey", null).invoke(clazz.getDeclaredMethod(WifiProCHRManager.LOG_GET_INSTANCE_API_NAME, null).invoke(clazz, (Object[]) null), (Object[]) null)).booleanValue();
        } catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | NoSuchMethodException | NullPointerException | InvocationTargetException e) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("isTrikeyExist, reflect method handle, and has exception: ");
            stringBuilder.append(e);
            Slog.e(str, stringBuilder.toString());
            return false;
        } catch (Exception e2) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append("isTrikeyExist, other exception: ");
            stringBuilder.append(e2);
            Slog.e(str, stringBuilder.toString());
            return false;
        }
    }

    public void reportFullscreenMode(IBinder token, boolean fullscreen) {
        if (!isUseSecureIME() || (token != null && this.mCurToken == token)) {
            super.reportFullscreenMode(token, fullscreen);
            return;
        }
        try {
            this.mSecurityInputMethodService.reportFullscreenMode(token, fullscreen);
        } catch (RemoteException e) {
            Slog.e(TAG, "reportFullscreenMode, remote exception");
        }
    }
}
