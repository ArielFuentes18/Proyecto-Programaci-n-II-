module.exports = app => {
  const inventario = require("../controllers/inventario_boleto.controller.js");
  var router = require("express").Router();

  router.post("/create/", inventario.create);
  router.get("/", inventario.findAll);
  router.get("/:id", inventario.findOne);
  router.put("/update/:id", inventario.update);
  router.delete("/delete/:id", inventario.delete);

  app.use('/api/inventario_boletos', router);
};