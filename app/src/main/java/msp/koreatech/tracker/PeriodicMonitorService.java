package msp.koreatech.tracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.os.Handler;

public class PeriodicMonitorService extends Service  {
    private static final String TAG = "Tracker";
    private static final int ALARM_INTERVAL = 1000 * 10;
    private static final int GPS_TIMEOUT = 1000 * 5;
    private static final String ACTION_ALARM = "msp.koreatech.tracker.alarm";
    private static final String ACTION_GPS_UPDATE = "msp.koreatech.tracker.gps";
    private static final String ACTION_GPS_PROXIMITY = "msp.koreatech.tracker.gps.proximity";
    private static final String ACTION_WIFI_UPDATE = "msp.koreatech.tracker.wifi";

    private LocationManager locationManager = null;
    private Intent intentUpdateGPS;
    private WifiManager wifiManager;
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private boolean isRequestRegistered = false;
    private boolean wifiScanning = false;
    double longitude;
    double latitude;
    private Location location1;
    private Location location2;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String stringAction = intent.getAction();
            Looper looper = Looper.myLooper();
            final Handler handler = new Handler(looper);
            handler.postDelayed(runnableRemoveUpdates, GPS_TIMEOUT);
            if(stringAction.equals(ACTION_ALARM)) {
                Log.d(TAG, "방송 수신: ACTION_ALARM");
                try {
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, looper);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
            if(stringAction.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Intent intentScanResults = new Intent(ACTION_WIFI_UPDATE);
                sendBroadcast(intentScanResults);
                Log.d(TAG, "방송 수신: SCAN_RESULTS_AVAILABLE_ACTION");
                checkProximity();
                wifiScanning = false;
            }

            if(stringAction.equals(ACTION_GPS_PROXIMITY)) {
                String place = "";
                if(latitude == location1.getLatitude() && longitude == location1.getLongitude())
                    place = "장소1";
                else if(latitude == location2.getLatitude() && longitude == location2.getLongitude())
                    place = "장소2";
                Toast.makeText(PeriodicMonitorService.this, place + "로 접근", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private Runnable runnableRemoveUpdates = new Runnable() {
        public void run() {
            if(locationManager != null && locationListener != null) {
                if (locationManager.getGpsStatus(null).getTimeToFirstFix() == 0 && !wifiScanning) {
                    wifiScanning = true;
                    wifiManager.startScan();   //Wi-Fi
                    Log.d(TAG, "start Wi-Fi scanning");
                }else
                    Log.d(TAG, "TTFF: " + locationManager.getGpsStatus(null).getTimeToFirstFix() + ", scanning: " + wifiScanning);
                try {
                    locationManager.removeUpdates(locationListener);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            }
            else
                Log.d(TAG, "locationManager or locationListener problem");
        }
    };

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            float accuracy = location.getAccuracy();

            longitude = location.getLongitude();
            latitude = location.getLatitude();
            Log.d(TAG, " Time : " + getCurrentTime() + " Longitude : " + longitude
                    + " Latitude : " + latitude + " Altitude: " + location.getAltitude()
                    + " Accuracy : " + location.getAccuracy());
            intentUpdateGPS.putExtra("longitude", longitude);
            intentUpdateGPS.putExtra("latitude", latitude);
            intentUpdateGPS.putExtra("accuracy", accuracy);
            sendBroadcast(intentUpdateGPS);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "GPS status changed.");
            //Toast.makeText(getApplicationContext(), status + ": GPS status changed.", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "GPS onProviderEnabled: " + provider);
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "GPS onProviderDisabled: " + provider);
            Toast.makeText(PeriodicMonitorService.this, "GPS is off, please turn on!", Toast.LENGTH_LONG).show();
        }
    };

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        Toast.makeText(this, "Monitor 시작", Toast.LENGTH_SHORT).show();

        Intent intentAlarm =  new Intent(ACTION_ALARM);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_ALARM);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(ACTION_GPS_PROXIMITY);
        intentUpdateGPS = new Intent(ACTION_GPS_UPDATE);
        registerReceiver(broadcastReceiver, intentFilter);
        location1 = new Location(LocationManager.GPS_PROVIDER);
        location2 = new Location(LocationManager.GPS_PROVIDER);
        location1.setLatitude(36.7614147);
        location1.setLongitude(127.2800555);
        location2.setLatitude(36.7618739);
        location2.setLongitude(127.2798745);
        requestLocation();
        wifiManager = (WifiManager)getSystemService(WIFI_SERVICE);
        alarmManager = (AlarmManager)this.getSystemService(Context.ALARM_SERVICE);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intentAlarm, 0);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                ALARM_INTERVAL, alarmIntent);

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
        Log.d(TAG, "onDestroy");
        Toast.makeText(this, "Monitor 중지", Toast.LENGTH_SHORT).show();
        unregisterReceiver(broadcastReceiver);
        alarmManager.cancel(alarmIntent);
        cancelLocationRequest();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private void requestLocation() {
        try {
            if(locationManager == null) {
                Log.d(TAG, "LocationManager obtained");
                locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
            }
            if(!isRequestRegistered) {
                /*locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                        MIN_TIME_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        locationListener);*/
                Intent intent = new Intent(ACTION_GPS_PROXIMITY);
                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
                locationManager.addProximityAlert(location1.getLatitude(), location1.getLongitude(), 10, -1, pendingIntent);
                locationManager.addProximityAlert(location2.getLatitude(), location2.getLongitude(), 10, -1, pendingIntent);
                isRequestRegistered = true;
            }
        } catch (SecurityException se) {
            se.printStackTrace();
            Log.e(TAG, "PERMISSION_NOT_GRANTED");
        }
    }

    private void cancelLocationRequest() {
        Log.d(TAG, "Cancel the location update request");
        runnableRemoveUpdates.run();
        locationManager = null;
        isRequestRegistered = false;
    }

    private void checkProximity() {
        List<ScanResult> scanList = wifiManager.getScanResults();
        boolean isProximate = false;

        for(int i = 1; i < scanList.size(); i++) {
            ScanResult result = scanList.get(i);
            Log.d(TAG, "SSID: " + result.SSID + ", BSSID: " + result.BSSID + ", RSSI: " + result.level );
            //if(top1APId.equals(result.SSID+result.BSSID) && result.level > top1rssi - 10)    //로직
            if(true)
                isProximate = true;
        }

        if(isProximate) {
            //장소 진입
        } else {
            //장소 진입 X
        }
    }

    public static String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA);
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }
}
