package com.sistematickets.Controller;

import com.sistematickets.Service.CorreoConfig;
import com.sistematickets.Service.QrCodigo;
import jakarta.activation.DataHandler;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.util.ByteArrayDataSource;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;


public final class CorreoCompra {
    private CorreoCompra() {}

    private static final Pattern CORREO = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]{2,}$");

    public record Resultado(int esperados, int enviados, List<String> errores, boolean configurado) {
        public boolean todoOk() { return configurado && errores.isEmpty() && enviados == esperados; }
    }

    public static boolean correoValido(String c) {
        return c != null && CORREO.matcher(c.trim()).matches();
    }

    public static Resultado enviar(FacturaDatos d, String destino, byte[] pdf) {
        List<BoletoDatos> boletos = d.boletos();
        int esperados = 1 + boletos.size();
        List<String> errores = new ArrayList<>();
        CorreoConfig cfg = CorreoConfig.cargar();
        if (!cfg.configurado()) return new Resultado(esperados, 0, errores, false);

        Session sesion = Session.getInstance(propiedades(cfg));
        int ok = 0;
        try (Transport tr = sesion.getTransport("smtp")) {
            tr.connect(cfg.host(), cfg.puerto(), cfg.usuario(), cfg.clave());

            try {
                MimeMessage m = factura(sesion, cfg, d, destino, pdf, boletos);
                tr.sendMessage(m, m.getAllRecipients());
                ok++;
            } catch (MessagingException | IOException | RuntimeException e) {
                errores.add("Factura: " + e.getMessage());
            }

            for (BoletoDatos b : boletos) {
                try {
                    MimeMessage m = ticket(sesion, cfg, d, destino, b);
                    tr.sendMessage(m, m.getAllRecipients());
                    ok++;
                } catch (MessagingException | IOException | RuntimeException e) {
                    errores.add("Ticket " + b.numero() + ": " + e.getMessage());
                }
            }
        } catch (MessagingException e) {
            errores.add("No se pudo conectar con el servidor de correo: " + e.getMessage());
        }
        return new Resultado(esperados, ok, errores, true);
    }

    private static Properties propiedades(CorreoConfig cfg) {
        Properties p = new Properties();
        p.put("mail.smtp.host", cfg.host());
        p.put("mail.smtp.port", String.valueOf(cfg.puerto()));
        p.put("mail.smtp.auth", "true");
        if (cfg.puerto() == 465) p.put("mail.smtp.ssl.enable", "true");
        else p.put("mail.smtp.starttls.enable", "true");
        p.put("mail.smtp.connectiontimeout", "15000");
        p.put("mail.smtp.timeout", "20000");
        p.put("mail.smtp.writetimeout", "20000");
        return p;
    }


    private static MimeMessage factura(Session s, CorreoConfig cfg, FacturaDatos d, String destino, byte[] pdf,
                                       List<BoletoDatos> boletos) throws MessagingException, IOException {
        Map<String, byte[]> imagenes = new LinkedHashMap<>();
        imagenes.put("qr-orden", QrCodigo.png(d.codigo(), 260));
        for (BoletoDatos b : boletos) imagenes.put("qr-" + b.numero(), QrCodigo.png(b.codigo(), 260));

        MimeMessage m = base(s, cfg, destino, "Tu factura " + d.numero() + " · " + d.evento());
        MimeMultipart mixto = new MimeMultipart("mixed");
        mixto.addBodyPart(cuerpoHtml(htmlFactura(d, boletos), imagenes));
        if (pdf != null && pdf.length > 0) {
            MimeBodyPart adj = new MimeBodyPart();
            adj.setDataHandler(new DataHandler(new ByteArrayDataSource(pdf, "application/pdf")));
            adj.setFileName(d.nombreArchivo());
            adj.setDisposition(MimeBodyPart.ATTACHMENT);
            mixto.addBodyPart(adj);
        }
        m.setContent(mixto);
        m.saveChanges();
        return m;
    }

    private static MimeMessage ticket(Session s, CorreoConfig cfg, FacturaDatos d, String destino, BoletoDatos b)
            throws MessagingException, IOException {
        byte[] qr = QrCodigo.png(b.codigo(), 320);
        Map<String, byte[]> imagenes = new LinkedHashMap<>();
        imagenes.put("qr-ticket", qr);

        MimeMessage m = base(s, cfg, destino, "Tu ticket " + b.numero() + " de " + b.total() + " · " + d.evento());
        MimeMultipart mixto = new MimeMultipart("mixed");
        mixto.addBodyPart(cuerpoHtml(htmlTicket(d, b), imagenes));
        MimeBodyPart adj = new MimeBodyPart();                  
        adj.setDataHandler(new DataHandler(new ByteArrayDataSource(qr, "image/png")));
        adj.setFileName("Ticket-" + b.codigo() + ".png");
        adj.setDisposition(MimeBodyPart.ATTACHMENT);
        mixto.addBodyPart(adj);
        m.setContent(mixto);
        m.saveChanges();
        return m;
    }

    private static MimeMessage base(Session s, CorreoConfig cfg, String destino, String asunto)
            throws MessagingException, UnsupportedEncodingException {
        MimeMessage m = new MimeMessage(s);
        m.setFrom(new InternetAddress(cfg.remitente(), cfg.nombre(), "UTF-8"));
        m.setRecipients(Message.RecipientType.TO, InternetAddress.parse(destino.trim(), true));
        m.setSubject(asunto, "UTF-8");
        return m;
    }

    private static MimeBodyPart cuerpoHtml(String html, Map<String, byte[]> imagenes) throws MessagingException {
        MimeMultipart rel = new MimeMultipart("related");
        MimeBodyPart h = new MimeBodyPart();
        h.setContent(html, "text/html; charset=UTF-8");
        rel.addBodyPart(h);
        for (var e : imagenes.entrySet()) {
            MimeBodyPart img = new MimeBodyPart();
            img.setDataHandler(new DataHandler(new ByteArrayDataSource(e.getValue(), "image/png")));
            img.setContentID("<" + e.getKey() + ">");
            img.setDisposition(MimeBodyPart.INLINE);
            img.setFileName(e.getKey() + ".png");
            rel.addBodyPart(img);
        }
        MimeBodyPart cont = new MimeBodyPart();
        cont.setContent(rel);
        return cont;
    }


    private static final String MORADO = "#2e1065", LILA = "#ede9fe", GRIS = "#8a84a3", TEXTO = "#1f1b2e";

    private static String marco(String titulo, String contenido) {
        return "<!DOCTYPE html><html><body style=\"margin:0;padding:24px 12px;background:#f5f3ff;font-family:Segoe UI,Helvetica,Arial,sans-serif;color:" + TEXTO + ";\">"
                + "<table role=\"presentation\" align=\"center\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" style=\"max-width:600px;width:100%;background:#ffffff;border-radius:14px;overflow:hidden;\">"
                + "<tr><td style=\"background:" + MORADO + ";padding:22px 28px;color:#ffffff;\">" + titulo + "</td></tr>"
                + "<tr><td style=\"padding:24px 28px;\">" + contenido + "</td></tr></table></body></html>";
    }

    private static String etiqueta(String t) {
        return "<div style=\"font-size:11px;letter-spacing:1px;font-weight:700;color:" + GRIS + ";margin-top:14px;\">" + esc(t) + "</div>";
    }

    private static String htmlFactura(FacturaDatos d, List<BoletoDatos> boletos) {
        StringBuilder sb = new StringBuilder();
        String titulo = "<div style=\"font-size:12px;letter-spacing:2px;color:#ddd6fe;\">FACTURA</div>"
                + "<div style=\"font-size:24px;font-weight:700;margin-top:4px;\">" + esc(d.numero()) + "</div>"
                + "<div style=\"font-size:12px;color:#c4b5fd;margin-top:4px;\">" + esc(d.fechaTexto()) + " · PAGADO</div>";

        sb.append("<p style=\"margin:0 0 6px;font-size:15px;\">¡Gracias por tu compra, <b>").append(esc(d.clienteNombre())).append("</b>!</p>")
          .append("<p style=\"margin:0;font-size:13px;color:#3b3556;\">Aquí está tu factura. Abajo encontrarás el QR de cada ticket; además te enviamos un correo aparte por cada uno.</p>");

        sb.append(etiqueta("EVENTO"))
          .append("<div style=\"font-size:16px;font-weight:700;color:").append(MORADO).append(";\">").append(esc(d.evento())).append("</div>");
        for (String linea : new String[]{d.artista(), d.fechaEvento(), d.recinto()}) {
            if (linea != null && !linea.isBlank()) sb.append("<div style=\"font-size:13px;color:#3b3556;\">").append(esc(linea)).append("</div>");
        }

        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:20px;font-size:13px;border-collapse:collapse;\">")
          .append("<tr style=\"color:").append(GRIS).append(";font-size:11px;letter-spacing:1px;\"><td style=\"padding:6px 0;border-bottom:1px solid #ece8f7;\">LOCALIDAD</td>")
          .append("<td align=\"right\" style=\"border-bottom:1px solid #ece8f7;\">CANT.</td><td align=\"right\" style=\"border-bottom:1px solid #ece8f7;\">PRECIO</td><td align=\"right\" style=\"border-bottom:1px solid #ece8f7;\">SUBTOTAL</td></tr>");
        for (LineaCompra l : d.lineas()) {
            sb.append("<tr><td style=\"padding:9px 0;border-bottom:1px solid #f3f0fb;\">").append(esc(l.localidad())).append("</td>")
              .append("<td align=\"right\" style=\"border-bottom:1px solid #f3f0fb;\">").append(l.cantidad()).append("</td>")
              .append("<td align=\"right\" style=\"border-bottom:1px solid #f3f0fb;\">").append(Impuestos.q(l.precio())).append("</td>")
              .append("<td align=\"right\" style=\"border-bottom:1px solid #f3f0fb;font-weight:700;\">").append(Impuestos.q(l.subtotal())).append("</td></tr>");
        }
        sb.append("</table>");

        sb.append("<table role=\"presentation\" align=\"right\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:12px;font-size:13px;\">")
          .append("<tr><td style=\"padding:2px 18px 2px 0;color:#3b3556;\">Subtotal (sin IVA)</td><td align=\"right\">").append(Impuestos.q(d.base())).append("</td></tr>")
          .append("<tr><td style=\"padding:2px 18px 2px 0;color:#3b3556;\">IVA 12 %</td><td align=\"right\">").append(Impuestos.q(d.iva())).append("</td></tr>")
          .append("<tr><td style=\"padding:8px 18px 0 0;font-weight:700;color:").append(MORADO).append(";\">TOTAL PAGADO</td>")
          .append("<td align=\"right\" style=\"padding-top:8px;font-size:18px;font-weight:700;color:").append(MORADO).append(";\">").append(Impuestos.q(d.total())).append("</td></tr></table>")
          .append("<div style=\"clear:both;\"></div>");

        sb.append(etiqueta("MÉTODO DE PAGO"))
          .append("<div style=\"font-size:14px;font-weight:700;color:").append(MORADO).append(";\">").append(esc(d.metodo())).append("</div>")
          .append("<div style=\"font-size:12px;color:#3b3556;\">").append(esc(d.detallePago())).append("</div>");

        // QR de la orden
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:22px;background:").append(LILA).append(";border-radius:10px;\"><tr>")
          .append("<td width=\"130\" style=\"padding:12px;\"><img src=\"cid:qr-orden\" width=\"110\" height=\"110\" alt=\"QR de la orden\" style=\"display:block;background:#fff;\"></td>")
          .append("<td style=\"padding:12px;\"><div style=\"font-size:11px;letter-spacing:1px;font-weight:700;color:").append(GRIS).append(";\">CÓDIGO DE LA ORDEN</div>")
          .append("<div style=\"font-size:15px;font-weight:700;color:").append(MORADO).append(";\">").append(esc(d.codigo())).append("</div>")
          .append("<div style=\"font-size:12px;color:#3b3556;margin-top:4px;\">Identifica toda tu compra. Para entrar al evento se usa el QR de cada ticket.</div></td></tr></table>");

        // QR de cada ticket
        sb.append("<div style=\"margin-top:26px;font-size:15px;font-weight:700;color:").append(MORADO).append(";\">Tus tickets (").append(boletos.size()).append(")</div>")
          .append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:10px;\">");
        for (int i = 0; i < boletos.size(); i += 2) {
            sb.append("<tr>");
            for (int j = i; j < i + 2; j++) {
                if (j >= boletos.size()) { sb.append("<td width=\"50%\"></td>"); continue; }
                BoletoDatos b = boletos.get(j);
                sb.append("<td width=\"50%\" align=\"center\" valign=\"top\" style=\"padding:8px;\">")
                  .append("<div style=\"border:1px solid #ece8f7;border-radius:10px;padding:12px;\">")
                  .append("<div style=\"font-size:12px;font-weight:700;color:").append(MORADO).append(";\">Ticket ").append(b.numero()).append(" de ").append(b.total()).append("</div>")
                  .append("<div style=\"font-size:12px;color:#3b3556;margin-bottom:8px;\">").append(esc(b.localidad())).append("</div>")
                  .append("<img src=\"cid:qr-").append(b.numero()).append("\" width=\"140\" height=\"140\" alt=\"QR ticket ").append(b.numero()).append("\" style=\"display:block;margin:0 auto;\">")
                  .append("<div style=\"font-size:11px;color:").append(GRIS).append(";margin-top:6px;word-break:break-all;\">").append(esc(b.codigo())).append("</div>")
                  .append("</div></td>");
            }
            sb.append("</tr>");
        }
        sb.append("</table>");
        return marco(titulo, sb.toString());
    }

    private static String htmlTicket(FacturaDatos d, BoletoDatos b) {
        String titulo = "<div style=\"font-size:12px;letter-spacing:2px;color:#ddd6fe;\">TICKET " + b.numero() + " DE " + b.total() + "</div>"
                + "<div style=\"font-size:22px;font-weight:700;margin-top:4px;\">" + esc(d.evento()) + "</div>";
        StringBuilder sb = new StringBuilder();
        for (String linea : new String[]{d.artista(), d.fechaEvento(), d.recinto()}) {
            if (linea != null && !linea.isBlank()) sb.append("<div style=\"font-size:14px;color:#3b3556;\">").append(esc(linea)).append("</div>");
        }
        sb.append("<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin-top:18px;\"><tr>")
          .append("<td valign=\"top\"><div style=\"font-size:11px;letter-spacing:1px;font-weight:700;color:").append(GRIS).append(";\">LOCALIDAD</div>")
          .append("<div style=\"font-size:18px;font-weight:700;color:").append(MORADO).append(";\">").append(esc(b.localidad())).append("</div>")
          .append("<div style=\"font-size:11px;letter-spacing:1px;font-weight:700;color:").append(GRIS).append(";margin-top:12px;\">PRECIO</div>")
          .append("<div style=\"font-size:14px;\">").append(Impuestos.q(b.precio())).append("</div>")
          .append("<div style=\"font-size:11px;letter-spacing:1px;font-weight:700;color:").append(GRIS).append(";margin-top:12px;\">FACTURA</div>")
          .append("<div style=\"font-size:14px;\">").append(esc(d.numero())).append("</div></td>")
          .append("<td align=\"right\" valign=\"top\"><img src=\"cid:qr-ticket\" width=\"200\" height=\"200\" alt=\"QR del ticket\" style=\"display:block;\"></td></tr></table>")
          .append("<div style=\"margin-top:14px;text-align:center;font-size:13px;font-weight:700;color:").append(MORADO).append(";\">").append(esc(b.codigo())).append("</div>")
          .append("<div style=\"margin-top:8px;text-align:center;font-size:12px;color:#3b3556;\">Presenta este QR en la entrada del evento. Es válido para una sola persona; no lo compartas.</div>");
        return marco(titulo, sb.toString());
    }

    private static String esc(String t) {
        if (t == null) return "";
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
