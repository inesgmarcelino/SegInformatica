import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class myAutent {
	
	public static void main(String[] args) {
		System.out.println("servidor: main");
		myAutent server = new myAutent();
		server.startServer();
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
								String line = "\r" + data.get("option_args");
								out.writeObject("O utilizador " + args[1] + " com o ID " + args[0] + " vai ser criado");
								if (!f.exists()) {
									new File("../server/" + args[0]).mkdirs();
									outFile.write(line.getBytes());
									out.writeObject("O utilizador " + args[1] + " foi criado");	
								} else {
									out.writeObject("O utilizador com ID " + args[0] + " já existe");
									System.out.println("O utilizador com ID " + args[0] + " já existe");
									System.exit(-1);
								}
							} else if (data.get("option").equals("e")) {
								out.writeObject(1);
								for (int i = 0; i < args.length; i++) {
									FileOutputStream fclient = new FileOutputStream("../server/" + data.get("user") + "/" + args[i]);
									BufferedOutputStream fBuff = new BufferedOutputStream(fclient);
									Long fSize = (Long) in.readObject();
									byte[] buffer = new byte[1024];
									
									int n = 0;
									int temp = fSize.intValue();
									while(temp > 0) {
										n = in.read(buffer, 0, (temp > 1024) ? 1024 : temp);
										fclient.write(buffer, 0, n);
										temp -= n;
									}
								}
								//stuff
								//pode ser mais q 1 ficheiro
								out.writeObject("O ficheiro " + args[0] + " foi enviado para o servidor");
							} else if (data.get("option").equals("l")) {
								out.writeObject(1);
								//stuff
//								out.writeObject();
							} else if (data.get("option").equals("d")) {
								out.writeObject(1);
								//stuff
								//pode ser mais q 1 ficheiro
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