**simple, clean, no-nonsense cheat sheet** you can rely on every day.
It’s designed exactly for your case: **custom images, chart edits, cluster mess, and not breaking everything unnecessarily**.

---

# ✅ **Kubernetes / Helm Action Cheat Sheet (Practical, Minimal, High-Value)**

Use this as your brain replacement when deciding “what do I do now?”.

---

# 🔄 **1. When you change your Docker image (new build)**

### **If the tag changed (e.g., `my:1.2 → my:1.3`)**

➡️ **Just run:**

```
helm upgrade superset ./chart
```

**Helm sees the tag change → updates Deployment → Kubernetes rolls out a new pod.**
No uninstall, no cleanup.

✔ Clean
✔ Idempotent
✔ The correct way

---

### **If the tag did NOT change (e.g., always using `:latest`)**

➡️ You MUST force the rollout:

```
kubectl rollout restart deployment superset
```

OR (better)

```
helm upgrade superset ./chart --set image.tag=newbuild123
```

✔ Best practice: **never reuse the same tag**
✔ Never mess with `:latest` in K8s
✔ Avoid orphan containers and cache issues

---

# 🧱 **2. When you change Helm chart templates (yaml files)**

➡️ **Just run:**

```
helm upgrade superset ./chart
```

If you want a clean diff:

```
helm upgrade superset ./chart --recreate-pods
```

✔ Never uninstall
✔ Never restart the VM
✔ Helm chart changes = helm upgrade only

---

# 🧽 **3. When you want a clean reinstall (fresh state, but cluster stays intact)**

Use this only if you **broke resources**, or want a **full reset**.

```
helm uninstall superset -n superset
kubectl delete pvc -n superset --all
```

Then reinstall:

```
helm install superset ./chart -n superset
```

Use this if:

* DB got corrupted
* PVC stuck
* Helm upgrade keeps failing
* Garbage was left from previous experiments

⚠ Rarely needed, but safe.

---

# ☠️ **4. When the environment is totally insane / zombie pods / dead CRDs**

➡️ You need a **node reboot** ONLY IF:

* Docker/Containerd is misbehaving
* kubelet is stuck
* pods are “ghost pods” you can’t remove
* volumes or mounts are locked

```
sudo reboot
```

⚠ Not a normal workflow
⚠ Doesn’t fix Helm or chart issues
⚠ Only fixes underlying VM/container-runtime issues

---

# 📦 **5. Restarting pods (without touching Helm)**

If you changed environment variables, secrets, configmaps **outside Helm**, but didn’t change chart/values:

```
kubectl rollout restart deployment superset
```

OR to restart everything:

```
kubectl delete pod -l app=superset
```

✔ Safe
✔ No uninstall
✔ Doesn’t modify Helm’s state

---

# 🔁 **6. “kubectl apply” vs “helm upgrade”**

### **If resource is managed by Helm → ALWAYS use helm**

Otherwise Helm will get out of sync.

* **Helm upgrade** → PATCHes changes and stores them in release history
* **kubectl apply** → writes changes but Helm doesn't know, future upgrades may overwrite them or break

✔ If using Helm → never use kubectl apply

---

# 🔍 **7. Are they idempotent?**

| Action                    | Idempotent? | Notes                                 |
| ------------------------- | ----------- | ------------------------------------- |
| `helm upgrade`            | ✔ Yes       | Safe and preferred                    |
| `helm uninstall/install`  | ✖ No        | Deletes then recreates everything     |
| `kubectl rollout restart` | ✖ No        | Forces restart regardless of state    |
| `kubectl delete pod`      | ✖ No        | Deletes pods manually; they come back |
| VM reboot                 | ✖ No        | Last-resort fix; not part of workflow |

---

# ⭐ **Your Golden Decision Tree (mental model)**

### **Did you change your image?**

➡️ Change tag? → `helm upgrade`
➡️ Same tag? → `rollout restart` or set new tag + upgrade

### **Did you change your Helm chart?**

➡️ `helm upgrade`

### **Did you break the environment?**

➡️ `helm uninstall` → delete PVC → helm install

### **Node is misbehaving?**

➡️ Reboot

---

# If you want, I can generate a **one-page PNG cheat sheet** you can keep on your desktop.

Just say: **“give me the png cheat sheet”**.

Let me know when your custom image finishes building—we’ll jump back to your Superset setup path exactly where we left off.

---

Yes — **you can absolutely do that.**
Superset **can use a custom backend plugin** to fetch data from *any* API instead of a SQL database.
This includes **your own HTTP API that returns JSON**, or any service that produces dynamic pivot tables based on parameters.

But you must understand one thing:

> **Superset only understands “tables”.**
> It does *not* natively know how to visualize arbitrary JSON.
> A backend datasource plugin must convert your API response → a pandas DataFrame.

And then Superset treats it like a normal dataset.

Below is the exact picture.

---

# ✅ Yes — Superset supports “non-SQL datasources” using a custom datasource plugin

Superset has a plugin architecture:

### 🔹 Frontend plugin

For visualizations (charts, UI elements)

### 🔹 Backend plugin

For datasources (SQL, NoSQL, API, custom drivers)

You want the **backend datasource plugin.**

It allows you to create a new datasource type:
`MyApiDatasource`

That datasource can:

* call your HTTP API
* pass query params (filters, group by, columns)
* get JSON back
* convert it into a table
* return it to Superset as if it came from SQL

---

# 🧱 How the backend plugin works (simple version)

### 1. Superset sends a “query object”

Example:

```json
{
  "columns": ["country", "sales"],
  "metrics": ["sum__sales"],
  "filters": [{"col": "year", "op": "==", "val": 2024}],
  "groupby": ["country"]
}
```

### 2. Your plugin converts it into an API request

Example:

```
GET https://my-service/pivot?year=2024&groupby=country&metric=sales
```

### 3. API returns JSON:

```json
[
  {"country": "USA", "sales": 1200},
  {"country": "UK", "sales": 700}
]
```

### 4. Plugin converts JSON → DataFrame

### 5. Superset renders it like SQL data

---

# ✔ You get full Superset capabilities

Once you register this datasource type:

* Slices
* Dashboards
* Filters
* Cross-filtering
* Time series
* Drilldowns
* Caching

All **work normally**.

Superset doesn’t care where data came from — as long as your plugin returns a table-like structure.

---

# 📌 Real example: Apache Druid plugin

Superset already includes a non-SQL datasource plugin: **Druid**.

Your plugin can follow the same pattern.

---

# 👌 What you need to build it

A custom Docker image with:

* your plugin code (Python)
* entry point to register it
* build & install it inside `/app/pythonpath/superset/ext/`

This is not complicated.
A minimal plugin is ~30 lines of Python.

I can generate a **ready-to-run template** for you in the next message if you want.

---

# 🧠 Summary (very short)

| Can Superset read from custom API?    | YES                         |
| ------------------------------------- | --------------------------- |
| Convert API → table?                  | You handle it in plugin     |
| Supports filters / groupby / metrics? | YES                         |
| Supports dashboards/charts?           | YES                         |
| Requires custom image?                | YES (to install the plugin) |
| Difficult?                            | No — 20–80 lines of code    |

---

If you want, tell me:

### ✔ What does your API endpoint look like?

### ✔ Does it return JSON rows or JSON cubes/pivots?

### ✔ Do you want Superset filters to translate into API params?

Then I can generate the **exact plugin code** + Dockerfile for your case.
