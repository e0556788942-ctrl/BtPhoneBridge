# PhoneLink — בדיקת היתכנות טכנית ממוקדת

תאריך עדכון: 2026-11-16

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

האבטיפוס הנוכחי (גרסה 1.1):
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
- בגרסה 0.4 המקלט נרשם זמנית עם `RECEIVER_NOT_EXPORTED`; הדבר תוקן בגרסה 0.5, כמפורט בסעיף תיקון האבטחה בהמשך. אין להתייחס להגדרת 0.4 כהתנהגות הגרסה הנוכחית.
- מיפוי UUID הופרד למחלקה טהורה `BluetoothServiceUuid`, ונוספו בדיקות יחידה ל-HFP AG/HF, ל-HSP AG/HS ולהבחנה בין A2DP Sink ל-A2DP Source.
- GitHub Actions מריץ כעת `testDebugUnitTest` לפני יצירת ה-APK. בדיקות היחידה מאמתות את מיפוי ה-UUID בלבד; הן אינן בדיקת Bluetooth או שמע על חומרה.

השיפורים האלה מתקנים מצב שבו בקשת SDP שלא קיבלה תשובה הייתה נשארת מסומנת כממתינה ללא הגבלת זמן. תיקון קליטת אירועי Bluetooth נעשה לאחר מכן בגרסה 0.5. הם אינם מוסיפים HFP Client ואינם משנים את מגבלות Android.

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

## שיפור אבחון בגרסה 0.9 (2026-11-14) — מעקב אירועי HFP/SCO

נוספו למקלט האירועים של האפליקציה:
- `BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED`: מתעד מעבר בין CONNECTING/CONNECTED/DISCONNECTING/DISCONNECTED של קישור HFP מול התקן Headset.
- `BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED`: מתעד את מצב ערוץ HFP SCO: CONNECTING, CONNECTED או DISCONNECTED.
- `BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED`: מתעד חיבור A2DP בנפרד, כדי שלא לבלבל חיבור מדיה עם חיבור דיבורית.

האירועים נרשמים בדוח עם שם ההתקן בלבד, ללא כתובת MAC. יש להשאיר את האפליקציה פתוחה בזמן הבדיקה; המקלט הוא דינמי ואינו משחזר אירועים שהתרחשו לפני פתיחתה. אירוע SCO_CONNECTED הוא ראיה לכך שערוץ HFP audio נפתח מול התקן ה-Headset המקומי. הוא אינו מוכיח שהשיחה מהטלפון נכנסת לנגן, שהנגן מפעיל HFP Client מול הטלפון, או שקיים גישור בין שני קישורי HFP.

נוספו בדיקות יחידה למיפוי מצבי חיבור ומצבי SCO. הן בודקות את תוויות המצבים בלבד, ולא מדמות Bluetooth או חומרה.

מקור API רשמי:
- BluetoothHeadset ACTION_CONNECTION_STATE_CHANGED / ACTION_AUDIO_STATE_CHANGED / STATE_AUDIO_CONNECTED: https://developer.android.com/reference/android/bluetooth/BluetoothHeadset
- BluetoothA2dp ACTION_CONNECTION_STATE_CHANGED: https://developer.android.com/reference/android/bluetooth/BluetoothA2dp


## שיפור אבחון בגרסה 1.0 (2026-11-15) — נתיב AudioTrack/AudioRecord בפועל

הבדיקה הקודמת הציגה התקני שמע זמינים ואת התקן התקשורת שנבחר ב-AudioManager, אך אלה אינם בהכרח ההתקן שאליו זרם מסוים מנותב בפועל. גרסה 1.0 מוסיפה:
- דגימה של AudioTrack.getRoutedDevice() לאחר תחילת השמעת צליל הבדיקה.
- דגימה של AudioRecord.getRoutedDevice() בזמן הקלטת המיקרופון.
- הצגת שם התקן, סוגו וכיוון source/sink; אם לא התקבל התקן בזמן הדגימה, הדוח מציין זאת במפורש.

לפי Android API, getRoutedDevice() תקף בזמן ש-AudioTrack מנגן או AudioRecord מקליט, ומחזיר null כאשר אין זרם פעיל או כאשר אין התקן מנותב מדווח. לכן הקוד דוגם בזמן פעילות הזרם ולא משתמש ב-getPreferredDevice() כהוכחה לניתוב בפועל.

**גבול ההוכחה:** התוצאה מוכיחה רק את הנתיב של זרם הבדיקה ששייך לאפליקציה. היא אינה מוכיחה את הנתיב של שיחת מערכת, את פעולת HFP Client מול הטלפון, או גישור דו-כיווני בין HFP Client ל-HFP Audio Gateway. נדרשת עדיין בדיקה פיזית ב-M393.

מקור API: https://developer.android.com/reference/android/media/AudioRouting#getRoutedDevice()


## עדכון 2026-11-16 — גרסה 1.1: מעקב נתיב שמע והבהרת תפקידי HFP

גרסה 1.1 מוסיפה `AudioRouting.OnRoutingChangedListener` גם לזרם `AudioTrack` וגם לזרם `AudioRecord`. בזמן השמעת צליל הבדיקה או הקלטת המיקרופון, שינויי נתיב נרשמים לדוח; נשמרת גם דגימת `getRoutedDevice()` בזמן פעילות הזרם. כך ניתן לראות אם Android ניתב את זרם האפליקציה מחדש במהלך הבדיקה. הדבר אינו מספק גישה לזרם השיחה של מערכת הטלפון.

הדוח וקוד האירועים הובהרו: `BluetoothHeadset` ו-`BluetoothHeadset.ACTION_AUDIO_STATE_CHANGED` מתייחסים לשירות Headset המקומי שבו הנגן הוא Audio Gateway מול אוזניות Hands-Free. אין להסיק מהם שהנגן התחבר לטלפון כ-HFP Client. כדי להוכיח את תפקיד HFP Client נדרשת תוצאת חיבור מהצד הזה, שאינה זמינה דרך API ציבורי רגיל של האפליקציה.

לפני בחירת התקן תקשורת, האפליקציה שומרת את מצב `AudioManager` הקודם. בעת ניקוי הנתיב או סגירת הפעילות היא מנסה לשחזר אותו, אך רק אם המצב הנוכחי עדיין `MODE_IN_COMMUNICATION`; אם מצב Android השתנה, היא לא דורסת אותו. זה מצמצם הפרעה אפשרית ליישום תקשורת אחר, אך עדיין מומלץ לא לבצע בדיקת ניתוב בזמן שיחה חשובה פעילה.

### ממצא AOSP נוסף: הפעלת שני תפקידי HFP

ב-AOSP קיימים מתגי תצורה נפרדים לתפקיד HFP Audio Gateway ולתפקיד HFP Hands-Free Client. לפי `BluetoothProperties.sysprop`, ערכי התפקידים נקבעים על ידי vendor overlay ונקראים בעת אתחול Bluetooth. לכן נוכחות Android 13 או Bluetooth Classic לבדם אינן מבטיחות שתפקיד HF Client מופעל ב-M393. קיום שני מתגים נפרדים גם אינו מוכיח ששני קישורי HFP או שני ערוצי SCO יכולים לפעול במקביל באותו ROM.

בתיעוד `BluetoothHeadset` מצוין שה-API המקומי תומך בחיבור אוזניית Bluetooth אחת בכל פעם. זה מתאים לתרחיש של זוג אוזניות אחד מול הנגן, אך אינו אומר דבר על חיבור HFP Client נפרד לטלפון. יש לאמת את שני התפקידים ואת ה-coexistence בפועל.

Android 12 ומעלה כולל יכולות Combined Audio Routing, אולם תיעוד AOSP מתאר בחירת כמה התקנים לאסטרטגיית שמע באמצעות system APIs עבור אפליקציות privileged, עם דיווח יכולות מצד Audio HAL של היצרן. זו אינה הרשאה ציבורית לאפליקציה רגילה, והיא אינה כשלעצמה גשר בין שיחת HFP Client לבין HFP AG.

מקורות ממוקדים:
- Bluetooth properties של AOSP: https://android.googlesource.com/platform/system/libsysprop/+/refs/heads/android13-release/srcs/android/sysprop/BluetoothProperties.sysprop
- `BluetoothHeadset` API והגבלת חיבור אוזנייה אחת: https://developer.android.com/reference/android/bluetooth/BluetoothHeadset
- Combined audio device routing ב-AOSP: https://source.android.com/docs/core/audio/combined-audio-routing
- `AudioRouting` ו-`getRoutedDevice()`: https://developer.android.com/reference/android/media/AudioRouting
- `AudioManager.setCommunicationDevice()`: https://developer.android.com/reference/android/media/AudioManager#setCommunicationDevice(android.media.AudioDeviceInfo)

## סטטוס גרסה

גרסה 1.1 היא אבטיפוס אבחוני (`versionCode 11`, `versionName 1.1-diagnostic`). היא עדיין אינה מממשת חיוג/מענה, HFP Client, או גישור שמע. בדיקות היחידה מאמתות לוגיקה טהורה ותוויות בלבד; רק בדיקת חומרה עם דוח מה-M393 תוכל לקבוע אם פרופילי Bluetooth והניתוב פועלים במכשיר.


## עדכון 2026-11-17 — איסוף דוח מערכת חיצוני ב-ADB

נוסף כלי קריאה בלבד: `tools/collect_m393_bt_diagnostics.ps1`.

הכלי אוסף מהמכשיר המחובר:
- דגם וגרסת Android/API.
- שורות נבחרות מ-`dumpsys package com.android.bluetooth`, כולל אזכורים של `HeadsetClientService` ומצב רכיבים כאשר Android חושף אותם.
- שורות נבחרות מ-`dumpsys bluetooth_manager` ומ-`dumpsys audio`.
- שירותי מערכת ששמם כולל Bluetooth או audio.

הכלי אינו משנה הגדרות, אינו מתקין APK, אינו מבקש root ואינו מקליט שמע. כתובות MAC מוסתרות אוטומטית בדוח; שמות התקנים ופרטי build עשויים עדיין להופיע, ולכן יש לעיין בדוח לפני שיתוף. נדרשים מחשב עם Android Platform Tools, חיבור USB ואישור USB debugging על הנגן.

הפעלה מתוך תיקיית המאגר ב-PowerShell:
```powershell
.\\tools\\collect_m393_bt_diagnostics.ps1
```

אם מחוברים כמה מכשירים, ניתן לציין את מזהה ה-ADB:
```powershell
.\\tools\\collect_m393_bt_diagnostics.ps1 -Serial "DEVICE_SERIAL"
```

הדוח נוצר כקובץ TXT בתיקייה הנוכחית. הכלי הוא איסוף ראיות מערכת בלבד: גם אם `HeadsetClientService` מופיע ב-dumpsys, הדבר אינו מוכיח שהמתג הפנימי של HFP HF פעיל, שהפרופיל התחיל, או שניתן להחזיק שני קישורי HFP במקביל.

### דיוק נוסף מקריאת תצורת AOSP Android 13

בקובץ `android/app/res/values/config.xml` בענף Android 13 נמצאו ערכי ברירת מחדל:
- `headset_client_initial_audio_route_allowed=true`.
- `hfp_client_connection_service_enabled=false`.
- `config_bluetooth_max_connected_audio_devices=5`.

יש לפרש אותם בזהירות:
- ערך route מותר בתחילת HFP Client הוא הגדרת מדיניות, לא הוכחה שהפרופיל HFP HF מופעל ב-ROM של M393.
- `hfp_client_connection_service_enabled` שולט בשילוב ConnectionService עם Telecom; הוא נפרד מהפעלת פרופיל HFP Client.
- תקרת חמישה התקני שמע היא מגבלת תצורה כללית של ה-stack, ואינה מוכיחה ששני תפקידי HFP או שני ערוצי SCO יכולים לפעול בו-זמנית.

היצרן יכול לשנות ערכי resource באמצעות overlay. לכן דוח ADB מהמכשיר עצמו ובדיקת חיבור בפועל עדיין נחוצים.

מקורות:
- תצורת Bluetooth ב-AOSP Android 13: https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/android13-release/android/app/res/values/config.xml
- תיעוד BluetoothHeadset (כולל הערת Android על חיבור אוזנייה אחת בפרופיל המקומי): https://developer.android.com/reference/android/bluetooth/BluetoothHeadset
- תיעוד registerReceiver והמשמעות של broadcastPermission: https://developer.android.com/reference/android/content/Context#registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter,java.lang.String,android.os.Handler)
- תיעוד ניתוב התקן תקשורת: https://developer.android.com/reference/android/media/AudioManager#setCommunicationDevice(android.media.AudioDeviceInfo)

**סטטוס:** השינוי בריצה זו הוא כלי איסוף ADB ומסמך בדיקה. לא שונה קוד האפליקציה, ולכן לא נבנה APK חדש בריצה זו. לא בוצעה בדיקת חומרה כי אין חיבור ADB פעיל לנגן מתוך סביבת העבודה.
