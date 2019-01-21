package huawei.android.telephony.wrapper;

import android.telephony.Rlog;
import com.android.internal.telephony.Phone;
import com.huawei.utils.reflect.HwReflectUtils;
import java.lang.reflect.Method;

public class DummyPhoneWrapper implements PhoneWrapper {
    private static final Class<?> CLASS_Phone = HwReflectUtils.getClass("com.android.internal.telephony.Phone");
    private static final String LOG_TAG = "DummyPhoneWrapper";
    private static final Method METHOD_getSubscription = HwReflectUtils.getMethod(CLASS_Phone, "getSubscription", new Class[0]);
    private static DummyPhoneWrapper mInstance = new DummyPhoneWrapper();

    public static PhoneWrapper getInstance() {
        return mInstance;
    }

    public int getSubscription(Phone phone) {
        try {
            return ((Integer) HwReflectUtils.invoke(phone, METHOD_getSubscription, new Object[0])).intValue();
        } catch (Exception e) {
            String str = LOG_TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("METHOD_getSubscription cause exception!");
            stringBuilder.append(e.toString());
            Rlog.e(str, stringBuilder.toString());
            return 0;
        }
    }
}
