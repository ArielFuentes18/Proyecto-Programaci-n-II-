const db = require("../models");
const Usuario = db.usuarios;
const Rol = db.roles;

const formatearEstatus = (statusId) => {
  const map = {
    0: { id_estatus: 0, nombre: "INACTIVO" },
    1: { id_estatus: 1, nombre: "ACTIVO" },
    2: { id_estatus: 2, nombre: "SUSPENDIDO" }
  };
  return map[statusId] || { id_estatus: statusId, nombre: "DESCONOCIDO" };
};

const transformarUsuario = (usuario) => {
  const userJson = usuario.toJSON ? usuario.toJSON() : usuario;
  userJson.estatus = formatearEstatus(userJson.status);
  return userJson;
};

exports.create = (req, res) => {
  if (!req.body.nombre || !req.body.correo || !req.body.password) {
    return res.status(400).send({ message: "El contenido no puede estar vacío." });
  }

  const usuario = {
    nombre: req.body.nombre,
    correo: req.body.correo,
    password: req.body.password,
    id_rol: req.body.id_rol || 1,
    status: req.body.status !== undefined ? req.body.status : 1
  };

  Usuario.create(usuario)
    .then(data => {
      return Usuario.findByPk(data.id_usuario, {
        include: [{
          model: Rol,
          as: 'rol',
          attributes: ['id_rol', 'nombre']
        }]
      });
    })
    .then(usuarioCreado => {
      res.status(201).send(transformarUsuario(usuarioCreado));
    })
    .catch(err => {
      res.status(500).send({ message: err.message || "Error al crear el usuario." });
    });
};

exports.findAll = (req, res) => {
  Usuario.findAll({
    include: [{
      model: Rol,
      as: 'rol',
      attributes: ['id_rol', 'nombre']
    }]
  })
    .then(data => {
      const resultado = data.map(u => transformarUsuario(u));
      res.send(resultado);
    })
    .catch(err => {
      res.status(500).send({ message: err.message || "Error al obtener los usuarios." });
    });
};

exports.findAllStatus = (req, res) => {
  const statusVal = req.params.status;

  Usuario.findAll({
    where: { status: statusVal },
    include: [{
      model: Rol,
      as: 'rol',
      attributes: ['id_rol', 'nombre']
    }]
  })
    .then(data => {
      const resultado = data.map(u => transformarUsuario(u));
      res.send(resultado);
    })
    .catch(err => {
      res.status(500).send({ message: err.message || "Error al filtrar usuarios por estatus." });
    });
};

exports.findOne = (req, res) => {
  const id = req.params.id;

  Usuario.findByPk(id, {
    include: [{
      model: Rol,
      as: 'rol',
      attributes: ['id_rol', 'nombre']
    }]
  })
    .then(data => {
      if (data) {
        res.send(transformarUsuario(data));
      } else {
        res.status(404).send({ message: `No se encontró el usuario con id=${id}.` });
      }
    })
    .catch(err => {
      res.status(500).send({ message: "Error al obtener el usuario con id=" + id });
    });
};

exports.update = (req, res) => {
  const id = req.params.id;

  Usuario.update(req.body, { where: { id_usuario: id } })
    .then(num => {
      if (num == 1) {
        res.send({ message: "Usuario actualizado correctamente." });
      } else {
        res.send({ message: `No se pudo actualizar el usuario con id=${id}.` });
      }
    })
    .catch(err => {
      res.status(500).send({ message: "Error al actualizar el usuario con id=" + id });
    });
};

exports.delete = (req, res) => {
  const id = req.params.id;

  Usuario.destroy({ where: { id_usuario: id } })
    .then(num => {
      if (num == 1) {
        res.send({ message: "Usuario eliminado correctamente." });
      } else {
        res.send({ message: `No se pudo eliminar el usuario con id=${id}.` });
      }
    })
    .catch(err => {
      res.status(500).send({ message: "No se pudo eliminar el usuario con id=" + id });
    });
};