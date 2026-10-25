module.exports = app => {
  const conciertos = require("../controllers/concierto.controller.js");
  const { verificarToken, soloRoles } = require("../middleware/auth");
  const admin = [verificarToken, soloRoles("administrador")];
  var router = require("express").Router();

  // Lectura pública; escritura solo administradores
  router.post("/create", ...admin, conciertos.create);
  router.get("/", conciertos.findAll);
  router.get("/:id", conciertos.findOne);
  router.put("/update/:id", ...admin, conciertos.update);
  router.delete("/delete/:id", ...admin, conciertos.delete);

  app.use('/api/conciertos', router);
};
