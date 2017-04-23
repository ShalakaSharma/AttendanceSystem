package com.example.shalaka.attendancesystem;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.test.mock.MockPackageManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TimePicker;
import android.widget.Toast;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * A login screen that offers login via email/password.
 */
public class RegistrationActivity extends AppCompatActivity {
    // UI references.
    private EditText mEmailView;
    private EditText mFirstNameView;
    private EditText mLastNameView;
    private EditText mstudentIDView;
    private EditText mCourseIDView;
    private EditText mCourseNameView;
    private EditText mDayOfTheWeekView;
    private TimePicker startTimePicker;
    private TimePicker endTimePicker;
    private View mProgressView;
    private View mLoginFormView;

    private String IMEINumber;
    private String android_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(getClass().getSimpleName(), "onCreate() called");
        setContentView(R.layout.activity_registration);

        mEmailView = (EditText) findViewById(R.id.email);
        mFirstNameView = (EditText) findViewById(R.id.studentfirstname);
        mLastNameView = (EditText) findViewById(R.id.studentlastname);
        mstudentIDView = (EditText) findViewById(R.id.studentid);
        mCourseIDView = (EditText) findViewById(R.id.courseid);
        mCourseNameView = (EditText) findViewById(R.id.coursename);
        startTimePicker = (TimePicker) findViewById(R.id.start_time_picker);
        endTimePicker = (TimePicker) findViewById(R.id.end_time_picker);
        mDayOfTheWeekView = (EditText) findViewById(R.id.course_day);

        attemptPermission();

        Button submit = (Button) findViewById(R.id.register_button);
        submit.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View view) {

                String startMin = startTimePicker.getCurrentMinute().toString();

                String startHour = startTimePicker.getCurrentHour().toString();

                String endHour = endTimePicker.getCurrentHour().toString();

                String endMin = endTimePicker.getCurrentMinute().toString();

                attemptRegister(mFirstNameView.getText().toString(),
                        mLastNameView.getText().toString(),
                        mEmailView.getText().toString(),
                        mCourseIDView.getText().toString(),
                        mCourseNameView.getText().toString(),
                        mstudentIDView.getText().toString(),
                        IMEINumber,
                        android_id,
                        mDayOfTheWeekView.getText().toString(),
                        startHour,
                        startMin,
                        endHour,
                        endMin);

            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mProgressView = findViewById(R.id.login_progress);
    }

    private void attemptPermission() {
        Log.i(getClass().getSimpleName(), "Requesting permission to access phone state");
        ActivityCompat.requestPermissions(RegistrationActivity.this,
                new String[]{Manifest.permission.READ_PHONE_STATE,Manifest.permission.ACCESS_FINE_LOCATION},
                1);
    }

    private void attemptLocationPermissions() {
        Log.i(getClass().getSimpleName(),"attemptLocationPermissions() called");
        try {
            if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                    != MockPackageManager.PERMISSION_GRANTED) {

                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        2);
            }
            Log.i(getClass().getSimpleName(),"location");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        Log.i(getClass().getSimpleName(), "Result for permission received");
        switch (requestCode) {
            case 1: {
                Log.i(getClass().getSimpleName(), "Request code was 1");
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(getClass().getSimpleName(), "PERMISSION_GRANTED");
                    Toast.makeText(RegistrationActivity.this, "Permission granted to read your phone state", Toast.LENGTH_SHORT).show();
                    TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
                    IMEINumber = tm.getDeviceId();
                    Log.i(getClass().getSimpleName(), "IMEINumber" + IMEINumber);
                    String phone1 = tm.getLine1Number();
                    Log.i(getClass().getSimpleName(), "Phone Number" + phone1);
                    String subscriberId = tm.getSubscriberId();
                    Log.i(getClass().getSimpleName(), "IMSI" + subscriberId);
                    Toast.makeText(RegistrationActivity.this, "IMEI" + IMEINumber, Toast.LENGTH_SHORT).show();
                    android_id = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
                    Log.i(getClass().getSimpleName(), "unique_id" + android_id);
                    //attemptLocationPermissions();
                } else {
                    Log.i(getClass().getSimpleName(), "PERMISSION_DENIED");
                    Toast.makeText(RegistrationActivity.this, "Permission denied to read your phone state", Toast.LENGTH_LONG).show();
                }
            }
            case 2: {
                Log.i(getClass().getSimpleName(), "Request code was 2");
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i(getClass().getSimpleName(), "PERMISSION_GRANTED");
                    Toast.makeText(RegistrationActivity.this, "Permission granted to use GPS", Toast.LENGTH_SHORT).show();
                } else {
                    Log.i(getClass().getSimpleName(), "PERMISSION_DENIED");
                    Toast.makeText(RegistrationActivity.this, "Permission denied to use GPS", Toast.LENGTH_LONG).show();
                }
            }

        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     *
     * @param first_name
     * @param last_name
     * @param email
     * @param course_ID
     * @param student_ID
     * @param IMEINumber
     * @param android_id
     */
    private void attemptRegister(String first_name, String last_name, String email, String course_ID, String course_name, String student_ID, String IMEINumber, String android_id, String course_day, String start_hour, String start_min, String end_hour, String end_min) {
        Log.i(getClass().getSimpleName(), "attemptRegister() called");
        Log.i(getClass().getSimpleName(), "course_name" + course_name);
        new HttpRequestStudent().execute(first_name, last_name, email, course_ID ,student_ID, IMEINumber, android_id, course_day, start_hour, start_min, end_hour, end_min, course_name);

    }

    public boolean isMyServiceRunning(Class<?> serviceClass) {
        Log.i("BackgroundService", "isMyServiceRunning called");
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    private class HttpRequestStudent extends AsyncTask<String, Object, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            boolean response = false;
            String url = null;
            try {
                url = "http://" + Util.getProperty("Server_IP", getApplicationContext()) + ":8080/access/addRecord";
            } catch (IOException e) {
                e.printStackTrace();
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("first_name", params[0])
                    .queryParam("IMEI", params[5])
                    .queryParam("Android_ID", params[6])
                    .queryParam("Student_ID", params[4])
                    .queryParam("last_name", params[1])
                    .queryParam("email", params[2])
                    .queryParam("course_ID", params[3])
                    .queryParam("course_day", params[7])
                    .queryParam("start_hour", params[8])
                    .queryParam("start_min", params[9])
                    .queryParam("end_hour", params[10])
                    .queryParam("end_min", params[11])
                    .queryParam("course_name", params[12]
                    );
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

            try {
                response = restTemplate.getForObject(
                        builder.build().encode().toUri(),
                        Boolean.class);
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), e.getMessage(), e);
            }
            return response;
        }

        @Override
        protected void onPostExecute(Boolean token) {
            Log.i(getClass().getSimpleName(), "Was student record added?:" + token.toString());
            if(token) {
                scheduleStartOfService();
            }

        }

    }

    public void scheduleStartOfService() {
        Log.i(getClass().getSimpleName(), "scheduleStartOfService() called");
        Log.i(getClass().getSimpleName(), "Background service scheduled");
        Intent intent = new Intent(getApplicationContext(), Scheduler.class);

        final PendingIntent pIntent = PendingIntent.getBroadcast(this, Scheduler.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);

        long firstMillis = System.currentTimeMillis();
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);

        alarm.setInexactRepeating(AlarmManager.RTC_WAKEUP, firstMillis,
                1, pIntent);

    }
}