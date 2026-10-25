module.exports = app => {
  const ventas = require("../controllers/venta.controller.js");
  const { verificarToken, soloRoles } = require("../middleware/auth");
  var router = require("express").Router();

  router.post("/create", verificarToken, soloRoles("vendedor"), ventas.create);
  router.get("/", verificarToken, soloRoles("administrador", "vendedor"), ventas.findAll);
  router.get("/:id", verificarToken, soloRoles("administrador", "vendedor"), ventas.findOne);

  app.use("/api/ventas", router);
};
