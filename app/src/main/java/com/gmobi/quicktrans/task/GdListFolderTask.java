package com.gmobi.quicktrans.task;

import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;


import com.gmobi.quicktrans.activity.MainActivity;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by water on 2017/5/3.
 */

public class GdListFolderTask extends AsyncTask<Void, Void, List<String>> {
    private com.google.api.services.drive.Drive mService = null;
    private Exception mLastError = null;
    private static final String TAG = "GdListFolderTask";
    private GdListCallback mCallback;

    public interface GdListCallback {
        void onPreExecute();
        void onCancelled(Exception e);
        void onPostExecute(List<String> output);
    }

    public GdListFolderTask(GoogleAccountCredential credential,GdListCallback callback) {
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        mService = new com.google.api.services.drive.Drive.Builder(
                transport, jsonFactory, credential)
                .setApplicationName("QuickTransDemo")
                .build();
        mCallback =callback;
    }





    @Override
    protected List<String> doInBackground(Void... params) {
        try {
            return getDataFromApi();
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
            return null;
        }
    }

    private List<String> getDataFromApi() throws IOException {
        // Get a list of up to 10 files.
        List<String> fileInfo = new ArrayList<String>();
        FileList result = mService.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<File> files = result.getFiles();
        if (files != null) {
            for (File file : files) {
                fileInfo.add(String.format("%s (%s)\n",
                        file.getName(), file.getId()));
                Log.e(TAG,file.getName());
                Log.e(TAG,file.getId());
            }
        }
        return fileInfo;
    }


    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onPreExecute();


    }

    @Override
    protected void onPostExecute(List<String> output) {
        if(mCallback != null)
            mCallback.onPostExecute(output);

    }

    @Override
    protected void onCancelled() {
        if(mCallback != null)
            mCallback.onCancelled(mLastError);

    }
}
