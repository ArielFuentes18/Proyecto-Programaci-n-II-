module.exports = app => {
  const productos = require("../controllers/producto.controller.js");
  var router = require("express").Router();

  // Crear un nuevo Producto
  router.post("/", productos.create);
  router.get("/", productos.findAll);
  router.get("/activos", productos.findAllStatus);
  router.get("/:id", productos.findOne);
  router.put("/:id", productos.update);
  router.delete("/:id", productos.delete);
  router.delete("/", productos.deleteAll);
  app.use('/api/productos', router);
};