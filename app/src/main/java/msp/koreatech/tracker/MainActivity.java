package msp.koreatech.tracker;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {
    private Intent intentService;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        intentService = new Intent(this, PeriodicMonitorService.class);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startService(intentService);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(intentService);
    }
}
