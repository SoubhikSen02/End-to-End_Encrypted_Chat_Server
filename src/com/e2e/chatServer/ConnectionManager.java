package com.e2e.chatServer;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ConnectionManager
{
    private static ServerSocket connectionAcceptingSocket = null;
    public static ServerSocket getConnectionAcceptingSocket()
    {
        return connectionAcceptingSocket;
    }
    private static Thread connectionAcceptingThread = null;

    //private static long numberOfActiveConnections = 0;
    private static ArrayList<ClientConnection> activeConnectionsList = new ArrayList<>();
    private static Timer activeConnectionsChecker = null;


    //Action listeners
    private static ArrayList<ActionListener> numberOfConnectedUsersChangedListeners = new ArrayList<>();



    synchronized public static void initializeNetwork()
    {
        try
        {
            connectionAcceptingSocket = new ServerSocket(Integer.parseInt(ConfigManager.getServerPort()));
            LogManager.updateServerMainLog("Server Socket opened on port " + connectionAcceptingSocket.getLocalPort() + ".");
            connectionAcceptingThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    LogManager.updateServerMainLog("Client connection accepting thread started.");
                    while(true)
                    {
                        if(connectionAcceptingSocket == null) {
                            LogManager.updateServerMainLog("Server Socket is not available. Closing client connection accepting thread.");
                            break;
                        }
                        Socket newConnection = null;
                        try
                        {
                            newConnection = connectionAcceptingSocket.accept();
                            ClientConnection newClientConnectionThread = new ClientConnection(newConnection);
                            activeConnectionsList.add(newClientConnectionThread);
                            fireNumberOfConnectedUsersChangedListeners();
                            if(newConnection != null)
                                LogManager.updateServerMainLog("New client connection accepted from IP " + newConnection.getInetAddress().getHostAddress() + " and port " + newConnection.getPort() + ".");
                        }
                        catch (Exception e)
                        {
                            if(newConnection != null) {
                                try {
                                    newConnection.close();
                                }
                                catch (Exception e2)
                                {
                                    LogManager.updateServerMainLog("Client connection accepting thread failed to close an open erroneous connection.");
                                }
                            }
                            LogManager.updateServerMainLog("Client connection accepting thread threw an exception while accepting a new connection and creating its connection object.");
                        }
                    }
                    LogManager.updateServerMainLog("Client connection accepting thread stopped.");
                }
            });
            connectionAcceptingThread.start();

            activeConnectionsChecker = new Timer();
            activeConnectionsChecker.schedule(new TimerTask() {
                @Override
                public void run() {
                    cleanUpActiveConnectionsList();
                }
            }, 60000, 60000);
            LogManager.updateServerMainLog("Client connection checking periodic timer started.");
        }
        catch(Exception e)
        {
            LogManager.updateServerMainLog("Exception occurred while trying to initialize network.");
        }
    }

    synchronized public static void stopAllNetworkConnections()
    {
        connectionAcceptingThread.interrupt();
        if(connectionAcceptingThread.isAlive())
            connectionAcceptingThread.interrupt();
        connectionAcceptingThread = null;
        LogManager.updateServerMainLog("Client connection accepting thread interrupted.");

        try {
            connectionAcceptingSocket.close();
            LogManager.updateServerMainLog("Client connection accepting socket closed.");
        }
        catch (Exception e)
        {
            LogManager.updateServerMainLog("Exception occurred while trying to close client connection accepting socket.");
        }
        connectionAcceptingSocket = null;

        activeConnectionsChecker.cancel();
        activeConnectionsChecker = null;
        LogManager.updateServerMainLog("Client connection checking periodic timer stopped.");

        while(!activeConnectionsList.isEmpty())
        {
            ClientConnection connection = activeConnectionsList.get(0);
            connection.closeConnectionOnTimeout();
            activeConnectionsList.remove(0);
        }
        LogManager.updateServerMainLog("All client connections halted.");
        fireNumberOfConnectedUsersChangedListeners();
        LogManager.updateServerMainLog("Server network component successfully stopped.");
    }

    synchronized public static void checkAndCloseOtherClientConnectionsWithSameUserID(String userID)
    {
        for(int i = activeConnectionsList.size() - 1; i >= 0; i--)
        {
            if(activeConnectionsList.get(i).getClientLoggedInUserID().equals(userID))
            {
                activeConnectionsList.get(i).closeConnectionOnTimeout();
                activeConnectionsList.remove(i);
            }
        }
        fireNumberOfConnectedUsersChangedListeners();
    }

    synchronized private static void cleanUpActiveConnectionsList()
    {
        for(int i = activeConnectionsList.size() - 1; i >= 0; i--)
        {
            Socket clientSocket = activeConnectionsList.get(i).getClientSocket();
            if(!clientSocket.isConnected() || clientSocket.isClosed())
            {
                activeConnectionsList.remove(i);
                fireNumberOfConnectedUsersChangedListeners();
                continue;
            }
            if(!activeConnectionsList.get(i).isAlive())
            {
                activeConnectionsList.get(i).closeConnectionOnTimeout();
                activeConnectionsList.remove(i);
                fireNumberOfConnectedUsersChangedListeners();
            }
        }
    }

    synchronized public static void addNumberOfConnectedUsersChangedActionListener(ActionListener listener)
    {
        numberOfConnectedUsersChangedListeners.add(listener);
        LogManager.updateServerMainLog("Action listener added to connected user count.");
    }

    synchronized public static void removeNumberOfConnectedUsersChangedActionListener(ActionListener listener)
    {
        numberOfConnectedUsersChangedListeners.remove(listener);
        LogManager.updateServerMainLog("Action listener removed from connected user count.");
    }

    synchronized private static void fireNumberOfConnectedUsersChangedListeners()
    {
        for(int i = 0; i < numberOfConnectedUsersChangedListeners.size(); i++)
        {
            numberOfConnectedUsersChangedListeners.get(i).actionPerformed(new ActionEvent(Integer.valueOf(activeConnectionsList.size()), activeConnectionsList.size(), null));
        }
    }

    synchronized public static void removeAllNumberOfConnectedUsersChangedActionListeners()
    {
        numberOfConnectedUsersChangedListeners.clear();
        LogManager.updateServerMainLog("All action listeners removed from connected user count.");
    }
}
