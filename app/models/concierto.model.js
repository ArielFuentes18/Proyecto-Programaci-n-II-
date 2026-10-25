module.exports = (sequelize, Sequelize) => {
  const Concierto = sequelize.define("concierto", {
    id_concierto: {
      type: Sequelize.INTEGER,
      primaryKey: true,
      autoIncrement: true
    },
    id_artista: {
      type: Sequelize.INTEGER,
      allowNull: false
    },
    titulo_evento: {
      type: Sequelize.STRING
    },
    fecha_concierto: {
      type: Sequelize.DATE
    },
    recinto: {
      type: Sequelize.STRING
    },
    estado: {
      type: Sequelize.STRING,
      defaultValue: "Programado",
      set(val) {
        if (val) {
          const valorFormateado = val.charAt(0).toUpperCase() + val.slice(1).toLowerCase();
          this.setDataValue('estado', valorFormateado);
        }
      },
      validate: {
        isIn: {
          args: [["Programado", "Cancelado", "Suspendido", "Completado"]],
          msg: "El estado debe ser exactamente uno de los siguientes: Programado, Cancelado, Suspendido o Completado."
        }
      }
    }
  }, {
    tableName: 'conciertos',
    timestamps: false
  });

  return Concierto;
};