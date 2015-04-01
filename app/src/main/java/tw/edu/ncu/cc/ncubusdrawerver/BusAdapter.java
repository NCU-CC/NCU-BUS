package tw.edu.ncu.cc.ncubusdrawerver;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by andre.hu on 2014/11/16.
 */
public class BusAdapter extends ArrayAdapter {

    Activity myContext;
    private ArrayList<BusData> datas;
    String rowColor[] = {"#8FFDB1","#FFFFFF"};

    public BusAdapter(Context context, int resource, ArrayList<BusData> objects) {
        super(context, resource, objects);
        myContext = (Activity)context;
        datas = objects;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            LayoutInflater inflater = myContext.getLayoutInflater();
            convertView = inflater.inflate(R.layout.bus_item, null);
            viewHolder = new ViewHolder();
            viewHolder.TitleView = (TextView) convertView
                    .findViewById(R.id.TitleLabel);
            viewHolder.TimeView = (TextView) convertView
                    .findViewById(R.id.TimeLabel);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        try {
            viewHolder.TimeView.setTextColor(Color.GRAY);
            viewHolder.TitleView.setTextColor(Color.BLACK);
            viewHolder.TitleView.setText(datas.get(position).busStop);
            viewHolder.TimeView.setText(datas.get(position).busTime);

            convertView.setBackgroundColor(Color.parseColor(rowColor[position%2]));


            Pattern p = Pattern.compile("-?\\d+");
            Matcher m = p.matcher(datas.get(position).busTime);
            if (m.find() == true) {
                int value = Integer.parseInt(m.group());
                if (value >= 15 && !datas.get(position).busTime.contains("未發車")) {
                    viewHolder.TimeView.setTextColor(Color.argb(255, 114, 229, 0));//GREEN
                } else if (value > 5 && value < 15) {
                    viewHolder.TimeView.setTextColor(Color.argb(255, 255, 165, 0));//ORANGE
                } else if (value <= 5) {
                    viewHolder.TimeView.setTextColor(Color.RED);
                    if (value == 0) {
                        viewHolder.TimeView.setText("進站中");
                    }else if(value < 0){
                        viewHolder.TimeView.setTextColor(Color.GRAY);
                        viewHolder.TimeView.setText("末班車已駛離");
                    }
                }
            }
        }catch (IndexOutOfBoundsException e){
            Log.e("debug",""+e.getMessage());
        }



        return convertView;
    }

    static class ViewHolder {
        TextView TitleView;
        TextView TimeView;
    }
}
