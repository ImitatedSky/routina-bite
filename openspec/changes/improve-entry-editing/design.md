# Design: improve-entry-editing

## Context

v0.2.0 在使用者機器上跑了一陣子（78 種食物、184 筆紀錄、31 天）。問題不在功能缺口，而在
「記一筆」這件事被切成三個各自殘缺的對話框（動機見 proposal.md - Why）：

| 入口 | 現況元件 | 有什麼欄位 |
| --- | --- | --- |
| 新增頁點一個食物 | `AmountDialog` | 份數、公克、一行文字預覽 |
| 新增頁按「快速輸入」 | `QuickAddDialog` | 名稱、熱量、蛋白質、脂肪、碳水 |
| 今日頁點一筆紀錄 | `EntryEditDialog` | 份數、餐別 |

三者的聯集正好是使用者要的那一張表單，交集卻幾乎是空的。這一波把三個合成一個。

沿用 add-bite-app 的既有決策：JSON 檔儲存（D1）、欄位只增不刪（D1）、紀錄存營養快照（D3）、
餐別可為 null（D4）、**份數是唯一真值、公克只是輸入方式（D5）**、單一 Repository + StateFlow（D9）、
視覺與刪除規範（D10）；以及 add-bite-charts 的 D5（中文不用字重）與 D6（間距）。
下面只寫這次新增的決策，編號接在 D20 之後。

## Goals / Non-Goals

**Goals:**
- 三個入口一張表單，欄位完全一致；食物與既有紀錄都只是「把表單預先填好」
- 表單上看得到的營養數字就是這一筆實際吃進去的量，而且每一個都改得動
- 連動規則可預期：使用者最後動的那個欄位就是他要的值
- 打開表單就能直接打字，不必先刪掉預設值
- 舊 `diary.json` 與舊備份不轉檔就讀得進來，備份格式不變

**Non-Goals:**
- 表單不編輯「每份幾公克」這個基準（`servingGrams`）。它是食物的屬性，要改去食物編輯頁改
- 不把表單做成獨立畫面（仍是對話框）
- 表單不負責建立／更新食物庫的食物
- 不做欄位層級的錯誤訊息，只用「份數不合法就停用按鈕」

## Decisions

### D21. 一張表單、一個 `EntryDraft`，三個入口各自做一個 draft 丟進去

```kotlin
/** 表單正在編輯的一筆紀錄。[basis] 是「每一份」的營養值，表單上顯示的是 basis × servings */
data class EntryDraft(
    val name: String = "",
    val servings: Double = 1.0,
    val servingGrams: Double? = null,
    val basis: Nutrients = Nutrients.EMPTY,
    val meal: Meal,
    val note: String = "",
    val foodId: String? = null
)

fun draftOf(meal: Meal): EntryDraft                  // 快速輸入：空白
fun draftOf(food: Food, meal: Meal): EntryDraft      // 從食物：名稱、每份公克、營養、foodId 都帶過來
fun draftOf(entry: DiaryEntry): EntryDraft           // 編輯：用這一筆的值

@Composable
fun EntryFormDialog(
    initial: EntryDraft,
    editing: Boolean,
    onConfirm: (EntryDraft) -> Unit,
    onDismiss: () -> Unit
)
```

考慮過的替代方案是 `sealed interface EntryFormSource { Quick / FromFood / Existing }`，
讓表單自己去判斷被誰打開。否決的理由：表單需要的其實只有「初始值」這一件事，
知道自己是被誰打開的只會在表單內部長出三條分支。用一個 draft 當輸入也當輸出，
表單就只剩下「編輯這個 draft」一個職責，三個入口的差別退回到各自的 `draftOf`（都是三行的函式）。

`editing` 是獨立參數而不是 draft 的欄位，因為它只影響標題與按鈕文字（「加入」／「儲存」），
不是被編輯的資料；呼叫端本來就知道自己在做哪一件事。

`servingGrams` 在表單裡是唯讀的基準值（見 Non-Goals）：它決定公克欄出不出現、以及公克↔份數怎麼換算。

### D22. 營養欄顯示總量，內部保存 `basis`（每份值），連動規則只有兩條

使用者要看的是「我吃了多少」，不是「這個食物每份多少」，所以四個營養欄顯示 `basis × 份數`。
但存進 `DiaryEntry` 的是 `perServing`（D3 的快照），所以表單內部必須保留一個每份值 `basis`：

- **改份數或公克** → 四個營養欄重新以 `basis × 份數` 填寫。
- **直接改某一個營養欄** → `basis` 的那一項改成 `輸入值 ÷ 份數`（其餘欄不動）。

兩條規則合起來就是「最後動的那個欄位是真的」。使用者先選 1 份（500 大卡）再改成 2 份會看到 1000；
改成 2 份之後把熱量改成 900，存下去就是 900 大卡（`basis.kcal = 450`），再把份數改成 1 就變 450——
這是 `basis` 語意的必然結果，也是合理的：他說的是「這 2 份共 900」。

考慮過「營養欄顯示每份值」。否決的理由：那正是現在 `AmountDialog` 讓人看不懂的地方——
使用者點 150 g 的雞胸肉，想看的是這 150 g 有幾大卡，不是每 100 g 幾大卡。
也考慮過「不存 basis，存總量，`perServing = 總量 / 份數`」——數學上等價，但每次改份數都得先除再乘，
浮點誤差會累積在使用者看得到的欄位上；保留 basis 只在顯示時乘一次。

**份數不合法（空的或 0）時不動 `basis`**，只改欄位文字：`輸入值 ÷ 0` 沒有意義，寫進去會得到 Infinity。
份數改回合法值時照 D22 的第一條重算（因為「改份數」本來就會重算）。按鈕在份數不合法時是停用的，
使用者無論如何得先把份數修好。

**沒上表單的五項（飽和脂肪、反式脂肪、糖、鈉、膽固醇）原樣留在 `basis` 裡**，存檔時跟著走、
隨份數一起縮放。歸零的話，從食物庫記一筆鈉含量就會憑空消失，而使用者根本沒碰過那個欄位。

**營養欄的 0 顯示成空白**（`basis` 的該項是 0 就填空字串）。理由是空白欄位在這個 App 裡本來就當作 0
（食物編輯頁的 `food_blank_hint` 已經這樣講），而快速輸入的空白表單如果四個欄位都印著「0」「0.0」，
等於又回到「要先刪字才能打」。

### D23. 份數的真值有三種來源，用一個 `AmountEdit` 說清楚

v0.1.0 修過一個 bug：份數欄顯示的是四捨五入過的文字（`formatAmount` 兩位小數），
拿它當真值會把「150 g」存成「150.6 g」。`AmountDialog` 用 `gramsEditedLast` 這個布林解掉了，
但表單多了「編輯既有紀錄」這個入口之後，還有第三種情況——使用者根本沒碰份數與公克：

```kotlin
private enum class AmountEdit { NONE, SERVINGS, GRAMS }

val amount: Double? = when (edited) {
    AmountEdit.NONE -> initial.servings                       // 沒動過就用帶進來的精確值
    AmountEdit.SERVINGS -> servings.toDoubleOrNull()
    AmountEdit.GRAMS -> grams.toDoubleOrNull()?.div(servingGrams)
}
```

`NONE` 這一條是新的：匯入的紀錄有不少是用公克記的（例如 46 g／份的食物記了 150 g，
存進去的份數是 3.260869…），份數欄顯示「3.26」。使用者進表單只改了餐別就存檔，
若拿「3.26」當真值，這一筆的公克數會從 150.0 漂成 149.96——資料在使用者沒動它的情況下被改寫了。

### D24. 名稱用 placeholder，不預填；存檔時才補「快速輸入」

`QuickAddDialog` 把「快速輸入」四個字預填進名稱欄，使用者每次都要先全選刪掉。
改成 `placeholder`（灰字提示，不是內容），預設值一律空字串。
存檔時 `name.trim().ifEmpty { "快速輸入" }`——**預設值的效力保留在結果上，只是不再擋在輸入路徑上**。

名稱欄與備註欄**不**套 D25 的聚焦全選：那兩欄是自由文字，使用者常常是要在既有文字後面接著打，
全選會讓下一個字把整句吃掉。

### D25. `NumberField` 聚焦時全選，對外 API 不變

```kotlin
// 對外仍是 String；內部換成 TextFieldValue 才控制得了選取範圍
var field by remember { mutableStateOf(TextFieldValue(value)) }
var focused by remember { mutableStateOf(false) }
if (field.text != value) field = field.copy(text = value, selection = TextRange(value.length))
LaunchedEffect(focused) {
    if (focused) field = field.copy(selection = TextRange(0, field.text.length))
}
...
modifier = modifier.onFocusChanged { focused = it.isFocused }
```

只改 `NumberField` 一個元件，份數、公克、每日目標、體重、體脂、食物編輯頁的九個營養欄同時修好，
呼叫端一行都不用改。

**全選不能直接寫在 `onFocusChanged` 裡。** 點進欄位的那一下做兩件事：先請求焦點，再把游標放在點到的位置。
在 `onFocusChanged` 當場設的選取範圍會被同一個手勢後面的游標定位蓋掉（實測：欄位是 1，打 2 會變成 12）。
把選取延到 `LaunchedEffect(focused)`，它在這次事件處理完、組合套用之後才跑，選取才設得住。

外部 `value` 變了要同步回內部（份數與公克互相連動時會發生），這段反過來寫在組合階段而不是 `LaunchedEffect`：
`LaunchedEffect` 會晚一個 frame，使用者看得到欄位先閃舊值。組合階段寫入 `MutableState` 會收斂
（下一次組合就相等了），是 Compose 允許的寫法。

過濾非數字字元的作法從「把不合法的字元濾掉」改成「整批拒收」：濾掉字元會讓游標位置對不上
（`TextFieldValue` 帶著 selection，長度變了就要自己重算）。正常打字一次只進一個字元，
兩種作法的可見行為一樣。

### D26. 每筆備註存在 `DiaryEntry.note`，與每日備註並存

```kotlin
@Serializable data class DiaryEntry(
    …, val perServing: Nutrients, val note: String = "", val createdAt: Long
)
```

有預設值，所以 `ignoreUnknownKeys`／缺欄位之下舊 `diary.json` 與舊備份（184 筆）直接讀得進來，
`BiteBackup.schemaVersion` 維持 1（舊版讀到多出來的 `note` 會忽略它）。
與 `add-food-categories` 的 D13 同一套作法，不重複論證。

考慮過「把備註併進 `name`」（例如「滷肉飯（加蛋）」）。否決的理由：名稱要拿來對照食物庫與做常吃統計，
把情境塞進名稱會讓同一個食物長出很多種寫法。

今日頁的紀錄列只在 `note` 非空時多一行（`bodySmall` + `onSurfaceVariant`，接在份數那行下面），
空的時候不佔高度也不顯示佔位文字——184 筆裡有備註的會是少數，為了少數幾筆讓每一列都變高不划算。

### D27. 對話框內容可捲動，份數與公克並排

欄位從 2～5 個變成 8～9 個。1080x1920（≈411dp 寬、≈683dp 高）上 `AlertDialog` 的可用高度
大約 480dp，8 個 `OutlinedTextField`（每個 56dp）加標題、餐別 chip 列與按鈕一定超過，
所以 `text` 區塊包 `verticalScroll`（`QuickAddDialog` 本來就這樣做）。**不砍欄位。**

份數與公克並排成一列（各佔一半）：它們是同一件事的兩種寫法，並排本身就在說「這兩個連動」，
而且省下一整列高度。四個營養欄維持直排——「蛋白質（g）」這種標籤塞進三分之一寬會被截斷。

間距照 add-bite-charts 的 D6：組內 8dp、組間 16dp（1:2）。分組是
〔名稱〕〔份數＋公克〕〔熱量＋蛋白質＋脂肪＋碳水〕〔餐別〕〔備註〕。

### D28. ⋯ 選單加「編輯」，點整列的行為不變

紀錄列的 ⋯ 從只有「刪除」變成「編輯／刪除」，編輯在上面。點整列仍然是開表單——
使用者已經在用這條路徑（只是以前開出來的東西不夠用），拿掉會是退步；
但「只有刪除這一種方法」的印象正是因為 ⋯ 裡面只看得到刪除，所以兩條路都要有。

刪除維持 ⋯ 選單 + `ConfirmDialog`（D10），不做滑動刪除。

### D29. ViewModel 的紀錄 API 收斂成兩個

`addFoodEntry(date, meal, food, servings)`、`addQuickEntry(date, meal, name, nutrients)`、
`updateEntry(entry, servings, meal)` 三個各自只蓋住一部分欄位的方法，換成：

```kotlin
fun addEntry(date: String, draft: EntryDraft)
fun updateEntry(entry: DiaryEntry, draft: EntryDraft)
```

`updateEntry` 用 `entry.copy(...)` 保留 `id`／`date`／`createdAt`，其餘欄位一律以 draft 為準，
包括 `foodId`——**從食物庫來的紀錄即使數字被改過也保留 `foodId`**，因為「常吃／最近」是從
`foodId` 統計出來的（D6），使用者調了一下克數就讓那一餐從常吃消失是錯的。

## Risks / Trade-offs

- [改份數會覆蓋使用者手打的營養數字] → 這是 D22 的規則本身，不是 bug；規則是對稱的
  （改營養欄也會改寫 basis），而且順序是使用者自己控制的：先調份數再打數字就得到他要的。
  不做「鎖定某欄」之類的開關——那要多一個使用者看不見的狀態。
- [`basis` 的五個隱藏欄位隨份數縮放，使用者看不到] → 與 v0.2.0 的行為一致（當時整筆就是
  `perServing × servings`），沒有新的意外；真要改那五項仍然是去食物編輯頁改食物。
- [表單變長，記一筆的步驟感覺變重] → 預填之後絕大多數情況仍然是「點食物 → 按加入」兩步，
  中間多出來的欄位是「看得到」而不是「要填」；按鈕只要求份數合法。
- [`NumberField` 改用 `TextFieldValue`，可能在某些 IME 上選取被收掉] → 影響範圍是「沒有全選到」，
  退回現況而不是壞掉；裝置驗證有一項專門測這件事。
- [`DiaryEntry` 多一個欄位，舊版 App 讀新檔會把 `note` 洗掉] → 與 D13 的分類同樣的取捨，
  回滾前先匯出備份。
- [三個對話框合併，等於一次改掉三條使用者已經熟悉的路徑] → 合併後的表單是三者的聯集，
  原本會做的事一件都沒少；差別只在多看得到幾個欄位。

## Migration Plan

沒有資料遷移步驟：`note` 有預設值，舊 `diary.json` 直接可讀，第一次寫入時補上空字串。

1. 在 main 上 commit（不 push、不 tag）。
2. 使用者驗收後決定何時打 `v0.3.0`；CI 會比對 tag 與 `app/build.gradle.kts` 的 versionName，
   所以 versionName 這一波先改成 `0.3.0`（versionCode 4）。

回滾：裝回 v0.2.0。`note` 對舊版是未知欄位會被忽略，但舊版寫回 `diary.json` 時會把它洗掉，
所以回滾前先匯出備份。

## Open Questions

- **紀錄附圖片**（使用者回饋第 4 點）留到下一版。要先決定的事：圖片存哪裡（App 私有目錄還是相簿）、
  進不進備份檔（進去的話 JSON 會從幾十 KB 變成幾十 MB，`schemaVersion` 要升版）、
  是走相機權限還是只用系統的照片選擇器（目前 manifest 是零權限，`PickVisualMedia` 不需要權限，
  能維持這個狀態的話最好）、以及刪掉紀錄時圖片要不要一起刪。這些都會動到備份格式，
  不適合塞進這一波。
- 表單現在不編輯 `servingGrams`。如果使用者之後回報「這個食物的每份公克標錯了，想在記錄時順手改」，
  再考慮把它變成可編輯欄位（會連帶要決定改了之後要不要回寫食物庫）。
