import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;


public class client {
	
	static int port;
	static String host;
	
	static int userId;
	static String userPwd;
	
	static Socket clientSocket;
	static ObjectInputStream in;
	static ObjectOutputStream out;
	
	static HashMap<String,String> data = new HashMap<String,String>();
	
	/* myAutentClient -u <userId> -a <serverAddress:port> [ -p <password> | -c <userId> <nome> <password> | -l
	| -e {<filename_1>, ..., <filename_n>} | -d {<filename_1>, ..., <filename_n>} ] */

	
	public static void main(String[] args) throws IOException {
		
		if(args.length==0) { //sem comandos
			System.out.println("Não foi escrito nenhum comando");
			System.exit(0);
		} 
		
		for(int i = 0; i < args.length; i++) { 
			if(args[i].charAt(0) == '-') { 
				switch(args[i].substring(1, args[i].length())) {
				case "u": //user ID
					userId = Integer.parseInt(args[i+1]);
					data.put("user", Integer.toString(userId)); 
					break;
					
				case "a": // server port + try connection
					try {
						host = args[i+1].split(":")[0];
						data.put("ip", host); 
						
						port = Integer.parseInt(args[i+1].split(":")[1]);
						data.put("server", Integer.toString(port)); 
						
						clientSocket = new Socket(host,port);
						
		    			ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
		    			ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());
		    			
					} catch(Exception e){
						System.out.println(e.getMessage());
		    			System.exit(-1);
					}
					break;
					
				case "p":
					userPwd = args[i+1];
					data.put("password", userPwd);
					break;
					
				case "c": 
					if(userId != 1) {
						System.out.println("Comando inválido, acesso restrito");
        				System.exit(-1);
						break;
					}
					
					data.put("option", args[6].substring(1));
    				StringBuilder sb = new StringBuilder();
    				
    				for (int j = 7; j < args.length-1; j++) {
    					sb.append(args[j]+";");
    				}
    				
    				sb.append(args[args.length-1]);
    				data.put("option_args", sb.toString());
					break;
					
				default:
					System.out.format("%s Não é um comando válido\n", args[i].substring(0, args[i].length()));
				}		
			}
		}
		
		if(userPwd == null) { //sem password
			Scanner auth = new Scanner(System.in);
			System.out.println("Inserir password: ");
			data.put("password",auth.nextLine());
		}
		
		out.writeObject(data);
		out.close();
		in.close();
		clientSocket.close();
		
	}
}

