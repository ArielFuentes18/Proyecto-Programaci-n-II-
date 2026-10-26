const db = require("../models");
const Venta = db.ventas;
const Usuario = db.usuarios;
const Rol = db.roles;

class ErrorNegocio extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

// POST /api/ventas/create
// Con "detalles": [{ id_inventario, cantidad }] registra venta + detalles + descuento de stock en UNA transacción.
// Precios y total se recalculan en el servidor. Sin "detalles" se mantiene el comportamiento anterior.
exports.create = async (req, res) => {
  const { detalles } = req.body;
  const id_usuario = req.user ? req.user.id : req.body.id_usuario;
  const METODOS = ["tarjeta", "transferencia"];
  const metodo_pago = METODOS.includes(String(req.body.metodo_pago || "").toLowerCase())
    ? String(req.body.metodo_pago).toLowerCase() : null;
  const MAX_BOLETOS_CLIENTE = 8;

  if (Array.isArray(detalles) && detalles.length > 0) {
    const t = await db.sequelize.transaction();
    try {
      let total = 0;
      let boletos = 0;
      const filas = [];
      for (const d of detalles) {
        const cantidad = Number(d.cantidad);
        if (!d.id_inventario || !Number.isInteger(cantidad) || cantidad <= 0) {
          throw new ErrorNegocio(400, "Cada detalle requiere id_inventario y una cantidad entera mayor a 0.");
        }
        boletos += cantidad;
        const inv = await db.inventario_boletos.findByPk(d.id_inventario, { transaction: t, lock: t.LOCK.UPDATE });
        if (!inv) throw new ErrorNegocio(404, `No existe el inventario con ID ${d.id_inventario}.`);
        if (inv.cantidad_disponible < cantidad) {
          throw new ErrorNegocio(409, `Stock insuficiente en el inventario ${inv.id_inventario}. Disponibles: ${inv.cantidad_disponible}.`);
        }
        inv.cantidad_disponible -= cantidad;
        await inv.save({ transaction: t });
        const subtotal = Math.round(Number(inv.precio) * cantidad * 100) / 100;
        total += subtotal;
        filas.push({ id_inventario: inv.id_inventario, cantidad, precio_unitario: inv.precio, subtotal });
      }
      total = Math.round(total * 100) / 100;
      if (req.user && req.user.rol === "cliente" && boletos > MAX_BOLETOS_CLIENTE) {
        throw new ErrorNegocio(400, `Solo puedes comprar hasta ${MAX_BOLETOS_CLIENTE} boletos por orden.`);
      }

      const nueva = await Venta.create({ id_usuario, fecha_venta: new Date(), total_venta: total, metodo_pago }, { transaction: t });
      await db.detalle_ventas.bulkCreate(filas.map(f => ({ ...f, id_venta: nueva.id_venta })), { transaction: t });
      await t.commit();

      const completa = await Venta.findByPk(nueva.id_venta, { include: [{ model: db.detalle_ventas, as: "detalles" }] });
      return res.status(201).send(completa);
    } catch (err) {
      await t.rollback().catch(() => {});
      if (err instanceof ErrorNegocio) return res.status(err.status).send({ message: err.message });
      return res.status(500).send({ message: err.message || "Error al registrar la venta." });
    }
  }

  // Comportamiento anterior (venta sin detalles)
  const { total_venta } = req.body;
  if (!id_usuario || !total_venta) {
    return res.status(400).send({ message: "Envía los detalles de la venta o los campos id_usuario y total_venta." });
  }
  try {
    const nuevaVenta = await Venta.create({
      id_usuario,
      fecha_venta: req.body.fecha_venta || new Date(),
      total_venta
    });
    res.status(201).send(nuevaVenta);
  } catch (err) {
    res.status(500).send({ message: err.message || "Error al registrar la venta." });
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