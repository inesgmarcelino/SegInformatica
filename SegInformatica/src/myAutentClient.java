import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

public class myAutentClient {
    
    public static void main(String[] args) {
    	if (args.length >= 4) {
    		Socket socket = null;
    		try {
    			String host = null;
    			int port = 0;
    			int user = 1; //default -> admin
//    			String password = null;
//    			List<String> newUser = new ArrayList<String>();
//    			List<String> files = new ArrayList<String>();
//    			char option = 0;
    			HashMap<String,String> data = new HashMap<String,String>();
    			
    			//user and address
        		if (args[0].equals("-u") && args[2].equals("-a")) {
    				data.put("user", args[1]);
    				user = Integer.parseInt(args[1]);
    				host = args[3].substring(0, 9);
    				port = Integer.parseInt(args[3].substring(10));
    			} else {
    				System.out.println("comando inválido");
    				System.exit(-1);
    			}
        		
        		socket = new Socket(host,port);
    			ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
    			ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        		
        		if (args.length > 4) {
        			//password
        			if (args[4].equals("-p")) {
        				data.put("password", args[5]);
        				
        				if (args[6].equals("-c") && user != 1) { //admin access only
        					System.out.println("comando inválido");
            				System.exit(-1);
        				}
        				
        				data.put("option", args[6].substring(1));
        				StringBuilder sb = new StringBuilder();
        				for (int i = 7; i < args.length-1; i++) {
        					sb.append(args[i]+";");
        				}
        				sb.append(args[args.length-1]);
        				data.put("option_args", sb.toString());
        				
        			} else {
        				Scanner scn = new Scanner(System.in);
        				System.out.println("Insere a password: ");
        				data.put("password",scn.nextLine());
        				
        				if (args[4].equals("-c") && user != 1) { //admin access only
        					System.out.println("comando inválido");
            				System.exit(-1);
        				}
        				
        				data.put("option", args[4].substring(1));
        				StringBuilder sb = new StringBuilder();
        				for (int i = 5; i < args.length-1; i++) {
        					sb.append(args[i]+";");
        				}
        				sb.append(args[args.length-1]);
        				data.put("option_args", sb.toString());
        		
        			}
        			
        		}
        		
        		out.writeObject(data);
        		out.close();
        		in.close();
        		socket.close();
    			
    		} catch (IOException e) {
    			System.err.println(e.getMessage());
    			System.exit(-1);
    		}
    	
    	} else {
    		System.out.println("comando inválido");
    	}
    }
}
