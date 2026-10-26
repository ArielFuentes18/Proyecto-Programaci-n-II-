const express = require("express");
const cors = require("cors");

const app = express();
app.set("trust proxy", 1); // Render usa un proxy: así req.protocol devuelve https

// CORS abierto por defecto (la app JavaFX no lo necesita; el panel web sí).
// En producción puedes limitarlo con la variable CORS_ORIGIN="https://tu-sitio.com,https://otro.com"
app.use(cors({ origin: process.env.CORS_ORIGIN ? process.env.CORS_ORIGIN.split(",") : true }));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

const db = require("./app/models");

// Prepara la base de datos UNA vez al arrancar. Las peticiones esperan a que termine (db.listo),
// así nunca consultan una columna que todavía no existe (causa típica de listas vacías / error 500).
// Si una columna opcional no se puede crear, el modelo deja de usarla y el resto sigue funcionando.
const COLUMNAS_OPCIONALES = [
  { tabla: "artistas", modelo: db.artistas, columna: "imagen_url", tipo: "TEXT" },
  { tabla: "ventas", modelo: db.ventas, columna: "metodo_pago", tipo: "VARCHAR(30)" }
];
db.listo = (async () => {
  try {
    await db.sequelize.sync();
  } catch (err) {
    console.error("Error al sincronizar la base de datos:", err.message);
  }
  for (const c of COLUMNAS_OPCIONALES) {
    try {
      await db.sequelize.query(`ALTER TABLE ${c.tabla} ADD COLUMN IF NOT EXISTS ${c.columna} ${c.tipo}`);
    } catch (err) {
      console.error(`No se pudo crear ${c.tabla}.${c.columna}; se ignora esa columna:`, err.message);
      c.modelo.removeAttribute(c.columna);
    }
  }
})();
app.use((req, res, next) => { db.listo.then(() => next(), () => next()); });

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
