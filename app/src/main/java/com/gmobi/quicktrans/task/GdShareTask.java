package com.gmobi.quicktrans.task;

import android.os.AsyncTask;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.model.Permission;

import java.io.IOException;

/**
 * Created by water on 2017/5/3.
 */
public class GdShareTask extends AsyncTask<String, Void, Void> {
    private com.google.api.services.drive.Drive mService = null;
    private Exception mLastError = null;
    private GdShareCallback mCallback;
    private static final String SHARE_URL_TEMPLATE = "https://drive.google.com/uc?export=download&id={fileid}";

    public interface GdShareCallback {
        void onPreExecute();

        void onCancelled(Exception e);

        void onPostExecute();

        void onPermissionSuccess(String url);
    }


    public GdShareTask(GoogleAccountCredential credential, GdShareCallback callback) {
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
            shareFile(params[0]);
        } catch (Exception e) {
            mLastError = e;
            cancel(true);
        }
        return null;
    }


    private void shareFile(final String fileId) throws IOException {
        JsonBatchCallback<Permission> callback = new JsonBatchCallback<Permission>() {
            @Override
            public void onFailure(GoogleJsonError e,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                System.err.println(e.getMessage());
            }

            @Override
            public void onSuccess(Permission permission,
                                  HttpHeaders responseHeaders)
                    throws IOException {
                System.out.println("Permission ID: " + permission.getId());

                if(mCallback != null)
                    mCallback.onPermissionSuccess(SHARE_URL_TEMPLATE.replace("{fileid}", fileId));

            }
        };
        BatchRequest batch = mService.batch();
        Permission userPermission = new Permission()
                .setType("anyone")
                .setRole("reader");
        mService.permissions().create(fileId, userPermission)
                .setFields("id")
                .queue(batch, callback);

        batch.execute();
    }

    @Override
    protected void onPreExecute() {
        if(mCallback != null)
            mCallback.onPreExecute();
    }

    @Override
    protected void onPostExecute(Void output) {
        if(mCallback != null)
            mCallback.onPostExecute();

    }

    @Override
    protected void onCancelled() {
        if(mCallback != null)
            mCallback.onCancelled(mLastError);
    }
}



