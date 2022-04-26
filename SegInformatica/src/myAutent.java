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
import java.math.BigInteger;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class myAutent {
	static int adminId = 0;
	static String adminPwd;
	static File file = new File("./src/users.txt");
	
	public static void main(String[] args) throws IOException {
		Scanner auth = new Scanner(System.in);
		if (args.length == 0) {
			System.out.println("Admin ID: ");
			adminId = auth.nextInt();
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
		ObjectInputStream in;
		ObjectOutputStream out;
		
		ServerThread (Socket client) {
			socket = client;
			System.out.println("thread do server para cada cliente");
		}
		
		@SuppressWarnings("unchecked")
		public void run() {
			try {
				out = new ObjectOutputStream(socket.getOutputStream());
				in = new ObjectInputStream(socket.getInputStream());
				
				HashMap<String,String> data = null;
				
				try {
					data = (HashMap<String, String>) in.readObject();
					System.out.println(data);
					
					BufferedReader br = usersRegistered();
					String l = br.readLine();
					while (l != null) {
						String[] linha_split = l.split(";");
						if (data.get("user").equals(linha_split[0])) {// && data.get("password").equals(linha_split[2])) {
							
							if (data.get("password").equals(linha_split[2])) {
								String[] args = data.get("option_args").split(";");
								
								if (data.get("option").equals("c")) {
									out.writeObject(2);
									out.writeObject("O utilizador " + args[1] + " com o ID " + args[0] + " vai ser criado");
									boolean valid = opcao_c(args);
									if (valid) {
										out.writeObject("O utilizador " + args[1] + " foi criado");
									} else {
										out.writeObject("O utilizador com ID " + args[0] + " já existe");
										System.exit(-1);
									}
									
								}  else if (data.get("option").equals("d")) {
									out.writeObject(args.length);
									for (String arg: args) {
										out.flush();
										opcao_d(data.get("user"), arg);
										out.writeObject("O ficheiro " + args[0] + " foi recebido pelo cliente");
									}
									
								} else if (data.get("option").equals("e")) {
									List<String> existed = new ArrayList<String>();
									List<String> created = new ArrayList<String>();
									for (String arg: args) {
										out.flush();
										File f = new File("./server/" + data.get("user") + "/" + arg);
										if (!f.exists()) {
											opcao_e(f);
											created.add(arg);
										} else {
											existed.add(arg);
										}
									}
									
									if (created.size() > 0) {
										out.writeObject(created.size());
										for (String f: created) {
											out.writeObject("O ficheiro " + f + " foi enviado para o servidor");
										}
									}
									
									if (existed.size() > 0) {
										out.writeObject(existed.size());		
										for (String f: existed) {
											out.writeObject("O ficheiro " + f + " já existe no servidor");										
										}										
									}
									
								} else if (data.get("option").equals("l")) {
									out.writeObject(1);			
									out.writeObject(opcao_l(data.get("user")));
									
								} else if (data.get("option").equals("s")) {
									byte[] hash = (byte[]) in.readObject();
									System.out.println(hash);
									
									out.writeObject(1);
									out.writeObject("hash enviado");
									
								} else if (data.get("option").equals("v")) {
									
								}
								
							} else {
								out.writeObject(1);
								out.writeObject("Credenciais inválidas!");
							}
						}
						l = br.readLine();
					}
					
					
				} catch (ClassNotFoundException | NoSuchAlgorithmException | OperatorCreationException | CertificateException | KeyStoreException e) {
					e.printStackTrace();
				}
				out.close();
				in.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public boolean opcao_c(String[] args) 
				throws IOException, NoSuchAlgorithmException, OperatorCreationException, CertificateException, KeyStoreException {
			
			File f = new File("./server/" +  args[0]);
			boolean valid = true;
			
			BufferedReader br = usersRegistered();
			String l = br.readLine();
			while (l != null) {
				String[] linha_split = l.split(";");
				if (linha_split[0].equals(args[0])) {
					valid = false;
				}
				l = br.readLine();
			}
			
			String line = "\r" + args[0] + ";" + args[1] + ";" + encrypt(args[2]);
			if (!f.exists() & valid) {
				f.mkdirs();
				FileOutputStream outFile = new FileOutputStream(file,true); //users.txt
				outFile.write(line.getBytes());
				outFile.close();
				createKeystore(args[0],args[2]);
				return true;
				
			} else {
				return false;
			}
		}
		
		public void opcao_d(String id, String file) throws IOException {
			File f = new File("./server/" +  id + "/" + file);
			Long tam = f.length();
			out.writeObject(tam);
			
			BufferedInputStream fBuff = new BufferedInputStream(new FileInputStream(f.getPath()));
			byte[] buffer = new byte[1024];
			
			int n;
			while ((n = fBuff.read(buffer, 0 , 1024)) > 0) {
				out.write(buffer,0,n);
			}
		}
		
		public void opcao_e(File f) throws ClassNotFoundException, IOException {
			FileOutputStream fclient = new FileOutputStream(f.getPath());
			BufferedOutputStream fBuff = new BufferedOutputStream(fclient);
			Long fSize = (Long) in.readObject();
			byte[] buffer = new byte[1024];
			
			int n = 0;
			int temp = fSize.intValue();
			while (temp > 0) {
				System.out.println(temp);
				n = in.read(buffer, 0, (temp > 1024) ? 1024: temp);
				fclient.write(buffer,0,n);
				temp -= n;
			}
			fclient.close();
		}
		
		public String opcao_l(String id) throws IOException {
			StringBuilder sb = new StringBuilder();
			File path = new File("./server/" +  id);
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
			
			return sb.toString();
		}
		
		public void opcao_s() {
			
		}
		
		public void opcao_v() {
			
		}
		
		public String encrypt(String pwd) 
				throws NoSuchAlgorithmException {
			
			SecureRandom rand = new SecureRandom();
			byte[] salt = new byte[16];
			rand.nextBytes(salt);
			
			MessageDigest md = MessageDigest.getInstance("SHA");
			md.update(salt);
			
			byte[] bytes = md.digest(pwd.getBytes());
			String encrypted = Base64.getEncoder().encodeToString(bytes) + "," + Base64.getEncoder().encodeToString(salt);
			
			return encrypted;
		}
		
		public void createKeystore(String id, String pwd) 
				throws NoSuchAlgorithmException, OperatorCreationException, CertificateException, KeyStoreException, IOException {

//			gera chaves assimetricas RSA
			KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
			keyPairGenerator.initialize(2048);
			KeyPair keyPair = keyPairGenerator.generateKeyPair();
				
//			define informacao para o certificado
			X500Name dnName = new X500Name("CN=" + "Grupo013");
		    BigInteger certSerialNumber = BigInteger.valueOf(System.currentTimeMillis());
		    Instant startDate = Instant.now();
		    Instant endDate = startDate.plus(2 * 365, ChronoUnit.DAYS);
		    
//		    classe que assina o certificado - certifcado auto assinado
		    String signatureAlgorithm = "SHA256WithRSA";
		    ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate());
		    
//		    cria o certificado
		    X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
		        dnName, certSerialNumber, Date.from(startDate), Date.from(endDate), dnName,
		        keyPair.getPublic());
		    Certificate certificate = new JcaX509CertificateConverter().getCertificate(certBuilder.build((ContentSigner)contentSigner));
		    
//		    guarda chave privada + certificado na keystore 
		    KeyStore kstore = KeyStore.getInstance("JKS");
		    if ((new File("./server/" +id+ "/" +id+ ".keystore")).exists()){  // **** file da keystore
				FileInputStream kfile1 = new FileInputStream("./server/" +id+ "/" +id+ ".keystore"); 
				kstore.load(kfile1, "123456".toCharArray()); // **** password da keystore
				kfile1.close();
		    } else {
				kstore.load(null, null); // **** caso em que o file da keystore ainda n�o existe
		    }
		    		
			Certificate chain [] = {certificate, certificate};
			
			// **** atencao ao alias do user e 'a password da chave privada
			kstore.setKeyEntry(pwd, (Key)keyPair.getPrivate(), pwd.toCharArray(), chain);
			FileOutputStream kfile = new FileOutputStream("./server/" +id+ "/" +id+ ".keystore"); // keystore
			kstore.store(kfile, pwd.toCharArray());
		}
	}
	
	
}