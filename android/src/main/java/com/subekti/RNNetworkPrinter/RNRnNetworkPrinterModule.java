
package com.subekti.RNNetworkPrinter;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Callback;
import java.net.*;
import java.io.*;
import javax.annotation.Nullable;
import android.util.Base64;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.facebook.react.bridge.*;
import com.subekti.RNNetworkPrinter.sdk.Command;
import com.subekti.RNNetworkPrinter.sdk.PrintPicture;
import com.subekti.RNNetworkPrinter.sdk.PrinterCommand;
import com.subekti.RNNetworkPrinter.sdk.TscCommand;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import java.util.concurrent.*;

public class RNRnNetworkPrinterModule extends ReactContextBaseJavaModule {
  public static final int WIDTH_58 = 384;
  public static final int WIDTH_80 = 576;
  private int deviceWidth = WIDTH_58;
  private final ReactApplicationContext reactContext;
  ExecutorService executorService = Executors.newFixedThreadPool(20);

  public RNRnNetworkPrinterModule(ReactApplicationContext reactContext) {
    super(reactContext);
    this.reactContext = reactContext;
  }

  @Override
  public String getName() {
    return "RNRnNetworkPrinter";
  }

  @ReactMethod
  public void PrintLabel(String ip, ReadableMap commands, Callback cb) {
      final String ipPrinter = ip;
      final Callback callback = cb;
      final ReadableMap pCommands = commands;

      Runnable runnable = new Runnable() {
          @Override
          public void run() {
              Socket sock = null;
              try {
                  sock = new Socket(ipPrinter, 9100);
                  DataOutputStream out = new DataOutputStream(sock.getOutputStream());

                  ReadableMap config = (pCommands != null && pCommands.hasKey("config") && !pCommands.isNull("config"))
                          ? pCommands.getMap("config")
                          : null;

                  int paperSize = (config != null && config.hasKey("paperSize") && !config.isNull("paperSize"))
                          ? config.getInt("paperSize") : 58;
                  int paperHeight = (config != null && config.hasKey("paperHeight") && !config.isNull("paperHeight"))
                          ? config.getInt("paperHeight") : 30;
                  int offsetX = (config != null && config.hasKey("xOffset") && !config.isNull("xOffset"))
                          ? config.getInt("xOffset") : 50;
                  int offsetY = (config != null && config.hasKey("yOffset") && !config.isNull("yOffset"))
                          ? config.getInt("yOffset") : 30;
                  String fontSize = (config != null && config.hasKey("fontSize") && !config.isNull("fontSize"))
                          ? config.getString("fontSize") : "TSS24.BF2";

                  ReadableArray data = (pCommands != null && pCommands.hasKey("data") && !pCommands.isNull("data"))
                          ? pCommands.getArray("data")
                          : null;

                  if (data != null) {
                      for (int i = 0; i < data.size(); i++) {
                          ReadableMap item = data.getMap(i);
                          ReadableArray lines = (item != null && item.hasKey("lines") && !item.isNull("lines"))
                                  ? item.getArray("lines")
                                  : null;

                          if (lines != null) {
                              out.write(TscCommand.sizeBymm(paperSize, paperHeight));
                              out.write(TscCommand.cls());

                              int lineColumnY = 0;
                              for (int j = 0; j < lines.size(); j++) {
                                  String lineText = lines.isNull(j) ? "" : lines.getString(j);

                                  out.write(TscCommand.text(
                                          Math.round(((float) offsetX / 2)),
                                          offsetY + lineColumnY,
                                          fontSize,
                                          0,
                                          1,
                                          1,
                                          "" + lineText
                                  ));

                                  int addY = 25;
                                  if ("1".equals(fontSize)) {
                                      addY = 15;
                                  }
                                  lineColumnY += addY;
                              }

                              out.write(TscCommand.print(1));
                          }
                      }
                  }
                  out.flush();
                  sock.close();
                  callback.invoke(null, "Success");
              }  catch (Exception e) {
                  if (sock != null && !sock.isClosed()) {
                      try {
                          sock.close();
                      } catch (IOException ignored) {}
                  }
                  callback.invoke(e.toString(), null);
              }
          }
      };
      executorService.execute(runnable);
  }

  @ReactMethod
  public void Print(String ip, ReadableArray commands, Callback cb) {
    final String ipPrinter = ip;
    final Callback callback = cb;
    final ReadableArray pCommands = commands;

    Runnable runnable = new Runnable() {
      public void run() {
        try {
          Socket sock = new Socket(ipPrinter, 9100);
          DataOutputStream out = new DataOutputStream(sock.getOutputStream());

          for (int i = 0; i < pCommands.size(); i++) {
            ReadableMap command = pCommands.getMap(i);
            if (command.hasKey("printText")) {
              // Print TEXT
              String encoding = "GBK";
              int codepage = 0;
              int widthTimes = 0;
              int heigthTimes = 0;
              int fonttype = 0;
              String toPrint = command.getString("printText");

              byte[] bytes = PrinterCommand.POS_Print_Text(toPrint, encoding, codepage, widthTimes, heigthTimes, fonttype);
              out.write(PrinterCommand.POS_S_Align(1));
              out.write(bytes);
              out.write(PrinterCommand.POS_Set_PrtAndFeedPaper(30));
              out.write(Command.ESC_Init);
            } else if (command.hasKey("printTextBig")) {
              // Print TEXT
              String encoding = "GBK";
              int codepage = 0;
              int widthTimes = 1;
              int heigthTimes = 1;
              int fonttype = 0;
              String toPrint = command.getString("printTextBig");

              byte[] bytes = PrinterCommand.POS_Print_Text(toPrint, encoding, codepage, widthTimes, heigthTimes, fonttype);
              out.write(PrinterCommand.POS_S_Align(0));
              out.write(bytes);
              out.write(PrinterCommand.POS_Set_PrtAndFeedPaper(30));
              out.write(Command.ESC_Init);
            } else if (command.hasKey("printPic")) {
              // Print PIC
              int width = 0;
              int leftPadding = 0;
              String base64Image = command.getString("printPic");
              if (command.hasKey("width")) {
                width = command.getInt("width");
              }
              if (command.hasKey("left")) {
                leftPadding = command.getInt("left");
              }
              // cannot larger then devicesWith;
              if (width > deviceWidth || width == 0) {
                width = deviceWidth;
              }

              byte[] bytes = Base64.decode(base64Image, Base64.DEFAULT);
              Bitmap mBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
              int nMode = 0;
              if (mBitmap != null) {
                byte[] data = PrintPicture.POS_PrintBMP(mBitmap, width, nMode, leftPadding);
                out.write(Command.ESC_Init);
                out.write(Command.LF);
                out.write(PrinterCommand.POS_S_Align(1));
                out.write(data);
                out.write(PrinterCommand.POS_Set_PrtAndFeedPaper(30));
                out.write(Command.ESC_Init);
              }
            } else if (command.hasKey("cutPaper")) {
              // CUT
              int cutPaper = command.getInt("cutPaper");
              out.write(PrinterCommand.POS_Set_Cut(cutPaper));
              out.write(Command.ESC_Init);
            } else if (command.hasKey("openCashDrawer")) {
              // CASH DRAWER
              boolean openCashDrawer = command.getBoolean("openCashDrawer");
              if (openCashDrawer == true) {
                out.write(PrinterCommand.POS_Set_Cashbox(0, 10, 10));
                out.write(PrinterCommand.POS_Set_Cashbox(1, 10, 10));
              }
            } else if (command.hasKey("feed")) {
              int feed = command.getInt("feed");
              out.write(PrinterCommand.POS_Set_PrtAndFeedPaper(feed));
            } else if (command.hasKey("barcode")) {
              String str = command.getString("barcode");
              int nType = 73; // CODE128
              int nWidth = 100;
              int nHeight = 2;
              int nHriFontType = 0; 
              int nHriFontPosition = 0;

              if (command.hasKey("barcodeWidth")) {
                nWidth = command.getInt("barcodeWidth");
              }
              if (command.hasKey("barcodeHeight")) {
                nHeight = command.getInt("barcodeHeight");
              }

              byte[] bytes = PrinterCommand.getBarCodeCommand(
                str, nType, nWidth, nHeight, nHriFontType, nHriFontPosition
              );

              out.write(PrinterCommand.POS_S_Align(1));
              out.write(bytes);
            }
          }
          sock.close();
          callback.invoke(null, "Success");
        } catch (Exception e) {
          callback.invoke(e.toString(), null);
        }
      }
    };
    executorService.execute(runnable);
  }
}

