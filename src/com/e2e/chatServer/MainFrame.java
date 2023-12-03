/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package com.e2e.chatServer;

import com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatArcDarkIJTheme;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;
import java.io.FileInputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author Soubhik
 */
public class MainFrame extends javax.swing.JFrame {

    /**
     * Creates new form MainFrame
     */
    public MainFrame(boolean logInitialized, boolean configInitialized, boolean databaseInitialized) {
        initComponents();
        setLocationRelativeTo(null);
        setMinimumSize(new Dimension(800, 600));

        if(logInitialized)
        {
            logStatusLabel.setText("Log - running");
            logStatusLabel.setForeground(Color.green);
        }
        if(configInitialized)
        {
            configStatusLabel.setText("Config - running");
            configStatusLabel.setForeground(Color.green);
        }
        if(databaseInitialized)
        {
            databaseStatusLabel.setText("Database - running");
            databaseStatusLabel.setForeground(Color.green);
        }
        if(!logInitialized || !configInitialized || !databaseInitialized)
        {
            startOrStopServerToggleButton.setEnabled(false);
            startOrStopServerToggleButton.setText("One or more server component is not working");
        }

        JLabel enterToSearchLabel = new JLabel("Enter a search string to find matching users");
        enterToSearchLabel.setHorizontalAlignment(JLabel.CENTER);
        enterToSearchLabel.setVerticalAlignment(JLabel.CENTER);
        enterToSearchLabel.setEnabled(false);
        usersFoundListScrollPanel.getViewport().setView(enterToSearchLabel);

        logDisplayControlsTabbedPanel.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        JScrollPane outsideScrollPanel = new JScrollPane();
        outsideScrollPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        outsideScrollPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        outsideScrollPanel.getVerticalScrollBar().setUnitIncrement(10);
        JTextArea insideTextPanel = new JTextArea();
        insideTextPanel.setLineWrap(true);
        insideTextPanel.setEditable(false);
        insideTextPanel.setFocusable(false);
        ((DefaultCaret)insideTextPanel.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
        new SmartScroller(outsideScrollPanel);
        try(FileInputStream log = new FileInputStream(ConfigManager.getAppFolderPath() + "\\log\\server_main.log"))
        {
            byte[] logData = log.readAllBytes();
            insideTextPanel.setText(new String(logData));
        }
        catch(Exception e)
        {
            insideTextPanel.setText("Exception thrown while reading log data.");
        }
        outsideScrollPanel.getViewport().setView(insideTextPanel);
        logDisplayControlsTabbedPanel.addTab("server_main.log", outsideScrollPanel);
        LogManager.addServerMainLogUpdatedListener(new ActionListener() {
            @Override
            synchronized public void actionPerformed(ActionEvent e) {
                try(FileInputStream log = new FileInputStream(ConfigManager.getAppFolderPath() + "\\log\\server_main.log"))
                {
                    byte[] logData = log.readAllBytes();
                    insideTextPanel.setText(new String(logData));
                }
                catch(Exception ex)
                {
                    insideTextPanel.setText(insideTextPanel.getText() + "\nException thrown while reading log data.");
                }
            }
        });

        selectedUserDetailsLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                String accountID = selectedUserDetailsLabel.getText().substring(selectedUserDetailsLabel.getText().indexOf("Account ID") + 17, selectedUserDetailsLabel.getText().indexOf("Account ID") + 33);
                //System.out.println(accountID);
                for(int i = 0; i < logDisplayControlsTabbedPanel.getTabCount(); i++)
                {
                    if(logDisplayControlsTabbedPanel.getTitleAt(i).equals("user_" + accountID + ".log")) {
                        logDisplayControlsTabbedPanel.setSelectedIndex(i);
                        return;
                    }
                }
                JScrollPane outsideScrollPanel = new JScrollPane();
                outsideScrollPanel.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
                outsideScrollPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
                outsideScrollPanel.getVerticalScrollBar().setUnitIncrement(10);
                JTextArea insideTextPanel = new JTextArea();
                insideTextPanel.setLineWrap(true);
                insideTextPanel.setEditable(false);
                insideTextPanel.setFocusable(false);
                ((DefaultCaret)insideTextPanel.getCaret()).setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
                new SmartScroller(outsideScrollPanel);
                try(FileInputStream log = new FileInputStream(ConfigManager.getAppFolderPath() + "\\log\\user_" + accountID + ".log"))
                {
                    byte[] logData = log.readAllBytes();
                    insideTextPanel.setText(new String(logData));
                }
                catch(Exception ex)
                {
                    insideTextPanel.setText("Exception thrown while reading log data.");
                }
                outsideScrollPanel.getViewport().setView(insideTextPanel);
                logDisplayControlsTabbedPanel.addTab("user_" + accountID + ".log", outsideScrollPanel);
                logDisplayControlsTabbedPanel.setSelectedIndex(logDisplayControlsTabbedPanel.getTabCount() - 1);
                JPanel tabComponent = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
                tabComponent.setOpaque(false);
                JLabel tabLabel = new JLabel("user_" + accountID + ".log");
                tabLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
                tabComponent.add(tabLabel);
                JButton tabCloseButton = new JButton("X");
                tabCloseButton.setBackground(Color.red);
                tabCloseButton.setForeground(Color.white);
                tabCloseButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        for(int i = 0; i < logDisplayControlsTabbedPanel.getTabCount(); i++)
                        {
                            if(logDisplayControlsTabbedPanel.getTitleAt(i).equals("user_" + accountID + ".log")) {
                                logDisplayControlsTabbedPanel.remove(i);
                                return;
                            }
                        }
                    }
                });
                tabComponent.add(tabCloseButton);
                logDisplayControlsTabbedPanel.setTabComponentAt(logDisplayControlsTabbedPanel.getTabCount() - 1, tabComponent);
                LogManager.addParticularUserLogUpdatedListener(accountID, new ActionListener() {
                    @Override
                    synchronized public void actionPerformed(ActionEvent e) {
                        try(FileInputStream log = new FileInputStream(ConfigManager.getAppFolderPath() + "\\log\\user_" + accountID + ".log"))
                        {
                            byte[] logData = log.readAllBytes();
                            insideTextPanel.setText(new String(logData));
                        }
                        catch(Exception ex)
                        {
                            insideTextPanel.setText(insideTextPanel.getText() + "\nException thrown while reading log data.");
                        }
                    }
                });
            }
        });

        setVisible(true);
        //System.out.println(getSize());
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">
    private void initComponents() {

        searchTypeButtonsGroup = new javax.swing.ButtonGroup();
        serverComponentStatusPanel = new javax.swing.JPanel();
        logStatusLabel = new javax.swing.JLabel();
        configStatusLabel = new javax.swing.JLabel();
        databaseStatusLabel = new javax.swing.JLabel();
        serverControlsPanel = new javax.swing.JPanel();
        startOrStopServerToggleButton = new javax.swing.JToggleButton();
        restartServerButton = new javax.swing.JButton();
        userDetailsQueryControlsPanel = new javax.swing.JPanel();
        usernameRadioButton = new javax.swing.JRadioButton();
        accountIdRadioButton = new javax.swing.JRadioButton();
        searchTextField = new javax.swing.JTextField();
        usersFoundListScrollPanel = new javax.swing.JScrollPane();
        selectedUserDetailsLabel = new javax.swing.JLabel();
        serverNetworkStatusPanel = new javax.swing.JPanel();
        networkStatusLabel = new javax.swing.JLabel();
        serverIpLabel = new javax.swing.JLabel();
        serverPortLabel = new javax.swing.JLabel();
        connectedUsersCountLabel = new javax.swing.JLabel();
        logDisplayControlsTabbedPanel = new javax.swing.JTabbedPane();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        exitMenuItem = new javax.swing.JMenuItem();
        editMenu = new javax.swing.JMenu();
        settingsMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                closeWindow();
            }
        });
        setTitle("E2E Chat Server");

        serverComponentStatusPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Server component status", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Segoe UI", 1, 14))); // NOI18N

        logStatusLabel.setForeground(Color.red);
        logStatusLabel.setText("Log - failed to initialize");

        configStatusLabel.setForeground(Color.red);
        configStatusLabel.setText("Config - failed to initialize");

        databaseStatusLabel.setForeground(Color.red);
        databaseStatusLabel.setText("Database - failed to initialize");

        javax.swing.GroupLayout serverComponentStatusPanelLayout = new javax.swing.GroupLayout(serverComponentStatusPanel);
        serverComponentStatusPanel.setLayout(serverComponentStatusPanelLayout);
        serverComponentStatusPanelLayout.setHorizontalGroup(
                serverComponentStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(serverComponentStatusPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(serverComponentStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(logStatusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(configStatusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(databaseStatusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE))
                                .addContainerGap())
        );
        serverComponentStatusPanelLayout.setVerticalGroup(
                serverComponentStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(serverComponentStatusPanelLayout.createSequentialGroup()
                                .addGap(14, 14, 14)
                                .addComponent(logStatusLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(configStatusLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(databaseStatusLabel)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        serverControlsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Server controls", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Segoe UI", 1, 14))); // NOI18N

        startOrStopServerToggleButton.setText("Start server");
        startOrStopServerToggleButton.setFocusPainted(false);
        startOrStopServerToggleButton.setBackground(Color.green);
        startOrStopServerToggleButton.setForeground(Color.black);
        startOrStopServerToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startOrStopServerToggleButtonActionPerformed(evt);
            }
        });

        restartServerButton.setText("Restart server");
        restartServerButton.setFocusPainted(false);
        restartServerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                restartServerButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout serverControlsPanelLayout = new javax.swing.GroupLayout(serverControlsPanel);
        serverControlsPanel.setLayout(serverControlsPanelLayout);
        serverControlsPanelLayout.setHorizontalGroup(
                serverControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(serverControlsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(serverControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(startOrStopServerToggleButton, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(restartServerButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE))
                                .addContainerGap())
        );
        serverControlsPanelLayout.setVerticalGroup(
                serverControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(serverControlsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(startOrStopServerToggleButton, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(restartServerButton, javax.swing.GroupLayout.PREFERRED_SIZE, 33, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        userDetailsQueryControlsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "User details query controls", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Segoe UI", 1, 14))); // NOI18N

        usernameRadioButton.setText("Username");
        usernameRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                usernameRadioButtonActionPerformed(evt);
            }
        });

        accountIdRadioButton.setText("Account ID");
        accountIdRadioButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                accountIdRadioButtonActionPerformed(evt);
            }
        });

        searchTypeButtonsGroup.add(usernameRadioButton);
        searchTypeButtonsGroup.add(accountIdRadioButton);
        usernameRadioButton.setSelected(true);

        //searchTextField.
        searchTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                searchTextFieldActionPerformed();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                searchTextFieldActionPerformed();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                searchTextFieldActionPerformed();
            }
        });

        usersFoundListScrollPanel.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        //selectedUserDetailsLabel.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        selectedUserDetailsLabel.setVisible(false);
        selectedUserDetailsLabel.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(0, 5, 0, 0)));

        javax.swing.GroupLayout userDetailsQueryControlsPanelLayout = new javax.swing.GroupLayout(userDetailsQueryControlsPanel);
        userDetailsQueryControlsPanel.setLayout(userDetailsQueryControlsPanelLayout);
        userDetailsQueryControlsPanelLayout.setHorizontalGroup(
                userDetailsQueryControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, userDetailsQueryControlsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(userDetailsQueryControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                        .addComponent(selectedUserDetailsLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(usersFoundListScrollPanel, javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.LEADING, userDetailsQueryControlsPanelLayout.createSequentialGroup()
                                                .addComponent(usernameRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(accountIdRadioButton, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(searchTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 170, Short.MAX_VALUE)))
                                .addContainerGap())
        );
        userDetailsQueryControlsPanelLayout.setVerticalGroup(
                userDetailsQueryControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(userDetailsQueryControlsPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(userDetailsQueryControlsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                        .addComponent(usernameRadioButton)
                                        .addComponent(accountIdRadioButton)
                                        .addComponent(searchTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(usersFoundListScrollPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 181, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(selectedUserDetailsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 135, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        serverNetworkStatusPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Server network status", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Segoe UI", 1, 14))); // NOI18N

        networkStatusLabel.setForeground(Color.red);
        networkStatusLabel.setText("Network - inactive");

        //serverIpLabel.setForeground(Color.red);
        serverIpLabel.setText("Server IP - unknown");
        serverIpLabel.setEnabled(false);

        //serverPortLabel.setForeground(Color.red);
        serverPortLabel.setText("Server Port - unknown");
        serverPortLabel.setEnabled(false);

        //connectedUsersCountLabel.setForeground(Color.red);
        connectedUsersCountLabel.setText("Number of connected users - 0");
        connectedUsersCountLabel.setEnabled(false);

        javax.swing.GroupLayout serverNetworkStatusPanelLayout = new javax.swing.GroupLayout(serverNetworkStatusPanel);
        serverNetworkStatusPanel.setLayout(serverNetworkStatusPanelLayout);
        serverNetworkStatusPanelLayout.setHorizontalGroup(
                serverNetworkStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(serverNetworkStatusPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(serverNetworkStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(serverIpLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(serverPortLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(connectedUsersCountLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                                        .addComponent(networkStatusLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addContainerGap())
        );
        serverNetworkStatusPanelLayout.setVerticalGroup(
                serverNetworkStatusPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(serverNetworkStatusPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(networkStatusLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(serverIpLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(serverPortLabel)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(connectedUsersCountLabel)
                                .addContainerGap(8, Short.MAX_VALUE))
        );

        logDisplayControlsTabbedPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Log display controls", javax.swing.border.TitledBorder.CENTER, javax.swing.border.TitledBorder.TOP, new java.awt.Font("Segoe UI", 1, 14))); // NOI18N

        fileMenu.setText("File");

        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                closeWindow();
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        editMenu.setText("Edit");

        settingsMenuItem.setText("Settings");
        settingsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new SettingsDialog(getSelfReference(), true);
            }
        });
        editMenu.add(settingsMenuItem);

        menuBar.add(editMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(serverComponentStatusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(serverControlsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(serverNetworkStatusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGroup(layout.createSequentialGroup()
                                .addComponent(userDetailsQueryControlsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(logDisplayControlsTabbedPanel))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                        .addComponent(serverControlsPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(serverComponentStatusPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(serverNetworkStatusPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(userDetailsQueryControlsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(logDisplayControlsTabbedPanel)))
        );

        pack();
    }// </editor-fold>

    private MainFrame getSelfReference()
    {
        return this;
    }

    private void startOrStopServerToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
        if(startOrStopServerToggleButton.isSelected())
        {
            ConnectionManager.initializeNetwork();
            networkStatusLabel.setText("Network - active");
            networkStatusLabel.setForeground(Color.green);
            try(final DatagramSocket socket = new DatagramSocket()){
                socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
                serverIpLabel.setText("Server IP - " + socket.getLocalAddress().getHostAddress());
            }
            catch (Exception e)
            {
                serverIpLabel.setText("Server IP - unknown");
            }
            serverPortLabel.setText("Server Port - " + ConnectionManager.getConnectionAcceptingSocket().getLocalPort());
            serverIpLabel.setEnabled(true);
            serverPortLabel.setEnabled(true);
            ConnectionManager.addNumberOfConnectedUsersChangedActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    connectedUsersCountLabel.setText("Number of connected users - " + e.getSource());
                }
            });
            connectedUsersCountLabel.setEnabled(true);

            startOrStopServerToggleButton.setText("Stop server");
            startOrStopServerToggleButton.setBackground(Color.red);
        }
        else
        {
            ConnectionManager.stopAllNetworkConnections();
            networkStatusLabel.setText("Network - inactive");
            networkStatusLabel.setForeground(Color.red);
            serverIpLabel.setText("Server IP - unknown");
            serverPortLabel.setText("Server Port - unknown");
            serverIpLabel.setEnabled(false);
            serverPortLabel.setEnabled(false);
            ConnectionManager.removeAllNumberOfConnectedUsersChangedActionListeners();
            connectedUsersCountLabel.setText("Number of connected users - 0");
            connectedUsersCountLabel.setEnabled(false);

            startOrStopServerToggleButton.setText("Start server");
            startOrStopServerToggleButton.setBackground(Color.green);
        }
    }

    private void restartServerButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
        int response = JOptionPane.showConfirmDialog(this, "Are you sure you want to restart the server?", "Restart", JOptionPane.YES_NO_OPTION);
        if(!(response == JOptionPane.YES_OPTION))
        {
            return;
        }

        JFrame restartDialog = new JFrame();
        restartDialog.setUndecorated(true);
        restartDialog.setLocationRelativeTo(null);
        JLabel restartLabel = new JLabel("Restarting.  ");
        restartLabel.setFont(restartLabel.getFont().deriveFont(Font.BOLD, 20));
        restartLabel.setBorder(new CompoundBorder(BorderFactory.createEtchedBorder(), new EmptyBorder(10, 10, 10, 10)));
        Timer restartAnimationTimer = new Timer();
        restartAnimationTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                int emptyIndex = restartLabel.getText().indexOf(' ');
                if(emptyIndex != -1) {
                    restartLabel.setText(restartLabel.getText().replaceFirst(" ", "."));
                }
                else
                {
                    restartLabel.setText("Restarting.  ");
                }
            }
        }, 0, 250);
        restartDialog.add(restartLabel);
        restartDialog.pack();
        restartDialog.setVisible(true);

        setVisible(false);

        //FlatArcDarkIJTheme.setup();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if(startOrStopServerToggleButton.isSelected())
                {
                    startOrStopServerToggleButton.doClick();
                }

                Timer invokeTimer = new Timer();
                invokeTimer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        DatabaseManager.closeDB();

                        dispose();

                        boolean logInitializationSuccess = LogManager.initializeLog();
                        boolean configInitializationSuccess = ConfigManager.initializeConfig();
                        boolean databaseInitializationSuccess = DatabaseManager.initializeDB();

                        SwingUtilities.invokeLater(new Runnable() {
                            @Override
                            public void run() {
                                new MainFrame(logInitializationSuccess, configInitializationSuccess, databaseInitializationSuccess);
                            }
                        });

                        restartDialog.setVisible(false);
                        restartAnimationTimer.cancel();
                        restartDialog.dispose();
                    }
                }, 2000);
            }
        });
    }

    private void usernameRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
        searchTextFieldActionPerformed();
    }

    private void accountIdRadioButtonActionPerformed(java.awt.event.ActionEvent evt) {
        // TODO add your handling code here:
        searchTextFieldActionPerformed();
    }

    private void searchTextFieldActionPerformed() {
        // TODO add your handling code here:
        selectedUserDetailsLabel.setVisible(false);
        if(searchTextField.getText().isEmpty())
        {
            usersFoundListScrollPanel.getViewport().setView(null);
            JLabel enterToSearchLabel = new JLabel("Enter a search string to find matching users");
            enterToSearchLabel.setHorizontalAlignment(JLabel.CENTER);
            enterToSearchLabel.setVerticalAlignment(JLabel.CENTER);
            enterToSearchLabel.setEnabled(false);
            usersFoundListScrollPanel.getViewport().setView(enterToSearchLabel);
            return;
        }

        String[][] matchingUsers = DatabaseManager.makeQuery("select * from accounts where " + (usernameRadioButton.isSelected()? "username" : "account_id") + " like '" + searchTextField.getText() + "%';");
        if(matchingUsers == null || matchingUsers.length == 0)
        {
            usersFoundListScrollPanel.getViewport().setView(null);
            JLabel noUsersFoundLabel = new JLabel("No users found matching the given search string");
            noUsersFoundLabel.setHorizontalAlignment(JLabel.CENTER);
            noUsersFoundLabel.setVerticalAlignment(JLabel.CENTER);
            noUsersFoundLabel.setEnabled(false);
            usersFoundListScrollPanel.getViewport().setView(noUsersFoundLabel);
            return;
        }

        usersFoundListScrollPanel.getViewport().setView(null);
        JPanel outsidePanel = new JPanel(new BorderLayout());
        JPanel insidePanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridwidth = GridBagConstraints.REMAINDER;
        constraints.weightx = 1.0;
        boolean usernameMode = usernameRadioButton.isSelected();
        for(int i = 0; i < matchingUsers.length; i++)
        {
            JButton userDetailsButton = new JButton();
            if(usernameMode)
            {
                userDetailsButton.setText(matchingUsers[i][1]);
            }
            else
            {
                userDetailsButton.setText(matchingUsers[i][0]);
            }
            insidePanel.add(userDetailsButton, constraints);
            userDetailsButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    String[][] userInfo = DatabaseManager.makeQuery("select * from accounts where " + (usernameRadioButton.isSelected()? "username":"account_id") + " = '" + userDetailsButton.getText() + "';");
                    if(userInfo == null || userInfo.length == 0)
                    {
                        selectedUserDetailsLabel.setText("Database error while fetching the selected user's details");
                        return;
                    }
                    selectedUserDetailsLabel.setText("<html><b><u>User Details</u></b><br>" +
                            "<b>Account ID</b> - " + userInfo[0][0] + "<br>" +
                            "<b>Username</b> - " + userInfo[0][1] + "<br>" +
                            "<b>Is Online?</b> - " + (userInfo[0][3].equals("1")? "yes":"no") + "<br>" +
                            "<b>Has Active Permanent Session?</b> - " + (userInfo[0][4].equals("1")? "yes":"no") + "<br>" +
                            "<b>Display Name</b> - " + userInfo[0][6] + "<br>" +
                            "<b>New Personal Chat Allowed?</b> - " + (userInfo[0][7].equals("1")? "yes":"no") + "<br>" +
                            "<b>New Group Chat Allowed?</b> - " + (userInfo[0][8].equals("1")? "yes":"no") + "</html>");
                    selectedUserDetailsLabel.setVisible(true);
                }
            });
        }
        outsidePanel.add(insidePanel, BorderLayout.NORTH);
        usersFoundListScrollPanel.getViewport().setView(outsidePanel);
    }

//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String args[]) {
//        /* Set the Nimbus look and feel */
//        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
//        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
//         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
//         */
//        try {
//            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
//                if ("Nimbus".equals(info.getName())) {
//                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
//                    break;
//                }
//            }
//        } catch (ClassNotFoundException ex) {
//            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (InstantiationException ex) {
//            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (IllegalAccessException ex) {
//            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
//            java.util.logging.Logger.getLogger(MainFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
//        }
//        //</editor-fold>
//
//        /* Create and display the form */
//        java.awt.EventQueue.invokeLater(new Runnable() {
//            public void run() {
//                new MainFrame().setVisible(true);
//            }
//        });
//    }

    private void closeWindow()
    {
        setVisible(false);
        if(startOrStopServerToggleButton.isSelected())
        {
            startOrStopServerToggleButton.doClick();
        }
        Timer waitForClosingTimer = new Timer();
        waitForClosingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                DatabaseManager.closeDB();
                dispose();
                waitForClosingTimer.cancel();
            }
        }, 2000);
    }

    // Variables declaration - do not modify
    private javax.swing.JRadioButton accountIdRadioButton;
    private javax.swing.ButtonGroup searchTypeButtonsGroup;
    private javax.swing.JLabel configStatusLabel;
    private javax.swing.JLabel connectedUsersCountLabel;
    private javax.swing.JLabel databaseStatusLabel;
    private javax.swing.JMenu editMenu;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JTabbedPane logDisplayControlsTabbedPanel;
    private javax.swing.JLabel logStatusLabel;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JLabel networkStatusLabel;
    private javax.swing.JButton restartServerButton;
    private javax.swing.JTextField searchTextField;
    private javax.swing.JLabel selectedUserDetailsLabel;
    private javax.swing.JPanel serverComponentStatusPanel;
    private javax.swing.JPanel serverControlsPanel;
    private javax.swing.JLabel serverIpLabel;
    private javax.swing.JPanel serverNetworkStatusPanel;
    private javax.swing.JLabel serverPortLabel;
    private javax.swing.JMenuItem settingsMenuItem;
    private javax.swing.JToggleButton startOrStopServerToggleButton;
    private javax.swing.JPanel userDetailsQueryControlsPanel;
    private javax.swing.JRadioButton usernameRadioButton;
    private javax.swing.JScrollPane usersFoundListScrollPanel;
    // End of variables declaration
}
