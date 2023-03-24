package com.ds.migration.authentication.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemReader;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JWTUtils {

	/**
	 * Helper method to create a JWT token for the JWT flow
	 *
	 * @param publicKeyFilename  the filename of the RSA public key
	 * @param privateKeyFilename the filename of the RSA private key
	 * @param oAuthBasePath      DocuSign OAuth base path (account-d.docusign.com
	 *                           for the developer sandbox and account.docusign.com
	 *                           for the production platform)
	 * @param clientId           DocuSign OAuth Client Id (AKA Integrator Key)
	 * @param userId             DocuSign user Id to be impersonated (This is a
	 *                           UUID)
	 * @param expiresIn          number of seconds remaining before the JWT
	 *                           assertion is considered as invalid
	 * @return a fresh JWT token
	 * @throws JWTCreationException if not able to create a JWT token from the input
	 *                              parameters
	 * @throws IOException          if there is an issue with either the public or
	 *                              private file
	 */
	public static String generateJWTAssertion(String publicKeyFilename, String privateKeyFilename, String oAuthBasePath,
			String clientId, String userId, long expiresIn, String scopes) throws JWTCreationException, IOException {
		String token;
		if (expiresIn <= 0L) {
			throw new IllegalArgumentException("expiresIn should be a non-negative value");
		}
		if (publicKeyFilename == null || "".equals(publicKeyFilename) || privateKeyFilename == null
				|| "".equals(privateKeyFilename) || oAuthBasePath == null || "".equals(oAuthBasePath)
				|| clientId == null || "".equals(clientId) || userId == null || "".equals(userId)) {
			throw new IllegalArgumentException("One of the arguments is null or empty");
		}

		try {
			RSAPublicKey publicKey = readPublicKeyFromFile(publicKeyFilename, "RSA");
			RSAPrivateKey privateKey = readPrivateKeyFromFile(privateKeyFilename, "RSA");
			Algorithm algorithm = Algorithm.RSA256(publicKey, privateKey);
			long now = System.currentTimeMillis();
			token = JWT.create().withIssuer(clientId).withSubject(userId).withAudience(oAuthBasePath)
					.withIssuedAt(new Date(now)).withExpiresAt(new Date(now + expiresIn * 1000))
					.withClaim("scope", scopes).sign(algorithm);
		} catch (JWTCreationException e) {
			throw e;
		} catch (IOException e) {
			throw e;
		}

		return token;
	}

	private static RSAPublicKey readPublicKeyFromFile(String filepath, String algorithm) throws IOException {
		File pemFile = new File(filepath);
		if (!pemFile.isFile() || !pemFile.exists()) {
			throw new FileNotFoundException(String.format("The file '%s' doesn't exist.", pemFile.getAbsolutePath()));
		}
		PemReader reader = new PemReader(new FileReader(pemFile));
		try {
			PemObject pemObject = reader.readPemObject();
			byte[] bytes = pemObject.getContent();
			RSAPublicKey publicKey = null;
			try {
				KeyFactory kf = KeyFactory.getInstance(algorithm);
				EncodedKeySpec keySpec = new X509EncodedKeySpec(bytes);
				publicKey = (RSAPublicKey) kf.generatePublic(keySpec);
			} catch (NoSuchAlgorithmException e) {
				log.info("Could not reconstruct the public key, the given algorithm could not be found.");
			} catch (InvalidKeySpecException e) {
				log.info("Could not reconstruct the public key");
			}

			return publicKey;
		} finally {
			reader.close();
		}
	}

	private static RSAPrivateKey readPrivateKeyFromFile(String filepath, String algorithm) throws IOException {
		File pemFile = new File(filepath);
		if (!pemFile.isFile() || !pemFile.exists()) {
			throw new FileNotFoundException(String.format("The file '%s' doesn't exist.", pemFile.getAbsolutePath()));
		}
		PemReader reader = new PemReader(new FileReader(pemFile));
		try {
			PemObject pemObject = reader.readPemObject();
			byte[] bytes = pemObject.getContent();
			RSAPrivateKey privateKey = null;
			try {
				Security.addProvider(new BouncyCastleProvider());
				KeyFactory kf = KeyFactory.getInstance(algorithm, "BC");
				EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(bytes);
				privateKey = (RSAPrivateKey) kf.generatePrivate(keySpec);
			} catch (NoSuchAlgorithmException e) {
				log.info("Could not reconstruct the private key, the given algorithm could not be found.");
			} catch (InvalidKeySpecException e) {
				log.info("Could not reconstruct the private key");
			} catch (NoSuchProviderException e) {
				log.info("Could not reconstruct the private key, invalid provider.");
			}

			return privateKey;
		} finally {
			reader.close();
		}
	}
}