package com.example.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.IntDef;

import com.bumptech.glide.Glide;
import com.example.coolweather.activity.WeatherActivity;
import com.example.coolweather.gson.HeWeather;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingPic();
        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int updateHz = 8*60*60*1000;
        long triggerAtTime = SystemClock.elapsedRealtime() +updateHz;
        Intent i = new Intent(this,AutoUpdateService.class);
        PendingIntent pendingIntent = PendingIntent.getService(this,0,i,0);
        manager.cancel(pendingIntent);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pendingIntent);



        return super.onStartCommand(intent, flags, startId);
    }

    private void updateBingPic() {
        String bingUrl = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(bingUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("background", bingPic);
                editor.apply();

            }
        });

    }

    private void updateWeather() {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherId = sp.getString("weatherId",null);
        if(weatherId!=null){
            String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=0b3831a3f74e4a35982380c2c6ed7e89";
            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String weatherString = response.body().toString();
                    HeWeather weather = Utility.handleWeatherResponse(weatherString);
                    if(weather!=null&& "ok".equals(weather.getStatus())){
                        SharedPreferences.Editor editor = sp.edit();
                        editor.putString("weather",weatherString);
                        editor.apply();
                    }
                }
            });
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }
}
