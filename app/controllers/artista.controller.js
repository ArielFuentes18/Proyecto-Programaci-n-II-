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

const { actualizar, eliminar } = require("../utils/crud");
exports.update = actualizar(Artista, "id_artista", "Artista");
exports.delete = eliminar(Artista, "id_artista", "Artista");
