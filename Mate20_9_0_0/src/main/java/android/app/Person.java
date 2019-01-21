package android.app;

import android.graphics.drawable.Icon;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;

public final class Person implements Parcelable {
    public static final Creator<Person> CREATOR = new Creator<Person>() {
        public Person createFromParcel(Parcel in) {
            return new Person(in, null);
        }

        public Person[] newArray(int size) {
            return new Person[size];
        }
    };
    private Icon mIcon;
    private boolean mIsBot;
    private boolean mIsImportant;
    private String mKey;
    private CharSequence mName;
    private String mUri;

    public static class Builder {
        private Icon mIcon;
        private boolean mIsBot;
        private boolean mIsImportant;
        private String mKey;
        private CharSequence mName;
        private String mUri;

        /* synthetic */ Builder(Person x0, AnonymousClass1 x1) {
            this(x0);
        }

        private Builder(Person person) {
            this.mName = person.mName;
            this.mIcon = person.mIcon;
            this.mUri = person.mUri;
            this.mKey = person.mKey;
            this.mIsBot = person.mIsBot;
            this.mIsImportant = person.mIsImportant;
        }

        public Builder setName(CharSequence name) {
            this.mName = name;
            return this;
        }

        public Builder setIcon(Icon icon) {
            this.mIcon = icon;
            return this;
        }

        public Builder setUri(String uri) {
            this.mUri = uri;
            return this;
        }

        public Builder setKey(String key) {
            this.mKey = key;
            return this;
        }

        public Builder setImportant(boolean isImportant) {
            this.mIsImportant = isImportant;
            return this;
        }

        public Builder setBot(boolean isBot) {
            this.mIsBot = isBot;
            return this;
        }

        public Person build() {
            return new Person(this, null);
        }
    }

    private Person(Parcel in) {
        this.mName = in.readCharSequence();
        if (in.readInt() != 0) {
            this.mIcon = (Icon) Icon.CREATOR.createFromParcel(in);
        }
        this.mUri = in.readString();
        this.mKey = in.readString();
        this.mIsImportant = in.readBoolean();
        this.mIsBot = in.readBoolean();
    }

    private Person(Builder builder) {
        this.mName = builder.mName;
        this.mIcon = builder.mIcon;
        this.mUri = builder.mUri;
        this.mKey = builder.mKey;
        this.mIsBot = builder.mIsBot;
        this.mIsImportant = builder.mIsImportant;
    }

    public Builder toBuilder() {
        return new Builder(this, null);
    }

    public String getUri() {
        return this.mUri;
    }

    public CharSequence getName() {
        return this.mName;
    }

    public Icon getIcon() {
        return this.mIcon;
    }

    public String getKey() {
        return this.mKey;
    }

    public boolean isBot() {
        return this.mIsBot;
    }

    public boolean isImportant() {
        return this.mIsImportant;
    }

    public String resolveToLegacyUri() {
        if (this.mUri != null) {
            return this.mUri;
        }
        if (this.mName == null) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("name:");
        stringBuilder.append(this.mName);
        return stringBuilder.toString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeCharSequence(this.mName);
        if (this.mIcon != null) {
            dest.writeInt(1);
            this.mIcon.writeToParcel(dest, 0);
        } else {
            dest.writeInt(0);
        }
        dest.writeString(this.mUri);
        dest.writeString(this.mKey);
        dest.writeBoolean(this.mIsImportant);
        dest.writeBoolean(this.mIsBot);
    }
}
