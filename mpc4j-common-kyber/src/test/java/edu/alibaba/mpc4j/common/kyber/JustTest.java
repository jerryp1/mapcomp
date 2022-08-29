package edu.alibaba.mpc4j.common.kyber;
import edu.alibaba.mpc4j.common.kyber.provider.KyberPackedPKI;
import edu.alibaba.mpc4j.common.kyber.provider.KyberVecPki;
import edu.alibaba.mpc4j.common.kyber.provider.kyber.*;
import edu.alibaba.mpc4j.common.tool.crypto.hash.Hash;
import edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Arrays;

import static edu.alibaba.mpc4j.common.tool.crypto.hash.HashFactory.HashType.JDK_SHA256;

public class JustTest {
    @Test
    public void testEncryptDecrypt() throws NoSuchAlgorithmException {
        for(int i = 0; i < 100;i++){
            KyberPackedPKI packedPKI= Indcpa.generateKyberKeys(4);
            SecureRandom Sr = new SecureRandom();
            byte[] msg = new byte[32];
            Sr.nextBytes(msg);
            byte[] seed = new byte[32];
            Sr.nextBytes(seed);
            byte[] Cipher = Indcpa.encrypt(msg,packedPKI.getPackedPublicKey(),seed,4);
            byte[] test = Indcpa.decrypt(Cipher,packedPKI.getPackedPrivateKey(),4);
            System.out.print(Arrays.toString(msg));
            System.out.print("\n");
            System.out.print(Arrays.toString(test));
            System.out.print("\n");
        }
    }
    @Test
    public void testPolyFromData() throws NoSuchAlgorithmException {
        for(int i = 0; i < 100;i++){
            SecureRandom Sr = new SecureRandom();
            byte[] msg = new byte[32];
            Sr.nextBytes(msg);
            short[] test = Poly.polyFromData(msg);
            System.out.print(Arrays.toString(test));
            System.out.print("\n");
        }
    }
    @Test
    public void testnewEncryptDecrypt() throws NoSuchAlgorithmException {
        for(int i = 0; i < 100;i++){
            KyberVecPki packedPKI= KyberKeyOps.generateKyberKeys(4);
            SecureRandom Sr = new SecureRandom();
            byte[] msg = new byte[32];
            Sr.nextBytes(msg);
            byte[] seed = new byte[32];
            Sr.nextBytes(seed);
            byte[] Cipher = KyberKeyOps.encrypt(msg,packedPKI.getPublicKeyVec(),packedPKI.getPublicKeyGenerator(),seed,4);
            byte[] test = KyberKeyOps.decrypt(Cipher,packedPKI.getPrivateKeyVec(),4);
            System.out.print(Arrays.toString(msg));
            System.out.print("\n");
            System.out.print(Arrays.toString(test));
            System.out.print("\n");
        }
    }

    @Test
    public void testNewHashFunction() throws NoSuchAlgorithmException {
        for(int i = 0; i < 100;i++){
            SecureRandom Sr = new SecureRandom();
            byte[] msg = new byte[KyberParams.paramsPolyvecBytesK1024];
            Sr.nextBytes(msg);
            short[][] newMsg = Poly.polyVectorFromBytes(msg);
            Hash hashFunction = HashFactory.createInstance(JDK_SHA256, 32);
            System.out.print(Arrays.toString(Poly.polyVectorToBytes(KyberPublicKeyOps.kyberPKHash(newMsg,hashFunction))));
            System.out.print("\n");
            //System.out.print(Arrays.toString(KyberPublicKeyOps.kyberPKHash(msg,hashFunction)));
            System.out.print(Arrays.toString(Poly.polyVectorToBytes(KyberPublicKeyOps.kyberPKHash(newMsg,hashFunction))));
            System.out.print("\n");
        }
    }



}
