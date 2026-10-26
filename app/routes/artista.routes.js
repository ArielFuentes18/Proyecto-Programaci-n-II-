module.exports = app => {
  const artistas = require("../controllers/artista.controller.js");
  const { verificarToken, soloRoles } = require("../middleware/auth");
  const admin = [verificarToken, soloRoles("administrador")];
  const express = require("express");
  var router = express.Router();

  // Lectura pública; escritura solo administradores
  router.post("/imagen", ...admin, express.raw({ type: "image/*", limit: "6mb" }), artistas.subirImagen);
  router.post("/create", ...admin, artistas.create);
  router.get("/", artistas.findAll);
  router.get("/:id", artistas.findOne);
  router.put("/update/:id", ...admin, artistas.update);
  router.delete("/delete/:id", ...admin, artistas.delete);

  app.use('/api/artistas', router);
};
