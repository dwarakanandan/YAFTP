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
import android.support.v4.app.NotificationCompat.Builder;
import android.util.Log;

import org.apache.commons.io.output.CountingOutputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.SocketException;
import java.util.ArrayList;


public class RemoteDownloadService extends IntentService {
    String ipAddress,username,password;
    int port;
    String root,savePath;
    ArrayList<String> downloadItemsList = new ArrayList<>();
    int count=1,old=0,id=1,mod=100;
    NotificationManager mNotifyManager;
    Builder mBuilder;
    Boolean downloadSuccess = true;

    public RemoteDownloadService() {
        super("RemoteDownloadService");
    }

    public void setParameters(Bundle bundle){
        this.ipAddress = bundle.getString("IP");
        this.username = bundle.getString("USERNAME");
        this.password = bundle.getString("PASSWORD");
        this.port = bundle.getInt("PORT");
        this.root = bundle.getString("ROOT");
        this.savePath = bundle.getString("SAVE_PATH");
        this.downloadItemsList = bundle.getStringArrayList("DOWNLOAD_LIST");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Messenger messageHandler;
        Bundle extras = intent.getExtras();
        setParameters(extras);
        messageHandler = (Messenger) extras.get("MESSENGER");
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle("Download")
                .setContentText("Download in progress")
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
            if(!client.changeWorkingDirectory(root))
                return;
            //Log.e("dwaraka", "pwd = " + client.printWorkingDirectory());
            String workingDirectory = client.printWorkingDirectory();
            FTPFile[] directoryListing = client.listFiles(workingDirectory);
            for(String i:downloadItemsList){
                mBuilder.setContentTitle("Downloading item " + count + " / " + downloadItemsList.size());
                for(FTPFile f:directoryListing){
                    if(f.getName().equals(i)){
                        String remoteFilePath = f.getName();
                        if(f.isDirectory()) {
                            downloadDirectory(client, remoteFilePath, "", savePath);
                        }
                        else {
                            if(downloadSingleFile(client,remoteFilePath,savePath+ File.separator+f.getName(),f.getSize(),f.getName()));
                            else downloadSuccess=false;
                        }
                    }
                }
                count++;
            }
            client.disconnect();
        }catch (SocketException e){
            Log.e("dwaraka", "SocketException");
            downloadSuccess = false;
            e.printStackTrace();
        }
        catch (IOException e) {
            Log.e("dwaraka","IOException");
            downloadSuccess = false;
            e.printStackTrace();
        }

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(300);
        SystemClock.sleep(500);
        v.vibrate(400);
        Message message = Message.obtain();
        if(downloadSuccess)
            message.arg1 = 1;
        else
            message.arg1 = 0;
        try {
            messageHandler.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        stopForeground(true);
        if(downloadSuccess) {
            mBuilder.setContentTitle("Download complete!");
            mBuilder.setContentText("All files have been downloaded successfully!");
        }
        else {
            mBuilder.setContentTitle("Error Downloading!");
            mBuilder.setContentText("Some files could not be downloaded!");
        }
        mBuilder.setProgress(0, 0, false);
        mNotifyManager.notify(id, mBuilder.build());

    }

    public boolean downloadSingleFile(FTPClient ftpClient,String remoteFilePath, String savePath, final long size, final String name) throws IOException {
        File downloadFile = new File(savePath);
        File parentDir = downloadFile.getParentFile();
        if (!parentDir.exists()) {
            parentDir.mkdirs();
        }
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
        OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(downloadFile));
        CountingOutputStream cos = new CountingOutputStream(outputStream){
            protected void beforeWrite(int n){
                super.beforeWrite(n);
                int percentage =(int)((getByteCount() * 100) / size);
                if(old!=percentage&&(percentage % mod) == 0) {
                    mBuilder.setProgress(100, percentage, false);
                    mBuilder.setContentText(percentage+" %           " + name);
                    mNotifyManager.notify(id, mBuilder.build());
                    old=percentage;
                }
            }
        };
        try {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            return ftpClient.retrieveFile(remoteFilePath, cos);
        } catch (IOException ex) {
            throw ex;
        } finally {
            outputStream.close();
            cos.close();
        }
    }

    public void downloadDirectory(FTPClient ftpClient, String parentDir,String currentDir, String saveDir) throws IOException {
        String dirToList = parentDir;
        if (!currentDir.equals("")) {
            dirToList += "/" + currentDir;
        }

        FTPFile[] subFiles = ftpClient.listFiles(root+File.separator+dirToList);

        if (subFiles != null ) {
            if(subFiles.length > 0) {
                for (FTPFile aFile : subFiles) {
                    String currentFileName = aFile.getName();
                    if (currentFileName.equals(".") || currentFileName.equals("..")) {
                        // skip parent directory and the directory itself
                        continue;
                    }
                    String filePath = parentDir + "/" + currentDir + "/" + currentFileName;
                    if (currentDir.equals("")) {
                        filePath = parentDir + "/" + currentFileName;
                    }

                    String newDirPath = saveDir + File.separator + parentDir + File.separator
                            + currentDir + File.separator + currentFileName;
                    if (currentDir.equals("")) {
                        newDirPath = saveDir + File.separator + parentDir + File.separator + currentFileName;
                    }

                    if (aFile.isDirectory()) {
                        // create the directory in saveDir
                        File newDir = new File(newDirPath);
                        boolean created = newDir.mkdirs();
                        if (created) {
                            Log.d("dwaraka", "directory created " + newDir);
                        } else {
                            Log.d("dwaraka", " could not create directory " + newDir);
                        }
                        // download the sub directory
                        downloadDirectory(ftpClient, dirToList, currentFileName, saveDir);
                    } else {
                        // download the file
                        boolean success = downloadSingleFile(ftpClient, filePath, newDirPath,aFile.getSize(),aFile.getName());
                        if (success) {
                            Log.d("dwaraka", "downloaded the file " + filePath);
                        } else {
                            Log.d("dwaraka", "could not download the file " + filePath);
                            downloadSuccess = false;
                        }
                    }
                }
            }
            else{
                String newDirPath = saveDir + File.separator + parentDir + File.separator+ currentDir;
                if (currentDir.equals("")) {
                    newDirPath = saveDir + File.separator + parentDir;
                }
                File newDir = new File(newDirPath);
                boolean created = newDir.mkdirs();
                if (created) {
                    Log.d("dwaraka", "directory created " + newDir);
                } else {
                    Log.d("dwaraka", " could not create directory " + newDir);
                }
            }

        }
    }
}
