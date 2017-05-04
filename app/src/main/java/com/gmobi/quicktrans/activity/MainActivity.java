package com.gmobi.quicktrans.activity;


import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.core.android.Auth;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.ListFolderResult;
import com.dropbox.core.v2.files.Metadata;
import com.dropbox.core.v2.users.FullAccount;
import com.gmobi.quicktrans.R;

import com.gmobi.quicktrans.client.DropboxClientFactory;

import com.gmobi.quicktrans.client.GoogleDriveClientFactory;
import com.gmobi.quicktrans.task.DbDownloadTask;
import com.gmobi.quicktrans.task.DbGetCurrentAccountTask;
import com.gmobi.quicktrans.task.DbListFolderTask;
import com.gmobi.quicktrans.task.DbShareTask;
import com.gmobi.quicktrans.task.DbUploadTask;
import com.gmobi.quicktrans.task.GdDownloadTask;
import com.gmobi.quicktrans.task.GdListFolderTask;
import com.gmobi.quicktrans.task.GdShareTask;
import com.gmobi.quicktrans.task.GdUploadTask;
import com.gmobi.quicktrans.util.ShareUtil;
import com.gmobi.quicktrans.util.UriHelpers;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.batch.BatchRequest;
import com.google.api.client.googleapis.batch.json.JsonBatchCallback;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GooglePlayServicesAvailabilityIOException;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.googleapis.json.GoogleJsonError;
import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.Permission;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;


public class MainActivity extends Activity implements EasyPermissions.PermissionCallbacks {
    GoogleAccountCredential mCredential;
    private TextView mOutputText;
    private ImageView mDownloadImage;
    private Button mCallApiButton;
    private Button mUploadButton;
    private Button mDownloadButton;
    private Button mShareImageButton;
    private Button mShareVideoButton;
    ProgressDialog mProgress;

    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String BUTTON_TEXT = "Call Drive API";
    private static final String BUTTON_UPLOAD = "Upload Files";
    private static final String BUTTON_DOWNLOAD = "Download Files";
    private static final String BUTTON_SHAREVIDEO = "Share Video";
    private static final String BUTTON_SHAREIMAGE = "Share Image";
    private static final String PREF_ACCOUNT_NAME = "accountName";
    private static final String[] SCOPES = {DriveScopes.DRIVE};


    private static final String TAG = "QuickTransDemo";
    private String dbToken;


    public static final String DRIVER_DROPBOX = "Dropbox";
    public static final String DRIVER_GOOGLE = "GoogleDriver";

    public static String driver = DRIVER_DROPBOX;


    private List<Metadata> dbItems;

    /**
     * Create the main activity.
     *
     * @param savedInstanceState previously saved instance data.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        LinearLayout activityLayout = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        activityLayout.setLayoutParams(lp);
        activityLayout.setOrientation(LinearLayout.VERTICAL);
        activityLayout.setPadding(16, 16, 16, 16);

        ViewGroup.LayoutParams tlp = new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

        mCallApiButton = new Button(this);
        mCallApiButton.setText(BUTTON_TEXT);
        mCallApiButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mCallApiButton.setEnabled(false);
                mOutputText.setText("");
                startAuth();
                mCallApiButton.setEnabled(true);
            }
        });
        activityLayout.addView(mCallApiButton);


        mUploadButton = new Button(this);
        mUploadButton.setText(BUTTON_UPLOAD);
        mUploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mUploadButton.setEnabled(false);
                mOutputText.setText("");
                UploadFileToApi();
                mUploadButton.setEnabled(true);
            }
        });
        activityLayout.addView(mUploadButton);


        mDownloadButton = new Button(this);
        mDownloadButton.setText(BUTTON_DOWNLOAD);
        mDownloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDownloadButton.setEnabled(false);
                mOutputText.setText("");
                DownloadFileToApi("0B2KvMBhBwmXOX1V0Y0hlbGZFMWs", "test_download.jpg");
                mDownloadButton.setEnabled(true);
            }
        });
        activityLayout.addView(mDownloadButton);


        mShareVideoButton = new Button(this);
        mShareVideoButton.setText(BUTTON_SHAREVIDEO);
        mShareVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mShareVideoButton.setEnabled(false);
                mOutputText.setText("");
                ShareFileToApi("0B2KvMBhBwmXOUVlyUWtTWHpZNWs");
                mShareVideoButton.setEnabled(true);
            }
        });
        activityLayout.addView(mShareVideoButton);


        mShareImageButton = new Button(this);
        mShareImageButton.setText(BUTTON_SHAREIMAGE);
        mShareImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mShareImageButton.setEnabled(false);
                mOutputText.setText("");
                ShareFileToApi("0B2KvMBhBwmXOX1V0Y0hlbGZFMWs");
                mShareImageButton.setEnabled(true);
            }
        });
        activityLayout.addView(mShareImageButton);


        mOutputText = new TextView(this);
        mOutputText.setLayoutParams(tlp);
        mOutputText.setPadding(16, 16, 16, 16);
        mOutputText.setVerticalScrollBarEnabled(true);
        mOutputText.setMovementMethod(new ScrollingMovementMethod());
        mOutputText.setText(
                "Click the \'" + BUTTON_TEXT + "\' button to test the API.");
        activityLayout.addView(mOutputText);


        mDownloadImage = new ImageView(this);
        mDownloadImage.setLayoutParams(tlp);
        mDownloadImage.setPadding(16, 16, 16, 16);
        mDownloadImage.setVerticalScrollBarEnabled(true);
        activityLayout.addView(mDownloadImage);

        mProgress = new ProgressDialog(this);
        mProgress.setMessage("Calling Drive API ...");

        setContentView(activityLayout);

        // Initialize credentials and service object.
        mCredential = GoogleDriveClientFactory.getCredential(getApplicationContext());
    }


    /**
     * Attempt to call the API, after verifying that all the preconditions are
     * satisfied. The preconditions are: Google Play Services installed, an
     * account was selected and the device currently has online access. If any
     * of the preconditions are not satisfied, the app will prompt the user as
     * appropriate.
     */
    private void startAuth() {
        if (driver.equals(DRIVER_GOOGLE)) {
            if (!isGooglePlayServicesAvailable()) {
                acquireGooglePlayServices();
            } else if (mCredential.getSelectedAccountName() == null) {
                chooseAccount();
            } else if (!isDeviceOnline()) {
                mOutputText.setText("No network connection available.");
            } else {
                loadGoogleDriveData();
            }
        } else {
            Auth.startOAuth2Authentication(this, getString(R.string.app_key));
        }
    }


    private void UploadFileToApi() {
        if (driver.equals(DRIVER_GOOGLE)) {
            if (!isGooglePlayServicesAvailable()) {
                acquireGooglePlayServices();
            } else if (mCredential.getSelectedAccountName() == null) {
                chooseAccount();
            } else if (!isDeviceOnline()) {
                mOutputText.setText("No network connection available.");
            } else {
                String fileName = "Download/football.mp4";
                String path = Environment.getExternalStorageDirectory() + "/" + fileName;
                gdUplaodFile(path);
            }
        } else {
            String fileName = "Download/test.jpg";
            String path = Environment.getExternalStorageDirectory() + "/" + fileName;
            dbUploadFile(path);
        }
    }

    private void DownloadFileToApi(String id, String name) {
        if (driver.equals(DRIVER_GOOGLE)) {
            if (!isGooglePlayServicesAvailable()) {
                acquireGooglePlayServices();
            } else if (mCredential.getSelectedAccountName() == null) {
                chooseAccount();
            } else if (!isDeviceOnline()) {
                mOutputText.setText("No network connection available.");
            } else {
                String path = Environment.getExternalStorageDirectory() + "/Download/" + name;
                gdDownloadFile(id,path);
            }
        } else {
            dbDownloadFile((FileMetadata) dbItems.get(5));
        }
    }

    private void ShareFileToApi(String id) {
        if (driver.equals(DRIVER_GOOGLE)) {
            if (!isGooglePlayServicesAvailable()) {
                acquireGooglePlayServices();
            } else if (mCredential.getSelectedAccountName() == null) {
                chooseAccount();
            } else if (!isDeviceOnline()) {
                mOutputText.setText("No network connection available.");
            } else {
                gdShareFile(id);
            }
        } else {
            dbShareFile("/test.jpg");
        }
    }


    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount() {
        String[] per = {Manifest.permission.GET_ACCOUNTS, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        if (EasyPermissions.hasPermissions(
                this, Manifest.permission.GET_ACCOUNTS)) {
            String accountName = getPreferences(Context.MODE_PRIVATE)
                    .getString(PREF_ACCOUNT_NAME, null);
            if (accountName != null) {
                mCredential.setSelectedAccountName(accountName);
                startAuth();
            } else {
                // Start a dialog from which the user can choose an account
                startActivityForResult(
                        mCredential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
            }
        } else {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(
                    this,
                    "This app needs to access your Google account (via Contacts).",
                    REQUEST_PERMISSION_GET_ACCOUNTS, per
            );
        }
    }


    @Override
    protected void onActivityResult(
            int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK) {
                    mOutputText.setText(
                            "This app requires Google Play Services. Please install " +
                                    "Google Play Services on your device and relaunch this app.");
                } else {
                    startAuth();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null &&
                        data.getExtras() != null) {
                    String accountName =
                            data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        SharedPreferences settings =
                                getPreferences(Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(PREF_ACCOUNT_NAME, accountName);
                        editor.apply();
                        mCredential.setSelectedAccountName(accountName);
                        startAuth();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK) {
                    startAuth();
                }
                break;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(
                requestCode, permissions, grantResults, this);
    }


    @Override
    public void onPermissionsGranted(int requestCode, List<String> list) {
        // Do nothing.
    }


    @Override
    public void onPermissionsDenied(int requestCode, List<String> list) {
        // Do nothing.
    }



    //==========================================GoogleDrive=============================================


    /**
     * Checks whether the device currently has a network connection.
     *
     * @return true if the device has a network connection, false otherwise.
     */
    private boolean isDeviceOnline() {
        ConnectivityManager connMgr =
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }


    private boolean isGooglePlayServicesAvailable() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }


    private void acquireGooglePlayServices() {
        GoogleApiAvailability apiAvailability =
                GoogleApiAvailability.getInstance();
        final int connectionStatusCode =
                apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode)) {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }



    void showGooglePlayServicesAvailabilityErrorDialog(
            final int connectionStatusCode) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(
                MainActivity.this,
                connectionStatusCode,
                REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }


    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences prefs = getSharedPreferences("dropbox-sample", MODE_PRIVATE);
        String accessToken = prefs.getString("access-token", null);
        if (accessToken == null) {
            accessToken = Auth.getOAuth2Token();
            if (accessToken != null) {
                prefs.edit().putString("access-token", accessToken).apply();
                initAndLoadData(accessToken);
            }
        } else {
            initAndLoadData(accessToken);
        }

        String uid = Auth.getUid();
        String storedUid = prefs.getString("user-id", null);
        if (uid != null && !uid.equals(storedUid)) {
            prefs.edit().putString("user-id", uid).apply();
        }
    }



    private void loadGoogleDriveData() {
        new GdListFolderTask(mCredential, new GdListFolderTask.GdListCallback() {
            @Override
            public void onPreExecute() {
                mOutputText.setText("");
                mProgress.show();
            }

            @Override
            public void onCancelled(Exception e) {
                mProgress.hide();
                if (e != null) {
                    if (e instanceof GooglePlayServicesAvailabilityIOException) {
                        showGooglePlayServicesAvailabilityErrorDialog(
                                ((GooglePlayServicesAvailabilityIOException) e)
                                        .getConnectionStatusCode());
                    } else if (e instanceof UserRecoverableAuthIOException) {
                        startActivityForResult(
                                ((UserRecoverableAuthIOException) e).getIntent(),
                                MainActivity.REQUEST_AUTHORIZATION);
                    } else {
                        mOutputText.setText("The following error occurred:\n"
                                + e.getMessage());
                    }
                } else {
                    mOutputText.setText("Request cancelled.");
                }
            }

            @Override
            public void onPostExecute(List<String> output) {
                mProgress.hide();
                if (output == null || output.size() == 0) {
                    mOutputText.setText("No results returned.");
                } else {
                    output.add(0, "Data retrieved using the Drive API:");
                    mOutputText.setText(TextUtils.join("\n", output));
                }
            }
        }).execute();
    }
    private void gdUplaodFile(String path)
    {
        new GdUploadTask(mCredential, new GdUploadTask.GdUpdateCallback() {
            @Override
            public void onPreExecute() {
                mOutputText.setText("");
                mProgress.show();
            }

            @Override
            public void onCancelled(Exception e) {
                mProgress.hide();
                if (e != null) {
                    if (e instanceof GooglePlayServicesAvailabilityIOException) {
                        showGooglePlayServicesAvailabilityErrorDialog(
                                ((GooglePlayServicesAvailabilityIOException) e)
                                        .getConnectionStatusCode());
                    } else if (e instanceof UserRecoverableAuthIOException) {
                        startActivityForResult(
                                ((UserRecoverableAuthIOException) e).getIntent(),
                                MainActivity.REQUEST_AUTHORIZATION);
                    } else {
                        mOutputText.setText("The following error occurred:\n"
                                + e.getMessage());
                    }
                } else {
                    mOutputText.setText("Request cancelled.");
                }
            }

            @Override
            public void onPostExecute(String output) {
                mProgress.hide();
                if (output == null) {
                    mOutputText.setText("No results returned.");
                } else {
                    mOutputText.setText(output);
                }
            }
        }).execute(path);
    }
    private void gdDownloadFile(String id,String name) {
        new GdDownloadTask(mCredential, new GdDownloadTask.GdDownloadCallback() {
            @Override
            public void onPreExecute() {
                mOutputText.setText("");
                mProgress.show();
            }

            @Override
            public void onCancelled(Exception e) {
                mProgress.hide();
                if (e != null) {
                    if (e instanceof GooglePlayServicesAvailabilityIOException) {
                        showGooglePlayServicesAvailabilityErrorDialog(
                                ((GooglePlayServicesAvailabilityIOException) e)
                                        .getConnectionStatusCode());
                    } else if (e instanceof UserRecoverableAuthIOException) {
                        startActivityForResult(
                                ((UserRecoverableAuthIOException) e).getIntent(),
                                MainActivity.REQUEST_AUTHORIZATION);
                    } else {
                        mOutputText.setText("The following error occurred:\n"
                                + e.getMessage());
                    }
                } else {
                    mOutputText.setText("Request cancelled.");
                }
            }

            @Override
            public void onPostExecute() {
                mProgress.hide();
            }

            @Override
            public void onDownloadComplete(String localPath) {
                Bitmap bitmap = BitmapFactory.decodeFile(localPath);
                mDownloadImage.setImageBitmap(bitmap);
            }
        }).execute(id, name);

    }

    private void gdShareFile(final String id) {
        new GdShareTask(mCredential, new GdShareTask.GdShareCallback() {
            @Override
            public void onPreExecute() {
                mOutputText.setText("");
                mProgress.show();
            }

            @Override
            public void onCancelled(Exception e) {
                mProgress.hide();
                if (e != null) {
                    if (e instanceof GooglePlayServicesAvailabilityIOException) {
                        showGooglePlayServicesAvailabilityErrorDialog(
                                ((GooglePlayServicesAvailabilityIOException) e)
                                        .getConnectionStatusCode());
                    } else if (e instanceof UserRecoverableAuthIOException) {
                        startActivityForResult(
                                ((UserRecoverableAuthIOException) e).getIntent(),
                                MainActivity.REQUEST_AUTHORIZATION);
                    } else {
                        mOutputText.setText("The following error occurred:\n"
                                + e.getMessage());
                    }
                } else {
                    mOutputText.setText("Request cancelled.");
                }
            }

            @Override
            public void onPostExecute() {
                mProgress.hide();
            }

            @Override
            public void onPermissionSuccess(String url) {
                ShareUtil.shareSend(MainActivity.this,url);
            }
        }).execute(id);
    }


//==========================================Dropbox=============================================




    private void initAndLoadData(String accessToken) {
        DropboxClientFactory.init(accessToken);
        loadDropboxData();
    }

    protected void loadSelfInfo() {
        new DbGetCurrentAccountTask(DropboxClientFactory.getClient(), new DbGetCurrentAccountTask.Callback() {
            @Override
            public void onComplete(FullAccount result) {
                List<String> info = new ArrayList<>();
                info.add(result.getEmail());
                info.add(result.getName().getDisplayName());
                info.add(result.getAccountType().name());


                mOutputText.setText(TextUtils.join("\n", info));
            }

            @Override
            public void onError(Exception e) {
                Log.e(getClass().getName(), "Failed to get account details.", e);
            }
        }).execute();
    }



    protected void loadDropboxData() {


        new DbListFolderTask(DropboxClientFactory.getClient(), new DbListFolderTask.Callback() {
            @Override
            public void onDataLoaded(ListFolderResult result) {
                mProgress.hide();
                List<String> itemList = new ArrayList<String>();
                for (int i = 0; i < result.getEntries().size(); i++) {
                    Metadata item = result.getEntries().get(i);
                    itemList.add(item.getName());
                }
                mOutputText.setText(TextUtils.join("\n", itemList));

                dbItems = result.getEntries();


            }

            @Override
            public void onError(Exception e) {
                mProgress.hide();
                Log.e(TAG, "Failed to list folder.", e);
                Toast.makeText(MainActivity.this,
                        "An error has occurred",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }).execute("");
    }

    private void dbUploadFile(String path) {


        Uri uri = UriHelpers.getFileUri(this, path);
        new DbUploadTask(this, DropboxClientFactory.getClient(), new DbUploadTask.Callback() {
            @Override
            public void onUploadComplete(FileMetadata result) {
                mProgress.hide();

                String message = result.getName() + " size " + result.getSize() + " modified " +
                        DateFormat.getDateTimeInstance().format(result.getClientModified());
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT)
                        .show();
                loadDropboxData();
            }

            @Override
            public void onError(Exception e) {
                mProgress.hide();

                Log.e(TAG, "Failed to upload file.", e);
                Toast.makeText(MainActivity.this,
                        "An error has occurred",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }).execute(path, "");
    }



    private void dbDownloadFile(FileMetadata file) {
        new DbDownloadTask(this, DropboxClientFactory.getClient(), new DbDownloadTask.Callback() {
            @Override
            public void onDownloadComplete(java.io.File result) {
                mProgress.hide();

                if (result != null) {
                    Bitmap bmp = BitmapFactory.decodeFile(result.getAbsolutePath());
                    mDownloadImage.setImageBitmap(bmp);
                }
            }

            @Override
            public void onError(Exception e) {
                mProgress.hide();

                Log.e(TAG, "Failed to download file.", e);
                Toast.makeText(MainActivity.this,
                        "An error has occurred",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }).execute(file);

    }


    private void dbShareFile(String remoteFileName) {
        new DbShareTask(this, DropboxClientFactory.getClient(), new DbShareTask.Callback() {
            @Override
            public void onComplete(String result) {
                mProgress.hide();
                result = result.replace("?dl=0", "?dl=1");
                ShareUtil.shareSend(MainActivity.this,result);
            }


            @Override
            public void onError(Exception e) {
                mProgress.hide();

                Log.e(TAG, "Failed to download file.", e);
                Toast.makeText(MainActivity.this,
                        "An error has occurred",
                        Toast.LENGTH_SHORT)
                        .show();
            }
        }).execute(remoteFileName);
    }
}


