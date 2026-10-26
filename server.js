const express = require("express");
const cors = require("cors");

const app = express();

// CORS abierto por defecto (la app JavaFX no lo necesita; el panel web sí).
// En producción puedes limitarlo con la variable CORS_ORIGIN="https://tu-sitio.com,https://otro.com"
app.use(cors({ origin: process.env.CORS_ORIGIN ? process.env.CORS_ORIGIN.split(",") : true }));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

const db = require("./app/models");
db.sequelize.sync()
  // Agrega la columna de la foto del cantante si todavía no existe (idempotente)
  .then(() => db.sequelize.query("ALTER TABLE artistas ADD COLUMN IF NOT EXISTS imagen_url TEXT"))
  .catch(err => console.error("Error al sincronizar la base de datos:", err.message));

app.get("/", (req, res) => {
  res.json({ message: "UMG Web Application" });
});

// Para mantener despierto el servidor de Render con un ping periódico (UptimeRobot, cron-job.org...)
app.get("/health", (req, res) => res.json({ ok: true }));

require("./app/routes/usuario.routes")(app);
require("./app/routes/venta.routes")(app);
require("./app/routes/artista.routes")(app);
require("./app/routes/concierto.routes")(app);
require("./app/routes/localidad.routes")(app);
require("./app/routes/inventario_boleto.routes")(app);
require("./app/routes/detalle_venta.routes")(app);
require("./app/routes/estadistica.routes")(app);

// Ruta inexistente -> JSON
app.use((req, res) => res.status(404).json({ message: "Ruta no encontrada." }));

// Errores no controlados (JSON mal formado, etc.) -> JSON
app.use((err, req, res, next) => {
  res.status(err.status || 500).json({ message: err.message || "Error interno del servidor." });
});

const PORT = process.env.PORT || 8081;
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}.`);
});
