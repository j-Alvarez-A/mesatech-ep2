export const msalConfig = {
    auth: {
        clientId: "7ec7e5ba-9693-4758-9e2f-73c0900cd68d",

        authority:
            "https://login.microsoftonline.com/a2d774d1-6d61-457f-8298-3217272b8234",

        // Usa la URL desde donde se abre la app (localhost o la URL https de Cloudflare).
        // Esa URL debe estar registrada como Redirect URI (SPA) en Entra ID.
        redirectUri: window.location.origin + "/"
    },

    cache: {
        cacheLocation: "sessionStorage"
    }
};

export const loginRequest = {
    scopes: ["openid", "profile", "email"]
};

export const apiRequest = {
    scopes: [
        "api://7ec7e5ba-9693-4758-9e2f-73c0900cd68d/access_as_user"
    ]
};