import java.io.*;
import java.net.*;
import java.*;
import javax.swing.*;

import java.util.Properties;

public class Server extends Thread implements Runnable {
    
    public ClientThread clients[];
    public ServerSocket server = null;
    public Thread thread = null; 
    public int clientCount = 0;
    public int port;
    public GUI logger; 
    public Properties prop = new Properties();
    public Database db;

    public Server(GUI log) {
        port = new Integer(getProp("port"));
        int maxclients = new Integer(getProp("maxusers"));
        clients = new ClientThread[maxclients];
        logger = log;
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
        if (msg.content.equals(".bye")) {
            Announce("signout", "SERVER", msg.sender);
            remove(ID); 
        } else {
            if(msg.type.equals("login")) {
                if(findUserThread(msg.sender) == null) {
                    if(db.checkLogin(msg.sender, msg.content)) {
                        clients[findClient(ID)].username = msg.sender;
                        clients[findClient(ID)].send(new Message("login", "SERVER", "TRUE", msg.sender));
                        Announce("newuser", "SERVER", msg.sender);
                        SendUserList(msg.sender);
                    } else {
                        clients[findClient(ID)].send(new Message("login", "SERVER", "FALSE", msg.sender));
                    }     
                } else {
                    clients[findClient(ID)].send(new Message("login", "SERVER", "FALSE", msg.sender));
                }
            } else if(msg.type.equals("message")) {
                if(msg.recipient.equals("All")) {
                    String noHTMLString = msg.content.replaceAll("\\<.*?\\>", "");
                    Announce("message", msg.sender, noHTMLString);
                } else {
                    String noHTMLString = msg.content.replaceAll("\\<.*?\\>", "");                
                    findUserThread(msg.recipient).send(new Message(msg.type, msg.sender, noHTMLString, msg.recipient));
                    clients[findClient(ID)].send(new Message(msg.type, msg.sender, noHTMLString, msg.recipient));
                }
            } else if(msg.type.equals("test")) {
		clients[findClient(ID)].send(new Message("test", "SERVER", "OK", msg.sender));
            } else if(msg.type.equals("signup")) {
                if(findUserThread(msg.sender) == null) {
                    if(!db.userExists(msg.sender)){
                        db.addUser(msg.sender, msg.content);
                        clients[findClient(ID)].username = msg.sender;
                        clients[findClient(ID)].send(new Message("signup", "SERVER", "TRUE", msg.sender));
                        String IP = findUserThread(msg.sender).socket.getInetAddress().getHostAddress();
                        clients[findClient(ID)].send(new Message("login", "SERVER", "TRUE", msg.sender));
                        Announce("newuser", "SERVER", msg.sender);
                        SendUserList(msg.sender);
                    } else {
                        clients[findClient(ID)].send(new Message("signup", "SERVER", "FALSE", msg.sender));
                    }
                } else {                
                    clients[findClient(ID)].send(new Message("signup", "SERVER", "FALSE", msg.sender));               
                }

            } else if(msg.type.equals("upload_req")) {
                if(msg.recipient.equals("All")) {
                    Announce("upload_req", msg.sender, msg.content);  
                } else {
                    findUserThread(msg.recipient).send(new Message("upload_req", msg.sender, msg.content, msg.recipient));
                }
            } else if(msg.type.equals("upload_res")) {
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
            if(clients[i].username != sender) { clients[i].send(msg); }
        }
    }
    
    public void SendUserList(String toWhom) {
        for(int i = 0; i < clientCount; i++){
            findUserThread(toWhom).send(new Message("newuser", "SERVER", clients[i].username, toWhom));
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
        try {
            w.writeObject(msg);
            w.flush();
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
    
}
