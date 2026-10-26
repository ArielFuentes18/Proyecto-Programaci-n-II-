const db = require("../models");
const Concierto = db.conciertos;
const Artista = db.artistas;
const { urlImagen } = require("./artista.controller");

// El concierto trae al artista anidado: incluimos la foto y la convertimos a la URL que sirve el backend
const incluirArtista = {
  model: db.artistas,
  as: 'artista',
  attributes: ['id_artista', 'nombre_artistico', 'genero_musical', 'pais_origen', 'imagen_url']
};
const conFoto = (req, concierto) => {
  const j = concierto && typeof concierto.toJSON === "function" ? concierto.toJSON() : concierto;
  if (j && j.artista) j.artista = { ...j.artista, imagen_url: urlImagen(req, j.artista.imagen_url) };
  return j;
};

exports.create = async (req, res) => {
  const { id_artista, titulo_evento, fecha_concierto, recinto, estado } = req.body;

  if (!id_artista || !titulo_evento) {
    return res.status(400).send({
      message: "Los campos id_artista y titulo_evento son obligatorios."
    });
  }

  try {
    const artistaExiste = await Artista.findByPk(id_artista);

    if (!artistaExiste) {
      return res.status(404).send({
        message: `No existe ningún artista registrado con el ID ${id_artista}.`
      });
    }

    const concierto = {
      id_artista,
      titulo_evento,
      fecha_concierto: fecha_concierto || new Date(),
      recinto,
      estado: estado || "Programado"
    };

    const data = await Concierto.create(concierto);
    res.status(201).send(data);

  } catch (err) {
    res.status(400).send({
      message: err.message || "Error al registrar el concierto."
    });
  }
};

exports.findAll = (req, res) => {
  Concierto.findAll({
    include: [incluirArtista]
  })
    .then(data => res.send(data.map(c => conFoto(req, c))))
    .catch(err => {
      res.status(500).send({
        message: err.message || "Error al obtener los conciertos."
      });
    });
};

exports.findOne = (req, res) => {
  const id = req.params.id;

  Concierto.findByPk(id, {
    include: [incluirArtista]
  })
    .then(data => {
      if (data) {
        res.send(conFoto(req, data));
      } else {
        res.status(404).send({
          message: `No se encontró el concierto con id=${id}.`
        });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al obtener el concierto con id=" + id
      });
    });
};

const { actualizar, eliminar } = require("../utils/crud");
exports.update = actualizar(Concierto, "id_concierto", "Concierto");
exports.delete = eliminar(Concierto, "id_concierto", "Concierto");
