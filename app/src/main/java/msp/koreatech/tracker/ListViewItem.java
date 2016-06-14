package msp.koreatech.tracker;

import android.os.Parcel;
import android.os.Parcelable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

/*
 * Created by JeongHeon on 2016. 6. 14..
 */
public class ListViewItem implements Parcelable {
    private long startTime, endTime;
    private boolean isMoving;
    private long stepCount;
    private String location = " ";
    long time;
    SimpleDateFormat dayTime = new SimpleDateFormat("HH:mm", Locale.KOREAN);

    public static final Parcelable.Creator<ListViewItem> CREATOR = new Parcelable.Creator<ListViewItem>() {
        public ListViewItem createFromParcel(Parcel src) {
            return new ListViewItem(src);
        }

        @Override
        public ListViewItem[] newArray(int size) {
            return new ListViewItem[0];
        }
    };

    public ListViewItem() {

    }

    public ListViewItem(Parcel src) {
        startTime = src.readLong();
        endTime = src.readLong();
        isMoving = src.readByte() != 0;
        stepCount = src.readLong();
        location = src.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(startTime);
        dest.writeLong(endTime);
        dest.writeByte((byte) (isMoving ? 1 : 0));
        dest.writeLong(stepCount);
        dest.writeString(location);
    }

    public long setStartTime() {
        startTime = System.currentTimeMillis();
        return startTime;
    }

    public long setEndTime() {
        time = System.currentTimeMillis();
        endTime = System.currentTimeMillis();
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

    public String getStartTimeString() {
        return dayTime.format(new Date(startTime));
    }

    public String getEndTimeString() {
        return dayTime.format(new Date(endTime));
    }

    public long getDuration() {
        return TimeUnit.MILLISECONDS.toMinutes(endTime - startTime) + 1;
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
}
