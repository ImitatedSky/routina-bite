# Design: add-entry-photos

## Context

v0.3.1 的紀錄是純文字：名稱、份數、四個營養數字、一句備註。使用者回饋的第 4 點是「附一張照片」，
`improve-entry-editing` 的 design 把它列為 Open Question，要先回答四件事：

| 問題 | 這一波的答案 | 決策 |
| --- | --- | --- |
| 圖片存哪裡 | `filesDir/photos/<uuid>.jpg`，model 只記檔名 | D30 |
| 來源是相機還是選擇器 | 只做系統相片選擇器，零權限；相機留到之後 | D31 |
| 進不進備份檔 | 進，但只在真的有照片時把備份包成 ZIP | D35 |
| 刪紀錄時圖片怎麼辦 | 一起刪；換照片、取消表單也各有出口 | D34 |

沿用既有決策：JSON 檔儲存與「欄位只增不刪」（add-bite-app D1）、紀錄存快照（D3）、
份數是唯一真值（D5）、備份依 id 合併永不刪除（D7）、單一 Repository + StateFlow（D9）、
視覺與刪除規範（D10）；add-bite-charts 的 D5（中文不用字重）與 D6（間距）；
improve-entry-editing 的 D21（一張表單一個 `EntryDraft`）。
下面只寫這次新增的決策，編號接在 D29 之後。

## Goals / Non-Goals

**Goals:**
- 從相簿挑一張圖到存檔，不跳出任何權限對話框
- 一張照片落地大約 150–400 KB，一年三百張也才一百多 MB 之內，不用另外做清理策略
- 舊 `diary.json` 與舊備份不轉檔就讀得進來；備份仍然是「一個檔還原整個 App」
- 沒有照片的使用者，備份流程與 v0.3.1 一模一樣（檔名、副檔名、格式都不變）
- 照片檔掉了（換機、只帶 JSON 備份）不會崩也不會出現破圖

**Non-Goals:**
- 拍照、一筆多張、縮放手勢、裁切、存進系統相簿（見 proposal 的 Non-goals）
- 不引入圖片載入函式庫（Coil／Glide）。要的功能只有「解一張本機 JPEG 到指定尺寸」，
  平台的 `BitmapFactory` 就是做這件事的
- 不做啟動時的孤兒檔掃描
- 不把照片放進 `BiteBackup` 的 JSON（base64）

## Decisions

### D30. 照片存 App 私有目錄，model 只記檔名

```
filesDir/photos/<uuid>.jpg
DiaryEntry.photo: String = ""     // 檔名，不是路徑；空字串 = 沒有照片
```

存完整路徑會在重裝或換機之後失效（`/data/user/0/com.routina.bite/files/...` 不保證不變），
而備份還原的目的正是「換一台機器」。存檔名，路徑一律由 `filesDir` 當場組出來。

副檔名固定 `.jpg`：存進去的一律是我們自己壓出來的 JPEG（D32），來源是 PNG、HEIC 還是 WebP 都一樣。
檔名用 UUID，所以**同名就是同一張**——匯入時同名覆蓋不會蓋錯圖（D36 用得到這個性質）。

放 `filesDir` 而不是 `cacheDir`：系統會在空間不足時清掉 cache，照片是使用者資料不能被清掉。
`android:allowBackup="false"` 已經設好，照片不會被系統雲端備份偷偷帶走。

考慮過存進系統相簿（`MediaStore`）讓使用者在相簿裡也看得到。否決：那需要權限，
而且使用者刪相簿的圖，App 這邊就變成破圖——照片是紀錄的一部分，該由 App 自己負責。

### D31. 來源只有 `PickVisualMedia`，零權限；相機留到之後

```kotlin
rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> ... }
launcher.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
```

`PickVisualMedia` 不需要任何權限：選擇器是系統的行程，回來的是一個單檔 URI 的臨時授權。
**manifest 照樣是零 `uses-permission`**，安裝時使用者不會看到任何權限請求——這是 Bite 的賣點之一。
Android 13 以下沒有系統照片選擇器時，androidx 會自動退回 `ACTION_OPEN_DOCUMENT`，同樣不需要權限
（BlueStacks 是 Android 9，實測走的就是這條）。

臨時授權只在回呼這一趟有效，所以**拿到 URI 就立刻複製一份進 `filesDir`**，不保存 URI。
保存 URI 會在重開機或使用者刪掉原圖之後失效，而且那等於把使用者資料放在我們管不到的地方。

相機（`TakePicture`）明確留到之後：它要 `FileProvider`、要先建一個暫存檔再讓相機寫進去、
還要處理「使用者按了取消」的回收。跟這一波的儲存與備份決策正交，之後加上去時
`photo` 欄位、備份格式、生命週期規則一個都不用改。

### D32. 存之前先縮圖：長邊 1600px、JPEG 85，兩段式解碼

手機相機一張 4000x3000 的 JPEG 是 3–5 MB，直接存會讓備份 ZIP 大得沒辦法用 email 或雲端硬碟搬。
縮到長邊 1600px、quality 85 之後大約 150–400 KB，在 1080x1920 的螢幕上全螢幕看仍然清楚
（螢幕短邊才 1080px）。

兩段式是為了記憶體：

```kotlin
// 1) 先只讀尺寸
val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
// 2) inSampleSize 粗縮（2 的次方），一張 4000px 的圖以 1/2 解進來就只有原來 1/4 的記憶體
options.inSampleSize = sampleSize(bounds, MAX_EDGE)
// 3) createScaledBitmap 精縮到剛好 1600
// 4) compress(JPEG, 85)
```

只用 `inSampleSize` 會停在 2 的次方（4000 → 2000 或 1000），長邊不會剛好是 1600；
只用 `createScaledBitmap` 則要先把整張 4000x3000（48 MB）讀進記憶體。兩段合起來才兩者都不吃虧。

**比上限小的圖不放大**：`inSampleSize = 1` 且跳過精縮，直接壓一次。

**EXIF 方向要套用**：相簿裡的照片常常是「感光元件橫著拍、靠 EXIF 轉正」。
用平台內建的 `android.media.ExifInterface`（API 24 起可吃 `InputStream`，minSdk 26 沒問題，
不是 androidx 那個要加相依的版本）讀 `TAG_ORIENTATION`，90／180／270 用 `Matrix` 轉正。
不轉的話使用者會看到躺著的照片，那是看得見的缺陷不是取捨。

### D33. 顯示也自己解，快取是一個 `LruCache`

```kotlin
sealed interface PhotoState { Loading; Missing; Ready(image) }

@Composable fun rememberPhoto(name: String, maxEdge: Dp, cache: Boolean = true): PhotoState
```

依目標尺寸算 `inSampleSize` 再解（40dp 的列縮圖在 xxhdpi 上只要 120px，
解一張 120px 的 bitmap 是 57 KB，解整張 1600px 是 10 MB）。解碼一律 `withContext(Dispatchers.IO)`，
解完才回到組合階段丟給 `Image(bitmap.asImageBitmap())`。

快取是一個 `LruCache<String, ImageBitmap>`，上限 6 MB，key 是 `檔名@目標像素`
（同一張圖在列表與表單是兩種尺寸，各快取一份）。列表會同時出現好幾張，
沒有快取的話每次捲動回來都重解一次。**全螢幕大圖不進快取**（`cache = false`）：
一張 1080px 的 bitmap 就 3.5 MB，放進去會把整個列表的縮圖擠出快取，而它只看一次。

`LruCache` 是平台內建（`android.util.LruCache`），不必為了快取加相依，也不必自己寫淘汰。

**三種狀態分開**是為了 D37 的「照片不在了要優雅」：
`Loading` 在列表上畫一個中性的圓角底色佔位（不然圖一張張解完會讓整列跳動），
`Missing` 什麼都不畫、連空間都不佔——那一筆看起來就跟從來沒有照片一樣。

### D34. 檔案生命週期：三個出口，沒有孤兒檔

照片檔不在 JSON 裡，所以沒有「刪一筆就自動不見」這回事，三條路都要自己收：

| 情況 | 做什麼 |
| --- | --- |
| 刪除紀錄 | `deleteEntry` 先刪照片檔再刪紀錄 |
| 表單換一張 | 存檔時刪掉 `initial.photo`（與最終那張不同時） |
| 表單按取消 | 刪掉「這次在表單裡新寫進去的每一張」 |

關鍵是**新選的照片在按下加入／儲存之前就已經寫進 `filesDir/photos/`**——因為 URI 的授權只在回呼期間有效，
不能等到存檔才複製。所以表單要自己記帳：

```kotlin
val added = remember { mutableStateListOf<String>() }   // 這次表單新寫進去的檔名
// 確認：added 裡不是最終那張的都刪掉；initial.photo 不是最終那張也刪掉
// 取消：added 全刪（initial.photo 留著，那是既有紀錄的照片）
```

考慮過「先寫到暫存檔、確認時才搬進 photos/」。否決：多一層暫存目錄與搬移的失敗處理，
換來的只是「取消時不用刪檔」——而刪檔本來就只有一行。

考慮過「啟動時掃 photos/ 比對 diary.json，刪掉沒人用的」。否決：三個出口已經涵蓋正常路徑，
剩下的只有「寫完檔當下行程被殺」這種殘檔，一張 300 KB，為它每次啟動多一次目錄掃描不划算。
真的累積了，使用者清除 App 資料或重新匯入備份就沒了。

**`copyYesterday` 複製紀錄時不複製照片**（`photo = ""`）。兩筆共用同一個檔名的話，
刪掉其中一筆就會把另一筆的圖刪掉；而且昨天那碗飯的照片本來就不是今天這碗。

### D35. 備份：有照片才是 ZIP，沒照片維持單一 JSON

```
bite-backup-20260919.zip
├── backup.json          ← 與 v0.3.1 的格式一模一樣（schemaVersion 仍是 1）
└── photos/
    ├── 9f2c….jpg
    └── …
```

照片不能塞進 JSON：base64 會膨脹 33%，而且 kotlinx.serialization 要把整包字串一次組在記憶體裡。
ZIP 是每個 entry 串流寫出，記憶體佔用與照片張數無關。用 `java.util.zip`（平台內建），不加相依。

**一張照片都沒有就維持現在的單一 JSON**（`bite-backup-yyyyMMdd.json`）。
使用者可能根本不用照片，沒有理由把他熟悉的匯出流程換成一個要解壓縮才看得到內容的檔。
這也讓「舊 App 讀新備份」在沒有照片時繼續成立。

`schemaVersion` 維持 1：`backup.json` 的內容格式沒變（只是 `DiaryEntry` 多一個有預設值的欄位），
v0.3.1 讀得進 ZIP 裡那份 `backup.json`（只是它不認得 `photo`，也讀不到 ZIP 外殼）。
升版號會讓舊版對著一份其實讀得懂的 JSON 說「請先更新 App」，那是說謊。

`CreateDocument` 的 MIME 在建構時就固定，所以**準備兩個 launcher**（`application/json` 與 `application/zip`），
按下匯出時依「目前有沒有任何一筆帶照片」挑一個。MIME 要對，不然檔案管理器與雲端硬碟會把 zip 當成 json 開。

### D36. 匯入看內容前兩個 byte，不看副檔名

選擇器回來的是 `content://` URI，副檔名不可靠（有的檔案管理器回傳的 URI 根本沒有檔名，
有的把 `.zip` 標成 `application/octet-stream`）。ZIP 的檔頭永遠是 `PK`（0x50 0x4B），
JSON 備份的第一個非空白字元是 `{`，兩者不會混淆：

```kotlin
val input = BufferedInputStream(raw)
input.mark(2); val zip = input.read() == 'P'.code && input.read() == 'K'.code; input.reset()
```

吃到 ZIP：邊掃 entry 邊做事——`photos/*` 直接寫進 `filesDir/photos/`（同名覆蓋，
檔名是 UUID 所以同名就是同一張，覆蓋等於沒變），`backup.json` 讀成字串，
掃完再走既有的 `BackupCodec.parse` 與 `applyBackup` 合併邏輯（D7 沒有任何改變）。
ZIP 裡沒有 `backup.json` 就當作讀不懂（`UNREADABLE`）。

解 entry 名稱時只取最後一段檔名（`substringAfterLast('/')`）並擋掉空字串——
zip slip（`../../` 跳出目錄）在自己匯出的檔案上不會發生，但匯入的檔案是外來的，
一行防呆比事後解釋便宜。

匯入的 `OpenDocument` 型別放寬成 `arrayOf("*/*")`：原本是 `arrayOf("application/json", "*/*")`，
既然已經有 `*/*` 就沒必要再列，而 zip 也得放行。

### D37. 照片檔不存在＝這一筆沒有照片

匯入一份只有 JSON 的備份（例如使用者手上舊的 `bite-import.json`，或是他只帶走了 JSON），
紀錄裡卻記著 `photo = "9f2c….jpg"`——檔案不在。

處理方式是**把它當作沒有照片**：`Missing` 狀態不畫任何東西、不佔空間（D33），
表單那一列退回成「加照片」按鈕。不清掉 `photo` 欄位：使用者之後補匯入帶照片的 ZIP，
那些照片就自己回來了；現在把欄位洗掉等於提前斷了還原的機會。

### D38. 版面：列表 40dp、表單 64dp，看大圖是全螢幕 Dialog

- **今日頁紀錄列**：有照片時最左邊一張 40dp、圓角 6dp 的縮圖，右邊 12dp 才接名稱那一欄。
  **沒照片的列一個像素都不變**（不留空位）——184 筆舊紀錄都沒有照片，
  為了少數幾筆讓每一列都縮排是本末倒置，與 D26（沒備註就不佔那一行）同一個道理。
- **表單**：照片列放在備註上面。沒照片是一顆「加照片」`OutlinedButton`；
  有照片是 64dp、圓角 8dp 的縮圖 ＋ 一行灰字提示 ＋ 一顆 ✕ 移除鈕。
  **點縮圖＝換一張，長按＝看大圖**。提示文字把這兩件事講出來，不然長按沒人找得到。
- **看大圖**：`Dialog(usePlatformDefaultWidth = false)`，黑底填滿、`ContentScale.Fit` 置中，
  點任何地方關掉。不做縮放手勢——1600px 的圖在手機上已經看得清楚，
  加手勢要處理邊界與雙擊，是另一個題目。

表單的欄位從 8～9 個變成 9～10 個。D27 已經把 `text` 區塊包了 `verticalScroll`，
照片列（64dp 一列）加進去仍然捲得到；1080x1920 上實測確認過（見 tasks 7.9）。
間距照既有規範：照片列自己是一組，與備註之間 16dp，組內（縮圖／提示／移除鈕）8dp。

### D39. 表單的照片操作要 ViewModel，所以 `EntryFormDialog` 多收一個 `viewModel`

選圖之後要在 IO 執行緒把圖複製＋縮圖進 `filesDir`，換照片與取消要刪檔——
這些都不是「編輯 draft」這件事，但它們必須發生在表單的生命週期裡（D34）。

`EntryFormDialog` 因此多一個 `viewModel: BiteViewModel` 參數。
考慮過傳三個 lambda（`onImportPhoto` / `onDeletePhoto` / `photoFile`）讓表單維持不認識 VM，
否決：兩個呼叫端本來就都拿著 VM，三個 lambda 只是把同一件事拆成三段再組回去，
而這個 App 的 VM 本來就是 Repository 的薄封裝（D9），不是什麼需要隔離的東西。

`photo` 進 `EntryDraft`（`photo: String = ""`），所以存檔路徑（`addEntry`／`updateEntry`）
一行都不用改——它們本來就是「draft 的欄位照抄進 `DiaryEntry`」。

## Risks / Trade-offs

- [`PickVisualMedia` 在沒有系統選擇器的舊機上退回 `ACTION_OPEN_DOCUMENT`，介面長得不一樣] →
  仍然是系統選擇器、仍然零權限、回來的一樣是單檔 URI。使用者看到的是檔案瀏覽器而不是相簿格狀圖，
  功能不缺。BlueStacks（Android 9）走的就是這條，已驗證。
- [1600px／quality 85 是寫死的，之後想調就是改常數] → 調了只影響「之後存進去的照片」，
  既有的檔不會動也不需要動。做成設定項目等於要使用者回答一個他不想回答的問題。
- [備份 ZIP 會隨照片變大，三百張就是 100 MB 上下] → 這是附照片的必然成本；
  沒有照片的使用者完全不受影響（仍然是幾十 KB 的 JSON）。真的大到搬不動，
  使用者可以只留近期的照片——刪紀錄就會刪照片。
- [匯入 ZIP 會把照片寫進 `filesDir`，即使合併之後那些紀錄被更新成沒有照片] →
  這種殘檔要靠使用者清資料才會消失。發生條件（匯入一份比現況舊的 ZIP）罕見，
  而為它做引用計數掃描違反 D34 的取捨。
- [`DiaryEntry` 多一個欄位，舊版 App 讀新檔會把 `photo` 洗掉] → 與 D13（分類）、D26（備註）
  同樣的取捨：回滾前先匯出備份。檔案本身不會被舊版刪掉，重裝新版再匯入 ZIP 就回來了。
- [解圖在 IO 執行緒，列表快速捲動時縮圖會晚一點才出現] → `Loading` 狀態畫的是中性佔位塊，
  版面不會跳；第二次捲回來直接命中快取。
- [沒有孤兒檔掃描] → 見 D34 的論證。最壞情況是幾張 300 KB 的殘檔。

## Migration Plan

沒有資料遷移步驟：`photo` 有預設值，舊 `diary.json` 直接可讀，第一次寫入時補上空字串；
`filesDir/photos/` 在第一次存照片時才建立。

1. 在 main 上 commit（不 push、不 tag）。
2. 使用者驗收後決定何時打 `v0.4.0`；CI 會比對 tag 與 `app/build.gradle.kts` 的 versionName，
   所以 versionName 這一波先改成 `0.4.0`（versionCode 6）。

回滾：裝回 v0.3.1。`photo` 對舊版是未知欄位會被忽略，但舊版寫回 `diary.json` 時會把它洗掉，
而且舊版的匯出只有 JSON——**回滾前先用新版匯出一份 ZIP**，照片才留得住。

## Open Questions

- **拍照**（使用者說的「之後再新增」）。加上去時要決定的只剩「暫存檔放哪」與「相機取消怎麼回收」，
  照片的儲存、顯示、備份、生命週期這一波都已經定好，屆時 `photo` 欄位與備份格式不用動。
- 一筆多張照片。如果使用者之後說「一餐有好幾道菜」，`photo: String` 要變成 `photos: List<String>`
  （欄位只增不刪，所以會是新欄位而不是改型別）。現在不預留——預留一個空的 list 欄位
  不會讓那天的工作變少。
