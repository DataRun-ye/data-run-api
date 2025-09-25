Here’s one way to do it in IntelliJ IDEA using a single regex search-and-replace (make sure **Regex** and **“. matches newline”** are both turned on):

---

**Find (search)**

```regex
(?s)^[ \t]*"(?:rules|label|path|order|description|valueTypeRendering|mandatory)"\s*:\s*(?:\{.*?\}|\[.*?\]|"[^"]*"|\d+|true|false)\s*,?\R
```

**Replace with**

```
```

*(leave empty)*

---

### How it works

* `(?s)`                 — dot-all mode so `.` can match newlines (needed for the multi‐line `label` block).
* `^`                    — start of a line.
* `[ \t]*`               — any leading indent.
* `"(?:…)"`              — one of the chosen keys:

  ```
  rules|label|path|order|description|valueTypeRendering|mandatory
  ```
* `\s*:\s*`              — the colon plus any surrounding whitespace.
* `(?:\{.*?\}|\[.*?\]|"[^"]*"|\d+|true|false)`
  — match a single JSON value which can be:

    * a `{…}` object (e.g. `label:{…}`)
    * a `[…]` array (e.g. `rules:[]`)
    * a string `"…"`
    * a number (`\d+`)
    * a Boolean (`true`/`false`)
* `\s*,?`                — optional trailing comma and any spaces
* `\R`                   — the line break (so we drop the entire line)

### Steps in IntelliJ

1. **Ctrl+R** (Replace in File)
2. Check **Regex** (Alt+G) and **. matches newline** (you may need to click the gear ⚙ and enable “Dot matches newline”).
3. Paste the **Find** and leave **Replace** blank.
4. Hit **Replace All**.

This will strip out any of those key-value pairs (including multi-line blocks like `label`) cleanly.
