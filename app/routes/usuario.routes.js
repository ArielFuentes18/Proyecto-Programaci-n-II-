module.exports = app => {
  const usuarios = require("../controllers/usuario.controller.js");
  const { verificarToken, soloRoles } = require("../middleware/auth");
  const admin = [verificarToken, soloRoles("administrador")];
  var router = require("express").Router();

  router.post("/login", usuarios.login);
  router.post("/solicitar-admin", usuarios.solicitarAdmin);
  router.post("/registro", usuarios.registrarCliente);

  router.post("/create", ...admin, usuarios.create);
  router.get("/", ...admin, usuarios.findAll);
  router.get("/status/:status", ...admin, usuarios.findAllStatus);
  router.get("/:id", ...admin, usuarios.findOne);
  router.put("/update/:id", ...admin, usuarios.update);
  router.delete("/delete/:id", ...admin, usuarios.delete);

  app.use("/api/usuarios", router);
};
