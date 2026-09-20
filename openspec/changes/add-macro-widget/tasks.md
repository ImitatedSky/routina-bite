# Tasks: add-macro-widget

## 1. 今日營養小工具

- [x] 1.1 `BiteMacroWidgetProvider`：讀今天的熱量與三大營養素合計
- [x] 1.2 `res/layout/bite_macro_widget.xml`：一行熱量＋進度條，下面三欄等寬
- [x] 1.3 `res/xml/bite_macro_widget_info.xml`：4×2，與另一個小工具同尺寸
- [x] 1.4 三大營養素色進 `colors.xml` 與 `values-night/colors.xml`（深色底要提亮一階）
- [x] 1.5 manifest 第二個 receiver、`proguard-rules.pro` 加 `-keep`
- [x] 1.6 `BiteApp` 的資料流一併重畫兩種小工具；`requestCode` 與另一個不撞

## 2. 熱量自動換算

- [x] 2.1 `EntryFormState.kcalIsManual`：使用者動過熱量欄就不再自動覆蓋
- [x] 2.2 `recalcKcalFromMacros()`：蛋白質與碳水 4、脂肪 9，改三大營養素時重算
- [x] 2.3 熱量欄清空＝交還自動換算
- [x] 2.4 `loadFrom` 時依 `basis.kcal > 0` 決定是不是「已經指定」（食物庫的標示值不被蓋掉）
- [x] 2.5 表單的說明文字寫清楚這個行為

## 3. 驗證與提交

- [x] 3.1 `assembleRelease` 綠燈
- [x] 3.2 兩個 provider 都出現在 `dumpsys appwidget`
- [x] 3.3 實機：蛋白質 20 + 脂肪 10 → 熱量自動變 170
- [x] 3.4 實機：手動把熱量改成 500，之後改碳水仍維持 500
- [ ] 3.5 今日營養小工具的實際渲染——測試宿主只綁得到第一個 provider，BlueStacks 的桌面沒有小工具選單，要等 Pixel
