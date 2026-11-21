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
- אזכור HeadsetClientService אינו מוכיח שהשירות עלה או שהפרופיל פעיל. רמז הסיכום החיובי דורש שורת רכיב עם `enabled=true` וללא שורת `enabled=false/disabled`; גם זה נשאר רמז בלבד ולא אישור פעילות.
- אזכור HEADSET_CLIENT בדוח הוא טקסט מתוך dumpsys, לא בדיקת חיבור.
- SCO input/output במדיניות השמע מעיד על רמזים של תצורת מערכת בלבד, לא על זרם SCO פעיל.
- היעדר ממצא בדוח המסונן אינו מוכיח שאין תמיכה: שורת מערכת עשויה לא להיכלל בסינון.
- אפילו כל הממצאים החיוביים יחד אינם מוכיחים שני חיבורי HFP במקביל או גישור שיחה.

## סיווג HFP Client מעודכן
המנתח מסווג ראיות לתפקיד HFP Client רק מתוך מקטע **Bluetooth manager** ורק כאשר שורת הפרופיל כוללת ערך הפעלה/תמיכה מפורש:
- ערך `true/enabled` בלבד מוצג כרמז חיובי (טקסט dumpsys בלבד; לא הוכחת חיבור). שורה `enabled=false` או `supported=false` לא תסווג בטעות כחיובית; היא תיחשב שלילית בלבד.
- ערך `false/disabled` מוצג כרמז שלילי.
- שם פרופיל ללא ערך הפעלה מפורש נשאר לא מכריע.
- אם נמצאים גם ערכים חיוביים וגם שליליים, הסיכום מציג ראיות סותרות ולא נותן ציון חיובי.
- שורת `HeadsetClientService enabled=true` במקטע Package details מתארת רכיב/שירות, **ואינה נחשבת ראיה לכך שתפקיד HFP Client מופעל**.

ההפרדה בין Package details ל-Bluetooth manager מונעת false positive שבו רכיב שירות קיים/מסומן enabled אך פרופיל HFP Client אינו מאומת.

## בטיחות ופרטיות
הסקריפט קורא קובץ מקומי בלבד. הוא אינו מפעיל ADB, אינו מתחבר לנגן, אינו משנה הגדרות, אינו מקליט שמע, ואינו כולל בקוד פקודות shell. בדוק את הדוח המקורי לפני שיתוף; הוא עשוי לכלול שמות התקנים או פרטי build.

## בדיקות
`tools/test_analyze_m393_bt_report.ps1` מריץ דוחות מדומים: חיובי, שלילי, אזכור ללא ערך, ערכים סותרים, וגם מקרה regression שבו Package details מציג `HeadsetClientService enabled=true` אך Bluetooth manager אינו מציג ראיה לפרופיל HFP Client. הבדיקות אינן משתמשות בנגן אמיתי.
