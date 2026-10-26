const db = require("../models");
const Artista = db.artistas;

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
    res.status(201).send(data);
  } catch (err) {
    res.status(500).send({
      message: err.message || "Error al registrar el artista."
    });
  }
};

exports.findAll = (req, res) => {
  Artista.findAll()
    .then(data => res.send(data))
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
        res.send(data);
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

// POST /api/artistas/imagen  (admin) - cuerpo = bytes de la imagen, Content-Type = image/jpeg|png|webp|gif
// Sube el archivo al bucket de Supabase Storage con la clave de servicio, que NUNCA sale del servidor.
exports.subirImagen = async (req, res) => {
  try {
    const key = process.env.SUPABASE_SERVICE_KEY;
    if (!key) {
      return res.status(500).send({ message: "Falta configurar SUPABASE_SERVICE_KEY en el servidor." });
    }
    const tipo = String(req.headers["content-type"] || "").split(";")[0].trim();
    const ext = { "image/jpeg": "jpg", "image/png": "png", "image/webp": "webp", "image/gif": "gif" }[tipo];
    if (!ext) {
      return res.status(400).send({ message: "Formato no permitido. Usa JPG, PNG, WebP o GIF." });
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
    res.status(201).send({ url: `${base}/storage/v1/object/public/${bucket}/${ruta}` });
  } catch (err) {
    res.status(500).send({ message: err.message || "Error al subir la imagen." });
  }
};

const { actualizar, eliminar } = require("../utils/crud");
exports.update = actualizar(Artista, "id_artista", "Artista");
exports.delete = eliminar(Artista, "id_artista", "Artista");
