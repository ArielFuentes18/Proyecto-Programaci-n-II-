// Handlers genéricos de actualizar / eliminar con códigos HTTP correctos.
const esFK = (err) => err.name === "SequelizeForeignKeyConstraintError";
const esUnico = (err) => err.name === "SequelizeUniqueConstraintError";
const esValidacion = (err) => err.name === "SequelizeValidationError" || err.name === "SequelizeDatabaseError";

exports.actualizar = (Model, pk, nombre) => async (req, res) => {
  const id = req.params.id;
  const datos = { ...req.body };
  delete datos[pk];
  try {
    const [num] = await Model.update(datos, { where: { [pk]: id } });
    if (!num) return res.status(404).send({ message: `No se encontró ${nombre} con id=${id}.` });
    res.send({ message: `${nombre} actualizado correctamente.` });
  } catch (err) {
    if (esUnico(err)) return res.status(409).send({ message: "Ya existe un registro con ese valor (dato duplicado)." });
    if (esFK(err)) return res.status(409).send({ message: "Uno de los datos relacionados no existe." });
    if (esValidacion(err)) return res.status(400).send({ message: err.message });
    res.status(500).send({ message: err.message || `Error al actualizar ${nombre}.` });
  }
};

exports.eliminar = (Model, pk, nombre) => async (req, res) => {
  const id = req.params.id;
  try {
    const num = await Model.destroy({ where: { [pk]: id } });
    if (!num) return res.status(404).send({ message: `No se encontró ${nombre} con id=${id}.` });
    res.send({ message: `${nombre} eliminado correctamente.` });
  } catch (err) {
    if (esFK(err)) {
      return res.status(409).send({
        message: "No se puede eliminar: tiene registros relacionados (conciertos, inventario o ventas)."
      });
    }
    res.status(500).send({ message: err.message || `Error al eliminar ${nombre}.` });
  }
};
