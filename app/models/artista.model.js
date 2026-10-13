module.exports = (sequelize, Sequelize) => {
  const Artista = sequelize.define("artista", {
    id_artista: {
      type: Sequelize.INTEGER,
      primaryKey: true,
      autoIncrement: true
    },
    nombre_artistico: {
      type: Sequelize.STRING
    },
    genero_musical: {
      type: Sequelize.STRING
    },
    pais_origen: {
      type: Sequelize.STRING
    }
  }, {
    tableName: 'artistas',
    timestamps: false
  });

  return Artista;
};