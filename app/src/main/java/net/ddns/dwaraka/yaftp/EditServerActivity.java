package net.ddns.dwaraka.yaftp;

import android.database.Cursor;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

public class EditServerActivity extends AppCompatActivity {
    public static Toolbar myToolbar;
    public TextView textViewServerName;
    public EditText editTextHostName,editTextPort,editTextUsername,editTextPassword,editTextRemoteDir;
    public String hostName,username,password,localDir,remoteDir;
    int port;
    public String clickedServerName;


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.create_server_action_bar,menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home){
            this.finish();
            return true;
        }
        if(item.getItemId() == R.id.saveServer){
            saveServerHandler();
            return true;
        }
        if(item.getItemId() == R.id.testConnection){
            testConnectionHandler();
            return true;
        }
        return false;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_server);
        clickedServerName = getIntent().getExtras().getString("SERVER_NAME");
        textViewServerName = (TextView) findViewById(R.id.textViewServerName);
        textViewServerName.setText("Server name : "+clickedServerName);
        editTextHostName = (EditText)findViewById(R.id.editTextHostName);
        editTextPort = (EditText)findViewById(R.id.editTextPort);
        editTextUsername = (EditText)findViewById(R.id.editTextUsername);
        editTextPassword = (EditText)findViewById(R.id.editTextPassword);
        editTextRemoteDir = (EditText)findViewById(R.id.editTextRemoteDir);
        setDetails();
        myToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        myToolbar.setTitle("Edit Server");
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        RadioButton radioButton1 = (RadioButton) findViewById(R.id.radioButtonInternal);
        RadioButton radioButton2 = (RadioButton) findViewById(R.id.radioButtonExternal);
        radioButton1.setText("Primary External Storage ("+ Environment.getExternalStorageDirectory().getAbsolutePath()+")");
        localDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        if(System.getenv("SECONDARY_STORAGE") != null)
            radioButton2.setText("External SD Card ("+System.getenv("SECONDARY_STORAGE")+")");
        else
            radioButton2.setVisibility(View.GONE);
    }

    public void setDetails(){
        FtpDatabaseHelper helper = new FtpDatabaseHelper(this);
        Cursor cursor = helper.getServerDetails(clickedServerName);
        cursor.moveToFirst();
        String hostName =cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_HOST_NAME));
        int port =cursor.getInt(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_PORT));
        String username = cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_USERNAME));
        String password =cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_PASSWORD));
        String remoteDir = cursor.getString(cursor.getColumnIndex(FtpDatabaseHelper.COLUMN_REMOTE_DIR));
        editTextHostName.setText(hostName);
        editTextPort.setText(Integer.toString(port));
        editTextUsername.setText(username);
        editTextPassword.setText(password);
        editTextRemoteDir.setText(remoteDir);
    }

    public void onRadioButtonClicked(View view) {
        boolean checked = ((RadioButton) view).isChecked();
        switch(view.getId()) {
            case R.id.radioButtonInternal:
                if (checked)
                    localDir = Environment.getExternalStorageDirectory().getAbsolutePath();
                break;
            case R.id.radioButtonExternal:
                if (checked)
                    localDir = System.getenv("SECONDARY_STORAGE");
                break;
        }
    }

    public void saveServerHandler(){
        hostName = editTextHostName.getText().toString().trim();
        if(editTextPort.getText().toString().trim().isEmpty())
            port = 21;
        else
            port =Integer.parseInt(editTextPort.getText().toString().trim());
        username = editTextUsername.getText().toString().trim();
        password = editTextPassword.getText().toString().trim();
        remoteDir = editTextRemoteDir.getText().toString().trim();
        TestConnection task = new TestConnection();
        task.setConnectionParameters(clickedServerName, hostName, port, username, password, localDir, remoteDir, this, true);
        task.execute();
    }

    public void addToDatabase(){
        FtpDatabaseHelper helper = new FtpDatabaseHelper(this);
        if(remoteDir.isEmpty())
            remoteDir=TestConnection.REMOTE_DIR_STATIC;
        helper.updateServer(clickedServerName, hostName, port, username, password, localDir, remoteDir);
        finish();
    }

    public void testConnectionHandler(){
        String hostName = editTextHostName.getText().toString().trim();
        int port;
        if(editTextPort.getText().toString().trim().isEmpty())
            port = 21;
        else
            port =Integer.parseInt(editTextPort.getText().toString().trim());
        String username = editTextUsername.getText().toString().trim();
        String password = editTextPassword.getText().toString().trim();
        String remoteDir = editTextRemoteDir.getText().toString().trim();
        TestConnection task = new TestConnection();
        task.setConnectionParameters(clickedServerName, hostName, port, username, password, localDir, remoteDir, this, false);
        task.execute();
    }
}
