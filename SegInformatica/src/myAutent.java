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
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLServerSocketFactory;

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
	static String MACPwd;
	static String mainPath = "./server/";
	static File file = new File(mainPath + "users.txt");
	
	public static void main(String[] args) throws IOException, NumberFormatException, NoSuchAlgorithmException {
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
			boolean valid = false;
			if (file.exists()) {
				BufferedReader br = usersRegistered();
				String l = br.readLine();
				if (l != null) {
					String[] linha_split = l.split(";");
					if (Integer.parseInt(linha_split[0]) == adminId && linha_split[1].equals("Administrador") && checkPwd(linha_split[2], adminPwd)) {
						valid = true;					
					} else {
						System.out.println("Credenciais inválidas!");
						System.exit(0);
					}
				} else {
					String line = adminId + ";Administrador;" + encrypt(adminPwd);
					FileOutputStream outFile = new FileOutputStream(file,true);
					outFile.write(line.getBytes());
					outFile.close();
					valid = true;
				}
			} else {
				String line = adminId + ";Administrador;" + encrypt(adminPwd);
				FileOutputStream userstxt = new FileOutputStream(file.getPath());
				userstxt.write(line.getBytes());
				userstxt.close();
				valid = true;
			}
			
			if (valid) {
				myAutent server = new myAutent();
				server.startServer();
			}
		} catch (IOException | InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	public static BufferedReader usersRegistered() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		macExists();
		
		FileInputStream inFile = new FileInputStream(file);
		InputStreamReader reader = new InputStreamReader(inFile);
		BufferedReader br = new BufferedReader(reader);

		return br;
	}
	
	public static String encrypt(String pwd) throws NoSuchAlgorithmException {
		SecureRandom rand = new SecureRandom();
		byte[] salt = new byte[16];
		rand.nextBytes(salt);
		
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(salt);
		
		byte[] bytes = md.digest(pwd.getBytes());
		String encrypted = Base64.getEncoder().encodeToString(bytes) + "," + Base64.getEncoder().encodeToString(salt);
		
		return encrypted;
	}
	
	public static boolean checkPwd(String crypt, String pwd) throws NoSuchAlgorithmException {
		String[] crypted = crypt.split(",");
		String salt = crypted[1];
		byte[] salt_b = Base64.getDecoder().decode(salt);
		
		MessageDigest md = MessageDigest.getInstance("SHA-256");
		md.update(salt_b);
		byte[] bytes = md.digest(pwd.getBytes());
		
		String pwd_crypted = Base64.getEncoder().encodeToString(bytes);
		return pwd_crypted.equals(crypted[0]);
	}
	
	public static void createMAC(String pwd, String f) throws NoSuchAlgorithmException, InvalidKeyException, IOException {
		FileOutputStream macFile = new FileOutputStream(mainPath + f);
		byte[] bytes = pwd.getBytes();
		
		SecretKey key = new SecretKeySpec(bytes, 0, bytes.length, "AES");
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(key);
		
		FileInputStream userstxt = new FileInputStream(file.getPath());
		BufferedInputStream bis = new BufferedInputStream(userstxt);
		
		byte[] buff = new byte[1024];
		int x;
		while ((x = bis.read(buff, 0, 1024)) > 0) {
			mac.update(buff, 0, x);
		}
		
		macFile.write(mac.doFinal());
		bis.close();
		userstxt.close();
		macFile.close();
	}
	
	public static boolean compareMACs(String mac1, String mac2) throws IOException {
		Path macP1 = Paths.get(mainPath + mac1);
		Path macP2 = Paths.get(mainPath + mac2);
		if (Files.size(macP1) != Files.size(macP2)) {
			return false;
		} else if (Files.size(macP1) < 2048) {
			return Arrays.equals(Files.readAllBytes(macP1), Files.readAllBytes(macP2));
		}
		
		BufferedReader buff1 = Files.newBufferedReader(macP1);
		BufferedReader buff2 = Files.newBufferedReader(macP2);
		String line;
		while ((line = buff1.readLine()) != null) {
//			mt duvidoso
			if (line != buff2.readLine()) {
				return false;
			}
		}
		return true;
	}
	
	public static void macExists() throws InvalidKeyException, NoSuchAlgorithmException, IOException {
		File mac = new File(mainPath + "users.mac");
		if (!mac.exists()) {
			Scanner in = new Scanner(System.in);
			System.out.println("Pretende calcular o MAC? (y/n)");
			String resp = in.next();
			in.close();
			if (resp.equals("y")) {
				createMAC(adminPwd, "users.mac");
				MACPwd = adminPwd;
			} else {
				System.exit(-1);
			}
		}
	}
	
	public void startServer() {
		System.setProperty("javax.net.ssl.keyStore", "server/keystores/keystore.server");
		System.setProperty("javax.net.ssl.keyStorePassword", "123456");
		
		ServerSocket servSock = null;
		
		try {
			macExists();
			createMAC(adminPwd, "users2.mac");
			File mac2 = new File(mainPath + "users2.mac");
			boolean compare = compareMACs("users.mac", "users2.mac");
				
			if (!compare) {
				System.out.println("Não é possível iniciar o servidor.\nOs dados do ficheiros users.txt foram alterados sem permissão.");
				mac2.delete();
				System.exit(-1);
			}
			
			MACPwd = adminPwd;
			mac2.delete();
			
			//servSock = new ServerSocket(23456);
			
			servSock = ((SSLServerSocketFactory)SSLServerSocketFactory.getDefault()).createServerSocket(23456);

			
			System.out.println("Servidor iniciado no port 23456");	
			
		} catch (IOException | InvalidKeyException | NoSuchAlgorithmException e) {
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
		String clientId;
		
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
					clientId = data.get("user");
					
					BufferedReader br = usersRegistered();
					String l = br.readLine();
					boolean idExists = false;
					while (l != null) {
						String[] linha_split = l.split(";");
						if (clientId.equals(linha_split[0])) {
							idExists = true;
							out.writeObject("0");
							if (checkPwd(linha_split[2], data.get("password"))) {
								String[] args = data.get("option_args").split(";");
								
								if (data.get("option").equals("c")) {
									opcao_c(args);
									
								}  else if (data.get("option").equals("d")) {
									String pwd = getPassword(clientId);
									PrivateKey priv = getPrivKey(clientId, pwd);
									for (String arg: args) {
										out.flush();
										opcao_d(clientId, arg, priv);
									}
									
								} else if (data.get("option").equals("e")) {
									List<String> existed = new ArrayList<String>();
									List<String> created = new ArrayList<String>();
									
									String pwd = getPassword(clientId);
									PrivateKey priv = getPrivKey(clientId, pwd);
									
									for (String arg: args) {
										File f = new File(mainPath + clientId + "/" + arg);
										if (!f.exists()) {
											opcao_e(clientId,f, priv);
											created.add(arg);
										} else {
											existed.add(arg);
										}
									}
									out.flush();
									
								} else if (data.get("option").equals("l")) {		
									out.writeObject(opcao_l(clientId));
									
								} else if (data.get("option").equals("s")) {
									String pwd = getPassword(clientId);
									PrivateKey priv = getPrivKey(clientId, pwd);
									for (String arg: args) {
										opcao_s(clientId,arg, priv);		
									}
									
									
								} else if (data.get("option").equals("v")) {
									
								}
								
							} else {
								out.writeObject("-1");
								out.writeObject("Credenciais inválidas!");
							}
							break;
						}
						l = br.readLine();
					}
					if (!idExists) {
						out.writeObject("-1");
						out.writeObject("Não existe nenhum utilizador com o ID " + clientId);
					}
					out.flush();
					
					
				} catch (ClassNotFoundException | NoSuchAlgorithmException | OperatorCreationException | CertificateException | KeyStoreException | UnrecoverableKeyException | InvalidKeyException | SignatureException e) {
					e.printStackTrace();
				}
				out.close();
				in.close();
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		public void opcao_c(String[] args) 
				throws IOException, NoSuchAlgorithmException, OperatorCreationException, CertificateException, KeyStoreException, InvalidKeyException {
			out.writeObject("O utilizador " + args[1] + " com o ID " + args[0] + " vai ser criado");
			
			File f = new File(mainPath  +  args[0]);
			File f2 = new File("./client/" + args[0]);
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
			
			String pwd_crypted = encrypt(args[2]);
			String line = "\r" + args[0] + ";" + args[1] + ";" + pwd_crypted;
			if (!f.exists() & !f2.exists() & valid) {
				f.mkdirs();
				f2.mkdirs();
				FileOutputStream outFile = new FileOutputStream(file,true); //users.txt
				outFile.write(line.getBytes());
				outFile.close();
				createKeystore(args[0], pwd_crypted);
				createMAC(adminPwd, "users.mac");
				out.writeObject("O utilizador " + args[1] + " foi criado");
				
			} else {
				out.writeObject("O utilizador com ID " + args[0] + " já existe");
				System.exit(-1);
			}
		}
		
		public void opcao_d(String id, String file, PrivateKey pk) throws IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException {
			File f = new File(mainPath +  id + "/" + file);
			sendToClient(id, f);
			
			byte[] bytes = Files.readAllBytes(Paths.get(f.getPath()));
			Signature s = signing(bytes,pk);
			byte[] sign = s.sign();
			out.writeObject(sign);
			
			String name = file.substring(0,file.indexOf("."));
			File fs = new File(mainPath + id + "/" + name + ".signed.user" + id);
			FileOutputStream signing = new FileOutputStream(fs.getPath());
			ObjectOutputStream oos = new ObjectOutputStream(signing);
			oos.writeObject(sign);
			signing.close();
		}
		
		public void opcao_e(String id, File f, PrivateKey pk) 
				throws ClassNotFoundException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, InvalidKeyException, SignatureException {
			
			if (!f.exists()) {
				receiveFromClient(id,f);
				out.writeObject("O ficheiro " + f.getName() + " foi enviado para o servidor");
			} else {
				out.writeObject("O ficheiro " + f.getName() + " já existe no servidor");
			}
			
			byte[] bytes = Files.readAllBytes(Paths.get(f.getPath()));
			Signature s = signing(bytes,pk);
			byte[] sign = s.sign();
			out.writeObject(sign);

			String name = f.getName().substring(0,f.getName().indexOf("."));
			File fs = new File(mainPath + id + "/" + name + ".signed.user" + id);
			FileOutputStream signing = new FileOutputStream(fs.getPath());
			ObjectOutputStream oos = new ObjectOutputStream(signing);
			oos.writeObject(sign);
			signing.close();
		}
		
		public String opcao_l(String id) throws IOException {
			StringBuilder sb = new StringBuilder();
			File path = new File(mainPath +  id);
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
		
		public void opcao_s(String id, String arg, PrivateKey pk) throws ClassNotFoundException, IOException, InvalidKeyException, NoSuchAlgorithmException, SignatureException {
			byte[] hash = (byte[]) in.readObject();
			out.writeObject("A síntese do ficheiro " + arg + " foi enviada para servidor");
			Signature s = signing(hash,pk);
			
			out.writeObject(s.sign());
		}
		
		public void opcao_v() {
			
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
		    if ((new File(mainPath +id+ "/" +id+ ".keystore")).exists()){  // **** file da keystore
				FileInputStream kfile1 = new FileInputStream(mainPath +id+ "/" +id+ ".keystore"); 
				kstore.load(kfile1, pwd.toCharArray()); // **** password da keystore
				kfile1.close();
		    } else {
				kstore.load(null, null); // **** caso em que o file da keystore ainda nao existe
		    }
		    		
			Certificate chain [] = {certificate, certificate};
			
			// **** atencao ao alias do user e 'a password da chave privada
			kstore.setKeyEntry("user"+id, (Key)keyPair.getPrivate(), pwd.toCharArray(), chain);
			FileOutputStream kfile = new FileOutputStream(mainPath +id+ "/" +id+ ".keystore"); // keystore
			kstore.store(kfile, pwd.toCharArray());
		}
		
		public String getPassword(String id) throws IOException, InvalidKeyException, NoSuchAlgorithmException {
			BufferedReader br = usersRegistered();
			String l = br.readLine();
			String pwd = null;
			while (l != null && pwd == null) {
				String[] linha_split = l.split(";");
				if (linha_split[0].equals(id)) {
					pwd = linha_split[2];
				}
				l = br.readLine();
			}
			
			return pwd;
		}
		
		public PrivateKey getPrivKey(String id, String pwd) 
				throws KeyStoreException, UnrecoverableKeyException, NoSuchAlgorithmException, CertificateException, FileNotFoundException, IOException {
			
			KeyStore kstore = KeyStore.getInstance("JKS");
			kstore.load(new FileInputStream(mainPath +id+"/"+id+".keystore"), pwd.toCharArray());
			PrivateKey priv = (PrivateKey) kstore.getKey("user"+id, pwd.toCharArray());
			
			return priv;
		}
		
		public void receiveFromClient(String id, File f) throws ClassNotFoundException, IOException {
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
			fclient.close();
		}
		

		public void sendToClient(String id, File f) throws IOException {
			Long tam = f.length();
			out.writeObject(tam);
			
			BufferedInputStream fBuff = new BufferedInputStream(new FileInputStream(f.getPath()));
			byte[] buffer = new byte[1024];
			
			int n;
			while ((n = fBuff.read(buffer, 0 , 1024)) > 0) {
				out.write(buffer,0,n);
			}
		}
		
		public Signature signing(byte[] bytes, PrivateKey pk) throws NoSuchAlgorithmException, InvalidKeyException, SignatureException {
			Signature s = Signature.getInstance("SHA256withRSA");
			s.initSign(pk);
			s.update(bytes);
	
			return s;
		}
	}
	
	
}