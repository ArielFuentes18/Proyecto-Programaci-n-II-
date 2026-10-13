const db = require("../models");
const DetalleVenta = db.detalle_ventas;
const Venta = db.ventas;
const InventarioBoleto = db.inventario_boletos;

exports.create = async (req, res) => {
  const { id_venta, id_inventario, cantidad } = req.body;

  if (!id_venta || !id_inventario || !cantidad) {
    return res.status(400).send({
      message: "Los campos id_venta, id_inventario y cantidad son obligatorios."
    });
  }

  try {
    const ventaExiste = await Venta.findByPk(id_venta);
    if (!ventaExiste) {
      return res.status(404).send({ message: `No existe la venta con ID ${id_venta}.` });
    }

    const itemInventario = await InventarioBoleto.findByPk(id_inventario);
    if (!itemInventario) {
      return res.status(404).send({ message: `No existe el item de inventario con ID ${id_inventario}.` });
    }

    if (itemInventario.cantidad_disponible < cantidad) {
      return res.status(400).send({
        message: `Stock insuficiente. Boletos disponibles: ${itemInventario.cantidad_disponible}`
      });
    }

    const precioUnitario = itemInventario.precio;
    const subtotalCalculado = Number(cantidad) * Number(precioUnitario);

    const nuevoDetalle = {
      id_venta,
      id_inventario,
      cantidad,
      precio_unitario: precioUnitario,
      subtotal: subtotalCalculado
    };

    const data = await DetalleVenta.create(nuevoDetalle);

    await itemInventario.update({
      cantidad_disponible: itemInventario.cantidad_disponible - cantidad
    });

    res.status(201).send(data);

  } catch (err) {
    res.status(500).send({
      message: err.message || "Error al registrar el detalle de la venta."
    });
  }
};

exports.findAll = (req, res) => {
  DetalleVenta.findAll({
    include: [
      {
        model: db.ventas,
        as: 'venta'
      },
      {
        model: db.inventario_boletos,
        as: 'inventario'
      }
    ]
  })
    .then(data => res.send(data))
    .catch(err => {
      res.status(500).send({
        message: err.message || "Error al obtener los detalles de venta."
      });
    });
};

exports.findOne = (req, res) => {
  const id = req.params.id;

  DetalleVenta.findByPk(id, {
    include: [
      {
        model: db.ventas,
        as: 'venta'
      },
      {
        model: db.inventario_boletos,
        as: 'inventario'
      }
    ]
  })
    .then(data => {
      if (data) {
        res.send(data);
      } else {
        res.status(404).send({
          message: `No se encontró el detalle de venta con id=${id}.`
        });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al obtener el detalle de venta con id=" + id
      });
    });
};

exports.update = (req, res) => {
  const id = req.params.id;

  DetalleVenta.update(req.body, {
    where: { id_detalle: id }
  })
    .then(num => {
      if (num == 1) {
        res.send({ message: "Detalle de venta actualizado correctamente." });
      } else {
        res.send({ message: `No se pudo actualizar el detalle con id=${id}.` });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al actualizar el detalle con id=" + id
      });
    });
};

exports.delete = (req, res) => {
  const id = req.params.id;

  DetalleVenta.destroy({
    where: { id_detalle: id }
  })
    .then(num => {
      if (num == 1) {
        res.send({ message: "Detalle de venta eliminado correctamente." });
      } else {
        res.send({ message: `No se pudo eliminar el detalle con id=${id}.` });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al eliminar el detalle con id=" + id
      });
    });
};