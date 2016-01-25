package net.ddns.dwaraka.yaftp;


import android.app.ProgressDialog;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

public class RemoteDirectoryListing extends AsyncTask<Object,String,Object>{

    ListFragment listFragment;
    ListView listView;
    TextView textView;
    FTPClient client;
    String root,workingDirectory;
    FTPFile[] ftpFiles;
    Boolean loginflag = false,changeflag = true;
    ProgressDialog progDailog;
    String HOST_NAME,USERNAME,PASSWORD;
    String oldDirectory;
    int PORT;

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

    public RemoteDirectoryListing(ListFragment listFragment1,String root1,ListView listView1,TextView textView1){
        listFragment = listFragment1;
        root=root1;
        listView=listView1;
        textView=textView1;
    }
    public void setConnectionParameters(String hostName,int port,String username,String password){
        this.HOST_NAME = hostName;
        this.PORT = port;
        this.USERNAME = username;
        this.PASSWORD = password;
    }


    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        progDailog = new ProgressDialog(listFragment.getActivity());
        progDailog.setMessage("Loading...");
        progDailog.setIndeterminate(false);
        progDailog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progDailog.setCancelable(false);
        progDailog.show();
    }

    @Override
    protected Object doInBackground(Object[] params) {
        client = new FTPClient();
        try {
            client.connect(HOST_NAME, PORT);
            //Log.e("dwaraka", "server found");
            publishProgress("Server found...");
            loginflag = client.login(USERNAME,PASSWORD);
            if(!loginflag)
                return null;
            client.enterLocalPassiveMode();
            //Log.e("dwaraka", "success login");
            publishProgress("Login Successful...");
            if(!root.isEmpty()) {
                changeflag = client.changeWorkingDirectory(root);
            }
            if(!changeflag)
                return null;
            publishProgress("Changing working directory...");
            Log.d("dwaraka", "pwd = " + client.printWorkingDirectory());
            workingDirectory = client.printWorkingDirectory();
            ftpFiles = client.listFiles(workingDirectory);
            client.disconnect();
        }catch (SocketException e){
            Log.e("dwaraka","SocketException");
            e.printStackTrace();
        }
        catch (IOException e) {
            Log.e("dwaraka","IOException");
            e.printStackTrace();
        }
        listFragment.setRemoteLists(ftpFiles);
        return null;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        progDailog.setMessage(values[0]);

    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        progDailog.dismiss();
        if(!loginflag){
            textView.setText("Error Logging In!");
            return;
        }
        if(!changeflag){
            textView.setText("Error Changing Directory!");
            return;
        }
        textView.setText(root);
        ArrayList<String> filename = new ArrayList<>();
        ArrayList<Integer> iconid = new ArrayList<>();
        ArrayList<String> fileSize = new ArrayList<>();
        for( FTPFile i:ftpFiles){
            filename.add(i.getName());
            if(i.isDirectory()) {
                iconid.add(icon[1]);
                fileSize.add("Folder");
            }
            else {
                fileSize.add(ListFragment.getStringFileSize(i.getSize()));
                String fileExtension = i.getName().substring(i.getName().lastIndexOf(".")+1);
                if(fileExtension.matches("pdf"))
                    iconid.add(icon[7]);
                else if(fileExtension.matches("mp3|wav|wma|mpa|m4a|aif|ra"))
                    iconid.add(icon[2]);
                else if(fileExtension.matches("mp4|mkv|3gp|avi|m4v|mov|mpg|swf|vob|3g2|flv|asf|asx|wmv"))
                    iconid.add(icon[3]);
                else if(fileExtension.matches("png|jpg|bmp|gif|psd|dds|tif"))
                    iconid.add(icon[4]);
                else if(fileExtension.matches("sh|exe|out|bat"))
                    iconid.add(icon[5]);
                else if(fileExtension.matches("c|cpp|java|py|sql|php|html|class"))
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
        listFragment.setRemoteAdapter(customArrayAdapter);
    }


}
