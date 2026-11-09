# PhoneLink — בדיקת היתכנות טכנית ממוקדת

תאריך עדכון: 2026-11-09

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

האבטיפוס הנוכחי:
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
