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
        if (packageVersions == null) {
            throw new NullPointerException("packageVersions == null");
        }
        this.mPackageVersions = packageVersions;
    }

    byte[] toByteArray() {
        IOException e;
        Throwable th;
        Throwable th2 = null;
        ByteArrayOutputStream baos = new ByteArrayOutputStream(12);
        DataOutputStream dataOutputStream = null;
        try {
            DataOutputStream dos = new DataOutputStream(baos);
            try {
                dos.writeInt(this.mOptimisticLockId);
                dos.writeInt(this.mPackageVersions.mUpdateAppVersion);
                dos.writeInt(this.mPackageVersions.mDataAppVersion);
                if (dos != null) {
                    try {
                        dos.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 == null) {
                    return baos.toByteArray();
                }
                try {
                    throw th2;
                } catch (IOException e2) {
                    e = e2;
                    dataOutputStream = dos;
                }
            } catch (Throwable th4) {
                th = th4;
                dataOutputStream = dos;
                if (dataOutputStream != null) {
                    try {
                        dataOutputStream.close();
                    } catch (Throwable th5) {
                        if (th2 == null) {
                            th2 = th5;
                        } else if (th2 != th5) {
                            th2.addSuppressed(th5);
                        }
                    }
                }
                if (th2 == null) {
                    try {
                        throw th2;
                    } catch (IOException e3) {
                        e = e3;
                        throw new RuntimeException("Unable to write into a ByteArrayOutputStream", e);
                    }
                }
                throw th;
            }
        } catch (Throwable th6) {
            th = th6;
            if (dataOutputStream != null) {
                dataOutputStream.close();
            }
            if (th2 == null) {
                throw th;
            }
            throw th2;
        }
    }

    static CheckToken fromByteArray(byte[] tokenBytes) throws IOException {
        Throwable th;
        Throwable th2 = null;
        DataInputStream dataInputStream = null;
        try {
            DataInputStream dis = new DataInputStream(new ByteArrayInputStream(tokenBytes));
            try {
                CheckToken checkToken = new CheckToken(dis.readInt(), new PackageVersions(dis.readInt(), dis.readInt()));
                if (dis != null) {
                    try {
                        dis.close();
                    } catch (Throwable th3) {
                        th2 = th3;
                    }
                }
                if (th2 == null) {
                    return checkToken;
                }
                throw th2;
            } catch (Throwable th4) {
                th = th4;
                dataInputStream = dis;
                if (dataInputStream != null) {
                    try {
                        dataInputStream.close();
                    } catch (Throwable th5) {
                        if (th2 == null) {
                            th2 = th5;
                        } else if (th2 != th5) {
                            th2.addSuppressed(th5);
                        }
                    }
                }
                if (th2 == null) {
                    throw th2;
                }
                throw th;
            }
        } catch (Throwable th6) {
            th = th6;
            if (dataInputStream != null) {
                dataInputStream.close();
            }
            if (th2 == null) {
                throw th;
            }
            throw th2;
        }
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
        return (this.mOptimisticLockId * 31) + this.mPackageVersions.hashCode();
    }

    public String toString() {
        return "Token{mOptimisticLockId=" + this.mOptimisticLockId + ", mPackageVersions=" + this.mPackageVersions + '}';
    }
}
