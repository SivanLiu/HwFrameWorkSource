package com.android.internal.app;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.UserHandle;
import android.os.UserManager;
import android.util.Log;

public class UnlaunchableAppActivity extends Activity implements OnDismissListener, OnClickListener {
    private static final String EXTRA_UNLAUNCHABLE_REASON = "unlaunchable_reason";
    private static final String TAG = "UnlaunchableAppActivity";
    private static final int UNLAUNCHABLE_REASON_QUIET_MODE = 1;
    private int mReason;
    private IntentSender mTarget;
    private int mUserId;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(1);
        Intent intent = getIntent();
        this.mReason = intent.getIntExtra(EXTRA_UNLAUNCHABLE_REASON, -1);
        this.mUserId = intent.getIntExtra("android.intent.extra.user_handle", -10000);
        this.mTarget = (IntentSender) intent.getParcelableExtra("android.intent.extra.INTENT");
        String str;
        if (this.mUserId == -10000) {
            str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Invalid user id: ");
            stringBuilder.append(this.mUserId);
            stringBuilder.append(". Stopping.");
            Log.wtf(str, stringBuilder.toString());
            finish();
        } else if (this.mReason == 1) {
            String dialogTitle = getResources().getString(17041422);
            String dialogMessage = getResources().getString(17041421);
            getWindow().setBackgroundDrawableResource(17170445);
            Builder builder = new Builder(this).setTitle(dialogTitle).setMessage(dialogMessage).setOnDismissListener(this);
            if (this.mReason == 1) {
                builder.setPositiveButton(17041423, this).setNegativeButton(17039360, null);
            } else {
                builder.setPositiveButton(17039370, null);
            }
            builder.show();
        } else {
            str = TAG;
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("Invalid unlaunchable type: ");
            stringBuilder2.append(this.mReason);
            Log.wtf(str, stringBuilder2.toString());
            finish();
        }
    }

    public void onDismiss(DialogInterface dialog) {
        finish();
    }

    public void onClick(DialogInterface dialog, int which) {
        if (this.mReason == 1 && which == -1) {
            UserManager.get(this).requestQuietModeEnabled(false, UserHandle.of(this.mUserId), this.mTarget);
        }
    }

    private static final Intent createBaseIntent() {
        Intent intent = new Intent();
        intent.setComponent(new ComponentName("android", UnlaunchableAppActivity.class.getName()));
        intent.setFlags(276824064);
        return intent;
    }

    public static Intent createInQuietModeDialogIntent(int userId) {
        Intent intent = createBaseIntent();
        intent.putExtra(EXTRA_UNLAUNCHABLE_REASON, 1);
        intent.putExtra("android.intent.extra.user_handle", userId);
        return intent;
    }

    public static Intent createInQuietModeDialogIntent(int userId, IntentSender target) {
        Intent intent = createInQuietModeDialogIntent(userId);
        intent.putExtra("android.intent.extra.INTENT", target);
        return intent;
    }
}
