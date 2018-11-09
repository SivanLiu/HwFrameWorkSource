package android.service.autofill;

import android.os.Parcelable;
import android.widget.RemoteViews;

abstract class InternalTransformation implements Transformation, Parcelable {
    abstract void apply(ValueFinder valueFinder, RemoteViews remoteViews, int i) throws Exception;

    InternalTransformation() {
    }
}
