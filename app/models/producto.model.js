// Utilizamos module.exports para exportar el modelo y que pueda ser utilizado en index.js
module.exports = (sequelize, Sequelize) => {
    // Usamos sequelize.define para crear la entidad "proovedor" en la base de datos
    const Producto = sequelize.define("producto", {
        nombre: {
            type: Sequelize.STRING
        },
        producto: {
            type: Sequelize.STRING
        },
        peso: {
            type: Sequelize.STRING
        },
        tipo: {
            type: Sequelize.STRING
        }       
    });

    return Producto;; 
};