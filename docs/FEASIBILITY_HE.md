# PhoneLink — בדיקת היתכנות טכנית ממוקדת

תאריך עדכון: 2026-11-13

## מסקנה זמנית

היעד אפשרי רק אם קושחת ה-M393 מספקת שני תפקידי Bluetooth Classic נפרדים במקביל:
1. **HFP Hands-Free (HF) מול הטלפון הכשר**, שבו הטלפון הוא Audio Gateway (AG).
2. **HFP Audio Gateway (AG) מול האוזניות**, שבהן האוזניות הן Hands-Free (HF).

בנוסף, מדיניות השמע של היצרן צריכה לאפשר ששני ערוצי הקול יפעלו יחד וינותבו זה לזה. Android 13 כשלעצמו אינו מבטיח זאת, ו-APK רגיל אינו מקבל API ציבורי מלא לשליטה בתפקיד HFP Client.

נכון לעדכון זה אין תוצאות בדיקה פיזית מה-M393 או מהטלפון הכשר. לכן התאימות אינה מאושרת.

## תפקידי Bluetooth וה-UUIDs שצריך לבדוק

| רכיב | התפקיד הנדרש | UUID | משמעות |
|---|---|---|---|
| הטלפון הכשר | HFP Audio Gateway (AG) | 0x111F | הטלפון מספק שירותי שיחה; הנגן צריך להיות HF Client |
| הטלפון הכשר, חלופה ישנה | HSP Audio Gateway (AG) | 0x1112 | חלופת HSP מוגבלת יותר |
| M393 מול הטלפון | HFP Hands-Free (HF) | 0x111E | זהו תפקיד HFP Client הנדרש בנגן |
| האוזניות | HFP Hands-Free (HF) | 0x111E | האוזניות הן התקן דיבורית מול הנגן |
| האוזניות, חלופה | HSP Headset (HS) | 0x1131 | תפקיד אוזנייה בפרופיל HSP |
| שירות Headset כללי | Headset | 0x1108 | Service Class של HSP; אין להסתמך עליו לבדו |
| אוזניות מדיה | A2DP Audio Sink | 0x110B | קבלת מוזיקה; לא מעיד על מיקרופון/שיחות |

UUID הוא רמז לשירות שההתקן מפרסם ב-SDP. הוא אינו מוכיח שהחיבור הוקם, שהפרופיל פעיל, או שהשמע דו-כיווני. יש לאמת גם מצב חיבור וגם בדיקת שמע.

## מה נבדק בקוד

האבטיפוס הנוכחי (גרסה 0.8):
- מציג גרסת Android ודגם.
- מציג תכונות מערכת: Bluetooth Classic, Bluetooth LE, Telephony ומיקרופון.
- מציג התקנים משויכים ומאפשר סריקת Bluetooth.
- מבקש SDP באמצעות `fetchUuidsWithSdp()` ומציג את UUIDs שמוחזרים ב-`ACTION_UUID`.
- בודק חיבורי HFP Headset ו-A2DP דרך ה-API הציבורי `BluetoothHeadset` ו-`BluetoothA2dp`.
- מציג התקני שמע לכניסה/יציאה והתקני תקשורת זמינים.
- מאפשר לבקש נתיב תקשורת דרך `AudioManager.setCommunicationDevice()`.
- משמיע צליל בדיקה ומודד רמת מיקרופון מקומית למשך שלוש שניות; לא נשמר קובץ קול.

### שיפורי אבחון בגרסה 0.3 (2026-11-08)

- נוסף `AudioDeviceCallback` כדי לתעד בזמן אמת התקני שמע שנוספו או נותקו, ולרענן את רשימת התקני התקשורת.
- נוסף `OnCommunicationDeviceChangedListener` ב-API 31 ומעלה. כך הדוח מתעד את נתיב התקשורת ש-Android מדווח שנבחר, ולא מסתמך רק על תשובת `setCommunicationDevice()` או על השהיה קבועה.
- אחרי בקשת ניתוב נשמרת גם תמונת מצב של התקני הקלט. `setCommunicationDevice()` מקבל התקן יציאה בתפקיד sink; Android בוחר אוטומטית את מקור הקלט המתאים.
- נוספה בקשת `ACCESS_FINE_LOCATION` רק ב-Android 8–11 לצורך סריקת Bluetooth. ב-Android 12 ומעלה נשארות הרשאות Nearby devices (`BLUETOOTH_SCAN` ו-`BLUETOOTH_CONNECT`).
- בדיקת ניתוב חייבת להתבצע כשהאפליקציה נשארת פעילה: בחירת התקן תקשורת נשארת כל עוד תהליך האפליקציה חי, עד `clearCommunicationDevice()` או ניתוק ההתקן. בעת סגירת האפליקציה הקוד מנקה את הבחירה.

העדכונים האלה משפרים את תצפיתיות נתיב השמע, אך אינם מוסיפים HFP Client ואינם יוצרים גישור בין שתי שיחות Bluetooth.


### שיפורי אבחון בגרסה 0.4 (2026-11-09)

- בקשות SDP מקבלות כעת timeout מפורש של 15 שניות. אם לא מתקבל אירוע `ACTION_UUID`, הדוח מציג שהבדיקה לא קיבלה תשובה, מסיר את מצב ה-pending ומאפשר ניסיון נוסף. timeout אינו מפורש כהיעדר תמיכה.
- אירועי Bluetooth נרשמים ב-Android 13 עם `RECEIVER_NOT_EXPORTED`, כדי שה-receiver לא יקבל שידורים מאפליקציות אחרות; הוא עדיין מיועד לקבל אירועי מערכת.
- מיפוי UUID הופרד למחלקה טהורה `BluetoothServiceUuid`, ונוספו בדיקות יחידה ל-HFP AG/HF, ל-HSP AG/HS ולהבחנה בין A2DP Sink ל-A2DP Source.
- GitHub Actions מריץ כעת `testDebugUnitTest` לפני יצירת ה-APK. בדיקות היחידה מאמתות את מיפוי ה-UUID בלבד; הן אינן בדיקת Bluetooth או שמע על חומרה.

השיפורים האלה מתקנים מצב שבו בקשת SDP שלא קיבלה תשובה הייתה נשארת מסומנת כממתינה ללא הגבלת זמן, ומשפרים את בטיחות קליטת אירועי המערכת. הם אינם מוסיפים HFP Client ואינם משנים את מגבלות Android.

## שיפור שבוצע בגרסה 0.6 (2026-11-11)

נוספה בדיקה פסיבית של PackageManager עבור שירות HeadsetClientService בחבילת AOSP com.android.bluetooth. היא מדווחת אם הרכיב מופיע ואם הוא מסומן enabled במניפסט. זהו סימן מוקדם בלבד: הרכיב עשוי להיות disabled על ידי resource overlay, או שהיצרן עשוי להשתמש בשם חבילה אחר. גם רכיב enabled אינו מוכיח ששירות הפרופיל רץ, ש-BluetoothHeadsetClient זמין, או ששיחות ו-SCO עובדים. הבדיקה אינה מנסה לקרוא API מוסתר ואינה דורשת BLUETOOTH_PRIVILEGED.

## שיפור שבוצע בעדכון זה
- תוצאות SDP נקראות כעת מתוך `BluetoothDevice.EXTRA_UUID` של אירוע `ACTION_UUID`, ולא מתוך cache בלבד.
- הדוח מבדיל בין timeout/אין תשובה לבין גילוי שהסתיים עם רשימת UUID ריקה.
- הדוח מציג אם מערכת Android מדווחת על FEATURE_TELEPHONY ועל FEATURE_MICROPHONE. זה חשוב כי התקן Android שאינו מדווח Telephony עשוי להחזיר null עבור התקן תקשורת נבחר.

## מגבלות Android 13

### HFP Client
ב-AOSP, `BluetoothHeadsetClient` הוא API מסומן `@SystemApi` ו-`@hide`. פעולות מסוימות דורשות `BLUETOOTH_CONNECT` וגם `BLUETOOTH_PRIVILEGED`. ההרשאה השנייה אינה הרשאת runtime רגילה לאפליקציה. מימוש אמיתי עשוי לדרוש שינוי ROM/שירות Bluetooth של היצרן, אפליקציית מערכת privileged ורשימת allowlist מתאימה.

לכן אין להוסיף ל-APK רגיל קריאה רפלקטיבית למחלקה מוסתרת ולתאר זאת כתמיכה אמינה. גם אם המחלקה קיימת ב-build, צריך לוודא ששירות HFP Client מופעל ומחובר בפועל.

### אוזניות וניתוב שמע
- `BluetoothHeadset` בודק את תפקיד הנגן מול אוזניות HFP; הוא לא בודק HFP Client מול הטלפון.
- A2DP מיועד בעיקר לשמע מדיה. הוא אינו מספק מיקרופון שיחה.
- `AudioManager.setCommunicationDevice()` בוחר התקן יציאה (sink) עבור נתיב התקשורת של האפליקציה; Android בוחר את מקור הקלט התואם. הבחירה נשארת כל עוד תהליך האפליקציה חי, עד ניקוי או ניתוק. היא אינה יוצרת bridge בין שיחת HFP של הטלפון לבין קישור HFP אחר לאוזניות.
- ניתוב שיחה אמיתי בין שני קישורי HFP דורש תמיכה בשני התפקידים במקביל, ניהול ערוצי SCO/audio focus ומדיניות Audio HAL/Audio Policy מתאימה של היצרן.

### הרשאות
ב-Android 12 ומעלה:
- `BLUETOOTH_SCAN` נדרש לסריקה.
- `BLUETOOTH_CONNECT` נדרש לקריאת התקנים משויכים, שירותים ו-UUIDs.
- `RECORD_AUDIO` נדרש לבדיקת המיקרופון.\n- ב-Android 8–11 נדרשת `ACCESS_FINE_LOCATION` לסריקת Bluetooth; האפליקציה מבקשת אותה רק בגרסאות אלה.
- `MODIFY_AUDIO_SETTINGS` נדרש לשינוי הגדרות נתיב השמע.
- `BLUETOOTH_ADVERTISE` אינו נדרש באבטיפוס הנוכחי, כי הוא אינו מפרסם את הנגן כמכשיר discoverable דרך API של האפליקציה.

## מטריצת בדיקות על חומרה

| בדיקה | מה צריך לראות | מה זה מוכיח / לא מוכיח |
|---|---|---|
| SDP של הטלפון | 0x111F או 0x1112 | הטלפון מפרסם AG; לא מוכיח שהנגן יודע להתחבר אליו |
| SDP של האוזניות | 0x111E או 0x1131 | האוזניות מפרסמות תפקיד דיבורית; לא מוכיח מיקרופון תקין |
| HFP Headset proxy בנגן | האוזניות מופיעות כמחוברות | הנגן פועל כ-AG מול האוזניות |
| חיבור הטלפון לנגן | הטלפון מציג את הנגן כדיבורית ומצב HFP מחובר | ראיה לתמיכת HFP Client; יש לבדוק שיחה בפועל |
| שני חיבורים יחד | הטלפון והאוזניות נשארים מחוברים בו-זמנית | ראיה לתמיכת coexistence; עדיין לא מוכיח bridge |
| צליל בדיקה + בדיקת מיקרופון | צליל נשמע באוזניות וקלט מיקרופון נמדד | בודק את נתיב האפליקציה, לא שיחת טלפון |
| שיחה נכנסת/יוצאת | מענה/חיוג דרך הנגן ושמע דו-כיווני באוזניות | רק בדיקה זו מאמתת את התרחיש המלא |

## מידע שחסר כדי לסיים בדיקת היתכנות

1. דוח PhoneLink שנוצר על ה-M393 לאחר ששויכו הטלפון והאוזניות.
2. יצרן ודגם מדויק של הטלפון הכשר.
3. דגם האוזניות, אם ידוע.
4. האם ב-Bluetooth של הנגן הטלפון מופיע כטלפון/דיבורית, והאם שני ההתקנים נשארים מחוברים יחד.

## מקורות

- Bluetooth SIG Assigned Numbers: https://www.bluetooth.com/specifications/assigned-numbers/
- Android Bluetooth permissions: https://developer.android.com/develop/connectivity/bluetooth/bt-permissions
- BluetoothDevice.fetchUuidsWithSdp / ACTION_UUID: https://developer.android.com/reference/android/bluetooth/BluetoothDevice
- BluetoothHeadset public API: https://developer.android.com/reference/android/bluetooth/BluetoothHeadset
- AOSP BluetoothHeadsetClient source: https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/main/framework/java/android/bluetooth/BluetoothHeadsetClient.java
- AudioManager communication routing: https://developer.android.com/reference/android/media/AudioManager


### תיקון אבטחה וקליטת אירועים בגרסה 0.5 (2026-11-10)

בגרסה 0.4 המקלט הדינמי נרשם עם `RECEIVER_NOT_EXPORTED`. בדיקה מול תיעוד Android גילתה שמקלט כזה עלול לא לקבל שידורים מאפליקציות מערכת בעלות הרשאות גבוהות, כגון Bluetooth, כאשר הן אינן פועלות תחת UID המערכת. הדבר עלול למנוע קבלת `ACTION_FOUND`, `ACTION_DISCOVERY_*` ו-`ACTION_UUID`, ולגרום לסריקה או SDP להיראות כאילו לא התקבלו תוצאות.

בגרסה 0.5 המקלט נרשם כ-`RECEIVER_EXPORTED` ב-Android 13 ומעלה, ובכל הגרסאות הנתמכות הוא מוגן בהרשאת שולח `BLUETOOTH_PRIVILEGED`. כך הוא יכול לקבל שידורים משירות Bluetooth המורשה, תוך צמצום האפשרות שאפליקציה רגילה תזייף אירועים. נדרשת עדיין בדיקה על M393, כי יצרן עשוי לשנות את חבילת Bluetooth או את הרשאותיה.

המקורות: תיעוד Android בנושא [Broadcasts](https://developer.android.com/develop/background-work/background-tasks/broadcasts) מסביר במפורש שיש להשתמש ב-`RECEIVER_EXPORTED` לקבלת שידורים מאפליקציות מערכת בעלות הרשאות גבוהות; מניפסט AOSP של חבילת Bluetooth מבקש `BLUETOOTH_PRIVILEGED`.


## ממצא ממוקד נוסף ממקור Android 13 AOSP

בקובץ התצורה של Bluetooth בענף Android 13, hfp_client_connection_service_enabled מוגדר כברירת מחדל false, וכך גם שירות החיבור ל-Emergency Calls. זה מתייחס לשילוב HFP Client עם Telecom, ואינו לבדו קובע אם פרופיל HFP Client עצמו מופעל: היצרן יכול לספק overlays ותצורה משלו. לכן יש לבדוק גם את רכיב השירות במכשיר עצמו וגם את התנהגות החיבור. מקור: https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/android13-release/android/app/res/values/config.xml

ה-API הציבורי של Android מתעד את BluetoothHeadset כפרופיל Headset/HFP מול אוזניות, ואינו מספק API ציבורי מקביל מלא לשליטה ב-HFP Client. מקור: https://developer.android.com/develop/connectivity/bluetooth/profiles


## עדכון 2026-11-12 — תיקון פרשנות בדיקת HFP Client

בדיקת הקוד בגרסה 0.6 הסתכלה על קיום HeadsetClientService ועל service.enabled. זו אינה בדיקת מצב הפרופיל בזמן ריצה. בגרסה 0.7 היא בודקת גם את מצב ההפעלה שה-PackageManager מחזיר עבור חבילת Bluetooth והרכיב, כולל overrides מפורשים. גם תוצאה זו היא רק מצב package/component.

בדיקת AOSP Android 13 הראתה שני מתגים שונים:
- HeadsetClientService.isEnabled() מחזיר את BluetoothProperties.isProfileHfpHfEnabled().orElse(false). זהו מתג מערכת פנימי לפרופיל HFP Hands-Free Client, שאינו זמין לאפליקציה רגילה דרך API ציבורי.
- המשאב hfp_client_connection_service_enabled מוגדר false כברירת מחדל בקובץ config.xml של AOSP Android 13. זהו מתג נפרד לשילוב שירות החיבור עם Telecom, ואינו זהה להפעלת פרופיל HFP HF.

המשמעות: אם האפליקציה מוצאת HeadsetClientService ומציגה אותו כ-enabled, עדיין אין אישור שהפרופיל נטען. אם אינה מוצאת אותו, ייתכן שהיצרן שינה את חבילת Bluetooth או את המימוש. אין דרך לקבוע את מצב HFP Client המלא מ-APK רגיל בלבד.

מקורות AOSP מדויקים:
- HeadsetClientService.isEnabled() בקוד Bluetooth: https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/android13-release/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientService.java
- ברירת המחדל של hfp_client_connection_service_enabled ב-Android 13: https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/android13-release/android/app/res/values/config.xml
- API BluetoothHeadsetClient מסומן @SystemApi ו-@hide, ופעולות מסוימות דורשות BLUETOOTH_PRIVILEGED: https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/android13-release/framework/java/android/bluetooth/BluetoothHeadsetClient.java

גרסה 0.7 אינה מוסיפה חיוג או גישור שמע; היא משפרת את הדיוק של אבחון המגבלה המרכזית.


## עדכון 2026-11-13 — דיוק בהבחנה בין UUID 0x1108 ל-0x1131 (גרסה 0.8)

בדיקה חוזרת מול רשימת Assigned Numbers של Bluetooth SIG הראתה שיש להבחין בין `0x1131` בשם Headset - HS, לבין `0x1108` בשם Headset (Service Class/Profile). גרסה 0.7 סיווגה את שניהם באותה פונקציה כ-HSP Headset. הדבר היה עלול להציג את `0x1108` כאילו הוא מזהה חד-משמעית את תפקיד HS. בגרסה 0.8:
- `0x1131` בלבד מסווג כ-HSP Headset - HS.
- `0x1108` מוצג בנפרד כ-Headset service class כללי, ואינו משמש לבדו להסקת תפקיד HS.
- נוספו בדיקות יחידה שמוודאות את ההפרדה.

המקור הוא Bluetooth SIG Assigned Numbers, סעיף Headset Profile (HSP), המונה את Headset `0x1108`, Headset Audio Gateway `0x1112`, ו-Headset - HS `0x1131`: https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/Assigned_Numbers/out/en/Assigned_Numbers.pdf

זהו תיקון דיוק בדוח SDP בלבד. הוא אינו מוכיח חיבור, אינו מוסיף HFP Client ואינו משנה את מגבלות Android 13. נדרשת עדיין בדיקה פיזית של M393, הטלפון והאוזניות.
