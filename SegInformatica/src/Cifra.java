
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

public class Cifra {

	public static void makeKeystore( String user)
		      throws NoSuchAlgorithmException, CertificateException, NoSuchProviderException, OperatorCreationException, IOException, KeyStoreException {
		
//		gera chaves assimetricas RSA
		KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
		keyPairGenerator.initialize(2048);
		KeyPair keyPair = keyPairGenerator.generateKeyPair();
			
//		define informacao para o certificado
		X500Name dnName = new X500Name("CN=" + "Grupo013");
	    BigInteger certSerialNumber = BigInteger.valueOf(System.currentTimeMillis());
	    Instant startDate = Instant.now();
	    Instant endDate = startDate.plus(2 * 365, ChronoUnit.DAYS);
	    
//	    classe que assina o certificado - certifcado auto assinado
	    String signatureAlgorithm = "SHA256WithRSA";
	    ContentSigner contentSigner = new JcaContentSignerBuilder(signatureAlgorithm).build(keyPair.getPrivate());
	    
//	    cria o certificado
	    X509v3CertificateBuilder certBuilder = new JcaX509v3CertificateBuilder(
	        dnName, certSerialNumber, Date.from(startDate), Date.from(endDate), dnName,
	        keyPair.getPublic());
	    Certificate certificate = new JcaX509CertificateConverter().getCertificate(certBuilder.build((ContentSigner)contentSigner));
	    
//	    guarda chave privada + certificado na keystore 
	    KeyStore kstore = KeyStore.getInstance("JKS");
	    if ((new File("./server/" +user+ "/" +user+ ".keystore")).exists()){  // **** file da keystore
			FileInputStream kfile1 = new FileInputStream("./server/" +user+ "/" +user+ ".keystore"); 
			kstore.load(kfile1, "123456".toCharArray()); // **** password da keystore
			kfile1.close();
	    } else {
			kstore.load(null, null); // **** caso em que o file da keystore ainda n�o existe
	    }
	    		
		Certificate chain [] = {certificate, certificate};
		
		// **** atencao ao alias do user e 'a password da chave privada
		kstore.setKeyEntry("mm11", (Key)keyPair.getPrivate(), "pass".toCharArray(), chain);
		FileOutputStream kfile = new FileOutputStream("./server/" +user+ "/" +user+ ".keystore"); // keystore
		kstore.store(kfile, "123456".toCharArray());
	}
}
