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
    private static final int TIMER_DELAY = 1000 * 10;
    private static final String ACTION_ALARM_IN_OR_OUT = "msp.koreatech.tracker.alarm";
    private static final String ACTION_GPS_UPDATE = "msp.koreatech.tracker.gps";
    private static final String ACTION_GPS_PROXIMITY = "msp.koreatech.tracker.gps.proximity";
    private static final String ACTION_GPS_PROXIMITY2 = "msp.koreatech.tracker.gps.proximity2";
    private static final String ACTION_GPS_PROXIMITY_SET = "msp.koreatech.tracker.gps.proximity.set";
    private static final String ACTION_WIFI_UPDATE = "msp.koreatech.tracker.wifi";
    private static final String ACTION_STATUS_UPDATE = "msp.koreatech.tracker.status";

    private Intent intentUpdateGPS;
    private Intent intentUpdateStatus;
    private LocationManager locationManager = null;
    private WifiManager wifiManager;
    private AlarmManager alarmManager;
    private PendingIntent alarmIntent;
    private PendingIntent intentGPSProximity1;
    private PendingIntent intentGPSProximity2;
    private Timer timerGPSTimeout;    ////TIMER_DELAY 만큼의 시간이 지나고 발생
    private int statusInOrOut = 0;  //0: 기본값, 1: 실외, 2: 실내
    private String stringGPSPlace = "";     //현위치 (실외)
    private String stringWifiPlace = "";    //현위치 (실내)
    private boolean isRequestRegistered = false;
    private boolean isGPSFix;
    private boolean isSensingGPS = false;
    double longitude;
    double latitude;
    float accuracy;

    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String stringAction = intent.getAction();
            statusInOrOut = 0;  //초기화
            switch (stringAction) {
                case ACTION_ALARM_IN_OR_OUT:  //알람이 발생하면 GPS 업데이트 요청을 하고 GPS 를 이용할 수 없으면 Wi-Fi 스캔을 실시한다.
                    Log.d(TAG, "방송 수신: ACTION_ALARM_IN_OR_OUT");
                    try {
                        Log.d(TAG, "GPS 요청");
                        intentUpdateStatus.putExtra("status", "GPS 요청");
                        sendBroadcast(intentUpdateStatus);
                        isGPSFix = false;
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, locationListener);
                        isSensingGPS = true;
                        timerGPSTimeout = new Timer();
                        timerGPSTimeout.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                boolean scanSuccessful;
                                String scanMessage;
                                if (locationManager != null)
                                    if (!isGPSFix)  //GPS 업데이트가 이루어지지 않았을 경우 GPS 업데이트 요청을 중단하고 Wi-Fi 스캔을 실시한다.
                                        try {
                                            locationManager.removeUpdates(locationListener);
                                            isSensingGPS = false;
                                            Log.d(TAG, "Wi-Fi 스캔");
                                            scanSuccessful = wifiManager.startScan();
                                            scanMessage = String.format(Locale.KOREAN, "Wi-Fi scanning: %b", scanSuccessful);
                                            intentUpdateStatus.putExtra("status", scanMessage);
                                            sendBroadcast(intentUpdateStatus);
                                        } catch (SecurityException e) {
                                            e.printStackTrace();
                                        }
                            }
                        }, TIMER_DELAY);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
                    break;
                case WifiManager.SCAN_RESULTS_AVAILABLE_ACTION:
                    Log.d(TAG, "방송 수신: SCAN_RESULTS_AVAILABLE_ACTION");
                    if(!isSensingGPS) {
                        checkWifiProximity();
                        statusInOrOut = 2;  //실외
                        Intent intentScanResults = new Intent(ACTION_WIFI_UPDATE);  //(디버깅용) 스캔이 완료됐음을 알린다
                        sendBroadcast(intentScanResults);
                        intentUpdateStatus.putExtra("status", "");
                        sendBroadcast(intentUpdateStatus);
                    }
                    break;
                case ACTION_GPS_PROXIMITY:  //
                case ACTION_GPS_PROXIMITY2:
                    checkGPSProximity(intent);
                    break;
                case ACTION_GPS_PROXIMITY_SET:
                    int flag = intent.getIntExtra("flag", 0);
                    setGPSProximityAlert(flag);
                    break;
            }
        }
    };

    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location)    //현재 위치가 변했음을 알린다.
        {
            timerGPSTimeout.cancel();
            longitude = location.getLongitude();
            latitude = location.getLatitude();
            accuracy = location.getAccuracy();
            intentUpdateGPS.putExtra("longitude", longitude);
            intentUpdateGPS.putExtra("latitude", latitude);
            intentUpdateGPS.putExtra("accuracy", accuracy);
            statusInOrOut = 1;  //실내
            sendBroadcast(intentUpdateGPS);
            try {
                locationManager.removeUpdates(locationListener);
                isSensingGPS = false;
            } catch (SecurityException e) {
                e.printStackTrace();
            }
            intentUpdateStatus.putExtra("status", "");
            sendBroadcast(intentUpdateStatus);
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
    public void onGpsStatusChanged(int event)   /*GPS 매니저로 위치를 제공하는 각 위성에 타임아웃을 건다
            하나라도 도착하는 신호가 있으면 isGPSFix 플래그를 참으로 바꾼다.*/
    {
        switch (event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                isGPSFix = true;
                break;
        }
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        Toast.makeText(this, "Monitor 시작", Toast.LENGTH_SHORT).show();
        Intent intentAlarm = new Intent(ACTION_ALARM_IN_OR_OUT);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_ALARM_IN_OR_OUT);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);  //Wi-Fi 스캔이 완료됐을 때 발생하는 이벤트
        intentFilter.addAction(ACTION_GPS_PROXIMITY);   //GPS 접근 알림1
        intentFilter.addAction(ACTION_GPS_PROXIMITY2);  //GPS 접근 알림2
        intentFilter.addAction(ACTION_GPS_PROXIMITY_SET);   //(디버깅용) GPS 접근 등록
        intentUpdateGPS = new Intent(ACTION_GPS_UPDATE);    //(디버깅용) 액티비티에 GPS 변화 알림
        intentUpdateStatus = new Intent(ACTION_STATUS_UPDATE);  //(디버깅용) 상태 변화 알림
        registerReceiver(broadcastReceiver, intentFilter);
        setupLocationManager();
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        alarmManager = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarmIntent = PendingIntent.getBroadcast(this, 0, intentAlarm, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime() + 1000, alarmIntent); //실내외 확인 알람
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

    /*LocationManager 서비스를 등록하고 특정 장소에 Proximity Alert 를 설정한다.*/
    private void setupLocationManager() {
        try {
            if (locationManager == null) {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.addGpsStatusListener(this);
            }
            if (!isRequestRegistered) {

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

    /*실외에서 지정된 장소로 접근하는지 확인*/
    public void checkGPSProximity(Intent intent) {
        boolean isEntering = intent.getBooleanExtra(LocationManager.KEY_PROXIMITY_ENTERING, false);
        stringGPSPlace= intent.getStringExtra("name");
        if (stringGPSPlace == null)
            stringGPSPlace = "";
        if (isEntering)
            Toast.makeText(PeriodicMonitorService.this, "실외: "  + stringGPSPlace + "(으)로 접근", Toast.LENGTH_SHORT).show();
        else
            Toast.makeText(PeriodicMonitorService.this, "실외: "  + stringGPSPlace + "에서 벗어남", Toast.LENGTH_SHORT).show();
    }

    /* 실내에서 지정된 장소로 접근하는지 확인
    * 와이파이 스캔 결과를 배열로 받아서 배열 요소 중에
    * 지정된 장소에 등록된 AP가 2개 이상 있고 RSSI 신호 차이가 20 이내에 있으면
    * 등록된 장소로 접근한 것으로 간주한다.*/
    private void checkWifiProximity() {
        List<ScanResult> scanList = wifiManager.getScanResults();
        HashMap<String, Integer> hashPlace1 = new HashMap<>();
        HashMap<String, Integer> hashPlace2 = new HashMap<>();
        int countPlace1 = 0;
        int countPlace2 = 0;
        boolean isApproaching = false;

        hashPlace1.put("50:1c:bf:5f:7c:ef", -46);
        hashPlace1.put("50:1c:bf:5f:7c:ee", -47);
        hashPlace1.put("90:9f:33:59:2a:88", -49);
        hashPlace2.put("50:1c:bf:5b:2c:c0", -46);
        hashPlace2.put("50:1c:bf:5f:14:71", -47);
        hashPlace2.put("00:26:66:cc:e3:8c", -49);

        for (int i = 1; i < scanList.size(); i++)   //일단 BSSID 가 일치하면 RSSI 를 비교한다.
        {
            ScanResult result = scanList.get(i);
            Log.d(TAG, "SSID: " + result.SSID + ", BSSID: " + result.BSSID + ", RSSI: " + result.level);
            Integer value = null;
            if (hashPlace1.containsKey(result.BSSID))
                value = hashPlace1.get(result.BSSID);
            if (value != null && Math.abs(value - result.level) <= 20)
                countPlace1++;
            if (hashPlace2.containsKey(result.BSSID))
                value = hashPlace1.get(result.BSSID);
            if (value != null && Math.abs(value - result.level) <= 20)
                countPlace2++;
        }
        if ((countPlace1 >= 2)) {
            stringWifiPlace = "실내 장소 1";
            isApproaching = true;
        }
        if((countPlace2 >= 2)) {
            stringWifiPlace = "실내 장소 2";
            isApproaching = true;
        }

        if (isApproaching) //실내에서 등록된 장소로부터 벗어나는 경우
            Toast.makeText(PeriodicMonitorService.this, "실내: "  + stringWifiPlace + "으로 접근", Toast.LENGTH_SHORT).show();
        if(!isApproaching && !stringWifiPlace.equals(""))
        {
            Toast.makeText(PeriodicMonitorService.this, "실내: "  + stringWifiPlace + "에서 벗어남", Toast.LENGTH_SHORT).show();
            stringWifiPlace = "";
        }
    }

    public void setGPSProximityAlert(int flag) throws SecurityException{
        Location location1;
        Location location2;

        if(intentGPSProximity1 != null && intentGPSProximity2 != null) {
            locationManager.removeProximityAlert(intentGPSProximity1);
            locationManager.removeProximityAlert(intentGPSProximity2);
        }
        Intent intent = new Intent(ACTION_GPS_PROXIMITY);
        Intent intent2 = new Intent(ACTION_GPS_PROXIMITY2);
        intent.putExtra("name", "야외 장소 1");
        intent2.putExtra("name", "야외 장소 2");
        location1 = new Location(LocationManager.GPS_PROVIDER);
        location2 = new Location(LocationManager.GPS_PROVIDER);
        if (flag == 0) {
            location1.setLatitude(36.7614271);
            location1.setLongitude(127.2801367);
            location2.setLatitude(36.7612948);
            location2.setLongitude(127.2803239);
        }
        else {
            location1.setLatitude(latitude);
            location1.setLongitude(longitude);
            location2.setLatitude(36.7612948);
            location2.setLongitude(127.2803239);
        }
        intentGPSProximity1 =  PendingIntent.getBroadcast(this, 0, intent, 0);
        intentGPSProximity2 =  PendingIntent.getBroadcast(this, 0, intent2, 0);
        locationManager.addProximityAlert(location1.getLatitude(), location1.getLongitude(), 10, -1, intentGPSProximity1);
        locationManager.addProximityAlert(location2.getLatitude(), location2.getLongitude(), 10, -1, intentGPSProximity2);
    }

    //현재 시간을 받아오는 함수
    public static String getCurrentTime() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.KOREA);
        Date currentTime = new Date();
        return formatter.format(currentTime);
    }
}
