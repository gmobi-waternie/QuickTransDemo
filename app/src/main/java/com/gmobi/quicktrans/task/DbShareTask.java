package com.gmobi.quicktrans.task;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

import com.dropbox.core.DbxException;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.sharing.ListSharedLinksResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Task to download a file from Dropbox and put it in the Downloads folder
 */
public class DbShareTask extends AsyncTask<String, Void, String> {

    private final Context mContext;
    private final DbxClientV2 mDbxClient;
    private final Callback mCallback;
    private Exception mException;

    public interface Callback {
        void onComplete(String url);
        void onError(Exception e);
    }

    public DbShareTask(Context context, DbxClientV2 dbxClient, Callback callback) {
        mContext = context;
        mDbxClient = dbxClient;
        mCallback = callback;
    }

    @Override
    public void onPostExecute(String v) {
        super.onPostExecute(v);
        if (mException != null) {
            mCallback.onError(mException);
        } else {
            mCallback.onComplete(v);
        }
    }

    @Override
    public String doInBackground(String... params) {
        try {
            ListSharedLinksResult result = mDbxClient.sharing().listSharedLinksBuilder()
                    .withPath(params[0]).withDirectOnly(true).start();

            return result.getLinks().get(0).getUrl();
        } catch (DbxException e) {
            e.printStackTrace();


        }

        return null;
    }
}
