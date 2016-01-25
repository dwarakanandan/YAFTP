package net.ddns.dwaraka.yaftp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    public static Toolbar myToolbar;
    public int MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE =0;
    public GridView gridView;
    ArrayList<String> serverList = new ArrayList<>();
    FtpDatabaseHelper helper = new FtpDatabaseHelper(this);

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_action_bar,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.add_server){
            Intent intent = new Intent(this,AddServerActivity.class);
            startActivity(intent);
            return  true;
        }
        if(item.getItemId() == R.id.about){
            Dialog dialog = new Dialog(this);
            dialog.setTitle("About...");
            dialog.setContentView(R.layout.about_dialog);
            dialog.show();
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        myToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(myToolbar);
        getSupportActionBar().setIcon(R.mipmap.ic_launcher);
        getPermissions();
        gridView = (GridView) findViewById(R.id.gridView);
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, ClientActivity.class);
                setExtras(intent, serverList.get(position));
                startActivity(intent);
            }
        });
        gridView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Options...")
                        .setItems(new String[]{"Details", "Edit", "Delete", "Close"}, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                final String clickedServer = serverList.get(position);
                                switch (which) {
                                    case 0:
                                        Cursor cursor = helper.getServerDetails(clickedServer);
                                        cursor.moveToFirst();
                                        String hostName = "HOST_NAME = " + cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_HOST_NAME));
                                        String port = "PORT = " + cursor.getInt(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_PORT));
                                        String username = "USERNAME = " + cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_USERNAME));
                                        String localDir = "LOCAL_DIR = " + cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_LOCAL_DIR));
                                        String remoteDir = "REMOTE_DIR = " + cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_REMOTE_DIR));
                                        AlertDialog.Builder builder1 = new AlertDialog.Builder(MainActivity.this);
                                        builder1.setTitle(clickedServer)
                                                .setMessage(hostName + "\n\n" + port + "\n\n" + username + "\n\n" + localDir + "\n\n" + remoteDir)
                                                .setPositiveButton("OK", null)
                                                .create().show();
                                        break;
                                    case 1:
                                        Intent intent = new Intent(MainActivity.this, EditServerActivity.class);
                                        intent.putExtra("SERVER_NAME", clickedServer);
                                        startActivity(intent);
                                        break;
                                    case 2:
                                        AlertDialog.Builder builder2 = new AlertDialog.Builder(MainActivity.this);
                                        builder2.setTitle("Delete?...")
                                                .setIcon(R.drawable.alert)
                                                .setMessage("Are you sure you want to delete " + clickedServer + " ?")
                                                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        helper.deleteServer(clickedServer);
                                                        serverList = helper.getAllServers();
                                                        if (serverList.size() == 0) {
                                                            LinearLayout linearLayout1 = (LinearLayout) findViewById(R.id.noitemsLinearLayout);
                                                            LinearLayout linearLayout2 = (LinearLayout) findViewById(R.id.placeholderLinearLayout);
                                                            if (serverList.size() == 0) {
                                                                linearLayout1.setVisibility(View.VISIBLE);
                                                                linearLayout2.setVisibility(View.GONE);
                                                            } else {
                                                                linearLayout1.setVisibility(View.GONE);
                                                                linearLayout2.setVisibility(View.VISIBLE);
                                                            }
                                                        }
                                                        gridView.setAdapter(new CustomGridAdapter(MainActivity.this, serverList));
                                                    }
                                                })
                                                .setNegativeButton("No", null).create().show();
                                        break;
                                }
                            }
                        });
                builder.create().show();
                return true;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        serverList = helper.getAllServers();
        LinearLayout linearLayout1 = (LinearLayout) findViewById(R.id.noitemsLinearLayout);
        LinearLayout linearLayout2 = (LinearLayout) findViewById(R.id.placeholderLinearLayout);
        if(serverList.size()==0) {
            linearLayout1.setVisibility(View.VISIBLE);
            linearLayout2.setVisibility(View.GONE);
        }
        else {
            //((ViewManager)linearLayout1.getParent()).removeView(linearLayout1);
            linearLayout1.setVisibility(View.GONE);
            linearLayout2.setVisibility(View.VISIBLE);
        }
        for(String i:serverList)
        gridView.setAdapter(new CustomGridAdapter(this, serverList));
    }

    public void setExtras(Intent intent,String serverName){
        Cursor cursor = helper.getServerDetails(serverName);
        cursor.moveToFirst();
        intent.putExtra("SERVER_NAME", cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_SERVER_NAME)));
        intent.putExtra("HOST_NAME", cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_HOST_NAME)));
        intent.putExtra("PORT",cursor.getInt(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_PORT)));
        intent.putExtra("USERNAME",cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_USERNAME)));
        intent.putExtra("PASSWORD",cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_PASSWORD)));
        intent.putExtra("LOCAL_DIRECTORY", cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_LOCAL_DIR)));
        intent.putExtra("REMOTE_DIRECTORY", cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_REMOTE_DIR)));
    }

    public void getPermissions(){
        if ( ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                Log.e("dwaraka", "Permission request initaited");
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            }
        }
    }

}
