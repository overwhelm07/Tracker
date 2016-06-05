package msp.koreatech.tracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * ListView를 사용하기 위한 어댑터 클래스를 생성했습니다
 * 이번에 처음으로 리스트뷰를 공부해서 라디오 버튼까지 구현할려고
 * 구글링을 통해 예제를 보면서 구현해보았습니다
 * 라디오버튼 부분이 구현하기 힘들었고 시간이 많이 소요되었는데 좀 더 심층적 공부를 해야겠습니다
 * 리스트뷰 어댑터에서는 getView()가 핵심 역할을 하는데 이 함수를 통해 실시간으로
 * 변경된 정보들을 뿌려주는 역할을 합니다«
 */
public class ListViewAdapter extends BaseAdapter {

    private LayoutInflater inflater;
    private ArrayList<ListViewItem> al;
    private int layout;
    private int selectedPosition = -1;

    public ListViewAdapter(Context context, int layout,
                           ArrayList<ListViewItem> al){
        this.inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.al = al;
        this.layout = layout;
    }

    @Override
    public int getCount() {
        return al.size();
    }

    @Override
    public Object getItem(int position) {
        return al.get(position).getStartTime();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if(convertView==null){
            convertView=inflater.inflate(layout,parent,false);
        }

        ListViewItem listViewItem = al.get(position);
        //리스트뷰의 하나의 정보들을 변수에 저장
        TextView startTimeTV = (TextView)convertView.findViewById(R.id.startTimeTV);
        TextView endTimeTV = (TextView)convertView.findViewById(R.id.endTimeTV);
        TextView duringTimeTV = (TextView)convertView.findViewById(R.id.duringTimeTV);
        TextView movingTV = (TextView) convertView.findViewById(R.id.duringTimeTV);
        TextView stepsTV = (TextView) convertView.findViewById(R.id.stepsTV);
        TextView locationTV = (TextView) convertView.findViewById(R.id.locationTV);
        //리스트 뷰에 표시
        startTimeTV.setText(listViewItem.getStartTime());
        endTimeTV.setText(String.valueOf(listViewItem.getEndTime()));
        duringTimeTV.setText(String.valueOf(listViewItem.getDuringTime()));
        if(listViewItem.isMoving()){
            movingTV.setText(" 이동 ");
        }else{
            movingTV.setText(" 정지 ");
        }
        stepsTV.setText(String.valueOf(listViewItem.getStepCount()+"걸음 "));

        return convertView;
    }


}
