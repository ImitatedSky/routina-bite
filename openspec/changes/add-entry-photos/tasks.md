# Tasks: add-entry-photos

## 1. 資料模型

- [x] 1.1 `model/Diary.kt`：`DiaryEntry` 加 `val photo: String = ""`（放在 `note` 之後、`createdAt` 之前），註解寫清楚存的是檔名不是路徑（D30）
- [x] 1.2 確認備份不用改版：`BiteBackup` 序列化的是整個 `DiaryEntry`，`schemaVersion` 維持 1（D35）

## 2. 照片檔（`data/Photos.kt`）

- [x] 2.1 `photosDir(context)` 與 `photoFile(context, name)`：檔名為空或檔案不存在回 null（D30、D37）
- [x] 2.2 `importPhotoFile(context, uri)`：`inJustDecodeBounds` 讀尺寸 → `inSampleSize` 粗縮 → `createScaledBitmap` 精縮到長邊 1600 → `compress(JPEG, 85)` 寫成 `<uuid>.jpg`，回傳檔名；失敗回 null（D32）
- [x] 2.3 套用 EXIF 方向（平台的 `android.media.ExifInterface`，90／180／270 用 `Matrix` 轉正）；比上限小的圖不放大（D32）
- [x] 2.4 `decodePhoto(file, maxEdgePx)`：依目標尺寸算 `inSampleSize` 再解，給顯示用（D33）
- [x] 2.5 `BiteRepository.deletePhoto(name)`：三個生命週期出口共用這一個（D34）

## 3. 備份的 ZIP（`data/BackupArchive.kt`）

- [x] 3.1 `writeZip(out, json, photos)`：`backup.json` 一個 entry，每張照片一個 `photos/<檔名>` entry，用 `java.util.zip`（D35）
- [x] 3.2 `readZip(input, photosDir)`：邊掃 entry 邊把 `photos/*` 寫進 `photosDir`（同名覆蓋），回傳 `backup.json` 的內容；沒有 `backup.json` 回 null（D36）
- [x] 3.3 entry 名稱只取最後一段檔名、空字串跳過（擋 zip slip）（D36）
- [x] 3.4 `BackupCodec`：加 `defaultZipFileName()`（`bite-backup-yyyyMMdd.zip`），既有的 JSON 檔名不動（D35）

## 4. ViewModel

- [x] 4.1 `importPhoto(uri)`：在 `Dispatchers.IO` 上呼叫 `importPhotoFile`，回傳檔名（D31）
- [x] 4.2 `deletePhoto(name)` 轉給 repository（D34）
- [x] 4.3 `deleteEntry(id)`：先刪照片檔再刪紀錄（D34）
- [x] 4.4 `copyYesterday`：複製出來的紀錄 `photo = ""`（D34）
- [x] 4.5 `exportBackup(uri, asZip)`：`asZip` 時用 `BackupArchive.writeZip` 帶上實際存在的照片檔，否則維持現在的單一 JSON；全程在 IO（D35）
- [x] 4.6 `importBackup(uri)`：`BufferedInputStream` 的前兩個 byte 判斷 `PK`，是 ZIP 就先解照片再拿 `backup.json` 走既有的 `parse` + `applyBackup`（D36）

## 5. 照片的 Composable（`ui/PhotoViews.kt`）

- [x] 5.1 `PhotoState { Loading, Missing, Ready }` 與 `rememberPhoto(name, maxEdge, cache)`：`LaunchedEffect` + `withContext(IO)` 解圖（D33）
- [x] 5.2 `LruCache<String, ImageBitmap>` 上限 6 MB，key = `檔名@目標像素`；全螢幕大圖不進快取（D33）
- [x] 5.3 `PhotoThumb(name, size, corner, ...)`：`Loading` 畫中性佔位塊、`Missing` 什麼都不畫、`Ready` 畫圖（D33、D37）
- [x] 5.4 `PhotoViewerDialog(name, onDismiss)`：`usePlatformDefaultWidth = false`、黑底填滿、`ContentScale.Fit`，點一下關掉（D38）

## 6. 表單

- [x] 6.1 `EntryDraft` 加 `photo: String = ""`，`draftOf(entry)` 帶進來（D39）
- [x] 6.2 `EntryFormDialog` 多收 `viewModel: BiteViewModel`，兩個呼叫端跟著改（D39）
- [x] 6.3 `PickVisualMedia` launcher（`ImageOnly`），選到就在 IO 上匯入並把檔名記進 `added`（D31、D34）
- [x] 6.4 照片列放在備註上面：沒照片是「加照片」按鈕；有照片是 64dp 圓角 8dp 縮圖 ＋ 提示 ＋ ✕ 移除鈕；點縮圖換一張、長按看大圖（D38）
- [x] 6.5 確認時：`added` 裡不是最終那張的都刪掉，`initial.photo` 與最終不同也刪掉；取消時 `added` 全刪（D34）
- [x] 6.6 存檔把 `photo` 帶進 draft（`addEntry`／`updateEntry` 本來就照抄 draft 的欄位，不用改）（D39）

## 7. 畫面與字串

- [x] 7.1 `ui/TodayScreen.kt`：紀錄列在有照片時最左邊加 40dp 圓角 6dp 縮圖 + 12dp 間距；沒照片的列版面不變（D38）
- [x] 7.2 `ui/SettingsScreen.kt`：兩個 `CreateDocument` launcher（`application/json`／`application/zip`），依有沒有照片挑一個（D35）
- [x] 7.3 `ui/SettingsScreen.kt`：匯入的 `OpenDocument` 型別放寬成 `arrayOf("*/*")`（D36）
- [x] 7.4 `res/values/strings.xml`：照片相關字串與更新後的備份說明文字，附加在檔尾，全部繁中不硬編
- [x] 7.5 `app/build.gradle.kts`：versionCode 5 → 6、versionName `0.3.1` → `0.4.0`
- [x] 7.6 確認 `AndroidManifest.xml` 仍然零 `uses-permission`（D31）

## 8. 驗證

- [x] 8.1 `./gradlew :app:assembleDebug` 與 `:app:assembleRelease` 綠燈
- [x] 8.2 裝置：表單「加照片」→ 系統選擇器跳出（不要權限對話框）→ 選一張 → 表單出現縮圖
- [x] 8.3 裝置：存檔後今日頁那一列左邊有小縮圖，沒照片的列版面沒變
- [x] 8.4 裝置：長按表單縮圖看得到大圖，點一下關掉
- [x] 8.5 裝置：換一張照片後舊檔不見了（用匯出的 zip 裡有幾張間接確認）
- [x] 8.6 裝置：刪除紀錄後照片檔也不見了（同樣用匯出確認）；表單取消不留孤兒檔
- [x] 8.7 裝置：有照片時匯出成 zip，拉回電腦 `unzip -l` 看得到 `backup.json` 與 `photos/*.jpg`
- [x] 8.8 裝置：`pm clear` 後把那個 zip 匯回去，照片回來了
- [x] 8.9 裝置：匯入舊的純 JSON（`bite-import.json`）仍然正常、不崩，記著檔名但沒有檔案的那一筆不顯示破圖
- [x] 8.10 裝置：1080x1920 上表單放得下或捲得到，每個欄位都碰得到
- [x] 8.11 裝置：全程 `logcat -b crash` 乾淨
- [x] 8.12 測完把使用者資料還原好（重新匯入 `bite-import.json`）
- [x] 8.13 commit（英文、無 Co-Authored-By）；不 push、不 tag
