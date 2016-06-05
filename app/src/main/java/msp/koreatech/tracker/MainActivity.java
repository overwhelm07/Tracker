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

    private TextView dateTV, movingTV, stepTV, placeTV;
    private int secCount = 0;
    private boolean keepMoving = false;
    ListViewAdapter adapter;
    ListViewItem info;
    ArrayList<ListViewItem> al;
    ListView listView;


    private BroadcastReceiver MyStepReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(BROADCAST_ACTION_ACTIVITY)) {
                //rms = intent.getDoubleExtra("rms", 0.0);
                //rmsText.setText("rms: " + rms);
                //boolean moving = intent.getBooleanExtra("moving", false);
                info = new ListViewItem();
                info = intent.getParcelableExtra("info");
                Log.e("startTime", info.getStartTime());
                Log.e("duringTime", info.getDuringTime());
                Log.e("stepCount", String.valueOf(info.getStepCount()));
                al.add(info);

                //어댑터에 모델이 바뀌었다고 알리기
                adapter.notifyDataSetChanged();
                /*if(moving) {
                    //안움직이고 있다가 움직임이 감자되면 시작시간을 time에 append한다
                    if(!keepMoving){
                        //info.setStartTime();
                    }
                    if(secCount >= 60){//1초당 검사해서 움직이면 증가 60되면 1분이니깐 화면에 표시
                        keepMoving = true;
                        movingTV.setText("Moving");
                    }else{
                        secCount++;
                    }
                } else {
                    if(keepMoving && secCount < 59){
                        //info.setEndTime();
                        //info.setIsMoving(true);//움직임이 1분동안 있었으니깐 이동으로 표시하기위해 true
                        keepMoving = false;
                        movingTV.setText("NOT Moving");
                    }else{
                        secCount--;
                    }
                }*/
                /*double lon = intent.getDoubleExtra("longitude", 0.0);
                double lat = intent.getDoubleExtra("latitude", 0.0);
                locationText.setText("Location: longitude " + lon + " latitude " + lat);*/
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        listView = (ListView) findViewById(R.id.listView);
        //상단 텍스트 뷰 선언
        dateTV = (TextView) findViewById(R.id.dateTV);
        movingTV = (TextView) findViewById(R.id.movingTV);
        stepTV = (TextView) findViewById(R.id.stepsTV);
        placeTV = (TextView) findViewById(R.id.placeTV);


        //현재 시간을 msec으로 구하기
        long now = System.currentTimeMillis();
        //현재 시간을 저장한다
        Date date = new Date(now);
        SimpleDateFormat curDateFormat = new SimpleDateFormat("yyyy년 MM월 dd일");
        String strCurDate = curDateFormat.format(date);
        dateTV.setText(strCurDate);


        al = new ArrayList<>();
        adapter = new ListViewAdapter(this, R.layout.item, al);

        //어댑터 적용한 이후에 listview Setting
        listView.setAdapter(adapter);
        listView.setDivider(new ColorDrawable(Color.BLACK));
        listView.setDividerHeight(4);
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);


        //PeriodicMoniorService 인텐트 서비스 시작
        Intent intent = new Intent(this, PeriodicMonitorService.class);
        startService(intent);



    }


    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter intentFilter = new IntentFilter(BROADCAST_ACTION_ACTIVITY);
        registerReceiver(MyStepReceiver, intentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(MyStepReceiver);
    }

}
