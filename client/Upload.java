import java.io.*;
import java.net.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Upload implements Runnable {

    public String addr;
    public int port;
    public Socket socket;
    public FileInputStream In;
    public OutputStream Out;
    public File file;
    public Blabl ui;
    
    public Upload(String addr, int port, File filepath, Blabl frame) {
        super();
        try {
            file = filepath; ui = frame;
            socket = new Socket(InetAddress.getByName(addr), port);
            Out = socket.getOutputStream();
            In = new FileInputStream(filepath);
        } 
        catch (Exception ex) { ex.printStackTrace(); }
    }    
    @Override
    public void run() {
        try {       
            byte[] buffer = new byte[1024];
            int count;
            while((count = In.read(buffer)) >= 0){
                Out.write(buffer, 0, count);
            }
            Out.flush();            
            ui.print_warning("[Application] Upload fertig");
            ui.ButtonDateiSuchen.setEnabled(true); ui.ButtonDateiSenden.setEnabled(true);
            ui.FieldDatei.setVisible(true);            
            if(In != null){ In.close(); }
            if(Out != null){ Out.close(); }
            if(socket != null){ socket.close(); }
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }
}
