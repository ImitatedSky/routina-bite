# Change: add-entry-photos

## Why

使用者在 v0.2.0 的回饋裡有五點，第 4 點是「每筆紀錄可以附一張照片」。
當時（`improve-entry-editing`）把它推遲了，理由寫在那份 design 的 Open Questions：
要先決定圖片存哪裡、進不進備份檔、走相機還是照片選擇器、刪紀錄時圖片怎麼辦。
這一波把那四個問題一次回答完並實作。

照片解決的是文字解決不了的事：一筆「快速輸入 · 650 大卡」過兩週回頭看，
使用者記不得那到底是哪一家的便當；一張圖就認得出來，也才比得出份量是不是跟上次一樣。

使用者這次的指示很明確：**先只做「從相簿選一張」，拍照留到之後**。

## What Changes

- **`DiaryEntry` 加 `photo: String = ""`**：存的是檔名（`<uuid>.jpg`），不是完整路徑——
  路徑會因為重裝而變，檔名不會。空字串＝沒有照片。`schemaVersion` 維持 1。
- **照片存在 App 私有目錄 `filesDir/photos/`**，不進相簿、不需要任何權限。
- **來源是系統相片選擇器**（`ActivityResultContracts.PickVisualMedia`，`ImageOnly`）。
  **manifest 仍然零 `uses-permission`**——零權限是這個 App 的賣點之一。
- **選到圖之後先縮圖再存**：長邊上限 1600px、JPEG quality 85（一張約 150–400 KB）。
  用平台內建的 `BitmapFactory`（`inSampleSize` 粗縮 + `createScaledBitmap` 精縮）與 `Bitmap.compress`，
  **不加任何相依**（不用 Coil／Glide）。顯示縮圖也自己依目標尺寸解，解完丟進一個 `LruCache`。
- **`EntryForm` 在備註上面加一列照片**：沒照片是一顆「加照片」；
  有照片是 64dp 圓角縮圖＋一顆移除鈕，點縮圖換一張、長按看大圖（全螢幕黑底 `Dialog`，點一下關掉）。
- **今日頁的紀錄列**在有照片時最左邊多一張 40dp 圓角縮圖；沒照片的列版面完全不變。
- **檔案生命週期**：刪紀錄連照片一起刪；表單換照片刪掉舊檔；
  表單取消把這次新寫進去的檔刪掉（不留孤兒檔）。載入時不做孤兒掃描。
- **備份改成「有照片才包成 ZIP」**：任何一筆有照片 → `bite-backup-yyyyMMdd.zip`
  （`backup.json` ＋ `photos/<檔名>.jpg`）；一張照片都沒有 → 維持現在的單一 JSON。
  匯入同一個入口吃 `.json` 與 `.zip`，**依內容前兩個 byte（`PK`）判斷、不看副檔名**。
  ZIP 讀寫用 `java.util.zip`（平台內建）。
- **照片檔不存在時優雅降級**：匯入只有 JSON 的備份、紀錄卻記著檔名 → 那一筆就當作沒有照片，
  不崩、不顯示破圖。
- **版本**：versionCode 5 → 6、versionName `0.3.1` → `0.4.0`。

## Non-goals（留給之後的版本）

- **拍照**。使用者明確說「之後再新增拍照，現在先上圖片就好」。
  相機要處理 `FileProvider`、`TakePicture` 的暫存檔與失敗回收，與這一波的儲存／備份決策正交，
  等使用者真的要了再加（加上去時 `photo` 欄位與備份格式都不用動）。
- 一筆多張照片。一筆一張已經回答了「那是哪一家的便當」，多張要再決定版面與刪除語意。
- 圖片的縮放手勢、裁切、濾鏡、旋轉 UI。
- 把照片存進系統相簿（`MediaStore`）——那會需要權限，違反零權限。
- 啟動時掃描孤兒照片檔。生命週期的三個出口（刪紀錄、換照片、取消表單）已經涵蓋，
  為了理論上的殘檔每次啟動掃一次目錄不划算。
- 食物庫的食物附圖（這一波只有紀錄有照片）。

## Capabilities

### New Capabilities
- `bite-entry-photos`: 每筆紀錄一張照片——來源、縮圖與儲存、顯示與看大圖、檔案生命週期

### Modified Capabilities
- `bite-settings-backup`: 匯出格式與匯入來源改變（MODIFIED：`Export backup` 有照片時改匯出 ZIP、
  `Import merges by id and never deletes` 要吃得下 ZIP 並還原照片）

（`openspec/specs/` 目前仍是空的，`add-bite-app` 尚未 archive，既有需求的正本在
`openspec/changes/add-bite-app/specs/bite-settings-backup/spec.md`；MODIFIED 區塊是從那裡整段複製再改寫。）

## Impact

- 新增 `app/src/main/java/com/routina/bite/data/Photos.kt`（照片目錄、匯入縮圖、解縮圖）
- 新增 `app/src/main/java/com/routina/bite/data/BackupArchive.kt`（ZIP 的寫與讀，`java.util.zip`）
- 新增 `app/src/main/java/com/routina/bite/ui/PhotoViews.kt`（縮圖快取與三個 Composable）
- 修改 `model/Diary.kt`（`DiaryEntry.photo`，有預設值，舊 `diary.json` 與舊備份照樣讀得進來）
- 修改 `data/BiteRepository.kt`（`deletePhoto(name)`）、`data/BackupCodec.kt`（ZIP 的預設檔名）
- 修改 `ui/EntryForm.kt`（照片列與選圖 launcher）、`ui/TodayScreen.kt`（紀錄列縮圖）、
  `ui/BiteViewModel.kt`（匯入照片、刪紀錄連照片、匯出分 JSON／ZIP、匯入吃 ZIP）、
  `ui/SettingsScreen.kt`（兩個匯出 launcher、匯入型別放寬、說明文字）
- 修改 `res/values/strings.xml`（新字串附加在檔尾）、`app/build.gradle.kts`（版本）
- **無新相依、無新權限、`schemaVersion` 不變**
