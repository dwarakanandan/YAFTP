package net.ddns.dwaraka.yaftp;

import android.app.Activity;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;


public class CustomArrayAdapter extends ArrayAdapter<String> {
    private final Activity context;
    private final ArrayList<String> itemname;
    private final ArrayList<String> itemSize;
    private final ArrayList<Integer> imgid;
    public ArrayList<Integer> selectedItem = new ArrayList<>();

    public CustomArrayAdapter(Activity context,ArrayList<String> itemname,ArrayList<String> itemSize,ArrayList<Integer> imageID) {
        super(context,R.layout.list_item,itemname);
        this.context = context;
        this.itemname = itemname;
        this.itemSize=itemSize;
        this.imgid =imageID;
    }

    public void setSelectedItem(ArrayList<Integer> selectedItem){
        this.selectedItem=selectedItem;
    }


    @Override
    public View getView(int position,View view,ViewGroup parent){
        LayoutInflater inflater=context.getLayoutInflater();
        View rowView=inflater.inflate(R.layout.list_item, null, true);
        TextView textView = (TextView) rowView.findViewById(R.id.textView_filename);
        TextView textView1 = (TextView) rowView.findViewById(R.id.textView_fileSize);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.imageView_fileicon);
        textView.setText(itemname.get(position));
        textView1.setText(itemSize.get(position));
        imageView.setImageResource(imgid.get(position));
        if (selectedItem.contains(position)) {
            rowView.setBackgroundColor(Color.parseColor("#c2e184"));
        }
        return rowView;
    }
}
