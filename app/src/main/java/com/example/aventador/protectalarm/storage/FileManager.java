package com.example.aventador.protectalarm.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.comthings.gollum.api.gollumandroidlib.callback.GollumCallbackGetGeneric;
import com.example.aventador.protectalarm.R;
import com.example.aventador.protectalarm.fragments.settings.SettingsHub;
import com.example.aventador.protectalarm.tools.Logger;
import com.github.angads25.filepicker.controller.DialogSelectionListener;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;


import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

/**
 * Created by Giangrasso on 11/10/2017.
 */

/**
 * It's responsible for loading and saving the Configurations.
 * It use Gson library to serialize/deserialize the differents configs.
 * It use a FilePicker from android arsenal to select a config.
 */
public class FileManager implements ISharedPreferencesManager {
    private final static String TAG = "FileManager";
    private static FileManager instance;
    private Gson gson;
    public static FileManager getInstance() {
        if (instance == null) {
            instance = new FileManager();
        }

        return instance;
    }

    private FileManager() {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.excludeFieldsWithoutExposeAnnotation();
        gson = gsonBuilder.create();
    }

    /**
     * Get the content of the given file.
     * @param file
     * @return
     */
    @Nullable
    private String getFileContent(@NonNull File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] data = new byte[(int) file.length()];
            fis.read(data);
            fis.close();
            return new String(data, "UTF-8");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get a configuration from the given filePath. May returns null
     * @param filePath
     * @return
     */
    @Nullable
    private Configuration load(@NonNull String filePath) {
        Logger.d(TAG, "private load()");
        File file = new File(filePath);
        if (file.exists()) {
            Logger.d(TAG, "file exist");
            String fileContent = getFileContent(file);
            if (fileContent != null) {
                Logger.d(TAG, "fileContent is: " + fileContent);
                return gson.fromJson(fileContent, Configuration.class); // deserializing... and return the Configuration.
            }
        } else {
            Logger.d(TAG, "file don't exist");
        }
        return null;
    }

    /**
     * Save settings of the app.
     * @param context
     * @param fileName
     * @param settingsHub
     * @return
     */
    public boolean saveSettings(@NonNull Context context, @Nullable String fileName, @Nullable SettingsHub settingsHub) {
        if (settingsHub == null || fileName == null) {
            Logger.e(TAG, "saveSettings: settingsHub: " + settingsHub + " filename: " + fileName);
            return false;
        }

        FileOutputStream outputStream;
        try {
            outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            outputStream.write(new Gson().toJson(settingsHub).getBytes());
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load settings of the app.
     * @param context
     * @param fileName
     * @return
     */
    @Nullable
    public SettingsHub loadSettings(@NonNull Context context, @Nullable String fileName) {
        if (fileName ==  null) {
            Logger.e(TAG, "loadSettings: fileName is null");
            return null;
        }
        FileInputStream inputStream;
        try {
            inputStream = context.openFileInput(fileName);
            String content = new Scanner(inputStream).useDelimiter("\\Z").next();
            return new Gson().fromJson(content, SettingsHub.class);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Save the configuration in directory "Documents/PandwarfDefender/configs/ in a file : fileName.json
     * @param context
     * @param fileName
     * @param configuration
     * @return true -> success, false -> fail
     */
    public boolean save(@NonNull Context context, @NonNull String fileName, @NonNull Configuration configuration) {
        Logger.d(TAG, "private save()");

        File fileDirDocuments = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsoluteFile();
        String rootDir = context.getString(R.string.root_dir);
        String configDir = context.getString(R.string.config_dir);
        File configLocation = new File(fileDirDocuments.toString() + "/" + rootDir + "/" + configDir);
        configLocation.mkdirs(); // create missing directories.
        Logger.d(TAG, "configLocation: " + configLocation);
        File fileConfiguration = new File(configLocation.getAbsolutePath().toString() + "/" + fileName + ".json");

        try {
            fileConfiguration.createNewFile();
            FileOutputStream fos = new FileOutputStream(fileConfiguration);
            PrintWriter printWriter = new PrintWriter(fos);
            Logger.d(TAG, "configuration: " + configuration.toString());
            printWriter.print(gson.toJson(configuration)); // serializing and store it in the file.
            printWriter.close();
            return true;
        } catch (FileNotFoundException e) {
            Logger.d(TAG, "exception 1");
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            Logger.d(TAG, "exception 2");
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Load a configuration. This method displays the file picker. this file picker get the selected file config.
     * This file content is deserialized and given to the callback cbDone
     * @param context
     * @param cbDone
     */
    public void load(@NonNull Context context, @NonNull final GollumCallbackGetGeneric<Configuration> cbDone) {
        Logger.d(TAG, "load()");
        DialogProperties dialogProperties = new DialogProperties();
        dialogProperties.selection_type = DialogConfigs.FILE_SELECT;
        dialogProperties.selection_mode = DialogConfigs.SINGLE_MODE;
        dialogProperties.root = Environment.getExternalStorageDirectory();
        dialogProperties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        dialogProperties.offset = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS);
        dialogProperties.extensions = new String[]{"json"};

        FilePickerDialog filePickerDialog = new FilePickerDialog(context, dialogProperties);
        filePickerDialog.setDialogSelectionListener(new DialogSelectionListener() {
            @Override
            public void onSelectedFilePaths(String[] files) {
                if (files.length > 0) {
                    Logger.d(TAG, "file selected: " + files[0]);
                    Configuration configuration = load(files[0]);
                    if (configuration != null) {
                        Logger.d(TAG, "configuration NOT NULL");
                        cbDone.done(configuration, null);
                    } else {
                        Logger.d(TAG, "configuration IS NULL");
                        cbDone.done(null, null);
                    }
                }
            }
        });
        filePickerDialog.show();
    }

    /**
     * Indicates if Shared Preferences contains this key in memory.
     * @param context
     * @param key
     * @return
     */
    public boolean fromSharedPrefsContains(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.shared_prefs_key), Context.MODE_PRIVATE);
        return sharedPreferences.contains(key);
    }

    /**
     * Returns a string linked to the given key.
     * If no link found null is returned.
     * @param context
     * @param key
     * @return
     */
    @Nullable
    @Override
    public String fromSharedPrefsGet(Context context, String key) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.shared_prefs_key), Context.MODE_PRIVATE);
        if (fromSharedPrefsContains(context, key)) {
            return sharedPreferences.getString(key, null);
        }
        return null;
    }

    /**
     * Returns true if value is successfully stored, false otherwise.
     * @param context
     * @param key
     * @param value
     * @return
     */
    @Override
    public boolean fromSharedPrefsPut(Context context, String key, String value) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getString(R.string.shared_prefs_key), Context.MODE_PRIVATE);
        sharedPreferences.edit().putString(key, value).commit(); // commit and not apply because just after I check if value is successfully stored.
        return fromSharedPrefsContains(context, key);
    }
}
