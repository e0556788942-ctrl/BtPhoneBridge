# PhoneLink M393 — אבטיפוס אבחון

האפליקציה בודקת (גרסה 1.3):
- דגם וגרסת Android, וכן תכונות מערכת Bluetooth Classic/LE, Telephony ומיקרופון.
- בדיקה פסיבית של רכיב HeadsetClientService בחבילת com.android.bluetooth, כולל מצב ההפעלה לפי PackageManager והגדרות רכיב; זו בדיקת הצהרה בלבד, לא בדיקת זמינות HFP Client בזמן ריצה.
- מכשירי Bluetooth משויכים וסריקה של מכשירים קרובים.
- חיבורי פרופיל HFP Headset ו-A2DP שהנגן חושף.
- גילוי שירותי SDP/UUID של מכשירים משויכים כדי לזהות אם הטלפון מפרסם HFP Audio Gateway ואם האוזניות מפרסמות HFP Hands-Free או HSP Headset. בקשת SDP מקבלת timeout של 15 שניות, ולאחריו ניתן לנסות שוב.
- העתקת דוח האבחון ללוח, בלי לכלול כתובות MAC.
- התקני שמע לכניסה/יציאה והתקני תקשורת הזמינים ל-Android.
- בקשת בחירת התקן תקשורת דרך AudioManager.setCommunicationDevice (Android 12+).
- צליל בדיקה ומדידת עוצמת מיקרופון מקומית ל-3 שניות; לא נשמר קובץ שמע.

## שיפור גרסה 1.3 (2026-11-27) — עמידות בקבלת Profile Proxy

- האפליקציה בודקת את ערך ההחזרה של `BluetoothAdapter.getProfileProxy()` עבור HEADSET ו-A2DP. בקשה ש-Android דחה אינה נשארת במצב pending.
- נשלחות בקשות רק לפרופילים שה-proxy שלהם עדיין חסר.
- אם Android מקבל בקשה אך לא מחזיר callback בתוך 15 שניות, הדוח מציין timeout ומאפשר ניסיון חוזר בלחיצה על "בדיקת מצב ועדכון דוח".
- מעקב הבקשות מנקה timeout ומשתחרר בעת סגירת Activity.
- נוספו בדיקות יחידה למצבי שתי בקשות, בקשה חלקית, דחיית בקשות, callback כפול ואיפוס.

השיפור מטפל באבחון של פרופילי HEADSET/A2DP בלבד. הוא אינו מוסיף HFP Client מול הטלפון, אינו יוצר גישור שמע ואינו מוכיח Dual-HFP.

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
4. המתן לתוצאות SDP/UUID. בטלפון חפש 0x111F (HFP Audio Gateway) או 0x1112 (HSP Audio Gateway); באוזניות חפש 0x111E (HFP Hands-Free), 0x1131 (HSP Headset - HS), 0x1108 (Headset service class כללי — לא מספיק לבדו לזיהוי תפקיד HS) ו-0x110B (A2DP Sink). היעדר UUID אינו הוכחה שאין תמיכה.
5. לחץ סריקה כדי לבדוק גילוי.
6. רענן התקני שמע לשיחה. אם מופיע Bluetooth SCO/HFP, בחר אותו ובקש מעבר.
7. השמע צליל בדיקה ובדוק היכן הוא נשמע; לאחר מכן הרץ בדיקת מיקרופון.
8. לחץ העתק דוח אבחון. לפני שיתוף, בדוק שאין בו מידע אישי; האפליקציה מסתירה כתובות MAC. ציין רק את יצרן ודגם הטלפון הכשר.

## בדיקת HFP Client בגרסאות 0.6–0.7

בגרסה 0.6 נוספה בדיקה פסיבית של רכיב HeadsetClientService בחבילת AOSP com.android.bluetooth. בגרסה 0.7 היא שופרה כך שתציג גם את מצב ההפעלה של החבילה והרכיב לפי PackageManager, לרבות overrides של enabled/disabled.

**הבהרה חשובה בעקבות בדיקת AOSP Android 13:** עצם קיום HeadsetClientService או מצב enabled שלו במניפסט/PackageManager אינם אומרים שהפרופיל פעיל. בקוד AOSP, HeadsetClientService.isEnabled() בודק את BluetoothProperties.isProfileHfpHfEnabled(). זהו מתג מערכת פנימי שאפליקציה רגילה אינה יכולה לאמת באמצעות API ציבורי.

בנוסף, משאב AOSP בשם hfp_client_connection_service_enabled מוגדר false כברירת מחדל ב-Android 13. זהו מתג נפרד לשילוב ConnectionService עם Telecom; אין לפרש אותו כאילו הוא מכבה בהכרח את פרופיל HFP Client עצמו. יצרן יכול לשנות overlays והגדרות מערכת.

לכן בדיקת 0.7 מספקת ראיה טובה יותר לגבי הצהרות PackageManager, אך אינה יכולה לקבוע אם HFP HF הופעל בפועל ב-ROM. לשם כך נדרשת בדיקת חיבור HFP Client אמיתית או בדיקת מערכת/יצרן.

## תיקון חשוב בגרסה 0.5

מקלט אירועי Bluetooth נרשם כעת כ-`RECEIVER_EXPORTED` עם הרשאת שולח `BLUETOOTH_PRIVILEGED`. שירות Bluetooth של Android עשוי לשדר מאפליקציית מערכת בעלת הרשאות גבוהות שאינה רצה ב-UID המערכת; `RECEIVER_NOT_EXPORTED` עלול לחסום אירועי `ACTION_FOUND` ו-`ACTION_UUID`. ההרשאה בצד השולח מצמצמת קבלת שידורים מאפליקציות רגילות. הדבר מתקן את קליטת אירועי הסריקה ותוצאות SDP; הוא אינו מוסיף HFP Client.

## בדיקות אוטומטיות

GitHub Actions מריץ תחילה `testDebugUnitTest`. בדיקות היחידה בודקות את מיפוי UUID של HFP/HSP/A2DP בלבד; הן אינן מדמות חיבור Bluetooth או שיחה.

## בנייה
הפרויקט משתמש ב-Java 17, Android Gradle Plugin 8.7.3, Gradle 8.9, compileSdk 35, minSdk 26 ו-targetSdk 33. גרסת האבחון הנוכחית: 1.2 (versionCode 13).
GitHub Actions בונה APK מסוג debug ושומר אותו כ-artifact בשם phonelink-m393-diagnostic-apk.

## פירוש UUID
UUID מצביע על שירות שהמכשיר מפרסם ב-SDP. הוא מסייע להבחין בין Audio Gateway לבין Hands-Free/Headset, אך אינו מוכיח שהנגן מסוגל להפעיל את פרופיל HFP Client, או ששני קישורי Bluetooth יפעלו במקביל. היעדר UUID עשוי לנבוע ממטמון, SDP שלא הושלם או מגבלות קושחה.

## מקורות טכניים
- מסמך בדיקת היתכנות ומטריצת בדיקות חומרה: [docs/FEASIBILITY_HE.md](docs/FEASIBILITY_HE.md)
- Bluetooth SIG Assigned Numbers (HFP/HSP/A2DP UUIDs): https://www.bluetooth.com/specifications/assigned-numbers/
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


## תיקון דיוק UUID בגרסה 0.8 (2026-11-13)

הגרסה מפרידה בין UUID `0x1131` (Headset - HS, תפקיד HSP Headset) לבין `0x1108` (Headset service class כללי). קודם שניהם סווגו יחד כ-HSP Headset, דבר שהיה עלול להציג תפקיד מדויק מדי על סמך UUID כללי. כעת הדוח מציג את `0x1108` כממצא כללי בלבד, ובדיקות היחידה מוודאות שלא מסיקים ממנו לבדו תפקיד HS. השינוי אינו מוסיף חיבור HFP Client או גישור שמע.

## מעקב חי אחר HFP/SCO בגרסה 0.9

גרסה 0.9 מוסיפה קליטת אירועי מערכת ציבוריים:
- `BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED` — מצב חיבור HFP Headset/Audio Gateway המקומי של הנגן מול האוזניות.
- `BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED` — מצב ערוץ השמע HFP/SCO בצד Headset/Audio Gateway המקומי של הנגן.
- `BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED` — מצב חיבור A2DP, כדי להבדיל בין חיבור מדיה לחיבור דיבורית.

השאר את PhoneLink פתוחה בזמן שיוך/חיבור/ניתוק. האירועים נרשמים בדוח בלי כתובות MAC. אירוע `SCO מחובר` כאן מתייחס לצד Headset/Audio Gateway המקומי מול האוזניות בלבד; הוא אינו מדווח על SCO של HFP Client מול הטלפון ואינו מוכיח ששיחת הטלפון מועברת לאוזניות.

מקור: Android BluetoothHeadset API — https://developer.android.com/reference/android/bluetooth/BluetoothHeadset


## שיפור גרסה 1.0 (2026-11-15) — בדיקת נתיב השמע בפועל

גרסה 1.0 מוסיפה שתי דגימות של הנתיב בפועל, בזמן שהזרם פעיל:
- אחרי הפעלת צליל הבדיקה, AudioTrack.getRoutedDevice() מתעד לאיזה התקן יציאה הזרם נותב בפועל.
- בזמן בדיקת המיקרופון, AudioRecord.getRoutedDevice() מתעד מאיזה התקן כניסה הזרם נקלט בפועל.
- אם Android מחזיר null בזמן הדגימה, הדוח מציין שלא התקבל התקן מנותב; אין להחליף זאת במידע מ-getPreferredDevice() או ברשימת התקנים זמינים.

הבדיקה נשענת על AudioRouting.getRoutedDevice() (API 24 ומעלה), ויש לקרוא לה רק בזמן שה-AudioTrack מנגן או AudioRecord מקליט. היא מספקת ראיה לנתיב של זרמי האודיו של PhoneLink בלבד; היא אינה מוכיחה ששיחת מערכת או שיחת HFP חיצונית מנותבת באותו אופן, ואינה יוצרת גישור בין שני קישורי Bluetooth.

בדיקות היחידה החדשות בודקות רק את עיצוב תיאור הנתיב. הן אינן מדמות Audio HAL, Bluetooth, מיקרופון או חומרה.


## שיפור גרסה 1.1 (2026-11-16) — מעקב רציף אחר שינויי נתיב שמע

- נוספו `AudioRouting.OnRoutingChangedListener` ל-`AudioTrack` ול-`AudioRecord`. הדוח מתעד שינויי נתיב שמתרחשים בזמן שזרם בדיקת הצליל או המיקרופון פעיל, בנוסף לדגימת `getRoutedDevice()` שכבר קיימת.
- אירועי `BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED` ו-`ACTION_AUDIO_STATE_CHANGED` מסומנים במפורש כאירועים של צד HFP Headset/Audio Gateway המקומי של הנגן מול האוזניות. הם אינם בדיקה של HFP Client מול הטלפון.
- לפני בקשת נתיב תקשורת האפליקציה שומרת את מצב `AudioManager` הקודם ומנסה לשחזרו לאחר ניקוי הנתיב או סגירת המסך. אם Android כבר שינה את מצב השמע, האפליקציה נמנעת מדריסה אוטומטית של המצב החדש.

המעקב הרציף משפר את תיעוד שינויי הניתוב, אך עדיין בודק רק זרמי אודיו שנוצרו על ידי PhoneLink. הוא אינו מאזין לשיחת מערכת, אינו פותח HFP Client מול הטלפון ואינו מגשר בין שני קישורי SCO.


## איסוף אבחון מערכת דרך מחשב (ללא Root)

החל מ-2026-11-18 כלי ADB מזהה גם שמות חבילות Bluetooth של יצרנים, ומוסיף בדיקת שירותים פעילים ומדיניות שמע. הוראות מלאות נמצאות ב-[מסמך ההיתכנות](docs/FEASIBILITY_HE.md).

מתוך PowerShell בתיקיית המאגר:
```powershell
.\tools\collect_m393_bt_diagnostics.ps1
```

הכלי דורש Android Platform Tools ו-USB debugging שאושר על הנגן. הוא לקריאה בלבד, לא מתקין דבר ולא משנה הגדרות. לפני שיתוף דוח יש לבדוק שמות התקנים ופרטי build.


## סיכום דוח ADB אופליין (חדש)

אפשר לנתח דוח ADB שנאסף מהנגן בלי לחבר את המחשב שוב: 

```powershell
.\tools\analyze_m393_bt_report.ps1 -ReportPath .\PhoneLink_M393_ADB_YYYYMMDD_HHMMSS.txt
```

כדי לשמור סיכום:

```powershell
.\tools\analyze_m393_bt_report.ps1 -ReportPath .\PhoneLink_M393_ADB_....txt -OutputPath .\PhoneLink_M393_Summary.txt
```

הכלי קורא קובץ בלבד ואינו מפעיל ADB או משנה את הנגן. הוא מסכם מצב Bluetooth, חבילות Bluetooth מועמדות, אזכור HeadsetClientService, אזכור HEADSET_CLIENT, וממצאי מדיניות SCO לכניסה/יציאה. **כל אלה הם רמזי מערכת בלבד ואינם מוכיחים HFP Client פעיל, שני קישורי HFP במקביל או גישור שיחה.**

הוראות מלאות: [docs/ADB_REPORT_ANALYZER_HE.md](docs/ADB_REPORT_ANALYZER_HE.md). בדיקות אופליין: `tools/test_analyze_m393_bt_report.ps1`.


## בדיקת תפקידי HFP דרך מאפייני מערכת (2026-11-25)

כלי ADB אוסף כעת גם את:
- `bluetooth.profile.hfp.hf.enabled` — תפקיד Hands-Free של הנגן מול הטלפון.
- `bluetooth.profile.hfp.ag.enabled` — תפקיד Audio Gateway של הנגן מול האוזניות.

הערכים מופיעים בקטע `Bluetooth HFP role system properties` ומנותחים בנפרד. ערך ריק הוא "לא נחשף/לא ידוע", לא false. גם true הוא רמז לתצורת המערכת בלבד ואינו הוכחת חיבור או פעולה במקביל. ראו [מסמך ההיתכנות](docs/FEASIBILITY_HE.md).


## מטריצת בדיקות חומרה מלאה (2026-11-26)

נוסף מסמך עבודה מדורג שמפריד בין תצורת HFP HF/AG של ה-M393, תפקידי SDP של הטלפון והאוזניות, חיבור כל צד בנפרד, חיבור כפול בו-זמני ובדיקת שמע דו-כיוונית בשיחה. לכל שלב מוגדרים תנאי PASS/FAIL/UNKNOWN, דרישות מערכת, מגבלות ה-API הציבורי ותבנית תוצאות ללא כתובות MAC או פרטים אישיים.

ראו [מטריצת אימות החומרה בעברית](docs/M393_HFP_HARDWARE_TEST_MATRIX_HE.md). המסמך אינו משנה את קוד האפליקציה ואינו טוען שנבדקה חומרת M393.


## עדכון 2026-11-28 — הגנה מפני callback מאוחר של Profile Proxy (גרסה 1.3)

נמצא מרוץ אפשרי בזרימת Profile Proxy: לאחר timeout, המשתמש יכול לבקש שוב את HEADSET/A2DP לפני שה-callback הישן הגיע. ללא זיהוי דור בקשה, callback מאוחר מהבקשה הישנה היה עלול להשלים בטעות את המעקב של הבקשה החדשה, ואף למחוק את handle של ה-timeout החדש.

תיקון גרסה 1.3:
- כל batch של בקשות HEADSET/A2DP מקבל generation ייחודי.
- callbacks מעדכנים את מצב הבקשות רק אם ה-generation שלהם עדיין הנוכחי.
- proxy שמגיע מ-callback של generation שפג תוקפו נסגר באמצעות closeProfileProxy במקום להישמר ב-Activity.
- timeout ישן אינו יכול לאפס batch חדש ואינו יכול למחוק את handle של timeout חדש.
- reset בעת סגירת Activity מבטל את כל ה-callbacks שנוצרו קודם לכן.
- נוספו בדיקות יחידה ל-callback מאוחר ול-timeout ישן שמנסה לאפס בקשה חדשה.

התיקון עוסק במרוץ של אבחון HEADSET/A2DP בלבד. הוא אינו מוסיף HFP Client, אינו יוצר חיוג/מענה ואינו מוכיח Dual-HFP או גישור שמע.
