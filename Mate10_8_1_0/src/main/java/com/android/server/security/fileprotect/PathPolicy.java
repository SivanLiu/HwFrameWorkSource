package com.android.server.security.fileprotect;

/* compiled from: PackStoragePolicy */
class PathPolicy {
    public static final int DIRECTORY_AND_FILES = 17;
    public static final int STORAGE_ECE_TYPE = 2;
    public static final int STORAGE_INVALID_TYPE = -1;
    public static final int STORAGE_SECE_TYPE = 3;
    public static final int TYPE_DIRECTORY = 16;
    public static final int TYPE_FILE = 0;
    public int encryptionType;
    public int fileType;
    public String path;

    public PathPolicy(String path, int encryptionType, int fileType) {
        this.path = path;
        this.encryptionType = encryptionType;
        this.fileType = fileType;
    }

    public PathPolicy(String path, String encryptionType, int fileType) {
        this.path = path;
        this.encryptionType = getStorageType(encryptionType);
        this.fileType = fileType;
    }

    private int getStorageType(String type) {
        if ("ece".equals(type)) {
            return 2;
        }
        if ("sece".equals(type)) {
            return 3;
        }
        return -1;
    }
}
