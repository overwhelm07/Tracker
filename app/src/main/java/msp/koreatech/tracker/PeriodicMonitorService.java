package msp.koreatech.tracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class PeriodicMonitorService extends Service implements GpsStatus.Listener{
    private static final String LOGTAG = "Tracker";
    private LocationManager locationManager = null;
    private GpsStatus gpsStatus;
    private final static int MIN_TIME_UPDATES = 5000; // milliseconds
    private final static int MIN_DISTANCE_CHANGE_FOR_UPDATES = 5; // m
    private boolean isRequestRegistered = false;
    private double lon = 0.0;
    private double lat = 0.0;

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(LOGTAG, " Time : " + getCurrentTime() + " Longitude : " + location.getLongitude()
                    + " Latitude : " + location.getLatitude() + " Altitude: " + location.getAltitude()
                    + " Accuracy : " + location.getAccuracy());
            lon = location.getLongitude();
            lat = location.getLatitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(LOGTAG, "GPS status changed.");
            Toast.makeText(getApplicationContext(), "GPS status changed.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(LOGTAG, "GPS onProviderEnabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(LOGTAG, "GPS onProviderDisabled: " + provider);
            Toast.makeText(getApplicationContext(), "GPS is off, please turn on!", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onCreate() {
        Log.d(LOGTAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id

        Log.d(LOGTAG, "onStartCommand");
        Toast.makeText(this, "Monitor 시작", Toast.LENGTH_SHORT).show();
        requestLocation();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Monitor 중지", Toast.LENGTH_SHORT).show();
        cancelLocationRequest();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void requestLocation() {
        try {
            if(locationManager == null) {
                Log.d(LOGTAG, "LocationManager obtained");
                locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                locationManager.addGpsStatusListener(this);
            }
            if(!isRequestRegistered) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        locationListener);
                isRequestRegistered = true;
            }
        } catch (SecurityException se) {
            se.printStackTrace();
            Log.e(LOGTAG, "PERMISSION_NOT_GRANTED");
        }
    }

    private void cancelLocationRequest() {
        Log.d(LOGTAG, "Cancel the location update request");
        if(locationManager != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch(SecurityException se) {
                se.printStackTrace();
                Log.e(LOGTAG, "PERMISSION_NOT_GRANTED");
            }
        }
        locationManager = null;
        isRequestRegistered = false;
    }

    public String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA);
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }

    @Override
    public void onGpsStatusChanged(int event) {
        gpsStatus = locationManager.getGpsStatus(gpsStatus);
        switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                Log.d(LOGTAG, "GPS_EVENT_STARTED");
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                Log.d(LOGTAG, "GPS_EVENT_STOPPED");
                break;

            case GpsStatus.GPS_EVENT_FIRST_FIX:
                Log.d(LOGTAG, "GPS_EVENT_FIRST_FIX");
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                Log.d(LOGTAG, "GPS_EVENT_SATELLITE_STATUS");
                break;

        }
    }
}
