package android.location;

import android.content.Context;
import android.location.ILocationManager.Stub;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Geocoder {
    private static final String TAG = "Geocoder";
    private GeocoderParams mParams;
    private ILocationManager mService;

    public static boolean isPresent() {
        ILocationManager lm = Stub.asInterface(ServiceManager.getService("location"));
        if (lm == null) {
            return false;
        }
        try {
            return lm.geocoderIsPresent();
        } catch (RemoteException e) {
            Log.e(TAG, "isPresent: got RemoteException", e);
            return false;
        }
    }

    public Geocoder(Context context, Locale locale) {
        if (locale != null) {
            this.mParams = new GeocoderParams(context, locale);
            this.mService = Stub.asInterface(ServiceManager.getService("location"));
            return;
        }
        throw new NullPointerException("locale == null");
    }

    public Geocoder(Context context) {
        this(context, Locale.getDefault());
    }

    public List<Address> getFromLocation(double latitude, double longitude, int maxResults) throws IOException {
        StringBuilder stringBuilder;
        if (latitude < -90.0d || latitude > 90.0d) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("latitude == ");
            stringBuilder.append(latitude);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (longitude < -180.0d || longitude > 180.0d) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("longitude == ");
            stringBuilder.append(longitude);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            try {
                List<Address> results = new ArrayList();
                String ex = this.mService.getFromLocation(latitude, longitude, maxResults, this.mParams, results);
                if (ex == null) {
                    return results;
                }
                throw new IOException(ex);
            } catch (RemoteException e) {
                Log.e(TAG, "getFromLocation: got RemoteException", e);
                return null;
            }
        }
    }

    public List<Address> getFromLocationName(String locationName, int maxResults) throws IOException {
        if (locationName != null) {
            try {
                List<Address> results = new ArrayList();
                String ex = this.mService.getFromLocationName(locationName, 0.0d, 0.0d, 0.0d, 0.0d, maxResults, this.mParams, results);
                if (ex == null) {
                    return results;
                }
                throw new IOException(ex);
            } catch (RemoteException e) {
                Log.e(TAG, "getFromLocationName: got RemoteException", e);
                return null;
            }
        }
        throw new IllegalArgumentException("locationName == null");
    }

    public List<Address> getFromLocationName(String locationName, int maxResults, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude) throws IOException {
        double d = lowerLeftLatitude;
        double d2 = lowerLeftLongitude;
        double d3 = upperRightLatitude;
        double d4 = upperRightLongitude;
        double d5;
        double d6;
        double d7;
        StringBuilder stringBuilder;
        if (locationName == null) {
            d5 = d4;
            d6 = d3;
            d7 = d2;
            double d8 = d;
            throw new IllegalArgumentException("locationName == null");
        } else if (d < -90.0d || d > 90.0d) {
            d5 = d4;
            d6 = d3;
            d7 = d2;
            stringBuilder = new StringBuilder();
            stringBuilder.append("lowerLeftLatitude == ");
            stringBuilder.append(lowerLeftLatitude);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (d2 < -180.0d || d2 > 180.0d) {
            d5 = d4;
            d6 = d3;
            stringBuilder = new StringBuilder();
            stringBuilder.append("lowerLeftLongitude == ");
            stringBuilder.append(lowerLeftLongitude);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (d3 < -90.0d || d3 > 90.0d) {
            d5 = d4;
            stringBuilder = new StringBuilder();
            stringBuilder.append("upperRightLatitude == ");
            stringBuilder.append(upperRightLatitude);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else if (d4 < -180.0d || d4 > 180.0d) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("upperRightLongitude == ");
            stringBuilder.append(upperRightLongitude);
            throw new IllegalArgumentException(stringBuilder.toString());
        } else {
            try {
                ArrayList<Address> result = new ArrayList();
                String ex = this.mService.getFromLocationName(locationName, d, d2, d3, upperRightLongitude, maxResults, this.mParams, result);
                if (ex == null) {
                    return result;
                }
                throw new IOException(ex);
            } catch (RemoteException e) {
                Log.e(TAG, "getFromLocationName: got RemoteException", e);
                return null;
            }
        }
    }
}
