package net.ddns.dwaraka.yaftp;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTPClient;

import java.io.IOException;
import java.net.SocketException;


public class TestConnection extends AsyncTask<Object,String,Object> {
    String SERVER_NAME,HOST_NAME,USERNAME,PASSWORD,LOCAL_DIR,REMOTE_DIR;
    int PORT;
    Boolean loginflag = false,changeflag = true,socketException = false,ioException = false,save;
    AddServerActivity activityAddServer;
    EditServerActivity activityEditServer;
    Context context;
    public boolean allOk = false;
    public static String REMOTE_DIR_STATIC;
    int mode;

    public void setConnectionParameters(String serverName,String hostName,int port,String username,String password,String localDir,String remoteDir,AddServerActivity activity,Boolean save){
        this.SERVER_NAME = serverName;
        this.HOST_NAME = hostName;
        this.PORT = port;
        this.USERNAME = username;
        this.PASSWORD = password;
        this.LOCAL_DIR = localDir;
        this.REMOTE_DIR = remoteDir;
        this.activityAddServer = activity;
        this.save = save;
        context = activity;
        mode=0;
    }

    public void setConnectionParameters(String serverName,String hostName,int port,String username,String password,String localDir,String remoteDir,EditServerActivity activity,Boolean save){
        this.SERVER_NAME = serverName;
        this.HOST_NAME = hostName;
        this.PORT = port;
        this.USERNAME = username;
        this.PASSWORD = password;
        this.LOCAL_DIR = localDir;
        this.REMOTE_DIR = remoteDir;
        this.activityEditServer = activity;
        this.save = save;
        context = activityEditServer;
        mode=1;
    }

    @Override
    protected void onProgressUpdate(String... values) {
        super.onProgressUpdate(values);
        Toast.makeText(context,values[0],Toast.LENGTH_SHORT).show();
    }

    @Override
    protected Object doInBackground(Object[] params) {
        FTPClient client = new FTPClient();
        try {
            client.connect(HOST_NAME, PORT);
            //Log.e("dwaraka", "server found");
            if(!save)
                publishProgress("Server found...");
            loginflag = client.login(USERNAME,PASSWORD);
            if(!loginflag)
                return null;
            client.enterLocalPassiveMode();
            //Log.e("dwaraka", "success login");
            if(!save)
                publishProgress("Login Successful...");
            if(!REMOTE_DIR.isEmpty()) {
                changeflag = client.changeWorkingDirectory(REMOTE_DIR);
            }
            else{
                REMOTE_DIR_STATIC = client.printWorkingDirectory();
            }
            if(!changeflag)
                return null;
            if(!save)
                publishProgress("Changing working directory...");
        }catch (SocketException e){
            Log.e("dwaraka","SocketException");
            socketException = true;
            publishProgress(e.getMessage());
            e.printStackTrace();
        }
        catch (IOException e) {
            Log.e("dwaraka","IOException");
            ioException = true;
            publishProgress(e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        if(socketException){
            Toast.makeText(context,"Connect failed: Check that host name and port",Toast.LENGTH_LONG).show();
            return;
        }
        else if(ioException){
            Toast.makeText(context,"Connect failed: IO Exception ",Toast.LENGTH_LONG).show();
            return;
        }
        else if(!loginflag){
            Toast.makeText(context,"Error Logging in!..Check that username and password",Toast.LENGTH_LONG).show();
            return;
        }
        else if(!changeflag){
            Toast.makeText(context,"Error changing to the specified remote directory!...Check that remote directory",Toast.LENGTH_LONG).show();
            return;
        }
        else {
            if(!save)
                Toast.makeText(context, "All good!...Hit that save button", Toast.LENGTH_LONG).show();
            else{
                if(mode == 0 )
                    activityAddServer.addToDatabase();
                else
                    activityEditServer.addToDatabase();
            }
        }
    }
}
