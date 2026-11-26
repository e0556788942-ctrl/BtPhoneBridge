# PhoneLink M393 — מטריצת אימות חומרה HFP

עודכן: 2026-11-26

מסמך זה מגדיר בדיקה חוזרת ומדורגת ל-M393, לטלפון ולזוג אוזניות Bluetooth. הוא מפריד בין **תצורת מערכת**, **שירות פרופיל פעיל**, **חיבור בפועל** ו-**העברת שמע בשיחה**. אין לסמן שלב PASS על סמך שם שירות, UUID בודד או הצלחת קומפילציה.

## 1. תפקידי Bluetooth הנדרשים

| התקן | תפקיד HFP הנדרש | UUID SDP צפוי | משמעות |
|---|---|---|---|
| M393 מול הטלפון | Hands-Free (HF) / HFP Client | ה-M393 צריך להציג/לפעול כ-HF | מאפשר לטלפון להיות Audio Gateway ולזהות את הנגן כדיבורית |
| טלפון כשר | Audio Gateway (AG) | 0x111F | צד הטלפון שמנהל את השיחה |
| M393 מול האוזניות | Audio Gateway (AG) | ה-M393 צריך להציג/לפעול כ-AG | מאפשר לנגן להתחבר לאוזניות כדיבורית |
| אוזניות Bluetooth | Hands-Free (HF) | 0x111E | צד האוזניות שמספק רמקול ומיקרופון |

HSP עשוי לספק מסלול בסיסי חלופי:
- HSP Audio Gateway: 0x1112.
- HSP Headset - HS: 0x1131.
- 0x1108 הוא Headset service class כללי ואינו מספיק לבדו לזיהוי תפקיד HS.
- A2DP Sink 0x110B הוא פרופיל מדיה, לא תחליף ל-HFP בשיחות.

UUID הוא ראיית SDP על שירות שההתקן מפרסם. הוא אינו מוכיח שהשירות התחבר, שהשיחה נתמכת או שהפרופיל פועל במקביל לפרופיל אחר.

## 2. שלב א׳ — בדיקת תצורת M393 דרך ADB

דרישות: מחשב, Android Platform Tools, כבל USB, USB debugging מופעל ואישור חיבור מהמכשיר.

מתיקיית המאגר ב-PowerShell:

```powershell
.\\tools\\collect_m393_bt_diagnostics.ps1
```

לאחר מכן נתח את הדוח:

```powershell
.\\tools\\analyze_m393_bt_report.ps1 -ReportPath .\\PhoneLink_M393_ADB_YYYYMMDD_HHMMSS.txt
```

יש להחליף את שם הקובץ בשם הדוח שנוצר.

הערכים החשובים:

| מאפיין | תפקיד מקומי ב-M393 | דרישה ראשונית |
|---|---|---|
| bluetooth.profile.hfp.hf.enabled | HFP Hands-Free / Client | TRUE רצוי |
| bluetooth.profile.hfp.ag.enabled | HFP Audio Gateway | TRUE רצוי |

פירוש:
- TRUE הוא ראיה לתצורת מערכת בלבד.
- FALSE הוא סימן לכך שהתפקיד כבוי בתצורה שנחשפה.
- UNKNOWN פירושו שהמאפיין ריק או לא נחשף; אין להמיר זאת ל-FALSE.
- CONFLICT מחייב בדיקה חוזרת.

גם HF=TRUE וגם AG=TRUE אינם מוכיחים ששני הפרופילים נטענו, ששני קישורי HFP יכולים להיות מחוברים בו-זמנית או ששני ערוצי SCO יכולים לפעול יחד.

## 3. שלב ב׳ — בדיקת הטלפון הכשר בנפרד

1. ודא שהטלפון תומך ב-Bluetooth Classic וב-HFP Audio Gateway.
2. בצע שיוך עם M393 דרך הגדרות Bluetooth של שני המכשירים.
3. בדוח PhoneLink בדוק את תוצאת SDP של הטלפון.
4. חפש 0x111F (HFP AG); 0x1112 עשוי להעיד על HSP AG בלבד.
5. תעד בנפרד אם ממשק הטלפון מציג את M393 כהתקן שיחות/דיבורית מחובר.

אם הטלפון מפרסם רק A2DP, אין בכך תמיכה בשיחות HFP. אם לא התקבלה תוצאת SDP, המצב UNKNOWN ולא FAIL.

## 4. שלב ג׳ — בדיקת האוזניות בנפרד

1. נתק זמנית את הטלפון.
2. שייך את האוזניות ל-M393.
3. חפש 0x111E (HFP HF) או 0x1131 (HSP Headset - HS).
4. ודא שהאוזניות מופיעות בפרופיל HFP Headset/AG של M393, ולא רק ב-A2DP.
5. בדוק שהאירוע HFP SCO עובר ל-CONNECTED.
6. הרץ בנפרד את בדיקת צליל היציאה ובדיקת המיקרופון של PhoneLink.

A2DP בלבד מתאים למדיה, ואינו מוכיח שקיים מיקרופון Bluetooth לשיחה.

## 5. שלב ד׳ — בדיקת שני החיבורים במקביל

בצע שלב זה רק אחרי שכל חיבור נבדק בנפרד.

1. חבר את הטלפון ל-M393 כ-HFP Client/HF.
2. חבר את האוזניות ל-M393 כ-HFP Audio Gateway/AG.
3. בדוק ששני החיבורים נשארים CONNECTED בו-זמנית.
4. המתן לפחות דקה ובדוק שאין ניתוק אוטומטי של אחד הפרופילים.
5. כבה והפעל Bluetooth, וחזור על הבדיקה.
6. אתחל את M393 וחזור על הבדיקה.

**מגבלת אבטיפוס:** API ציבורי רגיל של Android מאפשר ל-PhoneLink לבדוק את פרופיל HEADSET המקומי (M393 כ-AG מול אוזניות), אך אינו מספק proxy ציבורי מקביל ל-HFP Client. לכן את חיבור HF מול הטלפון צריך לאמת דרך ממשק הטלפון, לוגי מערכת מורשים או כלי יצרן. אין להסיק אותו מאירועי BluetoothHeadset של הצד השני.

## 6. שלב ה׳ — בדיקת שמע בשיחה

בדיקות AudioTrack/AudioRecord של PhoneLink מוכיחות רק את נתיב זרמי הבדיקה של האפליקציה. הן אינן מוכיחות את נתיב שיחת המערכת.

כדי לאשר שיחה מלאה צריך להוכיח בנפרד:
- שמע מהטלפון מגיע לאוזניות.
- קול מהמיקרופון באוזניות מגיע לצד השני דרך הטלפון.
- שני הכיוונים נשארים פעילים לאורך השיחה.
- ניתוק/חיבור מחדש אינו משאיר SCO או מצב AudioManager תקוע.

יש לבצע בדיקת שיחה רגילה ומתואמת עם איש קשר שמסכים לבדיקה; אין להשתמש במספרי חירום לצורך בדיקה. אין צורך להקליט את השיחה. תעד רק תוצאה בינארית/תיאור תקלה, לא תוכן שיחה או מספרי טלפון.

## 7. תנאי PASS / FAIL / UNKNOWN

| בדיקה | PASS רק אם | FAIL אם | UNKNOWN אם |
|---|---|---|---|
| M393 HFP HF | property TRUE וגם חיבור HF אמיתי לטלפון | property FALSE או החיבור נדחה עקבית | property לא נחשף ואין בדיקת יצרן |
| M393 HFP AG | property TRUE וגם אוזניות מתחברות ב-HFP | property FALSE או HFP לא מתחבר | אין SDP/לוג מספיק |
| טלפון HFP AG | SDP 0x111F או אימות יצרן + חיבור | אין HFP AG מאומת | SDP לא הושלם |
| אוזניות HFP HF | SDP 0x111E/0x1131 וחיבור HFP | אין פרופיל שיחות מתאים | SDP לא הושלם |
| Dual HFP | שני חיבורי התפקידים נשארים פעילים יחד | אחד מתנתק/נחסם באופן עקבי | לא נבדק יחד |
| גישור שיחה | שמע ומיקרופון דו-כיווניים בשיחה אמיתית | אחד הכיוונים לא עובד | לא בוצעה שיחה |

אין לחשב PASS כולל אם אחד השלבים UNKNOWN.

## 8. דרישות מערכת אפשריות אם האבטיפוס נעצר

אם HFP HF כבוי או שה-HFP Client API אינו נגיש, ייתכן שיידרשו:
- ROM/קושחה של היצרן שמפעילים HFP HF ו-HFP AG.
- שירות HFP Client משולב במערכת ו-ConnectionService/Telecom מתאים.
- אפליקציית מערכת חתומה והרשאות privileged, לפי מימוש היצרן.
- תמיכה של בקר Bluetooth ב-ACL ובערוצי SCO/eSCO הנדרשים במקביל.
- Audio HAL ומדיניות שמע שמסוגלים לנתב את קול השיחה בין קישור HFP Client לבין קישור HFP AG.
- בדיקות תאימות של היצרן לניתוק, חיבור מחדש, codec ושיחה דו-כיוונית.

Combined Audio Routing או AudioManager.setCommunicationDevice אינם יוצרים לבדם גשר בין שני חיבורי HFP. בחירת התקן תקשורת של אפליקציה אינה גישה לזרם שיחת הטלפון של המערכת.

## 9. תבנית לרישום תוצאות

העתק ומלא רק את התוצאות; אין לכלול כתובות MAC, מספרי טלפון או תוכן שיחה.

```text
M393 model/build:
Android/API:
HFP HF property: TRUE / FALSE / UNKNOWN / CONFLICT
HFP AG property: TRUE / FALSE / UNKNOWN / CONFLICT
Phone SDP 111F: YES / NO / UNKNOWN
Phone pairing/call profile: PASS / FAIL / UNKNOWN
Earbuds SDP 111E or 1131: YES / NO / UNKNOWN
Earbuds HFP connection: PASS / FAIL / UNKNOWN
M393 HF + AG simultaneously: PASS / FAIL / UNKNOWN
Call downlink to earbuds: PASS / FAIL / UNKNOWN
Earbuds microphone uplink: PASS / FAIL / UNKNOWN
Notes (no personal data):
```

## מקורות

- Bluetooth SIG Assigned Numbers: https://www.bluetooth.com/specifications/assigned-numbers/
- Android Bluetooth permissions: https://developer.android.com/develop/connectivity/bluetooth/bt-permissions
- Android BluetoothHeadset API: https://developer.android.com/reference/android/bluetooth/BluetoothHeadset
- AOSP Android 13 BluetoothProperties: https://android.googlesource.com/platform/system/libsysprop/+/refs/heads/android13-release/srcs/android/sysprop/BluetoothProperties.sysprop
- AOSP Android 13 HeadsetClientService: https://android.googlesource.com/platform/packages/modules/Bluetooth/+/refs/heads/android13-release/android/app/src/com/android/bluetooth/hfpclient/HeadsetClientService.java
- Android AudioManager communication routing: https://developer.android.com/reference/android/media/AudioManager
- AOSP combined audio routing: https://source.android.com/docs/core/audio/combined-audio-routing
