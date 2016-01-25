package net.ddns.dwaraka.yaftp;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


public class LocalUtilities extends AsyncTask<Object,Integer,Object> {
    private String mode;
    ListFragment listFragment;
    Context context;
    private String root;
    private String folderName,newName;
    private boolean folderCreation,folderExists,folderDeletion = true,folderCopied = true;
    public ClientActivity.MyPagerAdapter adapter;
    private ArrayList<String> deletedItemsList = new ArrayList<>();
    ProgressDialog deleteprogressDialog,copyprogressDialog;
    private ArrayList<String> copyItemsList = new ArrayList<>();
    String copiedFromLocation,pasteLocation;

    public void setMode(String mode,ListFragment listFragment,ClientActivity.MyPagerAdapter adapter){
        this.mode=mode;
        this.listFragment=listFragment;
        this.adapter=adapter;
        this.context = listFragment.getActivity().getBaseContext();
    }

    public void setFolderCreationParameters(String root,String folderName){
        this.root=root;
        this.folderName=folderName;
    }

    public void setDeleteParameters(String root,ArrayList<String> deletedItemsList){
        this.root=root;
        this.deletedItemsList = deletedItemsList;
    }

    public void setCopyParameters(String copiedFromLoaction,ArrayList<String> copyItemsList,String pasteLocation){
        this.copiedFromLocation = copiedFromLoaction;
        this.copyItemsList = copyItemsList;
        this.pasteLocation = pasteLocation;
    }

    public void setRenameParameters(String root,String folderName,String newName){
        this.root=root;
        this.folderName=folderName;
        this.newName = newName;
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
            case "COPY":
                copyprogressDialog= new ProgressDialog(listFragment.getActivity());
                copyprogressDialog.setTitle("Copying...");
                copyprogressDialog.setCancelable(false);
                copyprogressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                copyprogressDialog.setMax(copyItemsList.size());
                copyprogressDialog.setProgress(0);
                copyprogressDialog.show();
                break;
            case "MOVE":
                copyprogressDialog= new ProgressDialog(listFragment.getActivity());
                copyprogressDialog.setTitle("Moving...");
                copyprogressDialog.setCancelable(false);
                copyprogressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                copyprogressDialog.setMax(copyItemsList.size());
                copyprogressDialog.setProgress(0);
                copyprogressDialog.show();
                break;

        }

    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        super.onProgressUpdate(values);
        switch (mode) {
            case "DELETE":
                deleteprogressDialog.setProgress(values[0]);
                break;
            case "COPY":
                copyprogressDialog.setProgress(values[0]);
                break;
            case "MOVE":
                copyprogressDialog.setProgress(values[0]);
                break;
        }
    }

    @Override
    protected Object doInBackground(Object[] params) {
        switch (mode){
            case "CREATE_FOLDER":
                createFolder();
                break;
            case "DELETE":
                int deleteProgress=1;
                for(String i:deletedItemsList){
                    File file = new File(root+File.separator+i);
                    if(file.isDirectory())
                        deleteFolder(file);
                    else if(file.isFile())
                        deleteFile(file);
                    publishProgress(deleteProgress++);
                }
                break;
            case "COPY":
                int copyProgress=1;
                for(String i:copyItemsList){
                    File file = new File(copiedFromLocation+File.separator+i);
                    if(file.isDirectory())
                        copyFolder(file, new File(pasteLocation + File.separator + file.getName()));
                    else if(file.isFile())
                        copyFile(file, new File(pasteLocation + File.separator + file.getName()));
                    publishProgress(copyProgress++);
                }
                break;
            case "MOVE":
                int moveProgress=1;
                for(String i:copyItemsList){
                    File file = new File(copiedFromLocation+File.separator+i);
                    if(file.isDirectory())
                        moveFolder(file, new File(pasteLocation + File.separator + file.getName()));
                    else if(file.isFile())
                        moveFile(file, new File(pasteLocation + File.separator + file.getName()));
                    publishProgress(moveProgress++);
                }
                break;
            case "RENAME":
                File file = new File(root+File.separator+folderName);
                if(file.isDirectory())
                    moveFolder(file, new File(root + File.separator + newName));
                else if(file.isFile())
                    moveFile(file,new File(root+File.separator+newName));
                break;

        }


        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        switch (mode){
            case "CREATE_FOLDER":
                if(folderCreation&&!folderExists) {
                    Toast.makeText(context, folderName + " created successfully!", Toast.LENGTH_SHORT).show();
                    ClientActivity.MyPagerAdapter.changedFragment=listFragment;
                    adapter.notifyDataSetChanged();
                }
                else if(folderExists)
                    Toast.makeText(context,folderName+" already exists!",Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context,folderName+" could not be created!",Toast.LENGTH_SHORT).show();
                break;
            case "DELETE":
                deleteprogressDialog.dismiss();
                if(folderDeletion)
                    Toast.makeText(context,deletedItemsList.size()+" item(s) deleted!",Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context,"Some items could not be deleted!",Toast.LENGTH_SHORT).show();
                ClientActivity.MyPagerAdapter.changedFragment=listFragment;
                adapter.notifyDataSetChanged();
                break;
            case "COPY":
                copyprogressDialog.dismiss();
                if(folderCopied)
                    Toast.makeText(context,copyItemsList.size()+" item(s) copied!",Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context,"Some items could not be copied!",Toast.LENGTH_SHORT).show();
                ListFragment.localSelectedItemsCopy.clear();
                ClientActivity.MyPagerAdapter.changedFragment=listFragment;
                adapter.notifyDataSetChanged();
                break;
            case "MOVE":
                copyprogressDialog.dismiss();
                if(folderCopied)
                    Toast.makeText(context,copyItemsList.size()+" item(s) moved!",Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context,"Some items could not be moved!",Toast.LENGTH_SHORT).show();
                ListFragment.localSelectedItemsCopy.clear();
                ClientActivity.MyPagerAdapter.changedFragment=listFragment;
                adapter.notifyDataSetChanged();
                break;
            case "RENAME":
                if(folderCopied)
                    Toast.makeText(context, "Item renamed successfully!", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(context,"Item could not be renamed!",Toast.LENGTH_SHORT).show();
                ClientActivity.MyPagerAdapter.changedFragment=listFragment;
                adapter.notifyDataSetChanged();
                break;

        }
    }

    public void createFolder(){
        String dest = root+File.separator+folderName;
        File file = new File(dest);
        if(file.exists())
            folderExists=true;
        folderCreation = file.mkdir();
    }

    public void deleteFolder(File file){
        try {
            FileUtils.deleteDirectory(file);
        } catch (IOException e) {
            folderDeletion=false;
            Log.e("dwaraka","IOexception,Could not delete "+file.getName());
            e.printStackTrace();
        }
    }

    public void deleteFile(File file){
        if(file.delete()){}
        else{
            folderDeletion=false;
            Log.e("dwaraka", "IOexception,Could not delete " + file.getName());
        }
    }

    public void moveFolder(File src,File dest){
        try {
            FileUtils.moveDirectory(src, dest);
        } catch (IOException e) {
            folderCopied = false;
            Log.e("dwaraka","IOexception,Could not move "+src.getName());
            e.printStackTrace();
        }

    }

    public void moveFile(File src,File dest){
        try {
            FileUtils.moveFile(src, dest);
        } catch (IOException e) {
            folderCopied = false;
            Log.e("dwaraka","IOexception,Could not move "+src.getName());
            e.printStackTrace();
        }
    }



    public void copyFolder(File src,File dest){
        try {
            FileUtils.copyDirectory(src,dest);
        } catch (IOException e) {
            folderCopied = false;
            Log.e("dwaraka","IOexception,Could not copy "+src.getName());
            e.printStackTrace();
        }

    }

    public void copyFile(File src,File dest){
        try {
            FileUtils.copyFile(src,dest);
        } catch (IOException e) {
            folderCopied = false;
            Log.e("dwaraka","IOexception,Could not copy "+src.getName());
            e.printStackTrace();
        }
    }
}
