package net.ddns.dwaraka.yaftp;

import android.os.AsyncTask;
import android.widget.ListView;


import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class LocalDirectoryListing extends AsyncTask{
    ListFragment listFragment;
    File file;
    String root;
    ListView listView;
    File[] returnFileList;

    int[] icon = new int[]{
            R.drawable.file,//0
            R.drawable.folder,//1
            R.drawable.audio,//2
            R.drawable.video,//3
            R.drawable.image,//4
            R.drawable.executable,//5
            R.drawable.code,//6
            R.drawable.pdf,//7
            R.drawable.compressed,//8
            R.drawable.android,//9
            R.drawable.disk,//10
            R.drawable.java,//11
            R.drawable.text//12
    };

    public LocalDirectoryListing(ListFragment listFragment1,String root1,ListView listView1){
        listFragment = listFragment1;
        root=root1;
        listView=listView1;
    }

    @Override
    protected Object doInBackground(Object[] params) {
        file=new File(root);
        returnFileList = file.listFiles();
        Arrays.sort(returnFileList);
        listFragment.setLocalLists(returnFileList);
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        ArrayList<String> filename = new ArrayList<>();
        ArrayList<Integer> iconid = new ArrayList<>();
        ArrayList<String> fileSize = new ArrayList<>();
        for( File i:returnFileList){
            filename.add(i.getName());
            if(i.isDirectory()) {
                iconid.add(icon[1]);
                fileSize.add("Folder");
            }
            else {
                fileSize.add(ListFragment.getStringFileSize(i.length()));
                String fileExtension = i.getName().substring(i.getName().lastIndexOf(".")+1);
                if(fileExtension.matches("pdf"))
                    iconid.add(icon[7]);
                else if(fileExtension.matches("mp3|wav|wma|mpa|m4a|aif|ra"))
                    iconid.add(icon[2]);
                else if(fileExtension.matches("mp4|mkv|3gp|avi|m4v|mov|mpg|swf|vob|3g2|flv|asf|asx|wmv"))
                    iconid.add(icon[3]);
                else if(fileExtension.matches("png|jpg|bmp|gif|psd|dds|tif"))
                    iconid.add(icon[4]);
                else if(fileExtension.matches("sh|exe|out|bat|jar"))
                    iconid.add(icon[5]);
                else if(fileExtension.matches("c|cpp|py|sql|php|html"))
                    iconid.add(icon[6]);
                else if(fileExtension.matches("apk"))
                    iconid.add(icon[9]);
                else if(fileExtension.matches("zip|7z|tar|gz|deb|rar"))
                    iconid.add(icon[8]);
                else if(fileExtension.matches("iso|bin|vcd|dmg|img"))
                    iconid.add(icon[10]);
                else if(fileExtension.matches("txt|doc|docx|odt"))
                    iconid.add(icon[12]);
                else if(fileExtension.matches("java|class|jar"))
                    iconid.add(icon[11]);
                else
                    iconid.add(icon[0]);
            }
        }

        CustomArrayAdapter customArrayAdapter = new CustomArrayAdapter(listFragment.getActivity(),filename,fileSize,iconid);
        listView.setAdapter(customArrayAdapter);
        listFragment.setLocalAdapter(customArrayAdapter);
    }



}
