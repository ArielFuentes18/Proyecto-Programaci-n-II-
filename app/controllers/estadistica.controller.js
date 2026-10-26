const db = require("../models");
const { Op } = db.Sequelize;

const DIA_MS = 86400000;
// Guatemala = UTC-6. Cámbialo con la variable de entorno TZ_OFFSET_HORAS si tu zona es otra.
const OFFSET_H = process.env.TZ_OFFSET_HORAS !== undefined ? Number(process.env.TZ_OFFSET_HORAS) : -6;

const num = (v) => Number(v) || 0;
const r2 = (v) => Math.round(v * 100) / 100;
const plano = (x) => (x && typeof x.toJSON === "function" ? x.toJSON() : x);
const aLocal = (d) => new Date(new Date(d).getTime() + OFFSET_H * 3600000);
const claveDia = (d) => aLocal(d).toISOString().slice(0, 10);
const claveMes = (d) => claveDia(d).slice(0, 7);
const variacion = (act, prev) => (prev > 0 ? r2(((act - prev) / prev) * 100) : null);
const q = (v) => "Q" + r2(v).toFixed(2).replace(/\B(?=(\d{3})+(?!\d))/g, ",");

const sumar = (mapa, clave, extra) => {
  if (!mapa[clave]) mapa[clave] = { boletos: 0, ingresos: 0, ventas: 0 };
  const e = mapa[clave];
  e.boletos += extra.boletos || 0;
  e.ingresos += extra.ingresos || 0;
  e.ventas += extra.ventas || 0;
  return e;
};

// GET /api/estadisticas/resumen?dias=30   (dias=0 -> todo el historial). Solo administradores.
exports.resumen = async (req, res) => {
  try {
    const d = req.query.dias === undefined ? 30 : parseInt(req.query.dias, 10);
    const dias = Number.isFinite(d) && d > 0 ? Math.min(d, 3650) : 0;
    const ahora = new Date();
    const inicio = dias ? new Date(ahora.getTime() - dias * DIA_MS) : null;
    const inicioPrevio = dias ? new Date(ahora.getTime() - 2 * dias * DIA_MS) : null;
    const desdeConsulta = dias ? new Date(ahora.getTime() - Math.max(2 * dias, 400) * DIA_MS) : null;

    const [ventasRaw, invRaw, conRaw, locRaw] = await Promise.all([
      db.ventas.findAll({
        where: desdeConsulta ? { fecha_venta: { [Op.gte]: desdeConsulta } } : undefined,
        include: [
          { model: db.usuarios, as: "usuario", attributes: ["id_usuario", "nombre"] },
          { model: db.detalle_ventas, as: "detalles" }
        ]
      }),
      db.inventario_boletos.findAll(),
      db.conciertos.findAll({ include: [{ model: db.artistas, as: "artista", attributes: ["id_artista", "nombre_artistico", "genero_musical"] }] }),
      db.localidades.findAll()
    ]);

    const ventas = ventasRaw.map(plano);
    const inventario = invRaw.map(plano);
    const conciertos = conRaw.map(plano);
    const localidades = locRaw.map(plano);

    const invPorId = {};
    inventario.forEach((i) => { invPorId[i.id_inventario] = i; });
    const conPorId = {};
    conciertos.forEach((c) => { conPorId[c.id_concierto] = c; });
    const locPorId = {};
    localidades.forEach((l) => { locPorId[l.id_localidad] = l; });

    const fechaDe = (v) => new Date(v.fecha_venta);
    const periodo = ventas.filter((v) => !inicio || fechaDe(v) >= inicio);
    const previo = dias ? ventas.filter((v) => fechaDe(v) >= inicioPrevio && fechaDe(v) < inicio) : [];
    const boletosDe = (v) => (v.detalles || []).reduce((s, x) => s + num(x.cantidad), 0);
    const totales = (lista) => ({
      ingresos: r2(lista.reduce((s, v) => s + num(v.total_venta), 0)),
      boletos: lista.reduce((s, v) => s + boletosDe(v), 0),
      ventas: lista.length
    });
    const act = totales(periodo);
    const prev = totales(previo);

    // ---- agregados del período ----
    const porConcierto = {}, porLocalidad = {}, porGenero = {}, porVendedor = {};
    for (const v of periodo) {
      const bv = boletosDe(v);
      const vend = porVendedor[v.id_usuario] || (porVendedor[v.id_usuario] = { nombre: (v.usuario && v.usuario.nombre) || "Usuario " + v.id_usuario, ventas: 0, boletos: 0, ingresos: 0 });
      vend.ventas += 1;
      vend.boletos += bv;
      vend.ingresos += num(v.total_venta);
      for (const det of v.detalles || []) {
        const inv = invPorId[det.id_inventario];
        if (!inv) continue;
        const cant = num(det.cantidad);
        const sub = num(det.subtotal) || cant * num(det.precio_unitario);
        sumar(porConcierto, inv.id_concierto, { boletos: cant, ingresos: sub });
        sumar(porLocalidad, inv.id_localidad, { boletos: cant, ingresos: sub });
        const c = conPorId[inv.id_concierto];
        const genero = (c && c.artista && c.artista.genero_musical) || "Sin género";
        sumar(porGenero, genero, { boletos: cant, ingresos: sub });
      }
    }

    // ---- series de tiempo ----
    const nDias = Math.min(dias || 60, 60);
    const serieDiaria = [];
    const idxDia = {};
    for (let i = nDias - 1; i >= 0; i--) {
      const k = claveDia(new Date(ahora.getTime() - i * DIA_MS));
      idxDia[k] = { fecha: k, ingresos: 0, boletos: 0, ventas: 0 };
      serieDiaria.push(idxDia[k]);
    }
    const serieMensual = [];
    const idxMes = {};
    const base = aLocal(ahora);
    for (let i = 11; i >= 0; i--) {
      const f = new Date(Date.UTC(base.getUTCFullYear(), base.getUTCMonth() - i, 1));
      const k = f.toISOString().slice(0, 7);
      idxMes[k] = { mes: k, ingresos: 0, boletos: 0, ventas: 0 };
      serieMensual.push(idxMes[k]);
    }
    for (const v of ventas) {
      const kd = claveDia(v.fecha_venta), km = claveMes(v.fecha_venta);
      if (idxDia[kd]) { idxDia[kd].ingresos += num(v.total_venta); idxDia[kd].boletos += boletosDe(v); idxDia[kd].ventas += 1; }
      if (idxMes[km]) { idxMes[km].ingresos += num(v.total_venta); idxMes[km].boletos += boletosDe(v); idxMes[km].ventas += 1; }
    }
    [...serieDiaria, ...serieMensual].forEach((p) => { p.ingresos = r2(p.ingresos); });

    // ---- inventario (foto actual) ----
    const invPorConcierto = {}, invPorLocalidad = {};
    for (const i of inventario) {
      const cap = num(i.cantidad_total), disp = num(i.cantidad_disponible), precio = num(i.precio);
      const a = invPorConcierto[i.id_concierto] || (invPorConcierto[i.id_concierto] = { cap: 0, disp: 0, potencial: 0, restante: 0 });
      a.cap += cap; a.disp += disp; a.potencial += precio * cap; a.restante += precio * disp;
      const b = invPorLocalidad[i.id_localidad] || (invPorLocalidad[i.id_localidad] = { cap: 0, disp: 0 });
      b.cap += cap; b.disp += disp;
    }

    const fechas = ventas.map((v) => fechaDe(v).getTime());
    const periodoDias = dias || (fechas.length ? Math.max(1, Math.ceil((ahora.getTime() - Math.min(...fechas)) / DIA_MS)) : 1);
    const maxIngresos = Math.max(0, ...Object.values(porConcierto).map((x) => x.ingresos));

    // ---- viabilidad de cada concierto ----
    const filas = conciertos.map((c) => {
      const inv = invPorConcierto[c.id_concierto] || { cap: 0, disp: 0, potencial: 0, restante: 0 };
      const p = porConcierto[c.id_concierto] || { boletos: 0, ingresos: 0 };
      const vendidos = inv.cap - inv.disp;
      const ocup = inv.cap ? r2((vendidos / inv.cap) * 100) : 0;
      const fechaEv = c.fecha_concierto ? new Date(c.fecha_concierto) : null;
      const diasEv = fechaEv ? Math.ceil((fechaEv.getTime() - ahora.getTime()) / DIA_MS) : null;
      const finalizado = diasEv !== null && diasEv < 0;
      const ritmo = r2(p.boletos / periodoDias);
      const diasAgotar = !finalizado && inv.disp > 0 && ritmo > 0 ? Math.ceil(inv.disp / ritmo) : null;
      const demanda = inv.cap === 0 ? "Sin inventario" : ocup >= 75 ? "Alta" : ocup >= 40 ? "Media" : "Baja";
      const riesgo = !finalizado && inv.cap > 0 && diasEv !== null && diasEv <= 30 && ocup < 50;
      const puntaje = Math.round(0.6 * ocup + 0.4 * (maxIngresos ? (p.ingresos / maxIngresos) * 100 : 0));
      let recomendacion;
      if (finalizado) recomendacion = "Evento finalizado.";
      else if (inv.cap === 0) recomendacion = "Sin boletos cargados: agrega inventario para poder venderlo.";
      else if (ocup >= 90) recomendacion = "Casi agotado: evalúa subir precios o abrir una función adicional.";
      else if (ocup >= 75) recomendacion = "Alta demanda: mantén el precio y refuerza la logística.";
      else if (riesgo) recomendacion = "Evento cercano con baja venta: lanza una promoción o descuento.";
      else if (diasAgotar !== null && diasEv !== null && diasAgotar > diasEv) recomendacion = "Al ritmo actual no se agotará antes del evento: impulsa la promoción.";
      else if (ocup < 40) recomendacion = "Demanda baja: revisa precios y difusión.";
      else recomendacion = "Venta saludable: monitorea semanalmente.";
      return {
        id_concierto: c.id_concierto, titulo: c.titulo_evento,
        artista: (c.artista && c.artista.nombre_artistico) || "",
        genero: (c.artista && c.artista.genero_musical) || "",
        fecha: c.fecha_concierto, estado: c.estado, dias_para_evento: diasEv,
        capacidad: inv.cap, vendidos, disponibles: inv.disp, ocupacion_pct: ocup,
        boletos_periodo: p.boletos, ingresos_periodo: r2(p.ingresos),
        ingresos_potenciales: r2(inv.potencial), ritmo_diario: ritmo, dias_para_agotar: diasAgotar,
        demanda, riesgo, puntaje, recomendacion
      };
    }).sort((a, b) => b.puntaje - a.puntaje || b.ocupacion_pct - a.ocupacion_pct);

    const porLoc = Object.keys(locPorId).map((id) => {
      const s = porLocalidad[id] || { boletos: 0, ingresos: 0 };
      const i = invPorLocalidad[id] || { cap: 0, disp: 0 };
      return { id_localidad: Number(id), nombre: locPorId[id].nombre, boletos: s.boletos, ingresos: r2(s.ingresos), capacidad: i.cap, ocupacion_pct: i.cap ? r2(((i.cap - i.disp) / i.cap) * 100) : 0 };
    }).sort((a, b) => b.ingresos - a.ingresos);

    const porGen = Object.keys(porGenero).map((g) => ({ genero: g, boletos: porGenero[g].boletos, ingresos: r2(porGenero[g].ingresos) })).sort((a, b) => b.ingresos - a.ingresos);
    const topVend = Object.keys(porVendedor).map((id) => {
      const v = porVendedor[id];
      return { id_usuario: Number(id), nombre: v.nombre, ventas: v.ventas, boletos: v.boletos, ingresos: r2(v.ingresos), ticket_promedio: v.ventas ? r2(v.ingresos / v.ventas) : 0 };
    }).sort((a, b) => b.ingresos - a.ingresos);

    // ---- KPIs ----
    const capTotal = filas.reduce((s, f) => s + f.capacidad, 0);
    const dispTotal = filas.reduce((s, f) => s + f.disponibles, 0);
    const potencial = Object.values(invPorConcierto).reduce((s, x) => s + x.potencial, 0);
    const restante = Object.values(invPorConcierto).reduce((s, x) => s + x.restante, 0);
    const proximos = filas.filter((f) => f.dias_para_evento === null || f.dias_para_evento >= 0);
    const riesgos = filas.filter((f) => f.riesgo);
    const kpis = {
      ingresos: act.ingresos, ingresos_previo: prev.ingresos, variacion_ingresos: variacion(act.ingresos, prev.ingresos),
      boletos: act.boletos, boletos_previo: prev.boletos, variacion_boletos: variacion(act.boletos, prev.boletos),
      ventas: act.ventas, ventas_previo: prev.ventas, variacion_ventas: variacion(act.ventas, prev.ventas),
      ticket_promedio: act.ventas ? r2(act.ingresos / act.ventas) : 0,
      boletos_por_venta: act.ventas ? r2(act.boletos / act.ventas) : 0,
      capacidad_total: capTotal, boletos_disponibles: dispTotal, boletos_vendidos_total: capTotal - dispTotal,
      ocupacion_pct: capTotal ? r2(((capTotal - dispTotal) / capTotal) * 100) : 0,
      ingresos_potenciales: r2(potencial), ingresos_por_realizar: r2(restante),
      conciertos_total: filas.length, conciertos_proximos: proximos.length,
      conciertos_finalizados: filas.length - proximos.length, conciertos_riesgo: riesgos.length
    };

    // ---- hallazgos para la gerencia ----
    const hallazgos = [];
    const etiquetaPeriodo = dias ? `de los últimos ${dias} días` : "de todo el historial";
    if (act.ventas === 0) {
      hallazgos.push(`No hay ventas registradas en el período (${etiquetaPeriodo}).`);
    } else {
      const v = kpis.variacion_ingresos;
      hallazgos.push(`Ingresos ${etiquetaPeriodo}: ${q(act.ingresos)} en ${act.ventas} ventas` + (v === null ? "." : ` (${v >= 0 ? "+" : ""}${v}% frente al período anterior).`));
      const topC = filas.filter((f) => f.ingresos_periodo > 0).sort((a, b) => b.ingresos_periodo - a.ingresos_periodo)[0];
      if (topC) hallazgos.push(`Concierto líder: "${topC.titulo}" con ${q(topC.ingresos_periodo)} (${r2((topC.ingresos_periodo / act.ingresos) * 100)}% de los ingresos).`);
      const topL = porLoc[0];
      if (topL && topL.ingresos > 0) hallazgos.push(`Localidad estrella: ${topL.nombre} con ${q(topL.ingresos)} (${r2((topL.ingresos / act.ingresos) * 100)}% de los ingresos).`);
      if (topVend[0]) hallazgos.push(`Mejor vendedor: ${topVend[0].nombre} con ${q(topVend[0].ingresos)} en ${topVend[0].ventas} ventas.`);
    }
    if (riesgos.length) hallazgos.push(`Atención: ${riesgos.length} concierto(s) dentro de 30 días con ocupación menor al 50%: ${riesgos.slice(0, 3).map((f) => f.titulo).join(", ")}.`);
    const casi = filas.filter((f) => f.ocupacion_pct >= 90 && f.dias_para_evento !== null && f.dias_para_evento >= 0);
    if (casi.length) hallazgos.push(`Casi agotados (90% o más): ${casi.slice(0, 3).map((f) => f.titulo).join(", ")}.`);
    if (kpis.ingresos_por_realizar > 0) hallazgos.push(`Aún hay ${q(kpis.ingresos_por_realizar)} por vender en boletos disponibles (${r2(100 - kpis.ocupacion_pct)}% de la capacidad).`);

    res.send({
      generado: ahora.toISOString(),
      periodo: { dias, desde: inicio ? inicio.toISOString() : null },
      kpis, serie_diaria: serieDiaria, serie_mensual: serieMensual,
      por_concierto: filas, por_localidad: porLoc, por_genero: porGen, top_vendedores: topVend, hallazgos
    });
  } catch (err) {
    res.status(500).send({ message: err.message || "Error al calcular las estadísticas." });
  }
};
