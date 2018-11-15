package android.support.v4.media;

import android.media.browse.MediaBrowser.MediaItem;
import android.support.annotation.RequiresApi;
import java.lang.reflect.Constructor;
import java.util.List;

@RequiresApi(21)
class ParceledListSliceAdapterApi21 {
    private static Constructor sConstructor;

    /* JADX WARNING: Removed duplicated region for block: B:2:0x0015 A:{Splitter: B:0:0x0000, ExcHandler: java.lang.ClassNotFoundException (r0_2 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:2:0x0015, code:
            r0 = move-exception;
     */
    /* JADX WARNING: Missing block: B:3:0x0016, code:
            r0.printStackTrace();
     */
    /* JADX WARNING: Missing block: B:4:?, code:
            return;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static {
        try {
            sConstructor = Class.forName("android.content.pm.ParceledListSlice").getConstructor(new Class[]{List.class});
        } catch (ReflectiveOperationException e) {
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:4:0x000f A:{Splitter: B:1:0x0001, ExcHandler: java.lang.InstantiationException (r1_2 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Removed duplicated region for block: B:4:0x000f A:{Splitter: B:1:0x0001, ExcHandler: java.lang.InstantiationException (r1_2 'e' java.lang.ReflectiveOperationException)} */
    /* JADX WARNING: Missing block: B:4:0x000f, code:
            r1 = move-exception;
     */
    /* JADX WARNING: Missing block: B:5:0x0010, code:
            r1.printStackTrace();
     */
    /* JADX WARNING: Missing block: B:6:?, code:
            return null;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    static Object newInstance(List<MediaItem> itemList) {
        try {
            return sConstructor.newInstance(new Object[]{itemList});
        } catch (ReflectiveOperationException e) {
        }
    }

    private ParceledListSliceAdapterApi21() {
    }
}
