package servicio;

import modelo.Billetera;
import modelo.EstadoBilletera;
import modelo.TipoBilletera;
import modelo.Usuario;

import java.util.List;

public class BilleteraServicio {

    private final RepositorioDatos repo;
    private int contador = 0;

    public BilleteraServicio(RepositorioDatos repo) {
        this.repo = repo;
    }

    public Billetera crear(String idUsuario, String nombre, TipoBilletera tipo) {
        Usuario u = repo.getUsuarios().get(idUsuario);
        if (u == null) {
            throw new OperacionException("No existe el usuario " + idUsuario);
        }
        if (nombre == null || nombre.isBlank()) {
            throw new OperacionException("El nombre de la billetera es obligatorio");
        }
        if (tipo == null) {
            throw new OperacionException("El tipo de billetera es obligatorio");
        }

        String codigo = "B" + (++contador);
        Billetera b = new Billetera(codigo, nombre.trim(), tipo, idUsuario);

        u.agregarBilletera(b);
        repo.getBilleteras().put(codigo, b);
        return b;
    }

    public Billetera buscar(String codigo) {
        return repo.getBilleteras().get(codigo);
    }

    public Billetera obtener(String codigo) {
        Billetera b = repo.getBilleteras().get(codigo);
        if (b == null) {
            throw new OperacionException("No existe la billetera " + codigo);
        }
        return b;
    }

    public double consultarSaldo(String codigo) {
        return obtener(codigo).getSaldo();
    }

    public List<Billetera> listarDeUsuario(String idUsuario) {
        Usuario u = repo.getUsuarios().get(idUsuario);
        if (u == null) {
            throw new OperacionException("No existe el usuario " + idUsuario);
        }
        return u.getBilleteras();
    }

    public void cambiarEstado(String codigo, EstadoBilletera estado) {
        obtener(codigo).setEstado(estado);
    }
}