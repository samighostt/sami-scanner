[app]
title = SAMI Security Auditor
package.name = sami
package.domain = com.sami
source.dir = .
source.include_exts = py,png,jpg,kv,atlas
version = 1.0.0

# تم إزالة requests للاعتصام بمكتبة urllib المدمجة وتجنب فشل التجميع
requirements = python3==3.11.9,kivy,openssl

orientation = portrait
fullscreen = 0
android.permissions = INTERNET

android.api = 34
android.minapi = 24
android.ndk = 25b
android.archs = arm64-v8a
android.accept_sdk_license = True

# احرص على وجود صورة باسم icon.png في نفس المجلد أو قم بوضع # قبل السطر التالي
icon.filename = %(source.dir)s/icon.png

[buildozer]
log_level = 2
warn_on_root = 1
