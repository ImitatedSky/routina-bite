# Design: inline-entry-form

## Context

v0.5.0 的新增紀錄頁是一個「挑食物的頁面」：餐別晶片、常吃、最近、搜尋、三顆小按鈕、分類分組的清單。
表單只存在於對話框裡，要先按「快速輸入」或點一個食物才看得到。

使用者的回饋直指資訊架構：**按下「新增」就已經表達要輸入了，表單卻還藏在一次點擊之後**。
他上一輪講的「使用食物庫，也是希望像是新增一樣，只是協助填入快速輸入」是同一件事——
表單是主角，食物庫是填表單的工具。

沿用既有決策：紀錄存快照（add-bite-app D3）、份數是唯一真值（D5）、視覺與刪除規範（D10）；
add-bite-charts 的 D5（中文不用字重）與 D6（間距 4/8/12/16/24，組內 < 組間）；
add-food-categories 的 D18（分組清單是 `LazyListScope` 擴充函式，兩個頁面共用）；
improve-entry-editing 的 D21（一張表單一個 `EntryDraft`）、D22（營養欄顯示總量、內部保存 `basis`）、
D23（`AmountEdit` 三態）、D24（名稱用 placeholder）、D27（表單可捲動、份數與公克並排）；
add-entry-photos 的 D30（只記檔名）、D31（選到就複製進 `filesDir`）、D34（照片生命週期三個出口）、
D39（表單收一個 `viewModel` 做照片）。

下面只寫這次新增的決策，編號接在 D39 之後。

## Goals / Non-Goals

**Goals:**
- 今日頁按 ＋ → 下一個動作就是打字，中間沒有任何一次點擊
- 選食物的點擊數不變（點一個食物 → 按「加入」），而且點完看得到填進去的數字
- 連動規則（份數／公克／四個營養欄、`AmountEdit` 三態、精確份數）行為零變化
- 編輯既有紀錄的體驗完全不變，仍然是對話框
- 表單只有一份實作，不因為內嵌而複製第二份欄位程式碼
- 照片不留孤兒檔——內嵌之後「取消」不存在了，等價的出口要補上

**Non-Goals:**
- 不做「加入後留在原頁繼續記下一筆」（現在加入完回今日頁，與 v0.5.0 相同）
- 不做表單狀態跨頁保存（D46）
- 不碰今日頁、食物庫頁、食物編輯頁的版面
- 不動資料模型與備份格式

## Decisions

### D40. 表單內嵌進新增紀錄頁，「快速輸入」按鈕拿掉

```
新增紀錄
├─ 餐別晶片          ← 與表單同一組，16dp
├─ 紀錄表單（預設空白）
├─ ──────分隔線──────
├─ 新建食物 / 食物庫
├─ 常吃 / 最近 / 搜尋 / 分類分組的食物清單
└─ [bottomBar] 清空                      加入
```

表單放最上面而不是最下面：使用者進來的目的是輸入，第一眼就該看到欄位；
食物清單是輔助，往下捲才出現正好符合「我先試著自己打，打不出來再去找食物」。

**「快速輸入」那顆按鈕直接刪掉**，不是改成別的樣子——空白表單已經在畫面上，那顆按鈕的意思變成「把表單清空」，
而那件事「清空」已經在做了（D43）。留著兩個名字做同一件事只會讓人猶豫。
`add_quick` 這個字串仍然要：它是名稱欄的 placeholder，也是存檔時的預設名稱。

「新建食物」與「食物庫」從 `OutlinedButton` 降成 `TextButton`：它們現在是挑食物那一區的附屬入口，
不該和「加入」爭視覺重量。

考慮過把表單做成可摺疊的區塊（預設展開）。否決：摺疊狀態是第四個要記的東西，
而且使用者抱怨的就是多一個動作——給他一個可以再次把表單收起來的開關，等於把問題請回來。

### D41. 狀態與 UI 拆開：`EntryFormState` + `EntryFormFields`，對話框只剩編輯

一張表單現在要長在兩個地方，所以拆成兩塊，兩邊共用：

```kotlin
@Stable
class EntryFormState(initial: EntryDraft) {
    var source: EntryDraft private set     // 這張表單以哪一筆 draft 為底
    var name; var meal; var note; var photo                      // 直接改
    var servings; grams; kcal; protein; fat; carbs  private set  // 只能走 update*()
    val servingGrams: Double?   // source 帶進來的換算基準，有才給公克欄
    val allowNoMeal: Boolean    // source.meal == null 才給「未分餐」
    val amount: Double?         // AmountEdit 三態
    val valid: Boolean          // amount > 0
    fun updateServings / updateGrams / updateKcal / updateProtein / updateFat / updateCarbs(input: String)
    fun addPhoto(file: String); val newPhotos: List<String>; fun forgetNewPhotos()
    fun loadFrom(draft: EntryDraft)   // 點食物、開表單
    fun reset()                       // 清空：空白表單，只留目前餐別
    fun toDraft(quickName: String): EntryDraft?
}

@Composable fun rememberEntryFormState(initial: EntryDraft): EntryFormState
@Composable fun EntryFormFields(state, viewModel, modifier, showMeal: Boolean = true)
@Composable fun EntryFormDialog(viewModel, initial, onConfirm, onDismiss)   // 只剩編輯
```

**六個數字欄位是 `private set`**：它們之間有連動，外面直接指派就會繞過規則。
`name`／`note`／`meal`／`photo` 沒有連動，維持一般的 `var`，不為了一致性多包六個函式。
（函式名是 `updateServings` 而不是 `setServings`：後者與 `var servings` 產生的 JVM setter 撞名，編不過。）

`showMeal` 是唯一一個「哪裡在用我」的參數：內嵌版的餐別晶片在頁面最上面（D40），
對話框版的餐別在營養欄與照片之間（沿用 v0.5.0 的位置，不動它）。
考慮過把餐別完全移出 `EntryFormFields`、由兩個呼叫端各自畫，否決：那樣對話框得把欄位拆成上下兩段再夾一個餐別，
為了消滅一個布林參數把共用的那塊切開，不划算。

`EntryFormDialog` 的 `editing: Boolean` 參數刪掉：新增已經不走對話框，這個旗標只剩一個值。
標題固定「修改紀錄」、按鈕固定「儲存」，與 v0.5.0 的編輯情境一模一樣。

### D42. 點食物＝`loadFrom` 重填表單並捲回頂端，不開對話框

```kotlin
fun fillFrom(food: Food) {
    state.dropNewPhotos(viewModel)                 // 這次選的照片不屬於這個食物
    state.loadFrom(draftOf(food, state.meal))      // 餐別保留目前選的那個
    scope.launch { listState.animateScrollToItem(0) }
}
```

四個入口（常吃晶片、最近晶片、搜尋結果、分組清單）走同一個函式，行為一致。

**一定要捲回頂端**：使用者點的是畫面中下段的一個食物，填進去的東西在畫面外，
不捲的話看起來像「我點了但沒反應」。`animateScrollToItem` 而不是 `scrollToItem`——
動畫讓人看得出「上面有東西被填了」。

`loadFrom` 整筆換掉（含名稱、份數、公克、四個營養欄、備註、照片、`foodId`、`AmountEdit` 歸 `NONE`），
只有餐別是從現在的表單帶回去。選錯食物再點一個就好，不會留下前一個食物的殘值。

### D43. 「加入」放 bottomBar，旁邊一顆「清空」

`加入` 是這一頁唯一的完成動作，而這一頁會捲很長（78 個食物分十幾組）。放在表單下方的話，
「點食物 → 捲回頂端 → 再捲下去按加入」，白白多一次捲動。放 `bottomBar` 就永遠在。

- `Button`（填色）＝主要動作，左邊的「清空」用 `TextButton`＝次要動作，兩者權重差一級。
- 停用條件與 v0.5.0 的對話框相同：`amount` 不是數字或 ≤ 0。
- `windowInsetsPadding(WindowInsets.ime.union(WindowInsets.navigationBars))`：
  取兩者的聯集（各邊取大的），鍵盤起來時按鈕會待在鍵盤上方，沒有鍵盤時只避開導覽列，不會兩個 padding 相加。

「清空」的必要性：選錯食物時，八個欄位要一格一格刪。一顆按鈕解決，而且它同時是「我想從頭自己打」的入口，
正好接手被刪掉的那顆「快速輸入」（D40）。

### D44. 內嵌情境的照片生命週期：存檔、清空、離開頁面

照片在使用者選圖的當下就寫進 `filesDir/photos/`（D31 的必然結果），所以表單一定要記帳。
v0.5.0 的兩個出口是「確認」與「取消」；內嵌之後「取消」不存在了，等價的是「清空」與「離開這一頁」：

| 情況 | 做什麼 |
| --- | --- |
| 按「加入」／對話框按「儲存」 | `commitPhotos`：留下的那張歸紀錄所有，換掉的與沒用上的刪掉，並停止記帳 |
| 按「清空」 | `dropNewPhotos`：這次新寫的全刪 |
| 點另一個食物（`loadFrom` 會把 `photo` 換掉） | `dropNewPhotos` |
| 返回上一頁（含被導去「新建食物」「食物庫」） | `DisposableEffect` 的 `onDispose` → `dropNewPhotos` |
| 對話框按「取消」 | `dropNewPhotos`（與 v0.5.0 相同） |

```kotlin
DisposableEffect(Unit) { onDispose { state.dropNewPhotos(viewModel) } }
```

**`commitPhotos` 會清掉記帳**，所以「加入」之後緊接著發生的 `onDispose` 不會把剛存進紀錄的那張刪掉——
這是整段最容易寫錯的地方，也是 `forgetNewPhotos()` 存在的理由。

刪檔的動作留在畫面層（`commitPhotos` / `dropNewPhotos` 是 `EntryFormState` 的擴充函式，收 `viewModel`），
狀態本身不碰檔案系統：狀態只知道「這些檔是我這次寫的」，誰去刪由拿得到 `viewModel` 的那一層決定。

### D45. 版面：餐別與表單同一個 item，分隔線自己吃掉間距

`LazyColumn` 的 `verticalArrangement` 是 12dp（沿用），但表單內部的欄位間距是 16dp（沿用對話框）。
如果餐別晶片自己一個 item，它與名稱欄的距離（12dp）會比表單內部（16dp）還近，違反「組內 < 組間」。
所以**餐別晶片與 `EntryFormFields` 放在同一個 item 裡**，用同一個 16dp 的 `Column`——
它們本來就是同一組（餐別是表單的一個欄位，只是位置被提到最上面）。

分隔線 `HorizontalDivider(Modifier.padding(vertical = 12.dp))`：
表單區與挑食物區之間總共 12 + 12 + 12 + 12 = 48dp，是組內 16dp 的三倍，兩塊一眼分得開。

### D46. 表單狀態不做 `rememberSaveable`

`EntryFormState` 是 `remember`，離開新增紀錄頁（返回今日頁、或被導去「新建食物」／「食物庫」再回來）
就是一張新的空白表單。

做成 `rememberSaveable` 要為 `EntryDraft`＋`Nutrients`＋六個欄位文字＋`AmountEdit` 寫一個 `Saver`，
或把整組欄位搬進 ViewModel 並處理「同時有兩個新增頁」的情況。換來的好處只有「打到一半跑去新建食物，回來還在」——
而使用者會去按「新建食物」，正是因為他發現這個食物不在庫裡、表單還沒開始打。

真的變成問題（例如使用者回報「打到一半按了食物庫就沒了」）再處理，屆時做法是 `Saver`，不是把狀態搬去 ViewModel。

## Risks / Trade-offs

- [離開新增紀錄頁表單就重置] → D46 的取捨。附帶效果是選了照片但沒加入就離開，那張檔會被刪掉（D44），
  這反而是對的：畫面上已經沒有任何東西指向它。
- [新增紀錄頁一進來就是一整頁欄位，比 v0.5.0「先看到食物清單」多一點視覺重量] →
  這正是使用者要的（他抱怨的就是看不到輸入欄位）。常吃／最近仍然在第一次捲動就到得了。
- [`EntryFormFields` 被放在 `LazyColumn` 的一個 item 裡，捲出畫面時會被 dispose] →
  欄位的值都在 `EntryFormState`（畫面層 `remember`），捲回來原樣還在；
  只有 `photoFailed` 的提示與「看大圖」的開關會重置，兩者都是當下的瞬時狀態。
  照片選擇器的 launcher 雖然也註冊在這個 item 裡，但點「加照片」時它必然在畫面上，回呼回得來（已實機驗證）。
- [鍵盤蓋住 bottomBar] → 已用 `ime` 與 `navigationBars` 的聯集處理；BlueStacks 用實體鍵盤、沒有軟鍵盤，
  這一段沒能在裝置上實測。
- [「清空」沒有二次確認] → 表單是還沒存下去的暫存內容，誤觸的代價是重新選一次食物；
  加確認對話框反而讓正常路徑多一次點擊，與這一版的目的相反。
- [`storedPhotos()` 只看紀錄引用的檔，備份 zip 驗不出孤兒檔] → 這次改用「設定 → 應用程式資訊 → 儲存空間」
  的「使用者資料」KB 數驗證（選圖前後差 66 KB、清空／返回後回到原值），見 tasks 8。

## Migration Plan

沒有資料遷移：資料模型、檔案格式、備份格式都沒有動，`diary.json` 與既有備份原樣可讀。

1. 在 main 上 commit（不 push、不 tag）。
2. 使用者驗收後決定何時打 `v0.6.0`；CI 會比對 tag 與 `app/build.gradle.kts` 的 versionName，
   所以 versionName 這一波先改成 `0.6.0`（versionCode 8）。

回滾：裝回 v0.5.0，資料完全相容（沒有任何新欄位）。

## Open Questions

- **加入之後要不要留在新增紀錄頁**？現在是回今日頁（與 v0.5.0 相同）。如果使用者回報「一餐要記三樣，
  每次都要重按 ＋」，做法是加入後 `reset()` 並留在原頁、用 Snackbar 回饋「已加入」——
  表單與「清空」都已經就位，那時只要改 `add()` 的最後兩行。
- **表單狀態跨頁保存**（D46）。
