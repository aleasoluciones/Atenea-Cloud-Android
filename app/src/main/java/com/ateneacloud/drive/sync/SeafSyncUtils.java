package com.ateneacloud.drive.sync;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

public class SeafSyncUtils {


    public static String calculateHash(String inputString) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            byte[] data = inputString.getBytes(StandardCharsets.UTF_8);
            byte[] hashBytes = digest.digest(data);

            StringBuilder hash = new StringBuilder();
            for (byte b : hashBytes) {
                hash.append(String.format("%02x", b));
            }

            return hash.toString();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static boolean isDateOlder(Date date, Date dateToCompare) {
        return date.compareTo(dateToCompare) > 0;
    }

    public static boolean isDatePrevious(Date date, Date dateToCompare) {
        return date.compareTo(dateToCompare) < 0;
    }
}
