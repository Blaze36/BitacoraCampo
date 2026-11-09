// API de la Bitácora de campo (sin dependencias: solo Node.js).
// Ejecutar:  node server.js   ->   http://localhost:3000/api/registros
const http = require('http');
const fs = require('fs');
const path = require('path');

const PUERTO = 3000;
const ARCHIVO = path.join(__dirname, 'datos.json');
const MAX_CUERPO = 10 * 1024 * 1024; // 10 MB
const TITULO_MIN = 3, TITULO_MAX = 100, DESC_MAX = 500;

let registros = [];
let siguienteId = 1;
try {
  registros = JSON.parse(fs.readFileSync(ARCHIVO, 'utf8'));
  siguienteId = registros.reduce((m, r) => Math.max(m, r.id), 0) + 1;
} catch (e) { /* primera vez: archivo no existe */ }

function guardar() {
  fs.writeFileSync(ARCHIVO, JSON.stringify(registros, null, 2));
}

function responder(res, codigo, cuerpo) {
  const texto = cuerpo === undefined ? '' : JSON.stringify(cuerpo);
  res.writeHead(codigo, { 'Content-Type': 'application/json; charset=utf-8' });
  res.end(texto);
}

function leerCuerpo(req) {
  return new Promise((resolve, reject) => {
    let tamano = 0;
    const partes = [];
    req.on('data', (p) => {
      tamano += p.length;
      if (tamano > MAX_CUERPO) { reject({ codigo: 413, mensaje: 'La imagen es demasiado grande (máx. 10 MB).' }); req.destroy(); return; }
      partes.push(p);
    });
    req.on('end', () => {
      try { resolve(JSON.parse(Buffer.concat(partes).toString('utf8') || '{}')); }
      catch (e) { reject({ codigo: 400, mensaje: 'JSON inválido.' }); }
    });
    req.on('error', () => reject({ codigo: 400, mensaje: 'Error al leer la solicitud.' }));
  });
}

function validar(d) {
  const titulo = (d.titulo || '').trim();
  const descripcion = (d.descripcion || '').trim();
  if (!titulo) return 'El título es obligatorio.';
  if (titulo.length < TITULO_MIN) return `El título debe tener al menos ${TITULO_MIN} caracteres.`;
  if (titulo.length > TITULO_MAX) return `El título no puede superar ${TITULO_MAX} caracteres.`;
  if (!descripcion) return 'La descripción es obligatoria.';
  if (descripcion.length > DESC_MAX) return `La descripción no puede superar ${DESC_MAX} caracteres.`;
  if (!d.fotoBase64 || typeof d.fotoBase64 !== 'string') return 'La fotografía es obligatoria.';
  return null;
}

// Página para ver los registros en el navegador: http://localhost:3000/
const PAGINA = `<!doctype html>
<html lang="es"><head><meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Bitácora de campo</title>
<style>
  body{font-family:system-ui,sans-serif;background:#f4f5f7;margin:0;color:#222}
  header{background:#212121;color:#fff;padding:14px 20px;display:flex;justify-content:space-between;align-items:center}
  button{background:#00bfa5;border:0;color:#fff;padding:8px 14px;border-radius:6px;cursor:pointer;font-size:14px}
  main{max-width:900px;margin:20px auto;padding:0 16px;display:grid;gap:16px;grid-template-columns:repeat(auto-fill,minmax(260px,1fr))}
  .card{background:#fff;border-radius:10px;overflow:hidden;box-shadow:0 1px 4px rgba(0,0,0,.15)}
  .card img{width:100%;height:180px;object-fit:cover;background:#ddd;display:block}
  .card div{padding:12px}
  .card h3{margin:0 0 4px}
  .card small{color:#777}
  .vacio{grid-column:1/-1;text-align:center;color:#777;padding:40px}
</style></head><body>
<header><strong>Bitácora de campo — registros en el servidor</strong><button onclick="cargar()">Actualizar</button></header>
<main id="lista"></main>
<script>
async function cargar(){
  const lista=document.getElementById('lista'); lista.innerHTML='';
  const datos=await (await fetch('/api/registros')).json();
  if(!datos.length){lista.innerHTML='<div class="vacio">Aún no hay registros.</div>';return;}
  datos.slice().reverse().forEach(r=>{
    const c=document.createElement('div'); c.className='card';
    const img=document.createElement('img'); img.src='data:image/jpeg;base64,'+r.fotoBase64;
    const d=document.createElement('div');
    const h=document.createElement('h3'); h.textContent='#'+r.id+' '+r.titulo;
    const f=document.createElement('small'); f.textContent=new Date(r.fecha).toLocaleString();
    const p=document.createElement('p'); p.textContent=r.descripcion;
    d.append(h,f,p); c.append(img,d); lista.append(c);
  });
}
cargar();
</script></body></html>`;

const servidor = http.createServer(async (req, res) => {
  const ruta = req.url.split('?')[0].replace(/\/+$/, '');
  const m = ruta.match(/^\/api\/registros(?:\/(\d+))?$/);
  console.log(`${new Date().toISOString()}  ${req.method} ${req.url}`);

  if (req.method === 'GET' && (ruta === '' || ruta === '/ver')) {
    res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    return res.end(PAGINA);
  }

  if (!m) return responder(res, 404, { error: 'Ruta no encontrada.' });
  const id = m[1] ? parseInt(m[1], 10) : null;

  try {
    if (req.method === 'GET' && id === null) {
      return responder(res, 200, registros);
    }
    if (req.method === 'GET') {
      const r = registros.find((x) => x.id === id);
      return r ? responder(res, 200, r) : responder(res, 404, { error: 'Registro no encontrado.' });
    }
    if (req.method === 'POST' && id === null) {
      const d = await leerCuerpo(req);
      const error = validar(d);
      if (error) return responder(res, 400, { error });
      const nuevo = {
        id: siguienteId++,
        titulo: d.titulo.trim(),
        descripcion: d.descripcion.trim(),
        fotoBase64: d.fotoBase64,
        fecha: new Date().toISOString(),
      };
      registros.push(nuevo);
      guardar();
      return responder(res, 201, nuevo);
    }
    if (req.method === 'PUT' && id !== null) {
      const r = registros.find((x) => x.id === id);
      if (!r) return responder(res, 404, { error: 'Registro no encontrado.' });
      const d = await leerCuerpo(req);
      const error = validar(d);
      if (error) return responder(res, 400, { error });
      r.titulo = d.titulo.trim();
      r.descripcion = d.descripcion.trim();
      r.fotoBase64 = d.fotoBase64;
      guardar();
      return responder(res, 200, r);
    }
    if (req.method === 'DELETE' && id !== null) {
      const i = registros.findIndex((x) => x.id === id);
      if (i === -1) return responder(res, 404, { error: 'Registro no encontrado.' });
      registros.splice(i, 1);
      guardar();
      return responder(res, 204);
    }
    return responder(res, 405, { error: 'Método no permitido.' });
  } catch (e) {
    if (e && e.codigo) return responder(res, e.codigo, { error: e.mensaje });
    console.error(e);
    return responder(res, 500, { error: 'Error interno del servidor.' });
  }
});

servidor.listen(PUERTO, '0.0.0.0', () => {
  console.log(`API lista en http://localhost:${PUERTO}/api/registros`);
});