module.exports = (sequelize, Sequelize) => {
  const Venta = sequelize.define("venta", {
    id_venta: {
      type: Sequelize.INTEGER,
      primaryKey: true,
      autoIncrement: true
    },
    id_usuario: {
      type: Sequelize.INTEGER,
      allowNull: false,
      references: {
        model: 'usuarios',
        key: 'id_usuario'
      }
    },
    fecha_venta: {
      type: Sequelize.DATE,
      defaultValue: Sequelize.NOW
    },
    total_venta: {
      type: Sequelize.DECIMAL(10, 2),
      allowNull: false
    }
  }, {
    tableName: 'ventas',
    timestamps: false
  });

  return Venta;
};