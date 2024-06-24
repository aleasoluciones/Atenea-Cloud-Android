package com.ateneacloud.drive.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.ateneacloud.drive.SeadroidApplication;

public class NetworkUtils {

    // Método para verificar si estás conectado al WiFi
    public static boolean isConnectedToWifi() {
        ConnectivityManager connectivityManager = (ConnectivityManager) SeadroidApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            return wifiNetworkInfo != null && wifiNetworkInfo.isConnected();
        }
        return false;
    }

    // Método para verificar si estás utilizando datos móviles
    public static boolean isConnectedToMobileData() {
        ConnectivityManager connectivityManager = (ConnectivityManager) SeadroidApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo mobileNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
            return mobileNetworkInfo != null && mobileNetworkInfo.isConnected();
        }
        return false;
    }

    // Método para verificar si no tienes ninguna conexión de red
    public static boolean isNotConnected() {
        ConnectivityManager connectivityManager = (ConnectivityManager) SeadroidApplication.getAppContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo == null || !activeNetworkInfo.isConnected();
        }
        return true; // Devuelve true por defecto si no se puede obtener información de conectividad
    }
}
