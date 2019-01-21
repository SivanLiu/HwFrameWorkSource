package android.view.inputmethod;

import android.os.Bundle;
import android.os.FileObserver;
import android.os.LocaleList;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Printer;
import java.util.Arrays;

public class EditorInfo implements InputType, Parcelable {
    public static final Creator<EditorInfo> CREATOR = new Creator<EditorInfo>() {
        public EditorInfo createFromParcel(Parcel source) {
            EditorInfo res = new EditorInfo();
            res.inputType = source.readInt();
            res.imeOptions = source.readInt();
            res.privateImeOptions = source.readString();
            res.actionLabel = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            res.actionId = source.readInt();
            res.initialSelStart = source.readInt();
            res.initialSelEnd = source.readInt();
            res.initialCapsMode = source.readInt();
            res.hintText = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            res.label = (CharSequence) TextUtils.CHAR_SEQUENCE_CREATOR.createFromParcel(source);
            res.packageName = source.readString();
            res.fieldId = source.readInt();
            res.fieldName = source.readString();
            res.extras = source.readBundle();
            LocaleList hintLocales = (LocaleList) LocaleList.CREATOR.createFromParcel(source);
            res.hintLocales = hintLocales.isEmpty() ? null : hintLocales;
            res.contentMimeTypes = source.readStringArray();
            return res;
        }

        public EditorInfo[] newArray(int size) {
            return new EditorInfo[size];
        }
    };
    public static final int IME_ACTION_DONE = 6;
    public static final int IME_ACTION_GO = 2;
    public static final int IME_ACTION_NEXT = 5;
    public static final int IME_ACTION_NONE = 1;
    public static final int IME_ACTION_PREVIOUS = 7;
    public static final int IME_ACTION_SEARCH = 3;
    public static final int IME_ACTION_SEND = 4;
    public static final int IME_ACTION_UNSPECIFIED = 0;
    public static final int IME_FLAG_FORCE_ASCII = Integer.MIN_VALUE;
    public static final int IME_FLAG_NAVIGATE_NEXT = 134217728;
    public static final int IME_FLAG_NAVIGATE_PREVIOUS = 67108864;
    public static final int IME_FLAG_NO_ACCESSORY_ACTION = 536870912;
    public static final int IME_FLAG_NO_ENTER_ACTION = 1073741824;
    public static final int IME_FLAG_NO_EXTRACT_UI = 268435456;
    public static final int IME_FLAG_NO_FULLSCREEN = 33554432;
    public static final int IME_FLAG_NO_PERSONALIZED_LEARNING = 16777216;
    public static final int IME_MASK_ACTION = 255;
    public static final int IME_NULL = 0;
    public int actionId = 0;
    public CharSequence actionLabel = null;
    public String[] contentMimeTypes = null;
    public Bundle extras;
    public int fieldId;
    public String fieldName;
    public LocaleList hintLocales = null;
    public CharSequence hintText;
    public int imeOptions = 0;
    public int initialCapsMode = 0;
    public int initialSelEnd = -1;
    public int initialSelStart = -1;
    public int inputType = 0;
    public CharSequence label;
    public String packageName;
    public String privateImeOptions = null;

    public final void makeCompatible(int targetSdkVersion) {
        if (targetSdkVersion < 11) {
            int i = this.inputType & FileObserver.ALL_EVENTS;
            if (i == 2 || i == 18) {
                this.inputType = (this.inputType & InputType.TYPE_MASK_FLAGS) | 2;
            } else if (i == 209) {
                this.inputType = 33 | (this.inputType & InputType.TYPE_MASK_FLAGS);
            } else if (i == 225) {
                this.inputType = 129 | (this.inputType & InputType.TYPE_MASK_FLAGS);
            }
        }
    }

    public void dump(Printer pw, String prefix) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("inputType=0x");
        stringBuilder.append(Integer.toHexString(this.inputType));
        stringBuilder.append(" imeOptions=0x");
        stringBuilder.append(Integer.toHexString(this.imeOptions));
        stringBuilder.append(" privateImeOptions=");
        stringBuilder.append(this.privateImeOptions);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("actionLabel=");
        stringBuilder.append(this.actionLabel);
        stringBuilder.append(" actionId=");
        stringBuilder.append(this.actionId);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("initialSelStart=");
        stringBuilder.append(this.initialSelStart);
        stringBuilder.append(" initialSelEnd=");
        stringBuilder.append(this.initialSelEnd);
        stringBuilder.append(" initialCapsMode=0x");
        stringBuilder.append(Integer.toHexString(this.initialCapsMode));
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("hintText=");
        stringBuilder.append(this.hintText);
        stringBuilder.append(" label=");
        stringBuilder.append(this.label);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("packageName=");
        stringBuilder.append(this.packageName);
        stringBuilder.append(" fieldId=");
        stringBuilder.append(this.fieldId);
        stringBuilder.append(" fieldName=");
        stringBuilder.append(this.fieldName);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("extras=");
        stringBuilder.append(this.extras);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("hintLocales=");
        stringBuilder.append(this.hintLocales);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("contentMimeTypes=");
        stringBuilder.append(Arrays.toString(this.contentMimeTypes));
        pw.println(stringBuilder.toString());
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.inputType);
        dest.writeInt(this.imeOptions);
        dest.writeString(this.privateImeOptions);
        TextUtils.writeToParcel(this.actionLabel, dest, flags);
        dest.writeInt(this.actionId);
        dest.writeInt(this.initialSelStart);
        dest.writeInt(this.initialSelEnd);
        dest.writeInt(this.initialCapsMode);
        TextUtils.writeToParcel(this.hintText, dest, flags);
        TextUtils.writeToParcel(this.label, dest, flags);
        dest.writeString(this.packageName);
        dest.writeInt(this.fieldId);
        dest.writeString(this.fieldName);
        dest.writeBundle(this.extras);
        if (this.hintLocales != null) {
            this.hintLocales.writeToParcel(dest, flags);
        } else {
            LocaleList.getEmptyLocaleList().writeToParcel(dest, flags);
        }
        dest.writeStringArray(this.contentMimeTypes);
    }

    public int describeContents() {
        return 0;
    }
}
