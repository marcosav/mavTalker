package com.gmail.marcosav2010.cipher;

import javax.crypto.Cipher;
import javax.crypto.spec.OAEPParameterSpec;
import javax.crypto.spec.PSource;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.MGF1ParameterSpec;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class CipherPool {

    private static final int DEFAULT_MAX_CAPACITY = 10;
    private static final int DEFAULT_INITIAL_CIPHERS = 2;

    private static final String CIPHER_ASYMMETRIC_ALGORITHM = "RSA/ECB/OAEPWithSHA-512AndMGF1Padding";
    private static final AlgorithmParameterSpec ALGORITHM_SPEC = new OAEPParameterSpec("SHA3-512", "MGF1",
            MGF1ParameterSpec.SHA512, PSource.PSpecified.DEFAULT);
    private final int mode;
    private final Key key;
    private final int maxCapacity;
    private final BlockingQueue<Cipher> queue;
    private final AtomicInteger cipherCount;

    public CipherPool(int mode, Key key) throws GeneralSecurityException {
        this(mode, key, DEFAULT_INITIAL_CIPHERS, DEFAULT_MAX_CAPACITY);
    }

    public CipherPool(int mode, Key key, int initialCiphers, int maxCapacity) throws GeneralSecurityException {
        this.mode = mode;
        this.key = key;

        this.maxCapacity = maxCapacity;

        cipherCount = new AtomicInteger();

        queue = new ArrayBlockingQueue<>(maxCapacity);

        init(initialCiphers);
    }

    private void init(int count) throws GeneralSecurityException {
        if (count > maxCapacity)
            throw new IllegalArgumentException("Initial cipher number can't be higher than max ciphers");

        for (int i = 0; i < count; i++)
            queue.add(createCipher());
    }

    private Cipher createCipher() throws GeneralSecurityException {
        cipherCount.updateAndGet(i -> {
            if (++i > maxCapacity)
                throw new IllegalStateException("Cipher pool exceded cipher of " + maxCapacity);
            return i;
        });

        Cipher c = Cipher.getInstance(CIPHER_ASYMMETRIC_ALGORITHM);
        c.init(mode, key, ALGORITHM_SPEC);
        return c;
    }

    public final byte[] doFinal(byte[] bytes) throws GeneralSecurityException, InterruptedException {
        Cipher c;
        if (!queue.isEmpty())
            c = queue.take();
        else if (cipherCount.get() < maxCapacity)
            try {
                c = createCipher();
            } catch (IllegalStateException ex) {
                c = queue.take();
            }
        else
            c = queue.take();

        byte[] out = c.doFinal(bytes);
        queue.put(c);
        return out;
    }
}
