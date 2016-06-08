package msp.koreatech.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {
    private static final String ACTION_GPS_UPDATE = "msp.koreatech.tracker.gps";
    private Intent intentService;
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(MainActivity.this, "GPS 전달 완료", Toast.LENGTH_SHORT).show();
            double longitude = intent.getDoubleExtra("longitude", 0);
            double latitude = intent.getDoubleExtra("latitude", 0);


            TextView textGPS = (TextView) findViewById(R.id.textGPS);
            textGPS.setText(String.format("longitude: %f, latitude: %f", longitude, latitude));
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ACTION_GPS_UPDATE);
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
