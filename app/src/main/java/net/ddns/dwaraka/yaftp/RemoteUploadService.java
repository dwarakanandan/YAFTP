package net.ddns.dwaraka.yaftp;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
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


public class RemoteUploadService extends IntentService {

    String ipAddress,username,password;
    int port;
    String localDir,savePath;
    ArrayList<String> uploadItemsList = new ArrayList<>();
    int count=1,old=0,id=1,mod=100;
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;
    Boolean uploadSuccess = true,alreadyExists = false;

    public RemoteUploadService() {
        super("RemoteUploadService");
    }

    public void setParameters(Bundle bundle){
        this.ipAddress = bundle.getString("IP");
        this.username = bundle.getString("USERNAME");
        this.password = bundle.getString("PASSWORD");
        this.port = bundle.getInt("PORT");
        this.localDir = bundle.getString("LOCAL_DIR");
        this.savePath = bundle.getString("REMOTE_SAVE_PATH");
        this.uploadItemsList = bundle.getStringArrayList("UPLOAD_LIST");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Messenger messageHandler;
        Bundle extras = intent.getExtras();
        setParameters(extras);
        messageHandler = (Messenger) extras.get("MESSENGER");
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
                File file = new File(localDir+File.separator+i);
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
                if(file.isDirectory()) {
                    client.makeDirectory(savePath + File.separator + file.getName());
                    uploadDirectory(client, savePath + File.separator + file.getName(), file.getAbsolutePath(), "");
                }
                else{
                    if(uploadSingleFile(client,file.getAbsolutePath(),savePath + File.separator + file.getName(),file.getName(),file.length()));
                    else uploadSuccess=false;
                }
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
        Message message = Message.obtain();
        if(alreadyExists)
            message.arg1 = 2;
        else if(uploadSuccess)
            message.arg1 = 1;
        else
            message.arg1 = 0;
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
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


    public void uploadDirectory(FTPClient ftpClient,
                                       String remoteDirPath, String localParentDir, String remoteParentDir)
            throws IOException {

        System.out.println("LISTING directory: " + localParentDir);

        File localDir = new File(localParentDir);
        File[] subFiles = localDir.listFiles();
        if (subFiles != null && subFiles.length > 0) {
            for (File item : subFiles) {
                String remoteFilePath = remoteDirPath + "/" + remoteParentDir
                        + "/" + item.getName();
                if (remoteParentDir.equals("")) {
                    remoteFilePath = remoteDirPath + "/" + item.getName();
                }

                if (item.isFile()) {
                    // upload_dialog the file
                    String localFilePath = item.getAbsolutePath();
                    System.out.println("About to upload_dialog the file: " + localFilePath);
                    boolean uploaded = uploadSingleFile(ftpClient,
                            localFilePath, remoteFilePath,item.getName(),item.length());
                    if (uploaded) {
                        System.out.println("UPLOADED a file to: "
                                + remoteFilePath);
                    } else {
                        System.out.println("COULD NOT upload_dialog the file: "
                                + localFilePath);
                        uploadSuccess = false;
                    }
                } else {
                    // create directory on the server
                    boolean created = ftpClient.makeDirectory(remoteFilePath);
                    if (created) {
                        System.out.println("CREATED the directory: "
                                + remoteFilePath);
                    } else {
                        System.out.println("COULD NOT create the directory: "
                                + remoteFilePath);
                    }
                    // upload_dialog the sub directory
                    String parent = remoteParentDir + "/" + item.getName();
                    if (remoteParentDir.equals("")) {
                        parent = item.getName();
                    }

                    localParentDir = item.getAbsolutePath();
                    uploadDirectory(ftpClient, remoteDirPath, localParentDir,
                            parent);
                }
            }
        }
    }
}
