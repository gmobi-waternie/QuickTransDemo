package com.gmobi.quicktrans.task;

import android.os.AsyncTask;
import android.os.Environment;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.List;

/**
 * Created by water on 2017/5/3.
 */

public class GdUploadTask extends AsyncTask<String, Void, String> {
    private com.google.api.services.drive.Drive mService = null;
    private Exception mLastError = null;
    private GdUpdateCallback mCallback;

    public interface GdUpdateCallback {
        void onPreExecute();
        void onCancelled(Exception e);
        void onPostExecute(String output);
    }

    public GdUploadTask(GoogleAccountCredential credential,GdUpdateCallback callback) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("Drive API Android Quickstart")
                .build();
        mCallback = callback;

    }


    @Override
    protected String doInBackground(String... params) {
        try {
            return UploadFiles(params[0]);
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return null;
        }
    }

    private String UploadFiles(String path) throws IOException {


        File f = insertFile(mService, "test.jpg", "my test", "video/mp4", path);
        return f.getId();
    }


    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onPreExecute();
    }

    @Override
    protected void onPostExecute(String output) {
        if(mCallback != null)
            mCallback.onPostExecute(output);
    }

    @Override
    protected void onCancelled() {
        if(mCallback != null)
            mCallback.onCancelled(mLastError);
    }

    private  File insertFile(Drive service, String title, String description,
                                   String mimeType, String filename) {
        // File's metadata.
        File body = new File();
        body.setName(title);
        body.setDescription(description);
        body.setMimeType(mimeType);


        java.io.File fileContent = new java.io.File(filename);
        FileContent mediaContent = new FileContent(mimeType, fileContent);
        try {
            return service.files().create(body, mediaContent).execute();
        } catch (IOException e) {
            System.out.println("An error occurred: " + e);
            return null;
        }
    }
}

