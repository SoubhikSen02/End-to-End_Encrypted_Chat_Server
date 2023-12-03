package com.e2e.chatServer;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.PrintStream;
import java.net.Socket;
import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.Scanner;

public class ClientConnection extends Thread
{
    private Socket clientSocket = null;
    private PrintStream outToClient = null;
    private Scanner inFromClient = null;


    private Timer connectionTimeoutTimer = null;
    private boolean connected = false;
    //private boolean registeredUserLoggedIn = false;
    private String clientLoggedInUserID = "0000000000000000";


    public String getClientLoggedInUserID()
    {
        return clientLoggedInUserID;
    }

    public ClientConnection(Socket socket) throws IOException
    {
        super();
        clientSocket = socket;
        outToClient = new PrintStream(clientSocket.getOutputStream());
        inFromClient = new Scanner(clientSocket.getInputStream());
        connected = true;

        connectionTimeoutTimer = new Timer(Integer.parseInt(ConfigManager.getClientTimeoutInSeconds()) * 1000,
                new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeConnectionOnTimeout();
            }
        });

        start();
        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Connection successfully initialised.", clientLoggedInUserID);
    }

    @Override
    public void run()
    {
        if(clientSocket != null)
            LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Communication thread started.", clientLoggedInUserID);
        while(true)
        {
            if(!connected)
                break;
            String reply = receiveFromClient();
            if(reply == null)
                break;
            if(reply.equals("PROBE"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to probe server.", clientLoggedInUserID);
                boolean success = replyToProbe();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Successfully replied to client probe.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Failed to reply to client probe.", clientLoggedInUserID);
                }
            }
            else if(reply.contains("USRAVAIL"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to check availability of an username.", clientLoggedInUserID);
                String extractedUsername;
                try {
                    extractedUsername = reply.substring(9);
                }
                catch (Exception e)
                {
                    boolean success = sendToClient("ERROR");
                    if(clientSocket != null)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting username from username availability query.", clientLoggedInUserID);
                    continue;
                }
                boolean success = replyToUsernameAvailabilityQuery(extractedUsername);
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully notified that the username '" + extractedUsername + "' is available.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully notified that the username '" + extractedUsername + "' is not available.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("RGSTUSER"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to register a new user.", clientLoggedInUserID);
                boolean success = replyToUserRegistrationRequest();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully allowed to register the new user.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was disallowed to register the new user.", clientLoggedInUserID);
                }
            }
            else if(reply.contains("GETSECQS"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to retrieve security questions of an user.", clientLoggedInUserID);
                String extractedUsername;
                try {
                    extractedUsername = reply.substring(9);
                }
                catch (Exception e)
                {
                    boolean success = sendToClient("ERROR");
                    if(clientSocket != null)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting username from retrieve security questions request.", clientLoggedInUserID);
                    continue;
                }
                boolean success = replyToSecurityQuestionRetrieveRequest(extractedUsername);
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully sent the security questions for username '" + extractedUsername + "'.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Failed to send security questions for username '" + extractedUsername + "'.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("CHKSCANS"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to check security question answers.", clientLoggedInUserID);
                boolean success = replyToSecurityAnswerCheckingRequest();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully notified that the given answer is correct.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully notified that the given answer is wrong.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("RESETPW"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to reset password of an user.", clientLoggedInUserID);
                boolean success = replyToResetPasswordCommand();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was allowed to reset the password.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was disallowed from resetting the password.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("LOGINNEW"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to login to a new session with an user.", clientLoggedInUserID);
                boolean success = replyToLoginToNewSession();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client has successfully logged in.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client failed to log in.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("LOGINOLD"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to login to an old session with an user.", clientLoggedInUserID);
                boolean success = replyToLoginToOldSession();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client has successfully logged in.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client failed to log in.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("RELOGTMP"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to login to a temporary session with an user.", clientLoggedInUserID);
                boolean success = replyToReLoginToTemporarySession();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client has successfully logged in.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client failed to log in.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("LOGOUT"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to logout with the current user.", clientLoggedInUserID);
                boolean success = replyToLogoutUserRequest();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client has successfully logged out.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client failed to logout.", clientLoggedInUserID);
                }
            }
            else if(reply.contains("CHKACCID"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to check whether an account ID is valid or not.", clientLoggedInUserID);
                String extractedUserID;
                try {
                    extractedUserID = reply.substring(9);
                }
                catch (Exception e)
                {
                    boolean success = sendToClient("ERROR");
                    if(clientSocket != null)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting account ID from ID validity check query.", clientLoggedInUserID);
                    continue;
                }
                boolean success = replyToAccountIdValidityCheck(extractedUserID);
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully notified that the account ID '" + extractedUserID + "' is valid.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully notified that the account ID '" + extractedUserID + "' is invalid.", clientLoggedInUserID);
                }
            }
            else if(reply.contains("SETNPCAL"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to update new personal chat allowed setting.", clientLoggedInUserID);
                boolean extractedValue;
                try {
                    extractedValue = Boolean.parseBoolean(reply.substring(9));
                }
                catch (Exception e)
                {
                    boolean success = sendToClient("ERROR");
                    if(clientSocket != null)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting new setting value from personal chat allow setting update request.", clientLoggedInUserID);
                    continue;
                }
                boolean success = replyToAllowPersonalChatAccountSettingUpdate(extractedValue);
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Setting was successfully updated to '" + extractedValue + "'.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Failed to update setting.", clientLoggedInUserID);
                }
            }
            else if(reply.contains("SETNGCAL"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to update new group chat allowed setting.", clientLoggedInUserID);
                boolean extractedValue;
                try {
                    extractedValue = Boolean.parseBoolean(reply.substring(9));
                }
                catch (Exception e)
                {
                    boolean success = sendToClient("ERROR");
                    if(clientSocket != null)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting new setting value from group chat allow setting update request.", clientLoggedInUserID);
                    continue;
                }
                boolean success = replyToAllowGroupChatAccountSettingUpdate(extractedValue);
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Setting was successfully updated to '" + extractedValue + "'.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Failed to update setting.", clientLoggedInUserID);
                }
            }
            else if(reply.contains("CHKNPCAL"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to check whether an user allows new personal chats.", clientLoggedInUserID);
                String extractedUserID;
                try {
                    extractedUserID = reply.substring(9);
                }
                catch (Exception e)
                {
                    boolean success = sendToClient("ERROR");
                    if(clientSocket != null)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting account ID from personal chat allow setting check query.", clientLoggedInUserID);
                    continue;
                }
                boolean success = replyToPersonalChatAllowedCheck(extractedUserID);
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully notified that account ID '" + extractedUserID + "' allows new personal chats.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully notified that account ID '" + extractedUserID + "' does not allow new personal chats.", clientLoggedInUserID);
                }
            }
            else if(reply.contains("CHKNGCAL"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to check whether an user allows new group chats.", clientLoggedInUserID);
                String extractedUserID;
                try {
                    extractedUserID = reply.substring(9);
                }
                catch (Exception e)
                {
                    boolean success = sendToClient("ERROR");
                    if(clientSocket != null)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting account ID from group chat allow setting check query.", clientLoggedInUserID);
                    continue;
                }
                boolean success = replyToGroupChatAllowedCheck(extractedUserID);
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully notified that account ID '" + extractedUserID + "' allows new group chats.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully notified that account ID '" + extractedUserID + "' does not allow new group chats.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("CHNGPW"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to change the password of an user.", clientLoggedInUserID);
                boolean success = replyToChangePasswordRequest();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully able to change password.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client failed to change password.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("CHGDSPNM"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to change the display name of an user.", clientLoggedInUserID);
                boolean success = replyToChangeDisplayNameRequest();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully able to change display name.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client failed to change display name.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("CREATPCH"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to create a new personal chat with an user.", clientLoggedInUserID);
                boolean success = replyToNewPersonalChatCreateRequest();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully able to create the new personal chat.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client failed to create the new chat.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("CHKNEWCH"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to receive pending new chats for an user.", clientLoggedInUserID);
                boolean success = replyToNewPersonalChatCheckingRequest();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully able to retrieve new chats.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client failed to retrieve any new chats.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("SENDMSG"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to send a new message to a chat.", clientLoggedInUserID);
                boolean success = replyToSendNewMessageRequest();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully able to send the message.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client failed to send the message.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("CHKNWMSG"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to receive pending new messages for an user.", clientLoggedInUserID);
                boolean success = replyToNewMessageCheckingRequest();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully able to retrieve new messages.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client failed to retrieve any new messages.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("SENDNRDR"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to send a new read receipt for a message.", clientLoggedInUserID);
                boolean success = replyToSendNewReadReceiptRequest();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully able to send the read receipt.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client failed to send the read receipt.", clientLoggedInUserID);
                }
            }
            else if(reply.equals("CHKNWRDR"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is trying to receive pending new read receipts for an user.", clientLoggedInUserID);
                boolean success = replyToNewReadReceiptCheckingRequest();
                if(clientSocket != null) {
                    if(success)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client was successfully able to retrieve new read receipts.", clientLoggedInUserID);
                    else
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client failed to retrieve any new read receipts.", clientLoggedInUserID);
                }
            }
            else
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client-Server synchronization issue detected.", clientLoggedInUserID);
                closeConnectionOnTimeout();
            }
        }
        if(clientSocket != null)
            LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Communication thread stopped.", clientLoggedInUserID);
    }

    synchronized public void closeConnectionOnTimeout()
    {
        if(clientSocket != null)
            LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Trying to close client connection.", clientLoggedInUserID);
        if(clientSocket != null)
        {
            try
            {
                clientSocket.close();
                connected = false;
                outToClient = null;
                inFromClient = null;
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client connection socket successfully closed.", clientLoggedInUserID);

                if(!clientLoggedInUserID.equals("0000000000000000"))
                {
                    boolean success = DatabaseManager.makeUpdate("update accounts set online = 0 where account_id = '" + clientLoggedInUserID + "';");
                    if(!success && clientSocket != null)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database faced an error while updating logged in user's status to offline before client connection component is closed.", clientLoggedInUserID);
                }
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> User status successfully updated to offline.", clientLoggedInUserID);
                clientLoggedInUserID = "0000000000000000";

                connectionTimeoutTimer.stop();
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client connection successfully closed.", clientLoggedInUserID);
            }
            catch (Exception e)
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while trying to close client connection.", clientLoggedInUserID);
            }
        }
    }

    private boolean sendToClient(String message)
    {
        if(!connected) {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Failed to send message to client.", clientLoggedInUserID);
            return false;
        }

        try {
            outToClient.writeBytes((message + "\n").getBytes());
        }
        catch(Exception e)
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while trying to send message to client.", clientLoggedInUserID);
            return false;
        }

        connectionTimeoutTimer.restart();
        return true;
    }

    private String receiveFromClient()
    {
        if(!connected) {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Failed to receive message from client.", clientLoggedInUserID);
            return null;
        }

        String receivedMessage = null;

        try {
            receivedMessage = inFromClient.nextLine();
        }
        catch (Exception e)
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while trying to receive message from client.", clientLoggedInUserID);
            return null;
        }

        connectionTimeoutTimer.restart();
        return receivedMessage;
    }

    private boolean replyToProbe()
    {
        boolean success = sendToClient("OK");
        return success;
    }

    private boolean replyToUsernameAvailabilityQuery(String extractedUsername)
    {
        String[][] numberOfUsersWithSameName = DatabaseManager.makeQuery("select count(*) from accounts where username = '" + extractedUsername + "';");
        if(numberOfUsersWithSameName != null && Integer.parseInt(numberOfUsersWithSameName[0][0]) == 0)
        {
            boolean success = sendToClient("OK");
            return success;
        }
        else
        {
            boolean success = sendToClient("ERROR");
            return success;
        }
    }

    private boolean replyToUserRegistrationRequest()
    {
        boolean success = sendToClient("OK");
        if(!success)
            return false;



        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("USERNAME"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String username;
        try {
            username = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(username.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("PASSWORD"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String password;
        try {
            password = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(password.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("DSPLNAME"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String displayName;
        try {
            displayName = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(displayName.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        String[] securityQuestions = new String[]{null, null, null};
        String[] securityAnswers = new String[]{null, null, null};
        for(int i = 0; i < 3; i++)
        {
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(i == 0 && !reply.contains("SECQS"))
            {
                success = sendToClient("ERROR");
                if(!success)
                    return false;
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }
            if(!reply.contains("SECQS"))
            {
                break;
            }
            try {
                securityQuestions[i] = reply.substring(6);
            }
            catch(Exception e)
            {
                success = sendToClient("ERROR");
                if(!success)
                    return false;
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
                return false;
            }
            if(securityQuestions[i].isEmpty())
            {
                success = sendToClient("ERROR");
                if(!success)
                    return false;
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }
            success = sendToClient("OK");
            if(!success)
                return false;



            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.contains("SECANS"))
            {
                success = sendToClient("ERROR");
                if(!success)
                    return false;
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }
            try {
                securityAnswers[i] = reply.substring(7);
            }
            catch(Exception e)
            {
                success = sendToClient("ERROR");
                if(!success)
                    return false;
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
                return false;
            }
            if(securityAnswers[i].isEmpty())
            {
                success = sendToClient("ERROR");
                if(!success)
                    return false;
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }
            success = sendToClient("OK");
            if(!success)
                return false;
        }



        if(!reply.equals("DONE"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        String[][] highestAccountID = DatabaseManager.makeQuery("select max(account_id) from accounts;");
        if(highestAccountID == null || highestAccountID.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        String accountID = String.format("%016d", (Long.parseLong(highestAccountID[0][0]) + 1));
        success = DatabaseManager.makeUpdate("insert into accounts values('" + accountID + "', '" + username + "', '" + password + "', 0, 0, 'undefined', '" + displayName + "', 1, 1);");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        for(int i = 0; i < 3 && securityQuestions[i] != null; i++)
        {
            success = DatabaseManager.makeUpdate("insert into securityQuestions values('" + accountID + "', '" + securityQuestions[i] + "', '" + securityAnswers[i] + "');");
            if(!success)
            {
                success = sendToClient("ERROR");
                if(!success)
                    return false;
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
                return false;
            }
        }



        success = sendToClient("OK");
        if(!success)
            return false;
        return true;
    }

    private boolean replyToSecurityQuestionRetrieveRequest(String username)
    {
        String[][] userAccountID = DatabaseManager.makeQuery("select account_id from accounts where username = '" + username + "';");
        if(userAccountID == null || userAccountID.length == 0)
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        String[][] securityQuestions = DatabaseManager.makeQuery("select question from securityQuestions where account_id = '" + userAccountID[0][0] + "';");
        if(securityQuestions == null || securityQuestions.length == 0)
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }


        int numberOfQuestions = securityQuestions.length;
        boolean success = sendToClient("NUMBER " + numberOfQuestions);
        if(!success)
            return false;
        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }


        for(int i = 0; i < securityQuestions.length; i++)
        {
            success = sendToClient("SECQS" + (i + 1) + " " + securityQuestions[i][0]);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }
        }


        success = sendToClient("DONE");
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        return true;
    }

    private boolean replyToSecurityAnswerCheckingRequest()
    {
        boolean success = sendToClient("OK");
        if(!success)
            return false;



        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("USERNAME"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String username;
        try {
            username = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(username.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("QUESTION"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String question;
        try {
            question = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(question.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("ANSWER"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String answer;
        try {
            answer = reply.substring(7);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(answer.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("DONE"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        String[][] userAccountID = DatabaseManager.makeQuery("select account_id from accounts where username = '" + username + "';");
        if(userAccountID == null || userAccountID.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        String[][] correctAnswer = DatabaseManager.makeQuery("select answer from securityQuestions where account_id = '" + userAccountID[0][0] + "' and question = '" + question + "';");
        if(correctAnswer == null || correctAnswer.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        if(!answer.equals(correctAnswer[0][0]))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            return false;
        }



        success = sendToClient("OK");
        if(!success)
            return false;
        return true;
    }

    private boolean replyToResetPasswordCommand()
    {
        boolean success = sendToClient("OK");
        if(!success)
            return false;



        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("USERNAME"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String username;
        try {
            username = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(username.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("PASSWORD"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String password;
        try {
            password = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(password.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("DONE"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        success = DatabaseManager.makeUpdate("update accounts set password = '" + password + "' where username = '" + username + "';");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }



        success = sendToClient("OK");
        if(!success)
            return false;
        return true;
    }

    private boolean replyToLoginToNewSession()
    {
        if(!clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client tried to login again without logging out.", clientLoggedInUserID);
            return false;
        }


        boolean success = sendToClient("OK");
        if(!success)
            return false;



        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("USERNAME"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String username;
        try {
            username = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(username.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("PASSWORD"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String password;
        try {
            password = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(password.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("STAYLGIN"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        boolean stayLoggedIn;
        try {
            stayLoggedIn = Boolean.parseBoolean(reply.substring(9));
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;


        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("DONE"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        String[][] userDetails = DatabaseManager.makeQuery("select * from accounts where username = '" + username + "';");
        if(userDetails == null || userDetails.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        if(!password.equals(userDetails[0][2]))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client provided incorrect password.", clientLoggedInUserID);
            return false;
        }
        String userAccountID = userDetails[0][0];
        String displayName = userDetails[0][6];
        boolean allowNewPersonalChat = (Integer.parseInt(userDetails[0][7]) == 1);
        boolean allowNewGroupChat = (Integer.parseInt(userDetails[0][8]) == 1);
        ConnectionManager.checkAndCloseOtherClientConnectionsWithSameUserID(userAccountID);
        String sessionID = "";
        if(stayLoggedIn)
        {
            Random randomGenerator = new Random();
            for(int i = 0; i < 16; i++)
            {
                sessionID = sessionID + (char)(randomGenerator.nextBoolean()? randomGenerator.nextInt(65, 91) : (randomGenerator.nextBoolean()? randomGenerator.nextInt(97, 123) : randomGenerator.nextInt(48, 58)));
            }
        }
        else
        {
            sessionID = "null";
        }


        success = sendToClient("ACCID " + userAccountID);
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        success = sendToClient("USERNAME " + username);
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        success = sendToClient("DSPLNAME " + displayName);
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        success = sendToClient("ALLOWNPC " + allowNewPersonalChat);
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        success = sendToClient("ALLOWNGC " + allowNewGroupChat);
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        success = sendToClient("SESIONID " + sessionID);
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        success = sendToClient("DONE");
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        success = DatabaseManager.makeUpdate("update accounts set online = 1, permanent_session = " + (sessionID.equals("null")? 0:1) + ", session_id = '" + (sessionID.equals("null")? "undefined":sessionID) + "' where account_id = '" + userAccountID + "';");
        if(!success && clientSocket != null)
            LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database faced an error while updating user statuses such as online, session info, etc., after user logged in to a new session.", clientLoggedInUserID);
        clientLoggedInUserID = userAccountID;
        return true;
    }

    private boolean replyToLoginToOldSession()
    {
        if(!clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client tried to login again without logging out.", clientLoggedInUserID);
            return false;
        }


        boolean success = sendToClient("OK");
        if(!success)
            return false;



        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("ACCID"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String userAccountID;
        try {
            userAccountID = reply.substring(6);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(userAccountID.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("SESIONID"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String sessionID;
        try {
            sessionID = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(sessionID.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("DONE"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }


        String[][] userDetails = DatabaseManager.makeQuery("select * from accounts where account_id = '" + userAccountID + "';");
        if(userDetails == null || userDetails.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        if(!userDetails[0][4].equals("1") || !sessionID.equals(userDetails[0][5]))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client provided incorrect session info.", clientLoggedInUserID);
            return false;
        }
        ConnectionManager.checkAndCloseOtherClientConnectionsWithSameUserID(userAccountID);



        success = sendToClient("OK");
        if(!success)
            return false;
        success = DatabaseManager.makeUpdate("update accounts set online = 1 where account_id = '" + userAccountID + "';");
        if(!success && clientSocket != null)
            LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database faced an error while updating user's status to online after user logged back in to an old session.", clientLoggedInUserID);
        clientLoggedInUserID = userAccountID;
        return true;
    }

    private boolean replyToReLoginToTemporarySession()
    {
        if(!clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client tried to login again without logging out.", clientLoggedInUserID);
            return false;
        }


        boolean success = sendToClient("OK");
        if(!success)
            return false;



        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("ACCID"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String userAccountID;
        try {
            userAccountID = reply.substring(6);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(userAccountID.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("DONE"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        String[][] userDetails = DatabaseManager.makeQuery("select * from accounts where account_id = '" + userAccountID + "';");
        if(userDetails == null || userDetails.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        if(!userDetails[0][4].equals("0") || !userDetails[0][5].equals("undefined"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client provided incorrect session info.", clientLoggedInUserID);
            return false;
        }
        ConnectionManager.checkAndCloseOtherClientConnectionsWithSameUserID(userAccountID);



        success = sendToClient("OK");
        if(!success)
            return false;
        success = DatabaseManager.makeUpdate("update accounts set online = 1 where account_id = '" + userAccountID + "';");
        if(!success && clientSocket != null)
            LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database faced an error while updating user's status to online after the user logged back in to a temporary session.", clientLoggedInUserID);
        clientLoggedInUserID = userAccountID;
        return true;
    }

    private boolean replyToLogoutUserRequest()
    {
        if(clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client tried to logout when it was not even logged in.", clientLoggedInUserID);
            return false;
        }


        boolean success = sendToClient("OK");
        if(!success)
            return false;



        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("DONE"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        String[][] userDetails = DatabaseManager.makeQuery("select * from accounts where account_id = '" + clientLoggedInUserID + "';");
        if(userDetails == null || userDetails.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        if(!userDetails[0][3].equals("1"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }



        success = sendToClient("OK");
        if(!success)
            return false;
        success = DatabaseManager.makeUpdate("update accounts set online = 0, permanent_session = 0, session_id = 'undefined' where account_id = '" + clientLoggedInUserID + "';");
        if(!success && clientSocket != null)
            LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database faced an error while updating logged in user statuses such as online, session info, etc., after user logged out of a session.", clientLoggedInUserID);
        clientLoggedInUserID = "0000000000000000";
        return true;
    }

    private boolean replyToAccountIdValidityCheck(String userAccountID)
    {
        if(userAccountID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client tried to check validity of default user.", clientLoggedInUserID);
            return false;
        }
        String[][] userIdExists = DatabaseManager.makeQuery("select count(*) from accounts where account_id = '" + userAccountID + "';");
        if(userIdExists == null || userIdExists.length == 0 || Integer.parseInt(userIdExists[0][0]) == 0)
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results or could not find the given user.", clientLoggedInUserID);
            return false;
        }
        boolean success = sendToClient("OK");
        if(!success)
            return false;
        return true;
    }

    private boolean replyToAllowPersonalChatAccountSettingUpdate(boolean newValue)
    {
        if(clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is not logged in to any user.", clientLoggedInUserID);
            return false;
        }

        boolean success = DatabaseManager.makeUpdate("update accounts set allow_new_personal_chat = " + (newValue? 1:0) + " where account_id = '" + clientLoggedInUserID + "';");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }

        success = sendToClient("OK");
        if(!success)
            return false;
        return true;
    }

    private boolean replyToAllowGroupChatAccountSettingUpdate(boolean newValue)
    {
        if(clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is not logged in to any user.", clientLoggedInUserID);
            return false;
        }

        boolean success = DatabaseManager.makeUpdate("update accounts set allow_new_group_chat = " + (newValue? 1:0) + " where account_id = '" + clientLoggedInUserID + "';");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }

        success = sendToClient("OK");
        if(!success)
            return false;
        return true;
    }

    private boolean replyToPersonalChatAllowedCheck(String userAccountID)
    {
        if(userAccountID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is not logged in to any user.", clientLoggedInUserID);
            return false;
        }
        String[][] newPersonalChatAllowedStatus = DatabaseManager.makeQuery("select allow_new_personal_chat from accounts where account_id = '" + userAccountID + "';");
        if(newPersonalChatAllowedStatus == null || newPersonalChatAllowedStatus.length == 0)
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }

        if(Integer.parseInt(newPersonalChatAllowedStatus[0][0]) == 0)
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            return false;
        }

        boolean success = sendToClient("OK");
        if(!success)
            return false;
        return true;
    }

    private boolean replyToGroupChatAllowedCheck(String userAccountID)
    {
        if(userAccountID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is not logged in to any user.", clientLoggedInUserID);
            return false;
        }
        String[][] newGroupChatAllowedStatus = DatabaseManager.makeQuery("select allow_new_group_chat from accounts where account_id = '" + userAccountID + "';");
        if(newGroupChatAllowedStatus == null || newGroupChatAllowedStatus.length == 0)
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }

        if(Integer.parseInt(newGroupChatAllowedStatus[0][0]) == 0)
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            return false;
        }

        boolean success = sendToClient("OK");
        if(!success)
            return false;
        return true;
    }

    private boolean replyToChangePasswordRequest()
    {
        if(clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is not logged in to any user.", clientLoggedInUserID);
            return false;
        }


        boolean success = sendToClient("OK");
        if(!success)
            return false;



        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("OLDPW"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String oldPassword;
        try {
            oldPassword = reply.substring(6);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(oldPassword.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("NEWPW"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String newPassword;
        try {
            newPassword = reply.substring(6);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(newPassword.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        String[][] userDetails = DatabaseManager.makeQuery("select * from accounts where account_id = '" + clientLoggedInUserID + "';");
        if(userDetails == null || userDetails.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        if(!oldPassword.equals(userDetails[0][2]))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client provided wrong old password.", clientLoggedInUserID);
            return false;
        }
        success = DatabaseManager.makeUpdate("update accounts set password = '" + newPassword + "' where account_id = '" + clientLoggedInUserID + "';");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }


        success = sendToClient("OK");
        if(!success)
            return false;
        return true;
    }

    private boolean replyToChangeDisplayNameRequest()
    {
        if(clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is not logged in to any user.", clientLoggedInUserID);
            return false;
        }


        boolean success = sendToClient("OK");
        if(!success)
            return false;



        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("NEWDSPNM"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String newDisplayName;
        try {
            newDisplayName = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(newDisplayName.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        success = DatabaseManager.makeUpdate("update accounts set display_name = '" + newDisplayName + "' where account_id = '" + clientLoggedInUserID + "';");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }


        success = sendToClient("OK");
        if(!success)
            return false;
        return true;
    }

    private boolean replyToNewPersonalChatCreateRequest()
    {
        if(clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is not logged in to any user.", clientLoggedInUserID);
            return false;
        }


        boolean success = sendToClient("OK");
        if(!success)
            return false;



        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("CHATTYPE"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String chatType;
        try {
            chatType = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(chatType.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;



        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("PPLACCID"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String otherParticipant;
        try {
            otherParticipant = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(otherParticipant.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;


        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("DONE"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }


        if(!chatType.equals("PERSONAL"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String[][] participantInfo = DatabaseManager.makeQuery("select * from accounts where account_id = '" + otherParticipant + "';");
        if(participantInfo == null || participantInfo.length == 0 || Integer.parseInt(participantInfo[0][7]) == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        String[][] highestChatID = DatabaseManager.makeQuery("select max(chat_id) from chat");
        if(highestChatID == null)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        Long chatID;
        if(highestChatID.length == 0 || highestChatID[0][0] == null)
        {
            chatID = 1L;
        }
        else
        {
            chatID = Long.parseLong(highestChatID[0][0]) + 1;
        }
        String chatName = participantInfo[0][6];
        String participantList = clientLoggedInUserID + "," + otherParticipant;

        success = sendToClient("CHATID " + chatID);
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }

        success = sendToClient("CHATNAME " + chatName);
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }



        success = DatabaseManager.makeUpdate("insert into chat values(" + chatID + ", '" + chatType + "', 'PRSNL_VAR_NAME', '" + participantList + "');");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        String[][] chatNameForOtherParticipant = DatabaseManager.makeQuery("select display_name from accounts where account_id = '" + clientLoggedInUserID + "';");
        if(chatNameForOtherParticipant == null || chatNameForOtherParticipant.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        success = DatabaseManager.makeUpdate("insert into unsentNewChats values('" + otherParticipant + "', " + chatID + ", '" + chatType + "', '" + chatNameForOtherParticipant[0][0] + "', '" + clientLoggedInUserID + "');");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        success = DatabaseManager.makeUpdate("create table chat" + chatID + "(message_id number(64) primary key not null, from_account_id char(16) not null, message_timestamp number(64) not null, message_type varchar(16) not null, message_content varchar(1048576) not null);");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        Long chatCreationMessageID = 0L;
        String chatCreationMessageSenderID = "0000000000000000";
        Long chatCreationMessageTimestamp = System.currentTimeMillis();
        String chatCreationMessageType = "TEXT";
        String chatCreationMessageContent = "Chat created at " + (new Timestamp(chatCreationMessageTimestamp)).toLocalDateTime().format(DateTimeFormatter.ofPattern("hh:mm:ss a dd/MM/yy")).toUpperCase();
        success = DatabaseManager.makeUpdate("insert into chat" + chatID + " values(" + chatCreationMessageID + ", '" + chatCreationMessageSenderID + "', " + chatCreationMessageTimestamp + ", '" + chatCreationMessageType + "', '" + chatCreationMessageContent + "');");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        success = DatabaseManager.makeUpdate("insert into unsentNewMessages values('" + clientLoggedInUserID + "', " + chatID + ", " + chatCreationMessageID + ", '" + chatCreationMessageSenderID + "', " + chatCreationMessageTimestamp + ", '" + chatCreationMessageType + "', '" + chatCreationMessageContent + "');");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        success = DatabaseManager.makeUpdate("insert into unsentNewMessages values('" + otherParticipant + "', " + chatID + ", " + chatCreationMessageID + ", '" + chatCreationMessageSenderID + "', " + chatCreationMessageTimestamp + ", '" + chatCreationMessageType + "', '" + chatCreationMessageContent + "');");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }



        success = sendToClient("DONE");
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        return true;
    }

    private boolean replyToNewPersonalChatCheckingRequest()
    {
        if(clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("NUMBER 0");
            if(!success)
                return false;
            String reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is not logged in to any user.", clientLoggedInUserID);
            return false;
        }

        String[][] unsentChats = DatabaseManager.makeQuery("select * from unsentNewChats where to_be_sent_to = '" + clientLoggedInUserID + "';");
        if(unsentChats == null || unsentChats.length == 0)
        {
            boolean success = sendToClient("NUMBER 0");
            if(!success)
                return false;
            String reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results or there are no new chats to retrieve.", clientLoggedInUserID);
            return false;
        }

        int numberOfNewChats = unsentChats.length;
        boolean success = sendToClient("NUMBER " + numberOfNewChats);
        if(!success)
            return false;
        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }

        for(int i = 0; i < numberOfNewChats; i++)
        {
            Long chatID = Long.parseLong(unsentChats[i][1]);
            String chatType = unsentChats[i][2];
            String chatName = unsentChats[i][3];
            String participantsList = unsentChats[i][4];
            String[] participantsID = participantsList.split(",");
            int numberOfParticipants = participantsID.length;
            String[] participantsName = new String[numberOfParticipants];
            for(int j = 0; j < participantsName.length; j++)
            {
                String[][] name = DatabaseManager.makeQuery("select display_name from accounts where account_id = '" + participantsID[j] + "';");
                if(name == null || name.length == 0)
                {
                    participantsName[j] = "unknown";
                    if(clientSocket != null)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database could not find display name of an user.", clientLoggedInUserID);
                    continue;
                }
                participantsName[j] = name[0][0];
            }



            success = sendToClient("CHATID " + chatID);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }


            success = sendToClient("CHATTYPE " + chatType);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }



            success = sendToClient("CHATNAME " + chatName);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }



            success = sendToClient("PEOPLENO " + numberOfParticipants);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }


            for(int j = 0; j < numberOfParticipants; j++)
            {
                success = sendToClient("ACCID " + participantsID[j]);
                if(!success)
                    return false;
                reply = receiveFromClient();
                if(reply == null)
                {
                    return false;
                }
                if(!reply.equals("OK"))
                {
                    if(clientSocket != null)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                    return false;
                }



                success = sendToClient("ACCNAME " + participantsName[j]);
                if(!success)
                    return false;
                reply = receiveFromClient();
                if(reply == null)
                {
                    return false;
                }
                if(!reply.equals("OK"))
                {
                    if(clientSocket != null)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                    return false;
                }
            }


            success = DatabaseManager.makeUpdate("delete from unsentNewChats where to_be_sent_to = '" + clientLoggedInUserID + "' and chat_id = " + chatID + ";");
            if(!success && clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database faced an error while deleting a chat from unsent chats list after sending that chat to the client.", clientLoggedInUserID);
        }



        success = sendToClient("DONE");
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        return true;
    }

    private boolean replyToSendNewMessageRequest()
    {
        if(clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is not logged in to any user.", clientLoggedInUserID);
            return false;
        }

        boolean success = sendToClient("OK");
        if(!success)
            return false;

        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("CHATID"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String chatID;
        try {
            chatID = reply.substring(7);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(chatID.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;

        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("TMPMSGID"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String temporaryMessageID;
        try {
            temporaryMessageID = reply.substring(9);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(temporaryMessageID.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;

        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("MSGTYPE"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String messageType;
        try {
            messageType = reply.substring(8);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(messageType.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;

        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("CONTENT"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String messageContent;
        try {
            messageContent = reply.substring(8);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(messageContent.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;

        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("DONE"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }

        String[][] maxMessageID = DatabaseManager.makeQuery("select max(message_id) from chat" + chatID + ";");
        if(maxMessageID == null || maxMessageID.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        Long newMessageID = Long.parseLong(maxMessageID[0][0]) + 1;
        String senderAccountID = clientLoggedInUserID;
        Long newMessageTimestamp = System.currentTimeMillis();

        success = sendToClient("NEWMSGID " + newMessageID);
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }

        success = sendToClient("MSGTIME " + newMessageTimestamp);
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }


        success = DatabaseManager.makeUpdate("insert into chat" + chatID + " values(" + newMessageID + ", '" + senderAccountID + "', " + newMessageTimestamp + ", '" + messageType + "', '" + messageContent + "');");
        if(!success)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        String[][] chatDetails = DatabaseManager.makeQuery("select * from chat where chat_id = " + chatID + ";");
        if(chatDetails == null || chatDetails.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        String[] allParticipants = chatDetails[0][3].split(",");
        for(int i = 0; i < allParticipants.length; i++) {
            if(!allParticipants[i].equals(senderAccountID)) {
                success = DatabaseManager.makeUpdate("insert into unsentNewMessages values('" + allParticipants[i] + "', " + chatID + ", " + newMessageID + ", '" + senderAccountID + "', " + newMessageTimestamp + ", '" + messageType + "', '" + messageContent + "');");
                if(!success)
                {
                    success = sendToClient("ERROR");
                    if(!success)
                        return false;
                    if(clientSocket != null)
                        LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
                    return false;
                }
            }
        }


        success = sendToClient("DONE");
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        return true;
    }

    private boolean replyToNewMessageCheckingRequest()
    {
        if(clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("NUMBER 0");
            if(!success)
                return false;
            String reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is not logged in to any user.", clientLoggedInUserID);
            return false;
        }

        String[][] unsentMessages = DatabaseManager.makeQuery("select * from unsentNewMessages where to_be_sent_to = '" + clientLoggedInUserID + "';");
        if(unsentMessages == null || unsentMessages.length == 0)
        {
            boolean success = sendToClient("NUMBER 0");
            if(!success)
                return false;
            String reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results or there are no new messages to retrieve.", clientLoggedInUserID);
            return false;
        }

        int numberOfNewMessages = unsentMessages.length;
        boolean success = sendToClient("NUMBER " + numberOfNewMessages);
        if(!success)
            return false;
        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }

        for(int i = 0; i < numberOfNewMessages; i++)
        {
            Long chatID = Long.parseLong(unsentMessages[i][1]);
            Long messageID = Long.parseLong(unsentMessages[i][2]);
            String senderAccountID = unsentMessages[i][3];
            Long messageTimestamp = Long.parseLong(unsentMessages[i][4]);
            String messageType = unsentMessages[i][5];
            String messageContent = unsentMessages[i][6];


            success = sendToClient("CHATID " + chatID);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }


            success = sendToClient("MSGID " + messageID);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }


            success = sendToClient("SENDERID " + senderAccountID);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }


            success = sendToClient("MSGTIME " + messageTimestamp);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }


            success = sendToClient("MSGTYPE " + messageType);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }


            success = sendToClient("CONTENT " + messageContent);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }


            success = DatabaseManager.makeUpdate("delete from unsentNewMessages where to_be_sent_to = '" + clientLoggedInUserID + "' and chat_id = " + chatID + " and message_id = " + messageID + ";");
            if(!success && clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database faced an error while deleting a message from unsent messages list after sending that message to the client.", clientLoggedInUserID);
            if(!senderAccountID.equals("0000000000000000")) {
                success = DatabaseManager.makeUpdate("insert into unsentNewReadReceipts values('" + senderAccountID + "', " + chatID + ", " + messageID + ", 2);");
                if(!success && clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database faced an error while inserting a new read receipt into unsent read receipts list.", clientLoggedInUserID);
            }
        }


        success = sendToClient("DONE");
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        return true;
    }

    private boolean replyToSendNewReadReceiptRequest()
    {
        if(clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is not logged in to any user.", clientLoggedInUserID);
            return false;
        }

        boolean success = sendToClient("OK");
        if(!success)
            return false;

        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("CHATID"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String chatID;
        try {
            chatID = reply.substring(7);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(chatID.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;

        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.contains("MSGID"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        String messageID;
        try {
            messageID = reply.substring(6);
        }
        catch(Exception e)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Exception occurred while extracting client message.", clientLoggedInUserID);
            return false;
        }
        if(messageID.isEmpty())
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        success = sendToClient("OK");
        if(!success)
            return false;

        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("DONE"))
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }

        String[][] chatInfo = DatabaseManager.makeQuery("select * from chat where chat_id = " + chatID + ";");
        if(chatInfo == null || chatInfo.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        String[][] messageInfo = DatabaseManager.makeQuery("select * from chat" + chatID + " where message_id = " + messageID + ";");
        if(messageInfo == null || messageInfo.length == 0)
        {
            success = sendToClient("ERROR");
            if(!success)
                return false;
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
            return false;
        }
        String originalMessageSenderID = messageInfo[0][1];
        String chatParticipants = chatInfo[0][3];
        String chatParticipantsWithoutReadReceiptSender = chatParticipants.replaceAll(clientLoggedInUserID, "");
        if(chatParticipantsWithoutReadReceiptSender.contains(originalMessageSenderID))
        {
            success = DatabaseManager.makeUpdate("insert into unsentNewReadReceipts values('" + originalMessageSenderID + "', " + chatID + ", " + messageID + ", 3);");
            if(!success)
            {
                success = sendToClient("ERROR");
                if(!success)
                    return false;
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results.", clientLoggedInUserID);
                return false;
            }
        }

        success = sendToClient("OK");
        if(!success)
            return false;
        return true;
    }

    private boolean replyToNewReadReceiptCheckingRequest()
    {
        if(clientLoggedInUserID.equals("0000000000000000"))
        {
            boolean success = sendToClient("NUMBER 0");
            if(!success)
                return false;
            String reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client is not logged in to any user.", clientLoggedInUserID);
            return false;
        }

        String[][] unsentReadReceipts = DatabaseManager.makeQuery("select * from unsentNewReadReceipts where to_be_sent_to = '" + clientLoggedInUserID + "';");
        if(unsentReadReceipts == null || unsentReadReceipts.length == 0)
        {
            boolean success = sendToClient("NUMBER 0");
            if(!success)
                return false;
            String reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database did not give expected results or there are no new read receipts to retrieve.", clientLoggedInUserID);
            return false;
        }

        int numberOfNewReadReceipts = unsentReadReceipts.length;
        boolean success = sendToClient("NUMBER " + numberOfNewReadReceipts);
        if(!success)
            return false;
        String reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }

        for(int i = 0; i < numberOfNewReadReceipts; i++)
        {
            Long chatID = Long.parseLong(unsentReadReceipts[i][1]);
            Long messageID = Long.parseLong(unsentReadReceipts[i][2]);
            Integer newReadState = Integer.parseInt(unsentReadReceipts[i][3]);


            success = sendToClient("CHATID " + chatID);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }


            success = sendToClient("MSGID " + messageID);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }


            success = sendToClient("RDSTATE " + newReadState);
            if(!success)
                return false;
            reply = receiveFromClient();
            if(reply == null)
            {
                return false;
            }
            if(!reply.equals("OK"))
            {
                if(clientSocket != null)
                    LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
                return false;
            }


            success = DatabaseManager.makeUpdate("delete from unsentNewReadReceipts where to_be_sent_to = '" + clientLoggedInUserID + "' and chat_id = " + chatID + " and message_id = " + messageID + " and read_state = " + newReadState + ";");
            if(!success && clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Server database faced an error while deleting a read receipt from unsent read receipts list after sending that read receipt to the client.", clientLoggedInUserID);
        }


        success = sendToClient("DONE");
        if(!success)
            return false;
        reply = receiveFromClient();
        if(reply == null)
        {
            return false;
        }
        if(!reply.equals("OK"))
        {
            if(clientSocket != null)
                LogManager.updateUserLog(clientSocket.getInetAddress().getHostAddress() + "/" + clientSocket.getPort() + " -> Client sent invalid formatted message.", clientLoggedInUserID);
            return false;
        }
        return true;
    }






    synchronized public Socket getClientSocket()
    {
        return clientSocket;
    }
}
