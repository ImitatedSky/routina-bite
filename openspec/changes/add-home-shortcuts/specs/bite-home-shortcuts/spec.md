# bite-home-shortcuts

## Purpose

讓使用者不必開啟 Routina Bite 就能完成最常做的幾件事：長按 App 圖示（或按釘在桌面的捷徑）
記一杯水、記 1 份最近吃過的食物，或直接跳到今天的新增紀錄頁。

## ADDED Requirements

### Requirement: Always-available static shortcuts
The app SHALL publish two static shortcuts that exist from the moment it is installed, without the app
having been launched: 記一餐 (long label 記一餐（今天）) and 喝水 (long label 喝水 +250 ml).
記一餐 SHALL come first and 喝水 second in the launcher's menu.

#### Scenario: Fresh install, app never opened
- **WHEN** the app is installed and the user long-presses its launcher icon before opening it
- **THEN** 記一餐 and 喝水 are both offered, in that order

#### Scenario: Labels
- **WHEN** the launcher shows the shortcut menu
- **THEN** the short labels read 記一餐 and 喝水, and the long labels read 記一餐（今天） and 喝水 +250 ml

### Requirement: Dynamic shortcuts follow the most recently eaten foods
The app SHALL publish one dynamic shortcut for each of the two most recently logged foods, ordered
most recent first and placed after the static shortcuts. Each SHALL use the food's name as its short
label and 記 1 份 <名稱> as its long label. The set SHALL be recomputed when the app starts and whenever
diary entries or the food library change. A food that is deleted from the library SHALL NOT have a shortcut.
Recomputing and publishing SHALL happen off the main thread.

#### Scenario: Logging a food promotes it
- **WHEN** the user logs a food that was not among the two most recent
- **THEN** that food becomes the first dynamic shortcut and the previously second one drops off

#### Scenario: Deleting the entry demotes it again
- **WHEN** the user deletes the entry that had promoted a food
- **THEN** the dynamic shortcuts go back to the two foods that are now most recent

#### Scenario: Food removed from the library
- **WHEN** a food that had a shortcut is deleted from the food library
- **THEN** it no longer has a shortcut, even though the diary entries that referenced it remain

#### Scenario: Never eaten anything
- **WHEN** no diary entry references a food that still exists
- **THEN** no dynamic shortcut is published and the two static ones are still offered

### Requirement: Water shortcut logs without opening a screen
Choosing the 喝水 shortcut SHALL add 250 ml to today's water total and SHALL NOT present any screen of
the app. It SHALL report the result with a toast reading `+<ml> ml · 今天 <總量> / <目標>`.
Repeated uses SHALL accumulate.

#### Scenario: One tap
- **WHEN** the user picks 喝水 while today's total is 0 and the target is 2000
- **THEN** no app screen appears and a toast reads `+250 ml · 今天 250 / 2000`

#### Scenario: Tapping again
- **WHEN** the user picks 喝水 a second time
- **THEN** the toast reads `+250 ml · 今天 500 / 2000` and the today screen shows 500 / 2000 ml

#### Scenario: App already open on the today screen
- **WHEN** the user picks 喝水 while the today screen is in the foreground
- **THEN** the water figure on that screen updates in place and the screen is not replaced or restarted

### Requirement: Food shortcut logs one serving without opening a screen
Choosing a food shortcut SHALL add one serving of that food to today, SHALL NOT present any screen of
the app, and SHALL report with a toast reading `已記 1 份 <名稱> · <大卡> 大卡`. The entry SHALL be stored
exactly as an entry added inside the app: the food's name, serving grams and per-serving nutrients as a
snapshot, servings 1, and the food's id.

#### Scenario: One tap
- **WHEN** the user picks a food shortcut for a 193 kcal food
- **THEN** no app screen appears, a toast reads `已記 1 份 <名稱> · 193 大卡`, and today's total rises by 193

#### Scenario: Entry looks like any other
- **WHEN** the user opens the today screen afterwards
- **THEN** the entry is listed with its name, `1 份 · <每份公克> g` and its kcal, and can be edited or deleted normally

### Requirement: Shortcuts always mean now
Every shortcut SHALL act on today's date, regardless of which date the app is currently showing, and
SHALL choose the meal from the current time using the app's normal default-meal rule.

#### Scenario: App parked on an older day
- **WHEN** the app was left showing last Tuesday and the user picks the 喝水 or a food shortcut
- **THEN** the water or the entry is recorded on today, not on last Tuesday

#### Scenario: Meal by time of day
- **WHEN** the user picks a food shortcut at 12:15
- **THEN** the entry lands in 午餐

### Requirement: Shortcut failures never crash the app
A shortcut invocation that cannot be carried out SHALL report with a toast and end quietly. In particular,
a food shortcut whose food no longer exists SHALL toast `這個食物已經不在食物庫了` and SHALL NOT record anything.

#### Scenario: Food gone
- **WHEN** a food shortcut is invoked for a food id that is not in the library
- **THEN** a toast reads 這個食物已經不在食物庫了, nothing is added to the diary, and the app does not crash

#### Scenario: Unrecognised request
- **WHEN** the shortcut entry point is invoked without a recognised action
- **THEN** a toast explains that nothing could be done and the app does not crash

### Requirement: 記一餐 opens today's add screen, cold or warm
Choosing the 記一餐 shortcut SHALL open the app on the add-entry screen for today with the meal
preselected from the current time, whether or not the app was already running. Returning from that
screen SHALL land on the today screen. The request SHALL take effect once only: a configuration change
such as a rotation SHALL NOT reopen the add screen.

#### Scenario: App not running
- **WHEN** the user picks 記一餐 with the app fully stopped
- **THEN** the app starts and shows 新增紀錄 for today with the time-appropriate meal selected

#### Scenario: App already running on another screen
- **WHEN** the user picks 記一餐 while the app is showing, say, the settings screen
- **THEN** the app comes forward on 新增紀錄 for today

#### Scenario: Back from the add screen
- **WHEN** the user presses back on that add screen
- **THEN** the today screen is shown

#### Scenario: Rotation
- **WHEN** the user goes back to the today screen and then rotates the device
- **THEN** the today screen stays; the add screen does not reopen

### Requirement: Pinning from settings
The settings screen SHALL offer a 桌面捷徑 section with an 加到桌面 action for 喝水 +250 and for 記一餐,
each asking the launcher to pin the corresponding shortcut rather than creating a duplicate. When the
launcher does not support pinning, the app SHALL say so with
`這個桌面不支援釘選捷徑，請改用長按 App 圖示` instead of failing silently or claiming success.

#### Scenario: Launcher supports pinning
- **WHEN** the user taps 加到桌面 for 喝水 +250 on a launcher that supports pinning
- **THEN** the system's pin confirmation appears for the 喝水 shortcut

#### Scenario: Launcher does not support pinning
- **WHEN** the user taps 加到桌面 on a launcher that does not support pinning
- **THEN** the message 這個桌面不支援釘選捷徑，請改用長按 App 圖示 is shown and nothing else happens

### Requirement: Shortcuts need no permissions
Publishing, using and pinning shortcuts SHALL require no Android permission. The app manifest SHALL
continue to declare no `uses-permission`, and shortcut feedback SHALL use toasts rather than notifications.

#### Scenario: Installing the app
- **WHEN** the user installs this release
- **THEN** no permission is requested at install time or when any shortcut is used
