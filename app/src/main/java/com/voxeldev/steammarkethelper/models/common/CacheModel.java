package com.voxeldev.steammarkethelper.models.common;

import android.content.Context;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Scanner;

public class CacheModel {

    private Context context;

    public CacheModel(Context context) {
        this.context = context;
    }

    public String readFile(String filename) throws Exception {
        File file = new File(context.getFilesDir().getPath() + "/" + filename);

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

    public void writeToFile(String filename, String content) throws Exception {
        File file = new File(context.getFilesDir().getPath() + "/" + filename);

        file.getParentFile().mkdirs();
        BufferedWriter writer = new BufferedWriter(new FileWriter(file, false));
        writer.write(content);
        writer.close();
    }
}
