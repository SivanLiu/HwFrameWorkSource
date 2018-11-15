package android.os;

import java.io.FileDescriptor;

public interface IInstalld extends IInterface {

    public static abstract class Stub extends Binder implements IInstalld {
        private static final String DESCRIPTOR = "android.os.IInstalld";
        static final int TRANSACTION_assertFsverityRootHashMatches = 34;
        static final int TRANSACTION_clearAppData = 6;
        static final int TRANSACTION_clearAppProfiles = 19;
        static final int TRANSACTION_copySystemProfile = 18;
        static final int TRANSACTION_createAppData = 3;
        static final int TRANSACTION_createOatDir = 29;
        static final int TRANSACTION_createProfileSnapshot = 21;
        static final int TRANSACTION_createUserData = 1;
        static final int TRANSACTION_deleteOdex = 32;
        static final int TRANSACTION_destroyAppData = 7;
        static final int TRANSACTION_destroyAppProfiles = 20;
        static final int TRANSACTION_destroyProfileSnapshot = 22;
        static final int TRANSACTION_destroyUserData = 2;
        static final int TRANSACTION_dexopt = 14;
        static final int TRANSACTION_dumpProfiles = 17;
        static final int TRANSACTION_executeBackupTask = 42;
        static final int TRANSACTION_finishBackupSession = 43;
        static final int TRANSACTION_fixupAppData = 8;
        static final int TRANSACTION_freeCache = 27;
        static final int TRANSACTION_getAppSize = 9;
        static final int TRANSACTION_getExternalSize = 11;
        static final int TRANSACTION_getUserSize = 10;
        static final int TRANSACTION_hashSecondaryDexFile = 36;
        static final int TRANSACTION_idmap = 23;
        static final int TRANSACTION_installApkVerity = 33;
        static final int TRANSACTION_invalidateMounts = 37;
        static final int TRANSACTION_isQuotaSupported = 38;
        static final int TRANSACTION_linkFile = 30;
        static final int TRANSACTION_linkNativeLibraryDirectory = 28;
        static final int TRANSACTION_markBootComplete = 26;
        static final int TRANSACTION_mergeProfiles = 16;
        static final int TRANSACTION_migrateAppData = 5;
        static final int TRANSACTION_moveAb = 31;
        static final int TRANSACTION_moveCompleteApp = 13;
        static final int TRANSACTION_prepareAppProfile = 39;
        static final int TRANSACTION_reconcileSecondaryDexFile = 35;
        static final int TRANSACTION_removeIdmap = 24;
        static final int TRANSACTION_restoreCloneAppData = 40;
        static final int TRANSACTION_restoreconAppData = 4;
        static final int TRANSACTION_rmPackageDir = 25;
        static final int TRANSACTION_rmdex = 15;
        static final int TRANSACTION_setAppQuota = 12;
        static final int TRANSACTION_setFileXattr = 44;
        static final int TRANSACTION_startBackupSession = 41;

        private static class Proxy implements IInstalld {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public void createUserData(String uuid, int userId, int userSerial, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeInt(userId);
                    _data.writeInt(userSerial);
                    _data.writeInt(flags);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void destroyUserData(String uuid, int userId, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long createAppData(String uuid, String packageName, int userId, int flags, int appId, String seInfo, int targetSdkVersion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    _data.writeInt(appId);
                    _data.writeString(seInfo);
                    _data.writeInt(targetSdkVersion);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                    long _result = _reply.readLong();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void restoreconAppData(String uuid, String packageName, int userId, int flags, int appId, String seInfo) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    _data.writeInt(appId);
                    _data.writeString(seInfo);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void migrateAppData(String uuid, String packageName, int userId, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearAppData(String uuid, String packageName, int userId, int flags, long ceDataInode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    _data.writeLong(ceDataInode);
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void destroyAppData(String uuid, String packageName, int userId, int flags, long ceDataInode) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    _data.writeLong(ceDataInode);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void fixupAppData(String uuid, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeInt(flags);
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long[] getAppSize(String uuid, String[] packageNames, int userId, int flags, int appId, long[] ceDataInodes, String[] codePaths) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeStringArray(packageNames);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    _data.writeInt(appId);
                    _data.writeLongArray(ceDataInodes);
                    _data.writeStringArray(codePaths);
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    long[] _result = _reply.createLongArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long[] getUserSize(String uuid, int userId, int flags, int[] appIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    _data.writeIntArray(appIds);
                    this.mRemote.transact(10, _data, _reply, 0);
                    _reply.readException();
                    long[] _result = _reply.createLongArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public long[] getExternalSize(String uuid, int userId, int flags, int[] appIds) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    _data.writeIntArray(appIds);
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    long[] _result = _reply.createLongArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setAppQuota(String uuid, int userId, int appId, long cacheQuota) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeInt(userId);
                    _data.writeInt(appId);
                    _data.writeLong(cacheQuota);
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void moveCompleteApp(String fromUuid, String toUuid, String packageName, String dataAppName, int appId, String seInfo, int targetSdkVersion) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(fromUuid);
                    _data.writeString(toUuid);
                    _data.writeString(packageName);
                    _data.writeString(dataAppName);
                    _data.writeInt(appId);
                    _data.writeString(seInfo);
                    _data.writeInt(targetSdkVersion);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void dexopt(String apkPath, int uid, String packageName, String instructionSet, int dexoptNeeded, String outputPath, int dexFlags, String compilerFilter, String uuid, String sharedLibraries, String seInfo, boolean downgrade, int targetSdkVersion, String profileName, String dexMetadataPath, String compilationReason) throws RemoteException {
                Throwable th;
                int i;
                String str;
                int i2;
                String str2;
                String str3;
                String str4;
                String str5;
                boolean z;
                int i3;
                String str6;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(apkPath);
                    _data.writeInt(uid);
                    try {
                        _data.writeString(packageName);
                        try {
                            _data.writeString(instructionSet);
                        } catch (Throwable th2) {
                            th = th2;
                            i = dexoptNeeded;
                            str = outputPath;
                            i2 = dexFlags;
                            str2 = compilerFilter;
                            str3 = uuid;
                            str4 = sharedLibraries;
                            str5 = seInfo;
                            z = downgrade;
                            i3 = targetSdkVersion;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th3) {
                        th = th3;
                        str6 = instructionSet;
                        i = dexoptNeeded;
                        str = outputPath;
                        i2 = dexFlags;
                        str2 = compilerFilter;
                        str3 = uuid;
                        str4 = sharedLibraries;
                        str5 = seInfo;
                        z = downgrade;
                        i3 = targetSdkVersion;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeInt(dexoptNeeded);
                        try {
                            _data.writeString(outputPath);
                            try {
                                _data.writeInt(dexFlags);
                            } catch (Throwable th4) {
                                th = th4;
                                str2 = compilerFilter;
                                str3 = uuid;
                                str4 = sharedLibraries;
                                str5 = seInfo;
                                z = downgrade;
                                i3 = targetSdkVersion;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th5) {
                            th = th5;
                            i2 = dexFlags;
                            str2 = compilerFilter;
                            str3 = uuid;
                            str4 = sharedLibraries;
                            str5 = seInfo;
                            z = downgrade;
                            i3 = targetSdkVersion;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th6) {
                        th = th6;
                        str = outputPath;
                        i2 = dexFlags;
                        str2 = compilerFilter;
                        str3 = uuid;
                        str4 = sharedLibraries;
                        str5 = seInfo;
                        z = downgrade;
                        i3 = targetSdkVersion;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeString(compilerFilter);
                        try {
                            _data.writeString(uuid);
                            try {
                                _data.writeString(sharedLibraries);
                            } catch (Throwable th7) {
                                th = th7;
                                str5 = seInfo;
                                z = downgrade;
                                i3 = targetSdkVersion;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th8) {
                            th = th8;
                            str4 = sharedLibraries;
                            str5 = seInfo;
                            z = downgrade;
                            i3 = targetSdkVersion;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th9) {
                        th = th9;
                        str3 = uuid;
                        str4 = sharedLibraries;
                        str5 = seInfo;
                        z = downgrade;
                        i3 = targetSdkVersion;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeString(seInfo);
                        try {
                            _data.writeInt(downgrade);
                            try {
                                _data.writeInt(targetSdkVersion);
                                _data.writeString(profileName);
                                _data.writeString(dexMetadataPath);
                                _data.writeString(compilationReason);
                                this.mRemote.transact(14, _data, _reply, 0);
                                _reply.readException();
                                _reply.recycle();
                                _data.recycle();
                            } catch (Throwable th10) {
                                th = th10;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th11) {
                            th = th11;
                            i3 = targetSdkVersion;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th12) {
                        th = th12;
                        z = downgrade;
                        i3 = targetSdkVersion;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th13) {
                    th = th13;
                    String str7 = packageName;
                    str6 = instructionSet;
                    i = dexoptNeeded;
                    str = outputPath;
                    i2 = dexFlags;
                    str2 = compilerFilter;
                    str3 = uuid;
                    str4 = sharedLibraries;
                    str5 = seInfo;
                    z = downgrade;
                    i3 = targetSdkVersion;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public void rmdex(String codePath, String instructionSet) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(codePath);
                    _data.writeString(instructionSet);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean mergeProfiles(int uid, String packageName, String profileName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeString(packageName);
                    _data.writeString(profileName);
                    boolean z = false;
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean dumpProfiles(int uid, String packageName, String profileName, String codePath) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(uid);
                    _data.writeString(packageName);
                    _data.writeString(profileName);
                    _data.writeString(codePath);
                    boolean z = false;
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean copySystemProfile(String systemProfile, int uid, String packageName, String profileName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(systemProfile);
                    _data.writeInt(uid);
                    _data.writeString(packageName);
                    _data.writeString(profileName);
                    boolean z = false;
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearAppProfiles(String packageName, String profileName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(profileName);
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void destroyAppProfiles(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean createProfileSnapshot(int appId, String packageName, String profileName, String classpath) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(appId);
                    _data.writeString(packageName);
                    _data.writeString(profileName);
                    _data.writeString(classpath);
                    boolean z = false;
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void destroyProfileSnapshot(String packageName, String profileName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeString(profileName);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void idmap(String targetApkPath, String overlayApkPath, int uid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(targetApkPath);
                    _data.writeString(overlayApkPath);
                    _data.writeInt(uid);
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeIdmap(String overlayApkPath) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(overlayApkPath);
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void rmPackageDir(String packageDir) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageDir);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void markBootComplete(String instructionSet) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(instructionSet);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void freeCache(String uuid, long targetFreeBytes, long cacheReservedBytes, int flags) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeLong(targetFreeBytes);
                    _data.writeLong(cacheReservedBytes);
                    _data.writeInt(flags);
                    this.mRemote.transact(Stub.TRANSACTION_freeCache, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void linkNativeLibraryDirectory(String uuid, String packageName, String nativeLibPath32, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeString(packageName);
                    _data.writeString(nativeLibPath32);
                    _data.writeInt(userId);
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void createOatDir(String oatDir, String instructionSet) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(oatDir);
                    _data.writeString(instructionSet);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void linkFile(String relativePath, String fromBase, String toBase) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(relativePath);
                    _data.writeString(fromBase);
                    _data.writeString(toBase);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void moveAb(String apkPath, String instructionSet, String outputPath) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(apkPath);
                    _data.writeString(instructionSet);
                    _data.writeString(outputPath);
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void deleteOdex(String apkPath, String instructionSet, String outputPath) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(apkPath);
                    _data.writeString(instructionSet);
                    _data.writeString(outputPath);
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void installApkVerity(String filePath, FileDescriptor verityInput, int contentSize) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(filePath);
                    _data.writeRawFileDescriptor(verityInput);
                    _data.writeInt(contentSize);
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void assertFsverityRootHashMatches(String filePath, byte[] expectedHash) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(filePath);
                    _data.writeByteArray(expectedHash);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean reconcileSecondaryDexFile(String dexPath, String pkgName, int uid, String[] isas, String volume_uuid, int storage_flag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(dexPath);
                    _data.writeString(pkgName);
                    _data.writeInt(uid);
                    _data.writeStringArray(isas);
                    _data.writeString(volume_uuid);
                    _data.writeInt(storage_flag);
                    boolean z = false;
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public byte[] hashSecondaryDexFile(String dexPath, String pkgName, int uid, String volumeUuid, int storageFlag) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(dexPath);
                    _data.writeString(pkgName);
                    _data.writeInt(uid);
                    _data.writeString(volumeUuid);
                    _data.writeInt(storageFlag);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                    byte[] _result = _reply.createByteArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void invalidateMounts() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isQuotaSupported(String uuid) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    boolean z = false;
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean prepareAppProfile(String packageName, int userId, int appId, String profileName, String codePath, String dexMetadata) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeInt(appId);
                    _data.writeString(profileName);
                    _data.writeString(codePath);
                    _data.writeString(dexMetadata);
                    boolean z = false;
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void restoreCloneAppData(String uuid, String packageName, int userId, int flags, int appId, String seInfo, String parentDataUserCePkgDir, String cloneDataUserCePkgDir, String parentDataUserDePkgDir, String cloneDataUserDePkgDir) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(uuid);
                    _data.writeString(packageName);
                    _data.writeInt(userId);
                    _data.writeInt(flags);
                    _data.writeInt(appId);
                    _data.writeString(seInfo);
                    _data.writeString(parentDataUserCePkgDir);
                    _data.writeString(cloneDataUserCePkgDir);
                    _data.writeString(parentDataUserDePkgDir);
                    _data.writeString(cloneDataUserDePkgDir);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int startBackupSession(IBackupSessionCallback callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int executeBackupTask(int sessionId, String taskCmd) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    _data.writeString(taskCmd);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int finishBackupSession(int sessionId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(sessionId);
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setFileXattr(String path, String keyDesc, int storageType, int fileType) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(path);
                    _data.writeString(keyDesc);
                    _data.writeInt(storageType);
                    _data.writeInt(fileType);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static IInstalld asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof IInstalld)) {
                return new Proxy(obj);
            }
            return (IInstalld) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            if (i != 1598968902) {
                long[] _result;
                boolean _result2;
                boolean _result3;
                int _result4;
                switch (i) {
                    case 1:
                        parcel.enforceInterface(descriptor);
                        createUserData(data.readString(), data.readInt(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 2:
                        parcel.enforceInterface(descriptor);
                        destroyUserData(data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 3:
                        parcel.enforceInterface(descriptor);
                        long _result5 = createAppData(data.readString(), data.readString(), data.readInt(), data.readInt(), data.readInt(), data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeLong(_result5);
                        return true;
                    case 4:
                        parcel.enforceInterface(descriptor);
                        restoreconAppData(data.readString(), data.readString(), data.readInt(), data.readInt(), data.readInt(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 5:
                        parcel.enforceInterface(descriptor);
                        migrateAppData(data.readString(), data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 6:
                        parcel.enforceInterface(descriptor);
                        clearAppData(data.readString(), data.readString(), data.readInt(), data.readInt(), data.readLong());
                        reply.writeNoException();
                        return true;
                    case 7:
                        parcel.enforceInterface(descriptor);
                        destroyAppData(data.readString(), data.readString(), data.readInt(), data.readInt(), data.readLong());
                        reply.writeNoException();
                        return true;
                    case 8:
                        parcel.enforceInterface(descriptor);
                        fixupAppData(data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 9:
                        parcel.enforceInterface(descriptor);
                        long[] _result6 = getAppSize(data.readString(), data.createStringArray(), data.readInt(), data.readInt(), data.readInt(), data.createLongArray(), data.createStringArray());
                        reply.writeNoException();
                        parcel2.writeLongArray(_result6);
                        return true;
                    case 10:
                        parcel.enforceInterface(descriptor);
                        _result = getUserSize(data.readString(), data.readInt(), data.readInt(), data.createIntArray());
                        reply.writeNoException();
                        parcel2.writeLongArray(_result);
                        return true;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _result = getExternalSize(data.readString(), data.readInt(), data.readInt(), data.createIntArray());
                        reply.writeNoException();
                        reply.writeLongArray(_result);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        setAppQuota(data.readString(), data.readInt(), data.readInt(), data.readLong());
                        reply.writeNoException();
                        return true;
                    case 13:
                        data.enforceInterface(descriptor);
                        moveCompleteApp(data.readString(), data.readString(), data.readString(), data.readString(), data.readInt(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        dexopt(data.readString(), data.readInt(), data.readString(), data.readString(), data.readInt(), data.readString(), data.readInt(), data.readString(), data.readString(), data.readString(), data.readString(), data.readInt() != 0, data.readInt(), data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        rmdex(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        boolean _result7 = mergeProfiles(data.readInt(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _result2 = dumpProfiles(data.readInt(), data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        _result2 = copySystemProfile(data.readString(), data.readInt(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        clearAppProfiles(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        destroyAppProfiles(data.readString());
                        reply.writeNoException();
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        _result2 = createProfileSnapshot(data.readInt(), data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result2);
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        destroyProfileSnapshot(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        idmap(data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        removeIdmap(data.readString());
                        reply.writeNoException();
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        rmPackageDir(data.readString());
                        reply.writeNoException();
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        markBootComplete(data.readString());
                        reply.writeNoException();
                        return true;
                    case TRANSACTION_freeCache /*27*/:
                        parcel.enforceInterface(descriptor);
                        freeCache(data.readString(), data.readLong(), data.readLong(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        linkNativeLibraryDirectory(data.readString(), data.readString(), data.readString(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        createOatDir(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        linkFile(data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        moveAb(data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        deleteOdex(data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        installApkVerity(data.readString(), data.readRawFileDescriptor(), data.readInt());
                        reply.writeNoException();
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        assertFsverityRootHashMatches(data.readString(), data.createByteArray());
                        reply.writeNoException();
                        return true;
                    case 35:
                        parcel.enforceInterface(descriptor);
                        _result3 = reconcileSecondaryDexFile(data.readString(), data.readString(), data.readInt(), data.createStringArray(), data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        byte[] _result8 = hashSecondaryDexFile(data.readString(), data.readString(), data.readInt(), data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeByteArray(_result8);
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        invalidateMounts();
                        reply.writeNoException();
                        return true;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        boolean _result9 = isQuotaSupported(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result9);
                        return true;
                    case 39:
                        parcel.enforceInterface(descriptor);
                        _result3 = prepareAppProfile(data.readString(), data.readInt(), data.readInt(), data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        restoreCloneAppData(data.readString(), data.readString(), data.readInt(), data.readInt(), data.readInt(), data.readString(), data.readString(), data.readString(), data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 41:
                        parcel.enforceInterface(descriptor);
                        _result4 = startBackupSession(android.os.IBackupSessionCallback.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 42:
                        parcel.enforceInterface(descriptor);
                        int _result10 = executeBackupTask(data.readInt(), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result10);
                        return true;
                    case 43:
                        parcel.enforceInterface(descriptor);
                        _result4 = finishBackupSession(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 44:
                        parcel.enforceInterface(descriptor);
                        setFileXattr(data.readString(), data.readString(), data.readInt(), data.readInt());
                        reply.writeNoException();
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            Parcel parcel3 = parcel;
            parcel2.writeString(descriptor);
            return true;
        }
    }

    void assertFsverityRootHashMatches(String str, byte[] bArr) throws RemoteException;

    void clearAppData(String str, String str2, int i, int i2, long j) throws RemoteException;

    void clearAppProfiles(String str, String str2) throws RemoteException;

    boolean copySystemProfile(String str, int i, String str2, String str3) throws RemoteException;

    long createAppData(String str, String str2, int i, int i2, int i3, String str3, int i4) throws RemoteException;

    void createOatDir(String str, String str2) throws RemoteException;

    boolean createProfileSnapshot(int i, String str, String str2, String str3) throws RemoteException;

    void createUserData(String str, int i, int i2, int i3) throws RemoteException;

    void deleteOdex(String str, String str2, String str3) throws RemoteException;

    void destroyAppData(String str, String str2, int i, int i2, long j) throws RemoteException;

    void destroyAppProfiles(String str) throws RemoteException;

    void destroyProfileSnapshot(String str, String str2) throws RemoteException;

    void destroyUserData(String str, int i, int i2) throws RemoteException;

    void dexopt(String str, int i, String str2, String str3, int i2, String str4, int i3, String str5, String str6, String str7, String str8, boolean z, int i4, String str9, String str10, String str11) throws RemoteException;

    boolean dumpProfiles(int i, String str, String str2, String str3) throws RemoteException;

    int executeBackupTask(int i, String str) throws RemoteException;

    int finishBackupSession(int i) throws RemoteException;

    void fixupAppData(String str, int i) throws RemoteException;

    void freeCache(String str, long j, long j2, int i) throws RemoteException;

    long[] getAppSize(String str, String[] strArr, int i, int i2, int i3, long[] jArr, String[] strArr2) throws RemoteException;

    long[] getExternalSize(String str, int i, int i2, int[] iArr) throws RemoteException;

    long[] getUserSize(String str, int i, int i2, int[] iArr) throws RemoteException;

    byte[] hashSecondaryDexFile(String str, String str2, int i, String str3, int i2) throws RemoteException;

    void idmap(String str, String str2, int i) throws RemoteException;

    void installApkVerity(String str, FileDescriptor fileDescriptor, int i) throws RemoteException;

    void invalidateMounts() throws RemoteException;

    boolean isQuotaSupported(String str) throws RemoteException;

    void linkFile(String str, String str2, String str3) throws RemoteException;

    void linkNativeLibraryDirectory(String str, String str2, String str3, int i) throws RemoteException;

    void markBootComplete(String str) throws RemoteException;

    boolean mergeProfiles(int i, String str, String str2) throws RemoteException;

    void migrateAppData(String str, String str2, int i, int i2) throws RemoteException;

    void moveAb(String str, String str2, String str3) throws RemoteException;

    void moveCompleteApp(String str, String str2, String str3, String str4, int i, String str5, int i2) throws RemoteException;

    boolean prepareAppProfile(String str, int i, int i2, String str2, String str3, String str4) throws RemoteException;

    boolean reconcileSecondaryDexFile(String str, String str2, int i, String[] strArr, String str3, int i2) throws RemoteException;

    void removeIdmap(String str) throws RemoteException;

    void restoreCloneAppData(String str, String str2, int i, int i2, int i3, String str3, String str4, String str5, String str6, String str7) throws RemoteException;

    void restoreconAppData(String str, String str2, int i, int i2, int i3, String str3) throws RemoteException;

    void rmPackageDir(String str) throws RemoteException;

    void rmdex(String str, String str2) throws RemoteException;

    void setAppQuota(String str, int i, int i2, long j) throws RemoteException;

    void setFileXattr(String str, String str2, int i, int i2) throws RemoteException;

    int startBackupSession(IBackupSessionCallback iBackupSessionCallback) throws RemoteException;
}
