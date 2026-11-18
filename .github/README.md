# SmgBackup — نسخ احتياطي واستعادة انتقائية (Kotlin + Jetpack Compose)

تطبيق Android لإنشاء نسخة احتياطية **مشفّرة محليًا** (`.svbackup`) واستعادتها مع اختيار **كل عنصر** تريده على حدة (جهة اتصال، رسالة، مكالمة، ملف…) بواجهة تشبه Norton Ghost.

## بناء APK على GitHub
1. ارفع محتويات المجلد إلى مستودع GitHub (الفرع `main`).
2. تبويب **Actions** ← **SmgBackup - Build Android APK** ← **Run workflow**.
3. بعد النجاح حمّل **SmgBackup-apk** من *Artifacts* (أو من Releases إن وسمت النسخة `v1.2.0`).

الـ workflow يثبّت JDK 17 و Gradle 8.9 و Android SDK 35 ويبني `assembleDebug` مباشرة بدون `gradlew` (لا يوجد `gradle-wrapper.jar` في المشروع). لتوليد wrapper كامل محليًا: `gradle wrapper --gradle-version 8.9`.

## الأمان
- AES-256-GCM + PBKDF2-HMAC-SHA256 (100,000 تكرار)، ملف محمول بكلمة مرورك. **لا استرجاع للملف بدونها.**
- خزنة كلمات المرور المحلية بمفتاح Android Keystore غير قابل للتصدير.
- تحقق بيومتري/قفل الشاشة قبل النسخ والاستعادة، وحماية من اجتياز المسارات عند استخراج الملفات.
- لا اتصال بأي خادم، و`allowBackup=false`.

## قيود أندرويد (مهمة)
- **SMS**: الكتابة تتطلب كون التطبيق تطبيق الرسائل الافتراضي. أُضيفت المكوّنات المطلوبة للأهلية (`SMS_DELIVER`, `WAP_PUSH_DELIVER`, `SENDTO`, `RESPOND_VIA_MESSAGE`) لكن قبول النظام للدور غير مضمون، وبعد الاستعادة أعد تطبيق الرسائل المفضّل لديك.
- سجل المكالمات قد يرفض النظام الكتابة فيه على بعض الأجهزة.
- حسابات وتطبيقات وإعدادات النظام: للاطلاع فقط (لا يسمح أندرويد باستعادتها).
- `MANAGE_EXTERNAL_STORAGE` و`QUERY_ALL_PACKAGES` أذونات مقيّدة في Google Play، راجع `docs/`.
- غير منفّذ بعد: Google Drive، النسخ التزايدي، الجدولة.

## الملفات
`app/…/backup` النسخ والتشفير · `data` المستودعات · `sms` مكوّنات دور SMS · `ui` الشاشات · `.github/workflows/build-apk.yml` البناء · `docs/` التقارير.
