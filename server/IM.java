import javax.swing.*;
import java.awt.*; 
import java.awt.event.*;
import java.*;
import java.io.*; 
import java.net.*;

import java.util.Date;
import java.text.SimpleDateFormat;

import javax.swing.text.DefaultCaret;
import java.io.BufferedWriter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.WindowAdapter;

// XML Verarbeitung
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

// DOM - für XML?
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class IM { 

    public static void main(String[] args) {     
        JFrame gui = new GUI(); 
        gui.show();        
    } 
    
} 


class GUI extends JFrame { 
    
    public JButton XMLButton;
    public JButton ServerButton;
    public JButton CLRButton;
    
    public JScrollPane scrollPanel;
    public JTextArea logger;
    public DefaultCaret caret;
    
    public Server server;
    
    public TrayIcon trayIcon;
    public SystemTray tray;

    public String filePath = "./Data.xml";
    public JFileChooser fileChooser;

    public GUI() { 
    
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
        } catch (InstantiationException ex) {
        } catch (IllegalAccessException ex) {
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
        }    
    
        setTitle("IM 0.1 Server alpha"); 
        //setPreferredSize(new Dimension(700,300));
        setResizable(false);
        if(SystemTray.isSupported()){

            tray=SystemTray.getSystemTray();
            URL imageURL = GUI.class.getResource("im.png");
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
            trayIcon=new TrayIcon(image, "IM", popup);
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
        URL imageURL = GUI.class.getResource("im.png");
        setIconImage(Toolkit.getDefaultToolkit().getImage(imageURL));                
        
        fileChooser = new JFileChooser();
                 
        XMLButton = new JButton();
        ServerButton = new JButton();
        CLRButton = new JButton();
        
        scrollPanel = new JScrollPane();
        logger = new JTextArea();
        
        
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) { 
                closeWindow();
            }
        });        
        
        logger.setColumns(20);
        logger.setFont(new java.awt.Font("Consolas", 0, 12)); 
        logger.setRows(5); 
        logger.setLineWrap(true);
        logger.setWrapStyleWord(true);
        logger.setEditable(false);
        logger.setFocusable(false);
        
        caret = (DefaultCaret)logger.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
                                 
        scrollPanel.setViewportView(logger);
        
        XMLButton.setText("Datenbank");
        XMLButton.setEnabled(true);
        XMLButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                XMLButtonAction(evt);
            }
        });
        
        ServerButton.setText("Server Start");
        ServerButton.setEnabled(true);
        ServerButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ServerButtonAction(evt);
            }
        }); 
        
        CLRButton.setText("LOG leeren");
        CLRButton.setEnabled(true);
        CLRButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                CLRButtonAction(evt);
            }
        });
       
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(scrollPanel) 
                    .addGroup(GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(XMLButton, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(ServerButton, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(CLRButton, GroupLayout.PREFERRED_SIZE, 120, GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(XMLButton)
                    .addComponent(ServerButton)
                    .addComponent(CLRButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(scrollPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 187, Short.MAX_VALUE)
                .addContainerGap())
        );
        pack();        
        setLocationRelativeTo(null);

    } 
    
    public void closeWindow() {
        String ObjButtons[] = {"Beenden","abbrechen"};
        int PromptResult = JOptionPane.showOptionDialog(null,"Wollen Sie die SimpleMail wirklich beenden?","SimpleMail beenden",JOptionPane.DEFAULT_OPTION,JOptionPane.WARNING_MESSAGE,null,ObjButtons,ObjButtons[0]);
        if(PromptResult==JOptionPane.YES_OPTION) {
            System.exit(0);
        }    
    }
    
    private void XMLButtonAction(java.awt.event.ActionEvent evt) {    
        fileChooser.setCurrentDirectory(new File("."));        
        File XMLConfigFile = new File("./Data.xml"); 
        if (!XMLConfigFile.exists()) {
            try {        
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();                
                Document doc = docBuilder.newDocument();
                Element rootElement = doc.createElement("data");
                doc.appendChild(rootElement);                
                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();
                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File("./Data.xml"));                
                transformer.transform(source, result);                        
        	} catch (ParserConfigurationException pce) {
                pce.printStackTrace();
            } catch (TransformerException tfe) {
                tfe.printStackTrace();
            }
        }                 
        fileChooser.showDialog(this, "suchen..");
        File file = fileChooser.getSelectedFile();        
        if(file != null) {
            filePath = file.getPath().replace("\\", "/"); 
        }            
    }
    
    private void ServerButtonAction(java.awt.event.ActionEvent evt) {    
        String action = evt.getActionCommand();
        if (action.equals("Server Start")) {
            server = new Server(this);
            ServerButton.setText("Server Stop");
        }  
        if (action.equals("Server Stop")) {
           server.stopen();
           ServerButton.setText("Server Start");
        }            
    }
    
    private void CLRButtonAction(java.awt.event.ActionEvent evt) {    
        logger.setText("");            
    }
    
    public void popup(ActionEvent e) {
        String action = e.getActionCommand().toLowerCase();
        boolean i = editFile(new File(action+".ini"));
    }
    
    public boolean editFile(final File file) {
      if (!Desktop.isDesktopSupported()) {
        return false;
      }
    
      Desktop desktop = Desktop.getDesktop();
      if (!desktop.isSupported(Desktop.Action.EDIT)) {
        return false;
      }
    
      try {
        desktop.edit(file);
      } catch (IOException e) {
        // Log an error
        return false;
      }
    
      return true;
    }

    public void log(String s) {
    
        String logdate = new SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        logger.append("[" + logdate + "] " + s + "\n");
    
    }
    
}
