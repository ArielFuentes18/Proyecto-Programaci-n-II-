module.exports = (sequelize, Sequelize) => {
  const InventarioBoleto = sequelize.define("inventario_boleto", {
    id_inventario: {
      type: Sequelize.INTEGER,
      primaryKey: true,
      autoIncrement: true
    },
    id_concierto: {
      type: Sequelize.INTEGER,
      allowNull: false
    },
    id_localidad: {
      type: Sequelize.INTEGER,
      allowNull: false
    },
    precio: {
      type: Sequelize.DECIMAL(10, 2),
      allowNull: false
    },
    cantidad_total: {
      type: Sequelize.INTEGER,
      allowNull: false
    },
    cantidad_disponible: {
      type: Sequelize.INTEGER,
      allowNull: false
    }
  }, {
    tableName: 'inventario_boletos',
    timestamps: false
  });

  return InventarioBoleto;
};