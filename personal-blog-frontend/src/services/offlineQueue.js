const DB_NAME = 'blog-offline-queue';
const STORE = 'mutations';
const DB_VERSION = 1;

function openDb() {
  return new Promise((resolve, reject) => {
    const req = indexedDB.open(DB_NAME, DB_VERSION);
    req.onupgradeneeded = () => {
      const db = req.result;
      if (!db.objectStoreNames.contains(STORE)) {
        db.createObjectStore(STORE, { keyPath: 'id', autoIncrement: true });
      }
    };
    req.onsuccess = () => resolve(req.result);
    req.onerror = () => reject(req.error);
  });
}

async function enqueue(item) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite');
    tx.objectStore(STORE).add({ ...item, createdAt: Date.now() });
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

async function readAll() {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readonly');
    const req = tx.objectStore(STORE).getAll();
    req.onsuccess = () => resolve(req.result || []);
    req.onerror = () => reject(req.error);
  });
}

async function remove(id) {
  const db = await openDb();
  return new Promise((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite');
    tx.objectStore(STORE).delete(id);
    tx.oncomplete = () => resolve();
    tx.onerror = () => reject(tx.error);
  });
}

async function flushQueue() {
  if (!navigator.onLine) return;
  const items = await readAll();
  const request = (await import('~/utils/request')).default;
  for (const item of items.sort((a, b) => a.createdAt - b.createdAt)) {
    try {
      await request({
        url: item.url,
        method: item.method || 'post',
        data: item.data,
        headers: item.headers,
        idempotencyKey: item.idempotencyKey,
        skipErrorToast: true,
      });
      await remove(item.id);
    } catch {
      break;
    }
  }
}

export function queueMutation(config) {
  if (navigator.onLine) {
    return null;
  }
  return enqueue(config);
}

export function initOfflineQueue() {
  if (typeof window === 'undefined') return () => {};
  const onOnline = () => flushQueue().catch(() => {});
  window.addEventListener('online', onOnline);
  flushQueue().catch(() => {});
  return () => window.removeEventListener('online', onOnline);
}

export { flushQueue, enqueue };
