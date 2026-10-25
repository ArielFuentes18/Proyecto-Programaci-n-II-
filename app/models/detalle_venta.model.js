module.exports = (sequelize, Sequelize) => {
  const DetalleVenta = sequelize.define("detalle_venta", {
    id_detalle: {
      type: Sequelize.INTEGER,
      primaryKey: true,
      autoIncrement: true
    },
    id_venta: {
      type: Sequelize.INTEGER,
      allowNull: false
    },
    id_inventario: {
      type: Sequelize.INTEGER,
      allowNull: false
    },
    cantidad: {
      type: Sequelize.INTEGER,
      allowNull: false
    },
    precio_unitario: {
      type: Sequelize.DECIMAL(10, 2),
      allowNull: false
    },
    subtotal: {
      type: Sequelize.DECIMAL(10, 2),
      allowNull: false
    }
  }, {
    tableName: 'detalle_ventas',
    timestamps: false
  });

  return DetalleVenta;
};