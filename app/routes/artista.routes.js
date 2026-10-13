module.exports = app => {
  const artistas = require("../controllers/artista.controller.js");
  var router = require("express").Router();

  router.post("/create/", artistas.create);
  router.get("/", artistas.findAll);
  router.get("/:id", artistas.findOne);
  router.put("/update/:id", artistas.update);
  router.delete("/delete/:id", artistas.delete);

  app.use('/api/artistas', router);
};