import java.io.*;
import java.util.*;
import java.text.*;
import java.net.URL;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.*;
import javax.swing.text.DefaultCaret;

import oracle.jrockit.jfr.JFR;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.w3c.dom.*;

import java.util.Properties;

// WindowHandler prüft, ob das Fenster minimiert ist oder nicht
class WindowHandler extends WindowAdapter implements WindowListener, WindowFocusListener, WindowStateListener {
    public String activeState;
    public void windowClosed(WindowEvent e) {
    }
    public void windowOpened(WindowEvent e) {
    }
    public void windowIconified(WindowEvent e) {
        activeState = "iconified";
    }
    public void windowDeiconified(WindowEvent e) {
        activeState = "deiconified";
    }
    public void windowActivated(WindowEvent e) {
    }
    public void windowDeactivated(WindowEvent e) {
    }
    public void windowStateChanged(WindowEvent e) {
    }
    public String getActiveState() {
        return activeState;
    }     
}

// Das eigentliche GUI
public class Blabl extends JFrame {

    public SocketClient client;
    public int port;
    public String serverAddr, username, password;
    public Thread clientThread;
    public DefaultListModel model;
    public File file;

    public String activeState;
    public WindowHandler windowHandler;
    public HTMLEditorKit kit;
    public HTMLDocument doc;
    public boolean threadRunning = true;
    
    public int fontSize = 4;
    public String frameTitle;
    public String version;
    public String welcome;

    public Properties prop;
    
    public TrayIcon trayIcon;
    public SystemTray tray;
    
    public JButton ButtonDateiSenden;
    public JButton ButtonDateiSuchen;
    public JButton ButtonLogin;
    public JButton ButtonNachricht;
    public JButton ButtonRegistrieren;
    public JButton ButtonVerbinden;
    public JButton ButtonVerlauf;
    public JEditorPane Chat;
    public JTextField FieldDatei;
    public JTextField FieldNachricht;
    public JTextField FieldPasswort;
    public JTextField FieldPort;
    public JTextField FieldServer;
    public JTextField FieldUsername;
    public JTextField FieldVerlauf;
    public JLabel LabelPort;
    public JLabel LabelServer;
    public JLabel LabelDatei;
    public JLabel LabelNachricht;
    public JLabel LabelPasswort;
    public JLabel LabelUsername;
    public JLabel LabelVerlauf;
    public JList<String> Userlist;
    public JScrollPane chatPanel;
    public JSplitPane contentPanel;
    public JPanel contentPanelContainer;
    public JPanel controlPanel;
    public JPanel historyPanel;
    public JPanel serverPanel;
    public JSplitPane settingsPanel;
    public JPanel settingsPanelContainer;
    public JPanel userPanel;
    public JScrollPane userlistPanel;
    
    public Blabl() { 
    
        setTitle("Blabl 0.1");
        setLocationRelativeTo(null);
        setResizable(true);        
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {        
            @Override
            public void windowClosing(WindowEvent we) {                         
                closeWindow();                              
            }            
        });
        
        if(SystemTray.isSupported()){

            tray=SystemTray.getSystemTray();
            URL imageURL = Blabl.class.getResource("im.png");
            ImageIcon ico = new ImageIcon(imageURL);        
            Image image=Toolkit.getDefaultToolkit().getImage(imageURL);            
            ActionListener exitListener=new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    System.exit(0);
                }
            };
            PopupMenu popup=new PopupMenu();            
            MenuItem defaultItem = new MenuItem("anzeigen");
            defaultItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    setVisible(true);
                    setExtendedState(JFrame.NORMAL);
                }
            });
            popup.add(defaultItem);
            defaultItem=new MenuItem("beenden");
            defaultItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    closeWindow();
                }
            });
            popup.add(defaultItem);
            trayIcon=new TrayIcon(image, "Blabl", popup);
            trayIcon.setImageAutoSize(true);
        }
        addWindowStateListener(new WindowStateListener() {
            public void windowStateChanged(WindowEvent e) {
                if(e.getNewState()==ICONIFIED){
                    try {
                        tray.add(trayIcon);
                        setVisible(false);
                    } catch (AWTException ex) { }
                }
                if(e.getNewState()==7){
                    try{
                        tray.add(trayIcon);
                        setVisible(false);
                        }catch(AWTException ex){
                    }
                }
                if(e.getNewState()==MAXIMIZED_BOTH){
                    tray.remove(trayIcon);
                    setVisible(true);
                }
                if(e.getNewState()==NORMAL){
                    tray.remove(trayIcon);
                    setVisible(true);
                }
            }
        });
        URL imageURL = Blabl.class.getResource("im.png");
        setIconImage(Toolkit.getDefaultToolkit().getImage(imageURL));
        
        kit = new HTMLEditorKit();
        doc = new HTMLDocument();
        
        StyleSheet s = doc.getStyleSheet();
        s.addRule("body {padding:5px;margin:0px;font-size:12px;}.private{color:green;}#default:hover{background:red;}.default{color:black;}.error{color:red;}.warning{color:orange;}");
        kit.setStyleSheet(s);       
             
        initComponents();
        restoreDefaults();
        
        Chat.setEditorKit(kit);
        Chat.setDocument(doc);
        Chat.setEditable(false);
        Chat.setFocusable(false);
                                
        Userlist.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Userlist.setModel((model = new DefaultListModel()));        
        
        model.addElement("All");
        Userlist.setSelectedIndex(0);        
        
        // Listener ueberschreiben!!        
        this.addWindowListener(new WindowListener() {
            @Override public void windowOpened(WindowEvent e) {}
            @Override public void windowClosing(WindowEvent e) {}
            @Override public void windowClosed(WindowEvent e) {}
            @Override public void windowIconified(WindowEvent e) {}
            @Override public void windowDeiconified(WindowEvent e) {}
            @Override public void windowActivated(WindowEvent e) {}
            @Override public void windowDeactivated(WindowEvent e) {}
        });        
    }
    
    public void closeWindow() {    
        String ObjButtons[] = {"Beenden","abbrechen"};
        int PromptResult = JOptionPane.showOptionDialog(null,"Wollen Sie Blabl wirklich beenden?","Warnung",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,ObjButtons,ObjButtons[1]);
        if(PromptResult==JOptionPane.YES_OPTION) {
            if(username!=null) {
                try{ client.send(new Message("message", username, ".bye", "SERVER")); clientThread.stop();  }catch(Exception ex){ex.printStackTrace();}
            }
            System.exit(0);
        }         
    }
    
    // Setzt die Divider im JSplitPane nach Start des Programmes
    private void restoreDefaults() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                settingsPanel.setDividerLocation(settingsPanel.getSize().width/2);
                contentPanel.setDividerLocation((contentPanel.getSize().width/8)*6);
            }
        });
    } 
    
    @SuppressWarnings("unchecked")
    private void initComponents() {    

        settingsPanelContainer = new JPanel();        
            settingsPanel = new JSplitPane();
                serverPanel = new JPanel();
                LabelServer = new JLabel();
                LabelPort = new JLabel();
                FieldServer = new JTextField();
                FieldPort = new JTextField();
                ButtonVerbinden = new JButton();
                ButtonVerbinden.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        ButtonVerbindenActionPerformed(evt);
                    }
                });            
            userPanel = new JPanel();
                LabelUsername = new JLabel();
                LabelPasswort = new JLabel();
                FieldUsername = new JTextField();
                FieldPasswort = new JTextField();
                ButtonLogin = new JButton();
                ButtonLogin.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        ButtonLoginActionPerformed(evt);
                    }
                });                       
                ButtonRegistrieren = new JButton();
                ButtonRegistrieren.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        ButtonRegistrierenActionPerformed(evt);
                    }
                });
                
        contentPanelContainer = new JPanel();                
            contentPanel = new JSplitPane();
                chatPanel = new JScrollPane();
                    Chat = new JEditorPane();                    
                userlistPanel = new JScrollPane();
                    Userlist = new JList<>();
                
            controlPanel = new JPanel();        
                FieldNachricht = new JTextField();
                FieldNachricht.addActionListener(new ActionListener() {
                
                    @Override
                    public void actionPerformed(ActionEvent e) {
                       ButtonNachrichtActionPerformed(e);
                    }
                });                
                
                ButtonNachricht = new JButton();
                ButtonNachricht.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        ButtonNachrichtActionPerformed(e);
                    }
                });                    
                FieldDatei = new JTextField();
                ButtonDateiSenden = new JButton();
                ButtonDateiSenden.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        ButtonDateiSendenActionPerformed(evt);
                    }
                });            
                ButtonDateiSuchen = new JButton();
                ButtonDateiSuchen.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        ButtonDateiSuchenActionPerformed(evt);
                    }
                });            
                LabelNachricht = new JLabel();
                LabelDatei = new JLabel();
        
        chatPanel.setViewportView(Chat);
        userlistPanel.setViewportView(Userlist);
        
        contentPanel.setLeftComponent(chatPanel);
        contentPanel.setRightComponent(userlistPanel);

        // contentPanelContainer Layout
        javax.swing.GroupLayout contentPanelContainerLayout = new javax.swing.GroupLayout(contentPanelContainer);
        contentPanelContainer.setLayout(contentPanelContainerLayout);
        contentPanelContainerLayout.setHorizontalGroup(
            contentPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(contentPanelContainerLayout.createSequentialGroup()
                .addComponent(contentPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        contentPanelContainerLayout.setVerticalGroup(
            contentPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(contentPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 315, Short.MAX_VALUE)
        );

        ButtonNachricht.setText("senden");
        ButtonDateiSenden.setText("senden");
        ButtonDateiSuchen.setText("suchen");
        LabelNachricht.setText("Nachricht");
        LabelDatei.setText("Datei");
        
        // controlPanel Layout
        javax.swing.GroupLayout controlPanelLayout = new javax.swing.GroupLayout(controlPanel);
        controlPanel.setLayout(controlPanelLayout);
        controlPanelLayout.setHorizontalGroup(
            controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlPanelLayout.createSequentialGroup()
                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(LabelNachricht)
                    .addComponent(LabelDatei))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(controlPanelLayout.createSequentialGroup()
                        .addComponent(FieldDatei)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ButtonDateiSuchen)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ButtonDateiSenden))
                    .addGroup(controlPanelLayout.createSequentialGroup()
                        .addComponent(FieldNachricht)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(ButtonNachricht)))
                .addContainerGap())
        );
        controlPanelLayout.setVerticalGroup(
            controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(controlPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(FieldNachricht, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ButtonNachricht)
                    .addComponent(LabelNachricht))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(controlPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(FieldDatei, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(ButtonDateiSenden)
                    .addComponent(ButtonDateiSuchen)
                    .addComponent(LabelDatei)))
        );

        LabelServer.setText("Server");
        LabelPort.setText("Port");
        ButtonVerbinden.setText("verbinden");

        // serverPanel Layout
        javax.swing.GroupLayout serverPanelLayout = new javax.swing.GroupLayout(serverPanel);
        serverPanel.setLayout(serverPanelLayout);
        serverPanelLayout.setHorizontalGroup(
            serverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(serverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(serverPanelLayout.createSequentialGroup()
                        .addComponent(LabelServer)
                        .addGap(18, 18, 18)
                        .addComponent(FieldServer))
                    .addGroup(serverPanelLayout.createSequentialGroup()
                        .addComponent(LabelPort)
                        .addGap(18, 18, 18)
                        .addComponent(FieldPort))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, serverPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(ButtonVerbinden)))
                .addContainerGap())
        );
        serverPanelLayout.setVerticalGroup(
            serverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(serverPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(serverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LabelServer)
                    .addComponent(FieldServer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(serverPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LabelPort)
                    .addComponent(FieldPort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(ButtonVerbinden)
                .addContainerGap(23, Short.MAX_VALUE))
        );

        settingsPanel.setLeftComponent(serverPanel);
        LabelUsername.setText("Username");
        LabelPasswort.setText("Passwort");
        ButtonLogin.setText("Login");
        ButtonRegistrieren.setText("Registrieren");

        javax.swing.GroupLayout userPanelLayout = new javax.swing.GroupLayout(userPanel);
        userPanel.setLayout(userPanelLayout);
        userPanelLayout.setHorizontalGroup(
            userPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(userPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(userPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(userPanelLayout.createSequentialGroup()
                        .addComponent(LabelUsername)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(FieldUsername))
                    .addGroup(userPanelLayout.createSequentialGroup()
                        .addComponent(LabelPasswort)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(FieldPasswort))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, userPanelLayout.createSequentialGroup()
                        .addGap(0, 183, Short.MAX_VALUE)
                        .addComponent(ButtonRegistrieren)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ButtonLogin)))
                .addContainerGap())
        );
        userPanelLayout.setVerticalGroup(
            userPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(userPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(userPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(LabelUsername)
                    .addComponent(FieldUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(userPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(LabelPasswort)
                    .addComponent(FieldPasswort, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(userPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(ButtonLogin)
                    .addComponent(ButtonRegistrieren))
                .addContainerGap(23, Short.MAX_VALUE))
        );

        settingsPanel.setRightComponent(userPanel);

        javax.swing.GroupLayout settingsPanelContainerLayout = new javax.swing.GroupLayout(settingsPanelContainer);
        settingsPanelContainer.setLayout(settingsPanelContainerLayout);
        settingsPanelContainerLayout.setHorizontalGroup(
            settingsPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(settingsPanel)
        );
        settingsPanelContainerLayout.setVerticalGroup(
            settingsPanelContainerLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(settingsPanel)
        );


        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(contentPanelContainer, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(settingsPanelContainer, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(controlPanel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(settingsPanelContainer, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(contentPanelContainer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(controlPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        windowHandler = new WindowHandler();
        addWindowListener(windowHandler);
        pack();
    }
    
    // Einstellungen aus Properties Datei lesen
    public String pr(String proper) {
        prop = new Properties();
        InputStream input = null;
        try {
            input = new FileInputStream("config.properties");
            prop.load(input);
            return prop.getProperty(proper);
        } 
        catch (IOException ex) { } 
        finally {
            if(input!=null) {
                try {
                    input.close();
                } catch (IOException e) { }
            }
        }
        return "";
    }
    
    public void print_default(String str) {               
        Calendar now = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");        
        try { 
            kit.insertHTML(doc, doc.getLength(), "<div id='default'>[" + dateFormat.format(now.getTime()) + "] <font class='default'>" + str + "</font></div>", 0, 0, null); 
        } catch(Exception e) { 
            e.printStackTrace();
        }
        Chat.setCaretPosition(doc.getLength());
    }
    
    public void print_error(String str) { 
        SimpleAttributeSet errorString = new SimpleAttributeSet();
        StyleConstants.setForeground(errorString, Color.RED);
        StyleConstants.setBold(errorString, true);   
        Calendar now = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        try { 
            kit.insertHTML(doc, doc.getLength(), "[" + dateFormat.format(now.getTime()) + "] <font class='error'>" + str + "</font>", 0, 0, null); 
        } catch(Exception e) { 
            e.printStackTrace();
        }
        Chat.setCaretPosition(doc.getLength());
    }
    
    public void print_warning(String str) {
        SimpleAttributeSet warningString = new SimpleAttributeSet();
        StyleConstants.setForeground(warningString, Color.ORANGE);
        StyleConstants.setBold(warningString, true);   
        Calendar now = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        try {  
            kit.insertHTML(doc, doc.getLength(), "[" + dateFormat.format(now.getTime()) + "] <font class='warning'>" + str + "</font>", 0, 0, null);
        } catch(Exception e) { 
            e.printStackTrace();
        }
        Chat.setCaretPosition(doc.getLength());
    }
      
    public void print_private(String str) {
        SimpleAttributeSet privateString = new SimpleAttributeSet();
        StyleConstants.setBackground(privateString, Color.YELLOW);
        StyleConstants.setBold(privateString, true);   
        Calendar now = Calendar.getInstance();
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");        
        try {  
            kit.insertHTML(doc, doc.getLength(), "[" + dateFormat.format(now.getTime()) + "] <font class='private'>" + str + "</font>", 0, 0, null);
        } catch(Exception e) { 
            e.printStackTrace();
        }
        Chat.setCaretPosition(doc.getLength());
    }
      
    public boolean isWin32() {
        return System.getProperty("os.name").startsWith("Windows");
    }
    
    private String state() {
        return windowHandler.getActiveState();
    }
    
    // Verbinden
    private void ButtonVerbindenActionPerformed(ActionEvent evt) {
        serverAddr = FieldServer.getText(); port = Integer.parseInt(FieldPort.getText());        
        if(!serverAddr.isEmpty() && !FieldPort.getText().isEmpty()) {
            try{
                client = new SocketClient(this, doc);
                clientThread = new Thread(client);
                clientThread.start();
                client.send(new Message("test", "testUser", "testContent", "SERVER"));
            } catch(Exception ex) {
                print_error("Server nicht gefunden");
            }
        }
    }

    // Login
    private void ButtonLoginActionPerformed(ActionEvent evt) {
        username = FieldUsername.getText();
        password = FieldPasswort.getText();        
        if(!username.isEmpty() && !password.isEmpty()) {        
            client.send(new Message("login", username, password, "SERVER"));        
        }
    }

    //  Nachricht
    private void ButtonNachrichtActionPerformed(ActionEvent evt) {
        String msg = FieldNachricht.getText();
        String target = Userlist.getSelectedValue().toString();
        if(!msg.isEmpty() && !target.isEmpty()) {
            FieldNachricht.setText("");
            if(target=="All") {
                print_default(msg);
            } else {
                print_private(" an ["+target+"] " + msg);
            }
            client.send(new Message("message", username, msg, target));
            Chat.setCaretPosition(Chat.getDocument().getLength());
        }
    }

    // Registrieren
    private void ButtonRegistrierenActionPerformed(ActionEvent evt) {
        username = FieldUsername.getText();
        password = FieldPasswort.getText();        
        if(!username.isEmpty() && !password.isEmpty()) {
            client.send(new Message("signup", username, password, "SERVER"));
        }
    }

    // Upload
    private void ButtonDateiSuchenActionPerformed(ActionEvent evt) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.showDialog(this, "Datei suchen");
        file = fileChooser.getSelectedFile();        
        if(file != null){
            if(!file.getName().isEmpty()){
                ButtonDateiSenden.setEnabled(true); String str;                
                    if(FieldDatei.getText().length() > 30) {
                        String t = file.getPath();
                        str = t.substring(0, 20) + " [...] " + t.substring(t.length() - 20, t.length());
                    } else {
                        str = file.getPath();
                    }
                FieldDatei.setText(str);
            }
        }
    }

    // Upload senden
    private void ButtonDateiSendenActionPerformed(ActionEvent evt) {
            long size = file.length();
            client.send(new Message("upload_req", username, file.getName(), Userlist.getSelectedValue().toString()));
    }

    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch(Exception ex){
            System.out.println("UI Exception");
        }        
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Blabl().setVisible(true);
            }
        });
    }
    
}
