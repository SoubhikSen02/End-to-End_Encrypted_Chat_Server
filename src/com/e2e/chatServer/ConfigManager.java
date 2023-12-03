package com.e2e.chatServer;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {
    //app folder, config folder and config file path initializers
    private static String appFolderPath;
    private static String configFolderPath;
    private static String configFilePath;


    //config variables
    private static String serverPort = "3737";
    private static String clientTimeoutInSeconds = "300";
    private static String serverVersion = "1.0";
    private static String currentTheme = "Arc Dark (Material)";


    //getters
    synchronized public static String getServerPort() {
        return serverPort;
    }

    synchronized public static String getClientTimeoutInSeconds() {
        return clientTimeoutInSeconds;
    }

    synchronized public static String getServerVersion() {
        return serverVersion;
    }

    synchronized public static String getAppFolderPath() {
        return appFolderPath;
    }

    synchronized public static String getCurrentTheme()
    {
        return currentTheme;
    }

    //setters
    synchronized public static void setServerPort(String serverPort) {
        ConfigManager.serverPort = serverPort;
        updateConfig();
        LogManager.updateServerMainLog("Server port number updated to '" + serverPort + "' in config.");
    }

    synchronized public static void setClientTimeoutInSeconds(String clientTimeoutInSeconds) {
        ConfigManager.clientTimeoutInSeconds = clientTimeoutInSeconds;
        updateConfig();
        LogManager.updateServerMainLog("Client timeout period updated to '" + clientTimeoutInSeconds + "' in config.");
    }

    synchronized public static void setCurrentTheme(String newTheme)
    {
        currentTheme = newTheme;
        updateConfig();
        LogManager.updateServerMainLog("Theme updated to '" + newTheme + "' in config.");
    }

//    synchronized private static void setServerVersion(String serverVersion) {
//        ConfigManager.serverVersion = serverVersion;
//        updateConfig();
//    }


    //config initializer on app load
    synchronized public static boolean initializeConfig()
    {
        LogManager.updateServerMainLog("Config initialization started.");
        appFolderPath = FileSystemView.getFileSystemView().getDefaultDirectory().toString() + "\\E2E Chat Server";
        configFolderPath = appFolderPath + "\\config";
        configFilePath = configFolderPath + "\\config.xml";
        File configFile = new File(configFilePath);
        if(configFile.exists())
        {
            readConfig();
            LogManager.updateServerMainLog("Config successfully initialized with existing file read.");
            return true;
        }
        else
        {
            File appFolder = new File(appFolderPath);
            if(!appFolder.exists())
            {
                boolean success = appFolder.mkdir();
                if(!success)
                    return false;
            }

            File configFolder = new File(configFolderPath);
            if(!configFolder.exists())
            {
                boolean success = configFolder.mkdir();
                if(!success)
                    return false;
            }

            updateConfig();
            LogManager.updateServerMainLog("Config successfully initialized with new file write.");
            return true;
        }
    }

    synchronized private static void readConfig()
    {
        Properties configData = new Properties();

        try(FileInputStream configFile = new FileInputStream(configFilePath))
        {
            configData.loadFromXML(configFile);
        }
        catch(IOException fileReadError)
        {
            LogManager.updateServerMainLog("Exception occurred on trying to read from config file.");
            return;
        }

        serverPort = configData.getProperty("serverPort");
        clientTimeoutInSeconds = configData.getProperty("clientTimeoutInSeconds");
        serverVersion = configData.getProperty("serverVersion");
        currentTheme = configData.getProperty("currentTheme");
    }

    synchronized private static void updateConfig()
    {
        Properties configData = new Properties();
        configData.setProperty("serverPort", serverPort);
        configData.setProperty("clientTimeoutInSeconds", clientTimeoutInSeconds);
        configData.setProperty("serverVersion", serverVersion);
        configData.setProperty("currentTheme", currentTheme);

        try(FileOutputStream configFile = new FileOutputStream(configFilePath))
        {
            configData.storeToXML(configFile, null);
        }
        catch(IOException fileWriteError)
        {
            LogManager.updateServerMainLog("Exception occurred on trying to write to config file.");
            return;
        }
    }

}
