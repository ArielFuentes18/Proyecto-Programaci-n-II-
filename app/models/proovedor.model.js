// Utilizamos module.exports para exportar el modelo y que pueda ser utilizado en index.js
module.exports = (sequelize, Sequelize) => {
    // Usamos sequelize.define para crear la entidad "proovedor" en la base de datos
    const Proovedor = sequelize.define("proovedor", {
        nombre: {
            type: Sequelize.STRING
        },
        producto: {
            type: Sequelize.STRING
        },
        telefono: {
            type: Sequelize.STRING
        },
        correo: {
            type: Sequelize.STRING
        }       
    });

    return Proovedor; 
};