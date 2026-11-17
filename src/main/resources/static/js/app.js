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
  // WeeklyMealGrid : opérations partagées par l'éditeur de menu type et l'éditeur de plan élève.
  const MEAL_COLORS = ["breakfast", "lunch", "dinner", "snack", "dessert"];
  const COVERS = ["teal", "purple", "coral", "amber", "blue", "pink"];
  const plannedMeal = (recipe) => {
    const meal = { recipe_id: recipe.id, recipe_title: recipe.title, meal_type: "breakfast", servings: 1 };
    ["calories", "protein_g", "carbs_g", "fat_g"].forEach((k) => { if (recipe[k] != null) meal[k] = Number(recipe[k]); });
    return meal;
  };
  const mealGrid = (daysOf) => ({
    gridMeals(day) { return daysOf.call(this)[day] ?? []; },
    gridAdd(day, recipe) { const days = daysOf.call(this); days[day] = [...(days[day] ?? []), plannedMeal(recipe)]; },
    gridRemove(day, idx) { const days = daysOf.call(this); days[day] = (days[day] ?? []).filter((_, i) => i !== idx); },
    gridType(day, idx, type) {
      const days = daysOf.call(this);
      days[day] = (days[day] ?? []).map((m, i) => (i === idx ? { ...m, meal_type: type } : m));
    },
    mealStyle(type) {
      const key = MEAL_COLORS.includes(type) ? type : "other";
      return `background: var(--meal-${key}); color: var(--meal-${key}-fg)`;
    },
  });
  const emptyWeek = (n) => ({
    week_number: n,
    week_comment: null,
    days: { monday: [], tuesday: [], wednesday: [], thursday: [], friday: [], saturday: [], sunday: [] },
  });

  // MealPlanTemplateEditor : paramètres, semaines et grille de repas.
  Alpine.data("templateEditor", (form, recipes) => ({
    form,
    recipes,
    activeWeek: 0,
    ...mealGrid(function days() { return this.week.days; }),
    get week() { return this.form.weeks[this.activeWeek] ?? this.form.weeks[0]; },
    get kcalTarget() { return this.form.kcal_target; },
    coverStyle(key) { return `background: var(--cover-${COVERS.includes(key) ? key : "teal"}-bg)`; },
    macro(key) { return this.form.macros_target?.[key] ?? ""; },
    setMacro(key, value) { this.form.macros_target = { ...this.form.macros_target, [key]: value ? Number(value) : undefined }; },
    addWeek() {
      const next = this.form.weeks.length;
      this.form.weeks = [...this.form.weeks, emptyWeek(next + 1)];
      this.form.duration_weeks = next + 1;
      this.activeWeek = next;
    },
    duplicateWeek(i) {
      const src = this.form.weeks[i];
      const days = Object.fromEntries(Object.entries(src.days).map(([k, v]) => [k, [...v]]));
      this.form.weeks = [...this.form.weeks, { week_number: this.form.weeks.length + 1, week_comment: src.week_comment, days }];
      this.form.duration_weeks = this.form.weeks.length;
    },
    removeWeek(i) {
      if (this.form.weeks.length === 1) return;
      const count = this.form.weeks.length;
      this.form.weeks = this.form.weeks.filter((_, idx) => idx !== i).map((w, idx) => ({ ...w, week_number: idx + 1 }));
      this.form.duration_weeks = this.form.weeks.length;
      this.activeWeek = Math.max(0, Math.min(this.activeWeek, count - 2));
    },
  }));

  // Requête JSON hors htmx : jeton CSRF du <body>, puis HX-Trigger (toasts) et HX-Redirect rejoués comme htmx.
  Alpine.magic("postJson", (el) => async (url, body) => {
    const headers = {
      "Content-Type": "application/json", "HX-Request": "true",
      ...JSON.parse(document.body.getAttribute("hx-headers") || "{}"),
    };
    const response = await fetch(url, { method: "POST", headers, body: JSON.stringify(body) });
    const trigger = response.headers.get("HX-Trigger");
    if (trigger) {
      Object.entries(JSON.parse(trigger)).forEach(([name, detail]) => el.dispatchEvent(new CustomEvent(name, { detail, bubbles: true })));
    }
    const redirect = response.headers.get("HX-Redirect");
    if (redirect) {
      window.location.href = redirect;
      return null;
    }
    return response.ok && response.status !== 204 ? response.json() : null;
  });

  // MealPlanGenerator : étapes, configuration (pré-remplie depuis l'élève), repas et liste de courses.
  const MEAL_TYPE_LABELS = { breakfast: "Petit-déjeuner", lunch: "Déjeuner", dinner: "Dîner", snack: "Collation", dessert: "Dessert" };
  Alpine.data("mealPlanWizard", (members, recipes, preset) => ({
    members,
    recipes,
    steps: [
      { key: "member", label: "Élève" }, { key: "config", label: "Config" }, { key: "generate", label: "Plan" },
      { key: "shopping", label: "Courses" }, { key: "send", label: "Envoi" },
    ],
    step: "member",
    memberId: null,
    title: "Plan repas hebdomadaire",
    config: {
      kcalTarget: 2000, proteinG: 140, carbsG: 200, fatG: 70, mealsPerDay: 3, freeMealSlots: 0,
      includeDesserts: false, hideMacros: false, dietaryTags: [], excludedAllergens: [], availableEquipment: [],
    },
    meals: {},
    shoppingList: [],
    sending: false,
    init() { if (preset) this.chooseMember(preset); },
    get stepIndex() { return this.steps.findIndex((s) => s.key === this.step); },
    get member() { return this.members.find((m) => m.id === this.memberId); },
    get memberLabel() { return this.member ? this.member.label.trim() : "—"; },
    get hasMeals() { return Object.keys(this.meals).length > 0; },
    get shoppingEmpty() { return this.shoppingList.length === 0 || this.shoppingList[0]?.items.length === 0; },
    get totalMeals() { return Object.values(this.meals).reduce((acc, d) => acc + d.length, 0); },
    chooseMember(id) {
      this.memberId = id;
      const m = this.member;
      if (!m) return;
      this.config = { ...this.config, ...m.configPatch };
      this.title = m.planTitle;
    },
    togglePill(key, value) {
      const list = this.config[key];
      this.config[key] = list.includes(value) ? list.filter((v) => v !== value) : [...list, value];
    },
    dayMeals(day) { return this.meals[day] ?? []; },
    mealTypeLabel(type) { return MEAL_TYPE_LABELS[type] ?? type; },
    totalsLabel(day) {
      const t = this.dayMeals(day).reduce((a, m) => ({
        kcal: a.kcal + (m.calories ?? 0), p: a.p + (m.protein_g ?? 0), c: a.c + (m.carbs_g ?? 0), f: a.f + (m.fat_g ?? 0),
      }), { kcal: 0, p: 0, c: 0, f: 0 });
      return `${t.kcal} kcal · P ${t.p}g · G ${t.c}g · L ${t.f}g`;
    },
    async generated() {
      const result = await this.$postJson("/app/coach/nutrition/plans/generate/run", this.config);
      return result?.meals ?? null;
    },
    async refreshShopping() {
      this.shoppingList = await this.$postJson("/app/coach/nutrition/plans/generate/shopping", { ...this.config, meals: this.meals }) ?? [];
    },
    async generate() {
      const meals = await this.generated();
      if (!meals) return;
      this.meals = meals;
      await this.refreshShopping();
      this.$dispatch("toast", { message: "Plan généré", category: "success" });
    },
    async regenerateDay(day) {
      const meals = await this.generated();
      if (!meals) return;
      this.meals = { ...this.meals, [day]: meals[day] };
      await this.refreshShopping();
    },
    async swapMeal(day, idx, recipeId) {
      const recipe = this.recipes.find((r) => r.id === recipeId);
      if (!recipe) return;
      const slot = this.dayMeals(day)[idx];
      const updated = [...this.dayMeals(day)];
      updated[idx] = { ...plannedMeal(recipe), meal_type: slot.meal_type };
      this.meals = { ...this.meals, [day]: updated };
      await this.refreshShopping();
    },
    goNext() {
      if (this.step === "member" && !this.memberId) {
        this.$dispatch("toast", { message: "Sélectionnez un élève", category: "error" });
        return;
      }
      if (this.step === "config") {
        this.step = "generate";
        setTimeout(() => this.generate(), 50);
        return;
      }
      this.step = this.steps[Math.min(this.steps.length - 1, this.stepIndex + 1)].key;
    },
    goPrev() { this.step = this.steps[Math.max(0, this.stepIndex - 1)].key; },
    async send() {
      if (!this.memberId) return;
      if (!this.hasMeals) {
        this.$dispatch("toast", { message: "Générez le plan avant de l'envoyer", category: "error" });
        return;
      }
      this.sending = true;
      await this.$postJson("/app/coach/nutrition/plans", {
        memberId: this.memberId, title: this.title, meals: this.meals, shoppingList: this.shoppingList,
        kcalTarget: this.config.kcalTarget,
        macrosTarget: { protein_g: this.config.proteinG, carbs_g: this.config.carbsG, fat_g: this.config.fatG },
        hideMacros: this.config.hideMacros,
      });
      this.sending = false;
    },
  }));

  // NutritionEditor : aliments par jour et par repas, import des ingrédients d'une recette.
  Alpine.data("nutritionEditor", (state, recipes, memberId) => ({
    planId: state.planId,
    title: state.title,
    description: state.description,
    meals: state.meals,
    recipes,
    recipeSearch: "",
    pickerTarget: null,
    pickerOpen: false,
    saving: false,
    get filteredRecipes() {
      const q = this.recipeSearch.toLowerCase().trim();
      return q ? this.recipes.filter((r) => r.title.toLowerCase().includes(q)) : this.recipes;
    },
    // Un repas en texte libre faisait planter l'éditeur d'origine : il est affiché vide (« Libre »).
    items(day, meal) { const v = this.meals[day]?.[meal]; return Array.isArray(v) ? v : []; },
    setItems(day, meal, items) { this.meals = { ...this.meals, [day]: { ...this.meals[day], [meal]: items } }; },
    addItem(day, meal) { this.setItems(day, meal, [...this.items(day, meal), { name: "", quantity: "", unit: "g" }]); },
    updateItem(day, meal, idx, patch) {
      this.setItems(day, meal, this.items(day, meal).map((it, i) => (i === idx ? { ...it, ...patch } : it)));
    },
    removeItem(day, meal, idx) { this.setItems(day, meal, this.items(day, meal).filter((_, i) => i !== idx)); },
    openPicker(day, meal) {
      this.pickerTarget = { day, meal };
      this.pickerOpen = true;
      this.$refs.picker.showModal();
    },
    copyRecipe(recipe) {
      if (!this.pickerTarget) return;
      const { day, meal } = this.pickerTarget;
      const added = recipe.ingredients.map((ing) => ({ name: ing.name, quantity: ing.quantity, unit: ing.unit, fromRecipe: recipe.title }));
      this.setItems(day, meal, [...this.items(day, meal), ...added]);
      this.$refs.picker.close();
    },
    async save() {
      this.saving = true;
      const result = await this.$postJson(`/app/coach/members/${memberId}/nutrition`, {
        planId: this.planId, title: this.title, description: this.description || null, meals: this.meals,
      });
      if (result?.id) this.planId = result.id;
      this.saving = false;
    },
  }));

  // RecipeLibrary : onglet, recherche et filtres (l'allergène exclut les recettes qui le contiennent).
  Alpine.data("recipeFilters", () => ({
    tab: "all",
    search: "",
    category: "all",
    dietary: "all",
    allergen: "all",
    equipment: "all",
    mealType: "all",
    filtersOpen: false,
    visible: 0,
    init() {
      ["tab", "search", "category", "dietary", "allergen", "equipment", "mealType"]
        .forEach((key) => this.$watch(key, () => this.refresh()));
    },
    get activeAdvanced() {
      return ["dietary", "allergen", "equipment", "mealType"].filter((k) => this[k] !== "all").length;
    },
    get hasAny() { return Boolean(this.search) || this.category !== "all" || this.activeAdvanced > 0; },
    reset() {
      this.search = "";
      ["category", "dietary", "allergen", "equipment", "mealType"].forEach((k) => { this[k] = "all"; });
    },
    matches(el) {
      const d = el.dataset;
      const list = (key) => JSON.parse(d[key]);
      if (this.tab === "mine" && d.mine !== "true") return false;
      if (this.category !== "all" && d.category !== this.category) return false;
      if (this.dietary !== "all" && !list("dietary").includes(this.dietary)) return false;
      if (this.allergen !== "all" && list("allergens").includes(this.allergen)) return false;
      if (this.equipment !== "all" && !list("equipment").includes(this.equipment)) return false;
      if (this.mealType !== "all" && !list("mealTypes").includes(this.mealType)) return false;
      const q = this.search.toLowerCase().trim();
      return !q || d.search.includes(q);
    },
    refresh() {
      this.$nextTick(() => {
        this.visible = [...document.querySelectorAll("[data-recipe]")].filter((el) => this.matches(el)).length;
      });
    },
  }));

  // RecipeFormDialog : état complet du formulaire, envoyé en JSON au serveur.
  Alpine.data("recipeForm", () => ({
    form: {},
    init() { this.form = JSON.parse(this.$el.dataset.state); },
    has(key, value) { return this.form[key].includes(value); },
    toggle(key, value) {
      this.form[key] = this.has(key, value) ? this.form[key].filter((v) => v !== value) : [...this.form[key], value];
    },
    addIngredient() { this.form.ingredients.push({ name: "", quantity: "", unit: "g" }); },
  }));

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

  // Éditeur de séance par blocs (SessionEditor) : même logique d'état que l'original (ajout, conversion, déplacement).
  const BLOCK_TYPES = [
    { code: "single", label: "Exercice seul", slots: 1 },
    { code: "superset", label: "Superset", slots: 2 },
    { code: "triset", label: "Triset", slots: 3 },
    { code: "circuit", label: "Circuit", slots: 4 },
  ];
  let nextKey = 0;
  const emptyItem = () => ({ key: nextKey++, exercise_id: "", attributes: [], sets: 3, exercise_note: null });
  Alpine.data("sessionEditor", (state, exercises = [], types = []) => ({
    form: {
      ...state,
      blocks: state.blocks.map((b) => ({ ...b, key: nextKey++, exercises: b.exercises.map((e) => ({ ...e, key: nextKey++ })) })),
    },
    exercises,
    types,
    blockTypes: BLOCK_TYPES,
    meta(code) { return this.types.find((t) => t.code === code); },
    exerciseTitle(id) { return this.exercises.find((e) => e.id === id)?.title; },
    matching(query) {
      const q = query.toLowerCase().trim();
      return q ? this.exercises.filter((e) => e.title.toLowerCase().includes(q)) : this.exercises;
    },
    attrValue(attr) {
      const m = this.meta(attr.type);
      if (!m) return "";
      const value = m.kind === "BOOLEAN" ? (attr.default ? "oui" : "non") : String(attr.default ?? "—");
      return m.unit ? `${value} ${m.unit}` : value;
    },
    addBlock(type) {
      const t = BLOCK_TYPES.find((b) => b.code === type);
      this.form.blocks.push({
        key: nextKey++, block_type: type, rounds: type === "single" ? 1 : 3, notes: null,
        exercises: Array.from({ length: t.slots }, emptyItem),
      });
    },
    moveBlock(idx, dir) {
      const target = idx + dir;
      if (target < 0 || target >= this.form.blocks.length) return;
      const blocks = this.form.blocks;
      [blocks[idx], blocks[target]] = [blocks[target], blocks[idx]];
    },
    removeBlock(idx) { this.form.blocks.splice(idx, 1); },
    convertBlock(idx, type) {
      const block = this.form.blocks[idx];
      const slots = BLOCK_TYPES.find((b) => b.code === type).slots;
      const items = block.exercises.slice(0, slots);
      while (items.length < slots) items.push(emptyItem());
      block.block_type = type;
      block.rounds = type === "single" ? 1 : Math.max(block.rounds, 2);
      block.exercises = items;
    },
    addExercise(bi) { this.form.blocks[bi].exercises.push(emptyItem()); },
    removeExercise(bi, ei) {
      const block = this.form.blocks[bi];
      block.exercises.splice(ei, 1);
      if (block.exercises.length === 0) this.removeBlock(bi);
    },
    selectExercise(item, exercise) {
      item.exercise_id = exercise.id;
      item.attributes = (exercise.attributes ?? []).slice(0, 5).map((a) => ({ ...a }));
    },
    addAttr(item, type) {
      if (item.attributes.length >= 5) return;
      const t = this.meta(type);
      const attr = { type, default: t.defaultValue ?? null };
      if (["weight", "time", "rest"].includes(type)) attr.unit = t.unit;
      item.attributes.push(attr);
    },
  }));

  // Éditeur de planification (ProgramEditor) : semaines de 7 jours, mois de 4 semaines repliables.
  const WEEK_COLORS = [
    "bg-primary/15 text-primary border-primary/30",
    "bg-emerald-500/15 text-emerald-700 border-emerald-500/30",
    "bg-amber-500/15 text-amber-700 border-amber-500/30",
    "bg-sky-500/15 text-sky-700 border-sky-500/30",
    "bg-fuchsia-500/15 text-fuchsia-700 border-fuchsia-500/30",
    "bg-rose-500/15 text-rose-700 border-rose-500/30",
  ];
  const emptyDay = (n) => ({ day_of_week: n, day_type: "rest", day_comment: null, workout_session_ids: [] });
  const newWeek = () => ({ key: nextKey++, week_comment: null, days: Array.from({ length: 7 }, (_, i) => emptyDay(i + 1)) });
  const cloneWeek = (w) => ({ key: nextKey++, week_comment: w.week_comment, days: w.days.map((d) => ({ ...d, workout_session_ids: [...d.workout_session_ids] })) });
  Alpine.data("programEditor", (state, members = [], sessions = [], objectives = []) => ({
    form: { ...state, weeks: state.weeks.map((w) => ({ ...w, key: nextKey++ })) },
    targetMode: state.assignee_id ? "member" : "objectives",
    members,
    sessions,
    objectives,
    openMonths: { 0: true },
    dayLabels: ["Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"],
    get payload() {
      return {
        ...this.form,
        assignee_id: this.targetMode === "member" ? this.form.assignee_id : null,
        objectives: this.targetMode === "objectives" ? this.form.objectives : [],
      };
    },
    get targetLabel() {
      if (this.targetMode === "member") {
        return this.members.find((m) => m.id === this.form.assignee_id)?.label ?? "Élève à choisir";
      }
      return this.form.objectives.length ? this.form.objectives.join(", ") : "Aucun groupe";
    },
    get monthGroups() {
      const groups = [];
      for (let i = 0; i < this.form.weeks.length; i += 4) groups.push({ startIdx: i, weeks: this.form.weeks.slice(i, i + 4) });
      return groups;
    },
    weekColor(i) { return WEEK_COLORS[i % WEEK_COLORS.length]; },
    isMonthOpen(startIdx) { return Boolean(this.openMonths[startIdx / 4]); },
    toggleMonth(startIdx) { this.openMonths[startIdx / 4] = !this.isMonthOpen(startIdx); },
    toggleObjective(o) {
      this.form.objectives = this.form.objectives.includes(o) ? this.form.objectives.filter((v) => v !== o) : [...this.form.objectives, o];
    },
    sessionTitle(id) { return this.sessions.find((s) => s.id === id)?.label; },
    matchingSessions(query) {
      const q = query.toLowerCase().trim();
      return q ? this.sessions.filter((s) => s.label.toLowerCase().includes(q)) : this.sessions;
    },
    moveWeek(wi, dir) {
      const target = wi + dir;
      if (target < 0 || target >= this.form.weeks.length) return;
      const weeks = this.form.weeks;
      [weeks[wi], weeks[target]] = [weeks[target], weeks[wi]];
    },
    removeWeek(wi) { this.form.weeks.splice(wi, 1); },
    duplicateWeek(wi) { this.form.weeks.splice(wi + 1, 0, cloneWeek(this.form.weeks[wi])); },
    addWeek() { this.form.weeks.push(newWeek()); },
    duplicateMonth(startIdx) {
      const block = this.form.weeks.slice(startIdx, startIdx + 4);
      this.form.weeks.splice(startIdx + block.length, 0, ...block.map(cloneWeek));
    },
    addMonth() { for (let i = 0; i < 4; i++) this.form.weeks.push(newWeek()); },
    hasReview(day) { return day.day_type === "review" || day.day_type === "mixed"; },
    isRest(day) { return day.day_type === "rest" || day.day_type === "review"; },
    setRest(day) { day.day_type = this.hasReview(day) ? "review" : "rest"; day.workout_session_ids = []; },
    setWorkout(day) { day.day_type = this.hasReview(day) ? "mixed" : "workout"; },
    toggleReview(day) {
      const review = !this.hasReview(day);
      day.day_type = this.isRest(day) ? (review ? "review" : "rest") : (review ? "mixed" : "workout");
    },
    addSession(day, id) { if (!day.workout_session_ids.includes(id)) day.workout_session_ids.push(id); },
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

// Radix (FocusScope) sélectionne le texte du premier champ focalisé à l'ouverture d'un dialog : même rendu ici.
// `data-no-select` : dialogs dont l'original remplissait les champs après l'ouverture (rien n'était sélectionné).
const nativeShowModal = HTMLDialogElement.prototype.showModal;
HTMLDialogElement.prototype.showModal = function showModal() {
  nativeShowModal.call(this);
  const focused = document.activeElement;
  if (focused instanceof HTMLInputElement && this.contains(focused) && !this.hasAttribute("data-no-select")) {
    focused.select();
  }
};
