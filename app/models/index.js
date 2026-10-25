// Cargamos el archivo de configuración que contiene los datos de conexión a la base de datos
const dbConfig = require("../config/db.config.js");

// Importamos Sequelize, el ORM que nos permite trabajar con PostgreSQL como objetos JS
const Sequelize = require("sequelize");

// Creamos una instancia de Sequelize con los parámetros de conexión, incluyendo el puerto y SSL para Supabase
const sequelize = new Sequelize(dbConfig.DB, dbConfig.USER, dbConfig.PASSWORD, {
  host: dbConfig.HOST,          // Dirección del servidor (host) de la base de datos
  port: dbConfig.PORT,        
  dialect: dbConfig.dialect,      // El tipo de base de datos, en este caso 'postgres'

  // Configuraciones específicas del dialecto (PostgreSQL), incluyendo la conexión segura SSL
  dialectOptions: {
    ssl: {
      require: true,              // Indica que la conexión debe usar SSL obligatoriamente
      rejectUnauthorized: false   // Acepta certificados autofirmados o no verificados
    }
  },

  // Configuración del pool de conexiones para optimizar el rendimiento
  pool: {
    max: dbConfig.pool.max,       // Máximo de conexiones simultáneas
    min: dbConfig.pool.min,       // Mínimo de conexiones
    acquire: dbConfig.pool.acquire, // Tiempo máximo para obtener una conexión antes de lanzar error
    idle: dbConfig.pool.idle      // Tiempo que una conexión puede estar inactiva antes de ser liberada
  }
});
// Creamos un objeto `db` que exportaremos para acceder a Sequelize y los modelos desde otras partes del proyecto
const db = {};

// Asignamos la clase Sequelize al objeto `db`, útil si se requiere usar métodos del ORM manualmente
db.sequelize = sequelize;
db.Sequelize = Sequelize;
db.usuarios = require("./usuario.model.js")(sequelize, Sequelize);
db.roles = require("./rol.model.js")(sequelize, Sequelize)
db.ventas = require("./venta.model.js")(sequelize, Sequelize);
db.artistas = require("./artista.model.js")(sequelize, Sequelize);
db.conciertos = require("./concierto.model.js")(sequelize, Sequelize);
db.localidades = require("./localidad.model.js")(sequelize, Sequelize);
db.inventario_boletos = require("./inventario_boleto.model.js")(sequelize, Sequelize);
db.detalle_ventas = require("./detalle_venta.model.js")(sequelize, Sequelize);

db.roles.hasMany(db.usuarios, { foreignKey: 'id_rol' });
db.usuarios.belongsTo(db.roles, { foreignKey: 'id_rol', as: 'rol' });

db.ventas.belongsTo(db.usuarios, {
  foreignKey: 'id_usuario',
  as: 'usuario'
});

db.usuarios.hasMany(db.ventas, {
  foreignKey: 'id_usuario',
  as: 'ventas'
});


db.conciertos.belongsTo(db.artistas, {
  foreignKey: 'id_artista',
  as: 'artista'
});
db.artistas.hasMany(db.conciertos, {
  foreignKey: 'id_artista',
  as: 'conciertos'
});



// Relaciones con Conciertos
db.inventario_boletos.belongsTo(db.conciertos, {
  foreignKey: 'id_concierto',
  as: 'concierto'
});
db.conciertos.hasMany(db.inventario_boletos, {
  foreignKey: 'id_concierto',
  as: 'inventarios'
});

// Relaciones con Localidades
db.inventario_boletos.belongsTo(db.localidades, {
  foreignKey: 'id_localidad',
  as: 'localidad'
});
db.localidades.hasMany(db.inventario_boletos, {
  foreignKey: 'id_localidad',
  as: 'inventarios'
});

//detalle venta
db.detalle_ventas.belongsTo(db.ventas, {
  foreignKey: 'id_venta',
  as: 'venta'
});
db.ventas.hasMany(db.detalle_ventas, {
  foreignKey: 'id_venta',
  as: 'detalles'
});

db.detalle_ventas.belongsTo(db.inventario_boletos, {
  foreignKey: 'id_inventario',
  as: 'inventario'
});
db.inventario_boletos.hasMany(db.detalle_ventas, {
  foreignKey: 'id_inventario',
  as: 'detalles'
});


// Aquí puedes seguir importando otros modelos de forma similar
// Ejemplo: db.productos = require("./producto.model.js")(sequelize, Sequelize);

// Exportamos el objeto `db` para que pueda ser usado por otros módulos (por ejemplo, en el `server.js`)
module.exports = db;