package android.location;

import android.app.PendingIntent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import com.android.internal.location.ProviderProperties;
import java.util.ArrayList;
import java.util.List;

public interface ILocationManager extends IInterface {

    public static abstract class Stub extends Binder implements ILocationManager {
        private static final String DESCRIPTOR = "android.location.ILocationManager";
        static final int TRANSACTION_addGnssBatchingCallback = 19;
        static final int TRANSACTION_addGnssMeasurementsListener = 12;
        static final int TRANSACTION_addGnssNavigationMessageListener = 14;
        static final int TRANSACTION_addTestProvider = 35;
        static final int TRANSACTION_clearTestProviderEnabled = 40;
        static final int TRANSACTION_clearTestProviderLocation = 38;
        static final int TRANSACTION_clearTestProviderStatus = 42;
        static final int TRANSACTION_flushGnssBatch = 22;
        static final int TRANSACTION_geocoderIsPresent = 8;
        static final int TRANSACTION_getAllProviders = 25;
        static final int TRANSACTION_getBackgroundThrottlingWhitelist = 47;
        static final int TRANSACTION_getBestProvider = 27;
        static final int TRANSACTION_getFromLocation = 9;
        static final int TRANSACTION_getFromLocationName = 10;
        static final int TRANSACTION_getGnssBatchSize = 18;
        static final int TRANSACTION_getGnssHardwareModelName = 17;
        static final int TRANSACTION_getGnssYearOfHardware = 16;
        static final int TRANSACTION_getLastLocation = 5;
        static final int TRANSACTION_getNetworkProviderPackage = 30;
        static final int TRANSACTION_getProviderProperties = 29;
        static final int TRANSACTION_getProviders = 26;
        static final int TRANSACTION_injectLocation = 24;
        static final int TRANSACTION_isLocationEnabledForUser = 33;
        static final int TRANSACTION_isProviderEnabledForUser = 31;
        static final int TRANSACTION_locationCallbackFinished = 46;
        static final int TRANSACTION_providerMeetsCriteria = 28;
        static final int TRANSACTION_registerGnssStatusCallback = 6;
        static final int TRANSACTION_removeGeofence = 4;
        static final int TRANSACTION_removeGnssBatchingCallback = 20;
        static final int TRANSACTION_removeGnssMeasurementsListener = 13;
        static final int TRANSACTION_removeGnssNavigationMessageListener = 15;
        static final int TRANSACTION_removeTestProvider = 36;
        static final int TRANSACTION_removeUpdates = 2;
        static final int TRANSACTION_reportLocation = 44;
        static final int TRANSACTION_reportLocationBatch = 45;
        static final int TRANSACTION_requestGeofence = 3;
        static final int TRANSACTION_requestLocationUpdates = 1;
        static final int TRANSACTION_sendExtraCommand = 43;
        static final int TRANSACTION_sendNiResponse = 11;
        static final int TRANSACTION_setLocationEnabledForUser = 34;
        static final int TRANSACTION_setProviderEnabledForUser = 32;
        static final int TRANSACTION_setTestProviderEnabled = 39;
        static final int TRANSACTION_setTestProviderLocation = 37;
        static final int TRANSACTION_setTestProviderStatus = 41;
        static final int TRANSACTION_startGnssBatch = 21;
        static final int TRANSACTION_stopGnssBatch = 23;
        static final int TRANSACTION_unregisterGnssStatusCallback = 7;

        private static class Proxy implements ILocationManager {
            private IBinder mRemote;

            Proxy(IBinder remote) {
                this.mRemote = remote;
            }

            public IBinder asBinder() {
                return this.mRemote;
            }

            public String getInterfaceDescriptor() {
                return Stub.DESCRIPTOR;
            }

            public void requestLocationUpdates(LocationRequest request, ILocationListener listener, PendingIntent intent, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(1, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeUpdates(ILocationListener listener, PendingIntent intent, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(2, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void requestGeofence(LocationRequest request, Geofence geofence, PendingIntent intent, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (geofence != null) {
                        _data.writeInt(1);
                        geofence.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(3, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeGeofence(Geofence fence, PendingIntent intent, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (fence != null) {
                        _data.writeInt(1);
                        fence.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    if (intent != null) {
                        _data.writeInt(1);
                        intent.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(4, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public Location getLastLocation(LocationRequest request, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    Location _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (request != null) {
                        _data.writeInt(1);
                        request.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(packageName);
                    this.mRemote.transact(5, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (Location) Location.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean registerGnssStatusCallback(IGnssStatusListener callback, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(6, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void unregisterGnssStatusCallback(IGnssStatusListener callback) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    this.mRemote.transact(7, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean geocoderIsPresent() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(8, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getFromLocation(double latitude, double longitude, int maxResults, GeocoderParams params, List<Address> addrs) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeDouble(latitude);
                    _data.writeDouble(longitude);
                    _data.writeInt(maxResults);
                    if (params != null) {
                        _data.writeInt(1);
                        params.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(9, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    _reply.readTypedList(addrs, Address.CREATOR);
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getFromLocationName(String locationName, double lowerLeftLatitude, double lowerLeftLongitude, double upperRightLatitude, double upperRightLongitude, int maxResults, GeocoderParams params, List<Address> addrs) throws RemoteException {
                Throwable th;
                double d;
                double d2;
                int i;
                List<Address> list;
                double d3;
                double d4;
                GeocoderParams geocoderParams = params;
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    String _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    try {
                        _data.writeString(locationName);
                        try {
                            _data.writeDouble(lowerLeftLatitude);
                            try {
                                _data.writeDouble(lowerLeftLongitude);
                            } catch (Throwable th2) {
                                th = th2;
                                d = upperRightLatitude;
                                d2 = upperRightLongitude;
                                i = maxResults;
                                list = addrs;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            d3 = lowerLeftLongitude;
                            d = upperRightLatitude;
                            d2 = upperRightLongitude;
                            i = maxResults;
                            list = addrs;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th4) {
                        th = th4;
                        d4 = lowerLeftLatitude;
                        d3 = lowerLeftLongitude;
                        d = upperRightLatitude;
                        d2 = upperRightLongitude;
                        i = maxResults;
                        list = addrs;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _data.writeDouble(upperRightLatitude);
                        try {
                            _data.writeDouble(upperRightLongitude);
                            try {
                                _data.writeInt(maxResults);
                                if (geocoderParams != null) {
                                    _data.writeInt(1);
                                    geocoderParams.writeToParcel(_data, 0);
                                } else {
                                    _data.writeInt(0);
                                }
                                try {
                                    this.mRemote.transact(10, _data, _reply, 0);
                                    _reply.readException();
                                    _result = _reply.readString();
                                } catch (Throwable th5) {
                                    th = th5;
                                    list = addrs;
                                    _reply.recycle();
                                    _data.recycle();
                                    throw th;
                                }
                            } catch (Throwable th6) {
                                th = th6;
                                list = addrs;
                                _reply.recycle();
                                _data.recycle();
                                throw th;
                            }
                        } catch (Throwable th7) {
                            th = th7;
                            i = maxResults;
                            list = addrs;
                            _reply.recycle();
                            _data.recycle();
                            throw th;
                        }
                    } catch (Throwable th8) {
                        th = th8;
                        d2 = upperRightLongitude;
                        i = maxResults;
                        list = addrs;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                    try {
                        _reply.readTypedList(addrs, Address.CREATOR);
                        _reply.recycle();
                        _data.recycle();
                        return _result;
                    } catch (Throwable th9) {
                        th = th9;
                        _reply.recycle();
                        _data.recycle();
                        throw th;
                    }
                } catch (Throwable th10) {
                    th = th10;
                    String str = locationName;
                    d4 = lowerLeftLatitude;
                    d3 = lowerLeftLongitude;
                    d = upperRightLatitude;
                    d2 = upperRightLongitude;
                    i = maxResults;
                    list = addrs;
                    _reply.recycle();
                    _data.recycle();
                    throw th;
                }
            }

            public boolean sendNiResponse(int notifId, int userResponse) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(notifId);
                    _data.writeInt(userResponse);
                    boolean z = false;
                    this.mRemote.transact(11, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean addGnssMeasurementsListener(IGnssMeasurementsListener listener, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(12, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeGnssMeasurementsListener(IGnssMeasurementsListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(13, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean addGnssNavigationMessageListener(IGnssNavigationMessageListener listener, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(14, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeGnssNavigationMessageListener(IGnssNavigationMessageListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(15, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getGnssYearOfHardware() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(16, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getGnssHardwareModelName() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(17, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public int getGnssBatchSize(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(18, _data, _reply, 0);
                    _reply.readException();
                    int _result = _reply.readInt();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean addGnssBatchingCallback(IBatchedLocationCallback callback, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(callback != null ? callback.asBinder() : null);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(19, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeGnssBatchingCallback() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(20, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean startGnssBatch(long periodNanos, boolean wakeOnFifoFull, String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeLong(periodNanos);
                    _data.writeInt(wakeOnFifoFull);
                    _data.writeString(packageName);
                    boolean z = false;
                    this.mRemote.transact(21, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void flushGnssBatch(String packageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(packageName);
                    this.mRemote.transact(22, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean stopGnssBatch() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean z = false;
                    this.mRemote.transact(23, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean injectLocation(Location location) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    boolean _result = true;
                    if (location != null) {
                        _data.writeInt(1);
                        location.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(24, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getAllProviders() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(25, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public List<String> getProviders(Criteria criteria, boolean enabledOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (criteria != null) {
                        _data.writeInt(1);
                        criteria.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(enabledOnly);
                    this.mRemote.transact(26, _data, _reply, 0);
                    _reply.readException();
                    List<String> _result = _reply.createStringArrayList();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getBestProvider(Criteria criteria, boolean enabledOnly) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (criteria != null) {
                        _data.writeInt(1);
                        criteria.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(enabledOnly);
                    this.mRemote.transact(27, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean providerMeetsCriteria(String provider, Criteria criteria) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    boolean _result = true;
                    if (criteria != null) {
                        _data.writeInt(1);
                        criteria.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(28, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public ProviderProperties getProviderProperties(String provider) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    ProviderProperties _result;
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    this.mRemote.transact(29, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        _result = (ProviderProperties) ProviderProperties.CREATOR.createFromParcel(_reply);
                    } else {
                        _result = null;
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String getNetworkProviderPackage() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(30, _data, _reply, 0);
                    _reply.readException();
                    String _result = _reply.readString();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isProviderEnabledForUser(String provider, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    _data.writeInt(userId);
                    boolean z = false;
                    this.mRemote.transact(31, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean setProviderEnabledForUser(String provider, boolean enabled, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    _data.writeInt(enabled);
                    _data.writeInt(userId);
                    boolean z = false;
                    this.mRemote.transact(32, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean isLocationEnabledForUser(int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(userId);
                    boolean z = false;
                    this.mRemote.transact(33, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() != 0) {
                        z = true;
                    }
                    boolean _result = z;
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setLocationEnabledForUser(boolean enabled, int userId) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeInt(enabled);
                    _data.writeInt(userId);
                    this.mRemote.transact(34, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void addTestProvider(String name, ProviderProperties properties, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(name);
                    if (properties != null) {
                        _data.writeInt(1);
                        properties.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(opPackageName);
                    this.mRemote.transact(35, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void removeTestProvider(String provider, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(36, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setTestProviderLocation(String provider, Location loc, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    if (loc != null) {
                        _data.writeInt(1);
                        loc.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeString(opPackageName);
                    this.mRemote.transact(37, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearTestProviderLocation(String provider, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(38, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setTestProviderEnabled(String provider, boolean enabled, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    _data.writeInt(enabled);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(39, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearTestProviderEnabled(String provider, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(40, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void setTestProviderStatus(String provider, int status, Bundle extras, long updateTime, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    _data.writeInt(status);
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeLong(updateTime);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(41, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void clearTestProviderStatus(String provider, String opPackageName) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    _data.writeString(opPackageName);
                    this.mRemote.transact(42, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public boolean sendExtraCommand(String provider, String command, Bundle extras) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeString(provider);
                    _data.writeString(command);
                    boolean _result = true;
                    if (extras != null) {
                        _data.writeInt(1);
                        extras.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    this.mRemote.transact(43, _data, _reply, 0);
                    _reply.readException();
                    if (_reply.readInt() == 0) {
                        _result = false;
                    }
                    if (_reply.readInt() != 0) {
                        extras.readFromParcel(_reply);
                    }
                    _reply.recycle();
                    _data.recycle();
                    return _result;
                } catch (Throwable th) {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportLocation(Location location, boolean passive) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    if (location != null) {
                        _data.writeInt(1);
                        location.writeToParcel(_data, 0);
                    } else {
                        _data.writeInt(0);
                    }
                    _data.writeInt(passive);
                    this.mRemote.transact(44, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void reportLocationBatch(List<Location> locations) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeTypedList(locations);
                    this.mRemote.transact(45, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public void locationCallbackFinished(ILocationListener listener) throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    _data.writeStrongBinder(listener != null ? listener.asBinder() : null);
                    this.mRemote.transact(46, _data, _reply, 0);
                    _reply.readException();
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }

            public String[] getBackgroundThrottlingWhitelist() throws RemoteException {
                Parcel _data = Parcel.obtain();
                Parcel _reply = Parcel.obtain();
                try {
                    _data.writeInterfaceToken(Stub.DESCRIPTOR);
                    this.mRemote.transact(47, _data, _reply, 0);
                    _reply.readException();
                    String[] _result = _reply.createStringArray();
                    return _result;
                } finally {
                    _reply.recycle();
                    _data.recycle();
                }
            }
        }

        public Stub() {
            attachInterface(this, DESCRIPTOR);
        }

        public static ILocationManager asInterface(IBinder obj) {
            if (obj == null) {
                return null;
            }
            IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
            if (iin == null || !(iin instanceof ILocationManager)) {
                return new Proxy(obj);
            }
            return (ILocationManager) iin;
        }

        public IBinder asBinder() {
            return this;
        }

        public boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            int i = code;
            Parcel parcel = data;
            Parcel parcel2 = reply;
            String descriptor = DESCRIPTOR;
            boolean z;
            if (i != 1598968902) {
                boolean _arg1 = false;
                PendingIntent _arg12 = null;
                Parcel parcel3;
                LocationRequest _arg0;
                boolean _result;
                String _result2;
                boolean _result3;
                Location _arg02;
                boolean _result4;
                Criteria _arg03;
                String _result5;
                String _arg04;
                switch (i) {
                    case 1:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (LocationRequest) LocationRequest.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg0 = null;
                        }
                        ILocationListener _arg13 = android.location.ILocationListener.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        requestLocationUpdates(_arg0, _arg13, _arg12, data.readString());
                        reply.writeNoException();
                        return z;
                    case 2:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        ILocationListener _arg05 = android.location.ILocationListener.Stub.asInterface(data.readStrongBinder());
                        if (data.readInt() != 0) {
                            _arg12 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        removeUpdates(_arg05, _arg12, data.readString());
                        reply.writeNoException();
                        return z;
                    case 3:
                        Geofence _arg14;
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg0 = (LocationRequest) LocationRequest.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg0 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg14 = (Geofence) Geofence.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg14 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg12 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        requestGeofence(_arg0, _arg14, _arg12, data.readString());
                        reply.writeNoException();
                        return z;
                    case 4:
                        Geofence _arg06;
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg06 = (Geofence) Geofence.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg06 = null;
                        }
                        if (data.readInt() != 0) {
                            _arg12 = (PendingIntent) PendingIntent.CREATOR.createFromParcel(parcel);
                        }
                        removeGeofence(_arg06, _arg12, data.readString());
                        reply.writeNoException();
                        return z;
                    case 5:
                        LocationRequest _arg07;
                        i = 1;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg07 = (LocationRequest) LocationRequest.CREATOR.createFromParcel(parcel);
                        }
                        Location _result6 = getLastLocation(_arg07, data.readString());
                        reply.writeNoException();
                        if (_result6 != null) {
                            parcel3.writeInt(i);
                            _result6.writeToParcel(parcel3, i);
                        } else {
                            parcel3.writeInt(0);
                        }
                        return i;
                    case 6:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        _result = registerGnssStatusCallback(android.location.IGnssStatusListener.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        parcel3.writeInt(_result);
                        return z;
                    case 7:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        unregisterGnssStatusCallback(android.location.IGnssStatusListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return z;
                    case 8:
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        _arg1 = geocoderIsPresent();
                        reply.writeNoException();
                        parcel3.writeInt(_arg1);
                        return z;
                    case 9:
                        GeocoderParams _arg3;
                        z = true;
                        parcel3 = parcel2;
                        parcel.enforceInterface(descriptor);
                        descriptor = data.readDouble();
                        double _arg15 = data.readDouble();
                        int _arg2 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg3 = (GeocoderParams) GeocoderParams.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg3 = null;
                        }
                        List<Address> _arg4 = new ArrayList();
                        List<Address> _arg42 = _arg4;
                        _result2 = getFromLocation(descriptor, _arg15, _arg2, _arg3, _arg4);
                        reply.writeNoException();
                        parcel3.writeString(_result2);
                        parcel3.writeTypedList(_arg42);
                        return z;
                    case 10:
                        GeocoderParams _arg6;
                        parcel.enforceInterface(descriptor);
                        String _arg08 = data.readString();
                        double _arg16 = data.readDouble();
                        double _arg22 = data.readDouble();
                        double _arg32 = data.readDouble();
                        double _arg43 = data.readDouble();
                        int _arg5 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg6 = (GeocoderParams) GeocoderParams.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg6 = null;
                        }
                        z = true;
                        List<Address> _arg7 = new ArrayList();
                        _result2 = getFromLocationName(_arg08, _arg16, _arg22, _arg32, _arg43, _arg5, _arg6, _arg7);
                        reply.writeNoException();
                        parcel3 = reply;
                        parcel3.writeString(_result2);
                        parcel3.writeTypedList(_arg7);
                        return z;
                    case 11:
                        parcel.enforceInterface(descriptor);
                        _result = sendNiResponse(data.readInt(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 12:
                        parcel.enforceInterface(descriptor);
                        _result = addGnssMeasurementsListener(android.location.IGnssMeasurementsListener.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 13:
                        parcel.enforceInterface(descriptor);
                        removeGnssMeasurementsListener(android.location.IGnssMeasurementsListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 14:
                        parcel.enforceInterface(descriptor);
                        _result = addGnssNavigationMessageListener(android.location.IGnssNavigationMessageListener.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 15:
                        parcel.enforceInterface(descriptor);
                        removeGnssNavigationMessageListener(android.location.IGnssNavigationMessageListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 16:
                        parcel.enforceInterface(descriptor);
                        int _result7 = getGnssYearOfHardware();
                        reply.writeNoException();
                        parcel2.writeInt(_result7);
                        return true;
                    case 17:
                        parcel.enforceInterface(descriptor);
                        _result2 = getGnssHardwareModelName();
                        reply.writeNoException();
                        parcel2.writeString(_result2);
                        return true;
                    case 18:
                        parcel.enforceInterface(descriptor);
                        int _result8 = getGnssBatchSize(data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result8);
                        return true;
                    case 19:
                        parcel.enforceInterface(descriptor);
                        _result = addGnssBatchingCallback(android.location.IBatchedLocationCallback.Stub.asInterface(data.readStrongBinder()), data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 20:
                        parcel.enforceInterface(descriptor);
                        removeGnssBatchingCallback();
                        reply.writeNoException();
                        return true;
                    case 21:
                        parcel.enforceInterface(descriptor);
                        long _arg09 = data.readLong();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _result3 = startGnssBatch(_arg09, _arg1, data.readString());
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        return true;
                    case 22:
                        parcel.enforceInterface(descriptor);
                        flushGnssBatch(data.readString());
                        reply.writeNoException();
                        return true;
                    case 23:
                        parcel.enforceInterface(descriptor);
                        _arg1 = stopGnssBatch();
                        reply.writeNoException();
                        parcel2.writeInt(_arg1);
                        return true;
                    case 24:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (Location) Location.CREATOR.createFromParcel(parcel);
                        }
                        _result4 = injectLocation(_arg02);
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 25:
                        parcel.enforceInterface(descriptor);
                        List<String> _result9 = getAllProviders();
                        reply.writeNoException();
                        parcel2.writeStringList(_result9);
                        return true;
                    case 26:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (Criteria) Criteria.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        List<String> _result10 = getProviders(_arg03, _arg1);
                        reply.writeNoException();
                        parcel2.writeStringList(_result10);
                        return true;
                    case 27:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg03 = (Criteria) Criteria.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        _result5 = getBestProvider(_arg03, _arg1);
                        reply.writeNoException();
                        parcel2.writeString(_result5);
                        return true;
                    case 28:
                        parcel.enforceInterface(descriptor);
                        _result2 = data.readString();
                        if (data.readInt() != 0) {
                            _arg03 = (Criteria) Criteria.CREATOR.createFromParcel(parcel);
                        }
                        _result = providerMeetsCriteria(_result2, _arg03);
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 29:
                        parcel.enforceInterface(descriptor);
                        ProviderProperties _result11 = getProviderProperties(data.readString());
                        reply.writeNoException();
                        if (_result11 != null) {
                            parcel2.writeInt(1);
                            _result11.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 30:
                        parcel.enforceInterface(descriptor);
                        _result2 = getNetworkProviderPackage();
                        reply.writeNoException();
                        parcel2.writeString(_result2);
                        return true;
                    case 31:
                        parcel.enforceInterface(descriptor);
                        _result = isProviderEnabledForUser(data.readString(), data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result);
                        return true;
                    case 32:
                        parcel.enforceInterface(descriptor);
                        _arg04 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        boolean _result12 = setProviderEnabledForUser(_arg04, _arg1, data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result12);
                        return true;
                    case 33:
                        parcel.enforceInterface(descriptor);
                        _result4 = isLocationEnabledForUser(data.readInt());
                        reply.writeNoException();
                        parcel2.writeInt(_result4);
                        return true;
                    case 34:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setLocationEnabledForUser(_arg1, data.readInt());
                        reply.writeNoException();
                        return true;
                    case 35:
                        ProviderProperties _arg17;
                        parcel.enforceInterface(descriptor);
                        _result2 = data.readString();
                        if (data.readInt() != 0) {
                            _arg17 = (ProviderProperties) ProviderProperties.CREATOR.createFromParcel(parcel);
                        }
                        addTestProvider(_result2, _arg17, data.readString());
                        reply.writeNoException();
                        return true;
                    case 36:
                        parcel.enforceInterface(descriptor);
                        removeTestProvider(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 37:
                        parcel.enforceInterface(descriptor);
                        _result2 = data.readString();
                        if (data.readInt() != 0) {
                            _arg02 = (Location) Location.CREATOR.createFromParcel(parcel);
                        }
                        setTestProviderLocation(_result2, _arg02, data.readString());
                        reply.writeNoException();
                        return true;
                    case 38:
                        parcel.enforceInterface(descriptor);
                        clearTestProviderLocation(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 39:
                        parcel.enforceInterface(descriptor);
                        _arg04 = data.readString();
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        setTestProviderEnabled(_arg04, _arg1, data.readString());
                        reply.writeNoException();
                        return true;
                    case 40:
                        parcel.enforceInterface(descriptor);
                        clearTestProviderEnabled(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 41:
                        Bundle _arg23;
                        parcel.enforceInterface(descriptor);
                        String _arg010 = data.readString();
                        int _arg18 = data.readInt();
                        if (data.readInt() != 0) {
                            _arg23 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        } else {
                            _arg23 = null;
                        }
                        setTestProviderStatus(_arg010, _arg18, _arg23, data.readLong(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 42:
                        parcel.enforceInterface(descriptor);
                        clearTestProviderStatus(data.readString(), data.readString());
                        reply.writeNoException();
                        return true;
                    case 43:
                        Bundle _arg24;
                        parcel.enforceInterface(descriptor);
                        _result5 = data.readString();
                        String _arg19 = data.readString();
                        if (data.readInt() != 0) {
                            _arg24 = (Bundle) Bundle.CREATOR.createFromParcel(parcel);
                        }
                        _result3 = sendExtraCommand(_result5, _arg19, _arg24);
                        reply.writeNoException();
                        parcel2.writeInt(_result3);
                        if (_arg24 != null) {
                            parcel2.writeInt(1);
                            _arg24.writeToParcel(parcel2, 1);
                        } else {
                            parcel2.writeInt(0);
                        }
                        return true;
                    case 44:
                        parcel.enforceInterface(descriptor);
                        if (data.readInt() != 0) {
                            _arg02 = (Location) Location.CREATOR.createFromParcel(parcel);
                        }
                        if (data.readInt() != 0) {
                            _arg1 = true;
                        }
                        reportLocation(_arg02, _arg1);
                        reply.writeNoException();
                        return true;
                    case 45:
                        parcel.enforceInterface(descriptor);
                        reportLocationBatch(parcel.createTypedArrayList(Location.CREATOR));
                        reply.writeNoException();
                        return true;
                    case 46:
                        parcel.enforceInterface(descriptor);
                        locationCallbackFinished(android.location.ILocationListener.Stub.asInterface(data.readStrongBinder()));
                        reply.writeNoException();
                        return true;
                    case 47:
                        parcel.enforceInterface(descriptor);
                        String[] _result13 = getBackgroundThrottlingWhitelist();
                        reply.writeNoException();
                        parcel2.writeStringArray(_result13);
                        return true;
                    default:
                        return super.onTransact(code, data, reply, flags);
                }
            }
            z = true;
            parcel2.writeString(descriptor);
            return z;
        }
    }

    boolean addGnssBatchingCallback(IBatchedLocationCallback iBatchedLocationCallback, String str) throws RemoteException;

    boolean addGnssMeasurementsListener(IGnssMeasurementsListener iGnssMeasurementsListener, String str) throws RemoteException;

    boolean addGnssNavigationMessageListener(IGnssNavigationMessageListener iGnssNavigationMessageListener, String str) throws RemoteException;

    void addTestProvider(String str, ProviderProperties providerProperties, String str2) throws RemoteException;

    void clearTestProviderEnabled(String str, String str2) throws RemoteException;

    void clearTestProviderLocation(String str, String str2) throws RemoteException;

    void clearTestProviderStatus(String str, String str2) throws RemoteException;

    void flushGnssBatch(String str) throws RemoteException;

    boolean geocoderIsPresent() throws RemoteException;

    List<String> getAllProviders() throws RemoteException;

    String[] getBackgroundThrottlingWhitelist() throws RemoteException;

    String getBestProvider(Criteria criteria, boolean z) throws RemoteException;

    String getFromLocation(double d, double d2, int i, GeocoderParams geocoderParams, List<Address> list) throws RemoteException;

    String getFromLocationName(String str, double d, double d2, double d3, double d4, int i, GeocoderParams geocoderParams, List<Address> list) throws RemoteException;

    int getGnssBatchSize(String str) throws RemoteException;

    String getGnssHardwareModelName() throws RemoteException;

    int getGnssYearOfHardware() throws RemoteException;

    Location getLastLocation(LocationRequest locationRequest, String str) throws RemoteException;

    String getNetworkProviderPackage() throws RemoteException;

    ProviderProperties getProviderProperties(String str) throws RemoteException;

    List<String> getProviders(Criteria criteria, boolean z) throws RemoteException;

    boolean injectLocation(Location location) throws RemoteException;

    boolean isLocationEnabledForUser(int i) throws RemoteException;

    boolean isProviderEnabledForUser(String str, int i) throws RemoteException;

    void locationCallbackFinished(ILocationListener iLocationListener) throws RemoteException;

    boolean providerMeetsCriteria(String str, Criteria criteria) throws RemoteException;

    boolean registerGnssStatusCallback(IGnssStatusListener iGnssStatusListener, String str) throws RemoteException;

    void removeGeofence(Geofence geofence, PendingIntent pendingIntent, String str) throws RemoteException;

    void removeGnssBatchingCallback() throws RemoteException;

    void removeGnssMeasurementsListener(IGnssMeasurementsListener iGnssMeasurementsListener) throws RemoteException;

    void removeGnssNavigationMessageListener(IGnssNavigationMessageListener iGnssNavigationMessageListener) throws RemoteException;

    void removeTestProvider(String str, String str2) throws RemoteException;

    void removeUpdates(ILocationListener iLocationListener, PendingIntent pendingIntent, String str) throws RemoteException;

    void reportLocation(Location location, boolean z) throws RemoteException;

    void reportLocationBatch(List<Location> list) throws RemoteException;

    void requestGeofence(LocationRequest locationRequest, Geofence geofence, PendingIntent pendingIntent, String str) throws RemoteException;

    void requestLocationUpdates(LocationRequest locationRequest, ILocationListener iLocationListener, PendingIntent pendingIntent, String str) throws RemoteException;

    boolean sendExtraCommand(String str, String str2, Bundle bundle) throws RemoteException;

    boolean sendNiResponse(int i, int i2) throws RemoteException;

    void setLocationEnabledForUser(boolean z, int i) throws RemoteException;

    boolean setProviderEnabledForUser(String str, boolean z, int i) throws RemoteException;

    void setTestProviderEnabled(String str, boolean z, String str2) throws RemoteException;

    void setTestProviderLocation(String str, Location location, String str2) throws RemoteException;

    void setTestProviderStatus(String str, int i, Bundle bundle, long j, String str2) throws RemoteException;

    boolean startGnssBatch(long j, boolean z, String str) throws RemoteException;

    boolean stopGnssBatch() throws RemoteException;

    void unregisterGnssStatusCallback(IGnssStatusListener iGnssStatusListener) throws RemoteException;
}
