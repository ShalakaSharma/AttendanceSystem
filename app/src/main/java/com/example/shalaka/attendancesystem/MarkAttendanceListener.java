package com.example.shalaka.attendancesystem;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Created by Shalaka on 4/15/2017.
 */

public class MarkAttendanceListener extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;

    private Context myContext = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        myContext = context;
        Log.i(getClass().getSimpleName(),  "onReceive() for MarkAttendanceListener called");
        if (isWifiConnected(context) && isLocationCorrect(context)) {
            new HttpRequestPing().execute();
        }

    }
    boolean isWifiConnected(Context context) {

        Log.i(getClass().getSimpleName(),  "isWifiConnected() called");
        WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        WifiInfo wifiInfo;
        wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo.getSupplicantState() == SupplicantState.COMPLETED) {
            String bssid = wifiInfo.getBSSID();
            int frequency = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                frequency = wifiInfo.getFrequency();
            }
            Log.i(getClass().getSimpleName(),  bssid + " " + frequency);
            if(bssid.equalsIgnoreCase("ec:aa:a0:d3:28:08")) {
                Log.i(getClass().getSimpleName(), "Wifi Verified: true");
                return true;
            }
        }
        Log.i(getClass().getSimpleName(), "Wifi Verified: false");
        return false;
    }

    boolean isLocationCorrect(Context context) {
        Log.i(getClass().getSimpleName(), "isLocationCorrect() called");
        GPSTracker gps;

        Log.i(getClass().getSimpleName(),"location");
        gps = new GPSTracker(context);
        Log.i(getClass().getSimpleName(),"location");

        if(gps.canGetLocation()){

            double latitude = gps.getLatitude();
            double longitude = gps.getLongitude();

            Log.i(getClass().getSimpleName(), "Your Location is - \nLat: "
                    + latitude + "\nLong: " + longitude);
            Log.i(getClass().getSimpleName(),""+latitude +" "+longitude);
            double dist = calculateDistanceInMeter(latitude,longitude,39.1693236,-86.4930608);
            if(dist<50){
                Log.i(getClass().getSimpleName(),"attending" + dist);
                Log.i(getClass().getSimpleName(),"Location Verified: " + true);
                return true;
            }
            else{
                Log.i(getClass().getSimpleName(),"not attending" + dist );
                Log.i(getClass().getSimpleName(),"Location Verified: " + false);
                return false;
            }

        }else{
            Log.i(getClass().getSimpleName(), "Location not found");
            gps.showSettingsAlert();
        }
        Log.i(getClass().getSimpleName(),"Location Verified: " + false);
        Log.i(getClass().getSimpleName(),"GPS not working.");
        return false;
    }
    public final static double AVERAGE_RADIUS_OF_EARTH_KM = 6371000;
    public double calculateDistanceInMeter(double userLat, double userLng,
                                           double venueLat, double venueLng) {

        double latDistance = Math.toRadians(userLat - venueLat);
        double lngDistance = Math.toRadians(userLng - venueLng);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(venueLat))
                * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return AVERAGE_RADIUS_OF_EARTH_KM * c;
    }
    private class HttpRequestPing extends AsyncTask<String, Object, Course> {
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
        protected void onPostExecute(Course response) {
            Log.i(getClass().getSimpleName(), "onPostExecute() for successful ping");
            Log.i(getClass().getSimpleName(), "Course Details received");
            Log.i(getClass().getSimpleName(), "" + response);
            Log.i(getClass().getSimpleName(), "" + response.getCourse_ID() + " " +  response.getStart_time() + " "+ response.getEnd_time() + " " + response.getCourse_name() + " "+ response.getDay() );

            if(response.getCourse_ID() != null) {
                setAlarmsForPings(response);
            } else {
                Log.i(getClass().getSimpleName(), "Course Details received");
            }

        }
    }

    public void setAlarmsForPings(Course course) {

        Date sDate = new Date();
        Date eDate = new Date();
        sDate.setHours(course.getStart_time().getHours());
        sDate.setMinutes(course.getStart_time().getMinutes());
        eDate.setHours(course.getStart_time().getHours());
        eDate.setMinutes(course.getStart_time().getMinutes());

        long diff = eDate.getTime() - sDate.getTime();

        long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);

        Calendar sCalendar = dateToCalendar(sDate);

        //sCalendar.add(Calendar.MINUTE, (int)minutes/5);
        sCalendar.add(Calendar.MINUTE, 2);

        Log.i(getClass().getSimpleName(), "Hours on post after: " + sCalendar.get(Calendar.HOUR_OF_DAY));
        Log.i(getClass().getSimpleName(), "Minutes on post after" + sCalendar.get(Calendar.MINUTE));
        Log.i(getClass().getSimpleName(), "Day of Week on post after" + sCalendar.get(Calendar.DAY_OF_WEEK));
        Log.i(getClass().getSimpleName(), "Minutes to add after:" + (int) (minutes / 3));
        int k = 0;
        while (k < 5) {
            schedulePing(myContext, sCalendar, (k+1));
            //sCalendar.add(Calendar.MINUTE, (int)minutes/5);
            sCalendar.add(Calendar.MINUTE, 2);
            k++;
        }
    }

    private static Calendar dateToCalendar(Date date) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        return calendar;
    }

    public void schedulePing(Context context, Calendar calendar, int requestCode) {

        Log.i(getClass().getSimpleName(), "schedulePing() called");
        Log.i(getClass().getSimpleName(), "" + context);
        Log.i(getClass().getSimpleName(), "Hours: " + calendar.get(Calendar.HOUR_OF_DAY));
        Log.i(getClass().getSimpleName(), "Minutes" + calendar.get(Calendar.MINUTE));
        Log.i(getClass().getSimpleName(), "Day of Week" + calendar.get(Calendar.DAY_OF_WEEK));
        Log.i(getClass().getSimpleName(), "Month: " + calendar.get(Calendar.MONTH));
        Log.i(getClass().getSimpleName(), "Day" + calendar.get(Calendar.DAY_OF_MONTH));
        Log.i(getClass().getSimpleName(), "Year" + calendar.get(Calendar.YEAR));

        Intent i = new Intent(context, PingInitiator.class);

        final PendingIntent pIntent = PendingIntent.getBroadcast(context, requestCode,
                i, PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarm = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarm.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pIntent);
        Log.i(getClass().getSimpleName(), "Alarm was set for : " + calendar.getTime());
    }
}