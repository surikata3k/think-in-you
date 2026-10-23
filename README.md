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

## Descargar e instalar

1. Desde el móvil, abre este enlace:
   **https://github.com/surikata3k/think-in-you/releases/download/latest/ThinkInYou.apk**
2. Abre el archivo descargado. Android pedirá permitir "instalar apps de origen
   desconocido" para el navegador: acéptalo.
3. Abre **Think in you** y acepta el permiso de notificaciones.

Cada vez que se sube un cambio a `main`, GitHub compila la app y actualiza ese
enlace automáticamente (pestaña **Actions**). Para actualizar la app basta con
descargar e instalar de nuevo: se conserva tu progreso.

**Si sale "Aplicación bloqueada para proteger tu dispositivo"** (Play Protect):
pulsa *Más detalles → Instalar de todos modos*. Si no aparece esa opción, abre
Play Store → tu foto → *Play Protect* → ⚙ → desactiva *Analizar apps con Play Protect*,
instala la app y vuelve a activarlo. En Vivo, revisa también *Ajustes → Seguridad*
(o *Gestor del teléfono*) y desactiva temporalmente la verificación de instalaciones.

**Importante:** algunos móviles (Xiaomi, Huawei, Oppo, Samsung...) cierran las apps en
segundo plano. Si las frases no llegan, ve a *Ajustes → Apps → Think in you → Batería*
y elige **Sin restricciones**.

## Cambiar las frases

Las frases están en [`docs/frases.txt`](docs/frases.txt), una por línea (las que empiezan
por `#` se ignoran). Edítalo directamente en GitHub (icono del lápiz) y guarda:

- **La app** descarga el fichero cada vez que se abre. Las frases que ya tenías
  conservan su progreso; las nuevas entran en práctica antes que las demás; las que
  quites desaparecen. Las frases que añadas desde la propia app no se tocan nunca.
  GitHub puede tardar hasta 5 minutos en servir la versión nueva.
- **La web** muestra siempre la lista actual.
- El APK incluye una copia del fichero para la primera vez o cuando no hay conexión.

## Web

La página está en `docs/index.html` y se publica con GitHub Pages en
https://surikata3k.github.io/think-in-you/
