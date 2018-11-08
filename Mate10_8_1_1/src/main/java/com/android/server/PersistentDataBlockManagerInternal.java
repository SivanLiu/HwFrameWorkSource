package com.android.server;

public interface PersistentDataBlockManagerInternal {
    byte[] getFrpCredentialHandle();

    void setFrpCredentialHandle(byte[] bArr);
}
