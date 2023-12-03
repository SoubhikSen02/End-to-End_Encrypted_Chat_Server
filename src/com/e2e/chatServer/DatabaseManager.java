package com.e2e.chatServer;

import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    //app folder, database folder and database file path store
    private static String appFolderPath;
    private static String dbFolderPath;
    private static String dbFilePath;

    //database connection status
    private static Connection dbConnection = null;


    //database initializer on app load and reinitialization anytime during runtime
    synchronized public static boolean initializeDB()
    {
        LogManager.updateServerMainLog("Database initialization started.");
        appFolderPath = ConfigManager.getAppFolderPath();
        dbFolderPath = appFolderPath + "\\database";
        dbFilePath = dbFolderPath + "\\server_main.db";

        File appFolder = new File(appFolderPath);
        if(!appFolder.exists())
        {
            boolean success = appFolder.mkdir();
            if(!success)
                return false;
        }

        File dbFolder = new File(dbFolderPath);
        if(!dbFolder.exists())
        {
            boolean success = dbFolder.mkdir();
            if(!success)
                return false;
        }

        try
        {
            if(dbConnection != null) {
                dbConnection.close();
                LogManager.updateServerMainLog("Previous database connection closed.");
            }
            dbConnection = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath);
            LogManager.updateServerMainLog("New database connection opened.");
        }
        catch(Exception e)
        {
            LogManager.updateServerMainLog("Exception occurred while trying to connect to database.");
            return false;
        }

        try{
            if(!dbConnection.getMetaData().getTables(null, null, "accounts", null).next())
            {
                makeUpdate("create table accounts(account_id char(16) primary key not null, username varchar(32) unique not null, password varchar(32) not null, online integer(2) not null, permanent_session integer(2) not null, session_id char(16) not null, display_name varchar(64) not null, allow_new_personal_chat integer(2) not null, allow_new_group_chat integer(2) not null);");
            }
            String[][] defaultUser = makeQuery("select * from accounts where account_id = '0000000000000000';");
            if(defaultUser == null || defaultUser.length == 0)
            {
                makeUpdate("insert into accounts values('0000000000000000', 'anonymous', '@n0nym0us', 0, 0, 'undefined', 'anonymous', 0, 0);");
            }

            if(!dbConnection.getMetaData().getTables(null, null, "securityQuestions", null).next())
            {
                makeUpdate("create table securityQuestions(account_id char(16) not null, question varchar(64) not null, answer varchar(64) not null, primary key(account_id, question));");
            }

            if(!dbConnection.getMetaData().getTables(null, null, "chat", null).next())
            {
                makeUpdate("create table chat(chat_id number(32) primary key not null, chat_type varchar(8) not null, chat_name varchar(64) not null, chat_participants varchar(16384) not null);");
            }
            else
            {
                String[][] chatList = makeQuery("select chat_id from chat;");
                if(chatList != null)
                {
                    DatabaseMetaData tableList = dbConnection.getMetaData();
                    for(int i = 0; i < chatList.length; i++)
                    {
                        if(!tableList.getTables(null, null, "chat" + chatList[i][0], null).next())
                        {
                            makeUpdate("create table chat" + chatList[i][0] + "(message_id number(64) primary key not null, from_account_id char(16) not null, message_timestamp number(64) not null, message_type varchar(16) not null, message_content varchar(1048576) not null);");
                        }
                    }
                }
            }

            if(!dbConnection.getMetaData().getTables(null, null, "unsentNewChats", null).next())
            {
                makeUpdate("create table unsentNewChats(to_be_sent_to char(16) not null, chat_id number(32) not null, chat_type varchar(8) not null, chat_name varchar(64) not null, chat_participants varchar(16384) not null, primary key(to_be_sent_to, chat_id));");
            }

            if(!dbConnection.getMetaData().getTables(null, null, "unsentNewMessages", null).next())
            {
                makeUpdate("create table unsentNewMessages(to_be_sent_to char(16) not null, chat_id number(32) not null, message_id number(64) not null, from_account_id char(16) not null, message_timestamp number(64) not null, message_type varchar(16) not null, message_content varchar(1048576) not null, primary key(to_be_sent_to, chat_id, message_id));");
            }

            if(!dbConnection.getMetaData().getTables(null, null, "unsentNewReadReceipts", null).next())
            {
                makeUpdate("create table unsentNewReadReceipts(to_be_sent_to char(16) not null, chat_id number(32) not null, message_id number(64) not null, read_state integer(2) not null, primary key(to_be_sent_to, chat_id, message_id, read_state));");
            }

            LogManager.updateServerMainLog("Database tables initialized.");
        }
        catch(Exception e)
        {
            LogManager.updateServerMainLog("Exception occurred while trying to initialize database tables.");
            return false;
        }
        return true;
    }

    //close down database connection
    synchronized public static void closeDB()
    {
        try
        {
            if(dbConnection != null) {
                dbConnection.close();
                dbConnection = null;
                LogManager.updateServerMainLog("Previous database connection closed.");
            }
        }
        catch(Exception e)
        {
            LogManager.updateServerMainLog("Exception occurred while trying to disconnect database connection.");
        }
    }

    //for executing create, insert, delete, drop, update, modify and other statements which change database schema or table data
    synchronized public static boolean makeUpdate(String statement)
    {
        //System.out.println(statement);
        if(dbConnection == null)
            return false;
        try {
            Statement updateStatement = dbConnection.createStatement();
            updateStatement.setQueryTimeout(30);

            updateStatement.executeUpdate(statement);

            updateStatement.close();
        }
        catch (Exception e)
        {
            LogManager.updateServerMainLog("Database threw an exception while executing the following statement: " + statement);
            return false;
        }
        return true;
    }

    //for executing select statements to make any queries in the db
    synchronized public static String[][] makeQuery(String statement)
    {
        ResultSet queryResultSet;
        List<String[]> queryResultStringList = new ArrayList<>();
        String[][] queryResultString2dArray = null;
        if(dbConnection == null)
            return null;
        try
        {
            Statement updateStatement = dbConnection.createStatement();
            updateStatement.setQueryTimeout(30);

            queryResultSet = updateStatement.executeQuery(statement);

            int numberOfColumns = queryResultSet.getMetaData().getColumnCount();

            for(int i = 0; queryResultSet.next(); i++)
            {
                queryResultStringList.add(new String[numberOfColumns]);
                for(int j = 0; j < numberOfColumns; j++)
                {
                    queryResultStringList.get(i)[j] = queryResultSet.getString(j + 1);
                }
            }

            int numberOfRows = queryResultStringList.size();

            queryResultString2dArray = new String[numberOfRows][numberOfColumns];

            for(int i = 0; i < numberOfRows; i++)
            {
                for(int j = 0; j < numberOfColumns; j++)
                {
                    queryResultString2dArray[i][j] = queryResultStringList.get(i)[j];
                }
            }

            updateStatement.close();
        }
        catch (Exception e)
        {
            LogManager.updateServerMainLog("Database threw an exception while executing the following statement: " + statement);
            //System.out.println(e);
            return null;
        }
        return queryResultString2dArray;
    }
}
