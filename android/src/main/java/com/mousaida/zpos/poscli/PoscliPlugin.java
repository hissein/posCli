package com.mousaida.zpos.poscli;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.util.Base64;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.lvrenyang.io.Pos;
import com.lvrenyang.io.USBPrinting;

import java.util.HashMap;
import java.util.Iterator;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/** PoscliPlugin */
public class PoscliPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
  public static int nPrintWidth = 384;
  public static boolean bCutter = false;
  public static boolean bDrawer = false;
  public static boolean bBeeper = true;
  public static int nPrintCount = 1;
  public static int nCompressMethod = 0;
  public static boolean bAutoPrint = false;
  public static int nPrintContent = 0;
  /// The MethodChannel that will the communication between Flutter and native Android
  ///
  /// This local reference serves to register the plugin with the Flutter Engine and unregister it
  /// when the Flutter Engine is detached from the Activity
  private MethodChannel channel;
  private Activity activity;
  private Context context;
  Pos pos = new Pos();
  USBPrinting mUsb;

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "poscli");
    this.pos = new Pos();
    this.mUsb = new USBPrinting();
    this.pos.Set(this.mUsb);
    this.context = flutterPluginBinding.getApplicationContext();
    channel.setMethodCallHandler(this);
  }

  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    if (call.method.equals("initSdk")) {
      result.success(initSdk2());
    } else if (call.method.equals("printTicket")) {
      String file = call.argument("file");
      result.success(printTicketBit(file));
    } else {
      result.notImplemented();
    }
  }

   public boolean initSdk2() {
     try {
       initSdk(this.activity);
       return true;
     } catch (Exception e) {
       throw e;
     }
   }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    channel.setMethodCallHandler(null);
  }

  @Override
  public void onAttachedToActivity(@NonNull  ActivityPluginBinding activityPluginBinding) {
    this.activity = activityPluginBinding.getActivity();

  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {

  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull  ActivityPluginBinding activityPluginBinding) {

  }

  @Override
  public void onDetachedFromActivity() {

  }


  public void initSdk(final Activity context) {
    final UsbManager mUsbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);

    HashMap<String, UsbDevice> deviceList = mUsbManager.getDeviceList();
    Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
    if (deviceList.size() > 0) {
        final UsbDevice device = deviceIterator.next();
        PendingIntent mPermissionIntent = PendingIntent
                .getBroadcast(
                        context,
                        0,
                        new Intent(
                                context.getApplicationInfo().packageName),
                        0);

        if (!mUsbManager.hasPermission(device)) {
          mUsbManager.requestPermission(device,
                  mPermissionIntent);
          Toast.makeText(context,
                  "Permission refusé", Toast.LENGTH_LONG)
                  .show();
        } else {
          Toast.makeText(context, "Connecting...", Toast.LENGTH_SHORT).show();
          try {
             TaskOpen(mUsb, mUsbManager, device);
          }
          catch (Exception e) {
            throw e;
          }
        }

    }
  }
  private static Bitmap resize(Bitmap image, int maxWidth, int maxHeight) {
    if (maxHeight > 0 && maxWidth > 0) {
      int width = image.getWidth();
      int height = image.getHeight();
      float ratioBitmap = (float) width / (float) height;
      float ratioMax = (float) maxWidth / (float) maxHeight;

      int finalWidth = maxWidth;
      int finalHeight = maxHeight;
      if (ratioMax > ratioBitmap) {
        finalWidth = (int) ((float)maxHeight * ratioBitmap);
      } else {
        finalHeight = (int) ((float)maxWidth / ratioBitmap);
      }
      image = Bitmap.createScaledBitmap(image, finalWidth, finalHeight, true);
      return image;
    } else {
      return image;
    }
  }

  private boolean printTicketBit(String paths)  {
    byte[] bytes= Base64.decode(paths,Base64.NO_CLOSE);
    BitmapFactory.Options opt = new BitmapFactory.Options();
    opt.inPreferredConfig = Bitmap.Config.RGB_565;
    opt.inPurgeable = true;
    opt.inInputShareable = true;
    Bitmap bitmap=  BitmapFactory.decodeByteArray(bytes, 0, bytes.length,opt);

    final Bitmap newbitmap=  resize(bitmap,384, bitmap.getHeight()+100);
    pos.POS_PrintPicture(newbitmap,384, 0, 0);
    final boolean bIsOpened = pos.GetIO().IsOpened();

    return  true;


  }
  public void TaskOpen(USBPrinting usb, UsbManager usbManager, UsbDevice usbDevice)
  {
    usb.Open(usbManager,usbDevice,this.context);
  }
}
