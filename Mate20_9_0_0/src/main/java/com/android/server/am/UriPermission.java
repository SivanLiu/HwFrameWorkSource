package com.android.server.am;

import android.app.GrantedUriPermission;
import android.os.Binder;
import android.os.UserHandle;
import android.util.ArraySet;
import android.util.Log;
import android.util.Slog;
import com.android.server.am.ActivityManagerService.GrantUri;
import com.google.android.collect.Sets;
import java.io.PrintWriter;
import java.util.Comparator;
import java.util.Iterator;

final class UriPermission {
    private static final long INVALID_TIME = Long.MIN_VALUE;
    public static final int STRENGTH_GLOBAL = 2;
    public static final int STRENGTH_NONE = 0;
    public static final int STRENGTH_OWNED = 1;
    public static final int STRENGTH_PERSISTABLE = 3;
    private static final String TAG = "UriPermission";
    int globalModeFlags = 0;
    private ArraySet<UriPermissionOwner> mReadOwners;
    private ArraySet<UriPermissionOwner> mWriteOwners;
    int modeFlags = 0;
    int ownedModeFlags = 0;
    int persistableModeFlags = 0;
    long persistedCreateTime = INVALID_TIME;
    int persistedModeFlags = 0;
    final String sourcePkg;
    private String stringName;
    final String targetPkg;
    final int targetUid;
    final int targetUserId;
    final GrantUri uri;

    public static class PersistedTimeComparator implements Comparator<UriPermission> {
        public int compare(UriPermission lhs, UriPermission rhs) {
            return Long.compare(lhs.persistedCreateTime, rhs.persistedCreateTime);
        }
    }

    public static class Snapshot {
        final long persistedCreateTime;
        final int persistedModeFlags;
        final String sourcePkg;
        final String targetPkg;
        final int targetUserId;
        final GrantUri uri;

        private Snapshot(UriPermission perm) {
            this.targetUserId = perm.targetUserId;
            this.sourcePkg = perm.sourcePkg;
            this.targetPkg = perm.targetPkg;
            this.uri = perm.uri;
            this.persistedModeFlags = perm.persistedModeFlags;
            this.persistedCreateTime = perm.persistedCreateTime;
        }
    }

    UriPermission(String sourcePkg, String targetPkg, int targetUid, GrantUri uri) {
        this.targetUserId = UserHandle.getUserId(targetUid);
        this.sourcePkg = sourcePkg;
        this.targetPkg = targetPkg;
        this.targetUid = targetUid;
        this.uri = uri;
    }

    private void updateModeFlags() {
        int oldModeFlags = this.modeFlags;
        this.modeFlags = ((this.ownedModeFlags | this.globalModeFlags) | this.persistableModeFlags) | this.persistedModeFlags;
        if (Log.isLoggable(TAG, 2) && this.modeFlags != oldModeFlags) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Permission for ");
            stringBuilder.append(this.targetPkg);
            stringBuilder.append(" to ");
            stringBuilder.append(this.uri);
            stringBuilder.append(" is changing from 0x");
            stringBuilder.append(Integer.toHexString(oldModeFlags));
            stringBuilder.append(" to 0x");
            stringBuilder.append(Integer.toHexString(this.modeFlags));
            stringBuilder.append(" via calling UID ");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(" PID ");
            stringBuilder.append(Binder.getCallingPid());
            Slog.d(str, stringBuilder.toString(), new Throwable());
        }
    }

    void initPersistedModes(int modeFlags, long createdTime) {
        modeFlags &= 3;
        this.persistableModeFlags = modeFlags;
        this.persistedModeFlags = modeFlags;
        this.persistedCreateTime = createdTime;
        updateModeFlags();
    }

    void grantModes(int modeFlags, UriPermissionOwner owner) {
        boolean persistable = (modeFlags & 64) != 0;
        modeFlags &= 3;
        if (persistable) {
            this.persistableModeFlags |= modeFlags;
        }
        if (owner == null) {
            this.globalModeFlags |= modeFlags;
        } else {
            if ((modeFlags & 1) != 0) {
                addReadOwner(owner);
            }
            if ((modeFlags & 2) != 0) {
                addWriteOwner(owner);
            }
        }
        updateModeFlags();
    }

    boolean takePersistableModes(int modeFlags) {
        modeFlags &= 3;
        boolean z = false;
        if ((this.persistableModeFlags & modeFlags) != modeFlags) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Requested flags 0x");
            stringBuilder.append(Integer.toHexString(modeFlags));
            stringBuilder.append(", but only 0x");
            stringBuilder.append(Integer.toHexString(this.persistableModeFlags));
            stringBuilder.append(" are allowed");
            Slog.w(str, stringBuilder.toString());
            return false;
        }
        int before = this.persistedModeFlags;
        this.persistedModeFlags |= this.persistableModeFlags & modeFlags;
        if (this.persistedModeFlags != 0) {
            this.persistedCreateTime = System.currentTimeMillis();
        }
        updateModeFlags();
        if (this.persistedModeFlags != before) {
            z = true;
        }
        return z;
    }

    boolean releasePersistableModes(int modeFlags) {
        modeFlags &= 3;
        int before = this.persistedModeFlags;
        this.persistableModeFlags &= ~modeFlags;
        this.persistedModeFlags &= ~modeFlags;
        if (this.persistedModeFlags == 0) {
            this.persistedCreateTime = INVALID_TIME;
        }
        updateModeFlags();
        return this.persistedModeFlags != before;
    }

    boolean revokeModes(int modeFlags, boolean includingOwners) {
        Iterator it;
        boolean persistable = (modeFlags & 64) != 0;
        modeFlags &= 3;
        int before = this.persistedModeFlags;
        if ((modeFlags & 1) != 0) {
            if (persistable) {
                this.persistableModeFlags &= -2;
                this.persistedModeFlags &= -2;
            }
            this.globalModeFlags &= -2;
            if (this.mReadOwners != null && includingOwners) {
                this.ownedModeFlags &= -2;
                it = this.mReadOwners.iterator();
                while (it.hasNext()) {
                    ((UriPermissionOwner) it.next()).removeReadPermission(this);
                }
                this.mReadOwners = null;
            }
        }
        if ((modeFlags & 2) != 0) {
            if (persistable) {
                this.persistableModeFlags &= -3;
                this.persistedModeFlags &= -3;
            }
            this.globalModeFlags &= -3;
            if (this.mWriteOwners != null && includingOwners) {
                this.ownedModeFlags &= -3;
                it = this.mWriteOwners.iterator();
                while (it.hasNext()) {
                    ((UriPermissionOwner) it.next()).removeWritePermission(this);
                }
                this.mWriteOwners = null;
            }
        }
        if (this.persistedModeFlags == 0) {
            this.persistedCreateTime = INVALID_TIME;
        }
        updateModeFlags();
        if (this.persistedModeFlags != before) {
            return true;
        }
        return false;
    }

    public int getStrength(int modeFlags) {
        modeFlags &= 3;
        if ((this.persistableModeFlags & modeFlags) == modeFlags) {
            return 3;
        }
        if ((this.globalModeFlags & modeFlags) == modeFlags) {
            return 2;
        }
        if ((this.ownedModeFlags & modeFlags) == modeFlags) {
            return 1;
        }
        return 0;
    }

    private void addReadOwner(UriPermissionOwner owner) {
        if (this.mReadOwners == null) {
            this.mReadOwners = Sets.newArraySet();
            this.ownedModeFlags |= 1;
            updateModeFlags();
        }
        if (this.mReadOwners.add(owner)) {
            owner.addReadPermission(this);
        }
    }

    void removeReadOwner(UriPermissionOwner owner) {
        if (!this.mReadOwners.remove(owner)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown read owner ");
            stringBuilder.append(owner);
            stringBuilder.append(" in ");
            stringBuilder.append(this);
            Slog.wtf(str, stringBuilder.toString());
        }
        if (this.mReadOwners.size() == 0) {
            this.mReadOwners = null;
            this.ownedModeFlags &= -2;
            updateModeFlags();
        }
    }

    private void addWriteOwner(UriPermissionOwner owner) {
        if (this.mWriteOwners == null) {
            this.mWriteOwners = Sets.newArraySet();
            this.ownedModeFlags |= 2;
            updateModeFlags();
        }
        if (this.mWriteOwners.add(owner)) {
            owner.addWritePermission(this);
        }
    }

    void removeWriteOwner(UriPermissionOwner owner) {
        if (!this.mWriteOwners.remove(owner)) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Unknown write owner ");
            stringBuilder.append(owner);
            stringBuilder.append(" in ");
            stringBuilder.append(this);
            Slog.wtf(str, stringBuilder.toString());
        }
        if (this.mWriteOwners.size() == 0) {
            this.mWriteOwners = null;
            this.ownedModeFlags &= -3;
            updateModeFlags();
        }
    }

    public String toString() {
        if (this.stringName != null) {
            return this.stringName;
        }
        StringBuilder sb = new StringBuilder(128);
        sb.append("UriPermission{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(' ');
        sb.append(this.uri);
        sb.append('}');
        String stringBuilder = sb.toString();
        this.stringName = stringBuilder;
        return stringBuilder;
    }

    void dump(PrintWriter pw, String prefix) {
        Iterator it;
        UriPermissionOwner owner;
        StringBuilder stringBuilder;
        pw.print(prefix);
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("targetUserId=");
        stringBuilder2.append(this.targetUserId);
        pw.print(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" sourcePkg=");
        stringBuilder2.append(this.sourcePkg);
        pw.print(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" targetPkg=");
        stringBuilder2.append(this.targetPkg);
        pw.println(stringBuilder2.toString());
        pw.print(prefix);
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("mode=0x");
        stringBuilder2.append(Integer.toHexString(this.modeFlags));
        pw.print(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" owned=0x");
        stringBuilder2.append(Integer.toHexString(this.ownedModeFlags));
        pw.print(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" global=0x");
        stringBuilder2.append(Integer.toHexString(this.globalModeFlags));
        pw.print(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" persistable=0x");
        stringBuilder2.append(Integer.toHexString(this.persistableModeFlags));
        pw.print(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append(" persisted=0x");
        stringBuilder2.append(Integer.toHexString(this.persistedModeFlags));
        pw.print(stringBuilder2.toString());
        if (this.persistedCreateTime != INVALID_TIME) {
            stringBuilder2 = new StringBuilder();
            stringBuilder2.append(" persistedCreate=");
            stringBuilder2.append(this.persistedCreateTime);
            pw.print(stringBuilder2.toString());
        }
        pw.println();
        if (this.mReadOwners != null) {
            pw.print(prefix);
            pw.println("readOwners:");
            it = this.mReadOwners.iterator();
            while (it.hasNext()) {
                owner = (UriPermissionOwner) it.next();
                pw.print(prefix);
                stringBuilder = new StringBuilder();
                stringBuilder.append("  * ");
                stringBuilder.append(owner);
                pw.println(stringBuilder.toString());
            }
        }
        if (this.mWriteOwners != null) {
            pw.print(prefix);
            pw.println("writeOwners:");
            it = this.mReadOwners.iterator();
            while (it.hasNext()) {
                owner = (UriPermissionOwner) it.next();
                pw.print(prefix);
                stringBuilder = new StringBuilder();
                stringBuilder.append("  * ");
                stringBuilder.append(owner);
                pw.println(stringBuilder.toString());
            }
        }
    }

    public Snapshot snapshot() {
        return new Snapshot();
    }

    public android.content.UriPermission buildPersistedPublicApiObject() {
        return new android.content.UriPermission(this.uri.uri, this.persistedModeFlags, this.persistedCreateTime);
    }

    public GrantedUriPermission buildGrantedUriPermission() {
        return new GrantedUriPermission(this.uri.uri, this.targetPkg);
    }
}
