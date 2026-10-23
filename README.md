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
   **https://github.com/surikata3k/think-in-you/releases/latest/download/ThinkInYou.apk**
2. Abre el archivo descargado. Android pedirá permitir "instalar apps de origen
   desconocido" para el navegador: acéptalo.
3. Abre **Think in you** y acepta el permiso de notificaciones.

Cada vez que se sube un cambio a `main`, GitHub compila la app y actualiza ese
enlace automáticamente (pestaña **Actions**). Para actualizar la app basta con
descargar e instalar de nuevo: se conserva tu progreso.

**Importante:** algunos móviles (Xiaomi, Huawei, Oppo, Samsung...) cierran las apps en
segundo plano. Si las frases no llegan, ve a *Ajustes → Apps → Think in you → Batería*
y elige **Sin restricciones**.

## Cambiar las frases de serie

Están en `app/src/main/assets/frases.txt`, una por línea. Si cambias el archivo,
desinstala la app antes de instalar la nueva versión (las frases de serie se cargan solo la primera vez).
