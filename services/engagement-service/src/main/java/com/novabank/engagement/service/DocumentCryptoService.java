package com.novabank.engagement.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

@Service
public class DocumentCryptoService {
  private final SecretKeySpec key;
  private final SecureRandom random=new SecureRandom();
  public DocumentCryptoService(@Value("${novabank.documents.encryption-key}") String secret) {
    try { key=new SecretKeySpec(MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8)),"AES"); }
    catch(Exception e){ throw new IllegalStateException(e); }
  }
  public Encrypted encrypt(byte[] clear) { try { byte[] nonce=new byte[12]; random.nextBytes(nonce); var c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key,new GCMParameterSpec(128,nonce)); return new Encrypted(nonce,c.doFinal(clear)); } catch(Exception e){throw new IllegalStateException("Unable to encrypt document",e);} }
  public byte[] decrypt(byte[] nonce,byte[] encrypted) { try { var c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,nonce)); return c.doFinal(encrypted); } catch(Exception e){throw new IllegalStateException("Unable to decrypt document",e);} }
  public record Encrypted(byte[] nonce,byte[] content) {}
}
