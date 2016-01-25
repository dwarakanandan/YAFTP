package net.ddns.dwaraka.yaftp;

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
import android.widget.Toast;

public class AddServerActivity extends AppCompatActivity {
    public static Toolbar myToolbar;
    public EditText editTextServerName,editTextHostName,editTextPort,editTextUsername,editTextPassword,editTextRemoteDir;
    public String serverName,hostName,username,password,localDir,remoteDir;
    int port;


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
        setContentView(R.layout.activity_add_server);
        editTextServerName = (EditText)findViewById(R.id.editTextServerName);
        editTextHostName = (EditText)findViewById(R.id.editTextHostName);
        editTextPort = (EditText)findViewById(R.id.editTextPort);
        editTextUsername = (EditText)findViewById(R.id.editTextUsername);
        editTextPassword = (EditText)findViewById(R.id.editTextPassword);
        editTextRemoteDir = (EditText)findViewById(R.id.editTextRemoteDir);
        myToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        myToolbar.setTitle("Add new Server");
        setSupportActionBar(myToolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);
        RadioButton radioButton1 = (RadioButton) findViewById(R.id.radioButtonInternal);
        RadioButton radioButton2 = (RadioButton) findViewById(R.id.radioButtonExternal);
        radioButton1.setText("Primary External Storage ("+Environment.getExternalStorageDirectory().getAbsolutePath()+")");
        localDir = Environment.getExternalStorageDirectory().getAbsolutePath();
        if(System.getenv("SECONDARY_STORAGE") != null)
            radioButton2.setText("External SD Card ("+System.getenv("SECONDARY_STORAGE")+")");
        else
            radioButton2.setVisibility(View.GONE);
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
        serverName = editTextServerName.getText().toString().trim();
        if(serverName.isEmpty()) {
            Toast.makeText(this, "Please enter a Server name", Toast.LENGTH_SHORT).show();
            return;
        }
        FtpDatabaseHelper helper = new FtpDatabaseHelper(this);
        if(helper.getAllServers().contains(serverName)){
            Toast.makeText(this, "Server name already exists!", Toast.LENGTH_SHORT).show();
            return;
        }
        hostName = editTextHostName.getText().toString().trim();
        if(editTextPort.getText().toString().trim().isEmpty())
            port = 21;
        else
            port =Integer.parseInt(editTextPort.getText().toString().trim());
        username = editTextUsername.getText().toString().trim();
        password = editTextPassword.getText().toString().trim();
        remoteDir = editTextRemoteDir.getText().toString().trim();
        TestConnection task = new TestConnection();
        task.setConnectionParameters(serverName, hostName, port, username, password, localDir, remoteDir, this, true);
        task.execute();
    }

    public void addToDatabase(){
        FtpDatabaseHelper helper = new FtpDatabaseHelper(this);
        if(remoteDir.isEmpty())
            remoteDir=TestConnection.REMOTE_DIR_STATIC;
        helper.insertServer(serverName, hostName, port, username, password, localDir, remoteDir);
        finish();
    }

    public void testConnectionHandler(){
        String serverName = editTextServerName.getText().toString().trim();
        if(serverName.isEmpty()) {
            Toast.makeText(this, "Please enter a Server name", Toast.LENGTH_SHORT).show();
            return;
        }
        FtpDatabaseHelper helper = new FtpDatabaseHelper(this);
        if(helper.getAllServers().contains(serverName)){
            Toast.makeText(this, "Server name already exists!", Toast.LENGTH_SHORT).show();
            return;
        }
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
        task.setConnectionParameters(serverName, hostName, port, username, password, localDir, remoteDir, this, false);
        task.execute();
    }
}
