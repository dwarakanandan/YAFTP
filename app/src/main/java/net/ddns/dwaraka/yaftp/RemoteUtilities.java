package net.ddns.dwaraka.yaftp;


import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.io.IOException;
import java.net.SocketException;
import java.util.ArrayList;

public class RemoteUtilities extends AsyncTask<Object,Integer,Object> {

    String mode;
    ListFragment listFragment;
    ClientActivity.MyPagerAdapter adapter;
    Context context;
    String ipAddress,username,password;
    int port;
    String root,folderName,newName,movedFromLocation,pasteLocation;
    ArrayList<String> deletedItemsList = new ArrayList<>();
    ArrayList<String> movedItemList = new ArrayList<>();
    Boolean folderDeletion = true,folderCreation=true,fileRename = true;
    ProgressDialog deleteprogressDialog,moveProgressDialog;


    public void setMode(String mode,ListFragment listFragment,ClientActivity.MyPagerAdapter adapter){
        this.mode=mode;
        this.listFragment=listFragment;
        this.adapter=adapter;
        this.context = listFragment.getActivity().getBaseContext();
    }

    public void setDeleteParameters(String root,ArrayList<String> deletedItemsList){
        this.root=root;
        this.deletedItemsList = deletedItemsList;
    }
    public void setConnectionParameters(String ipAddress,int port,String username,String password){
        this.ipAddress=ipAddress;
        this.username=username;
        this.password=password;
        this.port =port;
    }
    public void setFolderCreationParameters(String root,String folderName){
        this.root=root;
        this.folderName=folderName;
    }
    public void setRenameParameters(String root,String folderName,String newName){
        this.root=root;
        this.folderName=folderName;
        this.newName = newName;
    }
    public void setMoveParameters(String movedFromLocation,ArrayList<String> selectedItemsMove,String pasteLocation){
        this.root = pasteLocation;
        this.movedFromLocation = movedFromLocation;
        this.movedItemList = selectedItemsMove;
        this.pasteLocation = pasteLocation;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        switch (mode){
            case "DELETE":
                deleteprogressDialog= new ProgressDialog(listFragment.getActivity());
                deleteprogressDialog.setTitle("Deleting...");
                deleteprogressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                deleteprogressDialog.setCancelable(false);
                deleteprogressDialog.setMax(deletedItemsList.size());
                deleteprogressDialog.setProgress(0);
                deleteprogressDialog.show();
                break;
            case "MOVE":
                moveProgressDialog = new ProgressDialog(listFragment.getActivity());
                moveProgressDialog.setTitle("Moving...");
                moveProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                moveProgressDialog.setCancelable(false);
                moveProgressDialog.setMax(movedItemList.size());
                moveProgressDialog.setProgress(0);
                moveProgressDialog.show();
                break;
        }
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        switch (mode) {
            case "DELETE":
                deleteprogressDialog.setProgress(values[0]);
                break;
            case "MOVE":
                moveProgressDialog.setProgress(values[0]);
                break;
        }
    }

    @Override
    protected Object doInBackground(Object[] params) {
        FTPClient client = new FTPClient();
        try {
            client.connect(ipAddress, port);
            client.enterLocalPassiveMode();
            //Log.e("dwaraka", "server found");
            if(!client.login(username, password))
                return null;
            //Log.e("dwaraka", "success login");
            if(!client.changeWorkingDirectory(root)) {
                Log.e("dwaraka","failed to change directory to "+root);
                return null;
            }
            //Log.e("dwaraka", "pwd = " + client.printWorkingDirectory());
            String workingDirectory = client.printWorkingDirectory();
            FTPFile[] directoryListing = client.listFiles(workingDirectory);
            switch (mode){
                case "DELETE":
                    int deleteProgress =1;
                    for(String i:deletedItemsList){
                        for(FTPFile f:directoryListing){
                            if(f.getName().equals(i)){
                                if(f.isDirectory()) {
                                    deleteDirectory(client, root + File.separator + f.getName(), "");
                                }
                                else
                                    deleteFile(client,root+File.separator+f.getName());
                            }
                        }
                        publishProgress(deleteProgress++);
                    }
                    break;
                case "MOVE":
                    int moveProgress =1;
                    for(String i:movedItemList){
                        rename(client, movedFromLocation + File.separator + i, pasteLocation + File.separator + i);
                        publishProgress(moveProgress++);
                    }
                    break;
                case "RENAME":
                    rename(client,root+File.separator+folderName,root+File.separator+newName);
                    break;
                case "CREATE_FOLDER":
                    createFolder(client);
                    break;
            }
        }catch (SocketException e){
            Log.e("dwaraka", "SocketException");
            e.printStackTrace();
        }
        catch (IOException e) {
            Log.e("dwaraka","IOException");
            e.printStackTrace();
        }
        return null;
    }


    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        switch (mode){
            case "DELETE":
                deleteprogressDialog.dismiss();
                if(folderDeletion)
                    Toast.makeText(context, deletedItemsList.size() + " item(s) deleted!", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context,"Some items could not be deleted!",Toast.LENGTH_SHORT).show();
                ClientActivity.MyPagerAdapter.changedFragment=listFragment;
                adapter.notifyDataSetChanged();
                break;
            case "CREATE_FOLDER":
                if(folderCreation){
                    Toast.makeText(context, folderName + " created successfully!", Toast.LENGTH_SHORT).show();
                    ClientActivity.MyPagerAdapter.changedFragment=listFragment;
                    adapter.notifyDataSetChanged();
                }
                else
                    Toast.makeText(context,folderName+" could not be created!",Toast.LENGTH_SHORT).show();
                break;
            case "RENAME":
                if(fileRename) {
                    Toast.makeText(context, "Item renamed successfully!", Toast.LENGTH_SHORT).show();
                    ClientActivity.MyPagerAdapter.changedFragment=listFragment;
                    adapter.notifyDataSetChanged();
                }
                else
                    Toast.makeText(context,"Item could not be renamed!",Toast.LENGTH_SHORT).show();
                break;
            case "MOVE":
                moveProgressDialog.dismiss();
                if(fileRename)
                    Toast.makeText(context, movedItemList.size()+" item(s) moved successfully!", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context,"Some items could not be moved!",Toast.LENGTH_SHORT).show();
                ClientActivity.MyPagerAdapter.changedFragment=listFragment;
                adapter.notifyDataSetChanged();
                ListFragment.remoteSelectedItemsMove.clear();
                break;
        }
    }


    public void createFolder(FTPClient ftpClient) {
        String dest = root+File.separator+folderName;
        try {
            ftpClient.makeDirectory(dest);
        } catch (IOException e) {
            folderCreation=false;
            e.printStackTrace();
        }
    }

    public void rename(FTPClient ftpClient,String old_name,String new_Name){
        try {
            ftpClient.rename(old_name, new_Name);
        } catch (IOException e) {
            fileRename = false;
            e.printStackTrace();
        }
    }

    public void deleteDirectory(FTPClient ftpClient, String parentDir,String currentDir) throws IOException {
        String dirToList = parentDir;
        if (!currentDir.equals("")) {
            dirToList += "/" + currentDir;
        }

        FTPFile[] subFiles = ftpClient.listFiles(dirToList);

        if (subFiles != null) {
            if( subFiles.length > 0) {
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

                    if (aFile.isDirectory()) {
                        // remove the sub directory
                        deleteDirectory(ftpClient, dirToList, currentFileName);
                    } else {
                        // delete the file
                        boolean deleted = ftpClient.deleteFile(filePath);
                        if (deleted) {
                            //deleted the file
                        } else {
                            //cannot delete file
                            folderDeletion = false;
                        }
                    }
                }
            }
            // finally, remove the directory itself
            boolean removed = ftpClient.removeDirectory(dirToList);
            if (removed) {
                //removed the directory
            } else {
                folderDeletion = false;
            }
        }
    }

    public void deleteFile(FTPClient ftpClient,String filePath) throws IOException{
        Boolean deleted = ftpClient.deleteFile(filePath);
        if (deleted) {
            //deleted the file
        } else {
            folderDeletion = false;
        }
    }

}
