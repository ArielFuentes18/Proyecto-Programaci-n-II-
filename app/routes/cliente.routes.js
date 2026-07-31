module.exports = app => {
    const clientes = require("../controllers/cliente.controller.js");
    var router = require("express").Router();
    // Create a new Client
    router.post("/create/", clientes.create);
    // Retrieve all Client
    router.get("/", clientes.findAll);
    // Retrieve all published Client
    router.get("/status", clientes.findAllStatus);
    // Retrieve a single Client with id
    router.get("/:id", clientes.findOne);
    // Update a Client with id
    router.put("/update/:id", clientes.update);
    // Delete a Client with id  
    router.delete("/delete/:id", clientes.delete);
    // Delete all Cliente
    router.delete("/delete/", clientes.deleteAll);
    // Podemos utilizar como una ocpion app.use("EndPoint",router" para simplicar el URI
    // Ej.  http://localhost:Puerto/api/cliente/
    app.use("/api/customer", router);
};