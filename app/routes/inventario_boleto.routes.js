module.exports = app => {
  const inventario = require("../controllers/inventario_boleto.controller.js");
  const { verificarToken, soloRoles } = require("../middleware/auth");
  const admin = [verificarToken, soloRoles("administrador")];
  var router = require("express").Router();

  // Lectura pública; escritura solo administradores
  router.post("/create", ...admin, inventario.create);
  router.get("/", inventario.findAll);
  router.get("/concierto/:id", inventario.findByConcierto);
  router.get("/:id", inventario.findOne);
  router.put("/update/:id", ...admin, inventario.update);
  router.delete("/delete/:id", ...admin, inventario.delete);

  app.use('/api/inventario_boletos', router);
};
