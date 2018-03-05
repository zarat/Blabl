import java.io.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import javax.xml.transform.OutputKeys; 
import org.w3c.dom.*;

// MD5
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Database {    
    public String filePath;    
    public Database(String filePath) {
        this.filePath = filePath;
    }    
    public boolean userExists(String username) {        
        try {
            File fXmlFile = new File(filePath);
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(fXmlFile);
            doc.getDocumentElement().normalize();            
            NodeList nList = doc.getElementsByTagName("user");            
            for (int temp = 0; temp < nList.getLength(); temp++) {
                Node nNode = nList.item(temp);
                if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode;
                    if(getTagValue("username", eElement).equals(username)) {
                        return true;
                    }
                }
            }
            return false;
        }
        catch(Exception e) {
            e.printStackTrace();
            //System.out.println("Database exception : userExists()");
            return false;
        }
    }    
    public boolean checkLogin(String username, String password) {        
        if(!userExists(username)) { 
            return false; 
        } 
        
        try {
        
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            byte byteData[] = md.digest();
            StringBuffer hexString = new StringBuffer();
        	for (int i=0;i<byteData.length;i++) {
        		String hex=Integer.toHexString(0xff & byteData[i]);
       	     	if(hex.length()==1) hexString.append('0');
       	     	hexString.append(hex);
        	}
        	String secretString = hexString.toString();                
           
            try {       
            
                File fXmlFile = new File(filePath);
                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
                Document doc = dBuilder.parse(fXmlFile);
                doc.getDocumentElement().normalize();            
                NodeList nList = doc.getElementsByTagName("user");            
                for (int temp = 0; temp < nList.getLength(); temp++) {
                    Node nNode = nList.item(temp);
                    if (nNode.getNodeType() == Node.ELEMENT_NODE) {
                        Element eElement = (Element) nNode;
                        if(getTagValue("username", eElement).equals(username) && getTagValue("password", eElement).equals(secretString)) {
                            return true;
                        }
                    }
                }
                System.out.println("Hippie");
                return false;
            }
            catch(Exception ex) {
                System.out.println("Database exception : userExists()");
                return false;
            }
        }catch(NoSuchAlgorithmException e) {}
        
        return false;
         
    }    
    public void addUser(String username, String password) {  
    
        try {
        
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(password.getBytes());
            byte byteData[] = md.digest();
            StringBuffer hexString = new StringBuffer();
        	for (int i=0;i<byteData.length;i++) {
        		String hex=Integer.toHexString(0xff & byteData[i]);
       	     	if(hex.length()==1) hexString.append('0');
       	     	hexString.append(hex);
        	}
        	String secretString = hexString.toString();

            try {
                DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
                DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
                Document doc = docBuilder.parse(filePath); 
                Node data = doc.getFirstChild();            
                Element newuser = doc.createElement("user");
                Element newusername = doc.createElement("username"); newusername.setTextContent(username);
                Element newpassword = doc.createElement("password"); newpassword.setTextContent(secretString);            

                newuser.appendChild(newusername); 
		newuser.appendChild(newpassword); 
		data.appendChild(newuser);          

                TransformerFactory transformerFactory = TransformerFactory.newInstance();
                Transformer transformer = transformerFactory.newTransformer();

		// https://stackoverflow.com/questions/22790146/create-xml-file-with-linebreaks
		transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transformer.setOutputProperty(OutputKeys.INDENT, "yes");

                DOMSource source = new DOMSource(doc);
                StreamResult result = new StreamResult(new File(filePath));
                transformer.transform(source, result); 
            } catch(Exception ex) {
                System.out.println("Exceptionmodify xml");
            }
            
        }catch(NoSuchAlgorithmException e) {}        
        
    }    
    public static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
        Node nValue = (Node) nlList.item(0);
        return nValue.getNodeValue();
    }
}
