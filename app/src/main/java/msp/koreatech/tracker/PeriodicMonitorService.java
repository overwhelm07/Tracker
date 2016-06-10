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
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class PeriodicMonitorService extends Service implements GpsStatus.Listener {
    private static final String TAG = "Tracker";
    private static final int TIMER_INTERVAL = 1000 * 10;
    private static final int GPS_TIMEOUT = 1000 * 5;
    private static final String ACTION_ALARM = "msp.koreatech.tracker.alarm";
    private static final String ACTION_GPS_UPDATE = "msp.koreatech.tracker.gps";
    private static final String ACTION_GPS_PROXIMITY = "msp.koreatech.tracker.gps.proximity";
    private static final String ACTION_GPS_PROXIMITY2 = "msp.koreatech.tracker.gps.proximity2";
    private static final String ACTION_WIFI_UPDATE = "msp.koreatech.tracker.wifi";
    private static final String ACTION_STATUS_UPDATE = "msp.koreatech.tracker.status";

    private Intent intentUpdateGPS;
    private Intent intentUpdateStatus;
    private LocationManager locationManager = null;
    private Location myLastLocation;
    private Location location1;
    private Location location2;
    private WifiManager wifiManager;
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private String stringWifiPlace = "";
    private boolean isRequestRegistered = false;
    private boolean wifiScanning = false;
    private boolean isGPSFix;
    double longitude;
    double latitude;
    private long lastLocationMillis;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String stringAction = intent.getAction();
            if (stringAction.equals(ACTION_ALARM)) {
                Log.d(TAG, "방송 수신: ACTION_ALARM");
                try {
                    Log.d(TAG, "GPS 요청");
                    intentUpdateStatus.putExtra("status", "GPS 요청");
                    sendBroadcast(intentUpdateStatus);
                    locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
                    Timer timer = new Timer();
                    timer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            boolean scanSuccessful;
                            String scanMessage;
                            if (locationManager != null)
                                if (!isGPSFix)
                                    try {
                                        locationManager.removeUpdates(locationListener);
                                        Log.d(TAG, "GPS failed and start Wi-Fi scanning");
                                        wifiScanning = true;
                                        scanSuccessful = wifiManager.startScan();
                                        scanMessage = String.format(Locale.KOREAN, "Wi-Fi scanning: %b", scanSuccessful);
                                        intentUpdateStatus.putExtra("status", scanMessage);
                                        sendBroadcast(intentUpdateStatus);
                                    } catch (SecurityException e) {
                                        e.printStackTrace();
                                    }
                                else {
                                    alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), alarmIntent);
                                }
                        }
                    }, TIMER_INTERVAL);
                } catch (SecurityException e) {
                    e.printStackTrace();
                }
            } else if (stringAction.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                Intent intentScanResults = new Intent(ACTION_WIFI_UPDATE);
                sendBroadcast(intentScanResults);
                intentUpdateStatus.putExtra("status", "SCAN_RESULTS_AVAILABLE_ACTION");
                sendBroadcast(intentUpdateStatus);
                Log.d(TAG, "방송 수신: SCAN_RESULTS_AVAILABLE_ACTION");
                if (!checkProximity() && !stringWifiPlace.equals("")) {
                    Toast.makeText(PeriodicMonitorService.this, stringWifiPlace + "에서 벗어남", Toast.LENGTH_SHORT).show();
                    stringWifiPlace = "";
                }
                wifiScanning = false;
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), alarmIntent);
            } else if (stringAction.equals(ACTION_GPS_PROXIMITY) || stringAction.equals(ACTION_GPS_PROXIMITY2)) {
                boolean isEntering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
                String place = intent.getStringExtra("name");
                if (place == null)
                    place = "";
                if (isEntering)
                    Toast.makeText(PeriodicMonitorService.this, place + "(으)로 접근", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(PeriodicMonitorService.this, place + "에서 벗어남", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location) {
            float accuracy = location.getAccuracy();
            lastLocationMillis = SystemClock.elapsedRealtime();
            // Do something.

            myLastLocation = location;
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            Log.d(TAG, " Time : " + getCurrentTime() + " Longitude : " + longitude
                    + " Latitude : " + latitude + " Altitude: " + location.getAltitude()
                    + " Accuracy : " + location.getAccuracy());
            intentUpdateGPS.putExtra("longitude", longitude);
            intentUpdateGPS.putExtra("latitude", latitude);
            intentUpdateGPS.putExtra("accuracy", accuracy);
            sendBroadcast(intentUpdateGPS);
            try {
                locationManager.removeUpdates(locationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, "GPS status changed: " + status);
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
    public void onGpsStatusChanged(int event) {
        switch (event) {
            case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                if (myLastLocation != null)
                    isGPSFix = (SystemClock.elapsedRealtime() - lastLocationMillis) < 5000;

                if (isGPSFix) { // A fix has been acquired.
                    // Do something.;
                    Log.d(TAG, "isGPSFix: " + isGPSFix);
                } else { // The fix has been lost.
                    // Do something.
                }
                break;
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                // Do something.
                isGPSFix = true;
                break;
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        Toast.makeText(this, "Monitor 시작", Toast.LENGTH_SHORT).show();
        Intent intentAlarm = new Intent(ACTION_ALARM);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_ALARM);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        intentFilter.addAction(ACTION_GPS_PROXIMITY);
        intentFilter.addAction(ACTION_GPS_PROXIMITY2);
        intentUpdateGPS = new Intent(ACTION_GPS_UPDATE);
        intentUpdateStatus = new Intent(ACTION_STATUS_UPDATE);
        registerReceiver(broadcastReceiver, intentFilter);
        location1 = new Location(LocationManager.GPS_PROVIDER);
        location2 = new Location(LocationManager.GPS_PROVIDER);
        location1.setLatitude(36.7613489);
        location1.setLongitude(127.2800892);
        location2.setLatitude(36.7613363);
        location2.setLongitude(127.2799273);
        setupLocationManager();
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000, alarmIntent);
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

    private void setupLocationManager() {
        try {
            if (locationManager == null) {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.addGpsStatusListener(this);
            }
            if (!isRequestRegistered) {
                Intent intent = new Intent(ACTION_GPS_PROXIMITY);
                Intent intent2 = new Intent(ACTION_GPS_PROXIMITY2);
                intent.putExtra("name", "장소1");
                intent2.putExtra("name", "장소2");
                locationManager.addProximityAlert(location1.getLatitude(), location1.getLongitude(), 15, -1, PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT));
                locationManager.addProximityAlert(location2.getLatitude(), location2.getLongitude(), 15, -1, PendingIntent.getBroadcast(this, 0, intent2, PendingIntent.FLAG_UPDATE_CURRENT));
                isRequestRegistered = true;
            }
        } catch (SecurityException se) {
            se.printStackTrace();
            Log.e(TAG, "PERMISSION_NOT_GRANTED");
        }
    }

    private void cancelLocationRequest() {
        Log.d(TAG, "Cancel the location update request");
        try {
            locationManager.removeUpdates(locationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }
        locationManager = null;
        isRequestRegistered = false;
    }

    private boolean checkProximity() {
        List<ScanResult> scanList = wifiManager.getScanResults();
        HashMap<String, Integer> hashPlace1 = new HashMap<>();
        int countPlace1 = 0;
        hashPlace1.put("50:1c:bf:5f:7c:ef", -46);
        hashPlace1.put("50:1c:bf:5f:7c:ee", -47);
        hashPlace1.put("90:9f:33:59:2a:88", -49);

        intentUpdateStatus.putExtra("status", "checkProximity");
        sendBroadcast(intentUpdateStatus);

        for (int i = 1; i < scanList.size(); i++) {
            ScanResult result = scanList.get(i);
            Log.d(TAG, "SSID: " + result.SSID + ", BSSID: " + result.BSSID + ", RSSI: " + result.level);
            Integer value = null;
            if (hashPlace1.containsKey(result.BSSID))
                value = hashPlace1.get(result.BSSID);
            if (value != null && Math.abs(value - result.level) <= 20)
                countPlace1++;
        }

        if ((countPlace1 >= 2)) {
            stringWifiPlace = "Wi-Fi 장소1";
            Toast.makeText(PeriodicMonitorService.this, stringWifiPlace + "으로 접근", Toast.LENGTH_SHORT).show();
            return true;
        }

        return false;
    }

    public static String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA);
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }

}
