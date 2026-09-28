# Fast Scan PDF - AI Coding Guidelines & Workspace Rules

This repository is **Fast Scan PDF** (`com.superfastscan`), a high-performance Android document scanning, OCR, and PDF utility application built with Kotlin, Jetpack Compose, and Clean Architecture.

---

## 1. Architecture & Layering (Clean Architecture + MVVM)

The project follows Clean Architecture principles separated into clear package boundaries under `com.superfastscan`:

* **`ui/`**: Jetpack Compose screens, reusable UI components, theme, and ViewModels.
* **`domain/`**: Business logic, UseCases (e.g., `ProcessDocumentUseCase`), domain models, and repository interfaces. Pure Kotlin; no Android framework or database dependencies.
* **`data/`**: Room entities (`PageEntity`, `DocumentEntity`), DAOs, repository implementations, DataStore preferences, and Hilt dependency injection modules (`data/di/`).
* **`navigation/`**: Jetpack Navigation Compose navigation graphs and type-safe destinations.
* **`ads/`**: AdMob ad management (banners, interstitials, rewarded ads).

---

## 2. Jetpack Compose & UI Best Practices

* **Material 3**: Exclusively use Material 3 components (`androidx.compose.material3.*`).
* **Unidirectional Data Flow (UDF)**:
  * ViewModels expose immutable `StateFlow<UiState>`.
  * Composables receive state and emit user events as lambdas (`onAction: (UiEvent) -> Unit`).
  * Avoid exposing mutable state (`MutableStateFlow` must remain `private`).
* **Localization & Strings**:
  * **Never hardcode user-facing strings** in Composables or ViewModels.
  * Always reference Android string resources via `stringResource(id = R.string.xxx)` or pass string resource IDs.
  * Ensure string entries exist in both default `res/values/strings.xml` and Turkish `res/values-tr/strings.xml`.
* **Stability & Recomposition**:
  * Keep Composables small, focused, and previewable (`@Preview`).
  * Use `remember` and `derivedStateOf` to prevent unnecessary recompositions on frequent state changes.
  * Use Coil Compose (`AsyncImage`) for loading document page previews and thumbnails.

---

## 3. Asynchronous Programming & Coroutines

* **Dispatchers**:
  * **`Dispatchers.Main`**: UI updates and StateFlow emissions.
  * **`Dispatchers.IO`**: All disk I/O, Room database queries, PDF operations (rendering, merging, compression), and ML Kit OCR processing.
  * **Never block the main thread** with PDF generation or image manipulation.
* **Lifecycle**:
  * Launch coroutines in ViewModels using `viewModelScope`.
  * In Composables, use `LaunchedEffect` with appropriate keys or `rememberCoroutineScope()` strictly for user-triggered animations/actions.

---

## 4. Dependency Injection (Hilt)

* All ViewModels must be annotated with `@HiltViewModel` and use `@Inject constructor(...)`.
* Inject UseCases or Repository interfaces, never concrete Room DAOs or database instances directly into ViewModels or UI layers.
* New dependencies must be provided or bound via Hilt modules in `data/di/`.

---

## 5. Document Scanning & PDF Engine Guidelines

* **ML Kit Document Scanner & Text Recognition**:
  * Handle edge detection, scanning results, and OCR asynchronously.
  * Ensure proper error handling and fallback when text recognition returns empty or fails.
* **PDFBox Android (`com.tom-roush:pdfbox-android`)**:
  * Always ensure `PDFBoxResourceLoader.init(context)` has run before calling PDFBox APIs.
  * Always use `use` blocks or explicit `try-finally { document.close() }` to prevent native memory leaks when working with `PDDocument`.
  * Respect non-destructive PDF manipulation, page rotation metadata (`/Rotate`), and page reordering.

---

## 6. Code Style & Conventions

* **Language**: Modern Kotlin (v2.0+), leveraging Kotlin Serialization for JSON.
* **State Modeling**: Use sealed interfaces/classes for UI States (`UiState.Loading`, `UiState.Success`, `UiState.Error`) and UI Events.
* **Code Integrity**: Preserve existing comments, docstrings, and error handling patterns. Keep edits modular and clean.

---

## 7. Fast Scan & Export UX Principles (Hız Odaklı Kullanıcı Deneyimi)

* **Speed-First Single Screen Flow (Tek Ekranda Birleşik Akış)**:
  * Tarama sonrası düzenleme (`Edit`) ve kaydetme (`Save/Export`) akışları iki ayrı ekrana bölünmemeli, tek bir ekranda (`EditDocumentScreen`) birleştirilmelidir.
  * Kullanıcı sayfaları tek ekranda inceleyip filtre/döndürme uyguladıktan sonra doğrudan tek tıkla ("Belgeyi Kaydet") işlemi tamamlayabilmelidir.
* **Zero-Friction Saving (Çekim Esnasında Tercih Sormama)**:
  * Her tarama kaydında dosya formatı veya kayıt konumu sorulmamalıdır.
  * İlk açılışta `InitialSetupDialog` ile alınan varsayılan format (`defaultExportFormat` - PDF/JPG) ve konum tercihleri `SettingsDataStore` üzerinden otomatik uygulanmalıdır.
  * Bu tercihler daima `SettingsScreen` üzerinden değiştirilebilir olmalıdır.
* **Auto-Open on Save (Kaydedilince Otomatik Açılma)**:
  * Belge dışa aktarımı tamamlandığında `FileProvider` ve `Intent.ACTION_VIEW` kullanılarak oluşturulan dosya (PDF veya JPG) cihazın varsayılan görüntüleyicisinde otomatik olarak açılmalıdır.
* **Static Save Button Label (Kaydet Buton Yazısının Sabitliği)**:
  * Tarama düzenleme ekranında ("Belgeyi Kaydet") butonuna tıklandığında buton metni kesinlikle değişmemeli, daima sabit kalmalıdır.
  * Kaydetme/dışa aktarma işlemi sırasında metin yerine veya yanında dönen dairesel yükleme göstergesi (`CircularProgressIndicator`) yer almalıdır.
* **Edit Screen Title Extension Badge (Başlık Uzantı Rozeti)**:
  * Düzenleme ekranında alt barda format değiştirme butonu yer almamalı, format ayarlara bağlı kalmalıdır.
  * Belge başlığının sağ tarafında ayarlardaki formata göre `.pdf` veya `.jpg` uzantısı belirgin bir rozet (`Surface`) olarak gösterilmelidir.
* **Home Screen Layout Balance (Ana Ekran Denge ve Boşluk Standardı)**:
  * Ana ekranda logo/karşılama bölümü ile "PDF Araçları" grid başlığı arasında aşırı boşluk bırakılmamalıdır (maks. 32-36 dp spacer). Araç kartları ekranın üst-orta bölgesinde dengeli şekilde yer almalıdır.

---

## 8. System Bars & Navigation Standards (Sistem Barları ve Navigasyon Standartları)

* **Permanent System Navigation Bar (Kalıcı Navigasyon Barı)**:
  * Uygulamada navigasyon barını gizleyen geçici (transient immersive) modlar kesinlikle kullanılmamalıdır.
  * `WindowCompat.setDecorFitsSystemWindows(window, true)` ve `windowInsetsController.show(Type.systemBars())` kullanılarak sistem navigasyon barı daima görünür ve uygulamanın altında tutulmalı; butonların veya arayüzün üzerine binmesine asla izin verilmemelidir.

---

## 9. Ad Placement Conventions (Reklam Yerleşim Standartları)

* **Home Screen Banner**: Ana ekrandaki banner reklam, ekranı kaydırınca kaybolmayacak şekilde **Sabit Alt (Bottom Sticky)** katmanında, gezinme barının (`BottomScanBar`) hemen üzerinde konumlandırılmalıdır.
* **Saved Scans Banner**: Kayıtlı taramalarım ekranındaki banner reklam, üst başlık çubuğunun (`TopAppBar`) hemen altında yer almalıdır.
* **PDF Tools Interstitial Ad Policy (PDF Araçları Geçiş Reklamı Standardı)**:
  * 15 PDF aracından (ve OCR) herhangi biri tamamlandığında, kümülatif olarak **her 2 tamamlamada 1 kez** geçiş reklamı (`InterstitialAd`) tetiklenmelidir (`actionCount % 2 == 0`).
  * **İki Farklı Tamamlanma Akışı:**
    1. **Ekran İçi Sonuç Veren Araçlar** (`Compress`, `Rotate`, `Watermark`, format dönüştürücüler, `OCR`): İşlem bittiğinde `LaunchedEffect(uiState.isComplete)` üzerinden `onToolActionComplete` çağrılmalıdır. Bu akışta kullanıcı sonuç ekranından koparılmaz; reklam kapatıldığında kullanıcı dosyasını paylaşabilir veya açabilir.
    2. **Ana Ekrana Dönen Araçlar** (`Merge`, `Split`, `Sign`, `Organize`): İşlem tamamlandığında `onToolCompleteAndPopHome` ile hem sayaç işletilmeli hem de ana ekrana dönülmelidir.
  * **Fiziksel Cihaz Test Güvencesi:** Test ortamında AdMob'un reklam kısıtlamasına takılmamak için fiziksel test cihazı ID'leri `AdManager` içinde `RequestConfiguration` listesine eklenmelidir.

---

## 10. In-App Purchase (IAP) & Premium Subscription Standards (Abonelik & Premium Standartları)

* **Product & Architecture**:
  * Product ID: `monthly_premium_no_ads` (Aylık otomatik yenilenen abonelik).
  * Mimari: Google Play Billing v7 (`BillingManager`) + Yerel Güvenli Önbellek (`SettingsDataStore.isPremiumUser`) + `@Singleton PremiumManager`.
* **Zero-Ad Invariant for Premium (Sıfır Reklam Standardı)**:
  * Premium aktif olduğunda tüm banner reklamlar 0px boyuta inmeli (`AdBanner` tamamen çökmeli, boşluk bırakmamalı).
  * Hem 15 PDF aracı ("2 kullanımda 1 reklam") hem de belge tarama akışı ("5 taramada 1 reklam") geçiş reklamları Premium kullanıcılara kesinlikle gösterilmemelidir.
* **Dynamic Premium Branding & Logo Invariant (Dinamik Logo ve Markalama)**:
  * Standart/ücretsiz sürümde ana ekranda `ic_app_logo.png` (sarı şimşek) gösterilmelidir.
  * Premium aktif olduğunda ana ekranda `ic_app_logo_premium.png` (saydam arka planlı, metalik 3D altın ve gümüş sayfalı şimşek) dinamik olarak gösterilmelidir.
  * `PaywallBottomSheet` ve `SettingsScreen` üzerindeki Premium kartlarında daima bu `ic_app_logo_premium.png` kullanılmalıdır.
* **Developer Debug Mode (Geliştirici Test Modu Güvencesi):**
  * Fiziksel cihazlarda Google Play satın alma test hesabı olmadan da özellikleri doğrulamak için `SettingsScreen` altındaki `Premium Mode (Debug)` aç/kapat toggle anahtarı daima işlevsel tutulmalıdır.

---

## 11. Home Screen Quick Scan Widget Standards (Ana Ekran Hızlı Tarama Widget Standardı)

* **1-Block Compact Footprint (1 Blok / 1x1 Hücre Standardı):**
  * Ana ekran widget'ı telefon başlatıcısında (Launcher) yatay bar veya çoklu hücre kaplamamalı, tam **1 blok (1x1 hücre - 48dp x 48dp)** yer kaplamalıdır (`targetCellWidth="1"`, `targetCellHeight="1"`).
* **Pure Visual Invariant (Yazısız & İkonsuz Saf Görsel Standardı):**
  * Widget tasarımında başlık, alt başlık, kamera/eylem ikonu veya ekstra butonlar yer almamalıdır.
  * Standart/ücretsiz sürümde yalnızca `widget_logo_standard.png` (sarı şimşekli belge tasarımı) gösterilmelidir.
  * Premium sürümde yalnızca `widget_logo_premium.png` (3D altın ve gümüş sayfalı tasarım) gösterilmelidir.
* **Zero-Friction Direct Scan Launch (Tek Dokunuşla Doğrudan Tarama):**
  * Widget'a dokunulduğunda doğrudan `ACTION_START_SCAN` intent'i tetiklenmeli ve araya hiçbir ekran girmeden anında `ML Kit Document Scanner` kamerası açılmalıdır.
  * Tarama tamamlandığında hız odaklı tek ekran kuralı uyarınca `EditDocumentScreen` akışına geçilmelidir.
* **Reactive Dynamic Synchronization (Anlık Senkronizasyon):**
  * Kullanıcının abonelik durumu her değiştiğinde (`BillingManager`, `SettingsDataStore`, satın alma, geri yükleme veya Debug modu aç/kapat), ana ekranda yer alan tüm widget'ların RemoteViews logoları `QuickScanWidgetProvider.updateAllWidgets(context)` ile anında güncellenmelidir.
* **Launcher Pinning Flow (Uygulama İçi Konumlandırma):**
  * `SettingsScreen` ve `HomeScreen` üzerinde kullanıcıya widget'ı ana ekranına tek tıkla ekleme imkanı (`AppWidgetManager.requestPinAppWidget`) sunulmalıdır; pinleme önizlemesinde kullanıcının o anki sürümüne ait görsel yer almalıdır.

---

## 12. Google Play In-App Review Standards (10. Tarama Değerlendirme Standardı)

* **Milestone Trigger (10. Tarama Koşulu):**
  * Kullanıcı toplamda **10 başarılı tarama ve kaydetme** işlemini tamamladığında (`completedScanCount == 10`) ve daha önce anket gösterilmediyse (`!isReviewPrompted`), ana ekranda `GooglePlayReviewDialog` gösterilmelidir.
* **Ad Suppression Invariant (Reklam Çakışma Önleme):**
  * 10. taramada normalde 5'te 1 çalışan geçiş reklamı (`scanCount % 5 == 0`), kullanıcının değerlendirme deneyimini ve Google Play akışını bölmemesi için **kesinlikle bastırılmalı** (`isReviewMilestone = true`), reklam gösterilmemelidir.
* **Review Persistence & Fallback:**
  * Anket bir kez gösterildiğinde durum kalıcı olarak kaydedilmeli (`isReviewPrompted = true`) ve bir daha otomatik açılmamalıdır.
  * Google Play In-App Review API'si sideload/debug ortamlarında veya kota limitinde başarısız olursa, kullanıcı doğrudan Play Store mağaza sayfasına yönlendirilmelidir (`openPlayStoreListing`).
* **Developer Debug & Reset:**
  * `SettingsScreen` altında geliştirici testleri için sayacı sıfırlama (`resetReviewState`) ve 10 tarama beklemeden anketi açma imkanı daima korunmalıdır.

---

## 13. Contact Us & Support UX Standards (Bize Ulaşın & Destek Standartları)

* **Menu Cleanliness & Privacy Invariant (Temiz Menü ve Gizlilik Standardı):**
  * Ayarlar ana menüsünde (`SettingsMainMenu`) ve Hakkında sayfasında (`AboutSubPage`) "Bize Ulaşın" öğesinin altında geliştirici/destek e-posta adresi (`seydialiiclek@gmail.com`) **kesinlikle alt başlık (subtitle) olarak gösterilmemelidir**. Menü öğesi sade bir şekilde yalnızca başlık ve ikon içermelidir.
* **Zero-Friction Direct Contact Flow (Sade İletişim Akışı):**
  * "Bize Ulaşın" alt sayfasında konu seçim chip'leri, destek e-posta adresi kartı veya "24-48 saat" yanıt süresi bilgilendirme metinleri yer almamalıdır.
  * Sayfa yalnızca başlık, açıklama metni ve bu metnin hemen altında yer alan birincil **"E-posta Gönder"** butonundan oluşmalıdır.
* **Pre-filled Technical Details (Hazır E-posta Şablonu):**
  * Butona basıldığında `mailto:seydialiiclek@gmail.com` çağrılmalı; konu varsayılan destek başlığıyla, gövde ise kullanıcının cihaz modeli, Android sürümü ve uygulama versiyonunu içeren hazır şablonla otomatik doldurulmalıdır.
  * Cihazda e-posta istemcisi bulunmaması durumunda adres otomatik panoya kopyalanmalı ve kullanıcı Toast ile bilgilendirilmelidir.

---

## 14. Application Launcher Icon & Branding Standards (Uygulama Başlatıcı Simgesi Standartları)

* **Manifest & Adaptive Icon Invariant (Manifest ve Uyarlanabilir Simge Bütünlüğü):**
  * `AndroidManifest.xml` dosyasında `<application>` etiketinde `android:icon="@mipmap/ic_launcher"` ve `android:roundIcon="@mipmap/ic_launcher_round"` tanımları daima bulunmalıdır; eksik bırakılarak sistem varsayılan robot simgesine düşülmesine izin verilmemelidir.
  * Android 8.0+ (API 26+) uyumluluğu için `res/mipmap-anydpi-v26/` altında `ic_launcher.xml` ve `ic_launcher_round.xml` tanımları daima yer almalı; `@drawable/ic_launcher_background` ve `@mipmap/ic_launcher_foreground` katmanlarına bağlanmalıdır.
* **Exact Symmetrical Centering & Zero Overflow Invariant (Tam Merkezleme ve Sıfır Taşma Kuralı):**
  * Başlatıcı simgesi üretilirken grafik öğeleri asla asimetrik veya kayık yerleştirilmemeli; 432x432 ön plan kanvasında hem X hem Y ekseninde tam simetrik olarak ortalanmalıdır (`pos_x = (432 - w) / 2`, `pos_y = (432 - h) / 2`).
  * Grafik ölçeği, hem Samsung One UI squircle maskesinde hem de Google Pixel dairesel maskesinde metinlerin (yıl, slogan, aksiyon kelimeleri) ve görselin maske dışına taşmasını kesinlikle önleyecek (`cut_pixels = 0`, `min alpha = 255`) güvenli alana göre hesaplanmalıdır. Kenarlardan taşma yapılmasına asla izin verilmez.
* **Complete Density Buckets Coverage (Eksiksiz Çözünürlük Seti):**
  * `mipmap-mdpi`, `mipmap-hdpi`, `mipmap-xhdpi`, `mipmap-xxhdpi` ve `mipmap-xxxhdpi` klasörlerinin tümünde `ic_launcher_foreground.png`, `ic_launcher.png` (squircle) ve `ic_launcher_round.png` (daire) yüksek kaliteli resampling (`LANCZOS`) ile eksiksiz üretilmelidir.
* **Separation from In-App Brand Assets (Uygulama İçi Marka Varlıklarının Korunumu):**
  * Launcher simgesi güncellemeleri sırasında uygulama içi marka varlıkları (`ic_app_logo.png`, `ic_app_logo_premium.png`) ve widget logoları (`widget_logo_*.png`) kesinlikle değiştirilmemeli; başlatıcı simgesi ile uygulama içi logoların ayrımı korunmalıdır.

