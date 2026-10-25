module.exports = app => {
  const localidades = require("../controllers/localidad.controller.js");
  const { verificarToken, soloRoles } = require("../middleware/auth");
  const admin = [verificarToken, soloRoles("administrador")];
  var router = require("express").Router();

  // Lectura pública; escritura solo administradores
  router.post("/create", ...admin, localidades.create);
  router.get("/", localidades.findAll);
  router.get("/:id", localidades.findOne);
  router.put("/update/:id", ...admin, localidades.update);
  router.delete("/delete/:id", ...admin, localidades.delete);

  app.use('/api/localidades', router);
};
