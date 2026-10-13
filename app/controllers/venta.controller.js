const db = require("../models");
const Venta = db.ventas;
const Usuario = db.usuarios;
const Rol = db.roles;

exports.create = async (req, res) => {
  const { id_usuario, total_venta } = req.body;

  if (!id_usuario || !total_venta) {
    return res.status(400).send({
      message: "Los campos id_usuario y total_venta son obligatorios."
    });
  }

  try {
    const usuario = await Usuario.findByPk(id_usuario, {
      include: [{
        model: Rol,
        as: 'rol',
        attributes: ['id_rol', 'nombre']
      }]
    });

    if (!usuario) {
      return res.status(404).send({
        message: `No existe ningún usuario registrado con el ID ${id_usuario}.`
      });
    }

    const esVendedor = usuario.rol && (
      usuario.rol.nombre.toUpperCase() === 'VENDEDOR' || 
      usuario.rol.id_rol === 2
    );

    if (!esVendedor) {
      const nombreRol = usuario.rol ? usuario.rol.nombre : 'NINGUNO';
      return res.status(403).send({
        message: `El usuario con ID ${id_usuario} (${usuario.nombre}) no puede registrar ventas porque su rol es '${nombreRol}'.`
      });
    }

    const venta = {
      id_usuario: id_usuario,
      fecha_venta: req.body.fecha_venta || new Date(),
      total_venta: total_venta
    };

    const nuevaVenta = await Venta.create(venta);
    res.status(201).send(nuevaVenta);

  } catch (err) {
    res.status(500).send({
      message: err.message || "Error al registrar la venta."
    });
  }
};

exports.findAll = (req, res) => {
  Venta.findAll({
    include: [{
      model: Usuario,
      as: 'usuario',
      attributes: ['id_usuario', 'nombre', 'correo']
    }]
  })
    .then(data => res.send(data))
    .catch(err => {
      res.status(500).send({
        message: err.message || "Error al obtener las ventas."
      });
    });
};

exports.findOne = (req, res) => {
  const id = req.params.id;

  Venta.findByPk(id, {
    include: [{
      model: Usuario,
      as: 'usuario',
      attributes: ['id_usuario', 'nombre', 'correo']
    }]
  })
    .then(data => {
      if (data) {
        res.send(data);
      } else {
        res.status(404).send({
          message: `No se encontró la venta con id=${id}.`
        });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al obtener la venta con id=" + id
      });
    });
};