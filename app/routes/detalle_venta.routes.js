module.exports = app => {
  const detalleVenta = require("../controllers/detalle_venta.controller.js");
  var router = require("express").Router();

  router.post("/create/", detalleVenta.create);
  router.get("/", detalleVenta.findAll);
  router.get("/:id", detalleVenta.findOne);
  router.put("/update/:id", detalleVenta.update);
  router.delete("/delete/:id", detalleVenta.delete);

  app.use('/api/detalle_ventas', router);
};