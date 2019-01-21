package android.accounts;

public class AccountAndUser {
    public Account account;
    public int userId;

    public AccountAndUser(Account account, int userId) {
        this.account = account;
        this.userId = userId;
    }

    public boolean equals(Object o) {
        boolean z = true;
        if (this == o) {
            return true;
        }
        if (!(o instanceof AccountAndUser)) {
            return false;
        }
        AccountAndUser other = (AccountAndUser) o;
        if (!(this.account.equals(other.account) && this.userId == other.userId)) {
            z = false;
        }
        return z;
    }

    public int hashCode() {
        return this.account.hashCode() + this.userId;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(this.account.toString());
        stringBuilder.append(" u");
        stringBuilder.append(this.userId);
        return stringBuilder.toString();
    }
}
