import requests
from threading import Thread

from kivy.app import App
from kivy.clock import Clock
from kivy.core.window import Window
from kivy.metrics import dp
from kivy.properties import StringProperty
from kivy.uix.boxlayout import BoxLayout
from kivy.uix.button import Button
from kivy.uix.label import Label
from kivy.uix.scrollview import ScrollView
from kivy.uix.textinput import TextInput


APP_GOLD = (0.93, 0.65, 0.18, 1)
APP_BG = (0.035, 0.055, 0.075, 1)
CARD_BG = (0.065, 0.09, 0.12, 1)
TEXT = (0.95, 0.96, 0.98, 1)
MUTED = (0.60, 0.65, 0.72, 1)
GOOD = (0.25, 0.85, 0.45, 1)
BAD = (0.95, 0.30, 0.30, 1)


SECURITY_HEADERS = [
    "Content-Security-Policy",
    "Permissions-Policy",
    "Strict-Transport-Security",
    "X-Content-Type-Options",
]

PATHS = ["admin", "login", ".env", "config"]


class SamiButton(Button):
    def __init__(self, **kwargs):
        super().__init__(**kwargs)
        self.background_normal = ""
        self.background_color = APP_GOLD
        self.color = (0.04, 0.04, 0.04, 1)
        self.bold = True
        self.font_size = dp(15)


class SamiAppLayout(BoxLayout):
    def __init__(self, **kwargs):
        super().__init__(
            orientation="vertical",
            padding=dp(18),
            spacing=dp(12),
            **kwargs
        )
        self.background_color = APP_BG

        header = BoxLayout(size_hint_y=None, height=dp(90), spacing=dp(12))

        logo = Label(
            text="[b]♛[/b]\n[b]S[/b]",
            markup=True,
            font_size=dp(25),
            color=APP_GOLD,
            size_hint_x=None,
            width=dp(58),
            halign="center",
            valign="middle",
        )
        logo.bind(size=lambda w, s: setattr(w, "text_size", s))
        header.add_widget(logo)

        title_box = BoxLayout(orientation="vertical")
        title_box.add_widget(Label(
            text="[b]S A M I[/b]",
            markup=True,
            font_size=dp(27),
            color=APP_GOLD,
            halign="left",
            valign="bottom",
        ))
        title_box.add_widget(Label(
            text="SECURITY SCANNER",
            font_size=dp(11),
            color=MUTED,
            halign="left",
            valign="top",
        ))
        header.add_widget(title_box)
        self.add_widget(header)

        self.add_widget(Label(
            text="Professional web security inspection",
            font_size=dp(13),
            color=MUTED,
            size_hint_y=None,
            height=dp(28),
            halign="left",
        ))

        self.url_input = TextInput(
            hint_text="Enter target URL  •  https://example.com",
            multiline=False,
            size_hint_y=None,
            height=dp(54),
            padding=[dp(14), dp(15)],
            font_size=dp(15),
            background_normal="",
            background_active="",
            background_color=CARD_BG,
            foreground_color=TEXT,
            hint_text_color=MUTED,
        )
        self.add_widget(self.url_input)

        self.scan_button = SamiButton(
            text="⌕   START SCAN",
            size_hint_y=None,
            height=dp(54),
        )
        self.scan_button.bind(on_press=self.start_scan)
        self.add_widget(self.scan_button)

        self.status = Label(
            text="Ready",
            font_size=dp(13),
            color=MUTED,
            size_hint_y=None,
            height=dp(30),
        )
        self.add_widget(self.status)

        scroll = ScrollView()
        self.results = BoxLayout(
            orientation="vertical",
            spacing=dp(7),
            size_hint_y=None,
            padding=[0, dp(4)],
        )
        self.results.bind(minimum_height=self.results.setter("height"))
        scroll.add_widget(self.results)
        self.add_widget(scroll)

        footer = Label(
            text="SAMI  •  Use only on systems you own or are authorized to test.",
            font_size=dp(10),
            color=MUTED,
            size_hint_y=None,
            height=dp(25),
        )
        self.add_widget(footer)

    def add_result(self, text, color=TEXT, bold=False):
        label = Label(
            text=text,
            markup=bold,
            font_size=dp(13),
            color=color,
            size_hint_y=None,
            height=dp(34),
            halign="left",
            valign="middle",
        )
        label.bind(size=lambda w, s: setattr(w, "text_size", (s[0], None)))
        self.results.add_widget(label)

    def clear_results(self):
        self.results.clear_widgets()

    def start_scan(self, *_):
        url = self.url_input.text.strip()
        if not url:
            self.status.text = "Please enter a URL."
            self.status.color = BAD
            return

        if not url.startswith(("http://", "https://")):
            url = "https://" + url

        self.clear_results()
        self.status.text = "Scanning..."
        self.status.color = APP_GOLD
        self.scan_button.disabled = True
        Thread(target=self.scan, args=(url,), daemon=True).start()

    def ui(self, func, *args):
        Clock.schedule_once(lambda dt: func(*args), 0)

    def scan(self, url):
        try:
            response = requests.get(url, timeout=7, allow_redirects=True)
        except requests.RequestException as exc:
            self.ui(self.finish_error, f"Connection error: {exc}")
            return

        self.ui(self.show_scan, response, url)

    def show_scan(self, response, original_url):
        self.add_result("━━━ TARGET ━━━", APP_GOLD, True)
        self.add_result(response.url)
        self.add_result(f"HTTP status: {response.status_code}")
        self.add_result(f"Response time: {response.elapsed.total_seconds():.2f}s")

        self.add_result("━━━ SECURITY HEADERS ━━━", APP_GOLD, True)
        for header in SECURITY_HEADERS:
            if header in response.headers:
                self.add_result(f"✓  {header}   FOUND", GOOD)
            else:
                self.add_result(f"✕  {header}   NOT FOUND", BAD)

        self.add_result("━━━ ENDPOINT CHECK ━━━", APP_GOLD, True)
        base = response.url.rstrip("/")

        for path in PATHS:
            target = f"{base}/{path}"
            try:
                result = requests.get(
                    target, timeout=5, allow_redirects=False
                )
                color = GOOD if result.status_code < 400 else BAD
                self.add_result(f"{result.status_code}   /{path}", color)
            except requests.RequestException:
                self.add_result(f"ERR   /{path}", BAD)

        self.status.text = "Scan completed"
        self.status.color = GOOD
        self.scan_button.disabled = False

    def finish_error(self, message):
        self.clear_results()
        self.add_result("✕  " + message, BAD)
        self.status.text = "Scan failed"
        self.status.color = BAD
        self.scan_button.disabled = False


class SamiApp(App):
    title = "SAMI Security Scanner"

    def build(self):
        Window.clearcolor = APP_BG
        return SamiAppLayout()


if __name__ == "__main__":
    SamiApp().run()
