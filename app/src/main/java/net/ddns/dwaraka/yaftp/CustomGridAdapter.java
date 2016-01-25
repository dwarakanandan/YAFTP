package net.ddns.dwaraka.yaftp;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


public class CustomGridAdapter extends BaseAdapter {
    ArrayList<String> itemNames = new ArrayList<>();
    Context context;
    static LayoutInflater inflater = null;

    public CustomGridAdapter(MainActivity mainActivity,ArrayList<String> itemNames){
        context = mainActivity;
        this.itemNames =itemNames;
        inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }


    @Override
    public int getCount() {
        return itemNames.size();
    }

    @Override
    public Object getItem(int position) {
        return position;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = inflater.inflate(R.layout.grid_item,null);
        ImageView imageView = (ImageView) view.findViewById(R.id.ftpFolderImage);
        TextView textView = (TextView) view.findViewById(R.id.ftpServerName);
        imageView.setImageResource(R.drawable.ftp_server);
        textView.setText(itemNames.get(position));
        return view;
    }
}
