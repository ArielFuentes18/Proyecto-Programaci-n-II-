const db = require("../models");
const Artista = db.artistas;

// La foto se sirve a través del backend (/api/artistas/imagen/:archivo), así funciona aunque
// el bucket de Supabase sea privado. En la base se guarda cualquier URL; aquí solo importa el nombre del archivo.
// Si el bucket es público, pon BUCKET_PUBLICO=true en Render y las fotos se cargan directo desde Supabase (más rápido).
const supaBase = () => (process.env.SUPABASE_URL || "https://runmfjrhshpxzroyqohi.supabase.co").replace(/\/$/, "");
const supaBucket = () => process.env.SUPABASE_BUCKET || "artistas";
const urlArchivo = (req, archivo) =>
  process.env.BUCKET_PUBLICO === "true"
    ? `${supaBase()}/storage/v1/object/public/${supaBucket()}/${archivo}`
    : `${req.protocol}://${req.get("host")}/api/artistas/imagen/${archivo}`;
const urlImagen = (req, guardada) => {
  if (!guardada) return guardada;
  return urlArchivo(req, String(guardada).split("?")[0].split("/").pop());
};
// Lo reutiliza concierto.controller.js para que la tarjeta del concierto reciba la misma URL de foto
exports.urlImagen = urlImagen;
const conImagen = (req, artista) => {
  const j = artista && typeof artista.toJSON === "function" ? artista.toJSON() : artista;
  return { ...j, imagen_url: urlImagen(req, j.imagen_url) };
};

exports.create = async (req, res) => {
  const { nombre_artistico, genero_musical, pais_origen, imagen_url } = req.body;

  if (!nombre_artistico || !genero_musical) {
    return res.status(400).send({
      message: "Los campos nombre_artistico y genero_musical son obligatorios."
    });
  }

  try {
    const artista = {
      nombre_artistico,
      genero_musical,
      pais_origen,
      imagen_url
    };

    const data = await Artista.create(artista);
    res.status(201).send(conImagen(req, data));
  } catch (err) {
    res.status(500).send({
      message: err.message || "Error al registrar el artista."
    });
  }
};

exports.findAll = (req, res) => {
  Artista.findAll()
    .then(data => res.send(data.map(a => conImagen(req, a))))
    .catch(err => {
      res.status(500).send({
        message: err.message || "Error al obtener los artistas."
      });
    });
};

exports.findOne = (req, res) => {
  const id = req.params.id;

  Artista.findByPk(id)
    .then(data => {
      if (data) {
        res.send(conImagen(req, data));
      } else {
        res.status(404).send({
          message: `No se encontró el artista con id=${id}.`
        });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al obtener el artista con id=" + id
      });
    });
};

// GET /api/artistas/imagen/:archivo  (público) - entrega la foto desde Supabase Storage, aunque el bucket sea privado
exports.verImagen = async (req, res) => {
  try {
    const key = process.env.SUPABASE_SERVICE_KEY;
    if (!key) return res.status(500).send({ message: "Falta configurar SUPABASE_SERVICE_KEY en el servidor." });
    const archivo = req.params.archivo;
    if (!/^[A-Za-z0-9][A-Za-z0-9._-]{0,150}$/.test(archivo)) {
      return res.status(400).send({ message: "Nombre de archivo inválido." });
    }
    const base = (process.env.SUPABASE_URL || "https://runmfjrhshpxzroyqohi.supabase.co").replace(/\/$/, "");
    const bucket = process.env.SUPABASE_BUCKET || "artistas";
    const r = await fetch(`${base}/storage/v1/object/${bucket}/${archivo}`, {
      headers: { apikey: key, Authorization: `Bearer ${key}` }
    });
    if (!r.ok) return res.status(404).send({ message: "Imagen no encontrada." });
    res.set("Content-Type", r.headers.get("content-type") || "application/octet-stream");
    res.set("Cache-Control", "public, max-age=86400");
    res.send(Buffer.from(await r.arrayBuffer()));
  } catch (err) {
    res.status(500).send({ message: err.message || "Error al obtener la imagen." });
  }
};

// POST /api/artistas/imagen  (admin) - cuerpo = bytes de la imagen, Content-Type = image/jpeg|png|gif
// Sube el archivo al bucket de Supabase Storage con la clave de servicio, que NUNCA sale del servidor.
exports.subirImagen = async (req, res) => {
  try {
    const key = process.env.SUPABASE_SERVICE_KEY;
    if (!key) {
      return res.status(500).send({ message: "Falta configurar SUPABASE_SERVICE_KEY en el servidor." });
    }
    const tipo = String(req.headers["content-type"] || "").split(";")[0].trim();
    // WebP se excluye a propósito: JavaFX (Image/ImageView) no sabe decodificarlo y la foto se vería en blanco.
    const ext = { "image/jpeg": "jpg", "image/png": "png", "image/gif": "gif" }[tipo];
    if (!ext) {
      return res.status(400).send({ message: "Formato no permitido. Usa JPG, PNG o GIF." });
    }
    if (!Buffer.isBuffer(req.body) || req.body.length === 0) {
      return res.status(400).send({ message: "No se recibió ninguna imagen." });
    }
    const base = (process.env.SUPABASE_URL || "https://runmfjrhshpxzroyqohi.supabase.co").replace(/\/$/, "");
    const bucket = process.env.SUPABASE_BUCKET || "artistas";
    const ruta = `${require("crypto").randomUUID()}.${ext}`;

    const r = await fetch(`${base}/storage/v1/object/${bucket}/${ruta}`, {
      method: "POST",
      headers: { apikey: key, Authorization: `Bearer ${key}`, "Content-Type": tipo },
      body: req.body
    });
    if (!r.ok) {
      let detalle = "";
      try { detalle = (await r.json()).message || ""; } catch (e) { /* sin detalle */ }
      return res.status(502).send({ message: "No se pudo subir la imagen a Supabase" + (detalle ? ": " + detalle : ".") });
    }
    res.status(201).send({ url: urlArchivo(req, ruta) });
  } catch (err) {
    res.status(500).send({ message: err.message || "Error al subir la imagen." });
  }
};

const { actualizar, eliminar } = require("../utils/crud");
exports.update = actualizar(Artista, "id_artista", "Artista");
exports.delete = eliminar(Artista, "id_artista", "Artista");
