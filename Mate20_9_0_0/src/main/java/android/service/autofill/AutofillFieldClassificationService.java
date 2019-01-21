package android.service.autofill;

import android.annotation.SystemApi;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.Parcelable.Creator;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.service.autofill.IAutofillFieldClassificationService.Stub;
import android.util.Log;
import android.view.autofill.AutofillValue;
import com.android.internal.util.function.pooled.PooledLambda;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

@SystemApi
public abstract class AutofillFieldClassificationService extends Service {
    public static final String EXTRA_SCORES = "scores";
    public static final String SERVICE_INTERFACE = "android.service.autofill.AutofillFieldClassificationService";
    public static final String SERVICE_META_DATA_KEY_AVAILABLE_ALGORITHMS = "android.autofill.field_classification.available_algorithms";
    public static final String SERVICE_META_DATA_KEY_DEFAULT_ALGORITHM = "android.autofill.field_classification.default_algorithm";
    private static final String TAG = "AutofillFieldClassificationService";
    private final Handler mHandler = new Handler(Looper.getMainLooper(), null, true);
    private AutofillFieldClassificationServiceWrapper mWrapper;

    public static final class Scores implements Parcelable {
        public static final Creator<Scores> CREATOR = new Creator<Scores>() {
            public Scores createFromParcel(Parcel parcel) {
                return new Scores(parcel, null);
            }

            public Scores[] newArray(int size) {
                return new Scores[size];
            }
        };
        public final float[][] scores;

        private Scores(Parcel parcel) {
            int size1 = parcel.readInt();
            int size2 = parcel.readInt();
            this.scores = (float[][]) Array.newInstance(float.class, new int[]{size1, size2});
            for (int i = 0; i < size1; i++) {
                for (int j = 0; j < size2; j++) {
                    this.scores[i][j] = parcel.readFloat();
                }
            }
        }

        private Scores(float[][] scores) {
            this.scores = scores;
        }

        public String toString() {
            int size1 = this.scores.length;
            int i = 0;
            int size2 = size1 > 0 ? this.scores[0].length : 0;
            StringBuilder builder = new StringBuilder("Scores [");
            builder.append(size1);
            builder.append("x");
            builder.append(size2);
            builder = builder.append("] ");
            while (i < size1) {
                builder.append(i);
                builder.append(": ");
                builder.append(Arrays.toString(this.scores[i]));
                builder.append(' ');
                i++;
            }
            return builder.toString();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel parcel, int flags) {
            int size2 = this.scores[0].length;
            parcel.writeInt(size1);
            parcel.writeInt(size2);
            for (float[] fArr : this.scores) {
                for (int j = 0; j < size2; j++) {
                    parcel.writeFloat(fArr[j]);
                }
            }
        }
    }

    private final class AutofillFieldClassificationServiceWrapper extends Stub {
        private AutofillFieldClassificationServiceWrapper() {
        }

        public void getScores(RemoteCallback callback, String algorithmName, Bundle algorithmArgs, List<AutofillValue> actualValues, String[] userDataValues) throws RemoteException {
            AutofillFieldClassificationService.this.mHandler.sendMessage(PooledLambda.obtainMessage(-$$Lambda$AutofillFieldClassificationService$AutofillFieldClassificationServiceWrapper$LVFO8nQdiSarBMY_Qsf1G30GEZQ.INSTANCE, AutofillFieldClassificationService.this, callback, algorithmName, algorithmArgs, actualValues, userDataValues));
        }
    }

    private void getScores(RemoteCallback callback, String algorithmName, Bundle algorithmArgs, List<AutofillValue> actualValues, String[] userDataValues) {
        Bundle data = new Bundle();
        float[][] scores = onGetScores(algorithmName, algorithmArgs, actualValues, Arrays.asList(userDataValues));
        if (scores != null) {
            data.putParcelable(EXTRA_SCORES, new Scores(scores, null));
        }
        callback.sendResult(data);
    }

    public void onCreate() {
        super.onCreate();
        this.mWrapper = new AutofillFieldClassificationServiceWrapper();
    }

    public IBinder onBind(Intent intent) {
        return this.mWrapper;
    }

    @SystemApi
    public float[][] onGetScores(String algorithm, Bundle algorithmOptions, List<AutofillValue> list, List<String> list2) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("service implementation (");
        stringBuilder.append(getClass());
        stringBuilder.append(" does not implement onGetScore()");
        Log.e(str, stringBuilder.toString());
        return null;
    }
}
