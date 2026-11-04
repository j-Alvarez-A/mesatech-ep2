import './App.css';
import { AuthenticatedTemplate, UnauthenticatedTemplate, useMsal } from '@azure/msal-react';
import { loginRequest, apiRequest } from './authConfig';
import { useEffect, useState, useCallback } from 'react';
import Axios from 'axios';

// React solo accede a los servicios a traves de API Gateway -> BFF.
// Definir REACT_APP_API_URL en el archivo .env (ver .env.example)
const BFF_URL = process.env.REACT_APP_API_URL;

function App() {
    const { instance, accounts } = useMsal();
    
    // Estados de datos
    const [solicitudes, setSolicitudes] = useState([]);
    const [catalogo, setCatalogo] = useState([]);
    // EP2: categorias que ya llegaron a Solicitudes (copia local recibida por RabbitMQ)
    const [categoriasLocales, setCategoriasLocales] = useState([]);
    const [mensaje, setMensaje] = useState("");
    const [errorBackend, setErrorBackend] = useState("");

    // Formulario de Solicitud (Cliente)
    const [titulo, setTitulo] = useState("");
    const [descripcion, setDescripcion] = useState("");
    const [categoria, setCategoria] = useState("");
    const [prioridad, setPrioridad] = useState("");

    // Formulario de Catálogo (Admin)
    const [nuevoNombre, setNuevoNombre] = useState("");
    const [nuevoTipo, setNuevoTipo] = useState("CATEGORIA");

    const correo = accounts[0]?.username;
    const rolesToken = accounts[0]?.idTokenClaims?.roles;

    const rol = rolesToken?.includes("ROLE_ADMINISTRADOR") ? "Administrador"
              : rolesToken?.includes("ROLE_OPERADOR") ? "Operador"
              : rolesToken?.includes("ROLE_CLIENTE") ? "Cliente"
              : "Desconocido";

    // Función auxiliar para obtener el access token vigente
    const getAccessToken = useCallback(async () => {
        const tokenResponse = await instance.acquireTokenSilent({
            ...apiRequest,
            account: accounts[0]
        });
        return tokenResponse.accessToken;
    }, [instance, accounts]);

    // Cargar datos según el rol
    const cargarDatos = useCallback(async () => {
        if (!accounts[0]) return;
        try {
            const token = await getAccessToken();
            const headers = { Authorization: `Bearer ${token}` };

            // 1. Catálogo (prioridades para el Cliente, mantenimiento para el Admin)
            const resCat = await Axios.get(`${BFF_URL}/catalogo`, { headers });
            setCatalogo(resCat.data || []);

            // 1b. EP2: categorías disponibles en Solicitudes (copia local)
            const resLoc = await Axios.get(`${BFF_URL}/categorias-locales`, { headers });
            setCategoriasLocales(resLoc.data || []);

            // 2. Solicitudes según rol
            if (rol === "Cliente") {
                const resSol = await Axios.get(`${BFF_URL}/solicitudes/mias`, { headers });
                setSolicitudes(resSol.data || []);
            } else if (rol === "Operador" || rol === "Administrador") {
                const resSol = await Axios.get(`${BFF_URL}/solicitudes`, { headers });
                setSolicitudes(resSol.data || []);
            }
        } catch (err) {
            console.error("Error cargando datos:", err);
            setErrorBackend("Error al comunicar con los servicios a través del BFF");
        }
    }, [accounts, rol, getAccessToken]);

    useEffect(() => {
        if (accounts.length > 0) {
            cargarDatos();
        }
    }, [accounts, cargarDatos]);

    // Handler: Crear Solicitud (Cliente)
    const handleCrearSolicitud = async (e) => {
        e.preventDefault();
        try {
            const token = await getAccessToken();
            setMensaje(""); setErrorBackend("");
            // EP2: se envía el id de la categoría local (id original de Catálogo)
            await Axios.post(`${BFF_URL}/solicitudes`, 
                { titulo, descripcion, categoriaId: Number(categoria), prioridad },
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setMensaje("Solicitud enviada exitosamente");
            setTitulo("");
            setDescripcion("");
            setCategoria("");
            cargarDatos();
        } catch (err) {
            setErrorBackend(err.response?.data?.error || "Error al registrar solicitud");
        }
    };

    // Handler: Cambiar Estado (Operador / Admin)
    const handleCambiarEstado = async (id, nuevoEstado) => {
        try {
            const token = await getAccessToken();
            await Axios.put(`${BFF_URL}/solicitudes/${id}/estado`, 
                { estado: nuevoEstado },
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setMensaje(`Solicitud #${id} actualizada a ${nuevoEstado}`);
            cargarDatos();
        } catch (err) {
            setMensaje("");
            setErrorBackend(err.response?.data?.error || "Error al cambiar el estado");
        }
    };

    // Handler: Agregar ítem al Catálogo (Admin)
    const handleAgregarCatalogo = async () => {
        if (!nuevoNombre.trim()) return;
        try {
            const token = await getAccessToken();
            setMensaje(""); setErrorBackend("");
            const res = await Axios.post(`${BFF_URL}/catalogo`, 
                { nombre: nuevoNombre, tipo: nuevoTipo },
                { headers: { Authorization: `Bearer ${token}` } }
            );
            setMensaje(`${res.data?.mensaje || "Ítem guardado"} (id ${res.data?.id})`);
            setNuevoNombre("");
            cargarDatos();
            // El consumo es asíncrono: se vuelve a consultar a los pocos segundos
            setTimeout(cargarDatos, 4000);
        } catch (err) {
            // 502: guardada en RDS Catálogo pero sin confirmación de RabbitMQ
            // 403: el usuario no tiene rol Administrador
            const data = err.response?.data;
            setErrorBackend(data?.error || (err.response?.status === 403
                ? "No tiene permisos para modificar el catálogo"
                : "Error al guardar en el catálogo"));
            cargarDatos();
        }
    };

    return (
        <div className="container" style={{ padding: "30px" }}>
            <h2>MesaTech Cloud - Plataforma de Solicitudes</h2>

            <UnauthenticatedTemplate>
                <div className="alert alert-info mt-3">Inicie sesión para acceder al sistema.</div>
                <button onClick={() => instance.loginRedirect(loginRequest)} className="btn btn-primary">
                    Iniciar sesión con Microsoft
                </button>
            </UnauthenticatedTemplate>

            <AuthenticatedTemplate>
                <div className="d-flex justify-content-between align-items-center mt-3 p-3 bg-light rounded">
                    <div>
                        <strong>Usuario:</strong> {accounts[0]?.name} ({correo})<br />
                        <strong>Rol activo:</strong> <span className="badge bg-success">{rol}</span>
                    </div>
                    <button onClick={() => instance.logoutRedirect()} className="btn btn-outline-danger btn-sm">
                        Cerrar sesión
                    </button>
                </div>

                {mensaje && <div className="alert alert-success mt-3 py-2">{mensaje}</div>}
                {errorBackend && <div className="alert alert-danger mt-3 py-2">{errorBackend}</div>}

                {/* --- VISTA CLIENTE --- */}
                {rol === "Cliente" && (
                    <div className="row mt-4">
                        <div className="col-md-5">
                            <div className="card shadow-sm">
                                <div className="card-header bg-primary text-white">Nueva Solicitud</div>
                                <div className="card-body">
                                    <form onSubmit={handleCrearSolicitud}>
                                        <div className="mb-2">
                                            <label className="form-label">Título</label>
                                            <input type="text" className="form-control" required
                                                value={titulo} onChange={(e) => setTitulo(e.target.value)} />
                                        </div>
                                        <div className="mb-2">
                                            <label className="form-label">Descripción</label>
                                            <textarea className="form-control" rows="2" required
                                                value={descripcion} onChange={(e) => setDescripcion(e.target.value)}></textarea>
                                        </div>
                                        <div className="row mb-3">
                                            <div className="col">
                                                <label className="form-label">Categoría</label>
                                                <select className="form-select" required value={categoria} onChange={(e) => setCategoria(e.target.value)}>
                                                    <option value="">Seleccione...</option>
                                                    {categoriasLocales.map(c => (
                                                        <option key={c.categoriaId} value={c.categoriaId}>{c.nombre}</option>
                                                    ))}
                                                </select>
                                            </div>
                                            <div className="col">
                                                <label className="form-label">Prioridad</label>
                                                <select className="form-select" required value={prioridad} onChange={(e) => setPrioridad(e.target.value)}>
                                                    <option value="">Seleccione...</option>
                                                    {catalogo.filter(c => c.tipo === "PRIORIDAD").map(c => (
                                                        <option key={c.id} value={c.nombre}>{c.nombre}</option>
                                                    ))}
                                                </select>
                                            </div>
                                        </div>
                                        <button type="submit" className="btn btn-success w-100">Enviar Solicitud</button>
                                    </form>
                                </div>
                            </div>
                        </div>

                        <div className="col-md-7">
                            <div className="card shadow-sm">
                                <div className="card-header bg-secondary text-white">Mis Solicitudes</div>
                                <div className="card-body">
                                    <table className="table table-sm table-hover">
                                        <thead>
                                            <tr>
                                                <th>ID</th>
                                                <th>Título</th>
                                                <th>Categoría</th>
                                                <th>Estado</th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {solicitudes.map(s => (
                                                <tr key={s.id}>
                                                    <td>#{s.id}</td>
                                                    <td>{s.titulo}</td>
                                                    <td>{s.categoria}</td>
                                                    <td><span className="badge bg-info text-dark">{s.estado}</span></td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>
                            </div>
                        </div>
                    </div>
                )}

                {/* --- VISTA OPERADOR --- */}
                {rol === "Operador" && (
                    <div className="card shadow-sm mt-4">
                        <div className="card-header bg-dark text-white">Gestión de Solicitudes Operativas</div>
                        <div className="card-body">
                            <table className="table table-hover align-middle">
                                <thead>
                                    <tr>
                                        <th>ID</th>
                                        <th>Solicitante</th>
                                        <th>Título</th>
                                        <th>Estado</th>
                                        <th>Acción de Flujo</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {solicitudes.map(s => (
                                        <tr key={s.id}>
                                            <td>#{s.id}</td>
                                            <td>{s.usuarioSolicitante}</td>
                                            <td>{s.titulo}</td>
                                            <td>
                                                <span className={`badge ${s.estado === 'RESUELTA' ? 'bg-success' : s.estado === 'EN_PROCESO' ? 'bg-warning text-dark' : 'bg-secondary'}`}>
                                                    {s.estado}
                                                </span>
                                            </td>
                                            <td>
                                                {s.estado === "CREADA" && (
                                                    <button onClick={() => handleCambiarEstado(s.id, "EN_PROCESO")} className="btn btn-sm btn-outline-warning">
                                                        Tomar (En Proceso)
                                                    </button>
                                                )}
                                                {s.estado === "EN_PROCESO" && (
                                                    <button onClick={() => handleCambiarEstado(s.id, "RESUELTA")} className="btn btn-sm btn-outline-success">
                                                        Finalizar (Resuelta)
                                                    </button>
                                                )}
                                                {s.estado === "RESUELTA" && <span className="text-muted small">Sin acciones</span>}
                                            </td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>
                        </div>
                    </div>
                )}

                {/* --- VISTA ADMINISTRADOR --- */}
                {rol === "Administrador" && (
                    <div className="mt-4">
                        <div className="card shadow-sm mb-4">
                            <div className="card-header bg-dark text-white">Supervisión Global de Solicitudes</div>
                            <div className="card-body">
                                <table className="table table-sm table-striped">
                                    <thead>
                                        <tr>
                                            <th>ID</th>
                                            <th>Solicitante</th>
                                            <th>Título</th>
                                            <th>Prioridad</th>
                                            <th>Estado</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {solicitudes.map(s => (
                                            <tr key={s.id}>
                                                <td>#{s.id}</td>
                                                <td>{s.usuarioSolicitante}</td>
                                                <td>{s.titulo}</td>
                                                <td>{s.prioridad}</td>
                                                <td><span className="badge bg-secondary">{s.estado}</span></td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>

                        <div className="card shadow-sm">
                            <div className="card-header bg-success text-white">Mantenimiento de Catálogo</div>
                            <div className="card-body">
                                <div className="d-flex gap-2 mb-3">
                                    <input type="text" className="form-control w-25" placeholder="Nombre ítem..."
                                        value={nuevoNombre} onChange={(e) => setNuevoNombre(e.target.value)} />
                                    <select className="form-select w-25" value={nuevoTipo} onChange={(e) => setNuevoTipo(e.target.value)}>
                                        <option value="CATEGORIA">CATEGORIA</option>
                                        <option value="PRIORIDAD">PRIORIDAD</option>
                                    </select>
                                    <button onClick={handleAgregarCatalogo} className="btn btn-success">Guardar Ítem</button>
                                </div>
                                <div className="d-flex justify-content-between align-items-center w-75 mb-2">
                                    <small className="text-muted">
                                        Las categorías nuevas se copian a Solicitudes de forma asíncrona (RabbitMQ).
                                    </small>
                                    <button onClick={cargarDatos} className="btn btn-sm btn-outline-secondary">Actualizar</button>
                                </div>
                                <table className="table table-sm table-bordered w-75">
                                    <thead>
                                        <tr>
                                            <th>ID</th>
                                            <th>Nombre</th>
                                            <th>Tipo</th>
                                            <th>Disponible en Solicitudes</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {catalogo.map(c => (
                                            <tr key={c.id}>
                                                <td>{c.id}</td>
                                                <td>{c.nombre}</td>
                                                <td><span className="badge bg-light text-dark border">{c.tipo}</span></td>
                                                <td>
                                                    {c.tipo !== "CATEGORIA" ? <span className="text-muted">—</span>
                                                        : categoriasLocales.some(l => l.categoriaId === c.id)
                                                            ? <span className="badge bg-success">✓ Disponible</span>
                                                            : <span className="badge bg-warning text-dark">⏳ Pendiente</span>}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        </div>
                    </div>
                )}
            </AuthenticatedTemplate>
        </div>
    );
}

export default App;