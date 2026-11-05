# PhoneLink M393 — אבטיפוס אבחון

האפליקציה בודקת:
- דגם וגרסת Android.
- מכשירי Bluetooth משויכים וסריקה של מכשירים קרובים.
- חיבורי פרופיל HFP Headset ו-A2DP שהנגן חושף.
- התקני שמע לכניסה/יציאה והתקני תקשורת הזמינים ל-Android.
- בקשת בחירת התקן תקשורת דרך AudioManager.setCommunicationDevice (Android 12+).
- צליל בדיקה ומדידת עוצמת מיקרופון מקומית ל-3 שניות; לא נשמר קובץ שמע.

## מה האפליקציה לא עושה
- לא מחייגת ולא עונה לשיחות.
- לא הופכת את הנגן ל-HFP Hands-Free Client מול הטלפון.
- לא מעבירה שיחת HFP שמגיעה מהטלפון לאוזניות. בדיקת setCommunicationDevice היא עבור נתיב התקשורת של האפליקציה בלבד, ואינה מוכיחה ניתוב שיחת מערכת.

## מגבלה עיקרית
ב-AOSP, BluetoothHeadsetClient מסומן @SystemApi ו-@hide; פעולות רבות שלו דורשות BLUETOOTH_PRIVILEGED. אפליקציה רגילה מחנות אינה יכולה להסתמך על API זה. כדי שהנגן ישמש דיבורית מול הטלפון נדרשת תמיכת HFP Client ב-ROM/קושחה, ולעיתים אפליקציית מערכת חתומה/הרשאות privileged. גם אם HFP Client קיים, יש לבדוק האם ה-ROM תומך בו במקביל ל-HFP Headset מול אוזניות.

## בדיקה פיזית נדרשת
1. שייך את הטלפון הכשר ואת אוזניות ה-Bluetooth דרך הגדרות המערכת.
2. פתח PhoneLink ואשר Nearby devices.
3. לחץ על בדיקת מצב; בדוק אם האוזניות מופיעות תחת HFP Headset ולא רק תחת A2DP.
4. לחץ סריקה כדי לבדוק גילוי.
5. רענן התקני שמע לשיחה. אם מופיע Bluetooth SCO/HFP, בחר אותו ובקש מעבר.
6. השמע צליל בדיקה ובדוק היכן הוא נשמע; לאחר מכן הרץ בדיקת מיקרופון.
7. שלח את הדוח ואת דגם הטלפון הכשר המדויק. אין להזין או לשתף מספרי טלפון או מידע אישי.

## בנייה
הפרויקט משתמש ב-Java 17, Android Gradle Plugin 8.7.3, Gradle 8.9, compileSdk 35, minSdk 26 ו-targetSdk 33.
GitHub Actions בונה APK מסוג debug ושומר אותו כ-artifact בשם phonelink-m393-diagnostic-apk.

## מקורות טכניים
- Android Bluetooth permissions: https://developer.android.com/develop/connectivity/bluetooth/bt-permissions
- BluetoothHeadset public API: https://developer.android.com/reference/android/bluetooth/BluetoothHeadset
- BluetoothHeadsetClient AOSP System API: https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/main/framework/java/android/bluetooth/BluetoothHeadsetClient.java
- AudioManager communication routing: https://developer.android.com/reference/android/media/AudioManager
- Android self-managed audio routing guide: https://developer.android.com/develop/connectivity/bluetooth/ble-audio/audio-manager

## הוראות בנייה מקומיות
ב-ZIP המקור שמופק ב-GitHub Actions כלולים גם Gradle Wrapper וקובצי הפרויקט:
- Windows: פתח תיקייה בטרמינל והרץ `gradlew.bat assembleDebug`.
- Linux/macOS: הרץ `./gradlew assembleDebug`.
- Android Studio: בחר Open, פתח את תיקיית הפרויקט, המתן ל-Gradle Sync ואז Build > Build APK(s).
ה-APK יופיע ב-`app/build/outputs/apk/debug/app-debug.apk`.
