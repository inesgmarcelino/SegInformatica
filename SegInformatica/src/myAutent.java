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
import java.security.NoSuchProviderException;
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
			BufferedReader br = usersRegistered();
			String l = br.readLine();
			if (l != null) {
				String[] linha_split = l.split(";");
				if (Integer.parseInt(linha_split[0]) == adminId && linha_split[1].equals("Administrador") && checkPwd(linha_split[2], adminPwd)) {
					System.out.println("servidor: main");
					myAutent server = new myAutent();
					server.startServer();					
				} else {
					System.out.println("Credenciais inválidas!");
					System.exit(0);
				}
			} else {
				String line = adminId + ";Administrador;" + encrypt(adminPwd);;
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
					while (l != null) {
						String[] linha_split = l.split(";");
						if (clientId.equals(linha_split[0])) {// && data.get("password").equals(linha_split[2])) {
							if (checkPwd(linha_split[2], data.get("password"))) {
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
//									out.writeObject(args.length);
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
									out.writeObject(opcao_l(clientId));
									
								} else if (data.get("option").equals("s")) {
									String pwd = getPassword(clientId);
									PrivateKey priv = getPrivKey(clientId, pwd);
									for (String arg: args) {
										opcao_s(clientId,arg, priv);		
										out.writeObject("A síntese do ficheiro " + arg + " foi enviada para servidor");
									}
									out.writeObject(1);
									out.writeObject("As assinaturas foram recebidas pelo cliente");
									
								} else if (data.get("option").equals("v")) {
									
								}
								
							} else {
								out.writeObject(1);
								out.writeObject("Credenciais inválidas!");
							}
						}
						l = br.readLine();
					}
					
					
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
		
		public boolean opcao_c(String[] args) 
				throws IOException, NoSuchAlgorithmException, OperatorCreationException, CertificateException, KeyStoreException {
			
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
				return true;
				
			} else {
				return false;
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
			
			out.writeObject("O ficheiro " + file + " foi recebido pelo cliente");
		}
		
		public void opcao_e(String id, File f, PrivateKey pk) 
				throws ClassNotFoundException, IOException, KeyStoreException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException, InvalidKeyException, SignatureException {
			
			receiveFromClient(id,f);
			
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
		
		public String getPassword(String id) throws IOException {
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