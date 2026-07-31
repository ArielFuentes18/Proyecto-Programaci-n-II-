const db = require("../models");
const Producto = db.producto;
const Op = db.Sequelize.Op;

// Crear y guardar un nuevo Producto
exports.create = (req, res) => {
  if (!req.body.nombre) {
    res.status(400).send({
      message: "El nombre no puede estar vacío"
    });
    return;
  }

  const producto = {
    nombre: req.body.nombre,
    producto: req.body.producto,
    peso: req.body.peso,
    tipo: req.body.tipo
  };

  Producto.create(producto)
    .then(data => {
      res.send(data);
    })
    .catch(err => {
      res.status(500).send({
        message: err.message || "Ocurrió un error al crear el producto."
      });
    });
};

// Obtener todos los Productos de la base de datos (o buscar por nombre)
exports.findAll = (req, res) => {
  const nombre = req.query.nombre;
  var condition = nombre ? { nombre: { [Op.iLike]: `%${nombre}%` } } : null;

  Producto.findAll({ where: condition })
    .then(data => {
      res.send(data);
    })
    .catch(err => {
      res.status(500).send({
        message: err.message || "Ocurrió un error al obtener los productos."
      });
    });
};

// Buscar un solo Producto por ID
exports.findOne = (req, res) => {
  const id = req.params.id;

  Producto.findByPk(id)
    .then(data => {
      if (data) {
        res.send(data);
      } else {
        res.status(404).send({
          message: `No se encontró el producto con id=${id}.`
        });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error recuperando el producto con id=" + id
      });
    });
};

// Actualizar un Producto por ID en la petición
exports.update = (req, res) => {
  const id = req.params.id;

  Producto.update(req.body, {
    where: { id: id }
  })
    .then(num => {
      // num[0] verifica el número de registros actualizados
      if (num[0] == 1 || num == 1) {
        res.send({
          message: "El producto se actualizó correctamente."
        });
      } else {
        res.send({
          message: `No se pudo actualizar el producto con id=${id}. ¡Tal vez no fue encontrado o el body está vacío!`
        });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al actualizar el producto con id=" + id
      });
    });
};

// Eliminar un Producto con el ID especificado
exports.delete = (req, res) => {
  const id = req.params.id;

  Producto.destroy({
    where: { id: id }
  })
    .then(num => {
      if (num == 1) {
        res.send({
          message: "¡El producto se eliminó correctamente!"
        });
      } else {
        res.send({
          message: `No se pudo eliminar el producto con id=${id}. ¡El producto no fue encontrado!`
        });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "No se pudo eliminar el producto con id=" + id
      });
    });
};

// Eliminar todos los Productos de la base de datos
exports.deleteAll = (req, res) => {
  Producto.destroy({
    where: {},
    truncate: false
  })
    .then(nums => {
      res.send({ message: `${nums} Productos se eliminaron correctamente.` });
    })
    .catch(err => {
      res.status(500).send({
        message: err.message || "Ocurrió un error al eliminar los productos."
      });
    });
};

// Buscar todos los productos activos (status: true)
exports.findAllStatus = (req, res) => {
  Producto.findAll({ where: { status: true } })
    .then(data => {
      res.send(data);
    })
    .catch(err => {
      res.status(500).send({
        message: err.message || "Ocurrió un error al buscar los productos activos."
      });
    });
};