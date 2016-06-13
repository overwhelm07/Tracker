package msp.koreatech.tracker;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by JeongHeon on 2016. 6. 2..
 */
public class ListViewItem implements Parcelable {
    private String startTime, endTime;
    private boolean isMoving;
    private long stepCount;
    private String location = " ";
    long time;
    SimpleDateFormat dayTime = new SimpleDateFormat("hh:mm");

    public ListViewItem() {

    }

    public ListViewItem(Parcel src) {
        startTime = src.readString();
        endTime = src.readString();
        isMoving = src.readByte() != 0;
        stepCount = src.readLong();
        location = src.readString();
    }

    public String setStartTime() {
        time = System.currentTimeMillis();
        startTime = dayTime.format(new Date(time));
        return startTime;
    }

    public String setEndTime() {
        time = System.currentTimeMillis();
        endTime = dayTime.format(new Date(time));
        return endTime;
    }

    public void setIsMoving(boolean isMoving) {
        this.isMoving = isMoving;
    }

    public void setStepCount(long stepCount) {
        this.stepCount = stepCount;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    /*
    11:28-08:17 289분 잘못 표시됨
     */
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

        if (hour1 == hour2) {
            during = Math.abs(min1 - min2);
            return String.valueOf(during);
        } else {
            if (min1 > min2) {
                during = 60 - min1 + min2;
                if (Math.abs((hour1 + 1) - hour2) == 0) {
                    return String.valueOf(during);
                } else {
                    during += Math.abs((hour1 + 1) - hour2) * 60;
                    return String.valueOf(during);
                }
            } else {
                during = (hour2 - hour1) * 60 + (min2 - min1);
                return String.valueOf(during);
            }
        }
    }

    public boolean isMoving() {
        return isMoving;
    }

    public long getStepCount() {
        return stepCount;
    }

    public String getLocation() {
        return location;
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(startTime);
        dest.writeString(endTime);
        dest.writeByte((byte) (isMoving ? 1 : 0));
        dest.writeLong(stepCount);
        dest.writeString(location);


    }

/*    public void readFromParcel(Parcel in) {
        startTime = in.readString();
        endTime = in.readString();
        isMoving = (in.readByte() != 0);
        stepCount = in.readLong();
        location = in.readString();
    }*/

    public static final Parcelable.Creator<ListViewItem> CREATOR = new Parcelable.Creator<ListViewItem>() {
        public ListViewItem createFromParcel(Parcel src) {
            return new ListViewItem(src);
        }

        @Override
        public ListViewItem[] newArray(int size) {
            return new ListViewItem[0];
        }
    };
}
