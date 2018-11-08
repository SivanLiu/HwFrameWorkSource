package tmsdkobf;

public class me {
    public static void a(Thread thread, Throwable th, String str, byte[] bArr) {
        try {
            Class.forName("com.tencent.feedback.eup.CrashReport").getDeclaredMethod("handleCatchException", new Class[]{Thread.class, Throwable.class, String.class, byte[].class}).invoke(null, new Object[]{thread, th, str, bArr});
        } catch (Object -l_4_R) {
            -l_4_R.printStackTrace();
        } catch (Object -l_4_R2) {
            -l_4_R2.printStackTrace();
        } catch (Object -l_4_R22) {
            -l_4_R22.printStackTrace();
        } catch (Object -l_4_R222) {
            -l_4_R222.printStackTrace();
        } catch (Object -l_4_R2222) {
            -l_4_R2222.printStackTrace();
        }
    }
}
