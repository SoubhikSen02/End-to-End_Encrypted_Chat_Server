# End-to-End Encrypted Chat Server
The whole application is based on a ***client-server*** architecture.

This repository is for the ***server*** version.

For the ***client*** version, [click here](https://github.com/SoubhikSen02/End-to-End_Encrypted_Chat_Client).

## Features
- User account management including registration, login, logout, account recovery, account settings updating, etc.
- User session management
- Forwarding of chats, messages and read receipts to the respective clients
- Effective network and resource management to handle any number of clients, all at the same time
- Provide a clean GUI for easy management

## Dependencies
1. [FlatLaf](https://github.com/JFormDesigner/FlatLaf) v3.2 -
   - core
   - intellij themes
2. [SQLite JDBC](https://github.com/xerial/sqlite-jdbc) v3.43.0.0

## Project folder structure
The 2 folders included in this project contains the following -
- `lib` - contains the JAR files of external libraries used
- `src` - contains the source java files 

## How to use
### Direct run -
Go to [releases](https://github.com/SoubhikSen02/End-to-End_Encrypted_Chat_Server/releases) and follow the steps given in the latest release.
### Compile and run -
1. Make sure JDK is installed and environment variables are set properly.
2. Download the entire repository code as ZIP, extract it and go into the extracted folder containing the sub-folders `lib` and `src`.
3. Execute the following commands individually line by line -
```
cd src
jar xvf ..\lib\flatlaf-3.2.jar
jar xvf ..\lib\flatlaf-intellij-themes-3.2.jar
jar xvf ..\lib\sqlite-jdbc-3.43.0.0.jar
javac com/e2e/chatServer/*.java
del /s *.java
jar cvfe ChatServer.jar com.e2e.chatServer.Main .
cd..
move src\ChatServer.jar .
```
4. Execute the following command to run the server -
```
java -jar ChatServer.jar
```
