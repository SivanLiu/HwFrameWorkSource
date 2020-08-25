package com.android.server.security.pwdprotect;

import android.content.Context;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Log;
import com.android.server.rms.iaware.memory.utils.BigMemoryConstant;
import com.android.server.security.core.IHwSecurityPlugin;
import com.android.server.security.pwdprotect.logic.ModifyPassWord;
import com.android.server.security.pwdprotect.logic.ResetPassWord;
import com.android.server.security.pwdprotect.logic.StartPwdProtect;
import com.android.server.security.pwdprotect.model.PasswordIvsCache;
import huawei.android.security.IPwdProtectManager;

public class PwdProtectService extends IPwdProtectManager.Stub implements IHwSecurityPlugin {
    public static final Creator CREATOR = new Creator() {
        /* class com.android.server.security.pwdprotect.PwdProtectService.AnonymousClass1 */

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public IHwSecurityPlugin createPlugin(Context context) {
            Log.d(PwdProtectService.TAG, "create PwdProtectService");
            return new PwdProtectService(context);
        }

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public String getPluginPermission() {
            return null;
        }
    };
    private static final String DB_KEY_PRIVACY_USER_PWD_PROTECT = "privacy_user_pwd_protect";
    private static final String PRIV_SPACE_PWD_PROTECT_PERMISSION = "com.huawei.privacyspace.permission.PASSWORD_RESET";
    private static final String TAG = "PwdProtectService";
    private Context mContext;

    public PwdProtectService(Context context) {
        this.mContext = context;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.server.security.pwdprotect.PwdProtectService */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.security.core.IHwSecurityPlugin
    public IBinder asBinder() {
        return this;
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStart() {
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStop() {
    }

    public boolean hasKeyFileExisted() throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission(PRIV_SPACE_PWD_PROTECT_PERMISSION, "does not have privSpace pwd protect permission!");
        if (PasswordIvsCache.FILE_E_PIN2.exists() || PasswordIvsCache.FILE_E_SK2.exists() || PasswordIvsCache.FILE_E_PWDQANSWER.exists() || PasswordIvsCache.FILE_E_PWDQ.exists()) {
            return true;
        }
        return false;
    }

    public boolean removeKeyFile() throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission(PRIV_SPACE_PWD_PROTECT_PERMISSION, "does not have privSpace pwd protect permission!");
        if (!hasKeyFileExisted()) {
            Log.e(TAG, "removeKeyFile fail, files do not exist");
            return false;
        } else if (!PasswordIvsCache.FILE_E_PIN2.delete() || !PasswordIvsCache.FILE_E_SK2.delete() || !PasswordIvsCache.FILE_E_PWDQANSWER.delete() || !PasswordIvsCache.FILE_E_PWDQ.delete()) {
            return false;
        } else {
            Log.i(TAG, "removeKeyFile success");
            return true;
        }
    }

    public boolean modifyPrivPwd(String password) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission(PRIV_SPACE_PWD_PROTECT_PERMISSION, "does not have privSpace pwd protect permission!");
        if (!TextUtils.isEmpty(password)) {
            return ModifyPassWord.modifyPrivSpacePw(password);
        }
        Log.e(TAG, "modifyPrivPwd failed,str is null");
        return false;
    }

    public boolean modifyMainPwd(String origPassword, String newPassword) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission(PRIV_SPACE_PWD_PROTECT_PERMISSION, "does not have privSpace pwd protect permission!");
        if (!TextUtils.isEmpty(origPassword) || !TextUtils.isEmpty(newPassword)) {
            return ModifyPassWord.modifyMainSpacePw(origPassword, newPassword);
        }
        Log.e(TAG, "modifyMainPwd failed,str is null");
        return false;
    }

    public String decodeCurrentPwd(String mainSpacePin, String answer) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission(PRIV_SPACE_PWD_PROTECT_PERMISSION, "does not have privSpace pwd protect permission!");
        if (!TextUtils.isEmpty(mainSpacePin) || !TextUtils.isEmpty(answer)) {
            String origPwd = ResetPassWord.decodeCurrentPwd(mainSpacePin, answer);
            if (TextUtils.isEmpty(origPwd)) {
                Flog.bdReport(this.mContext, (int) BigMemoryConstant.ACTIVITY_NAME_MAX_LEN);
            }
            return origPwd;
        }
        Log.e(TAG, "decodeCurrentPwd failed,str is null");
        return null;
    }

    public boolean startPwdProtect(String privSpacePin, String question, String answer, String mainSpacePin) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission(PRIV_SPACE_PWD_PROTECT_PERMISSION, "does not have privSpace pwd protect permission!");
        if (TextUtils.isEmpty(privSpacePin) && TextUtils.isEmpty(question) && TextUtils.isEmpty(answer) && TextUtils.isEmpty(mainSpacePin)) {
            Log.e(TAG, "startPwdProtect failed,str is null");
            return false;
        } else if (Boolean.valueOf(new StartPwdProtect().turnOnPwdProtect(privSpacePin, question, answer, mainSpacePin)).booleanValue()) {
            Log.i(TAG, "startPwdProtect success");
            if (isPrivSpacePwdProtectOpened()) {
                Flog.bdReport(this.mContext, 133);
                return true;
            }
            Flog.bdReport(this.mContext, 130);
            return true;
        } else {
            Log.e(TAG, "startPwdProtect failed");
            return false;
        }
    }

    public String getPwdQuestion() throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission(PRIV_SPACE_PWD_PROTECT_PERMISSION, "does not have privSpace pwd protect permission!");
        return ResetPassWord.getPwdQuestion();
    }

    public boolean pwdQAnswerVertify(byte[] pwQuestion) throws RemoteException {
        this.mContext.enforceCallingOrSelfPermission(PRIV_SPACE_PWD_PROTECT_PERMISSION, "does not have privSpace pwd protect permission!");
        if (pwQuestion != null) {
            return ResetPassWord.pwdQAnswerVertify(pwQuestion).booleanValue();
        }
        Log.e(TAG, "pwdQAnswerVertify failed,str is null");
        return false;
    }

    private boolean isPrivSpacePwdProtectOpened() {
        Context context = this.mContext;
        if (context != null && Settings.Global.getInt(context.getContentResolver(), "privacy_user_pwd_protect", 0) == 1) {
            return true;
        }
        return false;
    }
}
