# bite-diary

## ADDED Requirements

### Requirement: Swipe between days
The day screen SHALL let the user move to the previous or next day by swiping horizontally, with
the page following the finger and settling on a whole day, and SHALL keep the date arrows, the
back-to-today button and picking a day from history working and in step with the swipe.

#### Scenario: Swipe to the previous day
- **WHEN** the user swipes from left to right on the day screen
- **THEN** the previous day's totals, water, note and entries are shown

#### Scenario: Swipe to the next day
- **WHEN** the user swipes from right to left
- **THEN** the next day is shown

#### Scenario: Logging on the day being viewed
- **WHEN** the user has swiped to an earlier day and adds an entry or water
- **THEN** it is recorded on that earlier day, not on today

#### Scenario: Back to today
- **WHEN** the user taps back-to-today after swiping away
- **THEN** today's page is shown

#### Scenario: Picking a day far back from history
- **WHEN** the user picks a day a year ago from the history list
- **THEN** that day is shown at once, and swiping continues from it

#### Scenario: Vertical scrolling still works
- **WHEN** the user scrolls the day's content up or down
- **THEN** the content scrolls and the day does not change
