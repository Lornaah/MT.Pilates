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
      if (initial && initial.message) this.add(initial);
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
