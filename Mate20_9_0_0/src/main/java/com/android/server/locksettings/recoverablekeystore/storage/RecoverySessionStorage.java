package com.android.server.locksettings.recoverablekeystore.storage;

import android.util.SparseArray;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import javax.security.auth.Destroyable;

public class RecoverySessionStorage implements Destroyable {
    private final SparseArray<ArrayList<Entry>> mSessionsByUid = new SparseArray();

    public static class Entry implements Destroyable {
        private final byte[] mKeyClaimant;
        private final byte[] mLskfHash;
        private final String mSessionId;
        private final byte[] mVaultParams;

        public Entry(String sessionId, byte[] lskfHash, byte[] keyClaimant, byte[] vaultParams) {
            this.mLskfHash = lskfHash;
            this.mSessionId = sessionId;
            this.mKeyClaimant = keyClaimant;
            this.mVaultParams = vaultParams;
        }

        public byte[] getLskfHash() {
            return this.mLskfHash;
        }

        public byte[] getKeyClaimant() {
            return this.mKeyClaimant;
        }

        public byte[] getVaultParams() {
            return this.mVaultParams;
        }

        public void destroy() {
            Arrays.fill(this.mLskfHash, (byte) 0);
            Arrays.fill(this.mKeyClaimant, (byte) 0);
        }
    }

    public Entry get(int uid, String sessionId) {
        ArrayList<Entry> userEntries = (ArrayList) this.mSessionsByUid.get(uid);
        if (userEntries == null) {
            return null;
        }
        Iterator it = userEntries.iterator();
        while (it.hasNext()) {
            Entry entry = (Entry) it.next();
            if (sessionId.equals(entry.mSessionId)) {
                return entry;
            }
        }
        return null;
    }

    public void add(int uid, Entry entry) {
        if (this.mSessionsByUid.get(uid) == null) {
            this.mSessionsByUid.put(uid, new ArrayList());
        }
        ((ArrayList) this.mSessionsByUid.get(uid)).add(entry);
    }

    public void remove(int uid, String sessionId) {
        if (this.mSessionsByUid.get(uid) != null) {
            ((ArrayList) this.mSessionsByUid.get(uid)).removeIf(new -$$Lambda$RecoverySessionStorage$1ayqf2qqdJH00fvbhBUKWso4cdc(sessionId));
        }
    }

    public void remove(int uid) {
        ArrayList<Entry> entries = (ArrayList) this.mSessionsByUid.get(uid);
        if (entries != null) {
            Iterator it = entries.iterator();
            while (it.hasNext()) {
                ((Entry) it.next()).destroy();
            }
            this.mSessionsByUid.remove(uid);
        }
    }

    public int size() {
        int size = 0;
        for (int i = 0; i < this.mSessionsByUid.size(); i++) {
            size += ((ArrayList) this.mSessionsByUid.valueAt(i)).size();
        }
        return size;
    }

    public void destroy() {
        int numberOfUids = this.mSessionsByUid.size();
        for (int i = 0; i < numberOfUids; i++) {
            Iterator it = ((ArrayList) this.mSessionsByUid.valueAt(i)).iterator();
            while (it.hasNext()) {
                ((Entry) it.next()).destroy();
            }
        }
    }
}
