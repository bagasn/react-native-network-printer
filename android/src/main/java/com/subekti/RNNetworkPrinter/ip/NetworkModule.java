package com.subekti.RNNetworkPrinter.ip;

import android.util.Log;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;

import org.json.JSONArray;

import java.net.Inet4Address;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static com.facebook.react.common.ReactConstants.TAG;

public class NetworkModule extends ReactContextBaseJavaModule {
    public static List<String> DSLITE_LIST = Arrays.asList("192.0.0.0", "192.0.0.1", "192.0.0.2", "192.0.0.3", "192.0.0.4", "192.0.0.5", "192.0.0.6", "192.0.0.7");
    final ExecutorService executor = Executors.newFixedThreadPool(20);

    //constructor
    public NetworkModule(ReactApplicationContext reactContext) {
        super(reactContext);
    }

    //Mandatory function getName that specifies the module name
    @Override
    public String getName() {
        return "Network";
    }

    public static Future<WritableNativeMap> portIsOpen(final ExecutorService es, final String ip, final int port, final int timeout) {
        return es.submit(new Callable<WritableNativeMap>() {
            public WritableNativeMap call() {
                WritableNativeMap result = new WritableNativeMap();
                result.putString("ip", ip);
                result.putBoolean("open", false);
                try {
                    Socket socket = new Socket();
                    socket.connect(new InetSocketAddress(ip, port), timeout);
                    socket.close();
                    result.putBoolean("open", true);
                    return result;
                } catch (Exception ex) {
                    return result;
                }
            }
        });
    }


    @ReactMethod
    public void list(Integer type , Callback cb) throws ExecutionException, InterruptedException {
        try {
            String deviceIp = getIPV4Address().get();
            final ExecutorService es = Executors.newFixedThreadPool(50);
            final WritableArray printers = new WritableNativeArray();
            String[] ipPart = deviceIp.split("\\.");
            String subnetOri = ipPart[0] + "." + ipPart[1] + "." + ipPart[2];
            final int ipPart2Int = Integer.parseInt(ipPart[2]);
            String subnetMinOne = ipPart[0] + "." + ipPart[1] + "." + Integer.toString((ipPart2Int - 1));
            String subnetPlusOne = ipPart[0] + "." + ipPart[1] + "." + Integer.toString((ipPart2Int + 1));
            final int timeout = 200;

            final List<Future<WritableNativeMap>> futures = new ArrayList<>();


            for (int i = 0; i <= 255; i++) {
                String s = subnetOri;
                if (type == 1) s = subnetMinOne;
                if (type == 2) s = subnetPlusOne;
                String ip = s + "." + i;
                futures.add(portIsOpen(es, ip, 9100, timeout));
            }


            es.shutdown();
            for (final Future<WritableNativeMap> f : futures) {
                try {
                    if (f.get().getBoolean("open")) {
                        printers.pushString(f.get().getString("ip"));
                    }
                } catch (ExecutionException e) {
//              e.printStackTrace();
                } catch (InterruptedException e) {
//              e.printStackTrace();
                }
            }
            cb.invoke(null, printers);
        } catch (ExecutionException e) {
            cb.invoke(e.toString(), null);
//              e.printStackTrace();
        } catch (InterruptedException e) {
            cb.invoke(e.toString(), null);
//              e.printStackTrace();
        }
    }

    @ReactMethod
    public void isPortOpen(String ip, Integer port, Callback cb) {
        Socket socket = null;
        try {
            socket = new Socket(ip, port);
            cb.invoke(true);
        } catch (Exception e) {
            cb.invoke(false);
        }
    }

    @ReactMethod
    public void getIPAddress(final Callback callback) {
        String ipAddress = "error";
        String tmp = "0.0.0.0";

        for (InterfaceAddress address : getInetAddresses()) {
            if (!address.getAddress().isLoopbackAddress()) {
                tmp = address.getAddress().getHostAddress().toString();
                if (!inDSLITERange(tmp)) {
                    ipAddress = tmp;
                }
            }
        }

        callback.invoke(ipAddress);
    }

    
    public Future<String> getIPV4Address() {
        return executor.submit(new Callable<String>() {
            public String call() {
                String ipAddress = "0.0.0.0";
                String tmp = "0.0.0.0";

                for (InterfaceAddress address : getInetAddresses()) {
                    if (!address.getAddress().isLoopbackAddress() && address.getAddress() instanceof Inet4Address) {
                        tmp = address.getAddress().getHostAddress().toString();
                        if (!inDSLITERange(tmp)) {
                            ipAddress = tmp;
                        }
                    }
                }
                return ipAddress;
            }
        });
    }


    private Boolean inDSLITERange(String ip) {
        // Fixes issue https://github.com/pusherman/react-native-network-info/issues/43
        // Based on comment https://github.com/pusherman/react-native-network-info/issues/43#issuecomment-358360692
        // added this check in getIPAddress and getIPV4Address
        return NetworkModule.DSLITE_LIST.contains(ip);
    }


    private List<InterfaceAddress> getInetAddresses() {
        List<InterfaceAddress> addresses = new ArrayList<>();
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();

                for (InterfaceAddress interface_address : intf.getInterfaceAddresses()) {
                    addresses.add(interface_address);
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.toString());
        }
        return addresses;
    }


}