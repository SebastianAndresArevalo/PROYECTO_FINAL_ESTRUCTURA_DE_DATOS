package Pruebas;

import estructuras.ArbolAVL;
import modelo.Nivel;
import modelo.TipoBilletera;
import modelo.Usuario;
import servicio.*;

import java.util.List;

import static Pruebas.PruebaEtapa2.repo;

public class PruebaEtapa3 {

    public static void main(String[] args) {

        // 1. AVL solo (fuerza rotaciones)
        ArbolAVL<Integer> avl = new ArbolAVL<>(Integer::compare);
        int[] datos = {10, 20, 30, 40, 50, 25};
        for (int d : datos) {
            avl.insertar(d);
        }
        System.out.println("Altura AVL de prueba: " + avl.altura());
        System.out.println("Inorden: " + avl.inorden());

        // 2. Ranking de usuarios
        NotificacionServicio notif = new NotificacionServicio();
        PuntosServicio puntos = new PuntosServicio(notif);
        UsuarioServicio usuarios = new UsuarioServicio(repo, puntos);
        BilleteraServicio billeteras = new BilleteraServicio(repo);
        TransaccionServicio transacciones = new TransaccionServicio(repo, puntos, notif);

        String[] ids = {"U1", "U2", "U3", "U4", "U5", "U6"};
        String[] nombres = {"Ana", "Luis", "Marta", "Pedro", "Eva", "Juan"};
        for (int i = 0; i < ids.length; i++) {
            usuarios.registrar(ids[i], nombres[i], nombres[i].toLowerCase() + "@correo.com");
            billeteras.crear(ids[i], "Principal", TipoBilletera.GASTOS_DIARIOS);  // B1..B6
        }

        tx.recargar("B1", 20000);    // 200 pts
        tx.recargar("B2", 70000);    // 700 pts -> PLATA
        tx.recargar("B2", 10000);    // 100 + 5% bono = 105 -> 805
        tx.recargar("B3", 150000);   // 1500 -> ORO
        tx.recargar("B4", 600000);   // 6000 -> PLATINO
        tx.recargar("B5", 5000);     // 50
        tx.recargar("B6", 100000);   // 1000 -> PLATA

        System.out.println("--- Ranking ---");
        System.out.println("Ascendente: " + resumen(puntos.ordenadosAscendente()));
        System.out.println("Rango 500-1500: " + resumen(puntos.porRango(500, 1500)));
        System.out.println("Top 3: " + resumen(puntos.top(3)));
        System.out.println("Nivel PLATA: " + resumen(puntos.porNivel(Nivel.PLATA)));
        System.out.println("Nivel ORO: " + resumen(puntos.porNivel(Nivel.ORO)));
        System.out.println("Altura del ranking: " + puntos.altura());

        Usuario u2 = usuarios.obtener("U2");
        System.out.println("U2: " + u2.getPuntos() + " pts, nivel " + u2.getNivel());
        System.out.println("Notificaciones U2: " + u2.getNotificaciones().size());

        System.out.println("--- Altas y bajas ---");
        System.out.println("En el ranking: " + puntos.cantidad());
        usuarios.registrar("U7", "Sara", "sara@correo.com");
        System.out.println("Tras registrar U7: " + puntos.cantidad());
        usuarios.eliminar("U7");
        System.out.println("Tras eliminar U7: " + puntos.cantidad());
    }

    private static String resumen(List<Usuario> lista) {
        StringBuilder sb = new StringBuilder();
        for (Usuario u : lista) {
            sb.append(u.getId()).append("(").append(u.getPuntos()).append(") ");
        }
        return sb.toString().trim();
    }
}