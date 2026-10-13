module.exports = app => {
  const conciertos = require("../controllers/concierto.controller.js");
  var router = require("express").Router();

  router.post("/create/", conciertos.create);
  router.get("/", conciertos.findAll);
  router.get("/:id", conciertos.findOne);
  router.put("/update/:id", conciertos.update);
  router.delete("/delete/:id", conciertos.delete);

  app.use('/api/conciertos', router);
};