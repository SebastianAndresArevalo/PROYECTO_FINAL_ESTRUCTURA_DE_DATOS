package servicio;

import modelo.Billetera;
import modelo.Usuario;

import java.util.ArrayList;
import java.util.List;

public class UsuarioServicio {

    private final PuntosServicio puntos;
    private final RepositorioDatos repo;

    public UsuarioServicio(RepositorioDatos repo, PuntosServicio puntos) {
        this.repo = repo;
        this.puntos = puntos;
    }

    public Usuario registrar(String id, String nombre, String correo) {
        validarDatos(id, nombre, correo);
        id = id.trim();

        if (repo.getUsuarios().containsKey(id)) {
            throw new OperacionException("Ya existe un usuario con id " + id);
        }

        Usuario u = new Usuario(id, nombre.trim(), correo.trim());
        repo.getUsuarios().put(id, u);
        puntos.agregar(u);
        return u;
    }

    public Usuario buscar(String id) {
        return repo.getUsuarios().get(id);
    }

    public Usuario obtener(String id) {
        Usuario u = repo.getUsuarios().get(id);
        if (u == null) {
            throw new OperacionException("No existe el usuario " + id);
        }
        return u;
    }

    public void modificar(String id, String nombre, String correo) {
        Usuario u = obtener(id);
        validarDatos(id, nombre, correo);
        u.setNombre(nombre.trim());
        u.setCorreo(correo.trim());
    }

    public void eliminar(String id) {
        Usuario u = obtener(id);

        for (Billetera b : u.getBilleteras()) {
            if (b.getSaldo() > 0) {
                throw new OperacionException("No se puede eliminar: el usuario tiene billeteras con saldo");
            }
        }

        for (Billetera b : u.getBilleteras()) {
            repo.getBilleteras().remove(b.getCodigo());
        }
        puntos.quitar(u);
        repo.getUsuarios().remove(id);
    }

    public List<Usuario> listar() {
        return new ArrayList<>(repo.getUsuarios().values());
    }

    private void validarDatos(String id, String nombre, String correo) {
        if (id == null || id.isBlank()) {
            throw new OperacionException("La identificacion es obligatoria");
        }
        if (nombre == null || nombre.isBlank()) {
            throw new OperacionException("El nombre es obligatorio");
        }
        if (correo == null || !correo.contains("@")) {
            throw new OperacionException("El correo no es valido");
        }
    }
}