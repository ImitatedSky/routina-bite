# Routina Bite

熱量飲食紀錄。Routina 家族的成員之一，獨立安裝、**零權限**。

記一餐不用十秒：常吃與最近的食物一點就進，或直接「複製昨天」。食物庫可以依來源分類收合，
Subway 的三十幾種料都收在同一組裡。

## 功能

- **今日**：已吃／目標／剩餘，加上蛋白質、脂肪、碳水三條進度；依早餐、午餐、晚餐、點心分段，
  餐別依當下時間自動帶入。份數或公克擇一輸入，改一邊另一邊跟著算。
- **快速輸入**：只填熱量就能記一筆，不必先建食物。
- **食物庫**：台灣營養標示十項欄位，依分類分組、每組可收合，分組可整批改名。
- **趨勢**：熱量（長條＋目標線＋七日平均）、體重（雙軸帶體脂率，家用體重計與 InBody 分開標記）、
  營養素（三張小圖）。區間 7／30／90 天或全部。
- **體重紀錄**：公斤、體脂率、來源。
- **備份**：整包匯出／匯入 JSON，依 id 合併，重複匯入不會長出重複資料。

資料全部存在 App 私有目錄，沒有後端、沒有帳號、不連網。

## 家族

Bite 會出現在 [Routina](https://github.com/ImitatedSky/routina)（Hub）的家族目錄裡，
並對外開放一個能力 `open_today`（把今日紀錄帶到前景），家族成員可以直接呼叫：

```sh
adb shell am start -a com.routina.family.action.RUN_CAPABILITY \
  -n com.routina.bite/.CapabilityActivity \
  --es com.routina.family.extra.CAPABILITY_ID open_today
```

契約只是字串層級的約定（manifest meta-data + Intent extras），不是程式碼依賴，
所以這個 repo 不引用 Hub 的任何 library，自己留一份
[`family/Family.kt`](app/src/main/java/com/routina/bite/family/Family.kt) 即可。

## 建置

需要 JDK 17 與 Android SDK（`local.properties` 的 `sdk.dir`）。

```sh
./gradlew :app:assembleDebug
```

Release 在有完整簽章資訊時（根目錄 `keystore.properties` 或 CI 的 `KEYSTORE_*` 環境變數）
用正式簽章，否則沿用 debug 簽章。家族全體共用同一把 keystore，成員才能就地更新。

## 發版

`vX.Y.Z` 標籤會產出一個永久保留的 Release，附 `routina-bite-vX.Y.Z.apk`。
CI 會先擋下標籤與 `versionName` 不一致的發版。
