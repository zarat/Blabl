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

// RSA Encryption
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.*;

// message splitter
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Iterator;

public class SocketClient implements Runnable{
    
    public int port;
    public String serverAddr;
    public Socket socket;  
    public Blabl ui;
    public StyledDocument doc;
    public ObjectInputStream In;
    public ObjectOutputStream Out;
    
    public String username;
    
    private KeyPair keyPair;
    private PublicKey publicKey;
    private PrivateKey privateKey;  
    // just need to be a string  
    private String serverpublicKey;
    
    public SocketClient(Blabl frame, StyledDocument doc) throws IOException {
    
        // generate our keypair and save them for later usage
        try { 
            keyPair = buildKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
        } catch(Exception e) {}  
    
        ui = frame; 
        serverAddr = ui.serverAddr; 
        port = ui.port; 
        doc = doc;
        socket = new Socket(InetAddress.getByName(serverAddr), port);            
        Out = new ObjectOutputStream(socket.getOutputStream());
        Out.flush();
        In = new ObjectInputStream(socket.getInputStream());                              
    }
    
    public static KeyPair buildKeyPair() throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {    
        // generate our keypair
        KeyGenerator keyGenerator = KeyGenerator.getInstance("Blowfish");
        keyGenerator.init(448);
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(1024);
        KeyPair keyPair = keyPairGenerator.genKeyPair();
        return keyPair;       
    }
    
    public static String getEncrypted(String data, String Key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(Key.getBytes())));
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedbytes = cipher.doFinal(data.getBytes());
        return new String(Base64.getEncoder().encode(encryptedbytes));
    }

    public static String getDecrypted(String data, String Key) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        PrivateKey pk = KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(Base64.getDecoder().decode(Key.getBytes())));
        cipher.init(Cipher.DECRYPT_MODE, pk);
        byte[] encryptedbytes = cipher.doFinal(Base64.getDecoder().decode(data.getBytes()));
        return new String(encryptedbytes);
    }
    
    @Override
    public void run() {
        boolean keepRunning = true;
        ui.threadRunning = true;
        while(keepRunning) {
            try { 
                       
                Message msg = (Message) In.readObject();                
                System.out.println("Incoming : " + msg);
                                
                if(msg.type.equals("message")) { 
                

            String[] split = msg.content.split(":");
            StringBuilder sb = new StringBuilder();
            
            for (int i = 0; i < split.length; i++) {
            
                // decrypt the creds
                String priKey = new String(Base64.getEncoder().encode(privateKey.getEncoded()));
                String decryptedText = "";
                try {
                    decryptedText = getDecrypted(split[i], priKey);
                }
                catch(NoSuchAlgorithmException nsae) {}
                catch(NoSuchPaddingException nspe) {}
                catch(InvalidKeyException ike) {}
                catch(InvalidKeySpecException ikse) {}
                catch(IllegalBlockSizeException ibse) {}
                catch(BadPaddingException bpe) {}
                msg.content = decryptedText;
            
                sb.append(decryptedText);
                
                if (i != split.length - 1) {
                    //sb.append(" ");
                }
                
            }
            
            String joined = sb.toString();
            System.out.println(joined);
            msg.content = joined;
                
		                   
                    if(msg.recipient.equals(ui.username)) {
                        ui.print_private("["+ msg.sender + "] " + msg.content);                             
                    } else {
                        ui.print_default("["+msg.sender +"] " + msg.content);
                    }

                    if(msg.content.equals(".bye") && msg.sender.equals(ui.username)) {
                        ui.clientThread.stop();
                        ui.threadRunning = false;
                    }
                }
                
                // login
                else if(msg.type.equals("login")) {
                    if(msg.content.equals("TRUE")) {
                        ui.ButtonLogin.setEnabled(false); 
                        ui.ButtonRegistrieren.setEnabled(false);                        
                        ui.ButtonNachricht.setEnabled(true); 
                        ui.ButtonDateiSuchen.setEnabled(true);
                  			ui.FieldUsername.setEditable(false);
                  			ui.FieldUsername.setEnabled(false);
                  			ui.FieldPasswort.setEditable(false);
                  			ui.FieldPasswort.setEnabled(false);
                        ui.print_default("Du bist als " + msg.recipient + " angemeldet.");
                        username = msg.recipient;
                        String pk = new String(Base64.getEncoder().encode(publicKey.getEncoded()));
                        send(new Message("publickey", username, pk, "SERVER"));
                    } else {
                        ui.print_error("Du konntest nicht angemeldet werden.");
                    }
                    
                }
                
                // we have received the test response and the servers public key, so we can start encrypting our messages. YEY
                //
                // serverkey should be base64 encoded so we can sent it as a string                
                else if(msg.type.equals("test")) {

                    ui.ButtonVerbinden.setEnabled(false);                    
                    ui.ButtonLogin.setEnabled(true); 
                    ui.ButtonRegistrieren.setEnabled(true);                    
                    ui.FieldUsername.setEnabled(true); 
                    ui.FieldPasswort.setEnabled(true);                    
                    ui.FieldServer.setEditable(false); 
                    ui.FieldPort.setEditable(false);                   
                    ui.print_default("Verbindung hergestellt."); 
                    ui.print_warning("ServerKey erhalten, ausgehende Inhalte werden nun verschl&uuml;sselt");
                    serverpublicKey = msg.content;                                      

                }
                
                else if(msg.type.equals("publickey")) {
                  
                    ui.print_warning("Eigenen PublicKey an den Server &uuml;bertragen. Alle Inhalte werden nun verschl&uuml;sselt");                                       

                }
                                
                // new user has entered the room, so we have to update the userlist
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
                            ui.print_default(msg.content + " ist jetzt online");
                        }
                    } 
                }
                
                // we have got a response from server to our signup request
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
                
                // logout
                else if(msg.type.equals("signout")) {
                    if(msg.content.equals(ui.username)) {
                        ui.print_default("Du hast den Chat verlassen");
                        ui.ButtonVerbinden.setEnabled(true); 
                        ui.ButtonNachricht.setEnabled(false); 
                        ui.FieldServer.setEditable(true); 
                        ui.FieldServer.setEnabled(true);
                        ui.FieldPort.setEditable(true);                        
                        ui.FieldPort.setEnabled(true);
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
                
                // someone want to send us a file
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
                
                // we have had sent a file to someone and got a response
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
                
                // anything else we dont know
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
    
    public void send_encrypted(Message msg) {
    
        String text = msg.content;
        String erg = "";
        int index = 0;
        while (index < text.length()) {
        
            String part = text.substring(index, Math.min(index + 100,text.length()));
            
            try {
                String cipherText = getEncrypted(part, serverpublicKey);
                part = cipherText;            
            }
            catch(NoSuchAlgorithmException nsae) {}
            catch(NoSuchPaddingException nspe) {}
            catch(InvalidKeyException ike) {}
            catch(InvalidKeySpecException ikse) {}
            catch(IllegalBlockSizeException ibse) {}
            catch(BadPaddingException bpe) {}
        
            erg += part + ":";
            index += 100;
            
        }
        
        msg.content = erg;
        
        /*
        String text = msg.content;
        List<String> strings = new ArrayList<String>();
        int index = 0;
        while (index < text.length()) {
            strings.add(text.substring(index, Math.min(index + 100,text.length())));
            index += 100;
        }
        System.out.println(strings.toString());
        
        List<String> stringsarray = new ArrayList<String>();
        Iterator<String> i = strings.iterator();
        
        while ( i.hasNext() ) {
        
            String item = i.next();
        
            try {
                String cipherText = getEncrypted(item, serverpublicKey);
                item = cipherText;            
            }
            catch(NoSuchAlgorithmException nsae) {}
            catch(NoSuchPaddingException nspe) {}
            catch(InvalidKeyException ike) {}
            catch(InvalidKeySpecException ikse) {}
            catch(IllegalBlockSizeException ibse) {}
            catch(BadPaddingException bpe) {}
            
            stringsarray.add(item);
        
        }
        
        msg.content = "" + stringsarray;
        */
        
        try {
            Out.writeObject(msg);
            Out.flush();
            System.out.println("Outgoing : "+msg.toString());
        }catch(IOException ioe) { }
    
    }
    
    private static List<String> getParts(String string, int partitionSize) {    
        List<String> parts = new ArrayList<String>();
        int len = string.length();
        for (int i=0; i<len; i+=partitionSize) {
            parts.add(string.substring(i, Math.min(len, i + partitionSize)));
        }
        return parts;
    }

    public void closeThread(Thread t){
        t = null;
        ui.threadRunning = false;
    }
    
}
