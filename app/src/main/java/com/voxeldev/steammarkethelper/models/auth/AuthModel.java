package com.voxeldev.steammarkethelper.models.auth;

import android.content.Context;
import android.util.Log;

import com.voxeldev.steammarkethelper.MainActivity;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AuthModel {
    private final OkHttpClient client;
    private final Context context;

    public AuthModel(Context context){
        this.context = context;
        client = new OkHttpClient();
    }

    public boolean checkAuth(String cookie){
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36";
        Request request = new Request.Builder()
                .url("https://steamcommunity.com/")
                .addHeader("Cookie", cookie)
                .addHeader("User-Agent", userAgent)
                .build();

        try{
            Response response = client.newCall(request).execute();

            return response.body().string().contains("https://steamcommunity.com/login/home");
        }
        catch (Exception e){ Log.e(MainActivity.LOG_TAG, e.toString()); }

        return true;
    }

    public void saveCookie(String cookie) throws Exception {
        File file = new File(context.getFilesDir().getPath() + "/cookie.smh");
        file.getParentFile().mkdirs();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
        writer.write(cookie);
        writer.close();
    }

    public String loadCookie() throws Exception {
        File file = new File(context.getFilesDir().getPath() + "/cookie.smh");
        if (file.exists()){
            BufferedReader reader = new BufferedReader(new FileReader(file));
            StringBuilder stringBuilder = new StringBuilder();

            Scanner scanner = new Scanner(reader);
            while (scanner.hasNextLine()){
                stringBuilder.append(scanner.nextLine());
            }
            scanner.close();

            return stringBuilder.toString();
        }
        else{
            return null;
        }
    }
}
