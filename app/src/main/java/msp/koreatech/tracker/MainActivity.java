package msp.koreatech.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    private static final String BROADCAST_ACTION_ACTIVITY = "msp.tracker";
    private static final String BROADCAST_ACTION_LIVESTEP = "msp.tracker.step";

    private Intent intentService;
    private TextView dateTV, movingTV, stepTV, placeTV, liveStepTV;
    private int movingTimeSum;
    private long stepCountSum;
    ListViewAdapter adapter;
    ListViewItem info;
    ArrayList<ListViewItem> al;
    ListView listView;
    int topPlaceDuringTime = 0;

    private BroadcastReceiver MyStepReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(BROADCAST_ACTION_ACTIVITY)) {
                //rms = intent.getDoubleExtra("rms", 0.0);
                //rmsText.setText("rms: " + rms);
                //boolean moving = intent.getBooleanExtra("moving", false);
                info = new ListViewItem();
                info = intent.getParcelableExtra("info");
                Log.e("startTime", info.getStartTime());
                Log.e("duringTime", info.getDuringTime());
                Log.e("stepCount", String.valueOf(info.getStepCount()));
                Log.e("location", info.getLocation());
                al.add(info);

                //정지 되어있으면 그 때 duringTime을 이용해서 TopPlace의 장소를 구분한다
                if (!info.isMoving()) {
                    if (topPlaceDuringTime < Integer.valueOf(info.getDuringTime())) {
                        String tmp = info.getLocation().trim();
                        if (!(tmp.equals("") || tmp.equals("실외") || tmp.equals("실내"))) {
                            topPlaceDuringTime = Integer.valueOf(info.getDuringTime());
                            placeTV.setText("Top Place : " + info.getLocation());
                        }
                    }
                }

                if (al.get(al.size() - 1).isMoving()) {
                    movingTimeSum += Integer.valueOf(al.get(al.size() - 1).getDuringTime());
                    stepCountSum += al.get(al.size() - 1).getStepCount();
                }
                movingTV.setText("Moving time: " + movingTimeSum + "분");
                stepTV.setText("Steps: " + stepCountSum);

                //어댑터에 모델이 바뀌었다고 알리기
                adapter.notifyDataSetChanged();

            }
            if (intent.getAction().equals(BROADCAST_ACTION_LIVESTEP)) {
                Log.e("listep call", String.valueOf(intent.getExtras().getLong("step")));
                liveStepTV.setText("liveStep: " + String.valueOf(intent.getExtras().getLong("step")));
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION_ACTIVITY);
        intentFilter.addAction(BROADCAST_ACTION_LIVESTEP);
        registerReceiver(MyStepReceiver, intentFilter);

        listView = (ListView) findViewById(R.id.listView);
        //상단 텍스트 뷰 선언
        dateTV = (TextView) findViewById(R.id.dateTV);
        movingTV = (TextView) findViewById(R.id.movingTV);
        stepTV = (TextView) findViewById(R.id.stepsTV);
        placeTV = (TextView) findViewById(R.id.placeTV);
        liveStepTV = (TextView) findViewById(R.id.liveStepTV);

        //현재 시간을 msec으로 구하기
        long now = System.currentTimeMillis();
        //현재 시간을 저장한다
        Date date = new Date(now);
        SimpleDateFormat curDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일");
        String strCurDate = curDateFormat.format(date);
        dateTV.setText(strCurDate);

        //상단에 표시할 Moving time, Steps를 위한 변수
        movingTimeSum = 0;
        stepCountSum = 0;

        al = new ArrayList<>();
        adapter = new ListViewAdapter(this, R.layout.item, al);

        //어댑터 적용한 이후에 listview Setting
        listView.setAdapter(adapter);
        listView.setDivider(new ColorDrawable(Color.BLACK));
        listView.setDividerHeight(4);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);

        //PeriodicMoniorService 인텐트 서비스 시작
        intentService = new Intent(this, PeriodicMonitorService.class);
        startService(intentService);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        unregisterReceiver(MyStepReceiver);
        stopService(intentService);
        super.onDestroy();
    }
}
