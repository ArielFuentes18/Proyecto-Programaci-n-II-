// Importamos el modulo express 
const express = require("express");
const bodyParser = require("body-parser");
const cors = require("cors");

const app = express();

var corsOptions = {
  origin: "http://localhost:8081"
};

app.use(cors(corsOptions));

// parse requests of content-type - application/json
app.use(bodyParser.json());

// parse requests of content-type - application/x-www-form-urlencoded
app.use(bodyParser.urlencoded({ extended: true }));

const db = require("./app/models");
db.sequelize.sync();
// // drop the table if it already exists
// db.sequelize.sync({ force: true }).then(() => {
//   console.log("Drop and re-sync db.");
// });

// simple route
app.get("/", (req, res) => {
  res.json({ message: "UMG Web Application" });
});


require("./app/routes/usuario.routes")(app);
require("./app/routes/venta.routes")(app);
require("./app/routes/artista.routes")(app);
require("./app/routes/concierto.routes")(app);
require("./app/routes/localidad.routes")(app);
require("./app/routes/inventario_boleto.routes")(app);
require("./app/routes/detalle_venta.routes")(app);
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// set port, listen for requests
const PORT = process.env.PORT || 8081;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}.`);
});



