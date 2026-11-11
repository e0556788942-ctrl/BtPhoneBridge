package com.phonelink.m393;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.media.*;
import android.os.Build;
import android.os.ParcelUuid;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private static final int REQ_BT = 42;
    private static final int REQ_MIC = 43;
    private static final int REQ_LEGACY_LOCATION = 44;
    private BluetoothAdapter adapter;
    private BluetoothHeadset headsetProxy;
    private BluetoothA2dp a2dpProxy;
    private AudioManager audioManager;
    private TextView output;
    private Spinner routeSpinner;
    private final StringBuilder report = new StringBuilder();
    private final Set<String> discovered = new LinkedHashSet<>();
    private final Set<String> uuidRequests = new HashSet<>();
    private final Map<String, Runnable> uuidTimeouts = new HashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<AudioDeviceInfo> communicationDevices = new ArrayList<>();
    private boolean receiverRegistered;
    private boolean profilesRequested;
    private boolean micTestRequested;
    private AudioTrack testTrack;
    private AudioRecord audioRecord;
    private volatile boolean micRunning;
    private AudioDeviceCallback audioDeviceCallback;
    private AudioManager.OnCommunicationDeviceChangedListener communicationListener;

    private final BroadcastReceiver discoveryReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device;
                if (Build.VERSION.SDK_INT >= 33) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                } else {
                    //noinspection deprecation
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                }
                if (device != null) {
                    String item = safeName(device);
                    if (discovered.add(item)) append("נמצא בסריקה: " + item);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                append("סריקת Bluetooth התחילה…");
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                append("סריקת Bluetooth הסתיימה.");
            } else if (BluetoothDevice.ACTION_UUID.equals(action)) {
                BluetoothDevice device;
                ParcelUuid[] uuids;
                if (Build.VERSION.SDK_INT >= 33) {
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice.class);
                    uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID, ParcelUuid.class);
                } else {
                    //noinspection deprecation
                    device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    //noinspection deprecation
                    android.os.Parcelable[] raw = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);
                    uuids = null;
                    if (raw != null) {
                        uuids = new ParcelUuid[raw.length];
                        for (int i = 0; i < raw.length; i++) {
                            if (raw[i] instanceof ParcelUuid) uuids[i] = (ParcelUuid) raw[i];
                        }
                    }
                }
                if (device != null) {
                    try {
                        String address = device.getAddress();
                        uuidRequests.remove(address);
                        Runnable timeout = uuidTimeouts.remove(address);
                        if (timeout != null) mainHandler.removeCallbacks(timeout);
                    } catch (SecurityException ignored) {}
                    appendPeerUuidReport(device, uuids, "תוצאת גילוי SDP");
                }
            }
            showReport();
        }
    };

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        adapter = BluetoothAdapter.getDefaultAdapter();
        audioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
        buildUi();
        registerDiscoveryReceiver();
        registerAudioCallbacks();
        requestBluetoothPermissions();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(20, 24, 20, 24);
        root.setBackgroundColor(0xFF101827);
        scroll.addView(root);
        setContentView(scroll);

        TextView title = new TextView(this);
        title.setText("PhoneLink · M393");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("אבחון Bluetooth, פרופילים וניתוב שמע · Android 13");
        subtitle.setTextColor(0xFFB8C7DA);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, 4, 0, 14);
        root.addView(subtitle);

        addButton(root, "בדיקת מצב ועדכון דוח", v -> runDiagnostics());
        addButton(root, "סרוק מכשירי Bluetooth בסביבה", v -> startDiscovery());
        addButton(root, "פתח הגדרות Bluetooth לשיוך וחיבור", v -> {
            try { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); }
            catch (Exception e) { append("לא ניתן לפתוח הגדרות: " + e.getMessage()); showReport(); }
        });

        TextView routeTitle = new TextView(this);
        routeTitle.setText("בדיקת נתיב שמע לשיחה");
        routeTitle.setTextColor(0xFFFFFFFF);
        routeTitle.setTextSize(18);
        routeTitle.setPadding(0, 18, 0, 4);
        root.addView(routeTitle);

        routeSpinner = new Spinner(this);
        root.addView(routeSpinner);
        addButton(root, "רענן התקני שמע לשיחה", v -> refreshCommunicationDevices());
        addButton(root, "בקש מעבר להתקן הנבחר", v -> selectCommunicationDevice());
        addButton(root, "נקה בחירת נתיב שמע", v -> clearCommunicationDevice());
        addButton(root, "השמע צליל בדיקה קצר", v -> playTestTone());
        addButton(root, "בדיקת מיקרופון ל-3 שניות (לא נשמר)", v -> requestMicPermissionAndTest());
        addButton(root, "העתק דוח אבחון", v -> copyReport());

        TextView note = new TextView(this);
        note.setText("חשוב: זהו אבטיפוס אבחוני. הוא אינו מחייג ואינו עונה לשיחות. בדיקת נתיב השמע בודקת את נתיב התקשורת של האפליקציה בלבד; היא אינה מגשרת שיחת HFP מהטלפון אל האוזניות.");
        note.setTextColor(0xFFFFD58A);
        note.setTextSize(14);
        note.setPadding(0, 18, 0, 18);
        root.addView(note);

        output = new TextView(this);
        output.setTextColor(0xFFE7EDF5);
        output.setTextSize(13);
        output.setTextIsSelectable(true);
        root.addView(output);
    }

    private void addButton(LinearLayout root, String label, android.view.View.OnClickListener click) {
        Button b = new Button(this);
        b.setText(label);
        b.setOnClickListener(click);
        root.addView(b);
    }

    private void registerDiscoveryReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_UUID);
        // Bluetooth broadcasts are sent by the privileged Bluetooth app UID,
        // not necessarily by Android's system UID. NOT_EXPORTED can block them.
        // Export only behind the signature/privileged sender permission.
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(discoveryReceiver, filter,
                Manifest.permission.BLUETOOTH_PRIVILEGED, null, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(discoveryReceiver, filter,
                Manifest.permission.BLUETOOTH_PRIVILEGED, null);
        }
        receiverRegistered = true;
    }

    private void requestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            List<String> missing = new ArrayList<>();
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                missing.add(Manifest.permission.BLUETOOTH_CONNECT);
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                missing.add(Manifest.permission.BLUETOOTH_SCAN);
            if (!missing.isEmpty()) {
                requestPermissions(missing.toArray(new String[0]), REQ_BT);
                return;
            }
        } else if (Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LEGACY_LOCATION);
            return;
        }
        runDiagnostics();
    }

    @Override public void onRequestPermissionsResult(int code, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(code, permissions, results);
        if (code == REQ_BT) {
            boolean granted = true;
            for (int result : results) granted &= result == PackageManager.PERMISSION_GRANTED;
            if (granted) runDiagnostics();
            else { append("נדרשת הרשאת Nearby devices כדי לבדוק Bluetooth."); showReport(); }
        } else if (code == REQ_LEGACY_LOCATION) {
            runDiagnostics();
            append(results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED
                ? "הרשאת מיקום אושרה; היא נדרשת לסריקת Bluetooth בגרסאות Android 8–11."
                : "הרשאת מיקום לא אושרה; ב-Android 8–11 ייתכן שלא ניתן יהיה לסרוק התקנים. התקנים משויכים עדיין ניתנים לבדיקה.");
            showReport();
        } else if (code == REQ_MIC) {
            if (results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) runMicTest();
            else { append("הרשאת מיקרופון לא אושרה; לא בוצעה בדיקת מיקרופון."); showReport(); }
        }
    }

    private boolean hasBtPermission() {
        return Build.VERSION.SDK_INT < 31 ||
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasScanPermission() {
        if (Build.VERSION.SDK_INT >= 31)
            return checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED;
        return Build.VERSION.SDK_INT < 23 ||
            checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void registerAudioCallbacks() {
        if (audioManager == null) return;
        audioDeviceCallback = new AudioDeviceCallback() {
            @Override public void onAudioDevicesAdded(AudioDeviceInfo[] added) {
                append("מערכת השמע דיווחה על התקן/ים שנוספו:");
                appendAudio(added, "נוסף");
                refreshCommunicationDevices();
            }
            @Override public void onAudioDevicesRemoved(AudioDeviceInfo[] removed) {
                append("מערכת השמע דיווחה על התקן/ים שנותקו:");
                appendAudio(removed, "נותק");
                refreshCommunicationDevices();
            }
        };
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, new android.os.Handler(getMainLooper()));
        if (Build.VERSION.SDK_INT >= 31) {
            communicationListener = device -> {
                append("Android דיווח על שינוי נתיב תקשורת: " +
                    (device == null ? "אין התקן תקשורת נבחר" : device.getProductName() + " · " + audioType(device.getType())));
                appendAudio(audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS), "כניסות לאחר שינוי נתיב");
                showReport();
            };
            audioManager.addOnCommunicationDeviceChangedListener(getMainExecutor(), communicationListener);
        }
    }

    private void runDiagnostics() {
        report.setLength(0);
        line("PHONE LINK · דוח אבחון");
        line("Android API: " + Build.VERSION.SDK_INT + " · דגם: " + Build.MODEL);
        PackageManager pm = getPackageManager();
        line("יכולות מערכת:");
        line("• Bluetooth Classic: " + pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH));
        line("• Bluetooth LE: " + pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
        line("• Telephony feature: " + pm.hasSystemFeature(PackageManager.FEATURE_TELEPHONY));
        line("• מיקרופון: " + pm.hasSystemFeature(PackageManager.FEATURE_MICROPHONE));
        appendHfpClientServiceProbe(pm);
        if (adapter == null) {
            line("לא נמצא מתאם Bluetooth.");
            showReport();
            return;
        }
        if (!hasBtPermission()) {
            line("חסרה הרשאת Nearby devices. אשר אותה ולחץ שוב.");
            showReport();
            return;
        }
        try {
            line("Bluetooth מופעל: " + adapter.isEnabled());
            line("שם מקומי: " + adapter.getName());
            Set<BluetoothDevice> bonded = adapter.getBondedDevices();
            line("");
            line("מכשירים משויכים (" + bonded.size() + "):");
            if (bonded.isEmpty()) line("אין מכשירים משויכים. שייך את הטלפון ואת האוזניות בהגדרות.");
            for (BluetoothDevice d : bonded) {
                line("• " + safeName(d) + " · סוג התקן: " + d.getType() + " · שיוך: " + d.getBondState());
                requestPeerUuidCheck(d);
            }
        } catch (SecurityException e) {
            line("גישה ל-Bluetooth נחסמה: " + e.getMessage());
        }

        line("");
        line("פרופילי Bluetooth של הנגן:");
        line("HFP Headset = הנגן כ-AG מול אוזניות HF; A2DP = שמע מדיה.");
        line("הם אינם מוכיחים שהנגן תומך ב-HFP Client מול הטלפון.");
        line("HFP Client הוא תפקיד נפרד שנדרש כדי שהנגן יופיע לטלפון כדיבורית.");
        line("אין API ציבורי לאפליקציה רגילה שמאמת את מצב HFP Client המקומי של ה-ROM; UUID של הטלפון לבדו אינו מספיק.");
        requestProfileProxies();
        appendAudioInventory();
        refreshCommunicationDevices();
        line("");
        line("מגבלת Android: BluetoothHeadsetClient הוא System API מוסתר, ומוגן גם ב-BLUETOOTH_PRIVILEGED.");
        line("APK רגיל אינו מקבל הרשאה זו. נדרשת תמיכת ROM/יצרן או אפליקציית מערכת חתומה.");
        showReport();
    }


    /**
     * Best-effort passive check for the AOSP Bluetooth HFP Client service component.
     * A declared/enabled component is not proof that the profile is running or usable.
     */
    private void appendHfpClientServiceProbe(PackageManager pm) {
        line("");
        line("בדיקת רכיב HFP Client במערכת (בדיקה פסיבית):");
        final String bluetoothPackage = "com.android.bluetooth";
        try {
            PackageInfo info;
            long flags = PackageManager.GET_SERVICES | PackageManager.MATCH_DISABLED_COMPONENTS;
            if (Build.VERSION.SDK_INT >= 33) {
                info = pm.getPackageInfo(bluetoothPackage, PackageManager.PackageInfoFlags.of(flags));
            } else {
                //noinspection deprecation
                info = pm.getPackageInfo(bluetoothPackage, (int) flags);
            }
            boolean found = false;
            if (info.services != null) {
                for (android.content.pm.ServiceInfo service : info.services) {
                    String name = service.name == null ? "" : service.name;
                    if (HfpClientServiceDetector.matches(name)) {
                        found = true;
                        boolean appEnabled = info.applicationInfo != null && info.applicationInfo.enabled;
                        boolean effectiveByManifest = service.enabled && appEnabled;
                        line("נמצא רכיב: " + name);
                        line("מוצהר כפעיל במניפסט: " + effectiveByManifest);
                        line(effectiveByManifest
                            ? "סימן חיובי בלבד: הרכיב מוצהר כפעיל; עדיין לא הוכח ששירות HFP Client עלה או שניתן להתחבר אליו."
                            : "הרכיב קיים אך מוצהר כמושבת; נדרש שינוי קושחה/overlay של היצרן כדי להפעילו.");
                    }
                }
            }
            if (!found) {
                line("לא נמצא רכיב HFP Client בחבילת com.android.bluetooth.");
                line("ייתכן שהיצרן משתמש בחבילת Bluetooth אחרת; תוצאה זו אינה הוכחה מוחלטת שאין HFP Client.");
            }
        } catch (PackageManager.NameNotFoundException e) {
            line("חבילת com.android.bluetooth אינה גלויה/מותקנת בשם זה; ייתכן שם חבילה של יצרן.");
        } catch (SecurityException e) {
            line("Android חסם בדיקת רכיבי מערכת: " + e.getMessage());
        } catch (RuntimeException e) {
            line("בדיקת רכיב HFP Client לא הושלמה: " + e.getClass().getSimpleName());
        }
        line("הבדיקה אינה קוראת API מוסתר, אינה מתחברת לפרופיל ואינה מאמתת שיחות או ניתוב שמע.");
    }

    private void requestProfileProxies() {
        if (adapter == null || !hasBtPermission()) return;
        if (headsetProxy != null) appendProfile("HFP Headset (הנגן מול אוזניות)", headsetProxy);
        if (a2dpProxy != null) appendProfile("A2DP (מדיה)", a2dpProxy);
        if (profilesRequested) return;
        profilesRequested = true;
        BluetoothProfile.ServiceListener listener = new BluetoothProfile.ServiceListener() {
            @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
                if (profile == BluetoothProfile.HEADSET) {
                    headsetProxy = (BluetoothHeadset)proxy;
                    appendProfile("HFP Headset (הנגן מול אוזניות)", headsetProxy);
                } else if (profile == BluetoothProfile.A2DP) {
                    a2dpProxy = (BluetoothA2dp)proxy;
                    appendProfile("A2DP (מדיה)", a2dpProxy);
                }
                showReport();
            }
            @Override public void onServiceDisconnected(int profile) {
                if (profile == BluetoothProfile.HEADSET) headsetProxy = null;
                if (profile == BluetoothProfile.A2DP) a2dpProxy = null;
                profilesRequested = false;
                append("שירות Bluetooth נותק: " + profile);
                showReport();
            }
        };
        try {
            adapter.getProfileProxy(this, listener, BluetoothProfile.HEADSET);
            adapter.getProfileProxy(this, listener, BluetoothProfile.A2DP);
        } catch (Exception e) {
            append("שגיאה בבדיקת פרופילים: " + e.getMessage());
        }
    }

    private void appendProfile(String label, BluetoothProfile proxy) {
        append("");
        append(label + " — מחוברים:");
        try {
            List<BluetoothDevice> devices = proxy.getConnectedDevices();
            if (devices.isEmpty()) append("אין התקן מחובר כרגע.");
            for (BluetoothDevice d : devices) append("• " + safeName(d));
        } catch (Exception e) {
            append("לא ניתן לקרוא מצב שירות: " + e.getMessage());
        }
    }

    private void startDiscovery() {
        if (adapter == null || !hasBtPermission()) {
            append("אין מתאם Bluetooth או חסרה הרשאת Nearby devices.");
            showReport();
            return;
        }
        if (!hasScanPermission()) {
            if (Build.VERSION.SDK_INT >= 31)
                requestPermissions(new String[]{Manifest.permission.BLUETOOTH_SCAN}, REQ_BT);
            else
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQ_LEGACY_LOCATION);
            return;
        }
        try {
            discovered.clear();
            if (adapter.isDiscovering()) adapter.cancelDiscovery();
            boolean started = adapter.startDiscovery();
            append(started ? "בקשת סריקה נשלחה." : "המערכת לא התחילה סריקה. בדוק Bluetooth והגדרות מערכת.");
        } catch (SecurityException e) {
            append("הסריקה נחסמה: " + e.getMessage());
        }
        showReport();
    }

    private void appendAudioInventory() {
        if (audioManager == null) return;
        line("");
        line("כל התקני שמע שמחוברים כעת:");
        appendAudio(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS), "יציאה");
        appendAudio(audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS), "כניסה");
    }

    private void refreshCommunicationDevices() {
        if (audioManager == null || routeSpinner == null) return;
        communicationDevices.clear();
        List<String> labels = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 31) {
            communicationDevices.addAll(audioManager.getAvailableCommunicationDevices());
            for (AudioDeviceInfo d : communicationDevices) {
                labels.add(d.getProductName() + " · " + audioType(d.getType()) + " · ID " + d.getId());
            }
            AudioDeviceInfo current = audioManager.getCommunicationDevice();
            append("התקני תקשורת זמינים: " + communicationDevices.size() +
                (current == null ? " · אין נתיב תקשורת שנבחר כעת." : " · נבחר כעת: " + current.getProductName()));
        } else {
            labels.add("נדרש Android 12 ומעלה ל-API זה");
            append("setCommunicationDevice זמין מ-API 31.");
        }
        if (labels.isEmpty()) labels.add("לא נמצאו התקני תקשורת זמינים");
        routeSpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, labels));
        showReport();
    }

    private void selectCommunicationDevice() {
        if (Build.VERSION.SDK_INT < 31 || audioManager == null) {
            append("בחירת נתיב תקשורת דורשת Android 12 ומעלה.");
            showReport();
            return;
        }
        int index = routeSpinner.getSelectedItemPosition();
        if (index < 0 || index >= communicationDevices.size()) {
            append("אין התקן תקשורת מתאים לבחירה.");
            showReport();
            return;
        }
        AudioDeviceInfo device = communicationDevices.get(index);
        try {
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
            boolean accepted = audioManager.setCommunicationDevice(device);
            append("בקשת ניתוב ל-" + device.getProductName() + ": " + (accepted ? "התקבלה; יש לאמת בפועל." : "נדחתה."));
            new android.os.Handler(getMainLooper()).postDelayed(() -> {
                AudioDeviceInfo actual = audioManager.getCommunicationDevice();
                append("התקן תקשורת שנבחר לפי Android: " +
                    (actual == null ? "לא דווח התקן נבחר." : actual.getProductName() + " · " + audioType(actual.getType())));
                appendAudio(audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS), "כניסות שמע לאחר בחירת נתיב");
                showReport();
            }, 700);
        } catch (Exception e) {
            append("שגיאה בבחירת נתיב: " + e.getMessage());
        }
        showReport();
    }

    private void clearCommunicationDevice() {
        if (Build.VERSION.SDK_INT >= 31 && audioManager != null) {
            audioManager.clearCommunicationDevice();
            audioManager.setMode(AudioManager.MODE_NORMAL);
            append("ניקיתי את בחירת נתיב התקשורת של האפליקציה.");
        }
        showReport();
    }

    private void playTestTone() {
        if (audioManager == null) return;
        try {
            int rate = 16000;
            int samples = rate;
            short[] data = new short[samples];
            for (int i = 0; i < samples; i++) {
                data[i] = (short)(Math.sin(2.0 * Math.PI * 440.0 * i / rate) * 3500);
            }
            AudioTrack track = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build())
                .setAudioFormat(new AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(rate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build())
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(data.length * 2)
                .build();
            track.write(data, 0, data.length);
            track.setVolume(0.18f);
            track.play();
            append("הושמע צליל בדיקה קצר. בדוק באיזה התקן שמעת אותו.");
            new android.os.Handler(getMainLooper()).postDelayed(() -> {
                try { track.stop(); } catch (Exception ignored) {}
                track.release();
            }, 1200);
        } catch (Exception e) {
            append("לא ניתן להשמיע צליל בדיקה: " + e.getMessage());
        }
        showReport();
    }

    private void requestMicPermissionAndTest() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_MIC);
        } else runMicTest();
    }

    private void runMicTest() {
        if (micRunning) return;
        micRunning = true;
        new Thread(() -> {
            int rate = 16000;
            int min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (min <= 0) {
                runOnUiThread(() -> { append("לא ניתן ליצור חוצץ להקלטת בדיקה."); showReport(); micRunning = false; });
                return;
            }
            AudioRecord recorder = null;
            try {
                recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, rate,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, 4096));
                if (recorder.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("AudioRecord לא אותחל");
                audioRecord = recorder;
                short[] buffer = new short[1024];
                long sumSquares = 0;
                long count = 0;
                recorder.startRecording();
                long end = System.currentTimeMillis() + 3000;
                while (System.currentTimeMillis() < end && micRunning) {
                    int n = recorder.read(buffer, 0, buffer.length);
                    if (n > 0) {
                        for (int i = 0; i < n; i++) {
                            long x = buffer[i];
                            sumSquares += x * x;
                        }
                        count += n;
                    }
                }
                final double rms = count == 0 ? 0 : Math.sqrt((double)sumSquares / count);
                runOnUiThread(() -> {
                    append("בדיקת מיקרופון הסתיימה. רמת RMS: " + String.format(Locale.US, "%.1f", rms) +
                        " (נמדד מקומית בלבד; לא נשמרה הקלטה).");
                    showReport();
                });
            } catch (Exception e) {
                runOnUiThread(() -> { append("בדיקת מיקרופון נכשלה: " + e.getMessage()); showReport(); });
            } finally {
                micRunning = false;
                if (recorder != null) {
                    try { recorder.stop(); } catch (Exception ignored) {}
                    recorder.release();
                }
                audioRecord = null;
            }
        }, "PhoneLink-Mic-Test").start();
    }

    private void appendAudio(AudioDeviceInfo[] devices, String direction) {
        line(direction + " (" + devices.length + "):");
        for (AudioDeviceInfo d : devices) {
            line("• " + d.getProductName() + " · " + audioType(d.getType()) +
                " · מקור: " + d.isSource() + " · יעד: " + d.isSink());
        }
    }

    private String audioType(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO: return "Bluetooth SCO/HFP (שיחות)";
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP: return "Bluetooth A2DP (מדיה)";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC: return "מיקרופון מובנה";
            case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER: return "רמקול מובנה";
            case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE: return "אפרכסת";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET: return "אוזניות חוטיות עם מיקרופון";
            default: return "סוג שמע " + type;
        }
    }

    private void requestPeerUuidCheck(BluetoothDevice device) {
        String key = null;
        try {
            ParcelUuid[] cached = device.getUuids();
            if (cached != null && cached.length > 0) appendPeerUuidReport(device, cached, "UUID שמורים");
            else append("UUID של " + safeName(device) + ": אין מידע שמור עדיין; נשלחת בקשת SDP.");
            key = device.getAddress();
            if (uuidRequests.add(key)) {
                boolean started = device.fetchUuidsWithSdp();
                if (started) {
                    final String requestKey = key;
                    Runnable timeout = () -> {
                        uuidRequests.remove(requestKey);
                        uuidTimeouts.remove(requestKey);
                        append("לא התקבלה תשובת SDP עבור " + safeName(device) +
                            " בתוך 15 שניות. אין להסיק מכך שאין תמיכה; אפשר ללחוץ שוב על בדיקת מצב.");
                        showReport();
                    };
                    uuidTimeouts.put(key, timeout);
                    mainHandler.postDelayed(timeout, 15000);
                    append("בקשת גילוי שירותי SDP עבור " + safeName(device) + ": נשלחה; ממתין עד 15 שניות.");
                } else {
                    uuidRequests.remove(key);
                    append("בקשת גילוי שירותי SDP עבור " + safeName(device) + " לא התחילה; ניתן לנסות שוב.");
                }
            } else {
                append("בקשת SDP עבור " + safeName(device) + " כבר ממתינה לתוצאה.");
            }
        } catch (SecurityException e) {
            if (key != null) uuidRequests.remove(key);
            append("אין הרשאה לקרוא UUID של " + safeName(device) + ": " + e.getMessage());
        }
    }

    private void appendPeerUuidReport(BluetoothDevice device, ParcelUuid[] uuids, String source) {
        append("");
        append(source + " של " + safeName(device) + ":");
        if (uuids == null) {
            append("לא התקבלה רשימת UUID (ייתכן timeout או כשל SDP). אין להסיק מכך שאין תמיכה.");
            return;
        }
        if (uuids.length == 0) {
            append("גילוי SDP הסתיים ללא UUID. גם זו אינה הוכחה מוחלטת שאין תמיכה.");
            return;
        }
        boolean hfpAg = false, hfpHf = false, hspAg = false, hspHs = false, a2dpSink = false;
        for (ParcelUuid parcelUuid : uuids) {
            String id = parcelUuid.toString().toLowerCase(Locale.US);
            String label = BluetoothServiceUuid.label(id);
            hfpAg |= BluetoothServiceUuid.isHfpAudioGateway(id);
            hfpHf |= BluetoothServiceUuid.isHfpHandsFree(id);
            hspAg |= BluetoothServiceUuid.isHspAudioGateway(id);
            hspHs |= BluetoothServiceUuid.isHspHeadset(id);
            a2dpSink |= BluetoothServiceUuid.isA2dpSink(id);
            append("• " + id + " — " + label);
        }
        if (hfpAg || hspAg) append("ממצא: ההתקן מפרסם תפקיד Audio Gateway; זה סימן חיובי לטלפון שמולו הנגן אמור להיות Hands-Free.");
        if (hfpHf || hspHs) append("ממצא: ההתקן מפרסם תפקיד Hands-Free/Headset; זה סימן חיובי לאוזניות.");
        if (a2dpSink) append("ממצא: ההתקן מפרסם A2DP Sink, תפקיד נפוץ באוזניות מדיה.");
        append("הערה: UUID מסייע לזהות תפקידים שההתקן מפרסם, אך אינו מוכיח שחיבור, שמע דו-כיווני או עבודה בו-זמנית יתאפשרו.");
    }

    private void copyReport() {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if (clipboard == null) return;
        clipboard.setPrimaryClip(ClipData.newPlainText("PhoneLink diagnostic report", report.toString()));
        Toast.makeText(this, "דוח האבחון הועתק", Toast.LENGTH_SHORT).show();
    }

    private String safeName(BluetoothDevice d) {
        try {
            String name = d.getName();
            return name == null || name.trim().isEmpty() ? "מכשיר Bluetooth" : name;
        } catch (Exception e) { return "מכשיר Bluetooth"; }
    }
    private void line(String s) { report.append(s).append('\n'); }
    private void append(String s) { report.append(s).append('\n'); }
    private void showReport() { if (output != null) output.setText(report.toString()); }

    @Override protected void onDestroy() {
        super.onDestroy();
        micRunning = false;
        for (Runnable timeout : uuidTimeouts.values()) mainHandler.removeCallbacks(timeout);
        uuidTimeouts.clear();
        uuidRequests.clear();
        try { if (adapter != null && adapter.isDiscovering()) adapter.cancelDiscovery(); } catch (Exception ignored) {}
        if (receiverRegistered) {
            try { unregisterReceiver(discoveryReceiver); } catch (Exception ignored) {}
        }
        if (audioManager != null && audioDeviceCallback != null) {
            try { audioManager.unregisterAudioDeviceCallback(audioDeviceCallback); } catch (Exception ignored) {}
        }
        if (Build.VERSION.SDK_INT >= 31 && audioManager != null) {
            if (communicationListener != null) {
                try { audioManager.removeOnCommunicationDeviceChangedListener(communicationListener); } catch (Exception ignored) {}
            }
            try { audioManager.clearCommunicationDevice(); audioManager.setMode(AudioManager.MODE_NORMAL); } catch (Exception ignored) {}
        }
        try {
            if (adapter != null && headsetProxy != null) adapter.closeProfileProxy(BluetoothProfile.HEADSET, headsetProxy);
            if (adapter != null && a2dpProxy != null) adapter.closeProfileProxy(BluetoothProfile.A2DP, a2dpProxy);
        } catch (Exception ignored) {}
    }
}