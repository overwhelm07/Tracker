package msp.koreatech.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final String ACTION_GPS_UPDATE = "msp.koreatech.tracker.gps";
    private static final String ACTION_GPS_PROXIMITY = "msp.koreatech.tracker.gps.proximity";
    private static final String ACTION_WIFI_UPDATE = "msp.koreatech.tracker.wifi";
    private Intent intentService;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String stringAction = intent.getAction();
            if(stringAction.equals(ACTION_GPS_UPDATE)) {
                Toast.makeText(MainActivity.this, "GPS 전달 완료", Toast.LENGTH_SHORT).show();
                double longitude = intent.getDoubleExtra("longitude", 0);
                double latitude = intent.getDoubleExtra("latitude", 0);

                TextView textGPS = (TextView) findViewById(R.id.textGPS);
                textGPS.setText(String.format(Locale.KOREAN, "longitude: %f, latitude: %f", longitude, latitude));
            }
            else if(stringAction.equals(ACTION_WIFI_UPDATE)) {
                Toast.makeText(MainActivity.this, "WIFI 업데이트 완료", Toast.LENGTH_SHORT).show();
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
}
