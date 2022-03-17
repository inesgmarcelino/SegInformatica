import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
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
	
	public static void main(String[] args) {
		if(args.length==0) { //sem comandos
			System.out.println("Não foi escrito nenhum comando");
			System.exit(0);
		} 
		try {
			for (int i = 0; i < args.length; i++) {
				if (args[i].charAt(0) == '-') {
					
					switch (args[i].charAt(1)) {
					case 'u':
						userId = Integer.parseInt(args[i+1]);
						data.put("user", args[i+1]);
						break;
						
					case 'a':
						String[] address = args[i+1].split(":");
						host = address[0];
						data.put("ip", host);
						port = Integer.parseInt(address[1]);
						data.put("port", address[1]);
						
						clientSocket = new Socket (host,port);
						in = new ObjectInputStream(clientSocket.getInputStream());
		    			out = new ObjectOutputStream(clientSocket.getOutputStream());
						break;
						
					case 'p':
						userPwd = args[i+1];
						data.put("password", userPwd);
						break;
					}
					System.out.println(data);
					
					if (args[i].charAt(1) == 'c' && userId != 1) {
						System.out.println("Comando inválido, acesso restrito");
        				System.exit(-1);
						break;
					} else if (args[i].charAt(1) == 'c' || args[i].charAt(1) == 'e' || args[i].charAt(1) == 'd' || args[i].charAt(1) == 'l') {
						data.put("option", args[i].substring(1));
						System.out.println(i);
						StringBuilder sb = new StringBuilder();
						for (int j = i+1; j < args.length-1; j++) {
							sb.append(args[j]+";");
						}
						sb.append(args[args.length-1]);
						data.put("option_args", sb.toString());
					} else if (userPwd == null) {
						Scanner auth = new Scanner(System.in);
						System.out.println("Inserir password: ");
						userPwd = auth.nextLine();
						data.put("password",userPwd);
					}
					
					
				}
			}
			out.writeObject(data);
			out.close();
			in.close();
			clientSocket.close();
			System.out.println(data);
			
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}

}
