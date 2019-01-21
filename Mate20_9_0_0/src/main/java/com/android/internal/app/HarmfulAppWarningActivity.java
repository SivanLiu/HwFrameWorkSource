package com.android.internal.app;

import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentSender;
import android.content.IntentSender.SendIntentException;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import com.android.internal.app.AlertController.AlertParams;

public class HarmfulAppWarningActivity extends AlertActivity implements OnClickListener {
    private static final String EXTRA_HARMFUL_APP_WARNING = "harmful_app_warning";
    private static final String TAG = HarmfulAppWarningActivity.class.getSimpleName();
    private String mHarmfulAppWarning;
    private String mPackageName;
    private IntentSender mTarget;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        this.mPackageName = intent.getStringExtra("android.intent.extra.PACKAGE_NAME");
        this.mTarget = (IntentSender) intent.getParcelableExtra("android.intent.extra.INTENT");
        this.mHarmfulAppWarning = intent.getStringExtra(EXTRA_HARMFUL_APP_WARNING);
        if (this.mPackageName == null || this.mTarget == null || this.mHarmfulAppWarning == null) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid intent: ");
            stringBuilder.append(intent.toString());
            Log.wtf(str, stringBuilder.toString());
            finish();
        }
        try {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(this.mPackageName, 0);
            AlertParams p = this.mAlertParams;
            p.mTitle = getString(17040163);
            p.mView = createView(applicationInfo);
            p.mPositiveButtonText = getString(17040164);
            p.mPositiveButtonListener = this;
            p.mNegativeButtonText = getString(17040162);
            p.mNegativeButtonListener = this;
            this.mAlert.installContent(this.mAlertParams);
        } catch (NameNotFoundException e) {
            Log.e(TAG, "Could not show warning because package does not exist ", e);
            finish();
        }
    }

    private View createView(ApplicationInfo applicationInfo) {
        View view = getLayoutInflater().inflate(17367152, null);
        ((TextView) view.findViewById(16908734)).setText(applicationInfo.loadSafeLabel(getPackageManager()));
        ((TextView) view.findViewById(16908299)).setText(this.mHarmfulAppWarning);
        return view;
    }

    public void onClick(DialogInterface dialog, int which) {
        switch (which) {
            case -2:
                getPackageManager().setHarmfulAppWarning(this.mPackageName, null);
                try {
                    startIntentSenderForResult((IntentSender) getIntent().getParcelableExtra("android.intent.extra.INTENT"), -1, null, 0, 0, 0);
                } catch (SendIntentException e) {
                    Log.e(TAG, "Error while starting intent sender", e);
                }
                EventLogTags.writeHarmfulAppWarningLaunchAnyway(this.mPackageName);
                finish();
                return;
            case -1:
                getPackageManager().deletePackage(this.mPackageName, null, 0);
                EventLogTags.writeHarmfulAppWarningUninstall(this.mPackageName);
                finish();
                return;
            default:
                return;
        }
    }

    public static Intent createHarmfulAppWarningIntent(Context context, String targetPackageName, IntentSender target, CharSequence harmfulAppWarning) {
        Intent intent = new Intent();
        intent.setClass(context, HarmfulAppWarningActivity.class);
        intent.putExtra("android.intent.extra.PACKAGE_NAME", targetPackageName);
        intent.putExtra("android.intent.extra.INTENT", target);
        intent.putExtra(EXTRA_HARMFUL_APP_WARNING, harmfulAppWarning);
        return intent;
    }
}
