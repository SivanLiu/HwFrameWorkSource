package com.android.server.mtm.iaware.appmng.appclean;

import android.app.mtm.iaware.appmng.AppCleanParam;
import android.app.mtm.iaware.appmng.AppMngConstant.AppCleanSource;
import android.app.mtm.iaware.appmng.IAppCleanCallback;
import android.content.Context;
import android.rms.iaware.AwareLog;

public class AppCleaner {
    private static final String TAG = "AppCleaner";
    private static volatile AppCleaner mAppCleaner;
    private Context mContext;

    /* renamed from: com.android.server.mtm.iaware.appmng.appclean.AppCleaner$1 */
    static /* synthetic */ class AnonymousClass1 {
        static final /* synthetic */ int[] $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource = new int[AppCleanSource.values().length];

        static {
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[AppCleanSource.SMART_CLEAN.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[AppCleanSource.SYSTEM_MANAGER.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[AppCleanSource.POWER_GENIE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[AppCleanSource.THERMAL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    public static AppCleaner getInstance(Context context) {
        if (mAppCleaner == null) {
            synchronized (AppCleaner.class) {
                if (mAppCleaner == null && context != null) {
                    mAppCleaner = new AppCleaner(context);
                }
            }
        }
        return mAppCleaner;
    }

    private AppCleaner(Context context) {
        this.mContext = context;
    }

    public void requestAppClean(AppCleanSource config) {
        if (config == null) {
            AwareLog.e(TAG, "requestAppClean source = null");
        } else if (AnonymousClass1.$SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[config.ordinal()] != 1) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("bad request: no source for ");
            stringBuilder.append(config);
            AwareLog.e(str, stringBuilder.toString());
        } else {
            new SmartClean(this.mContext).clean();
        }
    }

    public void requestAppCleanWithCallback(AppCleanParam param, IAppCleanCallback callback) {
        if (param != null) {
            int sourceCode = param.getSource();
            String str;
            StringBuilder stringBuilder;
            if (sourceCode < 0 || sourceCode > AppCleanSource.values().length) {
                str = TAG;
                stringBuilder = new StringBuilder();
                stringBuilder.append("bad request: invalid source = ");
                stringBuilder.append(sourceCode);
                AwareLog.e(str, stringBuilder.toString());
                return;
            }
            CleanSource source;
            switch (AnonymousClass1.$SwitchMap$android$app$mtm$iaware$appmng$AppMngConstant$AppCleanSource[AppCleanSource.values()[sourceCode].ordinal()]) {
                case 2:
                    source = new HSMClean(param, callback, this.mContext);
                    break;
                case 3:
                    source = new PGClean(param, callback, this.mContext);
                    break;
                case 4:
                    source = new ThermalClean(param, callback, this.mContext);
                    break;
                default:
                    str = TAG;
                    stringBuilder = new StringBuilder();
                    stringBuilder.append("bad request: no source for ");
                    stringBuilder.append(sourceCode);
                    AwareLog.e(str, stringBuilder.toString());
                    return;
            }
            source.clean();
        } else {
            AwareLog.e(TAG, "bad request: param is null");
        }
    }
}
