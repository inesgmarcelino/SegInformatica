import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class myAutent {
	static int adminId = 0;
	static String adminPwd;
	static File file = new File("users.txt");
	
	public static void main(String[] args) {
		Scanner auth = new Scanner(System.in);
		if (args.length == 0) {
			System.out.println("Admin ID: ");
			adminId = Integer.valueOf(auth.next());
			System.out.println("Admin password: ");
			adminPwd = auth.next();
		} else {
			for (int i = 0; i < args.length; i++) {
				if (args[i].charAt(0) == '-') {
					switch(args[i].charAt(1)) {
					case 'u':
						adminId = Integer.parseInt(args[i+1]);
						break;
					case 'p':
						adminPwd = args[i+1];
						break;
					}
				}
			}
			if (adminId == 0) {
				System.out.println("Admin ID: ");
				adminId = auth.nextInt();
			} else if (adminPwd == null) {
				System.out.println("Admin password: ");
				adminPwd = auth.next();
			}
		}
		
		try {
			BufferedReader br = usersRegistered();
			String l = br.readLine();
			if (l != null) {
				String[] linha_split = l.split(";");
				if (Integer.parseInt(linha_split[0]) == adminId && linha_split[1].equals("Administrador") && linha_split[2].equals(adminPwd)) {
					System.out.println("servidor: main");
					myAutent server = new myAutent();
					server.startServer();					
				} else {
					System.out.println("Credenciais inválidas!");
					System.exit(0);
				}
			} else {
				String line = adminId + ";Administrador;" + adminPwd;
				FileOutputStream outFile = new FileOutputStream(file,true);
				outFile.write(line.getBytes());
				outFile.close();
				
				System.out.println("servidor: main");
				myAutent server = new myAutent();
				server.startServer();
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static BufferedReader usersRegistered() {
		BufferedReader br = null;
		try {
			FileInputStream inFile = new FileInputStream(file);
			InputStreamReader reader = new InputStreamReader(inFile);
			br = new BufferedReader(reader);
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		return br;
	}
	
	public void startServer() {
		ServerSocket servSock = null;
		
		try {
			servSock = new ServerSocket(23456);
			System.out.println("Servidor iniciado no port 23456");
		} catch (IOException e) {
			System.err.println(e.getMessage());
			System.exit(-1);
		}
		
		while (true) {
			try {
				Socket client = servSock.accept();
				ServerThread thread = new ServerThread(client);
				thread.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//servSock.close();
	}
	
	class ServerThread extends Thread {
		private Socket socket = null;
		
		ServerThread (Socket client) {
			socket = client;
			System.out.println("thread do server para cada cliente");
		}
		
		@SuppressWarnings("unchecked")
		public void run() {
			try {
				ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
				ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
				
				HashMap<String,String> data = null;
				
				try {
					data = (HashMap<String, String>) in.readObject();
					System.out.println(data);
					
					File file = new File("users.txt");
					FileInputStream inFile = new FileInputStream(file);
					InputStreamReader reader = new InputStreamReader(inFile);
					BufferedReader br = new BufferedReader(reader);
					String l = br.readLine();
					while (l != null) {
						String[] linha_split = l.split(";");
						if (data.get("user").equals(linha_split[0]) && data.get("password").equals(linha_split[2])) {
							FileOutputStream outFile = new FileOutputStream(file,true);
							String[] args = data.get("option_args").split(";");
							
							if (data.get("option").equals("c")) {
								out.writeObject(2);
								File f = new File("../server/" +  args[0]);
								File f2 = new File("../client/" + args[0]);
								String line = "\r" + data.get("option_args");
								out.writeObject("O utilizador " + args[1] + " com o ID " + args[0] + " vai ser criado");
								if (!f.exists() && !f2.exists()) {
									f.mkdirs();
									f2.mkdirs();
									outFile.write(line.getBytes());
									out.writeObject("O utilizador " + args[1] + " foi criado");	
								} else {
									out.writeObject("O utilizador com ID " + args[0] + " já existe");
									System.exit(-1);
								}
							} else if (data.get("option").equals("e")) {
								out.writeObject(1);
								for (String arg: args) {
									out.flush();
									File f = new File("../server/" + data.get("user") + "/" + arg);
									if (!f.exists()) {
										FileOutputStream fclient = new FileOutputStream(f.getPath());
										BufferedOutputStream fBuff = new BufferedOutputStream(fclient);
										Long fSize = (Long) in.readObject();
										byte[] buffer = new byte[1024];
										
										int n = 0;
										int temp = fSize.intValue();
										while (temp > 0) {
											n = in.read(buffer, 0, (temp > 1024) ? 1024: temp);
											fclient.write(buffer,0,n);
											temp -= n;
										}
									} else {
										out.writeObject("O ficheiro " + arg + " já existe no servidor");
										System.exit(-1);
									}
								}
								
								out.writeObject("O ficheiro " + args[0] + " foi enviado para o servidor");
							} else if (data.get("option").equals("l")) {
								out.writeObject(1);
								StringBuilder sb = new StringBuilder();
								File path = new File("../server/" +  data.get("user"));
								for (File f: path.listFiles()) {
									Path p = Paths.get(f.getPath());
									BasicFileAttributes at = Files.readAttributes(p, BasicFileAttributes.class);
									String info = at.lastModifiedTime().toString();
									String date = info.substring(8, 10) + "/" + info.substring(5, 7) + "/" + info.substring(0, 4);
									String hour = info.substring(11, 16);
									sb.append(date + "  " + hour + "  " + p.getFileName());
									sb.append("\n");
								}
								sb.delete(sb.length()-1, sb.length());
								
								out.writeObject(sb.toString());
							} else if (data.get("option").equals("d")) {
								for (int i = 0; i < args.length; i++) {
									out.flush();
									for (String arg: args) {
										File f = new File("../server/" +  data.get("user") + "/" + arg);
										Long tam = f.length();
										out.writeObject(tam);
										
										BufferedInputStream fBuff = new BufferedInputStream(new FileInputStream(f.getPath()));
										byte[] buffer = new byte[1024];
										
										int n;
										while ((n = fBuff.read(buffer, 0 , 1024)) > 0) {
											out.write(buffer,0,n);
										}
									}
									
								}
								//stuff
								//pode ser mais q 1 ficheiro
								out.writeObject(1);
								out.writeObject("O ficheiro " + args[0] + " foi recebido pelo cliente");
							}
							outFile.close();
						}
						l = br.readLine();
					}
					
					
				} catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
				out.close();
				in.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
}