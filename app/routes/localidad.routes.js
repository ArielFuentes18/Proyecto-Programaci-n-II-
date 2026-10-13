module.exports = app => {
  const localidades = require("../controllers/localidad.controller.js");
  var router = require("express").Router();

  router.post("/create/", localidades.create);
  router.get("/", localidades.findAll);
  router.get("/:id", localidades.findOne);
  router.put("/update/:id", localidades.update);
  router.delete("/delete/:id", localidades.delete);

  app.use('/api/localidades', router);
};