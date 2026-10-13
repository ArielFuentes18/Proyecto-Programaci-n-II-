module.exports = (sequelize, Sequelize) => {
  const Rol = sequelize.define("rol", {
    id_rol: {
      type: Sequelize.INTEGER,
      primaryKey: true,
      autoIncrement: true
    },
    nombre: {
      type: Sequelize.STRING,
      allowNull: false
    }
  }, {
    tableName: 'roles',
    timestamps: false
  });

  return Rol;
};