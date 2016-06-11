package msp.koreatech.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String ACTION_ALARM_IN_OR_OUT = "msp.koreatech.tracker.alarm";
    private static final String ACTION_GPS_UPDATE = "msp.koreatech.tracker.gps";
    private static final String ACTION_GPS_PROXIMITY = "msp.koreatech.tracker.gps.proximity";
    private static final String ACTION_GPS_PROXIMITY2 = "msp.koreatech.tracker.gps.proximity2";
    private static final String ACTION_GPS_PROXIMITY_SET = "msp.koreatech.tracker.gps.proximity.set";
    private static final String ACTION_WIFI_UPDATE = "msp.koreatech.tracker.wifi";
    private static final String ACTION_STATUS_UPDATE = "msp.koreatech.tracker.status";
    private Intent intentService;
    private double latitude;
    private double longitude;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String stringAction = intent.getAction();
            if(stringAction.equals(ACTION_GPS_UPDATE)) {
                Toast.makeText(MainActivity.this, "GPS 전달 완료", Toast.LENGTH_SHORT).show();
                longitude = intent.getDoubleExtra("longitude", 0);
                latitude = intent.getDoubleExtra("latitude", 0);
                float accuracy = intent.getFloatExtra("accuracy", 0);

                EditText editLat= (EditText) findViewById(R.id.textLat);
                EditText editLng= (EditText) findViewById(R.id.textLng);
                editLat.setText(String.format(Locale.KOREAN, "latitude: %f", latitude));
                editLng.setText(String.format(Locale.KOREAN, "longitude: %f", longitude));
              TextView textGPS = (TextView) findViewById(R.id.textGPS);
                textGPS.setText(String.format(Locale.KOREAN, "accuracy: %f", accuracy));
            }
            else if(stringAction.equals(ACTION_WIFI_UPDATE)) {
                Toast.makeText(MainActivity.this, "WIFI 업데이트 완료", Toast.LENGTH_SHORT).show();
            }
            else if(stringAction.equals(ACTION_STATUS_UPDATE)) {
                String status = intent.getStringExtra("status");
                if(status == null)
                    status = "";
                TextView textStatus = (TextView) findViewById(R.id.textStatus);
                textStatus.setText(String.format(Locale.KOREAN, "status: %s", status));
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GPS_UPDATE);
        intentFilter.addAction(ACTION_WIFI_UPDATE);
        intentFilter.addAction(ACTION_GPS_PROXIMITY);
        intentFilter.addAction(ACTION_GPS_PROXIMITY2);
        intentFilter.addAction(ACTION_STATUS_UPDATE);
        intentService = new Intent(this, PeriodicMonitorService.class);
        registerReceiver(broadcastReceiver, intentFilter);
        startService(intentService);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(intentService);
        unregisterReceiver(broadcastReceiver);
    }

    public void onClickSetAlarm(View view) {
        Intent intent = new Intent(ACTION_ALARM_IN_OR_OUT);
        sendBroadcast(intent);
    }

    public void onClickSetProximity(View view) {
        Intent intent = new Intent(ACTION_GPS_PROXIMITY_SET);
        intent.putExtra("latitude", latitude);
        intent.putExtra("longitude", longitude);
        sendBroadcast(intent);
    }
}
