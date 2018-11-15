package com.android.server.security;

import android.os.SharedMemory;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import android.util.Pair;
import android.util.apk.ApkSignatureVerifier;
import android.util.apk.ByteBufferFactory;
import android.util.apk.SignatureNotFoundException;
import java.io.FileDescriptor;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.DigestException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public abstract class VerityUtils {
    private static final boolean DEBUG = false;
    private static final String TAG = "VerityUtils";

    public static class SetupResult {
        private static final int RESULT_FAILED = 3;
        private static final int RESULT_OK = 1;
        private static final int RESULT_SKIPPED = 2;
        private final int mCode;
        private final int mContentSize;
        private final FileDescriptor mFileDescriptor;

        public static SetupResult ok(FileDescriptor fileDescriptor, int contentSize) {
            return new SetupResult(1, fileDescriptor, contentSize);
        }

        public static SetupResult skipped() {
            return new SetupResult(2, null, -1);
        }

        public static SetupResult failed() {
            return new SetupResult(3, null, -1);
        }

        private SetupResult(int code, FileDescriptor fileDescriptor, int contentSize) {
            this.mCode = code;
            this.mFileDescriptor = fileDescriptor;
            this.mContentSize = contentSize;
        }

        public boolean isFailed() {
            return this.mCode == 3;
        }

        public boolean isOk() {
            return this.mCode == 1;
        }

        public FileDescriptor getUnownedFileDescriptor() {
            return this.mFileDescriptor;
        }

        public int getContentSize() {
            return this.mContentSize;
        }
    }

    private static class TrackedShmBufferFactory implements ByteBufferFactory {
        private ByteBuffer mBuffer;
        private SharedMemory mShm;

        private TrackedShmBufferFactory() {
        }

        public ByteBuffer create(int capacity) throws SecurityException {
            try {
                if (this.mBuffer == null) {
                    this.mShm = SharedMemory.create("apkverity", capacity);
                    if (this.mShm.setProtect(OsConstants.PROT_READ | OsConstants.PROT_WRITE)) {
                        this.mBuffer = this.mShm.mapReadWrite();
                        return this.mBuffer;
                    }
                    throw new SecurityException("Failed to set protection");
                }
                throw new IllegalStateException("Multiple instantiation from this factory");
            } catch (ErrnoException e) {
                throw new SecurityException("Failed to set protection", e);
            }
        }

        public SharedMemory releaseSharedMemory() {
            if (this.mBuffer != null) {
                SharedMemory.unmap(this.mBuffer);
                this.mBuffer = null;
            }
            SharedMemory tmp = this.mShm;
            this.mShm = null;
            return tmp;
        }

        public int getBufferLimit() {
            return this.mBuffer == null ? -1 : this.mBuffer.limit();
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:23:0x0049 A:{PHI: r0 , Splitter: B:1:0x0001, ExcHandler: java.io.IOException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0049 A:{PHI: r0 , Splitter: B:1:0x0001, ExcHandler: java.io.IOException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0049 A:{PHI: r0 , Splitter: B:1:0x0001, ExcHandler: java.io.IOException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0049 A:{PHI: r0 , Splitter: B:1:0x0001, ExcHandler: java.io.IOException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Removed duplicated region for block: B:23:0x0049 A:{PHI: r0 , Splitter: B:1:0x0001, ExcHandler: java.io.IOException (r1_1 'e' java.lang.Exception)} */
    /* JADX WARNING: Missing block: B:23:0x0049, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:25:?, code:
            android.util.Slog.e(TAG, "Failed to set up apk verity: ", r1);
            r2 = com.android.server.security.VerityUtils.SetupResult.failed();
     */
    /* JADX WARNING: Missing block: B:26:0x0055, code:
            if (r0 != null) goto L_0x0057;
     */
    /* JADX WARNING: Missing block: B:27:0x0057, code:
            r0.close();
     */
    /* JADX WARNING: Missing block: B:28:0x005a, code:
            return r2;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static SetupResult generateApkVeritySetupData(String apkPath) {
        SharedMemory shm = null;
        try {
            byte[] signedRootHash = ApkSignatureVerifier.getVerityRootHash(apkPath);
            if (signedRootHash == null) {
                SetupResult skipped = SetupResult.skipped();
                if (shm != null) {
                    shm.close();
                }
                return skipped;
            }
            Pair<SharedMemory, Integer> result = generateApkVerityIntoSharedMemory(apkPath, signedRootHash);
            shm = (SharedMemory) result.first;
            int contentSize = ((Integer) result.second).intValue();
            FileDescriptor rfd = shm.getFileDescriptor();
            SetupResult failed;
            if (rfd == null || !rfd.valid()) {
                failed = SetupResult.failed();
                if (shm != null) {
                    shm.close();
                }
                return failed;
            }
            failed = SetupResult.ok(Os.dup(rfd), contentSize);
            if (shm != null) {
                shm.close();
            }
            return failed;
        } catch (Exception e) {
        } catch (Throwable th) {
            if (shm != null) {
                shm.close();
            }
            throw th;
        }
    }

    public static byte[] generateFsverityRootHash(String apkPath) throws NoSuchAlgorithmException, DigestException, IOException {
        return ApkSignatureVerifier.generateFsverityRootHash(apkPath);
    }

    public static byte[] getVerityRootHash(String apkPath) throws IOException, SignatureNotFoundException, SecurityException {
        return ApkSignatureVerifier.getVerityRootHash(apkPath);
    }

    private static Pair<SharedMemory, Integer> generateApkVerityIntoSharedMemory(String apkPath, byte[] expectedRootHash) throws IOException, SecurityException, DigestException, NoSuchAlgorithmException, SignatureNotFoundException {
        TrackedShmBufferFactory shmBufferFactory = new TrackedShmBufferFactory();
        if (Arrays.equals(expectedRootHash, ApkSignatureVerifier.generateApkVerity(apkPath, shmBufferFactory))) {
            int contentSize = shmBufferFactory.getBufferLimit();
            SharedMemory shm = shmBufferFactory.releaseSharedMemory();
            if (shm == null) {
                throw new IllegalStateException("Failed to generate verity tree into shared memory");
            } else if (shm.setProtect(OsConstants.PROT_READ)) {
                return Pair.create(shm, Integer.valueOf(contentSize));
            } else {
                throw new SecurityException("Failed to set up shared memory correctly");
            }
        }
        throw new SecurityException("Locally generated verity root hash does not match");
    }
}
