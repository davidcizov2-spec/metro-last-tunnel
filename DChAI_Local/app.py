import json, os, threading, urllib.request, urllib.error, tkinter as tk, subprocess
from tkinter import ttk, messagebox

APP = "D.Ch AI Local"
CFG_DIR = os.path.join(os.getenv("APPDATA", os.path.expanduser("~")), "DChAI_Local")
os.makedirs(CFG_DIR, exist_ok=True)
HISTORY = os.path.join(CFG_DIR, "history.json")
SETTINGS = os.path.join(CFG_DIR, "settings.json")
DEFAULT = {"model": "qwen2.5:3b", "overlay": True, "voice_reply": True}

def load(path, fallback):
    try:
        with open(path, "r", encoding="utf-8") as f: return json.load(f)
    except Exception: return fallback

def save(path, value):
    with open(path, "w", encoding="utf-8") as f: json.dump(value, f, ensure_ascii=False, indent=2)

class App:
    def __init__(self, root):
        self.root = root
        root.title(APP); root.geometry("850x620"); root.minsize(390, 420); root.configure(bg="#101522")
        self.cfg = {**DEFAULT, **load(SETTINGS, {})}
        self.history = load(HISTORY, [])
        self.busy = False; self.recording = False; self.overlay = False
        self.build_ui()
        self.set_overlay(self.cfg.get("overlay", True))
        self.add("D.Ch AI", "Привет! Я D.Ch AI Local — игровой помощник без API-ключа. Установи Ollama и модель qwen2.5:3b по инструкции в README_RU.md. Нажми «Говорить», чтобы задать вопрос голосом.")

    def build_ui(self):
        head = tk.Frame(self.root, bg="#172033"); head.pack(fill="x")
        tk.Label(head, text="D.Ch AI", fg="#70c8ff", bg="#172033", font=("Segoe UI", 16, "bold")).pack(side="left", padx=12, pady=9)
        ttk.Button(head, text="Настройки", command=self.settings).pack(side="right", padx=5, pady=7)
        ttk.Button(head, text="Игровая панель", command=self.toggle_overlay).pack(side="right", padx=5, pady=7)
        self.chat = tk.Text(self.root, wrap="word", bg="#0d1220", fg="#e9eef8", insertbackground="white", relief="flat", padx=13, pady=11, font=("Segoe UI", 10), state="disabled")
        self.chat.pack(fill="both", expand=True, padx=10, pady=10)
        self.chat.tag_configure("name", foreground="#70c8ff", font=("Segoe UI", 10, "bold"))
        self.chat.tag_configure("error", foreground="#ff9999")
        bottom = tk.Frame(self.root, bg="#101522"); bottom.pack(fill="x", padx=10, pady=(0, 8))
        self.entry = tk.Text(bottom, height=3, wrap="word", bg="#1b2436", fg="white", insertbackground="white", relief="flat", font=("Segoe UI", 10))
        self.entry.pack(side="left", fill="both", expand=True, padx=(0, 7))
        self.entry.bind("<Control-Return>", lambda e: (self.send(), "break"))
        buttons = tk.Frame(bottom, bg="#101522"); buttons.pack(side="right")
        ttk.Button(buttons, text="Отправить", command=self.send).pack(fill="x", pady=(0, 4))
        self.voice_button = ttk.Button(buttons, text="Говорить", command=self.voice); self.voice_button.pack(fill="x", pady=(0, 4))
        ttk.Button(buttons, text="Очистить", command=self.clear).pack(fill="x")
        self.status = tk.Label(self.root, text="Ctrl+Enter — отправить • Esc — обычное окно", anchor="w", bg="#101522", fg="#9aa8c0", font=("Segoe UI", 8))
        self.status.pack(fill="x", padx=12, pady=(0, 6))
        self.root.bind("<Escape>", lambda e: self.set_overlay(False) if self.overlay else None)

    def add(self, who, text, error=False):
        self.chat.configure(state="normal")
        self.chat.insert("end", who + "\n", "name"); self.chat.insert("end", text + "\n\n", "error" if error else "normal")
        self.chat.see("end"); self.chat.configure(state="disabled")

    def status_text(self, value):
        self.root.after(0, lambda: self.status.configure(text=value))

    def send(self):
        if self.busy: return
        prompt = self.entry.get("1.0", "end").strip()
        if not prompt: return
        self.entry.delete("1.0", "end"); self.add("Вы", prompt)
        self.history.append({"role": "user", "content": prompt}); save(HISTORY, self.history[-60:])
        self.busy = True; self.status_text("D.Ch AI думает…")
        threading.Thread(target=self.ask_worker, daemon=True).start()

    def ask_worker(self):
        try:
            messages = [{"role": "system", "content": "Ты D.Ch AI, дружелюбный русскоязычный помощник по видеоиграм, Minecraft, программированию и повседневным вопросам. Отвечай понятно и по делу."}]
            messages += self.history[-20:]
            data = json.dumps({"model": self.cfg.get("model", "qwen2.5:3b"), "messages": messages, "stream": False, "options": {"temperature": 0.7}}, ensure_ascii=False).encode("utf-8")
            req = urllib.request.Request("http://127.0.0.1:11434/api/chat", data=data, headers={"Content-Type": "application/json"}, method="POST")
            with urllib.request.urlopen(req, timeout=180) as response: result = json.loads(response.read().decode("utf-8"))
            answer = result.get("message", {}).get("content", "").strip() or "Пустой ответ модели."
            self.history.append({"role": "assistant", "content": answer}); save(HISTORY, self.history[-60:])
            self.root.after(0, lambda a=answer: self.add("D.Ch AI", a))
            if self.cfg.get("voice_reply", True): threading.Thread(target=self.speak, args=(answer,), daemon=True).start()
            self.status_text("Готово")
        except urllib.error.URLError:
            self.root.after(0, lambda: self.add("Ollama не найдена", "Установи Ollama и выполни в терминале: ollama run qwen2.5:3b", True))
            self.status_text("Нет соединения с Ollama")
        except Exception as e:
            self.root.after(0, lambda msg=str(e): self.add("Ошибка", msg, True)); self.status_text("Ошибка")
        finally: self.busy = False

    def speak(self, text):
        try:
            safe = text.replace("'", "''")[:3500]
            script = "Add-Type -AssemblyName System.Speech; $v=New-Object System.Speech.Synthesis.SpeechSynthesizer; $v.Speak('" + safe + "')"
            subprocess.run(["powershell", "-NoProfile", "-Command", script], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL, timeout=90)
        except Exception: pass

    def voice(self):
        if self.recording or self.busy: return
        self.recording = True; self.voice_button.configure(state="disabled", text="Слушаю…"); self.status_text("Говори сейчас…")
        threading.Thread(target=self.voice_worker, daemon=True).start()

    def voice_worker(self):
        try:
            import speech_recognition as sr
            r = sr.Recognizer()
            with sr.Microphone() as source:
                r.adjust_for_ambient_noise(source, duration=0.5); self.status_text("Говори сейчас…")
                audio = r.listen(source, timeout=8, phrase_time_limit=18)
            self.status_text("Распознаю речь…"); words = r.recognize_google(audio, language="ru-RU")
            def submit():
                self.entry.delete("1.0", "end"); self.entry.insert("1.0", words); self.add("Распознано", words); self.send()
            self.root.after(0, submit)
        except Exception as e:
            self.root.after(0, lambda msg=str(e): self.add("Голосовой ввод", "Проверь микрофон и интернет. " + msg, True))
            self.status_text("Не удалось распознать речь")
        finally:
            self.recording = False
            self.root.after(0, lambda: self.voice_button.configure(state="normal", text="Говорить"))

    def clear(self):
        if messagebox.askyesno("Очистить", "Очистить сохранённую историю?"):
            self.history = []; save(HISTORY, [])
            self.chat.configure(state="normal"); self.chat.delete("1.0", "end"); self.chat.configure(state="disabled")

    def toggle_overlay(self): self.set_overlay(not self.overlay)

    def set_overlay(self, enabled):
        self.overlay = bool(enabled); self.root.attributes("-topmost", self.overlay)
        if self.overlay: self.root.geometry("420x540+30+30"); self.root.title("D.Ch AI — игровая панель")
        else: self.root.geometry("850x620"); self.root.title(APP)

    def settings(self):
        win = tk.Toplevel(self.root); win.title("Настройки"); win.configure(bg="#101522"); win.resizable(False, False)
        frame = tk.Frame(win, bg="#101522", padx=15, pady=15); frame.pack(fill="both", expand=True)
        tk.Label(frame, text="Название установленной модели Ollama:", fg="white", bg="#101522").grid(row=0, column=0, sticky="w")
        model = tk.StringVar(value=self.cfg.get("model", "qwen2.5:3b"))
        ttk.Entry(frame, textvariable=model, width=38).grid(row=1, column=0, sticky="ew", pady=(4, 12))
        voice = tk.BooleanVar(value=self.cfg.get("voice_reply", True))
        ttk.Checkbutton(frame, text="Озвучивать ответы голосом Windows", variable=voice).grid(row=2, column=0, sticky="w", pady=(0, 8))
        overlay = tk.BooleanVar(value=self.cfg.get("overlay", True))
        ttk.Checkbutton(frame, text="Запускать поверх других окон", variable=overlay).grid(row=3, column=0, sticky="w", pady=(0, 12))
        tk.Label(frame, text="API-ключ не нужен. Для ответа требуется Ollama с установленной моделью. Голосовое распознавание требует интернет.", fg="#aab8ce", bg="#101522", justify="left", wraplength=360).grid(row=4, column=0, sticky="w", pady=(0, 12))
        def save_settings():
            self.cfg.update({"model": model.get().strip() or "qwen2.5:3b", "voice_reply": voice.get(), "overlay": overlay.get()})
            save(SETTINGS, self.cfg); self.set_overlay(self.cfg["overlay"]); win.destroy()
        ttk.Button(frame, text="Сохранить", command=save_settings).grid(row=5, column=0, sticky="e")

if __name__ == "__main__":
    root = tk.Tk(); App(root); root.mainloop()
