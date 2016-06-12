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
import android.os.CountDownTimer;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class PeriodicMonitorService extends Service implements GpsStatus.Listener{
    private static final String LOGTAG = "HS_Location_Tracking";
    private static final String BROADCAST_ACTION_ACTIVITY = "msp.tracker";
    private static final String BROADCAST_ACTION_ALARM = "msp.alarm";
    private static final String BROADCAST_ACTION_LIVESTEP = "msp.tracker.step";
    private static final String ACTION_ALARM_IN_OR_OUT = "msp.koreatech.tracker.alarm";
    private static final String ACTION_GPS_UPDATE = "msp.koreatech.tracker.gps";
    private static final String ACTION_GPS_PROXIMITY = "msp.koreatech.tracker.gps.proximity";
    private static final String ACTION_GPS_PROXIMITY2 = "msp.koreatech.tracker.gps.proximity2";
    private static final String ACTION_GPS_PROXIMITY_SET = "msp.koreatech.tracker.gps.proximity.set";
    private static final String ACTION_WIFI_UPDATE = "msp.koreatech.tracker.wifi";
    private static final String ACTION_STATUS_UPDATE = "msp.koreatech.tracker.status";
    private static final String TAG = "Tracker";
    private static final int TIMER_GPS_DELAY = 3500;
    private static final int TIMER_WIFI_DELAY = 1000 * 5;
    private AlarmManager am;
    private PendingIntent pendingIntent;
    private PowerManager.WakeLock wakeLock;
    private CountDownTimer timer;
    private StepMonitor accelMonitor;
    private long period = 5000;
    private static final long activeTime = 1000;
    private static final long periodForMoving = 1000;//1초
    private static final long periodIncrement = 5000;//정지시 주기 5초 증가
    private static final long periodMax = 10000;//최대 max 20

    private long stepCount = 0, stepCountTV = 0;
    private int secCount = 0, secCount2 = 0;//secCount는 이동할때의 초카운트 secCount2는 정지할때 초카운트
    private boolean keepMoving = false, keepStop = false;
    private boolean isEnd = false, isSetTime = false, isSetTime2 = false;
    private ListViewItem info;

    private Intent intentUpdateGPS;
    private Intent intentUpdateStatus;
    private LocationManager locationManager = null;
    private WifiManager wifiManager;
    private PendingIntent alarmIntent;
    private PendingIntent intentGPSProximity1;
    private PendingIntent intentGPSProximity2;
    private Timer timerGPSTimeout;    ////TIMER_DELAY 만큼의 시간이 지나고 발생
    private Timer timerWifiTimeout;    ////TIMER_DELAY 만큼의 시간이 지나고 발생
    private int statusInOrOut = 0;  //0: 기본값, 1: 실외, 2: 실내
    private String stringGPSPlace = "";     //현위치 (실외)
    private String stringWifiPlace;    //현위치 (실내)
    private boolean isRequestRegistered = false;
    private boolean isGPSFix;
    private boolean isSensingGPS = false;
    private boolean isSensingWifi = false;
    private boolean scanSuccessful = false;
    double longitude;
    double latitude;
    float accuracy;

    // Alarm 시간이 되었을 때 안드로이드 시스템이 전송해주는 broadcast를 받을 receiver 정의
    // 그리고 다시 동일 시간 후 alarm이 발생하도록 설정한다.
    private BroadcastReceiver AlarmReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BROADCAST_ACTION_ALARM)) {
                Log.d(LOGTAG, "Alarm fired!!!!");
                //-----------------
                // Alarm receiver에서는 장시간에 걸친 연산을 수행하지 않도록 한다
                // Alarm을 발생할 때 안드로이드 시스템에서 wakelock을 잡기 때문에 CPU를 사용할 수 있지만
                // 그 시간은 제한적이기 때문에 애플리케이션에서 필요하면 wakelock을 잡아서 연산을 수행해야 함
                //-----------------

                PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "HS_Wakelock");
                // ACQUIRE a wakelock here to collect and process accelerometer data and control location updates
                wakeLock.acquire();

                accelMonitor = new StepMonitor(context);
                accelMonitor.onStart();

                timer = new CountDownTimer(activeTime, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        Log.d(LOGTAG, "1-second accel data collected!!");
                        // stop the accel data update
                        accelMonitor.onStop();

                        boolean moving = accelMonitor.isMoving();
                        // 움직임 여부에 따라 GPS location update 요청 처리
                        if(moving) {//이동
                            isSetTime2 = false;
                            //정지중에 이동 되었을 때
                            if(keepStop && secCount2 < 300){
                                //isSetTime = true;
                                info.setEndTime();//정지끝나는시간
                                Log.i("정지끝나는시간", info.setEndTime());
                                info.setIsMoving(false);
                                info.setStepCount(0);
                                //info.setLocation("location");
                                //keepStop = false;
                                isEnd = true;
                            }else{
                                secCount2 = 0;
                            }

                            stepCount++;
                            Log.e("stepCount", String.valueOf(stepCount));
                            //안움직이고 있다가 움직임이 감자되면 시작시간을 Set
                            if(!keepMoving && !keepStop && !isSetTime){
                                isSetTime = true;
                                info.setStartTime();//이동시작시간
                                Log.i("이동시작시간", info.getStartTime());
                            }
                            if(secCount >= 10){//1초당 검사해서 움직이면 증가 60되면 1분이니깐 화면에 표시
                                keepMoving = true;
                                //movingTV.setText("Moving");
                            }else{
                                secCount++;
                            }

                        } else {//정지
                            isSetTime = false;

                            //이동중에 정지가 되었을 때
                            if(keepMoving && secCount < 10){
                                //isSetTime2 = true;
                                info.setEndTime();//이동 끝나는 시간
                                Log.i("이동 끝나는 시간", info.setEndTime());
                                info.setIsMoving(true);//움직임이 1분동안 있었으니깐 이동으로 표시하기위해 true
                                info.setStepCount(stepCountTV);
                                //info.setLocation(""); //체크포인트
                                Intent intentInOrOut = new Intent(ACTION_ALARM_IN_OR_OUT);  //텍스트에 실내외 구분 없이 등록된 장소 이름만 나와야 함
                                sendBroadcast(intentInOrOut);
                                stepCount = 0;
                                isEnd = true;
                            }else{
                                stepCountTV = stepCount;
                                stepCount = 0;
                                secCount = 0;
                            }

                            //이동중이지 않을때 정지되어 있으면 시작시간을 Set
                            if(!keepStop && !keepMoving && !isSetTime2){
                                isSetTime2 = true;
                                info.setStartTime();//정지 시작시간
                                Log.i("정지시작시간", info.getStartTime());
                            }
                            if(secCount2 >= 30){//정지를 300초이상(5분)이상되면 화면에 표시
                                /*
                                5분이상일 겨우에 info에 setLocation에 실내/실외/등록된 장소
                                 */
                                Intent intentInOrOut = new Intent(ACTION_ALARM_IN_OR_OUT);
                                sendBroadcast(intentInOrOut);
                                //info.setLocation();
                                keepStop = true;
                            }else{
                                Log.d("secCount2 : ", String.valueOf(secCount2));
                                secCount2+=period/1000;//정지되어있을때는 1초마다 호출이 아닌 주기마다 호출이되기 때문에 현재 주기(millisecond)에 1000을 나눠 현재 주기의 초를 더함
                            }
                        }
                        Log.e("secCount", String.valueOf(secCount));
                        Intent intent = new Intent(BROADCAST_ACTION_LIVESTEP);
                        intent.putExtra("step", stepCount);
                        sendBroadcast(intent);
                        // 움직임 여부에 따라 다음 alarm 설정
                        setNextAlarm(moving);

                        // When you finish your job, RELEASE the wakelock
                        wakeLock.release();
                    }
                };
                timer.start();
            }
            else if(intent.getAction().equals(ACTION_ALARM_IN_OR_OUT)){
                Log.d(TAG, "방송 수신: ACTION_ALARM_IN_OR_OUT");
                if (!isSensingGPS)
                    try {
                        Log.d(TAG, "GPS 요청");
                        intentUpdateStatus.putExtra("status", "GPS 요청");
                        sendBroadcast(intentUpdateStatus);
                        isGPSFix = false;
                        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000, 0, locationListener);
                        isSensingGPS = true;
                        if(timerGPSTimeout != null) {
                            timerGPSTimeout.cancel();
                        }
                        timerGPSTimeout = new Timer();
                        timerGPSTimeout.schedule(getTask(0), TIMER_GPS_DELAY);
                    } catch (SecurityException e) {
                        e.printStackTrace();
                    }
            }else if(intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)){
                Log.d(TAG, "방송 수신: SCAN_RESULTS_AVAILABLE_ACTION");
                if (!isSensingGPS && isSensingWifi) {
                    checkWifiProximity();
                    statusInOrOut = 2;  //실외
                    Intent intentScanResults = new Intent(ACTION_WIFI_UPDATE);  //(디버깅용) 스캔이 완료됐음을 알린다
                    sendBroadcast(intentScanResults);
                    intentUpdateStatus.putExtra("status", "");
                    sendBroadcast(intentUpdateStatus);
                    isSensingWifi = false;
                }
            }else if(intent.getAction().equals(ACTION_GPS_PROXIMITY)||intent.getAction().equals(ACTION_GPS_PROXIMITY2)){
                checkGPSProximity(intent);
            }else if(intent.getAction().equals(ACTION_GPS_PROXIMITY_SET)){
                setGPSProximityAlert(1);
            }
        }
    };

    //GPS 업데이트 요청이 타임아웃에 의해 종료되었을 때
    private LocationListener locationListener = new LocationListener() {
        @Override
        public void onLocationChanged(Location location)    //현재 위치가 변했음을 알린다.
        {
            timerGPSTimeout.cancel();
            timerGPSTimeout = null;
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
            하나라도 도착하는 신호가 있으면 isGPSFix 플래그를 참으로 바꾼다.*/ {
        switch (event) {
            case GpsStatus.GPS_EVENT_FIRST_FIX:
                isGPSFix = true;
                break;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        Log.d(LOGTAG, "onCreate");

        // Alarm 발생 시 전송되는 broadcast를 수신할 receiver 등록
        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION_ALARM);
        intentFilter.addAction(ACTION_ALARM_IN_OR_OUT);
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);  //Wi-Fi 스캔이 완료됐을 때 발생하는 이벤트
        intentFilter.addAction(ACTION_GPS_PROXIMITY);   //GPS 접근 알림1
        intentFilter.addAction(ACTION_GPS_PROXIMITY2);  //GPS 접근 알림2
        intentFilter.addAction(ACTION_GPS_PROXIMITY_SET);   //(디버깅용) 접근 알림 설정
        intentUpdateGPS = new Intent(ACTION_GPS_UPDATE);
        intentUpdateStatus = new Intent(ACTION_STATUS_UPDATE);

        registerReceiver(AlarmReceiver, intentFilter);
        setupLocationManager();
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        // AlarmManager 객체 얻기
        am = (AlarmManager)getSystemService(ALARM_SERVICE);

        info = new ListViewItem();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // intent: startService() 호출 시 넘기는 intent 객체
        // flags: service start 요청에 대한 부가 정보. 0, START_FLAG_REDELIVERY, START_FLAG_RETRY
        // startId: start 요청을 나타내는 unique integer id

        Log.d(LOGTAG, "onStartCommand");
        Toast.makeText(this, "Activity Monitor 시작", Toast.LENGTH_SHORT).show();

        // Alarm이 발생할 시간이 되었을 때, 안드로이드 시스템에 전송을 요청할 broadcast를 지정
        Intent in = new Intent(BROADCAST_ACTION_ALARM);
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, in, 0);

        // Alarm이 발생할 시간 및 alarm 발생시 이용할 pending intent 설정
        // 설정한 시간 (5000-> 5초, 10000->10초) 후 alarm 발생
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + period, pendingIntent);

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Toast.makeText(this, "Activity Monitor 중지", Toast.LENGTH_SHORT).show();

        try {
            // Alarm 발생 시 전송되는 broadcast 수신 receiver를 해제
            unregisterReceiver(AlarmReceiver);
            am.cancel(pendingIntent);
            cancelLocationRequest();
        } catch(IllegalArgumentException ex) {
            ex.printStackTrace();
        }
        // AlarmManager에 등록한 alarm 취소

        // release all the resources you use
        if(timer != null)
            timer.cancel();
        if(wakeLock != null && wakeLock.isHeld())
            wakeLock.release();
    }

    private void setNextAlarm(boolean moving) {

        // 움직임이면 3초 period로 등록
        // 움직임이 아니면 5초 증가, max 20초로 제한
        if(moving) {
            Log.d(LOGTAG, "MOVING!!");
            period = periodForMoving;
        } else {
            Log.d(LOGTAG, "NOT MOVING!!");
            period = period + periodIncrement;
            if(period >= periodMax) {
                period = periodMax;
            }
        }
        Log.d(LOGTAG, "Next alarm: " + period);

        // 다음 alarm 등록
        Intent in = new Intent(BROADCAST_ACTION_ALARM);
        pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, in, 0);
        am.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + period - activeTime, pendingIntent);

        //*****
        // 화면에 정보 표시를 위해 activity의 broadcast receiver가 받을 수 있도록 broadcast 전송
        if(isEnd){
            isEnd = false;
            //isSetTime = false;
            //isSetTime2 = false;
            keepMoving = false;
            keepStop = false;
            Log.i("location(isEnd) :", info.getLocation());
            Intent intent = new Intent(BROADCAST_ACTION_ACTIVITY);
            intent.putExtra("info", info);
            // broadcast 전송
            sendBroadcast(intent);
        }
    }

    /*LocationManager 서비스를 등록하고 특정 장소에 Proximity Alert 를 설정한다.*/
    private void setupLocationManager() {
        try {
            if (locationManager == null) {
                locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                locationManager.addGpsStatusListener(this);
            }

            if (!isRequestRegistered) {
                setGPSProximityAlert(0);
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

        if (stringGPSPlace == null)
            stringGPSPlace = "";
        if(isEntering) {
            stringGPSPlace = intent.getStringExtra("name");
            Toast.makeText(PeriodicMonitorService.this, "액션 이름: " + intent.getAction() + ", " + stringGPSPlace, Toast.LENGTH_SHORT).show();
        }
        if(!isEntering) {
                stringGPSPlace = "실외";  //등록된 장소가 아니고 정지해 있을 때
        }
        info.setLocation(stringGPSPlace);
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

        stringWifiPlace = "";

        hashPlace1.put("90:9f:33:5a:2a:88", -45);
        hashPlace1.put("90:9f:33:59:2a:88", -52);
        hashPlace1.put("64:e5:99:51:18:60", -52);
        hashPlace2.put("90:9f:33:cd:28:60", -120);
        hashPlace2.put("00:26:66:cc:e3:88", -148);
        hashPlace2.put("b8:8d:12:65:74:34", -200);

        for (int i = 1; i < scanList.size(); i++)   //일단 BSSID 가 일치하면 RSSI 를 비교한다.
        {
            ScanResult result = scanList.get(i);
            Log.d(TAG, "SSID: " + result.SSID + ", BSSID: " + result.BSSID + ", RSSI: " + result.level);
            Integer value;
            if (hashPlace1.containsKey(result.BSSID)) {
                value = hashPlace1.get(result.BSSID);
                if (value != null && Math.abs(value - result.level) <= 20)
                    countPlace1++;
            }
            if (hashPlace2.containsKey(result.BSSID)) {
                value = hashPlace2.get(result.BSSID);
                if (value != null && Math.abs(value - result.level) <= 20)
                    countPlace2++;
            }
        }
        if ((countPlace1 >= 2)) {
            stringWifiPlace = "실내 장소 1";
            isApproaching = true;
        }
        if ((countPlace2 >= 2)) {
            stringWifiPlace = "실내 장소 2";
            isApproaching = true;
        }

        /*if (isApproaching) { //실내에서 등록된 장소로부터 벗어나는 경우
            //Toast.makeText(PeriodicMonitorService.this, "실내: " + stringWifiPlace + "으로 접근", Toast.LENGTH_SHORT).show();
        }*/
        if (!isApproaching && !stringWifiPlace.equals("")) {
            Toast.makeText(PeriodicMonitorService.this, "실내: " + stringWifiPlace + "에서 벗어남", Toast.LENGTH_SHORT).show();
            stringWifiPlace = "실내";
        }

        info.setLocation(stringWifiPlace);
    }

    //GPS 접근 알림 설정
    public void setGPSProximityAlert(int flag) throws SecurityException {
        Location location1;
        Location location2;

        if (intentGPSProximity1 != null && intentGPSProximity2 != null) {
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
            location1.setLatitude(36.761349);
            location1.setLongitude(127.279715);
            location2.setLatitude(36.763065 );
            location2.setLongitude(127.282488);
        } else {
            location1.setLatitude(latitude);
            location1.setLongitude(longitude);
            location2.setLatitude(latitude);
            location2.setLongitude(longitude);
        }

        intentGPSProximity1 = PendingIntent.getBroadcast(this, 0, intent, 0);
        intentGPSProximity2 = PendingIntent.getBroadcast(this, 1, intent2, 0);
        locationManager.addProximityAlert(location1.getLatitude(), location1.getLongitude(), 10, -1, intentGPSProximity1);
        locationManager.addProximityAlert(location2.getLatitude(), location2.getLongitude(), 10, -1, intentGPSProximity2);
    }
    public TimerTask getTask(int flag) {
        if(flag == 0)
            return new TimerTask() {
                @Override
                public void run() {
                    String scanMessage;
                    if (locationManager != null)
                        if (!isGPSFix && !isSensingWifi)  //GPS 업데이트가 이루어지지 않았을 경우 GPS 업데이트 요청을 중단하고 Wi-Fi 스캔을 실시한다.
                            try {
                                locationManager.removeUpdates(locationListener);
                                isSensingGPS = false;
                                Log.d(TAG, "Wi-Fi 스캔");
                                scanSuccessful = wifiManager.startScan();
                                if(timerWifiTimeout != null) {
                                    timerWifiTimeout.cancel();
                                }
                                timerWifiTimeout = new Timer();
                                timerWifiTimeout.schedule(getTask(1), TIMER_WIFI_DELAY);
                                isSensingWifi = true;
                                scanMessage = String.format(Locale.KOREAN, "Wi-Fi scanning: %b", scanSuccessful);
                                intentUpdateStatus.putExtra("status", scanMessage);
                                sendBroadcast(intentUpdateStatus);
                            } catch (SecurityException e) {
                                e.printStackTrace();
                            }
                        else {
                            intentUpdateStatus.putExtra("status", "");
                            sendBroadcast(intentUpdateStatus);
                        }
                }
            };
        else    //taskWifiTimeout
            return new TimerTask() {
                @Override
                public void run() {
                    if (isSensingWifi && !scanSuccessful) {
                        isSensingWifi = false;
                        intentUpdateStatus.putExtra("status", "");
                        sendBroadcast(intentUpdateStatus);
                    }
                }
            };
    }
}
