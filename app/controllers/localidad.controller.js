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

exports.update = (req, res) => {
  const id = req.params.id;

  Localidad.update(req.body, {
    where: { id_localidad: id }
  })
    .then(num => {
      if (num == 1) {
        res.send({ message: "Localidad actualizada correctamente." });
      } else {
        res.send({ message: `No se pudo actualizar la localidad con id=${id}.` });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al actualizar la localidad con id=" + id
      });
    });
};

exports.delete = (req, res) => {
  const id = req.params.id;

  Localidad.destroy({
    where: { id_localidad: id }
  })
    .then(num => {
      if (num == 1) {
        res.send({ message: "Localidad eliminada correctamente." });
      } else {
        res.send({ message: `No se pudo eliminar la localidad con id=${id}.` });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al eliminar la localidad con id=" + id
      });
    });
};