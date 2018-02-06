import java.io.*;
import java.net.*;
import java.awt.*;
import java.util.Date;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

// Zeitangaben
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

// StyledDocument fuer HTML Support
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.BadLocationException;
import java.awt.Color;

import java.io.*;
import java.net.URL;
import javax.sound.sampled.*;

public class SocketClient implements Runnable{
    
    public int port;
    public String serverAddr;
    public Socket socket;  
    public Blabl ui;
    public StyledDocument doc;
    public ObjectInputStream In;
    public ObjectOutputStream Out;
    
    public SocketClient(Blabl frame, StyledDocument doc) throws IOException {
        ui = frame; 
        serverAddr = ui.serverAddr; 
        port = ui.port; 
        doc = doc;
        socket = new Socket(InetAddress.getByName(serverAddr), port);            
        Out = new ObjectOutputStream(socket.getOutputStream());
        Out.flush();
        In = new ObjectInputStream(socket.getInputStream());                              
    }
    
    @Override
    public void run() {
        boolean keepRunning = true;
        ui.threadRunning = true;
        while(keepRunning) {
            try {
                Message msg = (Message) In.readObject();                
                if(msg.type.equals("message")) {
                    if(msg.recipient.equals(ui.username)) {
                        if(!msg.sender.equals(ui.username)) {
                            if(ui.windowHandler.getActiveState() == "iconified") {
                                if(ui.pr("sound").equals("on")) {                            
                                    ui.print_private("["+ msg.sender + "] " + msg.content);
                                    System.out.println(ui.pr("sound"));
                                    try {
                                        URL url = this.getClass().getClassLoader().getResource("res/pm.wav");
                                        AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
                                        Clip clip = AudioSystem.getClip();
                                        clip.open(audioIn);
                                        clip.start();
                                    } catch (UnsupportedAudioFileException e) {
                                        e.printStackTrace();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    } catch (LineUnavailableException e) {
                                        e.printStackTrace();
                                    } catch(Exception e) { e.printStackTrace(); }
                                    System.out.println("error #4");                                                                        
                                } else {
                                ui.print_private("["+ msg.sender + "] " + msg.content);
                                System.out.println(ui.pr("sound"));
                                }                               
                            } else {
                                ui.print_private("["+ msg.sender + "] " + msg.content);
                            }
                        } 
                    } else {
                        if(!msg.sender.equals(ui.username)) {
                            ui.print_default("["+msg.sender +"] " + msg.content);
                        }
                    }
                    if(msg.content.equals(".bye") && msg.sender.equals(ui.username)) {
                        ui.clientThread.stop();
                        ui.threadRunning = false;
                    }
                }
                else if(msg.type.equals("login")) {
                    if(msg.content.equals("TRUE")) {
                        ui.ButtonLogin.setEnabled(false); 
                        ui.ButtonRegistrieren.setEnabled(false);                        
                        ui.ButtonNachricht.setEnabled(true); 
                        ui.ButtonDateiSuchen.setEnabled(true);
                        ui.print_default("Hallo " + msg.recipient + ", " + ui.pr("welcome"));                        
                        ui.FieldUsername.setEnabled(false); 
                        ui.FieldPasswort.setEnabled(false);
                    }
                    else{
                        ui.print_error("Anmeldefehler");
                    }
                }
                else if(msg.type.equals("test")) {
                    ui.ButtonVerbinden.setEnabled(false);                    
                    ui.ButtonLogin.setEnabled(true); 
                    ui.ButtonRegistrieren.setEnabled(true);                    
                    ui.FieldUsername.setEnabled(true); 
                    ui.FieldPasswort.setEnabled(true);                    
                    ui.FieldServer.setEditable(false); 
                    ui.FieldPort.setEditable(false);
                }
                else if(msg.type.equals("newuser")) {
                    if(!msg.content.equals(ui.username)) {
                        boolean exists = false;
                        for(int i = 0; i < ui.model.getSize(); i++) {
                            if(ui.model.getElementAt(i).equals(msg.content)) {
                                exists = true; 
                                break;
                            }
                        }
                        if(!exists){ 
                            ui.model.addElement(msg.content);
                            ui.print_default(msg.content + " ist online");
                        }
                    } 
                }
                else if(msg.type.equals("signup")) {
                    if(msg.content.equals("TRUE")) {
                        ui.ButtonLogin.setEnabled(false); ui.ButtonRegistrieren.setEnabled(false);
                        ui.ButtonNachricht.setEnabled(true); ui.ButtonDateiSuchen.setEnabled(true);
                        ui.print_default("erfolgreich registriert");
                    }
                    else{
                        ui.print_error("Registrierungsfehler");
                    }
                }
                else if(msg.type.equals("signout")) {
                    if(msg.content.equals(ui.username)) {
                        ui.print_default("Du hast den Chat verlassen");
                        ui.ButtonVerbinden.setEnabled(true); 
                        ui.ButtonNachricht.setEnabled(false); 
                        ui.FieldServer.setEditable(true); 
                        ui.FieldPort.setEditable(true);                        
                        for(int i = 1; i < ui.model.size(); i++){
                            ui.model.removeElementAt(i);
                        }                        
                        ui.clientThread.stop();
                        ui.threadRunning = false;
                    }
                    else{
                        ui.model.removeElement(msg.content);
                        ui.print_default(msg.content + " ist offline");
                    }
                }
                else if(msg.type.equals("upload_req")) {                    
                    if(JOptionPane.showConfirmDialog(ui, ("Willst du '"+msg.content+"' von "+msg.sender+" herunterladen?")) == 0) {                        
                        JFileChooser jf = new JFileChooser();
                        jf.setSelectedFile(new File(msg.content));
                        int returnVal = jf.showSaveDialog(ui);                       
                        String saveTo = jf.getSelectedFile().getPath();
                        if(saveTo != null && returnVal == JFileChooser.APPROVE_OPTION){
                            Download dwn = new Download(saveTo, ui);
                            Thread t = new Thread(dwn);
                            t.start();
                            send(new Message("upload_res", ui.username, (""+dwn.port), msg.sender));
                        }
                        else{
                            send(new Message("upload_res", ui.username, "NO", msg.sender));
                        }
                    }
                    else{
                        send(new Message("upload_res", ui.username, "NO", msg.sender));
                    }
                }
                else if(msg.type.equals("upload_res")) {
                    if(!msg.content.equals("NO")) {
                        int port  = Integer.parseInt(msg.content);
                        String addr = msg.sender;                        
                        ui.ButtonDateiSuchen.setEnabled(false); ui.ButtonDateiSenden.setEnabled(false);
                        Upload upl = new Upload(addr, port, ui.file, ui);
                        Thread t = new Thread(upl);
                        t.start();
                    }
                    else{
                        ui.print_default("[SERVER]: "+msg.sender+" hat den Download abgelehnt");
                    }
                }
                else{
                    ui.print_error("unbekannter Fehler");
                }
            }
            catch(Exception ex) {
                keepRunning = false;
                ui.print_error("Verbindungsfehler");
                ui.ButtonVerbinden.setEnabled(true); ui.FieldServer.setEditable(true); ui.FieldPort.setEditable(true);
                ui.ButtonNachricht.setEnabled(false); ui.ButtonDateiSuchen.setEnabled(false); ui.ButtonDateiSenden.setEnabled(false);                
                for(int i = 1; i < ui.model.size(); i++) {
                    ui.model.removeElementAt(i);
                }                
                ui.clientThread.stop();                
                ex.printStackTrace();
                ui.threadRunning = false;
            }
        }
    }    
    public void send(Message msg){
        try {
            Out.writeObject(msg);
            Out.flush();
            System.out.println("Outgoing : "+msg.toString());            
        } 
        catch (IOException ex) {
            System.out.println("Exception SocketClient send()");
        }
    }    
    public void closeThread(Thread t){
        t = null;
        ui.threadRunning = false;
    }
}
