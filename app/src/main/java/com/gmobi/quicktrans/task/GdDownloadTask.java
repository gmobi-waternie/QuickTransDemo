package com.gmobi.quicktrans.task;

import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by water on 2017/5/3.
 */

public class GdDownloadTask extends AsyncTask<String, Void, Void> {
    private com.google.api.services.drive.Drive mService = null;
    private Exception mLastError = null;
    private GdDownloadCallback mCallback;

    public interface GdDownloadCallback {
        void onPreExecute();
        void onCancelled(Exception e);
        void onPostExecute();
        void onDownloadComplete(String localPath);
    }


    public GdDownloadTask(GoogleAccountCredential credential,GdDownloadCallback callback) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Drive API Android Quickstart")
                .build();

        mCallback = callback;
    }

    @Override
    protected Void doInBackground(String... params) {
        try {
            getFileFromApi(params[0], params[1]);
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
        }
        return null;
    }


    private Void getFileFromApi(String id, String path) throws IOException {
        OutputStream outputStream = new FileOutputStream(new java.io.File(path));
        mService.files().get(id)
                .executeMediaAndDownloadTo(outputStream);


        if(mCallback!=null)
            mCallback.onDownloadComplete(path);

        return null;

    }


    @Override
    protected void onPreExecute() {
        if(mCallback!=null)
            mCallback.onPreExecute();

    }

    @Override
    protected void onPostExecute(Void output) {
        if(mCallback!=null)
            mCallback.onPostExecute();

    }

    @Override
    protected void onCancelled() {
        if(mCallback!=null)
            mCallback.onCancelled(mLastError);
    }
}
