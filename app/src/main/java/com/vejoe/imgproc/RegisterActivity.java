package com.vejoe.imgproc;

import android.annotation.TargetApi;
import android.app.LoaderManager;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.ContactsContract;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.vejoe.utils.Tools;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.BufferedSource;

import static android.Manifest.permission.READ_CONTACTS;

public class RegisterActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {
    public static final String SaltValue = "HuDoNetDc";
//    public static final String URL_STR = "http://192.168.1.126:8080";
    public static final String URL_STR = "http://api.hudonet.com";
    private static final int REQUEST_READ_CONTACTS = 0;
    private AutoCompleteTextView regEmailView;
    private EditText regPasswordView;
    private EditText regConfirmPasswordView;

    //private RegisterTask regTask = null;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(R.string.register);
        getSupportActionBar().setElevation(0);

        regEmailView = (AutoCompleteTextView) findViewById(R.id.reg_email);
        populateAutoComplete();

        regPasswordView = (EditText) findViewById(R.id.reg_password);
        regConfirmPasswordView = (EditText) findViewById(R.id.reg_confirm_password);

        findViewById(R.id.reg_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                attemptRegister();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    private void populateAutoComplete() {
        if (!mayRequestContacts()) {
            return;
        }

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean mayRequestContacts() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(regEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok, new View.OnClickListener() {
                        @Override
                        @TargetApi(Build.VERSION_CODES.M)
                        public void onClick(View v) {
                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
                        }
                    });
        } else {
            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
        }
        return false;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete();
            }
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        regEmailView.setAdapter(adapter);
    }

    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    private void attemptRegister() {
//        if (regTask != null) {
//            return;
//        }

        regEmailView.setError(null);
        regPasswordView.setError(null);
        regConfirmPasswordView.setError(null);

        String email = regEmailView.getText().toString();
        String password = regPasswordView.getText().toString();
        String confirmPassword = regConfirmPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        if (!password.equals(confirmPassword)) {
            regConfirmPasswordView.setError(getString(R.string.error_password_not_same));
            focusView = regConfirmPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(password)) {
            regPasswordView.setError(getString(R.string.error_field_required));
            focusView = regPasswordView;
            cancel = true;
        } else if (!isPasswordValid(password)) {
            regPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = regPasswordView;
            cancel = true;
        }

        if (TextUtils.isEmpty(email)) {
            regEmailView.setError(getString(R.string.error_field_required));
            focusView = regEmailView;
            cancel = true;
            //} else if (!isEmailValid(email)) {
        } else if (!Tools.isPhoneNumberValid(email)) {
            regEmailView.setError(getString(R.string.error_invalid_phone));
            focusView = regEmailView;
            cancel = true;
        }

        if (cancel) {
            if (focusView != null)
                focusView.requestFocus();
        } else {//验证通过
            OkHttpClient okHttpClient = new OkHttpClient();

            RequestBody body = new FormBody.Builder()
                    .add("user", email)
                    .add("pwd", Tools.strMd5(password + SaltValue))
                    .add("flag", "1")
                    .build();

            final Request request = new Request.Builder()
                    .url(URL_STR)
                    .post(body)
                    .build();

            if (progressDialog == null) {
                progressDialog = ProgressDialog.show(this, "", "正在注册请稍等...", true);
                progressDialog.setCancelable(false);
            }
            progressDialog.show();

            findViewById(R.id.reg_button).setEnabled(false);

            Call call = okHttpClient.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.dismiss();
                            findViewById(R.id.reg_button).setEnabled(true);
                            Toast.makeText(RegisterActivity.this, "注册失败", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    ResponseBody body = response.body();
                    byte[] data = body.bytes();
                    final String result = new String(data);
                    final int code = response.code();
                    final boolean suc = response.isSuccessful();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            findViewById(R.id.reg_button).setEnabled(true);
                            progressDialog.dismiss();
                            if (!suc || !"0".equals(result)) {
                                Toast.makeText(RegisterActivity.this, "注册失败:" + code, Toast.LENGTH_SHORT).show();
                            } else {
                                onBackPressed();
                            }
                        }
                    });
                }
            });
        }


    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private class RegisterTask extends AsyncTask<Void, Void, Boolean> {
        private final String email;
        private final String password;

        public RegisterTask(String email, String password) {
            this.email = email;
            this.password = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            return null;
        }
    }
}
