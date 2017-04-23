package com.example.shalaka.attendancesystem;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Calendar;

public class BackgroundService extends Service {

    static boolean isScheduled = false;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(getClass().getSimpleName(), "onStartCommand() called");
        Log.i(getClass().getSimpleName(), "BackgroundService started");
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String IMEINumber = tm.getDeviceId();
        if(!isScheduled) {
            new HttpRequestStudentDetails().execute(IMEINumber);
        }
        return START_STICKY;
    }
    private class HttpRequestStudentDetails extends AsyncTask<String, Object, Student> {
        @Override
        protected Student doInBackground(String... params) {
            Student response = null;
            String url = null;
            try {
                url = "http://" + Util.getProperty("Server_IP", getApplicationContext()) + ":8080/access/studentDetails";
            } catch (IOException e) {
                e.printStackTrace();
            }

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("IMEI", params[0]);
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

            try {
                response = restTemplate.getForObject(
                        builder.build().encode().toUri(),
                        Student.class);
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(),  e.getMessage(), e);
            }
            return response;
        }

        @Override
        protected void onPostExecute(Student student) {
            Log.i(getClass().getSimpleName(), "onPostExecute() for fetching student details");
            Log.i(getClass().getSimpleName(), "Student Details received");
            Log.i(getClass().getSimpleName(), "" + student);
            if (student != null) {
                Log.i(getClass().getSimpleName(), " " + student.getId() + student.getFirst_name());
                scheduleAttendanceService(getApplicationContext(), student.getCourse());
            } else {
                Log.i(getClass().getSimpleName(), "student information received is null");
            }
        }
    }

    public void scheduleAttendanceService(Context context, Course course) {

        Log.i(getClass().getSimpleName(), "scheduleAttendanceService() called");

        isScheduled = true;

        Intent i = new Intent(context, MarkAttendanceListener.class);

        final PendingIntent pIntent = PendingIntent.getBroadcast(context, MarkAttendanceListener.REQUEST_CODE,
                i, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calendar = Calendar.getInstance();

        calendar.set(Calendar.HOUR_OF_DAY, course.getStart_time().getHours());

        calendar.set(Calendar.MINUTE, course.getStart_time().getMinutes());

        calendar.set(Calendar.SECOND, 0);

        calendar.set(Calendar.DAY_OF_WEEK, getDay(course.getDay()));

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pIntent);

    }



    private static int getDay(String day) {
        switch(day) {
            case "Sunday": {
                return Calendar.SUNDAY;
            } case "Monday": {
                return Calendar.MONDAY;
            } case "Tuesday": {
                return Calendar.TUESDAY;
            } case "Wednesday": {
                return Calendar.WEDNESDAY;
            } case "Thursday": {
                return Calendar.THURSDAY;
            } case "Friday": {
                return Calendar.FRIDAY;
            } case "Saturday": {
                return Calendar.SATURDAY;
            } default: {
                return Calendar.MONDAY;
            }
        }
    }

    public BackgroundService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        Log.i(getClass().getSimpleName(), "Created");
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.i(getClass().getSimpleName(), "onDestroy() called");
        Toast.makeText(this, "Service Destroyed", Toast.LENGTH_LONG).show();

    }
}
