/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.example.sunshine.sync;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.text.format.DateUtils;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.sunshine.ForecastAdapter;
import com.example.sunshine.data.SunshinePreferences;
import com.example.sunshine.data.WeatherContract;
import com.example.sunshine.utilities.NotificationUtils;
import com.example.sunshine.utilities.OpenWeatherJsonUtils;

import org.json.JSONArray;
import org.json.JSONException;

import java.net.URL;
import java.util.ArrayList;

public class SunshineSyncTask {
    private static final String OWM_LIST = "list";;


synchronized public static void syncWeather(Context context) {

        // Instantiate the RequestQueue.

        RequestQueue queue = Volley.newRequestQueue(context);
        String url = "https://andfun-weather.udacity.com/staticweather";

        // Request a string response from the provided URL.
        JsonObjectRequest stringRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        JSONArray jsonArray = response.getJSONArray(OWM_LIST);
                        ContentValues[] weatherValues = OpenWeatherJsonUtils.getWeatherContentValuesFromJson(context, jsonArray);
                        if (weatherValues != null && weatherValues.length != 0) {
                            /* Get a handle on the ContentResolver to delete and insert data */
                            ContentResolver sunshineContentResolver = context.getContentResolver();

                            /* Delete old weather data because we don't need to keep multiple days' data */
                            sunshineContentResolver.delete(
                                    WeatherContract.WeatherEntry.CONTENT_URI,
                                    null,
                                    null);

                            /* Insert our new weather data into Sunshine's ContentProvider */
                            sunshineContentResolver.bulkInsert(
                                    WeatherContract.WeatherEntry.CONTENT_URI,
                                    weatherValues);
                            /*
                             * Finally, after we insert data into the ContentProvider, determine whether or not
                             * we should notify the user that the weather has been refreshed.
                             */
                            boolean notificationsEnabled = SunshinePreferences.areNotificationsEnabled(context);

                            /*
                             * If the last notification was shown was more than 1 day ago, we want to send
                             * another notification to the user that the weather has been updated. Remember,
                             * it's important that you shouldn't spam your users with notifications.
                             */
                            long timeSinceLastNotification = SunshinePreferences
                                    .getEllapsedTimeSinceLastNotification(context);

                            boolean oneDayPassedSinceLastNotification = false;

                            if (timeSinceLastNotification >= DateUtils.DAY_IN_MILLIS) {
                                oneDayPassedSinceLastNotification = true;
                            }

                            /*
                             * We only want to show the notification if the user wants them shown and we
                             * haven't shown a notification in the past day.
                             */
                            if (notificationsEnabled && oneDayPassedSinceLastNotification) {
                                NotificationUtils.notifyUserOfNewWeather(context);
                            }
                        }

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                }, error -> {
        });

// Add the request to the RequestQueue.
        queue.add(stringRequest);
    }
}