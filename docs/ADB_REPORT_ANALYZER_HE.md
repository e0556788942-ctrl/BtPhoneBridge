# PhoneLink — ניתוח דוח ADB (לא מקוון)

## למה נוצר הכלי
דוח ADB של PhoneLink כולל שורות מסוננות ממספר שירותי Android. ניתוח ידני עלול לפרש שורה חסרה כהוכחה שאין תמיכה. הסקריפט הזה מסכם רק את המידע שקיים בקובץ ומציג ממצאים בזהירות.

## שימוש
1. חבר את M393 למחשב עם USB debugging מאושר.
2. מתוך תיקיית הפרויקט הרץ:
   `tools/collect_m393_bt_diagnostics.ps1`
3. הרץ על קובץ הדוח:
   `tools/analyze_m393_bt_report.ps1 -ReportPath .\PhoneLink_M393_ADB_YYYYMMDD_HHMMSS.txt`
4. אפשר לשמור סיכום:
   `tools/analyze_m393_bt_report.ps1 -ReportPath .\PhoneLink_M393_ADB_....txt -OutputPath .\PhoneLink_M393_Summary.txt`

## מה הסיכום מציג
- מצב Bluetooth אם מופיע במקטע Bluetooth manager.
- חבילות Bluetooth מועמדות מתוך הדוח.
- אזכור HeadsetClientService במקטעי package dump, כולל enabled=true/false רק אם מופיע באותה שורה.
- אזכור HEADSET_CLIENT/HFP Client במקטע Bluetooth manager.
- ממצאי מדיניות SCO כניסה/יציאה במקטע Audio policy.

## פירוש זהיר
- אזכור HeadsetClientService אינו מוכיח שהשירות עלה או שהפרופיל פעיל.
- אזכור HEADSET_CLIENT בדוח הוא טקסט מתוך dumpsys, לא בדיקת חיבור.
- SCO input/output במדיניות השמע מעיד על רמזים של תצורת מערכת בלבד, לא על זרם SCO פעיל.
- היעדר ממצא בדוח המסונן אינו מוכיח שאין תמיכה: שורת מערכת עשויה לא להיכלל בסינון.
- אפילו כל הממצאים החיוביים יחד אינם מוכיחים שני חיבורי HFP במקביל או גישור שיחה.

## בטיחות ופרטיות
הסקריפט קורא קובץ מקומי בלבד. הוא אינו מפעיל ADB, אינו מתחבר לנגן, אינו משנה הגדרות, אינו מקליט שמע, ואינו כולל בקוד פקודות shell. בדוק את הדוח המקורי לפני שיתוף; הוא עשוי לכלול שמות התקנים או פרטי build.

## בדיקות
`tools/test_analyze_m393_bt_report.ps1` מריץ שני דוחות מדומים (חיובי/שלילי), מאמת את הסיווגים ואת יצירת קובץ סיכום. הבדיקות אינן משתמשות בנגן אמיתי.
