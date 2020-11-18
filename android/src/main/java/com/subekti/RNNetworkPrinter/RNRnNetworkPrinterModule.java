
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
import com.facebook.react.bridge.*;
import com.subekti.RNNetworkPrinter.sdk.Command;
import com.subekti.RNNetworkPrinter.sdk.PrintPicture;
import com.subekti.RNNetworkPrinter.sdk.PrinterCommand;
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
              }
            } else if (command.hasKey("feed")) {
              int feed = command.getInt("feed");
              out.write(PrinterCommand.POS_Set_PrtAndFeedPaper(feed));
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

