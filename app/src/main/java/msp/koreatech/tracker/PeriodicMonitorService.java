package msp.koreatech.tracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.os.Handler;

public class PeriodicMonitorService extends Service implements GpsStatus.Listener {
    private static final String TAG = "Tracker";
    private static final int ALARM_INTERVAL = 1000 * 5;
    private static final int GPS_TIMEOUT = 1000 * 5;
    private static final String ACTION_ALARM = "msp.koreatech.tracker.alarm";
    private static final String ACTION_GPS_UPDATE = "msp.koreatech.tracker.gps";
    private LocationManager locationManager = null;
    private GpsStatus gpsStatus;
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private Looper looper;
    private boolean isRequestRegistered = false;
    private double longitude = 0.0;
    private double latitude = 0.0;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(ACTION_ALARM)) {
                Log.d(TAG, "방송");
                if(gpsStatus != null && gpsStatus.getTimeToFirstFix() == 0)
                    Log.d(TAG, "GPS를 이용할 수 없음");
                else {
                    Intent intentMain = new Intent(context, MainActivity.class);
                    intentMain.setAction(ACTION_GPS_UPDATE);
                    intentMain.putExtra("longitude", longitude);
                    intentMain.putExtra("latitude", latitude);
                    sendBroadcast(intentMain);
                }
                try {
                    locationManager.removeUpdates(locationListener);
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }
    };


    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            Log.d(TAG, " Time : " + getCurrentTime() + " Longitude : " + location.getLongitude()
                    + " Latitude : " + location.getLatitude() + " Altitude: " + location.getAltitude()
                    + " Accuracy : " + location.getAccuracy());
            longitude = location.getLongitude();
            latitude = location.getLatitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "GPS status changed.");
            Toast.makeText(getApplicationContext(), "GPS status changed.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "GPS onProviderEnabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "GPS onProviderDisabled: " + provider);
            Toast.makeText(getApplicationContext(), "GPS is off, please turn on!", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        Intent intent =  new Intent(ACTION_ALARM);
        IntentFilter intentFilter = new IntentFilter(ACTION_ALARM);
        registerReceiver(broadcastReceiver, intentFilter);  Toast.makeText(this, "Monitor 시작", Toast.LENGTH_SHORT).show();
        requestLocation();
        alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
        alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(),
                ALARM_INTERVAL, alarmIntent);
        looper = Looper.myLooper();
        final Handler handler = new Handler(looper);
        handler.postDelayed(new Runnable() {
            public void run() {
                try {
                    locationManager.removeUpdates(locationListener);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
        }, GPS_TIMEOUT);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id

        Log.d(TAG, "onStartCommand");


        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Monitor 중지", Toast.LENGTH_SHORT).show();
        cancelLocationRequest();
        unregisterReceiver(broadcastReceiver);
        alarmManager.cancel(alarmIntent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onGpsStatusChanged(int event) {
        if(locationManager != null)
            gpsStatus = locationManager.getGpsStatus(null);
        /*switch (event) {
            case GpsStatus.GPS_EVENT_STARTED:
                Log.d(TAG, "GPS_EVENT_STARTED");
                break;

            case GpsStatus.GPS_EVENT_STOPPED:
                Log.d(TAG, "GPS_EVENT_STOPPED");
                break;

            case GpsStatus.GPS_EVENT_FIRST_FIX:
                Log.d(TAG, "GPS_EVENT_FIRST_FIX");
                break;

            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                Log.d(TAG, "GPS_EVENT_SATELLITE_STATUS");
                break;
        }
        Log.d( TAG, "TimeToFirstFix: " +  gpsStatus.getTimeToFirstFix() );
        */
    }

    private void requestLocation() {
        try {
            if(locationManager == null) {
                Log.d(TAG, "LocationManager obtained");
                locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
                locationManager.addGpsStatusListener(this);
            }
            if(!isRequestRegistered) {
                /*locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        locationListener);*/
                locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                isRequestRegistered = true;
            }
        } catch (SecurityException se) {
            se.printStackTrace();
            Log.e(TAG, "PERMISSION_NOT_GRANTED");
        }
    }

    private void cancelLocationRequest() {
        Log.d(TAG, "Cancel the location update request");
        if(locationManager != null) {
            try {
                locationManager.removeUpdates(locationListener);
            } catch(SecurityException se) {
                se.printStackTrace();
                Log.e(TAG, "PERMISSION_NOT_GRANTED");
            }
        }
        locationManager = null;
        isRequestRegistered = false;
    }

    public static String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA);
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }
}
