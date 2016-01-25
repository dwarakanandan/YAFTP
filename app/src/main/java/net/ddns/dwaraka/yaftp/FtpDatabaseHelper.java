package net.ddns.dwaraka.yaftp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

public class FtpDatabaseHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME = "ftp_server";
    public static final String COLUMN_SERVER_NAME = "server_name";
    public static final String COLUMN_HOST_NAME = "host_name";
    public static final String COLUMN_PORT = "port";
    public static final String COLUMN_USERNAME = "username";
    public static final String COLUMN_PASSWORD = "password";
    public static final String COLUMN_LOCAL_DIR = "local_directory";
    public static final String COLUMN_REMOTE_DIR = "remote_directory";

    public FtpDatabaseHelper(Context context) {
        super(context, TABLE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String query ="CREATE TABLE "+TABLE_NAME+" ("
                +COLUMN_SERVER_NAME+" text primary key,"
                +COLUMN_HOST_NAME+" text,"
                +COLUMN_PORT+" integer,"
                +COLUMN_USERNAME+" text,"
                +COLUMN_PASSWORD+" text,"
                +COLUMN_LOCAL_DIR+" text,"
                +COLUMN_REMOTE_DIR+" text)";
        Log.d("dwaraka", "Create table: " + query);
        db.execSQL(query);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public long insertServer(String SERVER_NAME,String HOST_NAME,int PORT,String USERNAME,String PASSWORD,String LOCAL_DIR,String REMOTE_DIR){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_SERVER_NAME,SERVER_NAME);
        contentValues.put(COLUMN_HOST_NAME,HOST_NAME);
        contentValues.put(COLUMN_PORT,PORT);
        contentValues.put(COLUMN_USERNAME,USERNAME);
        contentValues.put(COLUMN_PASSWORD,PASSWORD);
        contentValues.put(COLUMN_LOCAL_DIR,LOCAL_DIR);
        contentValues.put(COLUMN_REMOTE_DIR, REMOTE_DIR);
        long l= db.insert(TABLE_NAME,null,contentValues);
        Log.d("dwaraka", "added row at position " + l);
        return l;
    }

    public Cursor getServerDetails(String SERVER_NAME){
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from " + TABLE_NAME + " where " + COLUMN_SERVER_NAME + " = \"" + SERVER_NAME+"\"", null);
        return res;
    }

    public int numberOfRows(){
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, TABLE_NAME);
        return numRows;
    }

    public boolean updateServer(String SERVER_NAME,String HOST_NAME,int PORT,String USERNAME,String PASSWORD,String LOCAL_DIR,String REMOTE_DIR){
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_HOST_NAME,HOST_NAME);
        contentValues.put(COLUMN_PORT,PORT);
        contentValues.put(COLUMN_USERNAME,USERNAME);
        contentValues.put(COLUMN_PASSWORD, PASSWORD);
        contentValues.put(COLUMN_LOCAL_DIR, LOCAL_DIR);
        contentValues.put(COLUMN_REMOTE_DIR, REMOTE_DIR);
        long l= db.update(TABLE_NAME, contentValues, COLUMN_SERVER_NAME + " = ?", new String[]{SERVER_NAME});
        Log.d("dwaraka","updated row at position "+l);
        return true;
    }

    public void deleteServer(String SERVER_NAME){
        SQLiteDatabase db = this.getWritableDatabase();
        int delete=db.delete(TABLE_NAME, COLUMN_SERVER_NAME + " = ?", new String[]{SERVER_NAME});
        Log.d("dwaraka","deleted code returned = "+delete);
    }

    public ArrayList<String> getAllServers()
    {
        ArrayList<String> array_list = new ArrayList();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery("select * from "+TABLE_NAME, null);
        res.moveToFirst();
        while(!res.isAfterLast()){
            array_list.add(res.getString(res.getColumnIndex(COLUMN_SERVER_NAME)));
            res.moveToNext();
        }
        return array_list;
    }
}