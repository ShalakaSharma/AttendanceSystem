package com.example.shalaka.attendancesystem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Calendar;

/**
 * Created by Shalaka on 4/21/2017.
 */

public class PingInitiator extends BroadcastReceiver {
    public static final int REQUEST_CODE1 = 1;
    public static final int REQUEST_CODE2 = 2;
    public static final int REQUEST_CODE3 = 3;
    public static int successfulPings=0;
    public static boolean attendanceMarked = false;
    private Context myContext = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        myContext = context;
        Log.i(getClass().getSimpleName(), "onReceive() for PingInitiator called");
        if (isWifiConnected(context) && isLocationCorrect(context)) {
            new HttpRequestPingCheck().execute();
        }

    }

    boolean isWifiConnected(Context context) {

        Log.i(getClass().getSimpleName(),"isWifiConnected() called");
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo;

        wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            String bssid = wifiInfo.getBSSID();
            int frequency = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                frequency = wifiInfo.getFrequency();
            }

            Log.i(getClass().getSimpleName(), bssid + " " + frequency);

        }
        return true;
    }

    boolean isLocationCorrect(Context context) {
        Log.i(getClass().getSimpleName(), "isLocationCorrect() called");
        return true;
    }

    void attendanceComplete(Context context) {
        Log.i(getClass().getSimpleName(), "attendanceComplete() called");

    }

    private class HttpRequestPingCheck extends AsyncTask<String, Object, Course> {
        @Override
        protected Course doInBackground(String... params) {
            Course response = new Course();
            String url = null;
            try {
                url = "http://" + Util.getProperty("Server_IP", myContext) + ":8080/access/newPing";
            } catch (IOException e) {
                e.printStackTrace();
            }
            TelephonyManager tm = (TelephonyManager) myContext.getSystemService(Context.TELEPHONY_SERVICE);
            String IMEINumber = tm.getDeviceId();
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("IMEI", IMEINumber);
            RestTemplate restTemplate = new RestTemplate();
            restTemplate.getMessageConverters().add(new MappingJackson2HttpMessageConverter());

            try {
                response = restTemplate.getForObject(
                        builder.build().encode().toUri(),
                        Course.class);
            } catch (Exception e) {
                Log.e(getClass().getSimpleName(), e.getMessage(), e);
            }
            return response;
        }

        @Override
        protected void onPostExecute(Course course) {
            Log.i(getClass().getSimpleName(), "onPostExecute() for fetching course details");
            Log.i(getClass().getSimpleName(), "Course Details received");
            Log.i(getClass().getSimpleName(), "" + course);

            if(course.getCourse_ID() != null) {
                successfulPings++;
            }
            Log.i(getClass().getSimpleName(), "successfulPings: " + successfulPings);

            if ((!attendanceMarked) && (successfulPings >2  && successfulPings <= 5)) {
                Log.i(getClass().getSimpleName(), "Ping Verification Completed. successfulPings: " + successfulPings);
                TelephonyManager tm = (TelephonyManager) myContext.getSystemService(Context.TELEPHONY_SERVICE);
                String IMEINumber = tm.getDeviceId();
                new HttpRequestMarkAttendance().execute(IMEINumber);
            }
        }

    }

    private class HttpRequestMarkAttendance extends AsyncTask<String, Object, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            Boolean response = false;
            String url = null;
            try {
                url = "http://" + Util.getProperty("Server_IP", myContext) + ":8080/access/markAttendance";
            } catch (IOException e) {
                e.printStackTrace();
            }

            TelephonyManager tm = (TelephonyManager) myContext.getSystemService(Context.TELEPHONY_SERVICE);
            String IMEINumber = tm.getDeviceId();
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
                    .queryParam("IMEI", IMEINumber);
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
        protected void onPostExecute(Boolean response) {
            Log.i(getClass().getSimpleName(), "onPostExecute() for HttpRequestMarkAttendance");
            Calendar calendar = Calendar.getInstance();
            attendanceMarked = response;
            if(response) {
                Log.i(getClass().getSimpleName(), "Attendance marked for : " + calendar.getTime());
            } else {
                Log.i(getClass().getSimpleName(), "Attendance could not be marked for : " + calendar.getTime());
            }
        }
    }

}