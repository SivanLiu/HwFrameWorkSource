package huawei.android.telephony.wrapper;

import android.telephony.Rlog;

public class OptWrapperFactory {
    private static Factory mInstance;

    public interface Factory {
        IIccPhoneBookMSimWrapper getIIccPhoneBookMSimWrapper();

        MSimUiccControllerWrapper getMSimUiccControllerWrapper();

        PhoneWrapper getPhoneWrapper();
    }

    static {
        try {
            mInstance = (Factory) Class.forName("huawei.android.telephony.wrapper.OptWrapperFactoryImpl").getConstructor(new Class[0]).newInstance(new Object[0]);
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("got success! mInstance = ");
            stringBuilder.append(mInstance);
            Rlog.d("OptWrapperFactory", stringBuilder.toString());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SecurityException e2) {
            e2.printStackTrace();
        } catch (NoSuchMethodException e3) {
            e3.printStackTrace();
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        if (mInstance == null) {
            mInstance = new DummyOptWrapperFactory();
        }
    }

    public static MSimUiccControllerWrapper getMSimUiccControllerWrapper() {
        return mInstance.getMSimUiccControllerWrapper();
    }

    public static IIccPhoneBookMSimWrapper getIIccPhoneBookMSimWrapper() {
        return mInstance.getIIccPhoneBookMSimWrapper();
    }

    public static PhoneWrapper getPhoneWrapper() {
        return mInstance.getPhoneWrapper();
    }
}
