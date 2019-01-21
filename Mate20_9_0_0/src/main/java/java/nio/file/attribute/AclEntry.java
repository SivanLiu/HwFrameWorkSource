package java.nio.file.attribute;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public final class AclEntry {
    private final Set<AclEntryFlag> flags;
    private volatile int hash;
    private final Set<AclEntryPermission> perms;
    private final AclEntryType type;
    private final UserPrincipal who;

    public static final class Builder {
        static final /* synthetic */ boolean $assertionsDisabled = false;
        private Set<AclEntryFlag> flags;
        private Set<AclEntryPermission> perms;
        private AclEntryType type;
        private UserPrincipal who;

        static {
            Class cls = AclEntry.class;
        }

        private Builder(AclEntryType type, UserPrincipal who, Set<AclEntryPermission> perms, Set<AclEntryFlag> flags) {
            this.type = type;
            this.who = who;
            this.perms = perms;
            this.flags = flags;
        }

        public AclEntry build() {
            if (this.type == null) {
                throw new IllegalStateException("Missing type component");
            } else if (this.who != null) {
                return new AclEntry(this.type, this.who, this.perms, this.flags);
            } else {
                throw new IllegalStateException("Missing who component");
            }
        }

        public Builder setType(AclEntryType type) {
            if (type != null) {
                this.type = type;
                return this;
            }
            throw new NullPointerException();
        }

        public Builder setPrincipal(UserPrincipal who) {
            if (who != null) {
                this.who = who;
                return this;
            }
            throw new NullPointerException();
        }

        private static void checkSet(Set<?> set, Class<?> type) {
            for (Object e : set) {
                if (e != null) {
                    type.cast(e);
                } else {
                    throw new NullPointerException();
                }
            }
        }

        public Builder setPermissions(Set<AclEntryPermission> perms) {
            if (perms.isEmpty()) {
                perms = Collections.emptySet();
            } else {
                perms = EnumSet.copyOf((Collection) perms);
                checkSet(perms, AclEntryPermission.class);
            }
            this.perms = perms;
            return this;
        }

        public Builder setPermissions(AclEntryPermission... perms) {
            Set<AclEntryPermission> set = EnumSet.noneOf(AclEntryPermission.class);
            int length = perms.length;
            int i = 0;
            while (i < length) {
                AclEntryPermission p = perms[i];
                if (p != null) {
                    set.add(p);
                    i++;
                } else {
                    throw new NullPointerException();
                }
            }
            this.perms = set;
            return this;
        }

        public Builder setFlags(Set<AclEntryFlag> flags) {
            if (flags.isEmpty()) {
                flags = Collections.emptySet();
            } else {
                flags = EnumSet.copyOf((Collection) flags);
                checkSet(flags, AclEntryFlag.class);
            }
            this.flags = flags;
            return this;
        }

        public Builder setFlags(AclEntryFlag... flags) {
            Set<AclEntryFlag> set = EnumSet.noneOf(AclEntryFlag.class);
            int length = flags.length;
            int i = 0;
            while (i < length) {
                AclEntryFlag f = flags[i];
                if (f != null) {
                    set.add(f);
                    i++;
                } else {
                    throw new NullPointerException();
                }
            }
            this.flags = set;
            return this;
        }
    }

    private AclEntry(AclEntryType type, UserPrincipal who, Set<AclEntryPermission> perms, Set<AclEntryFlag> flags) {
        this.type = type;
        this.who = who;
        this.perms = perms;
        this.flags = flags;
    }

    public static Builder newBuilder() {
        return new Builder(null, null, Collections.emptySet(), Collections.emptySet());
    }

    public static Builder newBuilder(AclEntry entry) {
        return new Builder(entry.type, entry.who, entry.perms, entry.flags);
    }

    public AclEntryType type() {
        return this.type;
    }

    public UserPrincipal principal() {
        return this.who;
    }

    public Set<AclEntryPermission> permissions() {
        return new HashSet(this.perms);
    }

    public Set<AclEntryFlag> flags() {
        return new HashSet(this.flags);
    }

    public boolean equals(Object ob) {
        if (ob == this) {
            return true;
        }
        if (ob == null || !(ob instanceof AclEntry)) {
            return false;
        }
        AclEntry other = (AclEntry) ob;
        if (this.type == other.type && this.who.equals(other.who) && this.perms.equals(other.perms) && this.flags.equals(other.flags)) {
            return true;
        }
        return false;
    }

    private static int hash(int h, Object o) {
        return (h * 127) + o.hashCode();
    }

    public int hashCode() {
        if (this.hash != 0) {
            return this.hash;
        }
        this.hash = hash(hash(hash(this.type.hashCode(), this.who), this.perms), this.flags);
        return this.hash;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.who.getName());
        sb.append(':');
        for (AclEntryPermission perm : this.perms) {
            sb.append(perm.name());
            sb.append('/');
        }
        sb.setLength(sb.length() - 1);
        sb.append(':');
        if (!this.flags.isEmpty()) {
            for (AclEntryFlag flag : this.flags) {
                sb.append(flag.name());
                sb.append('/');
            }
            sb.setLength(sb.length() - 1);
            sb.append(':');
        }
        sb.append(this.type.name());
        return sb.toString();
    }
}
