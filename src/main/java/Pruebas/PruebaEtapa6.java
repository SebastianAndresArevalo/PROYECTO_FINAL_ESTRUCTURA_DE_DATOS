package Pruebas;

import estructuras.Grafo;
import estructuras.Grafo.Arista;
import estructuras.Grafo.Ruta;
import modelo.TipoBilletera;
import modelo.TipoTransaccion;
import modelo.Transaccion;
import servicio.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class PruebaEtapa6 {

    public static void main(String[] args) {

        RepositorioDatos repo = new RepositorioDatos();
        NotificacionServicio notif = new NotificacionServicio();
        PuntosServicio puntos = new PuntosServicio(notif);
        UsuarioServicio usuarios = new UsuarioServicio(repo, puntos);
        BilleteraServicio billeteras = new BilleteraServicio(repo);
        TransaccionServicio tx = new TransaccionServicio(repo, puntos, notif);
        ReversionServicio rev = new ReversionServicio(repo, puntos, notif);
        GrafoServicio grafos = new GrafoServicio(repo);
        AnaliticaServicio analitica = new AnaliticaServicio(repo);

        usuarios.registrar("U1", "Ana", "ana@correo.com");
        usuarios.registrar("U2", "Luis", "luis@correo.com");
        usuarios.registrar("U3", "Marta", "marta@correo.com");
        usuarios.registrar("U4", "Pedro", "pedro@correo.com");
        usuarios.registrar("U5", "Sofia", "sofia@correo.com");   // sin movimientos

        billeteras.crear("U1", "Gastos", TipoBilletera.GASTOS_DIARIOS);   // B1
        billeteras.crear("U1", "Ahorro", TipoBilletera.AHORRO);           // B2
        billeteras.crear("U2", "Compras", TipoBilletera.COMPRAS);         // B3
        billeteras.crear("U3", "Transporte", TipoBilletera.TRANSPORTE);   // B4
        billeteras.crear("U4", "Inversion", TipoBilletera.INVERSION);     // B5
        billeteras.crear("U5", "Gastos", TipoBilletera.GASTOS_DIARIOS);   // B6

        tx.recargar("B1", 5000);
        tx.recargar("B3", 3000);
        tx.recargar("B4", 3000);
        tx.recargar("B5", 1000);

        tx.transferir("B1", "B3", 500);                       // Ana -> Luis (x3)
        tx.transferir("B1", "B3", 300);
        Transaccion errada = tx.transferir("B1", "B3", 900);  // se revertira mas abajo
        tx.transferir("B3", "B4", 400);                       // Luis -> Marta
        tx.transferir("B4", "B1", 600);                       // Marta -> Ana (cierra el ciclo)
        tx.transferir("B3", "B1", 100);                       // Luis -> Ana (ciclo corto)
        tx.transferir("B4", "B5", 250);                       // Marta -> Pedro
        tx.transferir("B1", "B2", 200);                       // propia (solo grafo de billeteras)

        System.out.println("=== Grafo de usuarios ===");
        Grafo<String> g = grafos.grafoUsuarios();
        System.out.println("Vertices: " + g.numVertices() + " | Aristas: " + g.numAristas());
        for (Arista<String> a : g.getAristas()) {
            System.out.println("  " + a);
        }
        System.out.println("Grado salida/entrada de U1: " + g.gradoSalida("U1") + "/" + g.gradoEntrada("U1"));
        System.out.println("BFS desde U1: " + g.bfs("U1"));
        System.out.println("DFS desde U1: " + g.dfs("U1"));
        System.out.println("Camino U1 -> U4: " + grafos.caminoEntreUsuarios("U1", "U4"));
        System.out.println("Camino U4 -> U1 (no existe): " + grafos.caminoEntreUsuarios("U4", "U1"));
        System.out.println("Grupos de interaccion: " + grafos.gruposDeInteraccion());

        System.out.println("=== Grafo de billeteras ===");
        Grafo<String> gb = grafos.grafoBilleteras();
        System.out.println("Vertices: " + gb.numVertices() + " | Aristas: " + gb.numAristas());
        System.out.println("B1 -> B2 existe (transferencia propia): " + gb.existeArista("B1", "B2"));
        System.out.println("Ciclos entre billeteras: " + grafos.ciclosEntreBilleteras(4));

        System.out.println("=== Ciclos y rutas entre usuarios ===");
        for (Ruta<String> r : grafos.ciclosEntreUsuarios(4)) {
            System.out.println("  Ciclo: " + r);
        }
        for (Ruta<String> r : grafos.rutasFrecuentesEntreUsuarios(1, 3)) {
            System.out.println("  Ruta: " + r);
        }
        System.out.println("Rutas con cada tramo repetido 3+ veces: "
                + grafos.rutasFrecuentesEntreUsuarios(3, 3).size());

        System.out.println("=== Relaciones ===");
        System.out.println("Mas fuertes: " + grafos.relacionesMasFuertes(2, null, null));
        System.out.println("Envios de Ana: " + grafos.enviosDe("U1"));
        System.out.println("Recibos de Ana: " + grafos.recibosDe("U1"));

        System.out.println("=== Reversion: la transferencia revertida sale del grafo ===");
        System.out.println("U1 -> U2 antes: " + g.getArista("U1", "U2"));
        rev.revertir("U1", errada.getId());
        System.out.println("U1 -> U2 despues: " + grafos.grafoUsuarios().getArista("U1", "U2"));

        System.out.println("=== Analitica (sin rango) ===");
        System.out.println("Billeteras mas usadas: " + analitica.billeterasMasUsadas(3, null, null));
        System.out.println("Mas transferencias: " + analitica.usuariosConMasTransferencias(3, null, null));
        System.out.println("Categorias mas activas: " + analitica.categoriasMasActivas(null, null));
        Map<TipoTransaccion, Integer> frecuencia = analitica.frecuenciaPorTipo(null, null);
        System.out.println("Frecuencia por tipo: " + frecuencia);
        System.out.println("Monto total movilizado: " + analitica.montoTotalMovilizado(null, null));
        System.out.println("Usuario mas activo: " + analitica.usuarioMasActivo(null, null));
        System.out.println("Top 3 por valor:");
        for (Transaccion t : analitica.transaccionesMayorValor(3, null, null)) {
            System.out.println("  " + t);
        }

        System.out.println("=== Analitica con rango de fechas ===");
        LocalDateTime ayer = LocalDateTime.now().minusDays(1);
        LocalDateTime manana = LocalDateTime.now().plusDays(1);
        System.out.println("Rango de hoy -> monto: " + analitica.montoTotalMovilizado(ayer, manana));
        LocalDateTime pasadoLejano = LocalDateTime.now().minusYears(1);
        System.out.println("Rango del anio pasado -> monto: "
                + analitica.montoTotalMovilizado(pasadoLejano, ayer)
                + " | usuario mas activo: " + analitica.usuarioMasActivo(pasadoLejano, ayer)
                + " | aristas del grafo: " + grafos.grafoUsuarios(pasadoLejano, ayer).numAristas());

        System.out.println("=== Casos invalidos ===");
        probar(() -> grafos.enviosDe("U99"));
        probar(() -> analitica.billeterasMasUsadas(0, null, null));
        probar(() -> analitica.montoTotalMovilizado(manana, ayer));
        probar(() -> grafos.caminoEntreUsuarios("U1", "U99"));
        try {
            g.agregarArista("U1", "U1", 10);
            System.out.println("ERROR: debio fallar");
        } catch (IllegalArgumentException e) {
            System.out.println("Error esperado: " + e.getMessage());
        }
    }

    private static void probar(Runnable accion) {
        try {
            accion.run();
            System.out.println("ERROR: debio fallar");
        } catch (OperacionException e) {
            System.out.println("Error esperado: " + e.getMessage());
        }
    }
}

