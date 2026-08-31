# BookSocial — Guía de Desarrollo (Frontend)

> Para la arquitectura del backend (servicios, CQRS, RabbitMQ, seguridad), ver [GUIDE-BACKEND.md](./GUIDE-BACKEND.md).

---

BookSocial es una red social de libros con arquitectura de microservicios. El frontend es una **Angular 21 SPA** que se comunica vía HTTP con el Gateway (`:8080`), quien enruta a los microservicios (Identity `:8081`, User `:8082`, Book `:8083`, etc.). Las notificaciones en tiempo real llegan por **WebSocket STOMP** directamente al `notification-service` (`:8087`). El backend usa CQRS (PostgreSQL + MongoDB) y RabbitMQ para eventos asíncronos. Para todos los detalles del backend, ver [GUIDE-BACKEND.md](./GUIDE-BACKEND.md).

---

## Cómo usar esta guía

La guía está organizada en **bloques cronológicos**: cada bloque se construye sobre el anterior, como un curso progresivo. Si empiezas desde cero, sigue el orden recomendado.

**Para aprender haciendo**: cada bloque incluye código real del proyecto, explicaciones de _por qué_ se toma cada decisión, y pasos de verificación. Ejecuta el código mientras lees.

**Para consultar después**: la tabla de contenidos te permite saltar directamente al bloque que necesites. Los apéndices al final consolidan patrones repetidos (seguridad, Docker).

**Nivel de detalle**: se mantiene el código esencial (componentes, servicios, modelos, controladores, configuración) con explicaciones conceptuales. Los patrones de seguridad repetidos en varios servicios se consolidan en el [GUIDE-BACKEND.md](./GUIDE-BACKEND.md) para evitar redundancia.

---

## Tabla de contenidos

| Bloque                                            | Tema                                          | Fase       |
| ------------------------------------------------- | --------------------------------------------- | ---------- |
| [3. Frontend Angular](#bloque-3--frontend-angular) | SPA, signals, OAuth2 flow, reset | Fase 1     |
| [10. i18n](#bloque-10--i18n-internacionalización-angular) | @angular/localize, en/es/pt             | i18n       |
| [12. Frontend feed + notificaciones](#bloque-12--fase-10-frontend-del-feed-social--notificaciones-en-tiempo-real) | `/feed` SPA, campana, STOMP, proxy, People | Fase 10 |
| [C. Operación — Frontend](#apéndice-c--operación-despliegue-logs-y-depuración) | Dev server, build, i18n          | Referencia |

---

## Bloque 3 — Frontend Angular

Con el backend (Identity Service + Gateway) funcionando, el siguiente paso es construir la interfaz de usuario. Angular 21 con standalone components, signals y lazy loading nos permite crear una SPA moderna con autenticación OAuth2.

**Qué construiremos**: login/registro con formularios reactivos, login con Google, página principal con perfil de usuario, interceptor JWT automático con refresh, y guardas de ruta.

**Por qué Angular signals**: reemplazan a `BehaviorSubject` para el estado de sesión (`isAuthenticated`, `accessToken`). Un signal se lee como función (`auth.isAuthenticated()`) tanto en TypeScript como en plantillas, sin necesidad de suscripciones.

**Ficha de la aplicación**

|                 |                                                                                                                                                                    |
| --------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| Puerto          | `4200` (`ng serve` con proxy a `:8080`)                                                                                                                            |
| Stack           | Angular 21, standalone components, zoneless + signals, Reactive Forms                                                                                              |
| Responsabilidad | SPA: sesión (login/registro/Google), interceptor JWT con refresh, guardas de ruta y páginas del dominio (catálogo, libros, autores, reseñas, estanterías — Fase 8) |
| Estructura      | `core/` (services, models, guards), `features/<nombre>/` lazy-loaded, `shared/components/nav`                                                                      |
| Convención      | Cada feature exporta `routes.ts` con `loadComponent`; UI en inglés (i18n planeado)                                                                                 |

### 3.1 — Creación del proyecto y estructura

El frontend se genera con la CLI de Angular 21:

```powershell
ng new frontend --style scss
```

Estructura de `frontend/src/app`:

```
core/
  guards/          # auth.guard, guest.guard
  interceptors/    # auth.interceptor
  models/          # auth.models, user.models, api-error.models
  services/        # auth.service, user.service
features/
  auth/            # login, register, oauth2-callback
  home/            # home (página principal)
shared/
  pipes/           # capitalize, initials
environments/      # configuraciones por entorno
```

#### Punto de entrada

Angular 21 usa **standalone components** (no se crean `NgModule`). El arranque está en `main.ts`:

```ts
import { bootstrapApplication } from "@angular/platform-browser";
import { appConfig } from "./app/app.config";
import { App } from "./app/app";

bootstrapApplication(App, appConfig).catch((err) => console.error(err));
```

`index.html` solo contiene `<app-root></app-root>`, y el componente raíz `App` (`app.html`) se limita a un `<router-outlet />`:

```html
<router-outlet />
```

Todo lo demás son rutas y componentes cargados por el router.

#### Alias de rutas (tsconfig)

`tsconfig.json` define alias para evitar importaciones relativas largas (`../../../core/...`):

```json
"paths": {
  "@core/*":     ["./src/app/core/*"],
  "@features/*": ["./src/app/features/*"],
  "@shared/*":   ["./src/app/shared/*"],
  "@env/*":      ["./src/environments/*"]
}
```

Así, en cualquier componente: `import { AuthService } from '@core/services/auth.service'`.

Además, el compilador está en modo `strict` (incluido `strictTemplates`), por lo que **las plantillas HTML también se tipan**: un error en `user.age`, un pipe mal escrito o un control inexistente se detectan en build, no en producción.

#### `proxy.conf.json`: evitar CORS en desarrollo

Durante `ng serve`, el frontend está en `:4200` y el backend en `:8080`. Para no tener problemas de CORS, el proxy de desarrollo reenvía las llamadas al backend:

```json
{
  "^/auth(/|$)": {
    "target": "http://localhost:8080",
    "changeOrigin": true,
    "secure": false
  },
  "^/users(/|$)": {
    "target": "http://localhost:8080",
    "changeOrigin": true,
    "secure": false
  }
}
```

El código Angular llama a `/auth/login` (ruta relativa) y el proxy lo redirige a `http://localhost:8080/auth/login`. En producción, el frontend se serviría bajo el mismo dominio del gateway y no haría falta proxy.

> **Las claves llevan frontera de ruta** (`^/auth(/|$)`) porque el matching del proxy es por prefijo: `/author/5` (página de autor de la Fase 8) empieza por `/auth`, y con la clave plana el dev server habría enviado esa navegación al gateway → 401 al refrescar (error 5 en la sección 7.4). La misma técnica se aplica a todas las claves (`/books`, `/authors`, `/reviews`, ...). Si añades un prefijo nuevo al proxy, usa siempre esta forma.

#### `googleAuthUrl`: la excepción del gateway

```ts
export const environment = {
  production: false,
  googleAuthUrl: "http://localhost:8081/oauth2/authorization/google",
};
```

El login de Google **no pasa por el gateway**: apunta directamente a `:8081`. Es el comportamiento esperado documentado en el [GUIDE-BACKEND.md](./GUIDE-BACKEND.md) (Bloque 1.6 — el gateway devuelve `401` en esa ruta).

> El fichero se define dos veces (`environments.ts` y `environments.development.ts`). En Angular, `ng serve` usa el de desarrollo, y al compilar para producción se sustituye por el de producción (`fileReplacements` en `angular.json`). De esta forma, el valor puede cambiar por entorno sin tocar el código.

#### Enrutado y lazy loading

Las rutas raíz se definen en `app.routes.ts` combinando las de cada feature:

```ts
export const routes: Routes = [
  { path: "", pathMatch: "full", redirectTo: "home" },
  ...authRoutes, // /login, /register, /oauth2/callback
  ...homeRoutes, // /home
  { path: "**", redirectTo: "home" }, // ruta comodín
];
```

Cada feature exporta sus propias rutas y usa **carga diferida** (`loadComponent`): el código del componente solo se descarga cuando el usuario navega a esa ruta. Por ejemplo `features/auth/routes.ts`:

```ts
{
  path: 'login',
  canActivate: [guestGuard],                       // si ya está logueado → /home
  loadComponent: () => import('./login/login').then((m) => m.Login),
},
```

`loadComponent` con `import()` genera un **chunk separado por feature**: el bundle inicial es pequeño y las páginas de auth/home se descargan solo al visitarlas. Las guardas (`canActivate`) se evalúan antes de cargar el componente.

### 3.2 — `AuthService`: estado de sesión en el cliente

#### Modelos de datos (interfaces TypeScript)

Los modelos replican los DTOs del backend para que TypeScript tipifique las respuestas:

```ts
// auth.models.ts
export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  birthDate: string;
}

export interface TokenResponse {
  accessToken: string;
  refreshToken: string;
  expiresIn: number; // segundos (900 = 15 min)
  tokenType: string; // "Bearer"
}

// user.models.ts
export interface UserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  age: number | null;
  roles: string[];
}
```

`age` se tipa como `number | null` porque los usuarios de Google no tienen `birth_date` (el backend devuelve `-1`). Además existe `api-error.models.ts` con la forma de los errores del `GlobalExceptionHandler` (`{ error, message?, fields? }`).

El estado de la sesión se mantiene en memoria con **signals** de Angular. Una señal es un **contenedor** que encapsula un valor y **notifica** a los consumidores interesados ​​cuando dicho valor cambia:

```ts
private readonly accessTokenStore = signal<string | null>(null);
private readonly authenticatedStore = signal<boolean>(false);

readonly accessToken = this.accessTokenStore.asReadonly();        // solo lectura pública
readonly isAuthenticated = this.authenticatedStore.asReadonly();
```

Métodos:

- `login(credentials)` / `register(payload)`: `POST` a `/auth/login` o `/auth/register` y aplican el token de la respuesta.
- `forgotPassword(email)` / `resetPassword(token, newPassword)`: `POST` a `/auth/forgot-password` y `/auth/reset-password` (usados por las páginas `/forgot-password` y `/reset-password`).
- `refresh()`: `POST /auth/refresh` con `withCredentials: true` (para enviar la cookie httpOnly).
- `logout()`: `POST /auth/logout` con cookie y limpia el estado.
- `applyOAuthToken(accessToken)`: usado por el callback de Google, que recibe el token en el fragmento de la URL.
- `restoreSession()`: intenta `refresh()` al arrancar la app; si falla, limpia la sesión.
- `applyToken(tokens)`: guarda el token y dispara `materializeProfile()` → `GET /profiles/me` (fire-and-forget) para que el perfil del usuario esté listo en el directorio People al entrar (login, registro, OAuth2 y refresh).

> El access token vive en **memoria** (nunca en `localStorage`), lo que reduce el riesgo de robo por XSS. La sesión "larga" se restaura con la cookie httpOnly del refresh.

#### `UserService`

El segundo servicio del core es trivial pero ilustra el patrón: **cada dominio de la API tiene su servicio** que envuelve las llamadas HTTP tipadas.

```ts
import { inject, Injectable } from "@angular/core";

@Injectable({
  providedIn: "root",
})
export class UserService {
  private readonly http = inject(HttpClient);

  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>("/users/me");
  }
}
```

- `providedIn: 'root'`: crea un **singleton** inyectable en toda la app (no hace falta registrarlo en ningún módulo).
- **Inyección sin constructor**: `inject(HttpClient)` asigna la dependencia a un campo usando la función `inject()`, en vez de `constructor(private readonly http: HttpClient)`. Es el patrón de Angular standalone: al no declarar constructor, el componente o servicio es más simple y las dependencias son visibles como campos. Se usa igualmente en componentes, guardas, interceptores y el `app.config.ts`.

  ```ts
  // El patrón se repite en todos los componentes del frontend
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  ```

  > Los dos estilos son válidos; `inject()` solo puede usarse en el contexto de inyección (creación de un componente/servicio, o en funciones como guardas e interceptores). La ventaja principal: no hace falta el constructor y el código es declarativo.

- La ruta es relativa (`/users/me`): en desarrollo el proxy la reenvía a `:8080`; en producción se serviría bajo el mismo dominio que el gateway.

### 3.3 — Interceptor JWT y guardas de ruta

#### `AuthInterceptor` (`HttpInterceptorFn`)

Se registra con `provideHttpClient(withInterceptors([authInterceptor]))` en `app.config.ts`. Hace tres cosas:

1. **No toca los endpoints de auth**: `/auth/login`, `/auth/register`, `/auth/refresh`, `/auth/logout`, `/auth/forgot-password` y `/auth/reset-password` no llevan access token, por lo que no se interceptan.
2. **Clona y adjunta el `Bearer` token** a cualquier otra petición.
3. **Manejo del 401 (refresh automático)**: si una petición protegida devuelve `401` y la sesión existe, llama a `auth.refresh()` y **reintenta** la petición original con el token nuevo:

```ts
return auth.refresh().pipe(
  catchError(() => {
    auth.clearSession();
    return throwError(() => error);
  }),
  switchMap(() => {
    const freshToken = auth.accessToken(); // lectura del signal (llamada de función)
    return next(
      freshToken
        ? req.clone({ setHeaders: { Authorization: `Bearer ${freshToken}` } })
        : req,
    );
  }),
);
```

Antes de reintentar, el interceptor también lee el signal: `const token = auth.accessToken();` y lo adjunta como `Authorization: Bearer ...`.

Si el refresh también falla, limpia la sesión y propaga el error (el usuario tendrá que volver a loguearse).

#### Guardas

- **`authGuard`**: si `!auth.isAuthenticated()` redirige a `/login` → protege rutas privadas (p.ej. `home`).
- **`guestGuard`**: si `auth.isAuthenticated()` redirige a `/home` → evita que un usuario logueado vea login/registro.

```ts
// auth.guard.ts — las guardas leen el signal como función
export const authGuard = (): boolean => {
  const auth = inject(AuthService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) {
    router.navigate(["/login"]);
    return false;
  }
  return true;
};
```

#### `app.config.ts`: restauración de sesión al arrancar

```ts
provideAppInitializer(() => inject(AuthService).restoreSession());
```

Antes de renderizar la app, Angular intenta renovar la sesión con el refresh token de la cookie. Así, al hacer F5 la sesión sobrevive.

### 3.4 — Páginas y flujo OAuth2

- **`login`**: formulario reactivo construido con `FormBuilder` y bindeado a la plantilla con `[formGroup]` / `formControlName`. La validación es **reactiva**: los `Validators` se declaran en el modelo, no en el HTML.

  ```ts
  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });

  errorMessage = signal<string>('');
  loading = signal<boolean>(false);

  submit(): void {
    if (this.form.invalid) return;              // el botón además se deshabilita en el HTML
    this.loading.set(true);
    this.errorMessage.set('');
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.router.navigate(['/home']),
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.errorMessage.set(
          error.status === 401
            ? 'Invalid email or password.'
            : 'Unexpected error. Please try again.',
        );
      },
    });
  }

  loginWithGoogle(): void {
    window.location.href = environment.googleAuthUrl;   // inicia el flujo OAuth2 en el navegador
  }
  ```

  `nonNullable.group(...)` crea controles que nunca devuelven `null` (mejor tipado). En la plantilla (`login.html`), el botón de envío muestra "Logging in..." mientras carga y se deshabilita con `[disabled]="form.invalid"`; el error se muestra con `@if (errorMessage())`.

- **`register`**: igual que login, pero con un **validador personalizado** para la fecha de nacimiento (no puede ser hoy ni futura):

  ```ts
  const pastDate: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) return null;
    const date = new Date(value);
    const todayStart = new Date().setHours(0, 0, 0, 0);
    return date.getTime() >= todayStart ? { notPast: true } : null;
  };

  readonly form = this.fb.nonNullable.group({
    firstName: ['', Validators.required],
    lastName: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
    birthDate: ['', [Validators.required, pastDate]],
  });
  ```

  Los límites del password (`minLength(8)`, `maxLength(72)`) replican el `@Size(min = 8, max = 72)` del `RegisterRequest` (ver [GUIDE-BACKEND.md](./GUIDE-BACKEND.md), Bloque 1.3): la validación ocurre en **ambos lados** (defensa en profundidad). Además mapea más códigos del `GlobalExceptionHandler`:

  ```ts
  error.status === 409
    ? "There is already an account with that email."
    : error.status === 400
      ? "Please check the form data."
      : "Unexpected error. Please try again.";
  ```

- **`oauth2-callback`**: es la ruta a la que redirige el backend tras el login de Google (`app.oauth2.frontend-redirect-uri`, ver [GUIDE-BACKEND.md](./GUIDE-BACKEND.md), Bloque 1.6). El token llega en el **fragmento** de la URL, nunca en la ruta. El componente lo lee, lo aplica y limpia la URL:

  ```ts
  ngOnInit(): void {
    const params = new URLSearchParams(window.location.hash.replace(/^#/, ''));
    history.replaceState(null, '', '/oauth2/callback');   // quita el token de la barra de direcciones

    const token = params.get('access_token');
    const error = params.get('error');

    if (token) {
      this.auth.applyOAuthToken(token);
      this.router.navigate(['/home']);
      return;
    }

    this.errorMessage = error === 'access_denied'
      ? 'You have canceled the Google login.'
      : 'Failed to login with Google.';
    this.router.navigate(['/login'], { queryParams: { googleError: this.errorMessage } });
  }
  ```

  `history.replaceState` borra el fragmento `#access_token=...` de la URL (evita que quien vea la pantalla copie el token) y el login lee el error con `route.snapshot.queryParamMap.get('googleError')`.

- **`home`**: llama a `userService.me()` (`GET /users/me` vía gateway) para mostrar el perfil y ofrece logout. El componente muestra `loading` mientras carga, `error` si falla, y utiliza los pipes `InitialsPipe` (iniciales para el avatar) y `CapitalizePipe` (capitaliza nombres):

  ```ts
  ngOnInit(): void {
    this.userService.me().subscribe({
      next: (user) => { this.user.set(user); this.loading.set(false); },
      error: () => { this.loading.set(false); this.error.set('Failed to load your profile.'); },
    });
  }

  logout(): void {
    this.auth.logout().subscribe({ next: () => this.router.navigate(['/login']) });
  }
  ```

```
Sin sesión:  /login → (Google) → :8081 → :4200/oauth2/callback#access_token=... → /home
Con sesión:  /home → interceptor añade Bearer → gateway valida → me() responde
```

#### Plantillas con el control flow de Angular 21

Las plantillas usan las **nuevas estructuras de control** de Angular (`@if`, `@else if`, `@for`) en lugar de los antiguos `*ngIf`/`*ngFor`. En `home.html`:

```html
@if (loading()) {
<p>Loading profile…</p>
} @else if (error()) {
<p class="error">{{ error() }}</p>
} @else if (user()) {
<section class="profile">
  <div class="avatar">
    {{ user()?.firstName + ' ' + user()?.lastName | initials }}
  </div>
  <h2>{{ user()?.firstName }} {{ user()?.lastName }}</h2>
  <dl>
    <dt>Email</dt>
    <dd>{{ user()?.email }}</dd>
    <dt>Age</dt>
    <dd>{{ user()?.age === -1 ? '--' : user()?.age }}</dd>
    <dt>Roles</dt>
    <dd>
      @for (role of user()?.roles; track role) {
      <span class="role">{{ role | capitalize }}</span>
      }
    </dd>
  </dl>
</section>
}
```

Detalles:

- **`@if` / `@else if`**: se renderiza solo el bloque cuya condición se cumple (loading → error → usuario). Las condiciones leen signals como funciones: `loading()`, `error()`, `user()`.
- **`@for ... track role`**: el `track` proporciona a Angular la clave de identidad de cada elemento de la lista (clave para el rendimiento al re-renderizar).
- **Pipes**: `initials` muestra las iniciales del nombre (`InitialsPipe`) y `capitalize` pone la primera letra del rol en mayúscula (`CapitalizePipe`).
- **`user()?.age === -1 ? '--' : user()?.age`**: el operador `?.` (optional chaining) es necesario porque `user()` puede ser `null`. Si la edad es `-1` (usuarios de Google sin `birth_date`), muestra `--`.

El mismo patrón `@if` se usa en `login.html` para mostrar `errorMessage()`, y el botón se deshabilita con `[disabled]="form.invalid"` para evitar envíos de formularios inválidos.

### 3.5 — Reset de contraseña

Dos páginas lazy para recuperar la contraseña. Consumen los endpoints del identity-service (ver [GUIDE-BACKEND.md](./GUIDE-BACKEND.md), 1.9). El interceptor Angular **no** les añade `Authorization`: ambos endpoints son `permitAll` y el flujo ocurre antes de tener sesión.

#### Rutas (`features/auth/routes.ts`)

```typescript
{
  path: 'forgot-password',
  loadComponent: () => import('./forgot-password/forgot-password').then((m) => m.ForgotPassword),
},
{
  path: 'reset-password',
  loadComponent: () => import('./reset-password/reset-password').then((m) => m.ResetPassword),
},
```

A diferencia de `/login` y `/register` (que usan `guestGuard`), estas dos rutas **no llevan guard**: deben ser accesibles para un usuario no autenticado que abre el enlace del email. Además no se guardan en historial de ruta con parámetros: el token viaja en el **query string** (`?token=…`), no en la ruta.

#### Servicio (`AuthService`)

```typescript
forgotPassword(email: string): Observable<void> {
  return this.http.post<void>('/auth/forgot-password', { email } as ForgotPasswordRequest);
}

resetPassword(token: string, newPassword: string): Observable<void> {
  return this.http.post<void>('/auth/reset-password', { token, newPassword } as ResetPasswordRequest);
}
```

Con los modelos tipados `ForgotPasswordRequest { email }` y `ResetPasswordRequest { token, newPassword }`. Ambos devuelven `Observable<void>` (el backend responde `200` sin cuerpo) y no adjuntan header `Authorization`.

#### Componente `ForgotPassword`

```typescript
export class ForgotPassword {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
  });

  loading = signal<boolean>(false);
  sent = signal<boolean>(false);
  errorMessage = signal<string>('');

  submit(): void {
    if (this.form.invalid) return;
    this.loading.set(true);
    this.errorMessage.set('');
    this.auth.forgotPassword(this.form.getRawValue().email).subscribe({
      next: () => { this.loading.set(false); this.sent.set(true); },
      error: () => {
        this.loading.set(false);
        this.errorMessage.set($localize`:@@forgotErrorUnexpected:Unexpected error. Please try again.`);
      },
    });
  }
}
```

En `submit()` delega todo en `AuthService.forgotPassword`. Al completar (`next`) cambia `sent` a `true` y la plantilla intercambia el formulario por la pantalla de confirmación **"Check your email"**. En error muestra un mensaje genérico — el frontend **no** debe dar pistas de si el email existe (coherente con el backend, que siempre responde `200`).

#### Componente `ResetPassword`

```typescript
export class ResetPassword implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly route = inject(ActivatedRoute);

  token = '';

  readonly form = this.fb.nonNullable.group(
    {
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirm: ['', Validators.required],
    },
    {
      validators: (group) =>
        group.get('password')?.value === group.get('confirm')?.value
          ? null
          : { mismatch: true },
    },
  );

  loading = signal<boolean>(false);
  done = signal<boolean>(false);
  invalidToken = signal<boolean>(false);
  errorMessage = signal<string>('');

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) this.invalidToken.set(true);
  }

  submit(): void {
    if (this.form.invalid || !this.token) return;
    this.loading.set(true);
    this.errorMessage.set('');
    this.auth.resetPassword(this.token, this.form.getRawValue().password).subscribe({
      next: () => { this.loading.set(false); this.done.set(true); },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        const code = (error.error as { error?: string } | undefined)?.error;
        if (code === 'INVALID_TOKEN') this.invalidToken.set(true);
        else if (code === 'EXPIRED_TOKEN')
          this.errorMessage.set($localize`:@@resetErrorExpired:This link has expired. Please request a new one.`);
        else if (code === 'ALREADY_USED')
          this.errorMessage.set($localize`:@@resetErrorUsed:This link has already been used. Please request a new one.`);
        else
          this.errorMessage.set($localize`:@@resetErrorUnexpected:Unexpected error. Please try again.`);
      },
    });
  }
}
```

Puntos clave del componente de reset:

- **Lee el token en `ngOnInit`** del query string (`ActivatedRoute.snapshot.queryParamMap`). Si falta, marca `invalidToken` y no muestra el formulario ("Link not valid").
- **Doble campo con validación cruzada**: `password` (requerido, `minLength(8)`) + `confirm`, con un validador de grupo que añade el error `mismatch` si no coinciden.
- **Discrimina códigos de error del backend** leyendo `error.error.error`: `INVALID_TOKEN`, `EXPIRED_TOKEN` y `ALREADY_USED` producen mensajes distintos. Es el único punto donde el frontend traduce el estado del token a una UI específica.

#### Estados de la UI de `reset-password` (plantilla con `@if` / `@else if` / `@else`)

| Estado         | Condición                                                     | UI mostrada                                  |
| -------------- | ------------------------------------------------------------- | -------------------------------------------- |
| **Hecho**      | Respuesta `2xx`                                               | "Password updated" + enlace "Go to login"    |
| **Enlace no válido** | Query param `token` ausente, o backend `INVALID_TOKEN`  | "Link not valid" + enlace a `/forgot-password` |
| **Caducado**   | Backend `EXPIRED_TOKEN`                                       | Formulario + mensaje "This link has expired" |
| **Usado**      | Backend `ALREADY_USED`                                        | Formulario + mensaje "This link has already been used" |
| **Error inesperado** | Cualquier otro código                                  | Formulario + mensaje "Unexpected error"      |
| **Normal**     | Token presente, sin enviar                                    | Formulario (new password + confirm)          |

La plantilla usa el patrón `@if`/`@else if`/`@else` (igual que el resto de auth) para mostrar el bloque correcto, y el error `mismatch` solo aparece si el formulario ha sido "tocado".

#### Enlace desde el login

```html
<p class="forgot">
  <a routerLink="/forgot-password" i18n="@@loginForgotPassword">Forgot password?</a>
</p>
```

#### Verificación E2E

1. Desde `/login`, pulsar "Forgot password?" → `/forgot-password`.
2. Introducir un email (p. ej. `alice@test.com`) → "Check your email" (frontend muestra confirmación aunque el backend siempre responde `200`, no se sabe si existe la cuenta).
3. Abrir el enlace recibido por email → `/reset-password?token=<64-chars-hex>`.
4. Introducir una contraseña nueva + confirmación que coincida → "Password updated".
5. Con un token modificado → "Link not valid"; con un token repetido → "This link has already been used"; con un token viejo/caducado → "This link has expired".
6. `npm run build` sin warnings: el flujo añade claves i18n (`@@forgot*`, `@@reset*`, `@@loginForgotPassword`).

---

## Bloque 10 — i18n: internacionalización Angular

**Objetivo**: añadir soporte multilingüe al frontend usando `@angular/localize` (la solución oficial de Angular), con 3 idiomas: inglés (default), español y portugués.

**Por qué `@angular/localize` y no `@ngx-translate`**: es la solución oficial del framework, funciona a nivel de compilación (sin runtime overhead), maneja pluralización y descripciones de contexto nativamente, y genera un bundle por idioma. Para un proyecto con idiomas conocidos es la opción más robusta.

### 10.1 — Setup de `@angular/localize`

#### Instalación

```bash
cd frontend
npm install @angular/localize@^21.2.21 --save-dev
```

> **Error conocido**: `ng add @angular/localize` puede fallar por peer dependency conflict si los paquetes Angular no están todos en la misma versión. Solución: alinear todas las dependencias Angular a la misma versión (`^21.2.21`), borrar `node_modules` + `package-lock.json`, y reinstalar limpio.

#### Polyfill en `angular.json`

Un **polyfill** es un fragmento de código que extiende o re implementa funcionalidades que el runtime del navegador no provee nativamente. En este caso, `@angular/localize/init` inyecta la función global `$localize` en el entorno de ejecución antes de que arranque la aplicación, del mismo modo que un polyfill de JavaScript añade métodos a `Array.prototype` cuando el navegador no los soporta. Angular lo gestiona a través de la opción `polyfills` del build, que los incluye en el bundle automáticamente sin necesidad de importarlos manualmente en `main.ts`.

```json
"architect": {
  "build": {
    "options": {
      "polyfills": ["@angular/localize/init"]
    }
  }
}
```

#### Tipos en `tsconfig.app.json`

```json
"compilerOptions": {
  "types": ["@angular/localize"]
}
```

Sin esto, TypeScript no reconoce `$localize` como global.

#### Configuración i18n en `angular.json`

El bloque `i18n` va a **nivel de proyecto**:

```json
{
  "projects": {
    "frontend": {
      "i18n": {
        "sourceLocale": "en",
        "locales": {
          "es": "src/locale/messages.es.xlf",
          "pt": "src/locale/messages.pt.xlf"
        }
      },
      "architect": {
        "build": {
          "configurations": {
            "production": {
              "localize": true
            }
          }
        }
      }
    }
  }
}
```

Con `localize: true`, `ng build` genera un bundle por idioma en `dist/frontend/browser/{en,es,pt}/`.

### 10.2 — Anotación de strings en templates

Para strings visibles en el HTML, se usa el atributo `i18n` con una clave única:

```html
<!-- Texto simple -->
<h1 i18n="@@loginTitle">Login</h1>

<!-- Label que envuelve un input -->
<label i18n="@@loginEmailLabel">
  Email
  <input type="email" formControlName="email" />
</label>

<!-- Placeholder y aria-label -->
<input
  placeholder="Search by title or author…"
  i18n-placeholder="@@catalogSearchPlaceholder"
  aria-label="Search books"
  i18n-aria-label="@@catalogSearchAria"
/>

<!-- Botón con interpolación ternaria -->
<button i18n="@@loginSubmitButton">
  {{ loading() ? 'Logging in…' : 'Login' }}
</button>

<!-- Texto con link embebido -->
<p i18n="@@loginSwitch">
  Don't have an account? <a routerLink="/register">Sign up</a>
</p>
```

**Convención de claves**: `{área}{elemento}{contexto}` — ej. `@@loginTitle`, `@@catalogSearchPlaceholder`, `@@bookDetailErrorNotFound`.

**Qué NO anotar**:

- Datos dinámicos de BD: `{{ book.title }}`, `{{ book.category }}` — son datos, no UI.
- Acrónimos universales: ISBN, URL — se escriben igual en todos los idiomas.
- Nombres propios: BookSocial, Google, Open Library.

### 10.3 — Anotación de strings en TypeScript

Para strings en `.ts` (mensajes de error, labels, textos dinámicos), se usa `$localize` como tagged template literal:

```ts
// Mensaje de error simple
this.error.set($localize`@@catalogErrorLoad:Failed to load the catalog.`);

// Mapa de labels
private readonly statusLabels: Record<ShelfStatus, string> = {
  WANTS_TO_READ: $localize`@@shelfStatusWantToRead:Want to read`,
  READING: $localize`@@shelfStatusReading:Reading`,
  READ: $localize`@@shelfStatusRead:Read`,
};

// Array de filtros
readonly filters = [
  { value: null, label: $localize`@@shelfFilterAll:All` },
  { value: 'WANTS_TO_READ', label: $localize`@@shelfStatusWantToRead:Want to read` },
];
```

**Formato**: `` $localize`@@key:Texto en inglés` `` — el prefijo `@@key` se extrae automáticamente al generar el XLF y se elimina del runtime.

### 10.4 — Extracción y traducción

```bash
ng extract-i18n
```

Esto genera `src/locale/messages.xlf` (archivo fuente en inglés) con todos los strings anotados. Cada `<trans-unit>` tiene un `id` (la clave) y un `<source>`.

Para crear una traducción:

1. Copiar `messages.xlf` a `messages.es.xlf` y `messages.pt.xlf`
2. Añadir `target-language="es"` / `target-language="pt"` al tag `<file>`
3. Añadir `<target>` tras cada `<source>` con la traducción
4. **Nunca modificar** los tags `<x>`, `ctype`, `equiv-text` ni `<context-group>`

Ejemplo de trans-unit:

```xml
<trans-unit id="loginTitle" datatype="html">
  <source>Login</source>
  <target>Iniciar sesión</target>
  <context-group purpose="location">
    <context context-type="sourcefile">src/app/features/auth/login/login.html</context>
    <context context-type="linenumber">6,8</context>
  </context-group>
</trans-unit>
```

Para strings con interpolación en templates, el `<target>` replica los tags `<x>`:

```xml
<trans-unit id="loginSubmitButton" datatype="html">
  <source> <x id="INTERPOLATION" equiv-text="..."/> </source>
  <target> <x id="INTERPOLATION" equiv-text="..."/> </target>
</trans-unit>
```

Los textos de **interpolación ternaria** viven en el template:

```html
<button i18n="@@loginSubmitButton">
  {{ loading() ? 'Logging in…' : 'Login' }}
</button>
```

El archivo XLF **solo** almacena el placeholder <x id="INTERPOLATION"/> — no contiene las cadenas "Logging in…" ni "Login". Esas cadenas viven dentro del `.html`, no en el XLF.

### 10.5 — Build multi-locale

```bash
ng build                    # genera dist/frontend/browser/{en,es,pt}/
ng build --configuration development   # solo locale source (para dev)
```

En producción, cada locale tiene su propio bundle con las traducciones embebidas. El `sourceLocale` (`en`) siempre se genera; los demás solo si están listados en `locales`.

### 10.6 — Archivos relevantes

```
frontend/
├── angular.json                          # i18n section + localize: true
├── tsconfig.app.json                     # types: ["@angular/localize"]
├── package.json                          # @angular/localize en devDependencies
└── src/
    ├── main.ts                           # sin import de @angular/localize/init
    ├── locale/
    │   ├── messages.xlf                  # archivo fuente (87 messages, inglés)
    │   ├── messages.es.xlf               # traducciones español
    │   └── messages.pt.xlf               # traducciones portugués
    └── app/
        ├── features/
        │   ├── auth/login/login.{html,ts}
        │   ├── auth/register/register.{html,ts}
        │   ├── auth/oauth2-callback/oauth2-callback.{html,ts}
        │   ├── home/home.{html,ts}
        │   ├── catalog/catalog.{html,ts}
        │   ├── book-detail/book-detail.{html,ts}
        │   ├── my-shelf/my-shelf.{html,ts}
        │   └── author-detail/author-detail.{html,ts}
        └── shared/components/nav/nav.html
```

### 10.7 — Errores encontrados (con solución directa)

1. **`ng add @angular/localize` — peer dependency conflict**: paquetes Angular en versiones mixtas (`21.2.19` vs `21.2.21`). Solución: alinear todos a `^21.2.21`, borrar `node_modules` + `package-lock.json`, reinstalar limpio.

2. **Warning `Include '@angular/localize/init' as a polyfill instead`**: `import '@angular/localize/init'` en `main.ts` no es la forma recomendada. Solución: quitar el import y añadir `"polyfills": ["@angular/localize/init"]` en `angular.json`.

3. **Propiedad `i18n` no permitida**: el bloque `i18n` estaba dentro de `architect.build.options`. En `@angular/build:application`, `i18n` va a **nivel de proyecto** (dentro de `"frontend": {}`). Solución: mover el bloque un nivel más arriba.

4. **`$localize` not found**: `tsconfig.app.json` tenía `"types": []`. Solución: añadir `"@angular/localize"` al array.

### Decisiones de diseño de i18n

- **Compile-time vs runtime**: se eligió compile-time (`@angular/localize`) porque el proyecto tiene idiomas fijos (en/es/pt) y no necesita cambio dinámico de idioma en runtime. Un bundle por idioma es más eficiente que un mapa de traducciones en runtime.
- **Categorías no traducidas**: los valores de `book.category` (Fantasy, Science Fiction, etc.) son datos de BD que vienen de Google Books API. Traducirlos requeriría un mapping frontend o i18n en backend, lo cual escopa del i18n de UI. Se dejan en inglés como datos de contenido.
- **Strings compartidos**: `statusLabels` (Want to read / Reading / Read) aparecen en `book-detail.ts` y `my-shelf.ts`. Se usan las mismas claves `@@shelfStatus*` en ambos archivos para mantener consistencia.

---

## Bloque 12 — Fase 10: Frontend del feed social + notificaciones en tiempo real

Este bloque integra en el frontend Angular los servicios de la Fase 9: la página `/feed` con paginación por cursor y la campana de notificaciones con push WebSocket STOMP. También documenta dos trampas del dev-server (Vite) que conviene conocer antes de tocar el proxy.

**Objetivo**: que `/feed` y la campana funcionen contra el stack Docker, con sesión restaurada entre navegaciones (F5/refresh por cookie) y sin colisiones de rutas con el proxy.

### 12.1 — Servicios y modelo

- `@stomp/stompjs` para el cliente WebSocket. `NotificationRealtimeService` se conecta **directo** a `ws://localhost:8087/ws?token=<JWT>` porque el gateway WebMVC **no** proxea WebSockets (ver [GUIDE-BACKEND.md](./GUIDE-BACKEND.md), 11.7). El WS solo admite el origen `http://localhost:4200`.
- `FeedService.getFeed(cursor, limit)` → `GET /api/feed?limit=&cursor=`; `NotificationService` → `/api/notifications`, `/api/notifications/unread-count`, `POST /api/notifications/read`; `AuthService.userId()` decodifica el claim `uid` del access token sin librería JWT.
- La página `/feed` enriquece el nombre del actor vía `/profiles/{userId}` con una caché reactiva (`signal<Map<number,string>>`) que evita duplicar llamadas.

### 12.2 — El dev-server proxea ANTES que el fallback SPA (dos trampas)

**Trampa 1 — colisión ruta ↔ clave de proxy.** Vite aplica el proxy antes de servir `index.html`. Con una clave `^/feed`, un F5 a la página `/feed` (navegación de documento, sin `Authorization`) golpeaba el gateway → `401 {"error":"unauthorized","message":"Authentication required"}`. Solución: la API usa un prefijo **sin colisión** con rewrite:

```json
"^/api/feed(\\?|/|$)": {
  "target": "http://localhost:8080",
  "changeOrigin": true,
  "secure": false,
  "pathRewrite": { "^/api/feed": "/feed" }
}
```

(En el repo, `proxy.conf.json` tiene `\\?|/|$` porque el fichero se parsea como JSON y el valor final del regex debe ser `\?`.)

**Trampa 2 — el query string rompe la frontera `(/|$)`.** Vite matchea `req.url` **incluyendo el query string**: `GET /api/feed?limit=10` no casaba con `^/api/feed(/|$)` (el `?` no es `/` ni fin de línea) → el dev-server servía `index.html` como `text/html` → `HttpClient` fallaba al parsear → *"Failed to load your feed."* Por eso **todas** las claves del proxy usan la frontera `(\?|/|$)`. La frontera antigua habría roto igual a `/books/search?q=…`, `/authors/search?q=…` y `/books/search/full?q=…`.

> El proxy se lee **al arrancar** `ng serve`: tras tocar `proxy.conf.json` hay que reiniciar el dev-server.

### 12.3 — Sesión entre navegaciones (F5 / refresh por cookie)

El access token (TTL 15 min) vive en memoria; un F5 perdía la sesión salvo que la restauración por cookie terminara antes de la navegación. Fix aplicado:

- `provideAppInitializer(() => inject(AuthService).ensureSession())` + `withEnabledBlockingInitialNavigation()`: la restauración (`POST /auth/refresh`, cookie httpOnly `refresh_token`, TTL 7 días) termina **antes** de la primera navegación.
- `authGuard` asíncrono: `await auth.ensureSession()` antes de decidir → `/login` si no hay sesión.
- `AuthService.ensureSession()` memoiza la promesa de restauración (no se dispara `refresh` en paralelo).
- El interceptor `authInterceptor` rejuga la petición con el token renovado tras un `401` (y limpia la sesión si el refresh falla).

### 12.4 — Verificación E2E (dos usuarios)

1. Login con `social1@test.com` en `http://localhost:4200` (usar el puerto por defecto para el STOMP).
2. En otra terminal, `social2@test.com` deshace y repite el follow contra `/follows/{id}` (script del verificación del [GUIDE-BACKEND.md](./GUIDE-BACKEND.md), Bloque 11) → la campana de social1 recibe el push en vivo y el badge sube.
3. **F5** en `/feed` → la página se sirve como SPA y `GET /api/feed?limit=10` responde `application/json` con la actividad `FOLLOW` en primer lugar (el siguiente `nextCursor` alimenta "Load more").

Estado verificable en el navegador (DevTools → Network): `/feed` → `200 text/html` (documento); `/api/feed?limit=10` → `200 application/json`; `/auth/refresh` → `200 application/json` con `Set-Cookie: refresh_token=…`; `ws://localhost:8087/ws?token=…` → `101 Switching Protocols`.

### 12.5 — Directorio People, perfil público y botón Follow

Sobre la base del feed, se añade el directorio **People** para buscar usuarios, ver su perfil público y seguirlos. Consume las APIs del user-service (`/profiles/*`, `/follows/*`; ver [GUIDE-BACKEND.md](./GUIDE-BACKEND.md), 6.6).

**Archivos**:

- `core/services/follow.service.ts` — estado reactivo de "a quién sigo" y las llamadas a `/follows` y `/profiles/search`.
- `shared/components/follow-button/` — el botón Follow/Following/Dejar de seguir, reutilizado en feed, People y perfil.
- `features/users/` → `/users` (People).
- `features/user-profile/` → `/users/:id` (perfil público + pestañas Followers/Following).
- `nav` añade el enlace **People** (`@@navPeople`); `app.routes` registra ambas rutas con `authGuard`.

#### `FollowService`: cache reactiva de `followingIds`

```typescript
@Injectable({ providedIn: 'root' })
export class FollowService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);

  private readonly followingStore = signal<Set<number>>(new Set());
  private loaded = false;

  readonly followingIds = this.followingStore.asReadonly();

  isFollowing(userId: number): boolean {
    return this.followingStore().has(userId);
  }

  ensureLoaded(): void {
    if (this.loaded) return;
    this.loaded = true;
    this.reloadFollowing();
  }

  reloadFollowing(): void {
    const me = this.auth.userId();
    if (me === null) return;
    this.http.get<FollowResponse[]>(`/follows/${me}/following`).subscribe((follows) => {
      this.followingStore.set(new Set(follows.map((f) => Number(f.followeeId))));
    });
  }

  follow(targetUserId: number): Observable<FollowResponse> {
    return this.http
      .post<FollowResponse>(`/follows/${targetUserId}`, null)
      .pipe(tap(() => this.markFollowing(targetUserId, true)));
  }

  unfollow(targetUserId: number): Observable<void> {
    return this.http
      .delete<void>(`/follows/${targetUserId}`)
      .pipe(tap(() => this.markFollowing(targetUserId, false)));
  }

  toggle(targetUserId: number): Observable<unknown> {
    return this.isFollowing(targetUserId)
      ? this.unfollow(targetUserId)
      : this.follow(targetUserId);
  }

  followers(userId: number): Observable<FollowResponse[]> {
    return this.http.get<FollowResponse[]>(`/follows/${userId}/followers`);
  }

  following(userId: number): Observable<FollowResponse[]> {
    return this.http.get<FollowResponse[]>(`/follows/${userId}/following`);
  }

  search(query: string): Observable<ProfileResponse[]> {
    return this.http.get<ProfileResponse[]>('/profiles/search', { params: { q: query } });
  }

  private markFollowing(targetUserId: number, value: boolean): void {
    const next = new Set(this.followingStore());
    if (value) next.add(targetUserId);
    else next.delete(targetUserId);
    this.followingStore.set(next);
  }
}
```

El truco del servicio: mantiene un `signal<Set<number>>` en memoria con los IDs que sigo. Al hacer `follow`/`unfollow` actualiza ese Set de forma **optimista** con `tap()`, así **todos** los botones de la app reaccionan al instante sin recargar las listas. `ensureLoaded()` (con guard `loaded`) lo rellena una sola vez con `GET /follows/{me}/following`.

#### Componente compartido `FollowButton`

```typescript
@Component({
  selector: 'app-follow-button',
  imports: [],
  templateUrl: './follow-button.html',
  styleUrl: './follow-button.scss',
})
export class FollowButton implements OnInit {
  private readonly followService = inject(FollowService);
  private readonly auth = inject(AuthService);

  readonly userId = input.required<number>();
  readonly busy = signal<boolean>(false);

  get isSelf(): boolean      { return this.auth.userId() === this.userId(); }
  get isFollowing(): boolean { return this.followService.isFollowing(this.userId()); }

  ngOnInit(): void      { this.followService.ensureLoaded(); }

  onClick(): void {
    if (this.busy()) return;
    this.busy.set(true);
    this.followService.toggle(this.userId()).subscribe({
      next: () => this.busy.set(false),
      error: () => this.busy.set(false),
    });
  }
}
```

```html
@if (!isSelf) {
<button type="button" class="follow" [class.following]="isFollowing" [disabled]="busy()" (click)="onClick()">
  @if (busy()) { <span i18n="@@followBusy">…</span> }
  @else if (isFollowing) { <span i18n="@@followUnfollow">Unfollow</span> }
  @else { <span i18n="@@followFollow">Follow</span> }
</button>
}
```

Detalles:

- Se **oculta por completo** (`@if (!isSelf)`) si soy yo mismo — no tiene sentido seguirte a ti.
- El estado `busy` evita clics duplicados mientras la petición está en vuelo.
- La clase `.following` (fondo `#e3f2fd`) es la pista visual del estado; los labels usan i18n (`@@followFollow`, `@@followUnfollow`, `@@followBusy`).

#### Directorio People (`/users`) — `features/users`

```typescript
export class Users implements OnInit {
  private readonly followService = inject(FollowService);
  private readonly fb = inject(NonNullableFormBuilder);

  profiles = signal<ProfileResponse[]>([]);
  loading = signal<boolean>(true);
  searching = signal<boolean>(false);
  error = signal<string>('');

  searchForm = this.fb.group({ q: [''] });

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading.set(true);
    this.error.set('');
    this.followService.search('').subscribe({
      next: (profiles) => { this.profiles.set(profiles); this.loading.set(false); },
      error: () => {
        this.loading.set(false);
        this.error.set($localize`:@@usersErrorLoad:Failed to load users.`);
      },
    });
  }

  doSearch(): void {
    const q = this.searchForm.getRawValue().q.trim();
    if (!q) { this.load(); return; }
    this.searching.set(true);
    this.error.set('');
    this.followService.search(q).subscribe({
      next: (profiles) => { this.profiles.set(profiles); this.searching.set(false); },
      error: () => {
        this.searching.set(false);
        this.error.set($localize`:@@usersErrorSearch:Search failed. Try again.`);
      },
    });
  }
}
```

La plantilla (`users.html`) renderiza un formulario de búsqueda y una lista:

- Al montar llama `FollowService.search('')` (sin filtro = **todos**, el directorio inicial).
- `doSearch()` re-llama con `q`; si el campo está vacío vuelve a `load()`.
- Cada fila es un enlace a `/users/{userId}` con el nombre (o email si no hay `displayName`), los contadores followers/following y un `<app-follow-button [userId]="profile.userId"/>`.
- Recorre con `@for (profile of profiles(); track profile.userId)`.

#### Perfil público (`/users/:id`) — `features/user-profile`

```typescript
export class UserProfile implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly userService = inject(UserService);
  private readonly followService = inject(FollowService);
  private readonly auth = inject(AuthService);

  userId = 0;
  profile = signal<ProfileResponse | null>(null);
  followers = signal<ListedUser[]>([]);
  following = signal<ListedUser[]>([]);
  activeTab = signal<'followers' | 'following'>('followers');
  loading = signal<boolean>(true);
  loadingList = signal<boolean>(false);
  error = signal<string>('');

  get isSelf(): boolean { return this.auth.userId() === this.userId; }

  ngOnInit(): void {
    const raw = this.route.snapshot.paramMap.get('id');
    const id = raw ? Number(raw) : NaN;
    if (!Number.isFinite(id) || id <= 0) {
      this.error.set($localize`:@@profileErrorId:User not found.`);
      this.loading.set(false);
      return;
    }
    this.userId = id;
    this.followService.ensureLoaded();
    this.loadProfile();
  }

  private loadProfile(): void {
    this.userService.profile(this.userId).subscribe({
      next: (profile) => { this.profile.set(profile); this.loading.set(false); this.loadList('followers'); },
      error: () => { this.loading.set(false); this.error.set($localize`:@@profileErrorNotFound:User not found.`); },
    });
  }

  switchTab(tab: 'followers' | 'following'): void {
    this.activeTab.set(tab);
    this.loadList(tab);
  }

  private loadList(tab: 'followers' | 'following'): void {
    this.loadingList.set(true);
    const request =
      tab === 'followers'
        ? this.followService.followers(this.userId)
        : this.followService.following(this.userId);
    request.subscribe({
      next: (items) => this.resolveUsers(items, tab),
      error: () => this.loadingList.set(false),
    });
  }

  private async resolveUsers(items: FollowResponse[], tab: 'followers' | 'following'): Promise<void> {
    const ids = items.map((f) => Number(tab === 'followers' ? f.followerId : f.followeeId));
    const users = await Promise.all(
      ids.map(async (id) => {
        try {
          const p = await firstValueFrom(this.userService.profile(id));
          return { id, name: p.displayName || p.email };
        } catch {
          return { id, name: $localize`:@@profileActorUser:A reader` };
        }
      }),
    );
    if (tab === 'followers') this.followers.set(users);
    else this.following.set(users);
    this.loadingList.set(false);
  }
}
```

La plantilla (`user-profile.html`):

- Cabecera con `displayName` (o email), `email`, `location`, `bio` y el `<app-follow-button>`.
- Contadores `followersCount` / `followingCount`.
- Dos pestañas **Followers** / **Following**: `switchTab` recarga la lista correspondiente.

Detalle importante del perfil:

- **El id se valida** en `ngOnInit` (debe ser un entero positivo; si no, error "User not found").
- **Las listas Followers/Following se enriquecen con N+1**: `GET /follows/{id}/followers|following` devuelve solo IDs; cada uno se resuelve a un nombre con `UserService.profile(id)` dentro de `Promise.all` (asíncrono, no bloquea). Si falla, se muestra "A reader" como fallback.
- `UserService.profile(id)` → `GET /profiles/{id}`; `myProfile()` → `GET /profiles/me`.

#### Rutas y navegación

```typescript
// features/users/routes.ts
{ path: 'users', canActivate: [authGuard],
  loadComponent: () => import('./users').then((m) => m.Users) }

// features/user-profile/routes.ts
{ path: 'users/:id', canActivate: [authGuard],
  loadComponent: () => import('./user-profile').then((m) => m.UserProfile) }
```

| Ruta Angular | Componente | Auth        |
| ------------ | ---------- | ----------- |
| `/users`     | `Users` (lazy)     | `authGuard` |
| `/users/:id` | `UserProfile` (lazy) | `authGuard` |

El nav añade, dentro del bloque `@if (isAuthenticated())`:

```html
<a routerLink="/users" i18n="@@navPeople">People</a>
```

#### Flujo de datos (resumen)

```
FollowButton.ngOnInit ──▶ FollowService.ensureLoaded()
                              └─ GET /follows/{me}/following → Set<userId>
Usuarios en /users ──▶ FollowService.search(q) → GET /profiles/search?q=
Perfil /users/:id ──▶ UserService.profile(id) → GET /profiles/{id}
Pestaña Followers ──▶ FollowService.followers(id) → GET /follows/{id}/followers
Pestaña Following ──▶ FollowService.following(id) → GET /follows/{id}/following
Clic Follow ────────▶ FollowService.toggle(id) → POST|DELETE /follows/{id}
                         └─ tap() → actualiza el Set (optimista) → el botón cambia al instante
```

#### Verificación E2E

1. Login con `social1@test.com`; el nav muestra el enlace **People** y el perfil propio se **materializa** al entrar (login → `GET /profiles/me`).
2. En `/users`, buscar por nombre "alice" → lista filtrada; cada fila muestra contadores + `app-follow-button`.
3. Entrar a `/users/2` → perfil público con botón Follow; pestañas Followers/Following cargan las listas enriquecidas.
4. Pulsar Follow → el botón cambia a **Following** al instante (optimista) y el contador de seguidores del perfil sube.
5. `npm run build` sin warnings: i18n completo con **153 trans-units** (reset, People, perfil, follow en en/es/pt).

---

## Apéndice C — Operación: despliegue, logs y depuración

(Sección de **frontend**; para la operación del backend/stack Docker, ver [GUIDE-BACKEND.md](./GUIDE-BACKEND.md).)

### Frontend

```powershell
cd frontend
npm start                # ng serve en :4200 con proxy a :8080 (proxy.conf.json)
npm run build            # verificación de compilación (solo locale source)
ng build                 # build de producción con 3 idiomas (en/es/pt)
ng extract-i18n          # regenerar messages.xlf desde templates + $localize
```

Ojo: si editas `proxy.conf.json` hay que **reiniciar** `ng serve` — el proxy solo se lee al arrancar, el hot-reload no lo recoge.

#### i18n — añadir un string nuevo

1. En el template: añadir `i18n="@@nuevaClave"` al elemento.
2. En `.ts` (si aplica): usar `` $localize`@@nuevaClave:Texto en inglés` ``.
3. Ejecutar `ng extract-i18n` para regenerar `messages.xlf`.
4. Copiar las nuevas `<trans-unit>` a `messages.es.xlf` y `messages.pt.xlf` con sus `<target>`.
5. Verificar con `ng build`.

---
*Para la arquitectura del backend, servicios microservicios, CQRS, RabbitMQ, seguridad y operación, ver [GUIDE-BACKEND.md](./GUIDE-BACKEND.md).*
