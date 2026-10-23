# Think in you

App Android que te envía frases espirituales a horas aleatorias, dentro de los
tramos horarios que tú eliges, para ir interiorizándolas poco a poco.

## Qué hace

- **Tramos horarios**: añade los que quieras (por ejemplo 9:00–13:00 y 17:00–21:00)
  y el número de avisos de cada uno. Las horas exactas cambian cada día al azar.
- **Aprendizaje gradual**: empiezas con 3 frases y cada día entra una nueva
  (configurable de 0 a 5). Las frases recién llegadas salen bastante más a menudo;
  según las vas viendo, ceden protagonismo a las siguientes. Nunca se repite la misma
  frase dos veces seguidas.
- **Tus frases**: añade o borra frases desde la app. Las tuyas entran en práctica al momento.
- Funciona sin internet, no recoge datos y sobrevive a reinicios del móvil.

## Cómo conseguir el APK

### Opción A — Compilarlo gratis en GitHub (sin instalar nada)

1. Crea una cuenta en github.com (si no tienes) y un repositorio nuevo, por ejemplo `think-in-you`.
2. En el repositorio pulsa **Add file → Upload files** y arrastra *todo el contenido*
   de esta carpeta (incluida la carpeta `.github`; si tu explorador la oculta, actívalo
   para ver archivos ocultos). Pulsa **Commit changes**.
   Si la pestaña Actions no muestra nada, es que `.github` no se subió: en **Actions**
   pulsa *set up a workflow yourself*, pega el contenido de `.github/workflows/build.yml`
   y guarda.
3. Ve a la pestaña **Actions**: verás "Compilar APK" ejecutándose (3–5 minutos).
4. Cuando termine con ✓, entra en la ejecución y descarga **ThinkInYou-apk** (un .zip con el `app-debug.apk`).

### Opción B — Android Studio

1. Instala Android Studio y abre esta carpeta (**File → Open**).
2. Espera a que termine la sincronización y pulsa ▶ **Run** con el móvil conectado
   por USB (depuración USB activada), o **Build → Build APK(s)**.

## Instalar en el móvil

1. Pasa el `app-debug.apk` al móvil (Drive, correo, cable...) y ábrelo.
2. Android pedirá permitir "instalar apps de origen desconocido" para esa app: acéptalo.
3. Abre **Think in you** y acepta el permiso de notificaciones.

**Importante:** algunos móviles (Xiaomi, Huawei, Oppo, Samsung...) cierran las apps en
segundo plano. Si las frases no llegan, ve a *Ajustes → Apps → Think in you → Batería*
y elige **Sin restricciones**.

## Cambiar las frases de serie

Están en `app/src/main/assets/frases.txt`, una por línea. Si cambias el archivo,
desinstala la app antes de instalar la nueva versión (las frases se cargan la primera vez).
