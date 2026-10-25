module.exports = (sequelize, Sequelize) => {
  const Localidad = sequelize.define("localidad", {
    id_localidad: {
      type: Sequelize.INTEGER,
      primaryKey: true,
      autoIncrement: true
    },
    nombre: {
      type: Sequelize.STRING,
      allowNull: false
    }
  }, {
    tableName: 'localidades',
    timestamps: false
  });

  return Localidad;
};