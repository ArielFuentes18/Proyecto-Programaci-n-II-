const db = require("../models");
const Concierto = db.conciertos;
const Artista = db.artistas;

exports.create = async (req, res) => {
  const { id_artista, titulo_evento, fecha_concierto, recinto, estado } = req.body;

  if (!id_artista || !titulo_evento) {
    return res.status(400).send({
      message: "Los campos id_artista y titulo_evento son obligatorios."
    });
  }

  try {
    const artistaExiste = await Artista.findByPk(id_artista);

    if (!artistaExiste) {
      return res.status(404).send({
        message: `No existe ningún artista registrado con el ID ${id_artista}.`
      });
    }

    const concierto = {
      id_artista,
      titulo_evento,
      fecha_concierto: fecha_concierto || new Date(),
      recinto,
      estado: estado || "Programado"
    };

    const data = await Concierto.create(concierto);
    res.status(201).send(data);

  } catch (err) {
    res.status(400).send({
      message: err.message || "Error al registrar el concierto."
    });
  }
};

exports.findAll = (req, res) => {
  Concierto.findAll({
    include: [{
      model: db.artistas,
      as: 'artista',
      attributes: ['id_artista', 'nombre_artistico', 'genero_musical']
    }]
  })
    .then(data => res.send(data))
    .catch(err => {
      res.status(500).send({
        message: err.message || "Error al obtener los conciertos."
      });
    });
};

exports.findOne = (req, res) => {
  const id = req.params.id;

  Concierto.findByPk(id, {
    include: [{
      model: db.artistas,
      as: 'artista',
      attributes: ['id_artista', 'nombre_artistico', 'genero_musical']
    }]
  })
    .then(data => {
      if (data) {
        res.send(data);
      } else {
        res.status(404).send({
          message: `No se encontró el concierto con id=${id}.`
        });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al obtener el concierto con id=" + id
      });
    });
};

exports.update = (req, res) => {
  const id = req.params.id;

  Concierto.update(req.body, {
    where: { id_concierto: id }
  })
    .then(num => {
      if (num == 1) {
        res.send({
          message: "Concierto actualizado correctamente."
        });
      } else {
        res.send({
          message: `No se pudo actualizar el concierto con id=${id}.`
        });
      }
    })
    .catch(err => {
      res.status(400).send({
        message: err.message || "Error al actualizar el concierto con id=" + id
      });
    });
};

exports.delete = (req, res) => {
  const id = req.params.id;

  Concierto.destroy({
    where: { id_concierto: id }
  })
    .then(num => {
      if (num == 1) {
        res.send({
          message: "Concierto eliminado correctamente."
        });
      } else {
        res.send({
          message: `No se pudo eliminar el concierto con id=${id}.`
        });
      }
    })
    .catch(err => {
      res.status(500).send({
        message: "Error al eliminar el concierto con id=" + id
      });
    });
};