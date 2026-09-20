# Tasks: add-home-widget

## 1. 小工具本體

- [x] 1.1 `BiteWidgetProvider`：`onUpdate` 從 `BiteApp.repository` 取今天的熱量、喝水與目標
- [x] 1.2 `res/layout/bite_widget.xml`：兩欄（左熱量、右喝水）＋固定 40dp 的按鈕列
- [x] 1.3 `res/xml/bite_widget_info.xml`：4×2、`resizeMode` 橫向與縱向、`previewLayout`
- [x] 1.4 背景／按鈕／兩條進度條共四個 drawable
- [x] 1.5 `values/colors.xml` 與 `values-night/colors.xml` 兩組具名顏色

## 2. 互動

- [x] 2.1 「+250 水」→ `ShortcutActivity`（`EXTRA_ACTION=water`、`EXTRA_ML=250`）
- [x] 2.2 「記一餐」→ `MainActivity` + `EXTRA_OPEN_ADD`
- [x] 2.3 點其他區域 → `MainActivity` + `EXTRA_OPEN_TODAY`
- [x] 2.4 三個 `PendingIntent` 都是 `FLAG_IMMUTABLE`、`requestCode` 互不覆蓋

## 3. 更新

- [x] 3.1 `BiteApp` 觀察 `entries` / `water` / `targets`，變動就重畫（背景執行緒）
- [x] 3.2 manifest 的 receiver 收 `DATE_CHANGED` 與 `TIMEZONE_CHANGED`
- [x] 3.3 `updatePeriodMillis` 30 分鐘當保險
- [x] 3.4 一律讀 `todayDate()`，不受 App 裡「正在看的那一天」影響

## 4. 驗證與提交

- [x] 4.1 `assembleDebug` 與 `assembleRelease` 綠燈
- [x] 4.2 `dumpsys appwidget` 看得到 provider
- [x] 4.3 `APPWIDGET_UPDATE` 與 `DATE_CHANGED` 廣播不崩
- [x] 4.4 release（R8）版實機安裝，provider 與 layout 沒被 shrink 掉
- [x] 4.5 版面在測試宿主上渲染，數字與按鈕正確
- [ ] 4.6 真機（Pixel）確認實際尺寸下的完整外觀——BlueStacks 的桌面沒有小工具選單
