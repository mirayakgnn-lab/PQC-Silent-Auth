package com.example.prototipuygulama;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import android.util.Base64;

public class HMACUtils {

    public static String generateHMAC(String secretKey, String message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec =
                    new SecretKeySpec(secretKey.getBytes(), "HmacSHA256");
            mac.init(keySpec);

            byte[] rawHmac = mac.doFinal(message.getBytes());
            return Base64.encodeToString(rawHmac, Base64.NO_WRAP);

        } catch (Exception e) {
            return null;
        }
    }

    // Simulated verification (normally done by operator)
    public static boolean verifyHMAC(String secretKey, String message, String hmac) {
        String computed = generateHMAC(secretKey, message);
        return computed != null && computed.equals(hmac);
    }
}
