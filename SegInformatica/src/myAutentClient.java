import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.Scanner;

public class myAutentClient {
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
					
					default:
						if (userPwd == null) {
							Scanner auth = new Scanner(System.in);
							System.out.println("Inserir password: ");
							userPwd = auth.nextLine();
							data.put("password",userPwd);
						}
						
						if (args[i].charAt(1) == 'c' && userId != 1) {
							System.out.println("Comando inválido, acesso restrito");
							System.exit(-1);
							break;
						} else if (args[i].charAt(1) == 'c' || args[i].charAt(1) == 'e' || args[i].charAt(1) == 'd' || args[i].charAt(1) == 'l') {
							data.put("option", args[i].substring(1));
							StringBuilder sb = new StringBuilder();
							for (int j = i+1; j < args.length-1; j++) {
								if (sb.length() == 2) {
									sb.append(args[j]+" ");
								} else {
									sb.append(args[j]+";");									
								}
							}
							sb.append(args[args.length-1]);
							data.put("option_args", sb.toString());
						}
						break;
					}
					
					
					// e se houver comandos q n existem...
				}
			}
			out.writeObject(data);
			if (data.get("option").equals("e")) {
				String[] files = data.get("option_args").split(";");
				for (String file: files) {
					File f = new File(file);
					Long tam = f.length();
					out.writeObject(tam);
					
					BufferedInputStream fBuff = new BufferedInputStream(new FileInputStream(file));
					byte[] buffer = new byte[1024];
					
					int n;
					while ((n = fBuff.read(buffer, 0, 1024)) > 0) {
						out.write(buffer,0,n);
					}
				}
			}
			int count = (int) in.readObject();
			for (int i = 0; i < count; i++) {
				System.out.println((String) in.readObject());
			}
			
			out.close();
			in.close();
			clientSocket.close();
			
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}

}