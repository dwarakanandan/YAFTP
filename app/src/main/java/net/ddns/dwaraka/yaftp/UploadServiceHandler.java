package net.ddns.dwaraka.yaftp;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.apache.commons.io.input.CountingInputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.ArrayList;

public class UploadServiceHandler extends IntentService {

    String ipAddress,username,password;
    int port;
    String savePath;
    ArrayList<String> uploadItemsList = new ArrayList<>();
    int count=1,old=0,id=1,mod=100;
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    Boolean uploadSuccess = true,alreadyExists = false;

    public UploadServiceHandler() {
        super("UploadServiceHandler");
    }

    public void setParameters(Bundle bundle){
        this.ipAddress = bundle.getString("IP");
        this.username = bundle.getString("USERNAME");
        this.password = bundle.getString("PASSWORD");
        this.port = bundle.getInt("PORT");
        this.savePath = bundle.getString("REMOTE_SAVE_PATH");
        this.uploadItemsList = bundle.getStringArrayList("UPLOAD_LIST");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        setParameters(extras);
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Upload")
                .setContentText("Upload in progress")
                .setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setProgress(100, 0, false);
        startForeground(id, mBuilder.build());
        FTPClient client = new FTPClient();
        try {
            client.connect(ipAddress, port);
            client.enterLocalPassiveMode();
            //Log.e("dwaraka", "server found");
            if(!client.login(username, password))
                return;
            //Log.e("dwaraka", "success login");
            if(!client.changeWorkingDirectory(savePath))
                return;
            //Log.e("dwaraka", "pwd = " + client.printWorkingDirectory());
            for(String i:uploadItemsList){
                mBuilder.setContentTitle("Uploading item " + count + " / " + uploadItemsList.size());
                File file = new File(i);
                FTPFile[] list = client.listFiles(savePath);
                alreadyExists = false;
                for( FTPFile ftpFile:list ) {
                    if (ftpFile.getName().equals(file.getName())){
                        alreadyExists = true;
                        break;
                    }
                }
                if(alreadyExists) {
                    count++;
                    continue;
                }
                if(file.getName().startsWith("."))
                    savePath = savePath+ File.separator +file.getName().substring(1);
                else
                    savePath = savePath+ File.separator +file.getName();
                if(uploadSingleFile(client, file.getAbsolutePath(), savePath, file.getName(), file.length()));
                else uploadSuccess=false;
                count++;
            }
            client.disconnect();
        }catch (SocketException e){
            Log.e("dwaraka", "SocketException");
            uploadSuccess = false;
            e.printStackTrace();
        }
        catch (IOException e) {
            Log.e("dwaraka","IOException");
            uploadSuccess = false;
            e.printStackTrace();
        }

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(300);
        SystemClock.sleep(500);
        v.vibrate(400);

        stopForeground(true);
        if(alreadyExists){
            mBuilder.setContentTitle("Upload complete!");
            mBuilder.setContentText("Some files skipped since they already existed on the server!");
        }
        else if(uploadSuccess) {
            mBuilder.setContentTitle("Upload complete!");
            mBuilder.setContentText("All files have been uploaded successfully!");
        }
        else {
            mBuilder.setContentTitle("Error Uploading!");
            mBuilder.setContentText("Some files could not be uploaded!");
        }
        mBuilder.setProgress(0, 0, false);
        mNotifyManager.notify(id, mBuilder.build());
    }

    public boolean uploadSingleFile(final FTPClient ftpClient,
                                    String localFilePath, final String remoteFilePath, final String name, final Long size) throws IOException {
        File localFile = new File(localFilePath);
        mBuilder.setProgress(100, 0, false);
        mBuilder.setContentText(0+" %           " + name);
        mNotifyManager.notify(id, mBuilder.build());
        old=-1;

        if(size>1000)
            mod=100;
        if(size>1000000)
            mod=50;
        if(size>10000000)
            mod=25;
        if(size>50000000)
            mod=10;
        if(size>100000000)
            mod=5;
        if(size>500000000)
            mod=1;
        final InputStream inputStream = new FileInputStream(localFile);
        CountingInputStream cis = new CountingInputStream(inputStream){
            protected void afterRead(int n) {
                super.afterRead(n);
                int percentage =(int)((getByteCount() * 100) / size);
                if(old!=percentage&&(percentage % mod) == 0) {
                    mBuilder.setProgress(100, percentage, false);
                    mBuilder.setContentText(percentage + " %           " + name);
                    mNotifyManager.notify(id, mBuilder.build());
                    old=percentage;
                }
            }
        };
        try {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            return ftpClient.storeFile(remoteFilePath, cis);
        } finally {
            inputStream.close();
            cis.close();
        }
    }
}
