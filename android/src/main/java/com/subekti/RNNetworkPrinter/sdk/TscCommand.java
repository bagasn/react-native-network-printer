package com.subekti.RNNetworkPrinter.sdk;

import java.io.UnsupportedEncodingException;

public class TscCommand {
    private static final String DEFAULT_ENCODING = "GBK";

    public static byte[] strToBytes(String str) {
        try {
            return str.getBytes(DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            return str.getBytes();
        }
    }

    public static byte[] sizeBymm(double width, double height) {
        return strToBytes("SIZE " + width + " mm," + height + " mm\n");
    }

    public static byte[] gapBymm(double gap, double offset) {
        return strToBytes("GAP " + gap + " mm," + offset + " mm\n");
    }

    public static byte[] cls() {
        return strToBytes("CLS\n");
    }

    public static byte[] direction(int direction) {
        return strToBytes("DIRECTION " + direction + "\n");
    }

    public static byte[] reference(int x, int y) {
        return strToBytes("REFERENCE " + x + "," + y + "\n");
    }

    public static byte[] text(int x, int y, String font, int rotation, int xMultiplication, int yMultiplication, String content) {
        String safeText = content != null ? content : "";
        String cmd = "TEXT " + x + "," + y + ",\"" + font + "\"," + rotation + ","
                + xMultiplication + "," + yMultiplication + ",\"" + safeText + "\"\n";
        return strToBytes(cmd);
    }

    public static byte[] barCode(int x, int y, String codeType, int height, int humanReadable, int rotation, int narrow, int wide, String content) {
        String safeContent = content != null ? content : "";
        String cmd = "BARCODE " + x + "," + y + ",\"" + codeType + "\"," + height + ","
                + humanReadable + "," + rotation + "," + narrow + "," + wide + ",\"" + safeContent + "\"\n";
        return strToBytes(cmd);
    }

    public static byte[] qrCode(int x, int y, String eccLevel, int cellWidth, String mode, int rotation, String content) {
        String safeContent = content != null ? content : "";
        String cmd = "QRCODE " + x + "," + y + "," + eccLevel + "," + cellWidth + ","
                + mode + "," + rotation + ",\"" + safeContent + "\"\n";
        return strToBytes(cmd);
    }

    public static byte[] qrCode(int x, int y, String eccLevel, int cellWidth, String mode, int rotation, String model, String mask, String content) {
        String safeContent = content != null ? content : "";
        String cmd = "QRCODE " + x + "," + y + "," + eccLevel + "," + cellWidth + ","
                + mode + "," + rotation + "," + model + "," + mask + ",\"" + safeContent + "\"\n";
        return strToBytes(cmd);
    }

    public static byte[] cut() {
        return strToBytes("CUT\n");
    }

    public static byte[] sound(int level, int interval) {
        return strToBytes("SOUND " + level + "," + interval + "\n");
    }

    public static byte[] print(int m) {
        return strToBytes("PRINT " + m + "\n");
    }

    public static byte[] print(int m, int n) {
        return strToBytes("PRINT " + m + "," + n + "\n");
    }
}
