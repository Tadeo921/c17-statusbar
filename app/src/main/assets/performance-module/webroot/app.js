/* Local assets only. Native exec signature is verified against KernelSU's js/index.js
 * and APatch's WebViewInterface.kt. Shell output is data, never evaluated.
 * https://raw.githubusercontent.com/tiann/KernelSU/main/js/index.js
 * https://raw.githubusercontent.com/bmax121/APatch/main/app/src/main/java/me/bmax/apatch/ui/webui/WebViewInterface.kt
 */
(function () {
  "use strict";
  const COMMANDS = Object.freeze({
    read: "sh /data/adb/modules/c17_performance/webui.sh status",
    run: "sh /data/adb/modules/c17_performance/webui.sh run",
    enable: "sh /data/adb/modules/c17_performance/webui.sh enable",
    disable: "sh /data/adb/modules/c17_performance/webui.sh disable"
  });
  const EXPECTED_VERSION = 8;
  const REFRESH_MS = 4000;
  const c17 = window.c17 && typeof window.c17.readStatus === "function" && typeof window.c17.run === "function" &&
    typeof window.c17.setEnabled === "function" ? window.c17 : null;
  const ksu = window.ksu && typeof window.ksu.exec === "function" ? window.ksu : null;
  const bridge = c17 || ksu;
  const demo = !bridge && document.documentElement.dataset.readonlyDemo === "true" &&
    typeof window.C17_WEBUI_DEMO === "string";
  const fields = new Set(["installed", "pending", "disabled", "remove", "boot", "busy", "active_job",
    "module_version", "module_version_code", "record_present", "last_state", "last_time", "last_compiled",
    "last_packages", "last_error", "last_boot", "last_mode", "last_module_version_code", "last_reused",
    "last_verified", "last_deferred", "last_verification", "last_priority", "schedule_present",
    "schedule_scheduler", "schedule_enabled", "schedule_state", "schedule_interval_hours", "schedule_next_after",
    "schedule_kind", "last_idle_remaining", "last_diagnostic", "current_boot",
    "total_since", "total_compiles", "total_wall_ms", "total_cpu_ms", "total_size_kb", "total_reuses", "total_avoided_ms",
    "runtime_sampled", "runtime_jit_ms", "runtime_proc_s", "runtime_aot"]);
  const errors = Object.freeze({
    none: "", busy: "Ya hay una tarea en ejecución. Actualiza el estado más tarde.", root_required: "No se concedieron permisos Root; no se ejecutó la operación.",
    module_inactive: "El módulo está desactivado o pendiente de desinstalación; no se ejecutó la optimización.", device_hot: "La batería alcanzó los 55 °C; se aplazó la tarea automática.",
    battery_low: "La batería está al 15 % o menos; se aplazó la tarea automática.", device_interactive: "Esperando a que se apague la pantalla o a 10 minutos de inactividad.",
    power_unknown: "No se pudo leer el nivel de batería; se aplazó la tarea automática.", thermal_unknown: "No se pudo leer la temperatura de la batería; se aplazó la tarea automática.",
    screen_unknown: "No se pudo confirmar la inactividad; se aplazó la tarea automática.", boot_timeout: "Se agotó el tiempo de espera del inicio del sistema; se intentará en el próximo arranque.",
    boot_incomplete: "El sistema todavía está iniciándose; se aplazó la tarea automática.", idle_unknown: "No se pudo determinar el tiempo de inactividad; se esperará a que se apague la pantalla.",
    compile_failed: "El sistema no completó la compilación. Se conservaron los registros de ejecución.", compile_timeout: "Se agotó el tiempo de espera de la compilación y se solicitó cancelar. Revisa los registros de ejecución.",
    compile_cancel_failed: "No se pudo confirmar la cancelación de la tarea de compilación de ART. No se iniciará otra tarea por ahora; revisa los registros en C17.",
    restore_cancel_failed: "No se pudo confirmar la cancelación de la tarea de restauración de ART. Se conservó el registro del fallo; consulta C17.",
    art_cancel_unavailable: "El sistema no permite cancelar tareas de ART de forma fiable. Esta ejecución no se inició.", timeout_unavailable: "El sistema no ofrece un límite de tiempo de operación fiable. Esta ejecución no se inició.",
    art_unverified: "Los resultados actuales de ART todavía no se verificaron.", interrupted: "La tarea se interrumpió y se intentó cancelar. Comprueba los registros para saber si terminó.",
    restore_failed: "No se completó la restauración del método de compilación del sistema. Se conservó el registro del fallo.", restore_timeout: "Se agotó el tiempo de espera de la restauración. Se conservó el registro del fallo.",
    no_package: "No se reconocieron componentes de la interfaz del sistema.", package_query_failed: "Falló la consulta de componentes del sistema; la operación no se completó.",
    fingerprint_write_failed: "No se pudieron guardar los registros de componentes.", status_write_failed: "No se pudieron guardar los registros de ejecución.",
    path_link_rejected: "Falló la validación de la ruta del módulo; se detuvo la operación.", data_path_rejected: "Falló la validación del directorio de registros.",
    module_path_rejected: "Falló la validación de la ruta de instalación del módulo.", module_id_rejected: "Falló la validación de identidad del módulo.",
    module_version_rejected: "El registro de versión del módulo es anómalo.", migration_failed: "No se completó la migración de registros antiguos. Se conservaron los registros.",
    mode_rejected: "Este modo de ejecución no es compatible.", data_create_failed: "No se pudo crear el directorio de registros de ejecución.",
    uninstall_incomplete: "Quedan registros después de la desinstalación. Revísalos en C17.", uninstall_failed: "No se completó la limpieza de la desinstalación. Se conservaron los registros."
  });
  let callbackCounter = 0;
  let snapshot = null;
  let reading = false;
  let acting = false;
  let unconfirmed = false;
  let timer = null;
  let readError = "";
  let actionNotice = "";
  let holdUntil = 0;
  let actionKind = "";
  const el = (id) => document.getElementById(id);
  const hidden = () => document.hidden || window.c17HostPaused === true;
  function text(id, value) { el(id).textContent = value; }
  function count(value, maximum) {
    if (!/^[0-9]{1,10}$/.test(value || "")) return null;
    const result = Number(value);
    return Number.isSafeInteger(result) && result <= maximum ? result : null;
  }
  function parse(raw) {
    if (typeof raw !== "string" || raw.length > 16384) throw new Error("Tamaño de registro de ejecución no válido");
    const data = Object.create(null);
    let frame = false;
    let ended = false;
    for (const original of raw.replace(/\r/g, "").split("\n")) {
      const line = original.trim();
      if (!line) continue;
      if (line === "C17_WEB:begin" || line === "C17_PERF:begin") {
        if (frame || ended) throw new Error("El registro de ejecución está duplicado");
        frame = true;
      } else if (line === "C17_WEB:end" || line === "C17_PERF:end") {
        if (!frame || ended) throw new Error("El registro de ejecución está incompleto");
        frame = false; ended = true;
      } else if (frame) {
        const match = /^(?:C17_WEB:|C17_PERF:)?([a-z_]+)=(.*)$/.exec(line);
        if (!match) throw new Error("Formato del registro de ejecución no válido");
        if (!fields.has(match[1])) continue;
        if (Object.prototype.hasOwnProperty.call(data, match[1]) || match[2].length > 1024) throw new Error("Campo del registro de ejecución no válido");
        data[match[1]] = match[2];
      }
    }
    if (!ended || frame) throw new Error("El registro de ejecución está incompleto");
    for (const key of ["installed", "pending", "disabled", "remove", "record_present"]) {
      if (!/^[01]$/.test(data[key] || "")) throw new Error("No se pudo confirmar el estado del módulo");
    }
    if (!/^[0-9a-f-]{36}$/i.test(data.boot || "")) throw new Error("No se pudo confirmar el registro del arranque actual");
    if (!/^(0|1|unknown)$/.test(data.busy || "") || !/^(0|1|unknown)$/.test(data.active_job || "")) throw new Error("No se pudo confirmar el estado de la tarea");
    if (data.installed === "1" && count(data.module_version_code, 9999) === null) throw new Error("No se pudo confirmar la versión del módulo");
    if (data.record_present === "1") {
      if (!/^(ok|current|waiting|running|deferred|failed)$/.test(data.last_state || "")) throw new Error("No se pudo confirmar el estado de ejecución");
      if (count(data.last_time, 9999999999) === null || count(data.last_module_version_code, 9999) === null ||
        !/^[0-9a-f-]{36}$/i.test(data.last_boot || "") || !/^(boot|run|auto|idle|scheduled|uninstall)$/.test(data.last_mode || "")) throw new Error("No se pudo confirmar el origen del registro");
      for (const key of ["compiled", "packages", "reused", "verified", "deferred"]) {
        if (count(data["last_" + key], 9999) === null) throw new Error("No se pudo confirmar el contador de ejecuciones");
      }
      if (!/^[a-z0-9_:.\-]{1,120}$/i.test(data.last_error || "")) throw new Error("No se pudo confirmar el registro del error");
    }
    return data;
  }
  function currentRecord(data) {
    return data.record_present === "1" && data.last_boot === data.boot &&
      data.last_module_version_code === data.module_version_code && Number(data.module_version_code) >= EXPECTED_VERSION;
  }
  function errorMessage(code) {
    return Object.prototype.hasOwnProperty.call(errors, code) ? errors[code] : "La operación no se completó. Consulta los detalles en C17 (" + code + "）。";
  }
  function present(data, localBusy) {
    const current = currentRecord(data);
    const result = { title: "Instalado", message: "Se comprueba cuando hace falta y se reutilizan los resultados válidos.", tone: "blue", canRun: true, canToggle: true, running: false };
    if (data.installed !== "1") return { title: data.pending === "1" ? "Pendiente de reinicio" : "No instalado", message: data.pending === "1" ? "El módulo complementario se cargará después de reiniciar." : "Instala el módulo complementario desde C17.", tone: "neutral", canRun: false, canToggle: false, running: false };
    if (data.remove === "1") return { title: "Pendiente de desinstalación", message: "La desinstalación y la restauración se procesarán tras reiniciar.", tone: "red", canRun: false, canToggle: false, running: false };
    const uncertain = data.busy === "unknown" || data.active_job === "unknown";
    const live = localBusy || data.busy === "1";
    const unresolved = current && /^(waiting|running)$/.test(data.last_state) && !live;
    // A recorded cancel failure only holds while its task marker still exists; the module drops an unknown one after 15 minutes.
    const cancelled = current && data.active_job !== "0" && /^(compile_cancel_failed|restore_cancel_failed)$/.test(data.last_error || "");
    if (uncertain) return { title: "No se puede confirmar", message: "El estado de la tarea aún no está confirmado. No se iniciará otra tarea por ahora.", tone: "amber", canRun: false, canToggle: false, running: false };
    if (cancelled || data.active_job === "1" && !live) return { title: "Cancelación sin confirmar", message: "No se pudo confirmar que la tarea del sistema haya terminado.", tone: "red", canRun: false, canToggle: false, running: false };
    if (data.disabled === "1") return { title: "Desactivado", message: live ? "Todavía hay que confirmar que termine la tarea actual; la desactivación impide el procesamiento automático posterior." : "El procesamiento automático está desactivado.", tone: "amber", canRun: false,
      canToggle: !live && data.pending !== "1" && Number(data.module_version_code) >= EXPECTED_VERSION, running: live };
    if (data.pending === "1") return { title: "Pendiente de reinicio", message: "La nueva versión se cargará después de reiniciar.", tone: "cyan", canRun: false, canToggle: false, running: false };
    if (Number(data.module_version_code) < EXPECTED_VERSION) return { title: "Pendiente de actualización", message: "Actualiza el módulo complementario desde C17.", tone: "amber", canRun: false, canToggle: false, running: false };
    if (live) return { title: current && data.last_state === "waiting" && data.last_mode !== "run" ? "Esperando inactividad" : "Optimizando", message: "La tarea está en curso; el estado se actualizará automáticamente.", tone: "cyan", canRun: false, canToggle: false, running: true };
    if (unresolved) return { title: "Resultado pendiente de confirmar", message: "El registro actual todavía no está cerrado. Actualiza el estado o consulta C17.", tone: "amber", canRun: false, canToggle: false, running: false };
    if (!current && data.record_present === "1") result.message = "El registro anterior es solo orientativo; todavía no se comprobó el estado actual.";
    if (current && data.last_state === "ok") { result.title = "Optimización completada"; result.message = "El procesamiento terminó; no hace falta reiniciar manualmente la interfaz del sistema."; result.tone = "green"; }
    if (current && data.last_state === "current") { result.title = "No hace falta compilar"; result.message = "Se reutilizaron los resultados válidos."; result.tone = "green"; }
    if (current && data.last_state === "deferred") { result.title = "Aplazado"; result.message = data.last_mode === "run" ? "Esta vez no se ejecutó; consulta el motivo." : "Se volverá a comprobar cuando se cumplan las condiciones de inactividad."; result.tone = "amber"; }
    if (current && data.last_state === "deferred" && /^(art_cancel_unavailable|timeout_unavailable)$/.test(data.last_error || "")) {
      result.title = "La compilación no se inició"; result.message = errorMessage(data.last_error);
    }
    if (current && data.last_state === "failed") { result.title = "No completado"; result.message = "Se conservaron los registros de ejecución; consulta el motivo."; result.tone = "red"; }
    return result;
  }
  function nativeExec(kind) {
    return new Promise((resolve, reject) => {
      const name = "C17WebCallback" + (++callbackCounter);
      const timeout = window.setTimeout(() => {
        delete window[name];
        reject(new Error("El administrador tardó demasiado en responder; eso no significa que la tarea del sistema se haya cancelado"));
      }, c17 ? 50000 : 12000);
      window[name] = (errno, stdout, stderr) => {
        window.clearTimeout(timeout); delete window[name];
        const code = Number(errno);
        if (!Number.isInteger(code) || typeof stdout !== "string" || typeof stderr !== "string") reject(new Error("El administrador devolvió un formato no válido"));
        else resolve({ code, stdout, stderr });
      };
      try {
        if (c17) {
          if (kind === "read") c17.readStatus(name);
          else if (kind === "run") c17.run(name);
          else c17.setEnabled(kind === "enable", name);
        } else ksu.exec(COMMANDS[kind], "{}", name);
      }
      catch (_) { window.clearTimeout(timeout); delete window[name]; reject(new Error("La interfaz de ejecución del administrador no está disponible")); }
    });
  }
  function timestamp(value) {
    const seconds = count(value, 9999999999);
    if (!seconds) return "Sin hora registrada";
    return new Date(seconds * 1000).toLocaleString("es-AR", { month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", hour12: false });
  }
  function scheduleText(data) {
    if (data.schedule_kind === "on_demand") {
      if (data.schedule_state === "scheduled") return "Plan de comprobación registrado · el sistema puede aplazarlo";
      if (data.schedule_state === "inactive") return "No hay tareas pendientes";
      if (data.schedule_state === "unavailable") return "La comprobación automática no está disponible; consulta C17";
      if (data.schedule_state === "running") return "Confirmando la tarea automática";
      if (data.schedule_state === "deferred") return "Esta ronda se aplazó; se espera al próximo evento de inactividad.";
      if (data.schedule_state === "blocked") return "El resultado de la tarea no está confirmado; la comprobación automática se pausa durante este arranque";
      return "El plan de comprobación todavía no está confirmado";
    }
    if (data.schedule_present !== "1") return "La comprobación automática no está confirmada; consulta C17";
    if (data.schedule_enabled === "0" || data.schedule_state === "disabled") return "Comprobación automática desactivada";
    if (data.schedule_state === "registered") return "Programado · esperando inactividad";
    if (data.schedule_state === "unavailable") return "La comprobación automática no está disponible; consulta C17";
    return "Estado de la comprobación automática pendiente de confirmar";
  }
  const RING_LENGTH = 213.6;
  // Pure views over a parsed snapshot, exported for the local fixtures.
  function ringView(data, current) {
    const verified = count(data.last_verified, 9999), packages = count(data.last_packages, 9999);
    const known = data.record_present === "1" && verified !== null && packages !== null && packages > 0;
    const share = known ? Math.min(1, verified / packages) : 0;
    return { dash: (share * RING_LENGTH).toFixed(1) + " " + RING_LENGTH, opacity: known && !current ? "0.55" : "1", label: known ? verified + "/" + packages : "—" };
  }
  function seconds(ms) { return (ms / 1000).toFixed(ms >= 10000 ? 0 : 1) + " s"; }
  // Costs are what ART reported for each compile; "avoided" is an estimate; the runtime lines are one sample, not a comparison.
  function benefitView(data) {
    const compiles = count(data.total_compiles, 9999999), reuses = count(data.total_reuses, 9999999);
    const wall = count(data.total_wall_ms, 99999999999999), saved = count(data.total_avoided_ms, 99999999999999);
    const sizeKb = count(data.total_size_kb, 99999999999), since = count(data.total_since, 9999999999);
    const ledger = since !== null && since > 0;
    const sampled = count(data.runtime_sampled, 9999999999), age = count(data.runtime_proc_s, 99999999);
    const jit = count(data.runtime_jit_ms, 99999999999999);
    const haveSample = sampled !== null && sampled > 0 && age !== null && age > 0;
    const aot = data.runtime_aot === "1";
    const longEnough = haveSample && jit !== null && age >= 60;
    return {
      since: ledger ? "Desde " + timestamp(data.total_since) + "" : "Aún no hay registros",
      compiles: ledger ? (compiles || 0) + " veces" : "—",
      compilesNote: ledger ? "Tiempo empleado: " + seconds(wall || 0) + (sizeKb ? " · nuevos archivos: " + Math.round(sizeKb / 1024) + " MB" : "") : "Tiempo medido por ART",
      saved: ledger ? (reuses || 0) + " veces" : "—",
      savedNote: ledger ? "Tiempo de compilación ahorrado aprox.: " + seconds(saved || 0) + " (estimado)" : "Comparado con recompilar cada vez",
      aot: haveSample ? (aot ? "En uso" : "Sin uso") : "—",
      aotNote: haveSample ? (aot ? "La interfaz del sistema utiliza código precompilado" : "La interfaz del sistema todavía usa interpretación") : "Uso de código precompilado",
      jit: longEnough ? Math.round(jit / (age / 60)) + " ms/min" : "—",
      jitNote: longEnough ? "Uso del hilo JIT · el proceso lleva " + Math.round(age / 60) + " min" : haveSample ? "El proceso acaba de iniciarse; la muestra es demasiado breve" : "Uso del hilo JIT"
    };
  }
  function renderRing(current) {
    const view = ringView(snapshot, current);
    el("ring-arc").setAttribute("stroke-dasharray", view.dash);
    el("ring-arc").setAttribute("opacity", view.opacity);
    text("ring-text", view.label);
  }
  function renderBenefits() {
    const view = benefitView(snapshot);
    text("benefit-since", view.since);
    text("benefit-compiles", view.compiles); text("benefit-compiles-note", view.compilesNote);
    text("benefit-saved", view.saved); text("benefit-saved-note", view.savedNote);
    text("benefit-aot", view.aot); text("benefit-aot-note", view.aotNote);
    text("benefit-jit", view.jit); text("benefit-jit-note", view.jitNote);
  }
  function render() {
    const connected = !!bridge || demo;
    const holding = Date.now() < holdUntil;
    let view = snapshot ? present(snapshot, false) : { title: "Leyendo…", message: "Conectando con el administrador…", tone: "neutral", canRun: false, canToggle: false, running: false };
    if (snapshot && !view.running && view.canToggle && (acting || unconfirmed || holding)) {
      view = { title: unconfirmed ? "Esperando confirmación" : acting ? actionKind === "run" ? "Enviando" : "Procesando" : "Enviado",
        message: unconfirmed ? "El resultado de la tarea aún no está confirmado. Actualiza el estado para comprobarlo." : "Confirmando el nuevo registro de ejecución.", tone: "cyan", canRun: false, canToggle: false, running: false };
    }
    if (!connected) view = { title: "Consulta C17", message: "El administrador actual no ofrece una interfaz de ejecución.", tone: "neutral", canRun: false, canToggle: false, running: false };
    else if (readError) view = { title: "No se puede confirmar", message: "No se pudo leer el estado real. Actualiza el estado o consulta C17.", tone: "amber", canRun: false, canToggle: false, running: false };
    text("status-title", view.title); text("status-message", view.message);
    el("status-card").dataset.tone = view.tone;
    el("status-card").dataset.running = String(view.running && !hidden());
    el("optimize").disabled = !bridge || !view.canRun || reading || acting || unconfirmed || holding;
    el("module-toggle").disabled = !bridge || !view.canToggle || reading || acting || unconfirmed || holding;
    el("refresh").disabled = !connected || reading;
    text("optimize-title", acting || unconfirmed ? "Enviando…" : view.running ? "Optimización en curso" : "Optimizar ahora");
    el("demo-notice").hidden = !demo;
    text("action-hint", actionNotice || (reading ? "Actualizando…" : demo ? "Demostración sin conexión; no se ejecutarán operaciones" : !connected ? "Puedes consultar y operar desde la página del módulo complementario de C17" : view.canRun ? "Ejecución manual inmediata; se mantienen la protección contra tiempos de espera y la cancelación" : "No se iniciará otra tarea en el estado actual"));
    const diagnoses = { uuid_missing: "El sistema no proporcionó un identificador de tarea que se pueda seguir.", cancel_pending: "Se solicitó la cancelación, pero el sistema aún no confirmó la finalización.", cancel_unrecognized: "No se pudo verificar la respuesta de cancelación del sistema.", final_unrecognized: "No se pudo verificar el resultado final del sistema.", active_unknown: "Hay un registro de tarea pendiente de confirmar.", client_failed: "El administrador de paquetes del sistema no respondió temporalmente; se volvió a intentar automáticamente." };
    const reason = readError || (snapshot && snapshot.record_present === "1" && snapshot.last_error !== "none" ? errorMessage(snapshot.last_error) + " " + (diagnoses[snapshot.last_diagnostic] || "") : "");
    text("reason", reason); el("reason").hidden = !reason;
    if (!snapshot) return;
    text("version", "Módulo complementario " + (snapshot.module_version || "—"));
    const hasRecord = snapshot.record_present === "1";
    const current = currentRecord(snapshot);
    text("source", hasRecord ? (snapshot.last_mode === "run" ? "Optimización manual" : snapshot.last_mode === "boot" ? "Automático al iniciar" : snapshot.last_mode === "uninstall" ? "Restauración al desinstalar" : "Automático en inactividad") + (current ? "" : " · registro histórico") : "Todavía no hay registros de ejecución");
    text("time", hasRecord ? timestamp(snapshot.last_time) : "—");
    for (const key of ["compiled", "reused", "deferred"]) text(key, hasRecord ? snapshot["last_" + key] : "—");
    text("verified", hasRecord ? snapshot.last_verified + "/" + snapshot.last_packages : "—");
    renderRing(current);
    renderBenefits();
    text("module-toggle-title", snapshot.disabled === "1" ? "Activar módulo" : "Desactivar módulo");
    text("module-toggle-note", snapshot.disabled === "1" ? "Reanudar el procesamiento posterior" : "Pausar el procesamiento posterior");
    const plans = { scheduled: "Programado; a la espera de inactividad", registered: "Programado; a la espera de inactividad", inactive: "Sin tareas pendientes", disabled: "Desactivado", unavailable: "Temporalmente no disponible", deferred: "Esperando la próxima inactividad", blocked: "Pausar tareas nuevas", running: "Confirmando" };
    text("auto-title", snapshot.disabled === "1" ? "Desactivado" : plans[snapshot.schedule_state] || "Estado pendiente de confirmar");
    text("schedule", scheduleText(snapshot));
    text("auto-tag", snapshot.disabled === "1" ? "Desactivado" : plans[snapshot.schedule_state] || "Pendiente de confirmar");
    text("record-validity", !hasRecord ? "Aún no hay registros" : current ? "Arranque actual · versión actual del módulo" : "Registro histórico; no confirma la tarea actual");
    const verified = Number(snapshot.last_verified || 0);
    const packages = Number(snapshot.last_packages || 0);
    text("verification", !hasRecord || verified === 0 ? "Aún no verificado" : verified === packages && packages > 0 && snapshot.last_verification === "verified" ? "Se verificaron todos los componentes registrados" : "Solo se verificaron algunos componentes");
    text("priority", snapshot.last_priority === "background" ? "Prioridad de ART en segundo plano del sistema" : hasRecord ? "Modo de compatibilidad del sistema" : "—");
  }
  function scheduleRefresh() {
    if (timer) window.clearTimeout(timer);
    timer = null;
    const changing = acting || unconfirmed || snapshot && snapshot.busy === "1";
    if (bridge && !hidden()) timer = window.setTimeout(() => refresh(false), changing ? REFRESH_MS : 15000);
  }
  async function refresh(manual) {
    if (reading || hidden()) return;
    reading = true; render();
    try {
      if (demo) snapshot = parse(window.C17_WEBUI_DEMO);
      else if (bridge) {
        const result = await nativeExec("read");
        if (result.code !== 0) throw new Error(result.code === 21 ? "No se concedieron permisos Root" : "Falló la lectura del estado (" + result.code + "），Consulta C17");
        snapshot = parse(result.stdout);
      }
      readError = "";
      if (manual && snapshot && unconfirmed && snapshot.busy === "0" && snapshot.active_job === "0" && Date.now() >= holdUntil) unconfirmed = false;
      if (manual && !acting) actionNotice = "Estado actualizado";
    text("connection", demo ? "Demostración de solo lectura · sin conexión con el teléfono" : bridge ? "Actualizado recién · la actualización se detiene al salir de la página" : "El administrador actual no ofrece una interfaz de ejecución");
    } catch (error) { readError = error.message; }
    finally { reading = false; render(); scheduleRefresh(); }
  }
  async function act(kind) {
    if (!bridge || !snapshot || reading || acting || unconfirmed || Date.now() < holdUntil || hidden()) return;
    const view = present(snapshot, false);
    if (kind === "run" ? !view.canRun : !view.canToggle) return;
    acting = true; actionKind = kind; actionNotice = kind === "run" ? "Enviando la tarea de optimización…" : "Actualizando el estado del módulo…"; render();
    try {
      holdUntil = Date.now() + 8000;
      const result = await nativeExec(kind);
      if (result.code !== 0) throw new Error("La operación no se completó (" + result.code + "); consulta los registros de ejecución");
      actionNotice = kind === "run" ? "Enviado; el registro de ejecución es la referencia" : "Operación enviada; confirmando el estado";
    } catch (error) {
      actionNotice = error.message; unconfirmed = true;
    } finally { acting = false; await refresh(false); render(); }
  }
  el("refresh").addEventListener("click", () => refresh(true));
  el("optimize").addEventListener("click", () => act("run"));
  el("module-toggle").addEventListener("click", () => act(snapshot && snapshot.disabled === "1" ? "enable" : "disable"));
  el("auto-details").addEventListener("click", () => { el("details").open = true; el("details").scrollIntoView({ behavior: "auto", block: "nearest" }); });
  document.addEventListener("visibilitychange", () => {
    document.querySelector(".page").classList.toggle("paused", hidden());
    if (hidden()) { if (timer) window.clearTimeout(timer); timer = null; render(); }
    else refresh(false);
  });
  window.addEventListener("pagehide", () => { if (timer) window.clearTimeout(timer); timer = null; });
  window.addEventListener("pageshow", () => { if (!hidden() && bridge) refresh(false); });
  // Pure, read-only helpers allow deterministic local fixtures without a Root bridge.
  window.C17WebUI = Object.freeze({ parse, present, currentRecord, ringView, benefitView });
  if (bridge || demo) refresh(false); else render();
})();
