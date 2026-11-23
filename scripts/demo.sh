#!/usr/bin/env bash
set -eu
mkdir -p media
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell pm clear ru.student.diary
adb shell settings put secure show_ime_with_hard_keyboard 0
dir=/sdcard/Android/data/ru.student.diary/files/demo
adb shell am instrument -w ru.student.diary.test/androidx.test.runner.AndroidJUnitRunner > media/test.log &
test_pid=$!
for i in {1..90}; do
    if adb shell test -f "$dir/01-empty.png"; then break; fi
    sleep 1
done
adb shell test -f "$dir/01-empty.png"
adb shell screenrecord --time-limit 65 --bit-rate 2000000 /sdcard/diary.mp4 &
rec_pid=$!
wait "$test_pid"
wait "$rec_pid"
grep -q 'OK (1 test)' media/test.log
adb pull "$dir/." media/
adb pull /sdcard/diary.mp4 media/demo.mp4
