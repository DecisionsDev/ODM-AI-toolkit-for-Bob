# Prerequisites: installing the rules compiler

`odm build` needs IBM's `rules-compiler.jar` (the Build Command CLI). It comes from a licensed ODM install, so it is not shipped with the skill. Without it, the skill can scaffold and `check` a project but cannot validate that the rules compile.

## Where `odm` looks for it

`odm build`, `odm jdk` and `odm xom` take the first jar found, in this order:

1. **Project-local:** `buildcommand/rules-compiler/rules-compiler.jar`
   - in the working directory,
   - and in the project root: the directory of the `.properties` for `build`, the parent of the XOM dir for `xom`. Parent directories are never searched.
2. `--jar <path>` on the command line
3. `$ODM_RULES_COMPILER`
4. **Skill-wide:** `<skill>/tools/rules-compiler.jar`

The project-local jar wins, even over `--jar`. To use another jar for one project, remove or rename that project's `buildcommand/rules-compiler/rules-compiler.jar`. `odm build` and `odm jdk` print `rules compiler: <path>` so you can see which jar ran. When none is found, the error lists every path it tried and the `odm compiler` command to run.

**Never search the file system for `rules-compiler.jar`** (no `find ~`, `find /`, `mdfind`, `locate`). It is slow, and it can pick up a jar from an unrelated project or an unsupported ODM release. Use `odm compiler` (below).

**Which location to use:**
- **Project-local** (the default for a new install): each project pins its own ODM release, so projects on different releases (and so different JDKs) can live side by side. Install it in the project root, which is the `<parent>` directory given to `odm init`, next to the rule project and its `.properties`.
- **Skill-wide:** one jar shared by every project that has no project-local jar. Use it when the user asks for it, or when all their projects target the same ODM release.

If a `build_ruleset` MCP tool is connected, you can use it instead and skip this setup.

## 0. Check, then install with `odm compiler`

```bash
cd <project-root>
java -jar <skill>/scripts/odm.jar jdk             # prints the jar used, its ODM release, and the JDK
```

If `odm jdk` prints a jar, an ODM 9.x release and a JDK, go to step 3. If it reports `no rules compiler found`, run the command it prints, without asking first:

```bash
java -jar <skill>/scripts/odm.jar compiler --dir <project-root>
```

It installs `<project-root>/buildcommand/rules-compiler/rules-compiler.jar` from the first source that works:

1. **`$ODM_HOME`** (or `--odm-home <dir>`): copies `<ODM_HOME>/buildcommand/rules-compiler/rules-compiler.jar`.
2. **The ODM Docker image** when there is no ODM install: `docker create icr.io/cpopen/odm-k8s/odm:<tag>` (created with `--pull always`, so the newest fix pack of that tag is used, never a stale local copy; `docker` or `podman`), copies `buildcommand.zip` out of the image, extracts `rules-compiler/rules-compiler.jar`, and removes the container. No server is started and no port is opened. The first run pulls the image (about 1 GB).

Then it prints the ODM release and the JDK it needs, like `odm jdk`. A jar already installed is kept (`--force` replaces it).

**Pick the ODM release deliberately.** The compiler's release decides which JDK it must run on (9.0.x → 17, 9.5.x/9.6.x → 21, 9.7.x → 25), and the RuleApp it builds must be accepted by the target Decision Center / RES. **Image tag:** if the user asked for an ODM version, pass `--tag` with its release: `--tag 9.0`, `--tag 9.5` or `--tag 9.6` (a full version such as `9.5.0.1` is read as `9.5`). If the user named no version, leave `--tag` out: the default is `latest`, the newest release. Anything other than `latest` or 9.x is refused. Only ODM 9.x is supported.

Ask the user only when `odm compiler` fails: no `ODM_HOME` and no working Docker/Podman. Then they either set `ODM_HOME` to their ODM install or start Docker, and you run `odm compiler` again.

**Don't commit the jar.** It is an IBM-licensed file of about 50-60 MB. After a project-local install, tell the user that `buildcommand/rules-compiler/` should not be committed (for example, add it to their `.gitignore`). Don't edit their `.gitignore` yourself.

## Manual fallback

Use this only if `odm compiler` can't run (for example, no Java process may call Docker). Set the destination first:

```bash
DEST=<project-root>/buildcommand/rules-compiler   # project-local (default)
# DEST=<skill>/tools                              # skill-wide, only if the user asks for one jar for all projects
mkdir -p "$DEST"
```

**A. From an ODM install:**

```bash
cp "$ODM_HOME/buildcommand/rules-compiler/rules-compiler.jar" "$DEST/"
```

**B. From the ODM Docker image** (what `odm compiler` does):

```bash
C=$(docker create --pull always icr.io/cpopen/odm-k8s/odm:latest)   # or :9.0 / :9.5 / :9.6 for the version the user asked for
docker cp "$C":/opt/ibm/wlp/usr/servers/defaultServer/apps/decisioncenter.war/assets/buildcommand.zip buildcommand.zip
docker rm "$C"
unzip -j -o buildcommand.zip 'rules-compiler/rules-compiler.jar' -d "$DEST"
rm buildcommand.zip
```

## Using a jar kept elsewhere

To leave the jar in place (a shared ODM install, several ODM releases side by side), don't copy it. Point `odm` at it instead:

```bash
export ODM_RULES_COMPILER=/path/to/rules-compiler.jar
```

This only applies when the project has no `buildcommand/rules-compiler/rules-compiler.jar`, because the project-local jar comes first.

## 3. Verify the install and the JDK

```bash
ls -lh <project-root>/buildcommand/rules-compiler/rules-compiler.jar   # ~50-60 MB
cd <project-root> && java -jar <skill>/scripts/odm.jar jdk
```

`odm jdk` prints the jar it resolved (check it is the one you just installed), reads `Implementation-Version` from the jar's manifest, and prints the ODM release and the JDK it resolved. The JDK must be exactly the one for that release (see the table in `SKILL.md`). If it is missing, the user installs that JDK (IBM Semeru OpenJ9 recommended) or sets `ODM_JAVA_HOME` to it. Never work around it with another JDK: `build` refuses a JDK with the wrong version.

| Symptom | Fix |
|---|---|
| `no rules compiler found. Tried: …` | run the `odm compiler --dir <project-root>` command it prints |
| `no ODM_HOME and no working docker or podman` | ask the user to set `ODM_HOME` to an ODM install or to start Docker, then run `odm compiler` again |
| `rules compiler:` shows an unexpected jar | a project-local `buildcommand/rules-compiler/rules-compiler.jar` in the working directory or project root wins; remove or replace it |
| `no MANIFEST.MF` / `cannot parse ODM version` | not a genuine `rules-compiler.jar`; download it again |
| `ODM … is not supported` | the jar is not ODM 9.x; get the jar from an ODM 9.x release |
| required JDK not found | install that JDK or set `ODM_JAVA_HOME` |
