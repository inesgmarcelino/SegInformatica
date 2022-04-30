import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Signature;
import java.util.HashMap;
import java.util.Scanner;

public class myAutentClient {
	static int port;
	static String host;
	
	static int userId = 0;
	static String userPwd;
	
	static Socket clientSocket;
	static ObjectInputStream in;
	static ObjectOutputStream out;
	
	static String mainPath = "./client/";
	
	static HashMap<String,String> data = new HashMap<String,String>();
	
	public static void main(String[] args) {
		if(args.length==0) { //sem comandos
			System.out.println("N�o foi escrito nenhum comando");
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
						if (userId > 0 && userPwd == null) {
							Scanner auth = new Scanner(System.in);
							System.out.println("Inserir password: ");
							userPwd = auth.nextLine();
							data.put("password",userPwd);
						}
						
						if (args[i].charAt(1) == 'c' && userId != 1) {
							System.out.println("Comando inv�lido, acesso restrito");
							System.exit(-1);
						} else if (args[i].charAt(1) == 'c' || args[i].charAt(1) == 'e' || 
								args[i].charAt(1) == 'd' || args[i].charAt(1) == 'l' ||
								args[i].charAt(1) == 's' || (userId == 0 && args[i].charAt(1) == 'v')) {
							
							data.put("option", args[i].substring(1));
							StringBuilder sb = new StringBuilder();
							for (int j = i+1; j < args.length-1; j++) {
								if (args.length - (i+1) == 4 && j == i+2) {
									sb.append(args[j]+" ");
								} else {
									sb.append(args[j]+";");									
								}
							}
							sb.append(args[args.length-1]);
							data.put("option_args", sb.toString());
						} else {
							System.out.println("Comando inv�lido!");
							System.exit(-1);
						}
						break;
					}
				}
			}
			out.writeObject(data);
			
			String bool = (String) in.readObject();
			if (bool.equals("-1")) {
				System.out.println((String) in.readObject());
				System.exit(-1);
				
			} else {
				
				if (data.get("option").equals("c")) {
					System.out.println((String) in.readObject());
					System.out.println((String) in.readObject());
				}
				
				if (data.get("option").equals("d")) {
					String[] files = data.get("option_args").split(";");
					for (String file: files) {
						out.flush();
						File f = new File (mainPath + userId + "/" + file);
						opcao_d(f);
					}
				}
				
				if (data.get("option").equals("e")) {
					String[] files = data.get("option_args").split(";");
					for (String file: files) {
						opcao_e(file);
					}
					out.flush();
				}
				
				if (data.get("option").equals("l")) {
					System.out.println((String) in.readObject());
				}
				
				if (data.get("option").equals("s")) {
					String[] files = data.get("option_args").split(";");
					for (String file: files) {
						opcao_s(file);
					}
					out.flush();
				}
			}
			
			out.close();
			in.close();
			clientSocket.close();
			
		} catch (Exception e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
	}
	
	public static void opcao_d(File f) throws ClassNotFoundException, IOException {
		if (!f.exists()) {
			receiveFromServer(f);	
			System.out.println("O ficheiro " + f.getName() + " foi recebido pelo cliente");
		} else {
			in.readObject(); //ignorar			
			System.out.println("O ficheiro " + f.getName() + " j� existe no cliente");
		}
		
		String name = f.getName().substring(0, f.getName().indexOf("."));
		receiveSignature(name);
	}
	
	public static void opcao_e(String file) throws IOException, ClassNotFoundException {
		File f = new File(mainPath + userId + "/" +  file);
		sendToServer(f);
		out.flush();
		String name = file.substring(0, file.indexOf("."));
		receiveSignature(name);
	}
	
	public static void opcao_s(String file) throws NoSuchAlgorithmException, IOException, ClassNotFoundException {
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		DigestInputStream dis = new DigestInputStream( new FileInputStream(mainPath + userId + "/" + file), md);
		while (dis.read() != -1) {
			md = dis.getMessageDigest();
		}
		out.writeObject(md.digest());
		
		System.out.println((String) in.readObject());
		String name = file.substring(0, file.indexOf("."));
		receiveSignature(name);
	}
	
	public void opcao_v() {
		
	}
	
	public static void receiveFromServer(File f) throws ClassNotFoundException, IOException {
		FileOutputStream fserver = new FileOutputStream(f.getPath());
		BufferedOutputStream fBuff = new BufferedOutputStream(fserver);
		Long fSize = (Long) in.readObject();
		byte[] buffer = new byte[1024];
		
		int n = 0;
		int temp = fSize.intValue();
		while (temp > 0) {
			n = in.read(buffer, 0, (temp > 1024) ? 1024: temp);
			fserver.write(buffer, 0, n);
			temp -= n;
		}
		fserver.close();
	}
	
	public static void receiveSignature(String nameFile) throws IOException, ClassNotFoundException {
		String nameSig = nameFile + ".signed.user"+userId;
		FileOutputStream signature = new FileOutputStream(mainPath + userId + "/" +  nameSig);
		ObjectOutputStream oos = new ObjectOutputStream(signature);
		byte[] s = (byte[]) in.readObject();
		oos.writeObject(s);
		signature.close();
		System.out.println("A assinatura " + nameSig + " foi recebida pelo cliente");
	}
	
	public static void sendToServer(File f) throws IOException, ClassNotFoundException {
		Long tam = f.length();
		out.writeObject(tam);
		
		BufferedInputStream fBuff = new BufferedInputStream(new FileInputStream(f.getPath()));
		byte[] buffer = new byte[1024];
		
		int n;
		while ((n = fBuff.read(buffer, 0, 1024)) > 0) {
			out.write(buffer,0,n);
		}
		System.out.println((String) in.readObject());
	}

}