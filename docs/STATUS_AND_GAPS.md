# CoDev CRM v2 — İcra vəziyyəti və buraxılışa hazırlıq
**Tarix:** 2026-11-16  
**İşçi budaq:** `feat/education-crm-v2-20261116`  
**PR:** https://github.com/nihil01/codev_crm/pull/1 (Draft)  
**Əsas tələblər:** [CRM_TZ_AZ.md](CRM_TZ_AZ.md)

> Bu sənəd funksiyaların mənbə koduna əlavə edildiyini bildirir, onların production mühitində işlədiyini təsdiqləmir. Spring Boot, React və Flyway üçün tam build/integration sınaqları bu sessiyada icra edilməyib; GitHub commit statusları boşdur.

## Mənbə koduna əlavə edilən modullar
- ADMIN/MANAGER rolları, admin hesabından menecer yaratmaq/deaktiv etmək, rol əsaslı maliyyə və audit API məhdudiyyətləri.
- CSRF cookie/token protokolu; `GET /api/auth/csrf`, Axios XSRF header. Ətraflı brauzer və reverse-proxy sınağı tələb olunur.
- Lead mənbələri, mərhələlər, konsultasiya tarixi, itirmə səbəbi, admin tərəfindən menecer təyinatı və lead → tələbə keçidi.
- Tələbələrdə valideyn, ünvan, mənbə, qeyd, status, ayrılma səbəbi, könüllü və unikal FİN.
- Kurs kataloqu; qrup cədvəli, əyani/onlayn format, otaq, maksimum yer, Toplanır/Başlayıb/Dayandırılıb/Tamamlanıb statusları.
- Bir tələbənin bir neçə qrupu və fərdi qrup üzrə aylıq qiyməti.
- Ayrıca `payment_transactions` mədaxil jurnalı; qismən/hissəli ödənişlər, qalıq borc, 5 günlük güzəşt müddəti, admin endirimi, aylıq dondurma, çap edilə bilən PDF qəbzləri.
- Müəllim payının faktiki toplanan pula görə hesablanması.
- Xərclərin reklam kanalı; daimi xərclərin yaradılması və ayrı-ayrı ayların məbləğinin dəyişdirilməsi.
- Hesabatlar: aylıq maliyyə, qruplar, borclular, lead mənbələri, menecer performansı, kanal üzrə tələbə əldə etmə dəyəri, Excel ixracı.
- 3 və ya 6 aylıq pessimist/baza/optimist ssenarilərdə gəlir, xərc, tələbə sayı və mənfəət təxmini.
- Sertifikat yalnız tamamlanmış qrupa mənsub, həmin qrup üzrə borcsuz tələbəyə verilir.
- Administrator üçün HTTP dəyişiklik fəaliyyəti jurnalı (istifadəçi/vaxt/metod/yol/status).
- Java test kodu: qalıq borc, endirim, 5 günlük grace və menecerin maliyyə endpoint-lərinə çıxışının qadağan olunması.

## Açıq işlər və risklər
1. **Təsdiqlənməmiş biznes seçimləri:** müəllim paneli (indi/yoxsa sonra), ilk ödəniş günü, avtomatik xatırlatma. Hazırki işçi variant: müəllim girişi yoxdur, ilk ödəniş qeydiyyat/başlanğıc günüdür (`CRM_FIRST_PAYMENT_DELAY_MONTHS=0`), menecer üçün göndərilməyən hazır xatırlatma siyahısı. Bir aylıq gecikdirilmiş ilk ödəniş üçün `CRM_FIRST_PAYMENT_DELAY_MONTHS=1`; istehsalatda dəyişməzdən əvvəl təsdiq alın.
2. **Audit detallaşdırılması:** bütün müvəffəqiyyətli HTTP dəyişikliklərinin əsas metadatası yazılır, lakin sahə səviyyəsində *əvvəlki/yeni dəyərlər* və auditin dəyişməz saxlanılması hələ tam deyil. Tələbin bu hissəsi bitmiş sayılmır.
3. **Daimi xərc qaydaları:** mövcud `is_recurring` modelini ayrıca `recurring_rule_id` ilə idarə olunan, iki dəfə yaradılmanı DB səviyyəsində bloklayan cədvələ keçirmək lazımdır. Köhnə qeydlərin uyğunluğu test olunmalıdır.
4. **Xatırlatmalar:** hazırda menecer siyahısı və mətn var, avtomatik WhatsApp/SMS göndərilişi yoxdur; aktivləşdirmək üçün rəsmi provider, hesab, icazə və ödənişlər tələb olunur.
5. **Müəllim paneli:** müəllim login-i əlavə edilməyib; bu biznes qərarı açıq qalır. Müəllimlərin admin tərəfindən idarəsi və qrup faizləri mövcuddur.
6. **Proqnoz keyfiyyəti:** 3 ssenari sadə açıqlanmış faiz fərziyyələrinə əsaslanır, statistik etibarlılıq və mövsümi dövrlərlə kalibrasiya edilməyib.
7. **Legacy UI:** bəzi köhnə ekranlarda ingilis dilində etiketlər qala bilər. Tam Azərbaycan dili və əlçatanlıq yoxlaması tələb olunur.
8. **Data və təhlükəsizlik:** tarixdə commit edilmiş bütün parolları/SMPP məlumatları rotasiya etmək; `application-docker.yml` konfiqurasiya və reverse-proxy cookie/same-site davranışını yoxlamaq; login throttling, audit diff və logların saxlanma siyasətini əlavə etmək.
9. **Testlər:** JUnit testləri əlavə olunub, amma burada işə salınmayıb. React build və Flyway V5–V10 miqrasiyaları real test PostgreSQL-də yoxlanılmayıb. GitHub Actions workflow əlavə etmək cəhdi inteqrasiya tərəfindən bloklanıb.

## Buraxılışa qəbul sınaqları
- `cd frontend && npm ci && npm run build`; lint ayrıca yoxlanmalı.
- `cd backend && ./mvnw test && ./mvnw package` — təmiz və köhnə sxemli ayrıca test PostgreSQL üzərində.
- Miqrasiyalar `V5`, `V6`, `V7`, `V8`, `V9`, `V10` yeni və mövcud datada idempotent/uyğun şəkildə icra olunmalıdır (Flyway hər migration-u yalnız bir dəfə işlətməlidir).
- Manager: `/api/dashboard`, `/api/expenses/**`, `/api/reports/**`, `/api/forecast/**`, `/api/audit/**`, müəllim payı və aggregate collection API-ləri **403** qaytarmalıdır. Tələbə/lead/ödəniş idarəsi işləməlidir.
- 300 AZN üzrə 100 + 80 AZN qəbulundan sonra 120 AZN borc; 5 tam gün vaxtında `Gecikib` olmamalı, 6-cı gündən gecikmə; endirim yalnız admin və səbəblə.
- Dondurulmuş ayda həm mövcud, həm sonradan yaranan qrup hesabı tələb olunmamalıdır.
- Dayandırılmış qrup aktiv tələbə və gəlir hesablamasına düşməməlidir.
- Excel ixracında hesabat vərəqləri açılmalı, mənfəət hesabında xərclər iki dəfə sayılmamalıdır.
- Tamamlanmamış və ya borclu qrup/tələbə sertifikat ala bilməməlidir.
- CSRF token olmadan POST/PUT/DELETE 403; token ilə auth və redaktə işləməlidir.
- Faydalı səhvlər Azərbaycan dilində göstərilməlidir.
- Mövcud production DB üzrə backup/restore məşqi edilmədən PR merge və deployment olunmamalıdır.

## Konfiqurasiya
`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `CRM_BOOTSTRAP_ADMIN_EMAIL`, `CRM_BOOTSTRAP_ADMIN_PASSWORD`, `CRM_FIRST_PAYMENT_DELAY_MONTHS=0|1`, `CRM_CORS_ALLOWED_ORIGINS` yalnız mühitdə saxlanılmalıdır. Köhnə saxlanmış admin hesabı bootstrap parametrindən avtomatik yenilənmir; parolu ayrıca dəyişin.
