module.exports = app => {
  const detalleVenta = require("../controllers/detalle_venta.controller.js");
  const { verificarToken, soloRoles } = require("../middleware/auth");
  var router = require("express").Router();

  router.post("/create", verificarToken, soloRoles("vendedor"), detalleVenta.create);
  router.get("/", verificarToken, soloRoles("administrador", "vendedor"), detalleVenta.findAll);
  router.get("/:id", verificarToken, soloRoles("administrador", "vendedor"), detalleVenta.findOne);
  router.put("/update/:id", verificarToken, soloRoles("administrador"), detalleVenta.update);
  router.delete("/delete/:id", verificarToken, soloRoles("administrador"), detalleVenta.delete);

  app.use('/api/detalle_ventas', router);
};
