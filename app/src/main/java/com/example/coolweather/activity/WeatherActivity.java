package com.example.coolweather.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.coolweather.R;
import com.example.coolweather.gson.Daily_forecast;
import com.example.coolweather.gson.HeWeather;
import com.example.coolweather.service.AutoUpdateService;
import com.example.coolweather.util.HttpUtil;
import com.example.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {
    private ScrollView weatherLayout;
    private TextView titleCity;
    private TextView updateTime;
    private TextView degreeText;
    private TextView weatherInfoText;
    private Button nav;
    private LinearLayout forecastLayout;
    private TextView aqiText;
    private TextView pmText;
    private TextView comfortText;
    private TextView carWashText;
    private TextView sportText;
    private ImageView background;
    public SwipeRefreshLayout swipRefresh;
    private String mWeatherId;
    public DrawerLayout drawerLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21) {
            View view = getWindow().getDecorView();
            view.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);

        initView();
        nav.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        swipRefresh.setColorSchemeResources(R.color.colorPrimary);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        String bingPicString = prefs.getString("background", null);
        if (bingPicString != null) {
            Glide.with(this).load(bingPicString).into(background);
        } else {
            loadBingPic();
        }
        if (weatherString != null) {
            HeWeather weather = Utility.handleWeatherResponse(weatherString);
            mWeatherId = weather.getBasic().getId();
            showWeatherInfo(weather);
        } else {
            String weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.INVISIBLE);
            mWeatherId = weatherId;
            requestWeather(weatherId);
        }


        swipRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });


    }

    private void loadBingPic() {
        String bingUrl = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(bingUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("background", bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(bingPic).into(background);
                    }
                });
            }
        });
    }

    public void requestWeather(final String weatherId) {
        String weatherUrl = "http://guolin.tech/api/weather?cityid=" + weatherId + "&key=0b3831a3f74e4a35982380c2c6ed7e89";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseString = response.body().string();
                final HeWeather weather = Utility.handleWeatherResponse(responseString);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (weather != null && "ok".equals(weather.getStatus())) {
                            mWeatherId = weather.getBasic().getId();
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseString);
                            editor.putString("weatherId",mWeatherId);
                            editor.apply();
                            showWeatherInfo(weather);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipRefresh.setRefreshing(false);

                    }
                });

            }
        });

        loadBingPic();
    }

    private void showWeatherInfo(HeWeather weather) {
        String cityName = weather.getBasic().getCity();
        String updateTimeContent = weather.getBasic().getUpdate().getLoc().split(" ")[1];
        String degree = weather.getNow().getTmp();
        String weatherInfo = weather.getNow().getCond().getTxt();
        titleCity.setText(cityName);
        updateTime.setText(updateTimeContent);
        degreeText.setText(degree + "℃");
        weatherInfoText.setText(weatherInfo);

        forecastLayout.removeAllViews();
        for (Daily_forecast forecast : weather.getDaily_forecast()) {
            View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dataText = view.findViewById(R.id.tv_date_text);
            TextView infoText = view.findViewById(R.id.tv_info_text);
            TextView maxText = view.findViewById(R.id.tv_max);
            TextView minText = view.findViewById(R.id.tv_min);
            dataText.setText(forecast.getDate());
            infoText.setText(forecast.getCond().getTxt_d());
            maxText.setText(forecast.getTmp().getMax());
            minText.setText(forecast.getTmp().getMin());
            forecastLayout.addView(view);
        }

        if (weather.getAqi() != null) {
            aqiText.setText(weather.getAqi().getCity().getAqi());
            pmText.setText(weather.getAqi().getCity().getPm25());

        }

        comfortText.setText("舒适度：" + weather.getSuggestion().getComf().getTxt());
        carWashText.setText("洗车指数：" + weather.getSuggestion().getCw().getTxt());
        sportText.setText("运动建议：" + weather.getSuggestion().getSport().getTxt());

        weatherLayout.setVisibility(View.VISIBLE);

        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);


    }

    private void initView() {
        nav = (Button) findViewById(R.id.btn_nav);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        swipRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        background = (ImageView) findViewById(R.id.tv_backgound);
        weatherLayout = (ScrollView) findViewById(R.id.sv_weather);
        titleCity = (TextView) findViewById(R.id.tv_title_city);
        updateTime = (TextView) findViewById(R.id.tv_update_time);
        degreeText = (TextView) findViewById(R.id.tv_degree_text);
        weatherInfoText = (TextView) findViewById(R.id.tv_weather_info_text);
        forecastLayout = (LinearLayout) findViewById(R.id.ll_forecast);
        aqiText = (TextView) findViewById(R.id.tv_aqi);
        pmText = (TextView) findViewById(R.id.tv_pm);
        comfortText = (TextView) findViewById(R.id.tv_comfort_text);
        carWashText = (TextView) findViewById(R.id.tv_wash_text);
        sportText = (TextView) findViewById(R.id.tv_sport_text);
    }
}
