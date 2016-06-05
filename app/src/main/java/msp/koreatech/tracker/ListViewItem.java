package msp.koreatech.tracker;

import android.util.Log;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by JeongHeon on 2016. 6. 2..
 */
public class ListViewItem {
    private String startTime, endTime;
    private boolean isMoving;
    private int stepCount;
    private String location = " ";
    long time;
    SimpleDateFormat dayTime = new SimpleDateFormat("hh:mm");

    public String setStartTime(){
        time = System.currentTimeMillis();
        startTime = dayTime.format(new Date(time));
        return startTime;
    }
    public String setEndTime(){
        time = System.currentTimeMillis();
        endTime = dayTime.format(new Date(time));
        return endTime;
    }
    public void setIsMoving(boolean isMoving){
        this.isMoving = isMoving;
    }
    public void setStepCount(int stepCount){
        this.stepCount = stepCount;
    }
    public void setLocation(String location){
        this.location = location;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public String getDuringTime() {
        int during;
        String sHour = startTime.substring(0, 2);
        String sMin = startTime.substring(3, 5);
        Log.d("infoLog", sHour + " : " + sMin);
        String eHour = endTime.substring(0, 2);
        String eMin = endTime.substring(3, 5);
        int hour1 = Integer.parseInt(sHour);
        int min1 = Integer.parseInt(sMin);
        int hour2 = Integer.parseInt(eHour);
        int min2 = Integer.parseInt(eMin);

        if(hour1 == hour2){
            during = Math.abs(min1 - min2);
            return String.valueOf(during);
        }else{
            if(min1 > min2){
                during = 60-min1+min2;
                if(Math.abs((hour1+1)-hour2) == 0){
                    return String.valueOf(during);
                }else{
                    during += Math.abs((hour1+1)-hour2) * 60;
                    return String.valueOf(during);
                }
            }else{
                during = (hour2-hour1)*60 + (min2-min1);
                return String.valueOf(during);
            }
        }
    }

    public boolean isMoving() {
        return isMoving;
    }

    public int getStepCount() {
        return stepCount;
    }

    public String getLocation() {
        return location;
    }
}
