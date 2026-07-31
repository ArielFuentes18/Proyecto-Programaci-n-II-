module.exports = app => {
  const proovedores = require("../controllers/proovedor.controller.js");
  var router = require("express").Router();

   
  router.post("/", proovedores.create);
  router.get("/", proovedores.findAll);
  router.get("/activos", proovedores.findAllStatus); 
  router.get("/:id", proovedores.findOne);
  router.put("/:id", proovedores.update);
  router.delete("/:id", proovedores.delete);
  router.delete("/", proovedores.deleteAll);
  app.use('/api/proovedores', router); 
};