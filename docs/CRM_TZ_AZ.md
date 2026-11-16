# CoDev CRM — təsdiqlənmiş funksional tələblər
**Mənbə:** 16 noyabr 2026 tarixli təsdiqlənmiş qaydalar. Bu sənəd layihənin mərhələli həyata keçirilməsi üçün başlanğıc spesifikasiyasıdır. Aşağıdakı üç seçim hələ təsdiqlənməyib.

## 1. Rollar və giriş
- 1 administrator və bir neçə menecer. Hər istifadəçinin ayrı giriş hesabı və şifrəsi var.
- Menecer bütün tələbələri görür, ödənişləri daxil edir və idarə edir.
- Menecer hesabatları, gəlir, xərc və mənfəəti görmür; API səviyyəsində də giriş qadağandır.
- Administrator bütün funksiyalara və dəyişiklik tarixçəsinə daxil ola bilər.
- Hər dəyişiklik üçün istifadəçi, vaxt, obyekt və dəyişdirilən məlumatlar qeyd olunur. Audit tarixçəsini yalnız administrator görür.

## 2. Leadlər
- Mənbələr: Instagram, WhatsApp, TikTok, zəng, tövsiyə, sayt, birbaşa gəliş.
- Mərhələlər: Yeni → Əlaqə saxlanıldı → Sınaq dərsi / Konsultasiya → Düşünür → Qeydiyyat / İtirildi.
- Administrator lead-i istənilən menecerə təyin edir.
- Sınaq dərsinin/konsultasiyanın tarixi saxlanılır.
- İtirilmiş lead üçün sərbəst mətn şəklində səbəb tələb olunur.

## 3. Kurslar və qruplar
- Kurs siyahıdan seçilir; siyahıda olmayan kurs əl ilə əlavə edilə bilər.
- Qrupun məlumatları: kurs, müəllim(lər), dərs günləri və saat, əyani/onlayn, otaq, maksimum tələbə sayı.
- Minimum tələbə sayı məcburi deyil.
- Statuslar: Toplanır → Başlayıb → Dayandırılıb → Tamamlanıb.
- Dayandırılmış qrup aktiv tələbə sayına və hesabatlara daxil edilmir.

## 4. Tələbələr
- Qeydiyyat zamanı mənbə, valideyn adı və telefonu, ünvan və qeyd saxlanılır.
- FİN könüllüdür, amma təqdim edilən FİN təkrar olmamalıdır.
- Tələbə eyni vaxtda bir neçə qrupa qoşula bilər.
- Statuslar: Aktiv, Dondurulub, Bitirib, Ayrılıb.
- Ayrılma səbəbi məcburidir.
- Dondurulmuş aylar üçün ödəniş tələb olunmur.
- Aylıq ödəniş məbləği tələbə və qrup üzrə fərdi təyin oluna bilər.

## 5. Müəllimlər
- Müəllimin payı toplanmış faktiki pulun faizi ilə hesablanır.
- Bir qrupa bir neçə müəllim və ya mentor təyin edilə bilər.

## 6. Ödənişlər
- Birdəfəlik, hissəli və qismən ödəniş dəstəklənir; qalıq borc kimi saxlanılır.
- Ödəniş günündən 5 gün sonra ödənilməmiş hissə «Gecikib» olur.
- Üsullar: nağd, kart, köçürmə.
- Çap olunan qəbz olmalıdır.
- Endirimi yalnız administrator təyin edə bilər; səbəb məcburidir.

## 7. Xərclər
- Növlər: İşçi maaşı, Yer (icarə), Reklam, Kommunal, Avadanlıq, Digər.
- Reklam xərci kanala görə saxlanılır; bir tələbənin əldə olunma dəyəri hesablana bilir.
- Daimi xərclər hər ay avtomatik yaradılır, sonra dəyişdirilə bilir.

## 8. Hesabatlar və proqnoz
- Aylıq gəlir/xərc/mənfəət; qrupların qazancı; borclular; lead mənbələri; menecer nəticələri.
- Excel ixracı.
- 3–6 aylıq gəlir, tələbə sayı və mənfəət proqnozu: pessimist, baza və optimist ssenarilər.
- Aylıq hədəflər bu mərhələdə tələb olunmur.

## 9. Digər
- Sertifikat yalnız qrup tamamlandıqda və tələbənin borcu olmadıqda verilir.
- Davamiyyət sonrakı mərhələyə saxlanılır.
- İnterfeysin dili Azərbaycan dilidir.

## Qərar gözləyən 3 sual
1. Müəllim paneli: **A** indi giriş yoxdur, sonra əlavə ediləcək; **B** indidən yalnız öz qruplarını və payını görən müəllim hesabı.
2. Ödəniş gününün hesablanması: **A** ilk ödəniş başlanğıc günü, sonrakılar eyni tarixdə; **B** ilk ödəniş 1 ay sonra, sonrakılar eyni tarixdə.
3. WhatsApp/SMS xatırlatması: **A** yoxdur; **B** menecer üçün siyahı və hazır mətn, avtomatik göndəriş yoxdur; **C** avtomatik göndəriş (xarici ödənişli xidmət tələb edir).

**Qeyd:** Üç açıq sual yekunlaşdırılmadan bu davranışlar kodda geri dönməz biznes qaydası kimi təsbit edilməməlidir.