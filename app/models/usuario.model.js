module.exports = (sequelize, Sequelize) => {
  const Usuario = sequelize.define("usuario", {
    id_usuario: {
      type: Sequelize.INTEGER,
      primaryKey: true,
      autoIncrement: true
    },
    nombre: {
      type: Sequelize.STRING,
      allowNull: false
    },
    correo: {
      type: Sequelize.STRING,
      allowNull: false,
      unique: true
    },
    password: {
      type: Sequelize.STRING,
      allowNull: false
    },
    id_rol: {
      type: Sequelize.INTEGER,
      allowNull: false,
      defaultValue: 1
    },
    status: {
      type: Sequelize.INTEGER,
      allowNull: false,
      defaultValue: 1
    },
    fecha: {
      type: Sequelize.DATEONLY, 
      allowNull: true
    }
  }, {
    tableName: 'usuarios',
    timestamps: false
  });

  return Usuario;
};