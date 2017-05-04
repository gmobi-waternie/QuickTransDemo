package com.gmobi.quicktrans.client;

import android.content.Context;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.drive.DriveScopes;

/**
 * Created by water on 2017/5/3.
 */

public class GoogleDriveClientFactory {
    private static GoogleAccountCredential mCredential;

    public static GoogleAccountCredential getCredential(Context context) {
        if (mCredential == null)
            mCredential = GoogleAccountCredential.usingOAuth2(
                    context, DriveScopes.all())
                    .setBackOff(new ExponentialBackOff());
        return mCredential;
    }


}
