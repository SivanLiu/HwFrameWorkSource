package com.android.server.slice;

import android.text.TextUtils;
import com.android.server.slice.SliceClientPermissions.SliceAuthority;
import java.util.function.Function;

/* compiled from: lambda */
public final /* synthetic */ class -$$Lambda$SliceClientPermissions$SliceAuthority$lvjy01xuWTQLCsbGw02qqI7DYDM implements Function {
    public static final /* synthetic */ -$$Lambda$SliceClientPermissions$SliceAuthority$lvjy01xuWTQLCsbGw02qqI7DYDM INSTANCE = new -$$Lambda$SliceClientPermissions$SliceAuthority$lvjy01xuWTQLCsbGw02qqI7DYDM();

    private /* synthetic */ -$$Lambda$SliceClientPermissions$SliceAuthority$lvjy01xuWTQLCsbGw02qqI7DYDM() {
    }

    public final Object apply(Object obj) {
        return TextUtils.join(SliceAuthority.DELIMITER, (String[]) obj);
    }
}
