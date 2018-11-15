package com.android.server.media;

import android.media.session.MediaSession;
import android.os.Debug;
import android.util.IntArray;
import android.util.Log;
import android.util.SparseArray;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class MediaSessionStack {
    private static final int[] ALWAYS_PRIORITY_STATES = new int[]{4, 5, 9, 10};
    private static final boolean DEBUG = MediaSessionService.DEBUG;
    private static final String PRIORITY_PKG_CAMERA = "com.huawei.camera";
    private static final String TAG = "MediaSessionStack";
    private static final int[] TRANSITION_PRIORITY_STATES = new int[]{6, 8, 3};
    private final AudioPlayerStateMonitor mAudioPlayerStateMonitor;
    private final SparseArray<ArrayList<MediaSessionRecord>> mCachedActiveLists = new SparseArray();
    MediaSessionRecord mCachedButtonReceiver;
    private MediaSessionRecord mCachedDefault;
    private MediaSessionRecord mCachedVolumeDefault;
    MediaSessionRecord mGlobalPrioritySession;
    boolean mIsSupportMediaKey = true;
    MediaSessionRecord mLastInterestingRecord;
    private MediaSessionRecord mMediaButtonSession;
    private final OnMediaButtonSessionChangedListener mOnMediaButtonSessionChangedListener;
    final List<MediaSessionRecord> mSessions = new ArrayList();

    interface OnMediaButtonSessionChangedListener {
        void onMediaButtonSessionChanged(MediaSessionRecord mediaSessionRecord, MediaSessionRecord mediaSessionRecord2);
    }

    MediaSessionStack(AudioPlayerStateMonitor monitor, OnMediaButtonSessionChangedListener listener) {
        this.mAudioPlayerStateMonitor = monitor;
        this.mOnMediaButtonSessionChangedListener = listener;
    }

    public void addSession(MediaSessionRecord record) {
        this.mSessions.add(record);
        clearCache(record.getUserId());
        updateMediaButtonSessionIfNeeded();
    }

    public void removeSession(MediaSessionRecord record) {
        this.mSessions.remove(record);
        if (this.mMediaButtonSession == record) {
            updateMediaButtonSession(null);
        }
        clearCache(record.getUserId());
    }

    public boolean contains(MediaSessionRecord record) {
        return this.mSessions.contains(record);
    }

    public void onPlaystateChanged(MediaSessionRecord record, int oldState, int newState) {
        if (shouldUpdatePriority(oldState, newState)) {
            this.mSessions.remove(record);
            this.mSessions.add(0, record);
            clearCache(record.getUserId());
        } else if (!MediaSession.isActiveState(newState)) {
            this.mCachedVolumeDefault = null;
        }
        if (this.mMediaButtonSession != null && this.mMediaButtonSession.getUid() == record.getUid()) {
            MediaSessionRecord newMediaButtonSession = findMediaButtonSession(this.mMediaButtonSession.getUid());
            if (newMediaButtonSession != this.mMediaButtonSession) {
                updateMediaButtonSession(newMediaButtonSession);
            }
        }
    }

    public void onSessionStateChange(MediaSessionRecord record) {
        if (isSystemSession(record)) {
            if (record.isActive()) {
                updateMediaButtonSession(record);
            } else {
                updateMediaButtonSessionIfNeeded();
            }
        }
        clearCache(record.getUserId());
    }

    public void updateMediaButtonSessionIfNeeded() {
        if (DEBUG) {
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("updateMediaButtonSessionIfNeeded, callers=");
            stringBuilder.append(Debug.getCallers(2));
            Log.d(str, stringBuilder.toString());
        }
        MediaSessionRecord mediaButtonSession = getSystemActiveSession();
        if (mediaButtonSession != null) {
            updateMediaButtonSession(mediaButtonSession);
            return;
        }
        IntArray audioPlaybackUids = this.mAudioPlayerStateMonitor.getSortedAudioPlaybackClientUids();
        for (int i = 0; i < audioPlaybackUids.size(); i++) {
            mediaButtonSession = findMediaButtonSession(audioPlaybackUids.get(i));
            if (mediaButtonSession != null) {
                this.mAudioPlayerStateMonitor.cleanUpAudioPlaybackUids(mediaButtonSession.getUid());
                if (this.mMediaButtonSession != mediaButtonSession) {
                    updateMediaButtonSession(mediaButtonSession);
                }
                return;
            }
        }
    }

    private MediaSessionRecord findMediaButtonSession(int uid) {
        MediaSessionRecord mediaButtonSession = null;
        for (MediaSessionRecord session : this.mSessions) {
            if (uid == session.getUid()) {
                if (session.getPlaybackState() != null && session.isPlaybackActive() == this.mAudioPlayerStateMonitor.isPlaybackActive(session.getUid())) {
                    return session;
                }
                if (mediaButtonSession == null) {
                    mediaButtonSession = session;
                }
            }
        }
        return mediaButtonSession;
    }

    public ArrayList<MediaSessionRecord> getActiveSessions(int userId) {
        ArrayList<MediaSessionRecord> cachedActiveList = (ArrayList) this.mCachedActiveLists.get(userId);
        if (cachedActiveList != null) {
            return cachedActiveList;
        }
        cachedActiveList = getPriorityList(true, userId);
        this.mCachedActiveLists.put(userId, cachedActiveList);
        return cachedActiveList;
    }

    public MediaSessionRecord getMediaButtonSession() {
        if (this.mMediaButtonSession == null || !isSystemSession(this.mMediaButtonSession)) {
            return this.mMediaButtonSession;
        }
        if (this.mMediaButtonSession.isActive()) {
            return this.mMediaButtonSession;
        }
        return null;
    }

    private void updateMediaButtonSession(MediaSessionRecord newMediaButtonSession) {
        MediaSessionRecord oldMediaButtonSession = this.mMediaButtonSession;
        this.mMediaButtonSession = newMediaButtonSession;
        this.mOnMediaButtonSessionChangedListener.onMediaButtonSessionChanged(oldMediaButtonSession, newMediaButtonSession);
    }

    public MediaSessionRecord getDefaultVolumeSession() {
        if (this.mCachedVolumeDefault != null) {
            return this.mCachedVolumeDefault;
        }
        ArrayList<MediaSessionRecord> records = getPriorityList(true, -1);
        int size = records.size();
        for (int i = 0; i < size; i++) {
            MediaSessionRecord record = (MediaSessionRecord) records.get(i);
            if (record.isPlaybackActive()) {
                this.mCachedVolumeDefault = record;
                return record;
            }
        }
        return null;
    }

    public MediaSessionRecord getDefaultRemoteSession(int userId) {
        ArrayList<MediaSessionRecord> records = getPriorityList(true, userId);
        int size = records.size();
        for (int i = 0; i < size; i++) {
            MediaSessionRecord record = (MediaSessionRecord) records.get(i);
            if (record.getPlaybackType() == 2) {
                return record;
            }
        }
        return null;
    }

    public void dump(PrintWriter pw, String prefix) {
        int i = 0;
        ArrayList<MediaSessionRecord> sortedSessions = getPriorityList(false, -1);
        int count = sortedSessions.size();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("Media button session is ");
        stringBuilder.append(this.mMediaButtonSession);
        pw.println(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append(prefix);
        stringBuilder.append("Sessions Stack - have ");
        stringBuilder.append(count);
        stringBuilder.append(" sessions:");
        pw.println(stringBuilder.toString());
        String indent = new StringBuilder();
        indent.append(prefix);
        indent.append("  ");
        indent = indent.toString();
        while (i < count) {
            ((MediaSessionRecord) sortedSessions.get(i)).dump(pw, indent);
            pw.println();
            i++;
        }
    }

    public ArrayList<MediaSessionRecord> getPriorityList(boolean activeOnly, int userId) {
        ArrayList<MediaSessionRecord> result = new ArrayList();
        int lastPlaybackActiveIndex = 0;
        int lastActiveIndex = 0;
        int size = this.mSessions.size();
        for (int i = 0; i < size; i++) {
            MediaSessionRecord session = (MediaSessionRecord) this.mSessions.get(i);
            if (userId == -1 || userId == session.getUserId()) {
                if (session.isActive()) {
                    int lastPlaybackActiveIndex2;
                    if (session.isPlaybackActive()) {
                        lastPlaybackActiveIndex2 = lastPlaybackActiveIndex + 1;
                        result.add(lastPlaybackActiveIndex, session);
                        lastActiveIndex++;
                        lastPlaybackActiveIndex = lastPlaybackActiveIndex2;
                    } else {
                        lastPlaybackActiveIndex2 = lastActiveIndex + 1;
                        result.add(lastActiveIndex, session);
                        lastActiveIndex = lastPlaybackActiveIndex2;
                    }
                } else if (!activeOnly) {
                    result.add(session);
                }
            }
        }
        return result;
    }

    private boolean shouldUpdatePriority(int oldState, int newState) {
        if (containsState(newState, ALWAYS_PRIORITY_STATES)) {
            return true;
        }
        if (containsState(oldState, TRANSITION_PRIORITY_STATES) || !containsState(newState, TRANSITION_PRIORITY_STATES)) {
            return false;
        }
        return true;
    }

    private boolean containsState(int state, int[] states) {
        for (int i : states) {
            if (i == state) {
                return true;
            }
        }
        return false;
    }

    private void clearCache(int userId) {
        this.mCachedVolumeDefault = null;
        this.mCachedActiveLists.remove(userId);
        this.mCachedActiveLists.remove(-1);
    }

    private boolean isSystemSession(MediaSessionRecord mediaSession) {
        if (mediaSession == null || mediaSession.mPackageName == null || !mediaSession.mPackageName.equals(PRIORITY_PKG_CAMERA)) {
            return false;
        }
        return true;
    }

    private MediaSessionRecord getSystemActiveSession() {
        for (MediaSessionRecord mediaSession : this.mSessions) {
            if (isSystemSession(mediaSession) && mediaSession.isActive()) {
                return mediaSession;
            }
        }
        return null;
    }
}
