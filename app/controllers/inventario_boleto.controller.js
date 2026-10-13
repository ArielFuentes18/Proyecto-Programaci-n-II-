const db = require("../models");
const InventarioBoleto = db.inventario_boletos;
const Concierto = db.conciertos;
const Localidad = db.localidades;

exports.create = async (req, res) => {
  const { id_concierto, id_localidad, precio, cantidad_total, cantidad_disponible } = req.body;

  if (!id_concierto || !id_localidad || precio === undefined || cantidad_total === undefined) {
    return res.status(400).send({
      message: "Los campos id_concierto, id_localidad, precio y cantidad_total son obligatorios."
    });
  }

  try {
    const conciertoExiste = await Concierto.findByPk(id_concierto);
    if (!conciertoExiste) {
      return res.status(404).send({ message: `No existe el concierto con ID ${id_concierto}.` });
    }

    const localidadExiste = await Localidad.findByPk(id_localidad);
    if (!localidadExiste) {
      return res.status(404).send({ message: `No existe la localidad con ID ${id_localidad}.` });
    }

    const inventario = {
      id_concierto,
      id_localidad,
      precio,
      cantidad_total,
  
      cantidad_disponible: cantidad_disponible !== undefined ? cantidad_disponible : cantidad_total
    };

    const data = await InventarioBoleto.create(inventario);
    res.status(201).send(data);

  } catch (err) {
    res.status(500).send({
      message: err.message || "Error al crear el registro en el inventario."
    });
  }
};

exports.findAll = (req, res) => {
  InventarioBoleto.findAll({
    include: [
      {
        model: db.conciertos,
        as: 'concierto',
        attributes: ['id_concierto', 'titulo_evento', 'fecha_concierto', 'recinto']
      },
      {
        model: db.localidades,
        as: 'localidad',
        attributes: ['id_localidad', 'nombre']
      }
    ]
  })
    .then(data => res.send(data))
    .catch(err => {
      res.status(500).send({
        message: err.message || "Error al obtener el inventario de boletos."
      });
    });
};

exports.findOne = (req, res) => {
  const id = req.params.id;

  InventarioBoleto.findByPk(id, {
    include: [
      {
        model: db.conciertos,
        as: 'concierto',
        attributes: ['id_concierto', 'titulo_evento', 'fecha_concierto', 'recinto']
      },
      {
        model: db.localidades,
        as: 'localidad',
        attributes: ['id_localidad', 'nombre']
      }
    ]
  })
    .then(data => {
      if (data) {
        res.send(data);
      } else {
        res.status(404).send({
          message: `No se encontró el registro de inventario con id=${id}.`
        });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al obtener el inventario con id=" + id
      });
    });
};

exports.update = (req, res) => {
  const id = req.params.id;

  InventarioBoleto.update(req.body, {
    where: { id_inventario: id }
  })
    .then(num => {
      if (num == 1) {
        res.send({ message: "Inventario actualizado correctamente." });
      } else {
        res.send({ message: `No se pudo actualizar el inventario con id=${id}.` });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al actualizar el inventario con id=" + id
      });
    });
};

exports.delete = (req, res) => {
  const id = req.params.id;

  InventarioBoleto.destroy({
    where: { id_inventario: id }
  })
    .then(num => {
      if (num == 1) {
        res.send({ message: "Inventario eliminado correctamente." });
      } else {
        res.send({ message: `No se pudo eliminar el inventario con id=${id}.` });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al eliminar el inventario con id=" + id
      });
    });
};