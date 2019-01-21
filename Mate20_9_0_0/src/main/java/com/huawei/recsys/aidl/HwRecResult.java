package com.huawei.recsys.aidl;

import android.annotation.TargetApi;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public class HwRecResult implements Parcelable {
    public static final Creator<HwRecResult> CREATOR = new Creator<HwRecResult>() {
        public HwRecResult createFromParcel(Parcel source) {
            return new HwRecResult(source);
        }

        public HwRecResult[] newArray(int size) {
            return new HwRecResult[size];
        }
    };
    private String activity;
    private String hurl;
    private String icon;
    private int id;
    private String intent;
    private String pkg;
    private double proba;
    private String service;
    private int shortcut;
    private int startMode;
    private String titleCn;
    private String titleEn;

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwRecResult{id=");
        stringBuilder.append(this.id);
        stringBuilder.append(", activity='");
        stringBuilder.append(this.activity);
        stringBuilder.append('\'');
        stringBuilder.append(", hurl='");
        stringBuilder.append(this.hurl);
        stringBuilder.append('\'');
        stringBuilder.append(", icon='");
        stringBuilder.append(this.icon);
        stringBuilder.append('\'');
        stringBuilder.append(", intent='");
        stringBuilder.append(this.intent);
        stringBuilder.append('\'');
        stringBuilder.append(", pkg='");
        stringBuilder.append(this.pkg);
        stringBuilder.append('\'');
        stringBuilder.append(", service='");
        stringBuilder.append(this.service);
        stringBuilder.append('\'');
        stringBuilder.append(", startMode=");
        stringBuilder.append(this.startMode);
        stringBuilder.append(", titleCn='");
        stringBuilder.append(this.titleCn);
        stringBuilder.append('\'');
        stringBuilder.append(", titleEn='");
        stringBuilder.append(this.titleEn);
        stringBuilder.append('\'');
        stringBuilder.append(", proba=");
        stringBuilder.append(this.proba);
        stringBuilder.append(", shortcut=");
        stringBuilder.append(this.shortcut);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }

    public int getId() {
        return this.id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getActivity() {
        return this.activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public String getHurl() {
        return this.hurl;
    }

    public void setHurl(String hurl) {
        this.hurl = hurl;
    }

    public String getIcon() {
        return this.icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getIntent() {
        return this.intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getPkg() {
        return this.pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public String getService() {
        return this.service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public int getStartMode() {
        return this.startMode;
    }

    public void setStartMode(int startMode) {
        this.startMode = startMode;
    }

    public String getTitleCn() {
        return this.titleCn;
    }

    public void setTitleCn(String titleCn) {
        this.titleCn = titleCn;
    }

    public String getTitleEn() {
        return this.titleEn;
    }

    public void setTitleEn(String titleEn) {
        this.titleEn = titleEn;
    }

    public double getProba() {
        return this.proba;
    }

    public void setProba(double proba) {
        this.proba = proba;
    }

    public int getShortcut() {
        return this.shortcut;
    }

    public void setShortcut(int shortcut) {
        this.shortcut = shortcut;
    }

    public HwRecResult(Parcel in) {
        this.id = in.readInt();
        this.activity = in.readString();
        this.hurl = in.readString();
        this.icon = in.readString();
        this.intent = in.readString();
        this.pkg = in.readString();
        this.service = in.readString();
        this.startMode = in.readInt();
        this.titleCn = in.readString();
        this.titleEn = in.readString();
        this.proba = in.readDouble();
        this.shortcut = in.readInt();
    }

    public int describeContents() {
        return 0;
    }

    public void readFromParcel(Parcel in) {
        this.id = in.readInt();
        this.activity = in.readString();
        this.hurl = in.readString();
        this.icon = in.readString();
        this.intent = in.readString();
        this.pkg = in.readString();
        this.service = in.readString();
        this.startMode = in.readInt();
        this.titleCn = in.readString();
        this.titleEn = in.readString();
        this.proba = in.readDouble();
        this.shortcut = in.readInt();
    }

    @TargetApi(25)
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeString(this.activity);
        dest.writeString(this.hurl);
        dest.writeString(this.icon);
        dest.writeString(this.intent);
        dest.writeString(this.pkg);
        dest.writeString(this.service);
        dest.writeInt(this.startMode);
        dest.writeString(this.titleCn);
        dest.writeString(this.titleEn);
        dest.writeDouble(this.proba);
        dest.writeInt(this.shortcut);
    }
}
