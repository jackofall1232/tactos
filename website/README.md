# tactos website

The public site for tactos: a single self-contained `index.html` — system fonts,
inline SVG, pure HTML+CSS, no JavaScript.

**The rule (it's the brand): zero external requests.** The page makes exactly one
request — itself. Any PR adding a CDN, web font, `<script>`, analytics snippet, or
remote image will be rejected.

Preview locally:

    python3 -m http.server -d website

Deployed by `.github/workflows/pages.yml` (push to `main` touching `website/**`) to
https://jackofall1232.github.io/tactos/ — once Pages is enabled
(repo Settings → Pages → Source: GitHub Actions).
