package com.android.server;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.service.persistentdata.IPersistentDataBlockService.Stub;
import android.util.Slog;
import com.android.internal.annotations.GuardedBy;
import com.android.internal.util.Preconditions;
import com.android.server.pm.DumpState;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import libcore.io.IoUtils;

public class PersistentDataBlockService extends SystemService {
    public static final int DIGEST_SIZE_BYTES = 32;
    private static final String FLASH_LOCK_LOCKED = "1";
    private static final String FLASH_LOCK_PROP = "ro.boot.flash.locked";
    private static final String FLASH_LOCK_UNLOCKED = "0";
    private static final int FRP_CREDENTIAL_RESERVED_SIZE = 1000;
    private static final int HEADER_SIZE = 8;
    private static final int MAX_DATA_BLOCK_SIZE = 102400;
    private static final int MAX_FRP_CREDENTIAL_HANDLE_SIZE = 996;
    private static final String OEM_UNLOCK_PROP = "sys.oem_unlock_allowed";
    private static final int PARTITION_TYPE_MARKER = 428873843;
    private static final String PERSISTENT_DATA_BLOCK_PROP = "ro.frp.pst";
    private static final String TAG = PersistentDataBlockService.class.getSimpleName();
    private int mAllowedUid = -1;
    private long mBlockDeviceSize;
    private final Context mContext;
    private final String mDataBlockFile;
    private final CountDownLatch mInitDoneSignal = new CountDownLatch(1);
    private PersistentDataBlockManagerInternal mInternalService = new PersistentDataBlockManagerInternal() {
        public void setFrpCredentialHandle(byte[] handle) {
            boolean z = true;
            int i = 0;
            boolean z2 = handle == null || handle.length > 0;
            Preconditions.checkArgument(z2, "handle must be null or non-empty");
            if (handle != null && handle.length > PersistentDataBlockService.MAX_FRP_CREDENTIAL_HANDLE_SIZE) {
                z = false;
            }
            Preconditions.checkArgument(z, "handle must not be longer than 996");
            try {
                FileOutputStream outputStream = new FileOutputStream(new File(PersistentDataBlockService.this.mDataBlockFile));
                ByteBuffer data = ByteBuffer.allocate(1000);
                if (handle != null) {
                    i = handle.length;
                }
                data.putInt(i);
                if (handle != null) {
                    data.put(handle);
                }
                data.flip();
                synchronized (PersistentDataBlockService.this.mLock) {
                    if (PersistentDataBlockService.this.mIsWritable) {
                        try {
                            FileChannel channel = outputStream.getChannel();
                            channel.position((PersistentDataBlockService.this.getBlockDeviceSize() - 1) - 1000);
                            channel.write(data);
                            outputStream.flush();
                            IoUtils.closeQuietly(outputStream);
                            PersistentDataBlockService.this.computeAndWriteDigestLocked();
                            return;
                        } catch (IOException e) {
                            try {
                                Slog.e(PersistentDataBlockService.TAG, "unable to access persistent partition", e);
                            } finally {
                                IoUtils.closeQuietly(outputStream);
                            }
                            return;
                        }
                    }
                    IoUtils.closeQuietly(outputStream);
                }
            } catch (FileNotFoundException e2) {
                Slog.e(PersistentDataBlockService.TAG, "partition not available", e2);
            }
        }

        public byte[] getFrpCredentialHandle() {
            if (PersistentDataBlockService.this.enforceChecksumValidity()) {
                try {
                    DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                    try {
                        synchronized (PersistentDataBlockService.this.mLock) {
                            inputStream.skip((PersistentDataBlockService.this.getBlockDeviceSize() - 1) - 1000);
                            int length = inputStream.readInt();
                            if (length <= 0 || length > PersistentDataBlockService.MAX_FRP_CREDENTIAL_HANDLE_SIZE) {
                                IoUtils.closeQuietly(inputStream);
                                return null;
                            }
                            byte[] bytes = new byte[length];
                            inputStream.readFully(bytes);
                            IoUtils.closeQuietly(inputStream);
                            return bytes;
                        }
                    } catch (IOException e) {
                        try {
                            throw new IllegalStateException("frp handle not readable", e);
                        } catch (Throwable th) {
                            IoUtils.closeQuietly(inputStream);
                        }
                    }
                } catch (FileNotFoundException e2) {
                    throw new IllegalStateException("frp partition not available");
                }
            }
            throw new IllegalStateException("invalid checksum");
        }

        public void forceOemUnlockEnabled(boolean enabled) {
            synchronized (PersistentDataBlockService.this.mLock) {
                PersistentDataBlockService.this.doSetOemUnlockEnabledLocked(enabled);
                PersistentDataBlockService.this.computeAndWriteDigestLocked();
            }
        }
    };
    @GuardedBy("mLock")
    private boolean mIsWritable = true;
    private final Object mLock = new Object();
    private final IBinder mService = new Stub() {
        public int write(byte[] data) throws RemoteException {
            String access$000 = PersistentDataBlockService.TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("write data, callingUid=");
            stringBuilder.append(Binder.getCallingUid());
            stringBuilder.append(", callingPid=");
            stringBuilder.append(Binder.getCallingPid());
            Slog.i(access$000, stringBuilder.toString());
            PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
            long maxBlockSize = PersistentDataBlockService.this.doGetMaximumDataBlockSize();
            if (((long) data.length) > maxBlockSize) {
                return (int) (-maxBlockSize);
            }
            try {
                DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                ByteBuffer headerAndData = ByteBuffer.allocate(data.length + 8);
                headerAndData.putInt(PersistentDataBlockService.PARTITION_TYPE_MARKER);
                headerAndData.putInt(data.length);
                headerAndData.put(data);
                synchronized (PersistentDataBlockService.this.mLock) {
                    if (PersistentDataBlockService.this.mIsWritable) {
                        try {
                            outputStream.write(new byte[32], 0, 32);
                            outputStream.write(headerAndData.array());
                            outputStream.flush();
                            IoUtils.closeQuietly(outputStream);
                            if (PersistentDataBlockService.this.computeAndWriteDigestLocked()) {
                                int length = data.length;
                                return length;
                            }
                            return -1;
                        } catch (IOException e) {
                            try {
                                Slog.e(PersistentDataBlockService.TAG, "failed writing to the persistent data block", e);
                            } finally {
                                IoUtils.closeQuietly(outputStream);
                            }
                            return -1;
                        }
                    }
                    IoUtils.closeQuietly(outputStream);
                    return -1;
                }
            } catch (FileNotFoundException e2) {
                Slog.e(PersistentDataBlockService.TAG, "partition not available?", e2);
                return -1;
            }
        }

        /* JADX WARNING: Missing block: B:16:?, code:
            r2.close();
     */
        /* JADX WARNING: Missing block: B:18:0x0044, code:
            android.util.Slog.e(com.android.server.PersistentDataBlockService.access$000(), "failed to close OutputStream");
     */
        /* JADX WARNING: Missing block: B:26:?, code:
            r2.close();
     */
        /* JADX WARNING: Missing block: B:28:0x007c, code:
            android.util.Slog.e(com.android.server.PersistentDataBlockService.access$000(), "failed to close OutputStream");
     */
        /* JADX WARNING: Missing block: B:33:?, code:
            r2.close();
     */
        /* JADX WARNING: Missing block: B:35:0x008c, code:
            android.util.Slog.e(com.android.server.PersistentDataBlockService.access$000(), "failed to close OutputStream");
     */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public byte[] read() {
            byte[] bArr;
            byte[] bArr2;
            byte[] data;
            PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
            if (!PersistentDataBlockService.this.enforceChecksumValidity()) {
                return new byte[0];
            }
            bArr = null;
            try {
                DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                try {
                    synchronized (PersistentDataBlockService.this.mLock) {
                        int totalDataSize = PersistentDataBlockService.this.getTotalDataSizeLocked(inputStream);
                        if (totalDataSize == 0) {
                            bArr2 = new byte[0];
                        } else {
                            data = new byte[totalDataSize];
                            int read = inputStream.read(data, 0, totalDataSize);
                            if (read < totalDataSize) {
                                String access$000 = PersistentDataBlockService.TAG;
                                StringBuilder stringBuilder = new StringBuilder();
                                stringBuilder.append("failed to read entire data block. bytes read: ");
                                stringBuilder.append(read);
                                stringBuilder.append(SliceAuthority.DELIMITER);
                                stringBuilder.append(totalDataSize);
                                Slog.e(access$000, stringBuilder.toString());
                            }
                        }
                    }
                } catch (IOException e) {
                    try {
                        Slog.e(PersistentDataBlockService.TAG, "failed to read data", e);
                        return bArr;
                    } finally {
                        try {
                            inputStream.close();
                        } catch (IOException e2) {
                            Slog.e(PersistentDataBlockService.TAG, "failed to close OutputStream");
                        }
                    }
                }
            } catch (FileNotFoundException e3) {
                Slog.e(PersistentDataBlockService.TAG, "partition not available?", e3);
                return bArr;
            }
            return bArr;
            return data;
            return bArr2;
        }

        public void wipe() {
            PersistentDataBlockService.this.enforceOemUnlockWritePermission();
            synchronized (PersistentDataBlockService.this.mLock) {
                if (PersistentDataBlockService.this.nativeWipe(PersistentDataBlockService.this.mDataBlockFile) < 0) {
                    Slog.e(PersistentDataBlockService.TAG, "failed to wipe persistent partition");
                } else {
                    PersistentDataBlockService.this.mIsWritable = false;
                    Slog.i(PersistentDataBlockService.TAG, "persistent partition now wiped and unwritable");
                }
            }
        }

        public void setOemUnlockEnabled(boolean enabled) throws SecurityException {
            if (!ActivityManager.isUserAMonkey()) {
                PersistentDataBlockService.this.enforceOemUnlockWritePermission();
                PersistentDataBlockService.this.enforceIsAdmin();
                if (enabled) {
                    PersistentDataBlockService.this.enforceUserRestriction("no_oem_unlock");
                    PersistentDataBlockService.this.enforceUserRestriction("no_factory_reset");
                }
                synchronized (PersistentDataBlockService.this.mLock) {
                    PersistentDataBlockService.this.doSetOemUnlockEnabledLocked(enabled);
                    PersistentDataBlockService.this.computeAndWriteDigestLocked();
                }
            }
        }

        public boolean getOemUnlockEnabled() {
            PersistentDataBlockService.this.enforceOemUnlockReadPermission();
            return PersistentDataBlockService.this.doGetOemUnlockEnabled();
        }

        /* Code decompiled incorrectly, please refer to instructions dump. */
        public int getFlashLockState() {
            int i;
            PersistentDataBlockService.this.enforceOemUnlockReadPermission();
            String locked = SystemProperties.get(PersistentDataBlockService.FLASH_LOCK_PROP);
            switch (locked.hashCode()) {
                case 48:
                    if (locked.equals(PersistentDataBlockService.FLASH_LOCK_UNLOCKED)) {
                        i = 1;
                        break;
                    }
                case 49:
                    if (locked.equals(PersistentDataBlockService.FLASH_LOCK_LOCKED)) {
                        i = 0;
                        break;
                    }
                default:
                    i = -1;
                    break;
            }
            switch (i) {
                case 0:
                    return 1;
                case 1:
                    return 0;
                default:
                    return -1;
            }
        }

        public int getDataBlockSize() {
            enforcePersistentDataBlockAccess();
            try {
                DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(PersistentDataBlockService.this.mDataBlockFile)));
                try {
                    int access$800;
                    synchronized (PersistentDataBlockService.this.mLock) {
                        access$800 = PersistentDataBlockService.this.getTotalDataSizeLocked(inputStream);
                    }
                    IoUtils.closeQuietly(inputStream);
                    return access$800;
                } catch (IOException e) {
                    try {
                        Slog.e(PersistentDataBlockService.TAG, "error reading data block size");
                        return 0;
                    } finally {
                        IoUtils.closeQuietly(inputStream);
                    }
                }
            } catch (FileNotFoundException e2) {
                Slog.e(PersistentDataBlockService.TAG, "partition not available");
                return 0;
            }
        }

        private void enforcePersistentDataBlockAccess() {
            if (PersistentDataBlockService.this.mContext.checkCallingPermission("android.permission.ACCESS_PDB_STATE") != 0) {
                PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
            }
        }

        public long getMaximumDataBlockSize() {
            PersistentDataBlockService.this.enforceUid(Binder.getCallingUid());
            return PersistentDataBlockService.this.doGetMaximumDataBlockSize();
        }

        public boolean hasFrpCredentialHandle() {
            enforcePersistentDataBlockAccess();
            try {
                return PersistentDataBlockService.this.mInternalService.getFrpCredentialHandle() != null;
            } catch (IllegalStateException e) {
                Slog.e(PersistentDataBlockService.TAG, "error reading frp handle", e);
                throw new UnsupportedOperationException("cannot read frp credential");
            }
        }
    };

    private native long nativeGetBlockDeviceSize(String str);

    private native int nativeWipe(String str);

    public PersistentDataBlockService(Context context) {
        super(context);
        this.mContext = context;
        this.mDataBlockFile = SystemProperties.get(PERSISTENT_DATA_BLOCK_PROP);
        this.mBlockDeviceSize = -1;
    }

    private int getAllowedUid(int userHandle) {
        String allowedPackage = this.mContext.getResources().getString(17039837);
        try {
            return this.mContext.getPackageManager().getPackageUidAsUser(allowedPackage, DumpState.DUMP_DEXOPT, userHandle);
        } catch (NameNotFoundException e) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("not able to find package ");
            stringBuilder.append(allowedPackage);
            Slog.e(str, stringBuilder.toString(), e);
            return -1;
        }
    }

    public void onStart() {
        SystemServerInitThreadPool systemServerInitThreadPool = SystemServerInitThreadPool.get();
        Runnable -__lambda_persistentdatablockservice_ezl9oyat2enl7kfsr2nkubjxidk = new -$$Lambda$PersistentDataBlockService$EZl9OYaT2eNL7kfSr2nKUBjxidk(this);
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(TAG);
        stringBuilder.append(".onStart");
        systemServerInitThreadPool.submit(-__lambda_persistentdatablockservice_ezl9oyat2enl7kfsr2nkubjxidk, stringBuilder.toString());
    }

    public static /* synthetic */ void lambda$onStart$0(PersistentDataBlockService persistentDataBlockService) {
        persistentDataBlockService.mAllowedUid = persistentDataBlockService.getAllowedUid(0);
        persistentDataBlockService.enforceChecksumValidity();
        persistentDataBlockService.formatIfOemUnlockEnabled();
        persistentDataBlockService.publishBinderService("persistent_data_block", persistentDataBlockService.mService);
        persistentDataBlockService.mInitDoneSignal.countDown();
    }

    public void onBootPhase(int phase) {
        if (phase == 500) {
            try {
                if (this.mInitDoneSignal.await(10, TimeUnit.SECONDS)) {
                    LocalServices.addService(PersistentDataBlockManagerInternal.class, this.mInternalService);
                } else {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("Service ");
                    stringBuilder.append(TAG);
                    stringBuilder.append(" init timeout");
                    throw new IllegalStateException(stringBuilder.toString());
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                StringBuilder stringBuilder2 = new StringBuilder();
                stringBuilder2.append("Service ");
                stringBuilder2.append(TAG);
                stringBuilder2.append(" init interrupted");
                throw new IllegalStateException(stringBuilder2.toString(), e);
            }
        }
        super.onBootPhase(phase);
    }

    private void formatIfOemUnlockEnabled() {
        boolean enabled = doGetOemUnlockEnabled();
        if (enabled) {
            synchronized (this.mLock) {
                formatPartitionLocked(true);
            }
        }
        SystemProperties.set(OEM_UNLOCK_PROP, enabled ? FLASH_LOCK_LOCKED : FLASH_LOCK_UNLOCKED);
    }

    private void enforceOemUnlockReadPermission() {
        if (this.mContext.checkCallingOrSelfPermission("android.permission.READ_OEM_UNLOCK_STATE") == -1 && this.mContext.checkCallingOrSelfPermission("android.permission.OEM_UNLOCK_STATE") == -1) {
            throw new SecurityException("Can't access OEM unlock state. Requires READ_OEM_UNLOCK_STATE or OEM_UNLOCK_STATE permission.");
        }
    }

    private void enforceOemUnlockWritePermission() {
        this.mContext.enforceCallingOrSelfPermission("android.permission.OEM_UNLOCK_STATE", "Can't modify OEM unlock state");
    }

    private void enforceUid(int callingUid) {
        if (callingUid != this.mAllowedUid) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("uid ");
            stringBuilder.append(callingUid);
            stringBuilder.append(" not allowed to access PST");
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private void enforceIsAdmin() {
        if (!UserManager.get(this.mContext).isUserAdmin(UserHandle.getCallingUserId())) {
            throw new SecurityException("Only the Admin user is allowed to change OEM unlock state");
        }
    }

    private void enforceUserRestriction(String userRestriction) {
        if (UserManager.get(this.mContext).hasUserRestriction(userRestriction)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("OEM unlock is disallowed by user restriction: ");
            stringBuilder.append(userRestriction);
            throw new SecurityException(stringBuilder.toString());
        }
    }

    private int getTotalDataSizeLocked(DataInputStream inputStream) throws IOException {
        inputStream.skipBytes(32);
        if (inputStream.readInt() == PARTITION_TYPE_MARKER) {
            return inputStream.readInt();
        }
        return 0;
    }

    private long getBlockDeviceSize() {
        synchronized (this.mLock) {
            if (this.mBlockDeviceSize == -1) {
                this.mBlockDeviceSize = nativeGetBlockDeviceSize(this.mDataBlockFile);
            }
        }
        return this.mBlockDeviceSize;
    }

    private boolean enforceChecksumValidity() {
        byte[] storedDigest = new byte[32];
        synchronized (this.mLock) {
            byte[] digest = computeDigestLocked(storedDigest);
            if (digest == null || !Arrays.equals(storedDigest, digest)) {
                Slog.i(TAG, "Formatting FRP partition...");
                formatPartitionLocked(false);
                return false;
            }
            return true;
        }
    }

    private boolean computeAndWriteDigestLocked() {
        byte[] digest = computeDigestLocked(null);
        if (digest == null) {
            return false;
        }
        try {
            DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(new File(this.mDataBlockFile)));
            try {
                outputStream.write(digest, 0, 32);
                outputStream.flush();
                return true;
            } catch (IOException e) {
                Slog.e(TAG, "failed to write block checksum", e);
                return false;
            } finally {
                IoUtils.closeQuietly(outputStream);
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "partition not available?", e2);
            return false;
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:31:0x0042 A:{SYNTHETIC} */
    /* JADX WARNING: Removed duplicated region for block: B:17:0x003e A:{Catch:{ IOException -> 0x0029, all -> 0x0027 }} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private byte[] computeDigestLocked(byte[] storedDigest) {
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(this.mDataBlockFile)));
            try {
                byte[] data;
                IOException e;
                int read;
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                if (storedDigest != null) {
                    try {
                        if (storedDigest.length == 32) {
                            inputStream.read(storedDigest);
                            data = new byte[1024];
                            md.update(data, 0, 32);
                            while (true) {
                                e = inputStream.read(data);
                                read = e;
                                if (e == -1) {
                                    md.update(data, 0, read);
                                } else {
                                    IoUtils.closeQuietly(inputStream);
                                    return md.digest();
                                }
                            }
                        }
                    } catch (IOException e2) {
                        Slog.e(TAG, "failed to read partition", e2);
                        return null;
                    } finally {
                        IoUtils.closeQuietly(inputStream);
                    }
                }
                inputStream.skipBytes(32);
                data = new byte[1024];
                md.update(data, 0, 32);
                while (true) {
                    e2 = inputStream.read(data);
                    read = e2;
                    if (e2 == -1) {
                    }
                }
            } catch (NoSuchAlgorithmException e3) {
                Slog.e(TAG, "SHA-256 not supported?", e3);
                IoUtils.closeQuietly(inputStream);
                return null;
            }
        } catch (FileNotFoundException e4) {
            Slog.e(TAG, "partition not available?", e4);
            return null;
        }
    }

    private void formatPartitionLocked(boolean setOemUnlockEnabled) {
        try {
            DataOutputStream outputStream = new DataOutputStream(new FileOutputStream(new File(this.mDataBlockFile)));
            try {
                outputStream.write(new byte[32], 0, 32);
                outputStream.writeInt(PARTITION_TYPE_MARKER);
                outputStream.writeInt(0);
                outputStream.flush();
                doSetOemUnlockEnabledLocked(setOemUnlockEnabled);
                computeAndWriteDigestLocked();
            } catch (IOException e) {
                Slog.e(TAG, "failed to format block", e);
            } finally {
                IoUtils.closeQuietly(outputStream);
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "partition not available?", e2);
        }
    }

    private void doSetOemUnlockEnabledLocked(boolean enabled) {
        try {
            FileOutputStream outputStream = new FileOutputStream(new File(this.mDataBlockFile));
            try {
                FileChannel channel = outputStream.getChannel();
                channel.position(getBlockDeviceSize() - 1);
                ByteBuffer data = ByteBuffer.allocate(1);
                data.put(enabled);
                data.flip();
                channel.write(data);
                outputStream.flush();
                SystemProperties.set(OEM_UNLOCK_PROP, enabled ? FLASH_LOCK_LOCKED : FLASH_LOCK_UNLOCKED);
                IoUtils.closeQuietly(outputStream);
            } catch (IOException e) {
                Slog.e(TAG, "unable to access persistent partition", e);
                SystemProperties.set(OEM_UNLOCK_PROP, enabled ? FLASH_LOCK_LOCKED : FLASH_LOCK_UNLOCKED);
                IoUtils.closeQuietly(outputStream);
            } catch (Throwable th) {
                SystemProperties.set(OEM_UNLOCK_PROP, enabled ? FLASH_LOCK_LOCKED : FLASH_LOCK_UNLOCKED);
                IoUtils.closeQuietly(outputStream);
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "partition not available", e2);
        }
    }

    private boolean doGetOemUnlockEnabled() {
        try {
            DataInputStream inputStream = new DataInputStream(new FileInputStream(new File(this.mDataBlockFile)));
            try {
                boolean z;
                synchronized (this.mLock) {
                    inputStream.skip(getBlockDeviceSize() - 1);
                    z = inputStream.readByte() != (byte) 0;
                }
                IoUtils.closeQuietly(inputStream);
                return z;
            } catch (IOException e) {
                try {
                    Slog.e(TAG, "unable to access persistent partition", e);
                    return false;
                } finally {
                    IoUtils.closeQuietly(inputStream);
                }
            }
        } catch (FileNotFoundException e2) {
            Slog.e(TAG, "partition not available");
            return false;
        }
    }

    private long doGetMaximumDataBlockSize() {
        long actualSize = (((getBlockDeviceSize() - 8) - 32) - 1000) - 1;
        return actualSize <= 102400 ? actualSize : 102400;
    }
}
