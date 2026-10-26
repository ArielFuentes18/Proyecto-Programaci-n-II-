module.exports = app => {
  const estadisticas = require("../controllers/estadistica.controller.js");
  const { verificarToken, soloRoles } = require("../middleware/auth");
  var router = require("express").Router();

  router.get("/resumen", verificarToken, soloRoles("administrador"), estadisticas.resumen);

  app.use("/api/estadisticas", router);
};
