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
import java.util.HashMap;

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
					String linha = br.readLine();
					while(linha != null) {
						String[] linha_split = linha.split(";");
						if (data.get("user").equals(linha_split[0]) && data.get("password").equals(linha_split[2])) {
							FileOutputStream outFile = new FileOutputStream(file,true);
							
							if (data.get("option").equals("c")) {
								String line = "\r" + data.get("option_args");
								String[] args = data.get("option_args").split(";");
								new File("../clientFiles/" + args[0]).mkdirs();
								outFile.write(line.getBytes());
							}		
							outFile.close();
						}
						linha = br.readLine();
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