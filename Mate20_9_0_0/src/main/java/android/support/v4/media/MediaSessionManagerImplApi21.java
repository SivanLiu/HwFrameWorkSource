package android.support.v4.media;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;

@RequiresApi(21)
class MediaSessionManagerImplApi21 extends MediaSessionManagerImplBase {
    MediaSessionManagerImplApi21(Context context) {
        super(context);
        this.mContext = context;
    }

    public boolean isTrustedForMediaControl(@NonNull RemoteUserInfoImpl userInfo) {
        return hasMediaControlPermission(userInfo) || super.isTrustedForMediaControl(userInfo);
    }

    private boolean hasMediaControlPermission(@NonNull RemoteUserInfoImpl userInfo) {
        return getContext().checkPermission("android.permission.MEDIA_CONTENT_CONTROL", userInfo.getPid(), userInfo.getUid()) == 0;
    }
}
