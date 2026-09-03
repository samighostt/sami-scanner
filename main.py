import os
import ssl
import time
import urllib.error
import urllib.request
from concurrent.futures import ThreadPoolExecutor
from threading import Thread

from kivy.app import App
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.metrics import dp
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
from kivy.uix.textinput import TextInput

# الألوان الخاصة بالواجهة
APP_GOLD = (0.93, 0.65, 0.18, 1)
APP_BG = (0.035, 0.055, 0.075, 1)
CARD_BG = (0.065, 0.09, 0.12, 1)
TEXT = (0.95, 0.96, 0.98, 1)
MUTED = (0.60, 0.65, 0.72, 1)
GOOD = (0.25, 0.85, 0.45, 1)
BAD = (0.95, 0.30, 0.30, 1)
WARN = (0.95, 0.75, 0.20, 1)

SECURITY_HEADERS = {
    "Content-Security-Policy": "تمنع هجمات XSS وحقن البرمجيات الخبيثة",
    "Strict-Transport-Security": "تفرض الاتصال المشفر عبر HTTPS (HSTS)",
    "X-Frame-Options": "تحمي من هجمات Clickjacking",
    "X-Content-Type-Options": "تمنع التخمين الخاطئ لأنواع الملفات (MIME Sniffing)",
    "Referrer-Policy": "تحمي تسريب بيانات المرجع في الروابط",
    "Permissions-Policy": "تحكم الوصول للكاميرا والميكروفون والموقع",
}

SENSITIVE_PATHS = [
    "admin",
    "login",
    ".env",
    "config.php",
    "robots.txt",
    ".git/HEAD",
    "sitemap.xml",
    "backup.sql",
]


class NoRedirectHandler(urllib.request.HTTPRedirectHandler):
    def redirect_request(self, req, fp, code, msg, headers, newurl):
        return None


class SamiButton(Button):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.background_normal = ""
        self.background_color = APP_GOLD
        self.color = (0.04, 0.04, 0.04, 1)
        self.bold = True
        self.font_size = dp(14)


class SamiAppLayout(BoxLayout):
    def __init__(self, **kwargs):
        super().__init__(
            orientation="vertical",
            padding=dp(16),
            spacing=dp(10),
            **kwargs,
        )
        self.scan_logs = []

        # الهيدر العلوي
        header = BoxLayout(size_hint_y=None, height=dp(60), spacing=dp(10))
        logo = Label(
            text="[b]S[/b]",
            markup=True,
            font_size=dp(28),
            color=APP_GOLD,
            size_hint_x=None,
            width=dp(40),
            halign="center",
            valign="middle",
        )
        logo.bind(size=lambda w, s: setattr(w, "text_size", s))
        header.add_widget(logo)

        title_box = BoxLayout(orientation="vertical")
        title_box.add_widget(
            Label(
                text="[b]S A M I[/b]",
                markup=True,
                font_size=dp(20),
                color=APP_GOLD,
                halign="left",
                valign="bottom",
            )
        )
        title_box.add_widget(
            Label(
                text="ADVANCED SECURITY AUDITOR",
                font_size=dp(10),
                color=MUTED,
                halign="left",
                valign="top",
            )
        )
        header.add_widget(title_box)
        self.add_widget(header)

        # حقل إدخال الرابط
        self.url_input = TextInput(
            hint_text="Enter URL • https://example.com",
            multiline=False,
            size_hint_y=None,
            height=dp(48),
            padding=[dp(12), dp(12)],
            font_size=dp(14),
            background_normal="",
            background_active="",
            background_color=CARD_BG,
            foreground_color=TEXT,
            hint_text_color=MUTED,
        )
        self.add_widget(self.url_input)

        # أزرار التحكم
        btn_box = BoxLayout(
            size_hint_y=None, height=dp(48), spacing=dp(8)
        )
        self.scan_button = SamiButton(text="START AUDIT")
        self.scan_button.bind(on_press=self.start_scan)

        self.save_button = SamiButton(
            text="SAVE REPORT", background_color=CARD_BG
        )
        self.save_button.color = TEXT
        self.save_button.bind(on_press=self.save_report)
        self.save_button.disabled = True

        btn_box.add_widget(self.scan_button)
        btn_box.add_widget(self.save_button)
        self.add_widget(btn_box)

        # حالة الفحص
        self.status = Label(
            text="Ready to perform security audit",
            font_size=dp(12),
            color=MUTED,
            size_hint_y=None,
            height=dp(24),
        )
        self.add_widget(self.status)

        # منطقة عرض النتائج
        scroll = ScrollView()
        self.results = BoxLayout(
            orientation="vertical",
            spacing=dp(6),
            size_hint_y=None,
            padding=[0, dp(4)],
        )
        self.results.bind(minimum_height=self.results.setter("height"))
        scroll.add_widget(self.results)
        self.add_widget(scroll)

        footer = Label(
            text="SAMI AUDITOR • Authorized testing & security research only",
            font_size=dp(9),
            color=MUTED,
            size_hint_y=None,
            height=dp(20),
        )
        self.add_widget(footer)

    def add_result(self, text, color=TEXT, bold=False):
        self.scan_logs.append(text)
        label = Label(
            text=text,
            bold=bold,
            font_size=dp(12),
            color=color,
            size_hint_y=None,
            halign="left",
            valign="middle",
        )
        label.bind(size=lambda w, s: setattr(w, "text_size", (s[0], None)))
        label.bind(
            texture_size=lambda instance, value: setattr(
                instance, "height", max(value[1] + dp(8), dp(26))
            )
        )
        self.results.add_widget(label)

    def clear_results(self):
        self.results.clear_widgets()
        self.scan_logs.clear()

    def start_scan(self, *_):
        url = self.url_input.text.strip()
        if not url:
            self.status.text = "Please enter a valid URL."
            self.status.color = BAD
            return

        if not url.startswith(("http://", "https://")):
            url = "https://" + url

        self.clear_results()
        self.status.text = "Running comprehensive analysis..."
        self.status.color = APP_GOLD
        self.scan_button.disabled = True
        self.save_button.disabled = True
        Thread(target=self.run_audit, args=(url,), daemon=True).start()

    def ui(self, func, *args):
        Clock.schedule_once(lambda dt: func(*args), 0)

    def fetch_http(self, url, timeout=6, allow_redirects=True):
        req = urllib.request.Request(
            url,
            headers={
                "User-Agent": (
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) SAMI Auditor/2.0"
                )
            },
        )
        ctx = ssl.create_default_context()
        ctx.check_hostname = False
        ctx.verify_mode = ssl.CERT_NONE

        opener = (
            urllib.request.build_opener(urllib.request.HTTPSHandler(context=ctx))
            if allow_redirects
            else urllib.request.build_opener(
                urllib.request.HTTPSHandler(context=ctx), NoRedirectHandler
            )
        )

        start = time.time()
        try:
            with opener.open(req, timeout=timeout) as resp:
                return {
                    "code": resp.status,
                    "url": resp.geturl(),
                    "headers": {k.title(): v for k, v in resp.headers.items()},
                    "elapsed": time.time() - start,
                    "error": None,
                }
        except urllib.error.HTTPError as e:
            return {
                "code": e.code,
                "url": e.url,
                "headers": {k.title(): v for k, v in e.headers.items()},
                "elapsed": time.time() - start,
                "error": None,
            }
        except Exception as exc:
            return {
                "code": None,
                "url": url,
                "headers": {},
                "elapsed": 0,
                "error": str(exc),
            }

    def check_endpoint(self, base_url, path):
        target = f"{base_url.rstrip('/')}/{path}"
        res = self.fetch_http(target, timeout=5, allow_redirects=False)
        return path, res["code"]

    def run_audit(self, target_url):
        main_res = self.fetch_http(target_url, timeout=8, allow_redirects=True)
        if main_res["error"]:
            self.ui(
                self.finish_error, f"Connection failed: {main_res['error']}"
            )
            return

        # 1. Target Summary
        self.ui(self.add_result, "=== TARGET SUMMARY ===", APP_GOLD, True)
        self.ui(self.add_result, f"URL: {main_res['url']}")
        self.ui(
            self.add_result,
            f"Status Code: {main_res['code']} | Time: {main_res['elapsed']:.2f}s",
        )
        is_https = main_res["url"].startswith("https://")
        self.ui(
            self.add_result,
            "[+] HTTPS Encryption Enabled"
            if is_https
            else "[-] HTTP Protocol Used (Insecure)",
            GOOD if is_https else BAD,
        )

        # 2. Security Headers Audit
        headers_lower = {k.lower(): v for k, v in main_res["headers"].items()}
        self.ui(self.add_result, "\n=== HARDENING & HEADERS ===", APP_GOLD, True)
        for header, desc in SECURITY_HEADERS.items():
            if header.lower() in headers_lower:
                self.ui(self.add_result, f"[+] {header} : PRESENT", GOOD)
            else:
                self.ui(self.add_result, f"[-] {header} : MISSING", BAD)

        # 3. Information Disclosure & Technology Leak
        self.ui(self.add_result, "\n=== TECH & CORS AUDIT ===", APP_GOLD, True)
        server = headers_lower.get("server")
        powered = headers_lower.get("x-powered-by")
        cors = headers_lower.get("access-control-allow-origin")

        if server:
            self.ui(self.add_result, f"[!] Server Exposed: {server}", WARN)
        else:
            self.ui(self.add_result, "[+] Server Banner Hidden", GOOD)

        if powered:
            self.ui(self.add_result, f"[-] Technology Leak: {powered}", BAD)

        if cors == "*":
            self.ui(self.add_result, "[!] Wildcard CORS (*): High Exposure", BAD)
        elif cors:
            self.ui(self.add_result, f"[+] CORS Restricted: {cors}", GOOD)
        else:
            self.ui(self.add_result, "[*] CORS Header Not Present", MUTED)

        # 4. Cookie Security Analysis
        self.ui(self.add_result, "\n=== COOKIE FLAGS AUDIT ===", APP_GOLD, True)
        cookie_header = main_res["headers"].get("Set-Cookie")
        if cookie_header:
            cookie_str = str(cookie_header).lower()
            if "httponly" in cookie_str:
                self.ui(self.add_result, "[+] Cookie Flag: HttpOnly Set", GOOD)
            else:
                self.ui(
                    self.add_result,
                    "[-] Cookie Flag: HttpOnly Missing (XSS Risk)",
                    BAD,
                )

            if "secure" in cookie_str:
                self.ui(self.add_result, "[+] Cookie Flag: Secure Set", GOOD)
            else:
                self.ui(self.add_result, "[-] Cookie Flag: Secure Missing", BAD)

            if "samesite" in cookie_str:
                self.ui(
                    self.add_result, "[+] Cookie Flag: SameSite Configured", GOOD
                )
            else:
                self.ui(
                    self.add_result, "[!] Cookie Flag: SameSite Missing", WARN
                )
        else:
            self.ui(
                self.add_result, "[*] No Set-Cookie headers detected", MUTED
            )

        # 5. Parallel Endpoint Recon (خلفي لتجنب تجميد الواجهة)
        self.ui(
            self.add_result, "\n=== SENSITIVE PATHS CHECK ===", APP_GOLD, True
        )
        base = main_res["url"]

        with ThreadPoolExecutor(max_workers=4) as executor:
            futures = [
                executor.submit(self.check_endpoint, base, path)
                for path in SENSITIVE_PATHS
            ]
            for future in futures:
                path, code = future.result()
                if code and code < 400:
                    self.ui(
                        self.add_result,
                        f"[!] [HTTP {code}] /{path} ACCESSIBLE",
                        BAD,
                    )
                elif code:
                    self.ui(self.add_result, f"[+] [HTTP {code}] /{path}", GOOD)
                else:
                    self.ui(self.add_result, f"[-] [ERR] /{path}", MUTED)

        def complete_ui():
            self.status.text = "Audit completed successfully."
            self.status.color = GOOD
            self.scan_button.disabled = False
            self.save_button.disabled = False

        self.ui(complete_ui)

    def finish_error(self, message):
        self.clear_results()
        self.add_result("[-] " + message, BAD)
        self.status.text = "Audit failed."
        self.status.color = BAD
        self.scan_button.disabled = False

    def save_report(self, *_):
        try:
            report_text = "\n".join(self.scan_logs)
            app = App.get_running_app()
            save_dir = app.user_data_dir if app else "."
            file_path = os.path.join(save_dir, "sami_security_report.txt")

            with open(file_path, "w", encoding="utf-8") as f:
                f.write(report_text)

            self.status.text = f"Saved to app storage"
            self.status.color = GOOD
        except Exception as e:
            self.status.text = f"Save failed: {e}"
            self.status.color = BAD


class SamiApp(App):
    title = "SAMI Security Auditor"

    def build(self):
        Window.clearcolor = APP_BG
        return SamiAppLayout()


if __name__ == "__main__":
    SamiApp().run()
