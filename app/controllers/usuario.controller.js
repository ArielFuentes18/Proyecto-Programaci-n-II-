const db = require("../models");
const Usuario = db.usuarios;
const Rol = db.roles;
const ROLES = require("../config/roles");
const { firmar } = require("../middleware/auth");
const { actualizar, eliminar } = require("../utils/crud");

const incluirRol = [{ model: Rol, as: "rol", attributes: ["id_rol", "nombre"] }];
const EMAIL = /^\S+@\S+\.\S+$/;

const formatearEstatus = (statusId) => {
  const map = {
    0: { id_estatus: 0, nombre: "INACTIVO" },
    1: { id_estatus: 1, nombre: "ACTIVO" },
    2: { id_estatus: 2, nombre: "SUSPENDIDO" }
  };
  return map[statusId] || { id_estatus: statusId, nombre: "DESCONOCIDO" };
};

// Nunca se devuelve la contraseña.
const transformarUsuario = (usuario) => {
  const userJson = usuario.toJSON ? usuario.toJSON() : { ...usuario };
  delete userJson.password;
  userJson.estatus = formatearEstatus(userJson.status);
  return userJson;
};

const nombreRol = (u) => {
  if (u.id_rol === ROLES.ADMIN) return "administrador";
  if (u.id_rol === ROLES.VENDEDOR) return "vendedor";
  return u.rol && u.rol.nombre ? String(u.rol.nombre).toLowerCase() : "";
};

const errorDuplicado = (err) => err.name === "SequelizeUniqueConstraintError";

// POST /api/usuarios/login  { correo, password }  ->  { token, usuario }
exports.login = async (req, res) => {
  try {
    const { correo, password } = req.body;
    if (!correo || !password) {
      return res.status(400).send({ message: "Correo y contraseña son obligatorios." });
    }
    const u = await Usuario.findOne({
      where: db.Sequelize.where(
        db.Sequelize.fn("lower", db.Sequelize.col("correo")),
        String(correo).trim().toLowerCase()
      ),
      include: incluirRol
    });
    if (!u || u.password !== String(password)) {
      return res.status(401).send({ message: "Correo o contraseña incorrectos." });
    }
    if (u.status === 0) return res.status(403).send({ message: "Tu cuenta está pendiente de aprobación o inactiva." });
    if (u.status === 2) return res.status(403).send({ message: "Tu cuenta está suspendida." });
    if (u.status !== 1) return res.status(403).send({ message: "Tu cuenta no está activa." });

    const rol = nombreRol(u);
    const token = firmar({ id: u.id_usuario, rol });
    res.send({
      token,
      usuario: { id_usuario: u.id_usuario, nombre: u.nombre, correo: u.correo, id_rol: u.id_rol, status: u.status, rol }
    });
  } catch (err) {
    res.status(500).send({ message: err.message || "Error al iniciar sesión." });
  }
};

// POST /api/usuarios/solicitar-admin  (público)  -> crea un Admin en estado INACTIVO (0) hasta que otro admin lo apruebe
exports.solicitarAdmin = async (req, res) => {
  const { nombre, correo, password } = req.body;
  if (!nombre || !correo || !password) return res.status(400).send({ message: "Nombre, correo y contraseña son obligatorios." });
  if (!EMAIL.test(correo)) return res.status(400).send({ message: "Escribe un correo válido." });
  if (String(password).length !== 6) return res.status(400).send({ message: "La contraseña debe ser exactamente de 6 dígitos." });
  try {
    await Usuario.create({ nombre, correo: String(correo).trim(), password: String(password), id_rol: ROLES.ADMIN, status: 0, fecha: new Date() });
    res.status(201).send({ message: "Solicitud enviada. Un administrador debe aprobarla." });
  } catch (err) {
    if (errorDuplicado(err)) return res.status(409).send({ message: "Ya existe una cuenta con ese correo." });
    res.status(500).send({ message: err.message || "Error al registrar la solicitud." });
  }
};

// El rol "cliente" se crea la primera vez que alguien se registra (no hace falta tocar la base a mano).
const rolCliente = async () => {
  let r = await Rol.findOne({
    where: db.Sequelize.where(db.Sequelize.fn("lower", db.Sequelize.col("nombre")), "cliente")
  });
  if (!r) r = await Rol.create({ nombre: "cliente" });
  return r;
};

// POST /api/usuarios/registro  (público) -> crea un cliente ACTIVO
exports.registrarCliente = async (req, res) => {
  const { nombre, correo, password } = req.body;
  if (!nombre || !correo || !password) return res.status(400).send({ message: "Nombre, correo y contraseña son obligatorios." });
  if (String(nombre).trim().length < 3) return res.status(400).send({ message: "Escribe tu nombre completo." });
  if (!EMAIL.test(correo)) return res.status(400).send({ message: "Escribe un correo válido." });
  if (String(password).length < 6 || String(password).length > 30) {
    return res.status(400).send({ message: "La contraseña debe tener entre 6 y 30 caracteres." });
  }
  try {
    const rol = await rolCliente();
    await Usuario.create({
      nombre: String(nombre).trim(),
      correo: String(correo).trim(),
      password: String(password),
      id_rol: rol.id_rol,
      status: 1,
      fecha: new Date()
    });
    res.status(201).send({ message: "Cuenta creada. Ya puedes iniciar sesión." });
  } catch (err) {
    if (errorDuplicado(err)) return res.status(409).send({ message: "Ya existe una cuenta con ese correo." });
    res.status(500).send({ message: err.message || "Error al crear la cuenta." });
  }
};

// POST /api/usuarios/create  (solo administradores, con token)
exports.create = async (req, res) => {
  try {
    const { nombre, correo, password, id_rol, status, fecha } = req.body;
    if (!nombre || !correo || !password) return res.status(400).send({ message: "Nombre, correo y contraseña son obligatorios." });
    if (!EMAIL.test(correo)) return res.status(400).send({ message: "Escribe un correo válido." });
    if (String(password).length !== 6) return res.status(400).send({ message: "La contraseña del nuevo usuario debe ser exactamente de 6 dígitos." });

    const data = await Usuario.create({
      nombre,
      correo: String(correo).trim(),
      password: String(password),
      id_rol: id_rol || ROLES.VENDEDOR,
      status: status !== undefined ? status : 1,
      fecha: fecha || new Date()
    });
    const creado = await Usuario.findByPk(data.id_usuario, { include: incluirRol });
    return res.status(201).send(transformarUsuario(creado));
  } catch (err) {
    if (errorDuplicado(err)) return res.status(409).send({ message: "Ya existe una cuenta con ese correo." });
    return res.status(500).send({ message: err.message || "Error al crear el usuario." });
  }
};

exports.findAll = (req, res) => {
  Usuario.findAll({ include: incluirRol })
    .then(data => res.send(data.map(transformarUsuario)))
    .catch(err => res.status(500).send({ message: err.message || "Error al obtener los usuarios." }));
};

exports.findAllStatus = (req, res) => {
  Usuario.findAll({ where: { status: req.params.status }, include: incluirRol })
    .then(data => res.send(data.map(transformarUsuario)))
    .catch(err => res.status(500).send({ message: err.message || "Error al filtrar usuarios por estatus." }));
};

exports.findOne = (req, res) => {
  const id = req.params.id;
  Usuario.findByPk(id, { include: incluirRol })
    .then(data => {
      if (data) res.send(transformarUsuario(data));
      else res.status(404).send({ message: `No se encontró el usuario con id=${id}.` });
    })
    .catch(err => res.status(500).send({ message: "Error al obtener el usuario con id=" + id }));
};

const actualizarUsuario = actualizar(Usuario, "id_usuario", "Usuario");
exports.update = (req, res) => {
  if (req.body.password !== undefined && String(req.body.password).length !== 6) {
    return res.status(400).send({ message: "La contraseña debe ser exactamente de 6 dígitos." });
  }
  return actualizarUsuario(req, res);
};

const eliminarUsuario = eliminar(Usuario, "id_usuario", "Usuario");
exports.delete = (req, res) => {
  if (req.user && String(req.user.id) === String(req.params.id)) {
    return res.status(400).send({ message: "No puedes eliminar tu propia cuenta." });
  }
  return eliminarUsuario(req, res);
};
