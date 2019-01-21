package android.app;

import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.TextUtils;
import java.io.PrintWriter;

public final class RemoteAction implements Parcelable {
    public static final Creator<RemoteAction> CREATOR = new Creator<RemoteAction>() {
        public RemoteAction createFromParcel(Parcel in) {
            return new RemoteAction(in);
        }

        public RemoteAction[] newArray(int size) {
            return new RemoteAction[size];
        }
    };
    private static final String TAG = "RemoteAction";
    private final PendingIntent mActionIntent;
    private final CharSequence mContentDescription;
    private boolean mEnabled;
    private final Icon mIcon;
    private boolean mShouldShowIcon;
    private final CharSequence mTitle;

    RemoteAction(Parcel in) {
        this.mIcon = (Icon) Icon.CREATOR.createFromParcel(in);
        this.mTitle = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.mContentDescription = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(in);
        this.mActionIntent = (PendingIntent) PendingIntent.CREATOR.createFromParcel(in);
        this.mEnabled = in.readBoolean();
        this.mShouldShowIcon = in.readBoolean();
    }

    public RemoteAction(Icon icon, CharSequence title, CharSequence contentDescription, PendingIntent intent) {
        if (icon == null || title == null || contentDescription == null || intent == null) {
            throw new IllegalArgumentException("Expected icon, title, content description and action callback");
        }
        this.mIcon = icon;
        this.mTitle = title;
        this.mContentDescription = contentDescription;
        this.mActionIntent = intent;
        this.mEnabled = true;
        this.mShouldShowIcon = true;
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    public boolean isEnabled() {
        return this.mEnabled;
    }

    public void setShouldShowIcon(boolean shouldShowIcon) {
        this.mShouldShowIcon = shouldShowIcon;
    }

    public boolean shouldShowIcon() {
        return this.mShouldShowIcon;
    }

    public Icon getIcon() {
        return this.mIcon;
    }

    public CharSequence getTitle() {
        return this.mTitle;
    }

    public CharSequence getContentDescription() {
        return this.mContentDescription;
    }

    public PendingIntent getActionIntent() {
        return this.mActionIntent;
    }

    public RemoteAction clone() {
        RemoteAction action = new RemoteAction(this.mIcon, this.mTitle, this.mContentDescription, this.mActionIntent);
        action.setEnabled(this.mEnabled);
        action.setShouldShowIcon(this.mShouldShowIcon);
        return action;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        this.mIcon.writeToParcel(out, 0);
        TextUtils.writeToParcel(this.mTitle, out, flags);
        TextUtils.writeToParcel(this.mContentDescription, out, flags);
        this.mActionIntent.writeToParcel(out, flags);
        out.writeBoolean(this.mEnabled);
        out.writeBoolean(this.mShouldShowIcon);
    }

    public void dump(String prefix, PrintWriter pw) {
        pw.print(prefix);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("title=");
        stringBuilder.append(this.mTitle);
        pw.print(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" enabled=");
        stringBuilder.append(this.mEnabled);
        pw.print(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" contentDescription=");
        stringBuilder.append(this.mContentDescription);
        pw.print(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" icon=");
        stringBuilder.append(this.mIcon);
        pw.print(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" action=");
        stringBuilder.append(this.mActionIntent.getIntent());
        pw.print(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(" shouldShowIcon=");
        stringBuilder.append(this.mShouldShowIcon);
        pw.print(stringBuilder.toString());
        pw.println();
    }
}
