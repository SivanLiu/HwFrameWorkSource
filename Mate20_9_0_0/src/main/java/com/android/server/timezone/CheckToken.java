package com.android.server.timezone;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

final class CheckToken {
    final int mOptimisticLockId;
    final PackageVersions mPackageVersions;

    CheckToken(int optimisticLockId, PackageVersions packageVersions) {
        this.mOptimisticLockId = optimisticLockId;
        if (packageVersions != null) {
            this.mPackageVersions = packageVersions;
            return;
        }
        throw new NullPointerException("packageVersions == null");
    }

    byte[] toByteArray() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(12);
        DataOutputStream dos;
        try {
            dos = new DataOutputStream(baos);
            dos.writeInt(this.mOptimisticLockId);
            dos.writeLong(this.mPackageVersions.mUpdateAppVersion);
            dos.writeLong(this.mPackageVersions.mDataAppVersion);
            $closeResource(null, dos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Unable to write into a ByteArrayOutputStream", e);
        } catch (Throwable th) {
            $closeResource(r2, dos);
        }
    }

    private static /* synthetic */ void $closeResource(Throwable x0, AutoCloseable x1) {
        if (x0 != null) {
            try {
                x1.close();
                return;
            } catch (Throwable th) {
                x0.addSuppressed(th);
                return;
            }
        }
        x1.close();
    }

    /* JADX WARNING: Missing block: B:9:0x0029, code skipped:
            $closeResource(r2, r1);
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static CheckToken fromByteArray(byte[] tokenBytes) throws IOException {
        DataInputStream dis = new DataInputStream(new ByteArrayInputStream(tokenBytes));
        CheckToken checkToken = new CheckToken(dis.readInt(), new PackageVersions(dis.readLong(), dis.readLong()));
        $closeResource(null, dis);
        return checkToken;
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CheckToken checkToken = (CheckToken) o;
        if (this.mOptimisticLockId != checkToken.mOptimisticLockId) {
            return false;
        }
        return this.mPackageVersions.equals(checkToken.mPackageVersions);
    }

    public int hashCode() {
        return (31 * this.mOptimisticLockId) + this.mPackageVersions.hashCode();
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Token{mOptimisticLockId=");
        stringBuilder.append(this.mOptimisticLockId);
        stringBuilder.append(", mPackageVersions=");
        stringBuilder.append(this.mPackageVersions);
        stringBuilder.append('}');
        return stringBuilder.toString();
    }
}
