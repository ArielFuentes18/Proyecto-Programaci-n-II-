const db = require("../models");
const Localidad = db.localidades;

exports.create = async (req, res) => {
  const { nombre } = req.body;

  if (!nombre) {
    return res.status(400).send({
      message: "El campo 'nombre' es obligatorio."
    });
  }

  try {
    const localidad = { nombre };
    const data = await Localidad.create(localidad);
    res.status(201).send(data);
  } catch (err) {
    res.status(500).send({
      message: err.message || "Error al crear la localidad."
    });
  }
};

exports.findAll = (req, res) => {
  Localidad.findAll()
    .then(data => res.send(data))
    .catch(err => {
      res.status(500).send({
        message: err.message || "Error al obtener las localidades."
      });
    });
};

exports.findOne = (req, res) => {
  const id = req.params.id;

  Localidad.findByPk(id)
    .then(data => {
      if (data) {
        res.send(data);
      } else {
        res.status(404).send({
          message: `No se encontró la localidad con id=${id}.`
        });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al obtener la localidad con id=" + id
      });
    });
};

const { actualizar, eliminar } = require("../utils/crud");
exports.update = actualizar(Localidad, "id_localidad", "Localidad");
exports.delete = eliminar(Localidad, "id_localidad", "Localidad");
