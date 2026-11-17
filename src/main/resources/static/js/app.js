// Composants Alpine de l'interface (état purement client). Aucune logique métier ici :
// les données et les règles viennent du serveur, via HTMX.
document.addEventListener("alpine:init", () => {
  const pad = (n) => String(n).padStart(2, "0");
  const isoDate = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
  const sameDay = (a, b) => a && b && isoDate(a) === isoDate(b);
  const fmt = (d, options) => new Intl.DateTimeFormat("fr-FR", options).format(d);
  const MONTHS = ["Janvier", "Février", "Mars", "Avril", "Mai", "Juin", "Juillet",
    "Août", "Septembre", "Octobre", "Novembre", "Décembre"];

  // Toasts façon « sonner » : alimentés par l'en-tête HX-Trigger {"toast": {...}} ou par un flash serveur.
  Alpine.data("toaster", (initial) => ({
    toasts: [],
    nextId: 0,
    init() {
      (Array.isArray(initial) ? initial : []).forEach((toast) => this.add(toast));
    },
    add({ message, category = "info" }) {
      const id = this.nextId++;
      this.toasts.push({ id, message, category, visible: true });
      setTimeout(() => this.dismiss(id), 4000);
    },
    dismiss(id) {
      const toast = this.toasts.find((t) => t.id === id);
      if (toast) toast.visible = false;
      setTimeout(() => { this.toasts = this.toasts.filter((t) => t.id !== id); }, 300);
    },
  }));

  // Apparition au défilement (ScrollReveal de l'original : seuil 0.1, marge basse -60px, une seule fois).
  Alpine.data("scrollReveal", (delay = 0) => ({
    visible: false,
    init() {
      if (delay) this.$el.style.transitionDelay = `${delay}ms`;
      const observer = new IntersectionObserver(([entry]) => {
        if (entry.isIntersecting) {
          this.visible = true;
          observer.disconnect();
        }
      }, { threshold: 0.1, rootMargin: "0px 0px -60px 0px" });
      observer.observe(this.$el);
    },
  }));

  // Sélecteur de date « M3 » (M3DatePicker / M3DateTimePicker de l'original).
  // La valeur soumise est un input caché au format yyyy-MM-dd ou yyyy-MM-ddTHH:mm.
  Alpine.data("datePicker", ({ value = "", withTime = false, fromYear = 1925, toYear = new Date().getFullYear() + 5 } = {}) => ({
    open: false,
    withTime,
    selected: null,
    time: "09:00",
    month: new Date(),
    months: MONTHS,
    years: [],
    init() {
      for (let y = toYear; y >= fromYear; y--) this.years.push(y);
      this.parse(value);
    },
    parse(raw) {
      if (!raw) return;
      const [datePart, timePart] = raw.split("T");
      const [y, m, d] = datePart.split("-").map(Number);
      if (!y || !m || !d) return;
      this.selected = new Date(y, m - 1, d);
      if (timePart) this.time = timePart.slice(0, 5);
      this.month = new Date(y, m - 1, 1);
    },
    get submitted() {
      if (!this.selected) return "";
      return this.withTime ? `${isoDate(this.selected)}T${this.time}` : isoDate(this.selected);
    },
    get display() {
      if (!this.selected) return "";
      if (this.withTime) {
        return `${fmt(this.selected, { day: "numeric", month: "short", year: "numeric" })} à ${this.time}`;
      }
      return fmt(this.selected, { day: "numeric", month: "long", year: "numeric" });
    },
    get headerDate() {
      return this.selected ? fmt(this.selected, { weekday: "short", day: "numeric", month: "short", year: "numeric" }) : "—";
    },
    get weeks() {
      const first = new Date(this.month.getFullYear(), this.month.getMonth(), 1);
      const offset = (first.getDay() + 6) % 7; // semaine commençant le lundi
      const start = new Date(first);
      start.setDate(first.getDate() - offset);
      const daysInMonth = new Date(this.month.getFullYear(), this.month.getMonth() + 1, 0).getDate();
      const weekCount = Math.ceil((offset + daysInMonth) / 7);
      const cursor = new Date(start);
      return Array.from({ length: weekCount }, () => Array.from({ length: 7 }, () => {
        const day = {
          date: new Date(cursor),
          outside: cursor.getMonth() !== this.month.getMonth(),
          today: sameDay(cursor, new Date()),
          selected: sameDay(cursor, this.selected),
        };
        cursor.setDate(cursor.getDate() + 1);
        return day;
      }));
    },
    setMonthIndex(i) { this.month = new Date(this.month.getFullYear(), Number(i), 1); },
    setYear(y) { this.month = new Date(Number(y), this.month.getMonth(), 1); },
    shift(delta) { this.month = new Date(this.month.getFullYear(), this.month.getMonth() + delta, 1); },
    pick(date) {
      this.selected = new Date(date);
      this.month = new Date(date.getFullYear(), date.getMonth(), 1);
      if (!this.withTime) this.open = false;
      this.$nextTick(() => this.$refs.input.dispatchEvent(new Event("change", { bubbles: true })));
    },
    today() { this.pick(new Date()); },
  }));

  // Page /auth : onglets, email partagé, « Se souvenir de moi » (email seul, comme l'original) et jauge du mot de passe.
  const REMEMBER_KEY = "mt_remember_email";
  const storage = {
    get: () => { try { return localStorage.getItem(REMEMBER_KEY); } catch (e) { return null; } },
    set: (v) => { try { localStorage.setItem(REMEMBER_KEY, v); } catch (e) { /* stockage indisponible */ } },
    clear: () => { try { localStorage.removeItem(REMEMBER_KEY); } catch (e) { /* stockage indisponible */ } },
  };
  Alpine.data("authPage", ({ email = "", unconfirmedEmail = "" } = {}) => ({
    tab: "login",
    email: email || storage.get() || "",
    password: "",
    rememberMe: storage.get() !== null,
    showPassword: false,
    loading: false,
    unconfirmedEmail,
    rules: [
      { key: "length", label: "24 caractères ou plus" },
      { key: "lower", label: "Une lettre minuscule (a-z)" },
      { key: "upper", label: "Une lettre majuscule (A-Z)" },
      { key: "digit", label: "Un chiffre (0-9)" },
      { key: "special", label: "Un caractère spécial (! @ # $ % …)" },
    ],
    get checks() {
      const pw = this.password;
      return {
        length: pw.length >= 24 && pw.length <= 72,
        lower: /[a-z]/.test(pw),
        upper: /[A-Z]/.test(pw),
        digit: /\d/.test(pw),
        special: /[^A-Za-z0-9]/.test(pw),
      };
    },
    get passed() { return Object.values(this.checks).filter(Boolean).length; },
    rememberEmail() {
      const normalized = this.email.trim().toLowerCase();
      if (this.rememberMe) storage.set(normalized); else storage.clear();
    },
  }));

  // Select riche (Radix Select) : le déclencheur reprend le contenu HTML de l'option choisie (badge, pastille…).
  Alpine.data("richSelect", (value = "") => ({
    open: false,
    value,
    get label() {
      const option = this.options().find((o) => o.dataset.value === this.value);
      return option ? option.querySelector("[data-label]").innerHTML : "";
    },
    toggle() {
      this.open = !this.open;
      if (this.open) this.$nextTick(() => (this.options().find((o) => o.dataset.value === this.value) ?? this.options()[0])?.focus());
    },
    options() {
      return this.$refs.list ? [...this.$refs.list.querySelectorAll("[role=option]")] : [];
    },
    move(delta) {
      const options = this.options();
      const index = options.indexOf(document.activeElement);
      options[Math.min(options.length - 1, Math.max(0, index + delta))]?.focus();
    },
    close() {
      this.open = false;
      this.$refs.trigger.focus({ preventScroll: true });
    },
    choose(option) {
      this.value = option.dataset.value;
      this.close();
      this.$nextTick(() => this.$refs.input.dispatchEvent(new Event("change", { bubbles: true })));
    },
  }));

  // « Générer un mot de passe fort » (generateStrongPassword de l'original) : une lettre minuscule, une majuscule,
  // un chiffre et un caractère spécial garantis, caractères ambigus exclus, tirage cryptographique puis mélange.
  Alpine.magic("strongPassword", () => (length = 28) => {
    const lowers = "abcdefghijkmnopqrstuvwxyz";
    const uppers = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    const digits = "23456789";
    const specials = "!@#$%^&*()-_=+[]{};:,.?";
    const all = lowers + uppers + digits + specials;
    const rnd = new Uint32Array(length + length);
    crypto.getRandomValues(rnd);
    const chars = [lowers, uppers, digits, specials].map((set, i) => set[rnd[i] % set.length]);
    for (let i = 4; i < length; i++) chars.push(all[rnd[i] % all.length]);
    for (let i = chars.length - 1; i > 0; i--) {
      const j = rnd[length + i] % (i + 1);
      [chars[i], chars[j]] = [chars[j], chars[i]];
    }
    return chars.join("");
  });

  // Graphique d'évolution (TrackingChartCard) : mode relatif/brut, période et séries affichées.
  const CHART_FIELDS = [
    { key: "weightKg", label: "Poids (kg)", unit: "kg", cssVar: "--chart-weight", lowerIsBetter: true },
    { key: "waistCm", label: "Tour de taille", unit: "cm", cssVar: "--chart-waist", lowerIsBetter: true },
    { key: "hipCm", label: "Hanches", unit: "cm", cssVar: "--chart-hip", lowerIsBetter: false },
    { key: "chestCm", label: "Poitrine", unit: "cm", cssVar: "--chart-chest", lowerIsBetter: false },
    { key: "armCm", label: "Bras", unit: "cm", cssVar: "--chart-arm", lowerIsBetter: false },
    { key: "thighCm", label: "Cuisses", unit: "cm", cssVar: "--chart-thigh", lowerIsBetter: false },
  ];
  const cssColor = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  Alpine.data("trackingChart", (points = []) => {
    let chart = null; // hors du proxy réactif d'Alpine : Chart.js gère son propre état
    return {
      points,
      mode: "relative",
      period: "all",
      active: CHART_FIELDS.map((f) => f.key),
      fields: CHART_FIELDS.map((f) => ({ ...f, color: cssColor(f.cssVar) })),
      init() {
        const draw = () => this.draw();
        if (window.Chart) this.$nextTick(draw); else window.addEventListener("load", draw, { once: true });
        this.$watch("mode", draw);
        this.$watch("period", draw);
        this.$watch("active", draw);
      },
      isActive(key) { return this.active.includes(key); },
      toggle(key) {
        this.active = this.isActive(key) ? this.active.filter((k) => k !== key) : [...this.active, key];
      },
      get rows() {
        if (this.period === "all") return this.points;
        const cutoff = new Date();
        cutoff.setMonth(cutoff.getMonth() - Number(this.period));
        return this.points.filter((p) => new Date(`${p.date}T00:00:00`) >= cutoff);
      },
      get baselines() {
        const map = {};
        for (const f of CHART_FIELDS) map[f.key] = this.rows.map((r) => r[f.key]).find((v) => v != null) ?? null;
        return map;
      },
      value(row, key) {
        const raw = row[key];
        if (this.mode !== "relative") return raw ?? null;
        const base = this.baselines[key];
        return raw != null && base != null && base !== 0 ? Number(((raw / base) * 100).toFixed(2)) : null;
      },
      get activeFields() { return this.fields.filter((f) => this.isActive(f.key)); },
      get tiles() {
        return this.activeFields.flatMap((f) => {
          const first = this.baselines[f.key];
          const last = [...this.rows].reverse().map((r) => r[f.key]).find((v) => v != null) ?? null;
          if (first == null || last == null) return [];
          const delta = last - first;
          const pct = first !== 0 ? (delta / first) * 100 : 0;
          return [{ ...f, last, delta, pct, good: f.lowerIsBetter ? delta < 0 : delta > 0 }];
        });
      },
      get domain() {
        const values = this.rows.flatMap((r) => this.activeFields.map((f) => this.value(r, f.key))).filter((v) => v != null);
        if (values.length === 0) return [95, 105];
        const min = Math.min(...values);
        const max = Math.max(...values);
        const pad = Math.max((max - min) * 0.15, 2);
        return [Math.floor(min - pad), Math.ceil(max + pad)];
      },
      draw() {
        if (!window.Chart || this.rows.length === 0) return;
        const relative = this.mode === "relative";
        const border = cssColor("--border");
        const muted = cssColor("--muted-foreground");
        const config = {
          type: "line",
          data: {
            labels: this.rows.map((r) => r.label),
            datasets: this.activeFields.map((f) => ({
              label: f.label,
              data: this.rows.map((r) => this.value(r, f.key)),
              borderColor: f.color,
              backgroundColor: f.color,
              borderWidth: 2.5,
              pointRadius: 3,
              pointBorderWidth: 2,
              pointBackgroundColor: cssColor("--background"),
              pointHoverRadius: 5,
              cubicInterpolationMode: "monotone",
              spanGaps: true,
            })),
          },
          options: {
            responsive: true,
            maintainAspectRatio: false,
            layout: { padding: { top: 5, right: 10 } },
            interaction: { mode: "index", intersect: false },
            scales: {
              x: {
                grid: { color: border, tickBorderDash: [3, 3] },
                border: { color: border, dash: [3, 3] },
                ticks: { color: muted, font: { size: 11 } },
              },
              y: {
                min: relative ? this.domain[0] : undefined,
                max: relative ? this.domain[1] : undefined,
                grid: { color: border },
                border: { display: false, dash: [3, 3] },
                // Recharts vise environ 5 graduations entières.
                ticks: {
                  color: muted,
                  font: { size: 11 },
                  stepSize: relative ? Math.max(1, Math.ceil((this.domain[1] - this.domain[0]) / 4)) : undefined,
                  maxTicksLimit: relative ? undefined : 5,
                  callback: (v) => (relative ? `${v}%` : String(v)),
                },
              },
            },
            plugins: {
              legend: { position: "bottom", labels: { color: cssColor("--foreground"), font: { size: 12 }, usePointStyle: true, pointStyle: "line" } },
              tooltip: {
                callbacks: {
                  label: (ctx) => {
                    const field = this.activeFields[ctx.datasetIndex];
                    const raw = this.rows[ctx.dataIndex][field.key];
                    return relative && raw != null
                      ? `${field.label} : ${ctx.parsed.y}% (${raw} ${field.unit})`
                      : `${field.label} : ${ctx.parsed.y}`;
                  },
                },
              },
            },
          },
          plugins: [{
            id: "referenceLine",
            afterDatasetsDraw: (c) => {
              if (!relative) return;
              const y = c.scales.y.getPixelForValue(100);
              const { left, right } = c.chartArea;
              c.ctx.save();
              c.ctx.setLineDash([4, 4]);
              c.ctx.globalAlpha = 0.5;
              c.ctx.strokeStyle = muted;
              c.ctx.beginPath();
              c.ctx.moveTo(left, y);
              c.ctx.lineTo(right, y);
              c.ctx.stroke();
              c.ctx.restore();
            },
          }],
        };
        if (chart) chart.destroy();
        chart = new window.Chart(this.$refs.canvas, config);
      },
    };
  });

  // Bibliothèque d'exercices : recherche, onglets (tous / mes / favoris) et filtres combinés, côté navigateur.
  Alpine.data("exerciseFilters", () => ({
    search: "",
    tab: "all",
    type: "all",
    filters: { muscles: [], equipment: [], objectives: [] },
    filtersOpen: false,
    visible: 0,
    init() {
      ["search", "tab", "type", "filters"].forEach((key) => this.$watch(key, () => this.refresh(), { deep: true }));
    },
    has(key, value) { return this.filters[key].includes(value); },
    toggleIn(key, value) {
      this.filters[key] = this.has(key, value) ? this.filters[key].filter((v) => v !== value) : [...this.filters[key], value];
    },
    selectedOf(el) { return this.filters[el.closest("[data-key]").dataset.key]; },
    get activeFilterCount() {
      return ["muscles", "equipment", "objectives"].filter((k) => this.filters[k].length).length + (this.type !== "all" ? 1 : 0);
    },
    get hasFilters() { return Boolean(this.search) || this.activeFilterCount > 0; },
    clear() {
      this.search = "";
      this.type = "all";
      this.filters = { muscles: [], equipment: [], objectives: [] };
    },
    matches(el) {
      const d = el.dataset;
      if (this.tab === "favorites" && d.favorite !== "true") return false;
      if (this.tab === "mine" && d.mine !== "true") return false;
      if (this.search && !d.title.includes(this.search.toLowerCase())) return false;
      if (this.type !== "all" && d.type !== this.type) return false;
      const any = (key, values) => values.length === 0 || values.some((v) => JSON.parse(d[key]).includes(v));
      return any("muscles", this.filters.muscles) && any("equipment", this.filters.equipment)
        && any("objectives", this.filters.objectives);
    },
    refresh() {
      this.$nextTick(() => {
        this.visible = [...document.querySelectorAll("[data-exercise]")].filter((el) => this.matches(el)).length;
      });
    },
  }));

  // Saisie de tags avec suggestions (bibliothèque d'exercices) : Entrée ou virgule valide, Retour arrière retire le dernier.
  Alpine.data("chipsInput", (initial = [], suggestions = []) => ({
    values: [...initial],
    suggestions,
    draft: "",
    get remaining() { return this.suggestions.filter((s) => !this.values.includes(s)).slice(0, 12); },
    commit(raw) {
      const tag = raw.trim();
      if (!tag || this.values.includes(tag)) return;
      this.values.push(tag);
      this.draft = "";
    },
    remove(value) { this.values = this.values.filter((v) => v !== value); },
    backspace() { if (!this.draft && this.values.length) this.values.pop(); },
  }));

  // Attributs par défaut d'un exercice (5 maximum) ; les types et leurs valeurs par défaut viennent du serveur.
  Alpine.data("attributesEditor", (initial = [], types = []) => ({
    attrs: initial,
    types,
    pickerOpen: false,
    pickerType: "reps",
    meta(code) { return this.types.find((t) => t.code === code); },
    add() {
      if (this.attrs.length >= 5) return;
      const t = this.meta(this.pickerType);
      const attr = { type: t.code, default: t.defaultValue ?? null };
      if (["weight", "time", "rest"].includes(t.code)) attr.unit = t.unit;
      this.attrs.push(attr);
      this.pickerOpen = false;
    },
    remove(idx) { this.attrs.splice(idx, 1); },
  }));

  // Saisie de tags (TagInput de l'original) : Entrée ou « + » ajoute, la croix retire.
  Alpine.data("tagInput", (initial = []) => ({
    tags: [...initial],
    draft: "",
    add() {
      const value = this.draft.trim();
      if (value && !this.tags.includes(value)) this.tags.push(value);
      this.draft = "";
    },
    remove(tag) { this.tags = this.tags.filter((t) => t !== tag); },
  }));
});

// Les dialogs natifs (<dialog>) se ferment au clic sur le fond, comme ceux de Radix.
document.addEventListener("click", (event) => {
  const dialog = event.target;
  if (dialog instanceof HTMLDialogElement && dialog.open) dialog.close();
});

// Fermeture d'un dialog demandée par le serveur (HX-Trigger {"dialog-close": "<id>"}) : formulaire remis à zéro.
document.addEventListener("dialog-close", (event) => {
  const dialog = document.getElementById(event.detail.value);
  if (!(dialog instanceof HTMLDialogElement)) return;
  dialog.close();
  dialog.querySelectorAll("form").forEach((form) => form.reset());
});
