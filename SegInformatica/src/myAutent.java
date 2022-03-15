import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
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
					
					FileOutputStream outFile = new FileOutputStream("users.txt");
					byte[] buffer = new byte[1024];
					if (data.get("option").equals("c")) {
						String line = data.get("option_args");
						outFile.write(line.getBytes());
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