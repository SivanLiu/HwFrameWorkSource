package com.android.internal.statusbar;

import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.UserHandle;
import android.text.TextUtils;

public class StatusBarIcon implements Parcelable {
    public static final Creator<StatusBarIcon> CREATOR = new Creator<StatusBarIcon>() {
        public StatusBarIcon createFromParcel(Parcel parcel) {
            return new StatusBarIcon(parcel);
        }

        public StatusBarIcon[] newArray(int size) {
            return new StatusBarIcon[size];
        }
    };
    public CharSequence contentDescription;
    public Icon icon;
    public int iconLevel;
    public int number;
    public String pkg;
    public UserHandle user;
    public boolean visible;

    public StatusBarIcon(UserHandle user, String resPackage, Icon icon, int iconLevel, int number, CharSequence contentDescription) {
        this.visible = true;
        if (icon.getType() == 2 && TextUtils.isEmpty(icon.getResPackage())) {
            icon = Icon.createWithResource(resPackage, icon.getResId());
        }
        this.pkg = resPackage;
        this.user = user;
        this.icon = icon;
        this.iconLevel = iconLevel;
        this.number = number;
        this.contentDescription = contentDescription;
    }

    public StatusBarIcon(String iconPackage, UserHandle user, int iconId, int iconLevel, int number, CharSequence contentDescription) {
        this(user, iconPackage, Icon.createWithResource(iconPackage, iconId), iconLevel, number, contentDescription);
    }

    public String toString() {
        StringBuilder stringBuilder;
        String stringBuilder2;
        StringBuilder stringBuilder3 = new StringBuilder();
        stringBuilder3.append("StatusBarIcon(icon=");
        stringBuilder3.append(this.icon);
        if (this.iconLevel != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" level=");
            stringBuilder.append(this.iconLevel);
            stringBuilder2 = stringBuilder.toString();
        } else {
            stringBuilder2 = "";
        }
        stringBuilder3.append(stringBuilder2);
        stringBuilder3.append(this.visible ? " visible" : "");
        stringBuilder3.append(" user=");
        stringBuilder3.append(this.user.getIdentifier());
        if (this.number != 0) {
            stringBuilder = new StringBuilder();
            stringBuilder.append(" num=");
            stringBuilder.append(this.number);
            stringBuilder2 = stringBuilder.toString();
        } else {
            stringBuilder2 = "";
        }
        stringBuilder3.append(stringBuilder2);
        stringBuilder3.append(" )");
        return stringBuilder3.toString();
    }

    public StatusBarIcon clone() {
        StatusBarIcon that = new StatusBarIcon(this.user, this.pkg, this.icon, this.iconLevel, this.number, this.contentDescription);
        that.visible = this.visible;
        return that;
    }

    public StatusBarIcon(Parcel in) {
        this.visible = true;
        readFromParcel(in);
    }

    public void readFromParcel(Parcel in) {
        this.icon = (Icon) in.readParcelable(null);
        this.pkg = in.readString();
        this.user = (UserHandle) in.readParcelable(null);
        this.iconLevel = in.readInt();
        this.visible = in.readInt() != 0;
        this.number = in.readInt();
        this.contentDescription = in.readCharSequence();
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(this.icon, 0);
        out.writeString(this.pkg);
        out.writeParcelable(this.user, 0);
        out.writeInt(this.iconLevel);
        out.writeInt(this.visible);
        out.writeInt(this.number);
        out.writeCharSequence(this.contentDescription);
    }

    public int describeContents() {
        return 0;
    }
}
