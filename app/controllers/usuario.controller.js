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

exports.create = async (req, res) => {
  try {
    const { id_usuario_admin, password_admin, nombre, correo, password, id_rol, status, fecha } = req.body;

    if (!id_usuario_admin || !password_admin) {
      return res.status(400).send({ 
        message: "Se requiere el ID y la contraseña del administrador para realizar esta acción." 
      });
    }

    if (!nombre || !correo || !password) {
      return res.status(400).send({ message: "El contenido no puede estar vacío." });
    }

    if (!password || password.length !== 6) {
      return res.status(400).send({ message: "La contraseña del nuevo usuario debe ser exactamente de 6 dígitos." });
    }

    const admin = await Usuario.findByPk(id_usuario_admin, {
      include: [{
        model: Rol,
        as: 'rol',
        attributes: ['id_rol', 'nombre']
      }]
    });

    const esRolAdmin = admin && admin.rol && (admin.rol.id_rol === 1 || admin.rol.nombre === 'ADMIN');
    const passwordCorrecto = admin && admin.password === password_admin;

    if (!admin || !passwordCorrecto || !esRolAdmin) {
      return res.status(403).send({ 
        message: "El ID o la contraseña del administrador son incorrectos, o no cuenta con permisos de administrador." 
      });
    }

    const usuarioNuevoData = {
      nombre,
      correo,
      password,
      id_rol: id_rol || 1,
      status: status !== undefined ? status : 1,
      fecha: fecha || new Date()
    };

    const data = await Usuario.create(usuarioNuevoData);
    
    const usuarioCreado = await Usuario.findByPk(data.id_usuario, {
      include: [{
        model: Rol,
        as: 'rol',
        attributes: ['id_rol', 'nombre']
      }]
    });

    return res.status(201).send(transformarUsuario(usuarioCreado));

  } catch (err) {
    return res.status(500).send({ message: err.message || "Error al crear el usuario." });
  }
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