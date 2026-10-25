const crypto = require("crypto");
const jwt = require("jsonwebtoken");

// Define JWT_SECRET en las variables de entorno de Render. Si falta, se genera uno temporal
// (las sesiones se invalidan cada vez que el servidor se reinicia).
const SECRET = process.env.JWT_SECRET || crypto.randomBytes(32).toString("hex");
if (!process.env.JWT_SECRET) {
  console.warn("AVISO: JWT_SECRET no está definido. Configúralo en las variables de entorno.");
}

exports.firmar = (payload) =>
  jwt.sign(payload, SECRET, { expiresIn: process.env.JWT_EXPIRES || "8h" });

exports.verificarToken = (req, res, next) => {
  const header = req.headers.authorization || "";
  const token = header.startsWith("Bearer ") ? header.slice(7) : null;
  if (!token) return res.status(401).send({ message: "Debes iniciar sesión." });
  try {
    req.user = jwt.verify(token, SECRET); // { id, rol }
    next();
  } catch (e) {
    res.status(401).send({ message: "Tu sesión es inválida o expiró." });
  }
};

exports.soloRoles = (...roles) => (req, res, next) =>
  req.user && roles.includes(req.user.rol)
    ? next()
    : res.status(403).send({ message: "No tienes permisos para realizar esta acción." });
