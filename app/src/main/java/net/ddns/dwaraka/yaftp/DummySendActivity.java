package net.ddns.dwaraka.yaftp;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

public class DummySendActivity extends Activity {
    public ListView listView;
    public ArrayList<String> uploadList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pick_server);
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();
        if (Intent.ACTION_SEND.equals(action) && type != null) {
            Uri uri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if(uri == null){
                Toast.makeText(this,"YAFTP could not handle the share..Please choose a different appliaction",Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            String path = new File(uri.getPath()).getPath();
            Log.d("dwaraka", "Got path:" + path);
            uploadList.add(path);
        }
        else if(Intent.ACTION_SEND_MULTIPLE.equals(action) && type != null) {
            ArrayList<Uri> uris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            for(Uri i:uris){
                if(i == null){
                    Toast.makeText(this,"YAFTP could not handle the share..Please choose a different appliaction",Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                String path = new File(i.getPath()).getPath();
                Log.d("dwaraka","Got path:"+path);
                uploadList.add(path);
            }
        }
        final ArrayList<String> list = new FtpDatabaseHelper(this).getAllServers();
        ArrayAdapter adapter = new ArrayAdapter(this,R.layout.pick_server_listitem,list);
        listView = (ListView) findViewById(R.id.listViewPickServer);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor cursor = new FtpDatabaseHelper(DummySendActivity.this).getServerDetails(list.get(position));
                cursor.moveToFirst();
                Intent intent = new Intent(DummySendActivity.this, UploadServiceHandler.class);
                intent.putExtra("IP",cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_HOST_NAME)));
                intent.putExtra("USERNAME",cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_USERNAME)));
                intent.putExtra("PASSWORD",cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_PASSWORD)));
                intent.putExtra("PORT",cursor.getInt(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_PORT)));
                intent.putExtra("REMOTE_SAVE_PATH",cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_REMOTE_DIR)));
                intent.putStringArrayListExtra("UPLOAD_LIST",uploadList);
                startService(intent);
                finish();
            }
        });
    }
}
