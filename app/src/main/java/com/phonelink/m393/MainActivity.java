package com.phonelink.m393;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.*;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import java.util.Set;

public class MainActivity extends Activity {
    private static final int REQ_BT = 42;
    private LinearLayout root;
    private TextView output;
    private BluetoothAdapter adapter;
    private BluetoothHeadset headsetProxy;
    private BluetoothA2dp a2dpProxy;
    private final StringBuilder report = new StringBuilder();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        adapter = BluetoothAdapter.getDefaultAdapter();
        buildUi();
        requestBtPermission();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 28, 24, 28);
        root.setBackgroundColor(0xFF101827);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("PhoneLink · בדיקת M393");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("אבחון Bluetooth וניתוב שמע · Android 13");
        subtitle.setTextColor(0xFFB8C7DA);
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        Button refresh = new Button(this);
        refresh.setText("בדוק שוב");
        refresh.setOnClickListener(v -> runDiagnostics());
        root.addView(refresh);

        Button settings = new Button(this);
        settings.setText("פתח הגדרות Bluetooth");
        settings.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); }
            catch (Exception e) { append("לא ניתן לפתוח הגדרות: " + e.getMessage()); }
        });
        root.addView(settings);

        TextView note = new TextView(this);
        note.setText("כלי אבחון בלבד: האפליקציה אינה מחייגת, אינה עונה לשיחות ואינה מנתבת שמע. היא בודקת אילו יכולות Bluetooth מערכת Android מדווחת.");
        note.setTextColor(0xFFFFD58A);
        note.setTextSize(14);
        note.setPadding(0, 18, 0, 18);
        root.addView(note);

        output = new TextView(this);
        output.setTextColor(0xFFE7EDF5);
        output.setTextSize(14);
        root.addView(output);
    }

    private void requestBtPermission() {
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN}, REQ_BT);
        } else runDiagnostics();
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == REQ_BT) runDiagnostics();
    }

    private void runDiagnostics() {
        report.setLength(0);
        line("PHONE LINK · דוח אבחון");
        line("Android API: " + Build.VERSION.SDK_INT + " · דגם: " + Build.MODEL);
        if (adapter == null) { line("לא נמצא מתאם Bluetooth."); showReport(); return; }
        if (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            line("חסרה הרשאת Nearby devices. אשר אותה ולחץ בדוק שוב."); showReport(); return;
        }
        try {
            line("Bluetooth מופעל: " + adapter.isEnabled());
            line("שם מקומי: " + adapter.getName());
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            line("");
            line("מכשירים משויכים (" + bonded.size() + "):");
            if (bonded.isEmpty()) line("אין מכשירים משויכים. שייך טלפון ואוזניות בהגדרות Bluetooth.");
            for (BluetoothDevice d : bonded) {
                line("• " + safeName(d) + " · סוג התקן: " + d.getType() + " · מצב שיוך: " + d.getBondState());
            }
        } catch (SecurityException e) { line("גישה ל-Bluetooth נחסמה: " + e.getMessage()); }

        AudioManager am = (AudioManager)getSystemService(AUDIO_SERVICE);
        line("");
        line("יציאות שמע:");
        if (am != null) appendAudio(am.getDevices(AudioManager.GET_DEVICES_OUTPUTS));
        line("כניסות שמע:");
        if (am != null) appendAudio(am.getDevices(AudioManager.GET_DEVICES_INPUTS));

        line("");
        line("HFP Client (הנגן כדיבורית מול הטלפון):");
        line("אין API ציבורי רגיל לאפליקציות Android לתפקיד זה.");
        line("BluetoothHeadsetClient הוא API מערכת מוסתר ודורש הרשאות מערכת.");
        line("לכן APK רגיל אינו יכול להבטיח חיוג או מענה דרך הטלפון.");
        line("");
        line("שירותי HFP Headset ו-A2DP של הנגן מול אוזניות ייבדקו בנפרד.");
        showReport();
        requestProfileProxies();
    }

    private void requestProfileProxies() {
        if (adapter == null || (Build.VERSION.SDK_INT >= 31 &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)) return;
        BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {
            @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HEADSET) {
                    headsetProxy = (BluetoothHeadset)proxy;
                    appendProfile("שירות HFP Headset", headsetProxy);
                } else if (profile == BluetoothProfile.A2DP) {
                    a2dpProxy = (BluetoothA2dp)proxy;
                    appendProfile("שירות A2DP", a2dpProxy);
                }
            }
            @Override public void onServiceDisconnected(int profile) {
                append("שירות Bluetooth נותק: " + profile); showReport();
            }
        };
        try {
            adapter.getProfileProxy(this, listener, BluetoothProfile.HEADSET);
            adapter.getProfileProxy(this, listener, BluetoothProfile.A2DP);
        } catch (Exception e) { append("שגיאה בבדיקת פרופילים: " + e.getMessage()); showReport(); }
    }

    private void appendProfile(String label, BluetoothProfile proxy) {
        append("");
        append(label + " — מכשירים מחוברים:");
        try {
            Set<BluetoothDevice> unused = null;
            java.util.List<BluetoothDevice> devices = proxy.getConnectedDevices();
            if (devices.isEmpty()) append("אין התקן מחובר כרגע.");
            for (BluetoothDevice d : devices) append("• " + safeName(d));
        } catch (Exception e) { append("לא ניתן לקרוא מצב שירות: " + e.getMessage()); }
        showReport();
    }

    private void appendAudio(AudioDeviceInfo[] devices) {
        line("נמצאו " + devices.length + " התקנים:");
        for (AudioDeviceInfo d : devices) {
            line("• " + d.getProductName() + " · " + audioType(d.getType()) +
                " · מקור: " + d.isSource() + " · יעד: " + d.isSink());
        }
    }

    private String audioType(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO: return "Bluetooth SCO (שיחות)";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP: return "Bluetooth A2DP (מדיה)";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC: return "מיקרופון מובנה";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER: return "רמקול מובנה";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET: return "אוזניות חוטיות עם מיקרופון";
            default: return "סוג שמע " + type;
        }
    }

    private String safeName(BluetoothDevice d) {
        try { return String.valueOf(d.getName()) + " · " + d.getAddress(); }
        catch (Exception e) { return "התקן Bluetooth"; }
    }
    private void line(String s) { report.append(s).append('\n'); }
    private void append(String s) { report.append(s).append('\n'); }
    private void showReport() { if (output != null) output.setText(report.toString()); }

    @Override protected void onDestroy() {
        super.onDestroy();
        try {
            if (adapter != null && headsetProxy != null) adapter.closeProfileProxy(BluetoothProfile.HEADSET, headsetProxy);
            if (adapter != null && a2dpProxy != null) adapter.closeProfileProxy(BluetoothProfile.A2DP, a2dpProxy);
        } catch (Exception ignored) {}
    }
}