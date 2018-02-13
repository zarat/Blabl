import java.io.*;
import java.net.*;
import java.*;
import javax.swing.*;

import java.util.Properties;

// RSA Encryption
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.crypto.*;


public class Server extends Thread implements Runnable {
    
    public ClientThread clients[];
    public ServerSocket server = null;
    public Thread thread = null; 
    public int clientCount = 0;
    public int port;
    public GUI logger; 
    public Properties prop = new Properties();
    public Database db;
    
    // RSA Encryption
    public KeyPair keyPair;
    public PublicKey publicKey;
    public PrivateKey privateKey;
    
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

    public Server(GUI log) {
    
        logger = log;
    
        // generate our keypair and save them for later usage
        try { 
            keyPair = buildKeyPair();
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();
            logger.log("KeyPair erzeugt");
        } catch(Exception e) {}
    
        port = new Integer(getProp("port"));
        int maxclients = new Integer(getProp("maxusers"));
        clients = new ClientThread[maxclients];
        
        db = new Database(getProp("userdb"));
    	  try{  
    	      server = new ServerSocket(port);
    	      start(); 
        } catch(IOException ioe) { }
    }
    
    public void run() {  
  	    while (thread != null){  
            try {   
                addThread(server.accept()); 
            } catch(Exception ioe){ }
        }
      	if (thread == null){  
            thread = new Thread(this); 
  	        thread.start();
  	    }
    }	
    
    public String getProp(String p) {
        InputStream input = null;    
        try {    
            input = new FileInputStream("server.ini");
            prop.load(input);    
            return prop.getProperty(p);    
        } catch (IOException ex) { ex.printStackTrace(); } 
        finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) { e.printStackTrace(); }
            }
        }    
        return "";   
    }  
    	
    public void stopen() {
        for(int i = 0; i < clientCount; i++) {
            try { clients[i].close(); }catch(IOException ioe) { ioe.printStackTrace(); }
        }
        try {
            server.close();
        } catch(IOException ioe) { ioe.printStackTrace(); }
    }
    
    private void addThread(Socket socket) {  
      	if (clientCount < clients.length) {  
      	    clients[clientCount] = new ClientThread(this, socket); 
      	    try{ 
      	      	clients[clientCount].open(); 
      	        clients[clientCount].start();  
                clientCount++;
      	    } catch(IOException ioe) { } 
      	} else { }
    }
    
    public synchronized void handle(int ID, Message msg) {
    
        System.out.println("Incoming : " + msg);
    
        // user logout
        if (msg.content.equals(".bye")) {
            Announce("signout", "SERVER", msg.sender);
            remove(ID); 
            logger.log(msg.sender + " ist offline");
        } else {
        
            // user login
            if(msg.type.equals("login")) {
            
                // decrypt the creds
                String priKey = new String(Base64.getEncoder().encode(privateKey.getEncoded()));
                String decryptedText = "";
                try {
                    decryptedText = getDecrypted(msg.content, priKey);
                }
                catch(NoSuchAlgorithmException nsae) {}
                catch(NoSuchPaddingException nspe) {}
                catch(InvalidKeyException ike) {}
                catch(InvalidKeySpecException ikse) {}
                catch(IllegalBlockSizeException ibse) {}
                catch(BadPaddingException bpe) {}
                msg.content = decryptedText;
            
                if(findUserThread(msg.sender) == null) {
                    if(db.checkLogin(msg.sender, msg.content)) {
                        clients[findClient(ID)].username = msg.sender;
                        clients[findClient(ID)].send(new Message("login", "SERVER", "TRUE", msg.sender));
                        Announce("newuser", "SERVER", msg.sender);
                        SendUserList(msg.sender);
                        logger.log(msg.sender + " hat sich angemeldet");
                    } else {
                        clients[findClient(ID)].send(new Message("login", "SERVER", "FALSE", msg.sender));
                    }     
                } else {
                    clients[findClient(ID)].send(new Message("login", "SERVER", "FALSE", msg.sender));
                }
                
            }             
            
            // test message after connection is established.. 
            else if(msg.type.equals("test")) {
                
                // convert our public key to a byte array
                byte[] encodedPublicKey = publicKey.getEncoded();
                // then to base64 encoded string
                String base64PublicKey = Base64.getEncoder().encodeToString(encodedPublicKey);
                // and send it to the client
                clients[findClient(ID)].send(new Message("test", "SERVER", base64PublicKey, msg.sender));
                logger.log("Neue Verbindung");
                    
            }
            // after test message, the user send his publickey 
            else if(msg.type.equals("publickey")) {
                
                clients[findClient(ID)].publicKey = msg.content;
                // and send it to the client
                clients[findClient(ID)].send(new Message("publickey", "SERVER", "OK", msg.sender));
                logger.log("PublicKey von " + msg.sender + " erhalten");
                    
            }
                        
            // user has sent a message
            //
            // message is encrypted with our public key
            // so first decrypt it using our private key
            else if(msg.type.equals("message")) {
            
                // decrypt the creds
                String priKey = new String(Base64.getEncoder().encode(privateKey.getEncoded()));
                String decryptedText = "";
                try {
                    decryptedText = getDecrypted(msg.content, priKey);
                }
                catch(NoSuchAlgorithmException nsae) {}
                catch(NoSuchPaddingException nspe) {}
                catch(InvalidKeyException ike) {}
                catch(InvalidKeySpecException ikse) {}
                catch(IllegalBlockSizeException ibse) {}
                catch(BadPaddingException bpe) {}
                msg.content = decryptedText;
 
                // to all users
                if(msg.recipient.equals("All")) {

                    AnnounceEncrypted("message", msg.sender, msg.content);
                    
                // or a single user
                } else {
                    
                    String noHTMLString = msg.content.replaceAll("\\<.*?\\>", "");                                    
                    findUserThread(msg.recipient).send_encrypted(new Message(msg.type, msg.sender, noHTMLString, msg.recipient));                    
                    //clients[findClient(ID)].send(new Message(msg.type, msg.sender, noHTMLString, msg.recipient));                    
                    
                }                
            }
            
            // user signup
            else if(msg.type.equals("signup")) {
            
                // decrypt the creds
                String priKey = new String(Base64.getEncoder().encode(privateKey.getEncoded()));
                String decryptedText = "";
                try {
                    decryptedText = getDecrypted(msg.content, priKey);
                }
                catch(NoSuchAlgorithmException nsae) {}
                catch(NoSuchPaddingException nspe) {}
                catch(InvalidKeyException ike) {}
                catch(InvalidKeySpecException ikse) {}
                catch(IllegalBlockSizeException ibse) {}
                catch(BadPaddingException bpe) {}
                msg.content = decryptedText;
            
                if(findUserThread(msg.sender) == null) {
                
                    if(!db.userExists(msg.sender)){
                        db.addUser(msg.sender, msg.content);
                        clients[findClient(ID)].username = msg.sender;
                        clients[findClient(ID)].send(new Message("signup", "SERVER", "TRUE", msg.sender));
                        String IP = findUserThread(msg.sender).socket.getInetAddress().getHostAddress();
                        clients[findClient(ID)].send(new Message("login", "SERVER", "TRUE", msg.sender));
                        
                        Announce("newuser", "SERVER", msg.sender);
                        SendUserList(msg.sender);
                        logger.log(msg.sender + " hat sich registriert");
                        
                    } else {
                        clients[findClient(ID)].send(new Message("signup", "SERVER", "FALSE", msg.sender));
                    }
                } else {                
                    clients[findClient(ID)].send(new Message("signup", "SERVER", "FALSE", msg.sender));               
                }

            } 
            
            // upload request
            else if(msg.type.equals("upload_req")) {
                if(msg.recipient.equals("All")) {
                    Announce("upload_req", msg.sender, msg.content);  
                } else {
                    findUserThread(msg.recipient).send(new Message("upload_req", msg.sender, msg.content, msg.recipient));
                }
            } 
            
            // upload response
            else if(msg.type.equals("upload_res")) {
                if(!msg.content.equals("NO")){
                    String IP = findUserThread(msg.sender).socket.getInetAddress().getHostAddress();
                    findUserThread(msg.recipient).send(new Message("upload_res", IP, msg.content, msg.recipient));
                } else {
                    findUserThread(msg.recipient).send(new Message("upload_res", msg.sender, msg.content, msg.recipient));
                }
            }
            
        }
    }
    
    public void Announce(String type, String sender, String content) {    
        Message msg = new Message(type, sender, content, "All");  
        for(int i = 0; i < clientCount; i++){        
            if(clients[i].username != msg.sender) {                 
            		clients[i].send(msg);                
	          }            
        }
    }

    public void AnnounceEncrypted(String type, String sender, String content) {    
        //Message msg = new Message(type, sender, content, "All");
        for(int i = 0; i < clientCount; i++){        
            if(clients[i].username != sender) {  
          			// encrypt it using users public key
          			String encryptedText = "";
          			try {
          			    encryptedText = getEncrypted(content, clients[i].publicKey);
          			}
          			catch(NoSuchAlgorithmException nsae) {}
          			catch(NoSuchPaddingException nspe) {}
          			catch(InvalidKeyException ike) {}
          			catch(InvalidKeySpecException ikse) {}
          			catch(IllegalBlockSizeException ibse) {}
          			catch(BadPaddingException bpe) {}          
          			Message msg = new Message(type, sender, encryptedText, "All");               
            		clients[i].send(msg);                
	          }            
        }
    }
    
    public void SendUserList(String to) {
        for(int i = 0; i < clientCount; i++){
            findUserThread(to).send(new Message("newuser", "SERVER", clients[i].username, to));
        }
    }
    
    private int findClient(int ID) {  
    	for (int i = 0; i < clientCount; i++){
        	if (clients[i].getID() == ID){
                return i;
            }
	    }
	    return -1;
    }
        
    public ClientThread findUserThread(String usr) {
        for(int i = 0; i < clientCount; i++) {
            if(clients[i].username.equals(usr)) {
                return clients[i];
            }
        }
        return null;
    }
    	
    @SuppressWarnings("deprecation")
    public synchronized void remove(int ID) {  
        int pos = findClient(ID);
        if (pos >= 0){  
            ClientThread toTerminate = clients[pos];
            if (pos < clientCount-1) {
                for (int i = pos+1; i < clientCount; i++) {
                    clients[i-1] = clients[i];
                }
            }
            clientCount--;
            try {  
                toTerminate.close(); 
            } catch(IOException ioe) {  
            }
            toTerminate.stop(); 
        }
    }    

}
                          
class ClientThread extends Thread { 
	
    public Server server = null;
    public Socket socket = null;
    
    public int ID = -1;
    public String username = "";
    public String mailbox = "";
    public String userpass = "";
    public String full_username = "";

    public ObjectInputStream r  =  null;
    public ObjectOutputStream w = null;
    public String publicKey;

    public ClientThread(Server _server, Socket _socket) {  
    	  super();
        server = _server;
        socket = _socket;
        ID = socket.getPort();
    }
          
    @SuppressWarnings("deprecation")
	  public void run() {  
        while (true){  
    	      try {  
                Message line = (Message)r.readObject();
        	    	server.handle(ID, line); 
            }
            catch(Exception ioe){  
                server.remove(ID);
                stop();
            }
        }
    } 
       
    public void open() throws IOException {  
        r = new ObjectInputStream(socket.getInputStream());
        w = new ObjectOutputStream(socket.getOutputStream());
        w.flush();
        
    }
       
    public void close() throws IOException {  
    	  if (socket != null) socket.close();
        if (r != null) r.close();
        if (w != null) w.close();
    }
    
    public void send(Message msg) {
        System.out.println("Outgoing : " + msg);
        try {
            w.writeObject(msg);
            w.flush();
        } 
        catch (IOException e) {
            System.out.println("Exception [SocketClient : send(...)]");
        }
    }
    
    public static String getEncrypted(String data, String Key) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidKeySpecException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        PublicKey publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(Base64.getDecoder().decode(Key.getBytes())));
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        byte[] encryptedbytes = cipher.doFinal(data.getBytes());
        return new String(Base64.getEncoder().encode(encryptedbytes));
    }
    
    public void send_encrypted(Message msg) {
        String encryptedText = "";
        try {
            encryptedText = getEncrypted(msg.content, publicKey);
        }
        catch(NoSuchAlgorithmException nsae) {}
        catch(NoSuchPaddingException nspe) {}
        catch(InvalidKeyException ike) {}
        catch(InvalidKeySpecException ikse) {}
        catch(IllegalBlockSizeException ibse) {}
        catch(BadPaddingException bpe) {}
        msg.content = encryptedText;
        
        try {
            w.writeObject(msg);
            w.flush();
            System.out.println("Outgoing : " + msg);
        } 
        catch (IOException e) {
            System.out.println("Exception [SocketClient : send(...)]");
        }
    }
            
    public int getID() {  
        return ID;
    }
    
    public String getUsername() {  
        return username;
    }
    
    public String getPublicKey() {
        return publicKey;
    }
    
}
