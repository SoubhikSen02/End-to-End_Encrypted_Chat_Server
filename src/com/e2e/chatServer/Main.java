package com.e2e.chatServer;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatArcDarkIJTheme;

import javax.swing.*;

public class Main {
    public static void main(String[] args)
    {
        boolean logInitializationSuccess = LogManager.initializeLog();
        boolean configInitializationSuccess = ConfigManager.initializeConfig();
        boolean databaseInitializationSuccess = DatabaseManager.initializeDB();

        FlatArcDarkIJTheme.setup();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new MainFrame(logInitializationSuccess, configInitializationSuccess, databaseInitializationSuccess);
            }
        });


        //should be called by start button in GUI, should be called by a SwingWorker thread, not by GUI main thread,
        //this method may take some time processing, so calling with main thread will completely freeze the GUI
        //ConnectionManager.initializeNetwork();

        //should be called by stop button in GUI, should be called by a SwingWorker thread, not by GUI main thread,
        //this method may take some time processing, so calling with main thread will completely freeze the GUI
        //ConnectionManager.stopAllNetworkConnections();


//        TODO: Refer to this list below for detailed descriptions of which functions are exposed by the backend for frontend use
//        1) ConnectionManager.initializeNetwork() -
//        Uses - Call for starting the server backend which accepts incoming connections, communicates with them and so on
//               Note that the server config, database and log are initialized by default.
//               This method simply initializes the network component.
//        Parameters - None required
//        Returns - Nothing
//
//        2) ConnectionManager.stopAllNetworkConnections() -
//        Uses - Call for stopping the server backend which accepts incoming connections and communicates with them.
//               This method basically shuts down the network component of the server. Must call before app exit.
//               Note that the server config, database and log still run as normal.
//        Parameters - None required
//        Returns - Nothing
//
//        3) ConnectionManager.addNumberOfConnectedUsersChangedActionListener(ActionListener listener) -
//        Uses - Call for adding an action listener to current connected users count.
//               When the current active user count changes, this added action listener will be called.
//        Parameters - a) listener - The action listener that is to be called when active user count changes.
//        Returns - Nothing
//
//        4) ConnectionManager.removeNumberOfConnectedUsersChangedActionListener(ActionListener listener) -
//        Uses - Call for removing an action listener from current connected users count.
//               When the current active user count changes, this given action listener will no longer be called.
//        Parameters - a) listener - The action listener that is now not to be called when active user count changes.
//        Returns - Nothing
//
//        5) ConfigManager.getServerPort() -
//        Uses - Call for getting the current port number on which the server accepts new incoming connections.
//        Parameters - None required
//        Returns - Current server port number
//
//        6) ConfigManager.getServerVersion() -
//        Uses - Call for getting the current server version number.
//        Parameters - None required
//        Returns - Current server version
//
//        7) ConfigManager.getClientTimeoutInSeconds() -
//        Uses - Call for getting the timeout period at which the server disconnects from client if no communications
//               happens in between this given period.
//        Parameters - None required
//        Returns - Timeout period in seconds
//
//        8) ConfigManager.getAppFolderPath() -
//        Uses - Call for getting the path to the folder in which the server stores its config, database and logs.
//        Parameters - None required
//        Returns - Server data folder path
//
//        9) ConfigManager.setClientTimeoutInSeconds(String clientTimeoutInSeconds) -
//        Uses - Call for updating the network timeout period with a new value. The new value is updated in the
//               stored config file on disk immediately on calling this function.
//        Parameters - a) clientTimeoutInSeconds - The new timeout period in seconds.
//        Returns - Nothing
//
//        10) ConfigManager.setServerPort(String serverPort) -
//        Uses - Call for updating the new connection accepting server port number with a new value. The new value is updated
//               in the stored config file on disk immediately on calling this function.
//        Parameters - a) serverPort - The new server port number.
//        Returns - Nothing
//
//        11) LogManager.addServerMainLogUpdatedListener(ActionListener listener) -
//        Uses - Call for adding an action listener to server main log updates notification.
//               When the server main log is updated, this added action listener be be called.
//               When the action listener is called, it is passed an action event containing the new updated value.
//               If the action event parameter is called e, then the new value can be obtained by doing e.getSource();
//        Parameters - a) listener - The action listener that is to be called when server main log updates.
//        Returns - Nothing
//
//        12) LogManager.removeServerMainLogUpdatedListener(ActionListener listener) -
//        Uses - Call for removing an action listener from server main log updates notification.
//               When the server main log is updated, this given action listener will no longer be called.
//        Parameters - a) listener - The action listener that is now not to be called when server main log updates.
//        Returns - Nothing
//
//        13) LogManager.addParticularUserLogUpdatedListener(String userAccountID, ActionListener listener) -
//        Uses - Call for adding an action listener to some particular user's log updates notification.
//               When that user's log is updated, this added action listener be be called.
//               When the action listener is called, it is passed a null action event parameter.
//        Parameters - a) userAccountID - The account ID of the user for which the action listener is to be called.
//                     b) listener - The action listener that is to be called when the user's log updates.
//        Returns - Nothing
//
//        14) LogManager.removeParticularUserLogUpdatedListener(String userAccountID, ActionListener listener) -
//        Uses - Call for removing an action listener from some particular user's log updates notification.
//               When that user's log is updated, this given action listener will no longer be called.
//        Parameters - a) userAccountID - The account ID of the user for which the action listener is not to be called anymore.
//                     b) listener - The action listener that is now not to be called when the user's log updates.
//        Returns - Nothing
//
//        14) DatabaseManager.closeDB() -
//        Uses - Call for closing database connection. Must call before app exit.
//        Parameters - None
//        Returns - Nothing
//
    }
}