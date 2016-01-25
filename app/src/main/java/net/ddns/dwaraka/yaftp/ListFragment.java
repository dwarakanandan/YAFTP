package net.ddns.dwaraka.yaftp;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.net.ftp.FTPFile;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

public class ListFragment extends Fragment {
    View fragmentView;
    String root,backup,pageType;
    File file;
    ListView listView;
    TextView textView;
    File[] localfileList;
    FTPFile[] remotefileList;
    ListFragment current;
    Menu menu;
    ArrayList<String> localSelectedItems = new ArrayList<>();
    ArrayList<String> remoteSelectedItems = new ArrayList<>();
    ArrayList<Integer> localSelectedIndex = new ArrayList<>();
    ArrayList<Integer> remoteSelectedIndex = new ArrayList<>();
    CustomArrayAdapter localAdapter,remoteAdapter;
    String changedFragmentID;
    static ArrayList<String> localSelectedItemsCopy = new ArrayList<>();
    static ArrayList<String> remoteSelectedItemsMove = new ArrayList<>();
    static Boolean localGlobalCopyFlag = false,localGlobalMoveFlag = false,remoteGlobalMoveFlag = false;
    static String localCopiedFromLocation,localPasteLocation,remoteMovedFromLocation,remotePasteLocation;

    public static ClientActivity.MyPagerAdapter adapter;
    public static ViewPager viewPager;

    public ListFragment(){
        current = this;
    }

    public static ListFragment newInstance(Bundle args,ClientActivity.MyPagerAdapter m,ViewPager v) {
        ListFragment fragment = new ListFragment();
        fragment.setArguments(args);
        adapter=m;
        viewPager=v;
        return fragment;
    }

    public void setChangedFragmentID(String changedFragmentID){
        this.changedFragmentID=changedFragmentID;
    }


    @Override
    public void onCreateOptionsMenu(Menu menu1, MenuInflater inflater) {
        menu = menu1;
        inflater.inflate(R.menu.action_bar, menu1);
        if(this.getArguments().getString("pageType").equals("local")) {
            updateLocalActionBar();
        }
        else{
            updateRemoteActionBar();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.plus){
            if(this.getArguments().getString("pageType").equals("local")) {
                createNewFolderLocal();
            }
            else {
                createNewFolderRemote();
            }
            return true;
        }
        if(item.getItemId() == R.id.trash){
            if(this.getArguments().getString("pageType").equals("local")){
                deleteSelectedItemsLocal();
            }
            else{
                deleteSelectedItemsRemote();
            }
            return true;
        }
        if(item.getItemId() == R.id.clearselection){
            if(this.getArguments().getString("pageType").equals("local")){
                clearLocalSelection();
            }
            else{
                clearRemoteSelection();
            }
            return true;
        }
        if(item.getItemId() == R.id.selectall){
            if(this.getArguments().getString("pageType").equals("local")){
                localSelectedItems.clear();
                localSelectedIndex.clear();
                if(localfileList != null) {
                    for (int i = 0; i < localfileList.length; i++)
                        localItemClickHandler(i);
                    localAdapter.notifyDataSetChanged();
                    updateLocalActionBar();
                }
            }
            else{
                remoteSelectedItems.clear();
                remoteSelectedIndex.clear();
                if(remotefileList != null) {
                    for (int i = 0; i < remotefileList.length; i++)
                        remoteItemClickHandler(i);
                    remoteAdapter.notifyDataSetChanged();
                    updateRemoteActionBar();
                }
            }
            return true;
        }
        if(item.getItemId()== R.id.copy){
            localCopiedFromLocation = root;
            for (String i:localSelectedItems)
                localSelectedItemsCopy.add(i);
            localGlobalCopyFlag =true;
            clearLocalSelection();
            updateLocalActionBar();
            return true;
        }
        if(item.getItemId() == R.id.copyPaste){
            localPasteLocation = root;
            localGlobalCopyFlag = false;
            copyPasteSelectedItems();
            updateLocalActionBar();
            return true;
        }
        if(item.getItemId() == R.id.cancelCopy){
            localGlobalCopyFlag = false;
            localSelectedItemsCopy.clear();
            updateLocalActionBar();
            return true;
        }
        if(item.getItemId() == R.id.move){
            if(this.getArguments().getString("pageType").equals("local")){
                localCopiedFromLocation = root;
                for (String i:localSelectedItems)
                    localSelectedItemsCopy.add(i);
                localGlobalMoveFlag =true;
                clearLocalSelection();
                updateLocalActionBar();
                return true;
            }
            else{
                remoteMovedFromLocation = root;
                for(String i :remoteSelectedItems)
                    remoteSelectedItemsMove.add(i);
                remoteGlobalMoveFlag = true;
                clearRemoteSelection();
                updateRemoteActionBar();
                return true;
            }
        }
        if(item.getItemId() == R.id.cancelMove){
            if(this.getArguments().getString("pageType").equals("local")){
                localGlobalMoveFlag = false;
                localSelectedItemsCopy.clear();
                updateLocalActionBar();
                return true;
            }
            else{
                remoteGlobalMoveFlag = false;
                remoteSelectedItemsMove.clear();
                updateRemoteActionBar();
                return  true;
            }
        }
        if(item.getItemId() == R.id.movePaste){
            if(this.getArguments().getString("pageType").equals("local")){
                localPasteLocation = root;
                localGlobalMoveFlag = false;
                movePasteSelectedItemsLocal();
                updateLocalActionBar();
                return true;
            }
            else{
                remotePasteLocation = root;
                remoteGlobalMoveFlag = false;
                movePasteSelectedItemsRemote();
                updateRemoteActionBar();
                return true;
            }
        }
        if(item.getItemId() == R.id.rename){
            if(this.getArguments().getString("pageType").equals("local")){
                renameSelectedItemLocal();
            }
            else{
                renameSelectedItemRemote();
            }
        }
        if(item.getItemId() == R.id.download){
            downloadSelectedItems();
        }
        if(item.getItemId() == R.id.upload){
            uploadSelectedItems();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        fragmentView = inflater.inflate(R.layout.fragment, container, false);
        root = this.getArguments().getString("path");
        pageType = this.getArguments().getString("pageType");
        listView = (ListView) fragmentView.findViewById(R.id.list_view);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        textView = (TextView) fragmentView.findViewById(R.id.textView);
        setHasOptionsMenu(true);
        if(pageType.equals("local"))
            LocalListHandler();
        else
            RemoteListHandler();
        return fragmentView;
    }

    public void setLocalLists(File[] gotFileList){
        localfileList=gotFileList;//can get absolute path of file
    }

    public void setLocalAdapter(CustomArrayAdapter gotcustomArrayAdapter){
        localAdapter = gotcustomArrayAdapter;
    }


    public void LocalListHandler(){
        file = new File(root);
        if(file.list() == null){
            textView.setText("Elevated Privileges may be required!");
            Toast.makeText(getActivity().getBaseContext(),"Could not change Directory!", Toast.LENGTH_SHORT).show();
            root=backup;
            return;
        }
        textView.setText(root);
        LocalDirectoryListing task = new LocalDirectoryListing(current,root,listView);
        task.execute();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (localfileList[position].isDirectory()) {
                    if(localSelectedItems.size()>0){
                        localItemClickHandler(position);
                        localAdapter.notifyDataSetChanged();
                        updateLocalActionBar();
                    }
                    else{
                        backup = root;
                        if (root.equals("/"))
                            root = "/" + localfileList[position].getName();
                        else
                            root = root + "/" + localfileList[position].getName();
                        Bundle bundle = new Bundle();
                        bundle.putString("path", root);
                        bundle.putString("pageType", "local");
                        ClientActivity.MyPagerAdapter.setRootSecondPage(bundle);
                        ClientActivity.MyPagerAdapter.changedFragment=current;
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    localItemClickHandler(position);
                    localAdapter.notifyDataSetChanged();
                    updateLocalActionBar();
                }
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                localItemClickHandler(position);
                localAdapter.notifyDataSetChanged();
                updateLocalActionBar();
                return true;
            }
        });
    }

    public void setRemoteLists(FTPFile[] gotFilelist){
        remotefileList=gotFilelist;
    }

    public  void setRemoteAdapter(CustomArrayAdapter customArrayAdapter){
        remoteAdapter=customArrayAdapter;
    }

    public void RemoteListHandler(){
        if(!isNetworkAvailable())
            noNetworkHandler();
        RemoteDirectoryListing task = new RemoteDirectoryListing(current,root,listView,textView);
        task.setConnectionParameters(ClientActivity.HOST_NAME,ClientActivity.PORT,ClientActivity.USERNAME,ClientActivity.PASSWORD);
        task.execute();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (remotefileList[position].isDirectory()) {
                    if (remoteSelectedItems.size() > 0) {
                        remoteItemClickHandler(position);
                        remoteAdapter.notifyDataSetChanged();
                        updateRemoteActionBar();
                    }
                    else {
                        root = root + "/" + remotefileList[position].getName();
                        Bundle bundle = new Bundle();
                        bundle.putString("path", root);
                        bundle.putString("pageType", "remote");
                        ClientActivity.MyPagerAdapter.setRootFirstPage(bundle);
                        ClientActivity.MyPagerAdapter.changedFragment=current;
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    remoteItemClickHandler(position);
                    remoteAdapter.notifyDataSetChanged();
                    updateRemoteActionBar();
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                remoteItemClickHandler(position);
                remoteAdapter.notifyDataSetChanged();
                updateRemoteActionBar();
                return true;
            }
        });

    }

    public void noNetworkHandler(){
        new AlertDialog.Builder(this.getActivity())
                .setIcon(R.drawable.alert)
                .setTitle("Unable to access the network!")
                .setMessage("You must be connected to the Internet for YAFTP to work..Please connect to a network and try again. ")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //finish();
                    }

                })
                .show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) this.getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null;
    }

    public void localItemClickHandler(int position){

        if (localSelectedItems.contains(localfileList[position].getName())) {
            localSelectedItems.remove(localfileList[position].getName());
            localSelectedIndex.remove(localSelectedIndex.indexOf(position));
            localAdapter.setSelectedItem(localSelectedIndex);

        }
        else {
            localSelectedItems.add(localfileList[position].getName());
            localSelectedIndex.add(position);
            localAdapter.setSelectedItem(localSelectedIndex);

        }

    }

    public void remoteItemClickHandler( int position){
        if(remoteSelectedItems.contains(remotefileList[position].getName())){
            remoteSelectedItems.remove(remotefileList[position].getName());
            remoteSelectedIndex.remove(remoteSelectedIndex.indexOf(position));
            remoteAdapter.setSelectedItem(remoteSelectedIndex);

        }
        else {
            remoteSelectedItems.add(remotefileList[position].getName());
            remoteSelectedIndex.add(position);
            remoteAdapter.setSelectedItem(remoteSelectedIndex);

        }

    }

    public static String getStringFileSize(double bytes){
        double kilobytes = bytes/1024;
        double megabytes = kilobytes/1024;
        double gigabytes = megabytes/1024;
        String rval = Double.toString(bytes)+" Bytes";
        if(kilobytes<1){}
        else if(megabytes<1){
            DecimalFormat numberFormat = new DecimalFormat("#.00");
            rval = numberFormat.format(kilobytes).toString()+" KB";
        }
        else if(gigabytes<1){
            DecimalFormat numberFormat = new DecimalFormat("#.00");
            rval = numberFormat.format(megabytes).toString()+" MB";
        }
        else {
            DecimalFormat numberFormat = new DecimalFormat("#.00");
            rval = numberFormat.format(gigabytes).toString()+" GB";
        }
        return rval;
    }

    public void updateLocalActionBar(){
        menu.findItem(R.id.download).setVisible(false);
        menu.findItem(R.id.copyPaste).setVisible(false);
        menu.findItem(R.id.cancelCopy).setVisible(false);
        menu.findItem(R.id.movePaste).setVisible(false);
        menu.findItem(R.id.cancelMove).setVisible(false);
        menu.findItem(R.id.plus).setVisible(true);
        if (localGlobalCopyFlag){
            menu.findItem(R.id.rename).setVisible(false);
            menu.findItem(R.id.clearselection).setVisible(false);
            menu.findItem(R.id.selectall).setVisible(false);
            menu.findItem(R.id.trash).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.copy).setVisible(false);
            menu.findItem(R.id.plus).setVisible(false);
            menu.findItem(R.id.movePaste).setVisible(false);
            menu.findItem(R.id.cancelMove).setVisible(false);
            menu.findItem(R.id.upload).setVisible(false);
            menu.findItem(R.id.copyPaste).setVisible(true);
            menu.findItem(R.id.cancelCopy).setVisible(true);
            return;
        }
        if (localGlobalMoveFlag){
            menu.findItem(R.id.rename).setVisible(false);
            menu.findItem(R.id.clearselection).setVisible(false);
            menu.findItem(R.id.selectall).setVisible(false);
            menu.findItem(R.id.trash).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.copy).setVisible(false);
            menu.findItem(R.id.plus).setVisible(false);
            menu.findItem(R.id.copyPaste).setVisible(false);
            menu.findItem(R.id.cancelCopy).setVisible(false);
            menu.findItem(R.id.upload).setVisible(false);
            menu.findItem(R.id.movePaste).setVisible(true);
            menu.findItem(R.id.cancelMove).setVisible(true);
            return;
        }
        if(localSelectedItems.size()>0) {
            if(localSelectedItems.size()==1) {
                menu.findItem(R.id.rename).setVisible(true);
                ClientActivity.myToolbar.setTitle(localSelectedItems.size() + " item selected");
            }
            else {
                menu.findItem(R.id.rename).setVisible(false);
                ClientActivity.myToolbar.setTitle(localSelectedItems.size() + " items selected");
            }
            if(localSelectedItems.size()==localfileList.length)
                menu.findItem(R.id.selectall).setVisible(false);
            else
                menu.findItem(R.id.selectall).setVisible(true);
            menu.findItem(R.id.clearselection).setVisible(true);
            menu.findItem(R.id.move).setVisible(true);
            menu.findItem(R.id.copy).setVisible(true);
            menu.findItem(R.id.trash).setVisible(true);
            menu.findItem(R.id.upload).setVisible(true);
        }
        if(localSelectedItems.size()==0) {
            menu.findItem(R.id.rename).setVisible(false);
            menu.findItem(R.id.clearselection).setVisible(false);
            menu.findItem(R.id.selectall).setVisible(true);
            menu.findItem(R.id.trash).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.copy).setVisible(false);
            menu.findItem(R.id.upload).setVisible(false);
            ClientActivity.myToolbar.setTitle(ClientActivity.SERVER_NAME);
        }
    }

    public void updateRemoteActionBar(){
        menu.findItem(R.id.copy).setVisible(false);
        menu.findItem(R.id.copyPaste).setVisible(false);
        menu.findItem(R.id.cancelCopy).setVisible(false);
        menu.findItem(R.id.upload).setVisible(false);
        menu.findItem(R.id.movePaste).setVisible(false);
        menu.findItem(R.id.cancelMove).setVisible(false);
        menu.findItem(R.id.plus).setVisible(true);
        if(remoteGlobalMoveFlag){
            menu.findItem(R.id.rename).setVisible(false);
            menu.findItem(R.id.clearselection).setVisible(false);
            menu.findItem(R.id.selectall).setVisible(false);
            menu.findItem(R.id.trash).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
            menu.findItem(R.id.plus).setVisible(false);
            menu.findItem(R.id.download).setVisible(false);
            menu.findItem(R.id.movePaste).setVisible(true);
            menu.findItem(R.id.cancelMove).setVisible(true);
            return;
        }
        if(remoteSelectedItems.size()>0) {
            if(remoteSelectedItems.size()==1) {
                menu.findItem(R.id.rename).setVisible(true);
                ClientActivity.myToolbar.setTitle(remoteSelectedItems.size() + " item selected");
            }
            else {
                menu.findItem(R.id.rename).setVisible(false);
                ClientActivity.myToolbar.setTitle(remoteSelectedItems.size() + " items selected");
            }
            if(remoteSelectedItems.size()==remotefileList.length)
                menu.findItem(R.id.selectall).setVisible(false);
            else
                menu.findItem(R.id.selectall).setVisible(true);
            menu.findItem(R.id.clearselection).setVisible(true);
            menu.findItem(R.id.download).setVisible(true);
            menu.findItem(R.id.move).setVisible(true);
            menu.findItem(R.id.trash).setVisible(true);
        }
        if(remoteSelectedItems.size()==0) {
            menu.findItem(R.id.rename).setVisible(false);
            menu.findItem(R.id.selectall).setVisible(true);
            menu.findItem(R.id.clearselection).setVisible(false);
            menu.findItem(R.id.download).setVisible(false);
            menu.findItem(R.id.trash).setVisible(false);
            menu.findItem(R.id.move).setVisible(false);
            ClientActivity.myToolbar.setTitle(ClientActivity.SERVER_NAME);
        }
    }

    public void clearLocalSelection(){
        localSelectedItems.clear();
        localSelectedIndex.clear();
        localAdapter.notifyDataSetChanged();
        updateLocalActionBar();
    }

    public void clearRemoteSelection(){
        remoteSelectedItems.clear();
        remoteSelectedIndex.clear();
        remoteAdapter.notifyDataSetChanged();
        updateRemoteActionBar();
    }

    public void createNewFolderLocal(){
       final AlertDialog.Builder builder = new AlertDialog.Builder(current.getActivity());
        builder.setTitle("New Folder Name...");
        final EditText input = new EditText(current.getActivity());
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LocalUtilities task = new LocalUtilities();
                task.setMode("CREATE_FOLDER", current, adapter);
                task.setFolderCreationParameters(root, input.getText().toString());
                task.execute();
            }
        });
        builder.setNegativeButton("Cancel", null);
        final AlertDialog dialog = builder.show();
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    public void deleteSelectedItemsLocal(){
        new AlertDialog.Builder(this.getActivity())
                .setIcon(R.drawable.alert)
                .setTitle("Delete?...")
                .setMessage("Are you sure you want to delete "+localSelectedItems.size()+" item(s)")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LocalUtilities task = new LocalUtilities();
                        task.setMode("DELETE", current, adapter);
                        task.setDeleteParameters(root, localSelectedItems);
                        task.execute();
                    }
                })
                .setNegativeButton("No",null)
                .show();
    }

    public void copyPasteSelectedItems(){
        new AlertDialog.Builder(this.getActivity())
                .setTitle("Copy?...")
                .setMessage("Are you sure you want to copy "+localSelectedItemsCopy.size()+" item(s) to "+localPasteLocation)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LocalUtilities task = new LocalUtilities();
                        task.setMode("COPY", current, adapter);
                        task.setCopyParameters(localCopiedFromLocation, localSelectedItemsCopy, localPasteLocation);
                        task.execute();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        localGlobalCopyFlag = false;
                        localSelectedItemsCopy.clear();
                        updateLocalActionBar();
                    }
                })
                .show();
    }

    public void movePasteSelectedItemsLocal(){
        new AlertDialog.Builder(this.getActivity())
                .setTitle("Move?...")
                .setMessage("Are you sure you want to move "+localSelectedItemsCopy.size()+" item(s) to "+localPasteLocation)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        LocalUtilities task = new LocalUtilities();
                        task.setMode("MOVE", current, adapter);
                        task.setCopyParameters(localCopiedFromLocation, localSelectedItemsCopy, localPasteLocation);
                        task.execute();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        localGlobalMoveFlag = false;
                        localSelectedItemsCopy.clear();
                        updateLocalActionBar();
                    }
                })
                .show();
    }

    public void renameSelectedItemLocal(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(current.getActivity());
        builder.setTitle("New Name...");
        final EditText input = new EditText(current.getActivity());
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                LocalUtilities task = new LocalUtilities();
                task.setMode("RENAME", current, adapter);
                task.setRenameParameters(root,localSelectedItems.get(0),input.getText().toString());
                task.execute();
            }
        });
        builder.setNegativeButton("Cancel", null);
        final AlertDialog dialog = builder.show();
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    public void deleteSelectedItemsRemote(){
        new AlertDialog.Builder(this.getActivity())
                .setIcon(R.drawable.alert)
                .setTitle("Delete?...")
                .setMessage("Are you sure you want to delete "+remoteSelectedItems.size()+" item(s)")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RemoteUtilities task = new RemoteUtilities();
                        task.setConnectionParameters(ClientActivity.HOST_NAME,ClientActivity.PORT,ClientActivity.USERNAME,ClientActivity.PASSWORD);
                        task.setMode("DELETE", current, adapter);
                        task.setDeleteParameters(root, remoteSelectedItems);
                        task.execute();
                    }
                })
                .setNegativeButton("No",null)
                .show();
    }

    public void downloadSelectedItems(){
        new AlertDialog.Builder(this.getActivity())
                .setIcon(R.drawable.download_dialog)
                .setTitle("Download?...")
                .setMessage("Are you sure you want to download "+remoteSelectedItems.size()+" item(s)")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(current.getActivity().getBaseContext(),remoteSelectedItems.size()+" item(s) added to download queue...",Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getActivity().getBaseContext(), RemoteDownloadService.class);
                        intent.putExtra("MESSENGER",new Messenger(messageHandler));
                        intent.putExtra("IP",ClientActivity.HOST_NAME);
                        intent.putExtra("USERNAME",ClientActivity.USERNAME);
                        intent.putExtra("PASSWORD",ClientActivity.PASSWORD);
                        intent.putExtra("PORT",ClientActivity.PORT);
                        intent.putExtra("ROOT", root);
                        intent.putExtra("SAVE_PATH", ClientActivity.LOCAL_DIRECTORY);
                        intent.putStringArrayListExtra("DOWNLOAD_LIST", remoteSelectedItems);
                        getActivity().startService(intent);

                    }
                })
                .setNegativeButton("No",null)
                .show();
    }

    public void uploadSelectedItems(){
        new AlertDialog.Builder(this.getActivity())
                .setIcon(R.drawable.upload_dialog)
                .setTitle("Upload?...")
                .setMessage("Are you sure you want to upload "+localSelectedItems.size()+" item(s)")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Toast.makeText(current.getActivity().getBaseContext(),localSelectedItems.size()+" item(s) added to upload queue...",Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getActivity().getBaseContext(), RemoteUploadService.class);
                        intent.putExtra("MESSENGER",new Messenger(messageHandlerUpload));
                        intent.putExtra("IP",ClientActivity.HOST_NAME);
                        intent.putExtra("USERNAME",ClientActivity.USERNAME);
                        intent.putExtra("PASSWORD",ClientActivity.PASSWORD);
                        intent.putExtra("PORT",ClientActivity.PORT);
                        intent.putExtra("LOCAL_DIR", root);
                        intent.putExtra("REMOTE_SAVE_PATH",ClientActivity.REMOTE_DIRECTORY);
                        intent.putStringArrayListExtra("UPLOAD_LIST", localSelectedItems);
                        getActivity().startService(intent);

                    }
                })
                .setNegativeButton("No",null)
                .show();
    }

    public void createNewFolderRemote(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(current.getActivity());
        builder.setTitle("New Folder Name...");
        final EditText input = new EditText(current.getActivity());
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RemoteUtilities task = new RemoteUtilities();
                task.setMode("CREATE_FOLDER", current, adapter);
                task.setConnectionParameters(ClientActivity.HOST_NAME,ClientActivity.PORT,ClientActivity.USERNAME,ClientActivity.PASSWORD);
                task.setFolderCreationParameters(root, input.getText().toString());
                task.execute();
            }
        });
        builder.setNegativeButton("Cancel", null);
        final AlertDialog dialog = builder.show();
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    public void renameSelectedItemRemote(){
        final AlertDialog.Builder builder = new AlertDialog.Builder(current.getActivity());
        builder.setTitle("New Name...");
        final EditText input = new EditText(current.getActivity());
        builder.setView(input);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RemoteUtilities task = new RemoteUtilities();
                task.setMode("RENAME", current, adapter);
                task.setConnectionParameters(ClientActivity.HOST_NAME,ClientActivity.PORT,ClientActivity.USERNAME,ClientActivity.PASSWORD);
                task.setRenameParameters(root, remoteSelectedItems.get(0), input.getText().toString());
                task.execute();
            }
        });
        builder.setNegativeButton("Cancel", null);
        final AlertDialog dialog = builder.show();
        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    public void movePasteSelectedItemsRemote(){
        new AlertDialog.Builder(this.getActivity())
                .setTitle("Move?...")
                .setMessage("Are you sure you want to move "+remoteSelectedItemsMove.size()+" item(s) to "+remotePasteLocation)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        RemoteUtilities task = new RemoteUtilities();
                        task.setMode("MOVE", current, adapter);
                        task.setConnectionParameters(ClientActivity.HOST_NAME,ClientActivity.PORT,ClientActivity.USERNAME,ClientActivity.PASSWORD);
                        task.setMoveParameters(remoteMovedFromLocation, remoteSelectedItemsMove, remotePasteLocation);
                        task.execute();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        remoteGlobalMoveFlag = false;
                        remoteSelectedItemsMove.clear();
                        updateLocalActionBar();
                    }
                })
                .show();
    }

    public Handler messageHandler = new MessageHandler();
    public static class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message message) {
            int state= message.arg1;
            switch (state){
                case 1:
                    ClientActivity.MyPagerAdapter.changedFragment=ClientActivity.MyPagerAdapter.remoteFragment;
                    Toast.makeText(ClientActivity.MyPagerAdapter.remoteFragment.getActivity().getBaseContext(),"All items have finished downloading!",Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    ClientActivity.MyPagerAdapter.changedFragment=ClientActivity.MyPagerAdapter.localFragment;
                    adapter.notifyDataSetChanged();
                    break;
                case 0:
                    ClientActivity.MyPagerAdapter.changedFragment=ClientActivity.MyPagerAdapter.remoteFragment;
                    Toast.makeText(ClientActivity.MyPagerAdapter.remoteFragment.getActivity().getBaseContext(),"Some items could not be downloaded!",Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    ClientActivity.MyPagerAdapter.changedFragment=ClientActivity.MyPagerAdapter.localFragment;
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    }

    public Handler messageHandlerUpload = new MessageHandlerUpload();
    public static class MessageHandlerUpload extends Handler {
        @Override
        public void handleMessage(Message message) {
            int state= message.arg1;
            switch (state){
                case 1:
                    ClientActivity.MyPagerAdapter.changedFragment=ClientActivity.MyPagerAdapter.remoteFragment;
                    adapter.notifyDataSetChanged();
                    Toast.makeText(ClientActivity.MyPagerAdapter.remoteFragment.getActivity().getBaseContext(),"All items have finished uploading!",Toast.LENGTH_SHORT).show();
                    ClientActivity.MyPagerAdapter.changedFragment=ClientActivity.MyPagerAdapter.localFragment;
                    adapter.notifyDataSetChanged();
                    break;
                case 0:
                    ClientActivity.MyPagerAdapter.changedFragment=ClientActivity.MyPagerAdapter.remoteFragment;
                    adapter.notifyDataSetChanged();
                    Toast.makeText(ClientActivity.MyPagerAdapter.remoteFragment.getActivity().getBaseContext(),"Some items could not be uploaded!",Toast.LENGTH_SHORT).show();
                    ClientActivity.MyPagerAdapter.changedFragment=ClientActivity.MyPagerAdapter.localFragment;
                    adapter.notifyDataSetChanged();
                    break;
                case 2:
                    ClientActivity.MyPagerAdapter.changedFragment=ClientActivity.MyPagerAdapter.remoteFragment;
                    adapter.notifyDataSetChanged();
                    Toast.makeText(ClientActivity.MyPagerAdapter.remoteFragment.getActivity().getBaseContext(),"Some files skipped since they already existed on the server!",Toast.LENGTH_SHORT).show();
                    ClientActivity.MyPagerAdapter.changedFragment=ClientActivity.MyPagerAdapter.localFragment;
                    adapter.notifyDataSetChanged();
                    break;
            }
        }
    }
}
