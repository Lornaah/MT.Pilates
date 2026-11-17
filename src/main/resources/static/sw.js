/*
 * Service worker minimal (application installable) : réseau d'abord, page hors ligne en secours pour la navigation.
 * Aucune donnée de l'application n'est mise en cache : seules la page hors ligne et ses ressources le sont.
 */
const CACHE = "mtpilates-offline-v1";
const OFFLINE_ASSETS = ["/offline.html", "/css/app.css", "/favicon.png", "/pwa-icon-192.png"];

self.addEventListener("install", (event) => {
  event.waitUntil(caches.open(CACHE).then((cache) => cache.addAll(OFFLINE_ASSETS)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (event) => {
  event.waitUntil(
    caches.keys()
      .then((keys) => Promise.all(keys.filter((key) => key !== CACHE).map((key) => caches.delete(key))))
      .then(() => self.clients.claim()),
  );
});

self.addEventListener("fetch", (event) => {
  const { request } = event;
  if (request.method !== "GET") return;
  if (request.mode === "navigate") {
    event.respondWith(fetch(request).catch(() => caches.match("/offline.html")));
    return;
  }
  if (OFFLINE_ASSETS.includes(new URL(request.url).pathname)) {
    event.respondWith(fetch(request).catch(() => caches.match(request)));
  }
});
