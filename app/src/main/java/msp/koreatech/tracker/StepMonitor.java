package msp.koreatech.tracker;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class StepMonitor implements SensorEventListener {
    private static final String LOGTAG = "HS_Location_Tracking";

    private Context context;
    private SensorManager mSensorManager;
    private Sensor mLinear;
    private boolean isMoving;
    // onStart() 호출 이후 onStop() 호출될 때까지 센서 데이터 업데이트 횟수를 저장하는 변수
    private int sensingCount;
    // 센서 데이터 업데이트 중 움직임으로 판단된 횟수를 저장하는 변수
    private int movementCount;

    // 움직임 여부를 판단하기 위한 3축 가속도 데이터의 RMS 값의 기준 문턱값
    private static final double RMS_THRESHOLD = 1.0;

    public StepMonitor(Context context) {
        this.context = context;

        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mLinear = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
    }

    public void onStart() {
        // SensorEventListener 등록
        if (mLinear != null) {
            Log.d(LOGTAG, "Register Accel Listener!");
            mSensorManager.registerListener(this, mLinear, SensorManager.SENSOR_DELAY_NORMAL);
        }
        // 변수 초기화
        isMoving = false;
        sensingCount = 0;
        movementCount = 0;
    }

    public void onStop() {
        // SensorEventListener 등록 해제
        if (mSensorManager != null) {
            Log.d(LOGTAG, "Unregister Accel Listener!");
            mSensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    // 센서 데이터가 업데이트 되면 호출
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {

            // 센서 업데이트 횟수 증가
            sensingCount++;

            //***** sensor data collection *****//
            // event.values 배열의 사본을 만들어서 values 배열에 저장
            float[] values = event.values.clone();

            // movement detection
            detectMovement(values);
        }
    }

    private void detectMovement(float[] values) {
        // 현재 업데이트 된 accelerometer x, y, z 축 값의 Root Mean Square 값 계산
        double rms = Math.sqrt(values[0] * values[0] + values[1] * values[1] + values[2] * values[2]);
        Log.d(LOGTAG, "rms: " + rms);

        // 계산한 rms 값을 threshold 값과 비교하여 움직임이면 count 변수 증가
        if(rms > RMS_THRESHOLD) {
            movementCount++;
        }
    }

    // 일정 시간 동안 움직임 판단 횟수가 센서 업데이트 횟수의 50%를 넘으면 움직임으로 판단
    public boolean isMoving() {
        Log.d("movement, sensing", movementCount + ", " + sensingCount);
        double ratio = (double)movementCount / (double)sensingCount;
        if(ratio >= 0.5) {
            isMoving = true;
        } else {
            isMoving = false;
        }
        return isMoving;
    }

    //movementCount 리턴
    public int getMovementCount(){
        return movementCount;
    }
}