package android.os;

import android.os.Parcelable.Creator;

public final class UserManager$EnforcingUser implements Parcelable {
    public static final Creator<UserManager$EnforcingUser> CREATOR = new Creator<UserManager$EnforcingUser>() {
        public UserManager$EnforcingUser createFromParcel(Parcel in) {
            return new UserManager$EnforcingUser(in);
        }

        public UserManager$EnforcingUser[] newArray(int size) {
            return new UserManager$EnforcingUser[size];
        }
    };
    private final int userId;
    private final int userRestrictionSource;

    public UserManager$EnforcingUser(int userId, int userRestrictionSource) {
        this.userId = userId;
        this.userRestrictionSource = userRestrictionSource;
    }

    private UserManager$EnforcingUser(Parcel in) {
        this.userId = in.readInt();
        this.userRestrictionSource = in.readInt();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.userId);
        dest.writeInt(this.userRestrictionSource);
    }

    public UserHandle getUserHandle() {
        return UserHandle.of(this.userId);
    }

    public int getUserRestrictionSource() {
        return this.userRestrictionSource;
    }
}
