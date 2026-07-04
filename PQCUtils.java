package com.example.prototipuygulama;

public class PQCUtils {

    public static boolean verifyKyberSession(String sharedSecret) {
        return sharedSecret != null &&
                sharedSecret.startsWith("KYBER_SHARED_");
    }
}