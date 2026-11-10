// Copie les bibliothèques JS et les icônes Lucide dans target/classes, sans bundler :
// l'app n'a pas de JavaScript métier, seulement HTMX, Alpine et Chart.js.
import { cpSync, mkdirSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const root = resolve(dirname(fileURLToPath(import.meta.url)), "../../..");
const out = (p) => resolve(root, "target/classes", p);
const nm = (p) => resolve(root, "node_modules", p);

const files = [
  ["htmx.org/dist/htmx.min.js", "static/vendor/htmx.min.js"],
  ["alpinejs/dist/cdn.min.js", "static/vendor/alpine.min.js"],
  ["chart.js/dist/chart.umd.min.js", "static/vendor/chart.umd.min.js"],
];

for (const [from, to] of files) {
  mkdirSync(dirname(out(to)), { recursive: true });
  cpSync(nm(from), out(to));
}

// Icônes : rendues côté serveur (IconRegistry), donc hors du dossier public.
cpSync(nm("lucide-static/icons"), out("icons/lucide"), { recursive: true });
console.log("vendor: copie terminée");
