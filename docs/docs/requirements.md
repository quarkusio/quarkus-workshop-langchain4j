# Requirements

## Software Requirements

- **JDK 21.0 or later** – [Download from Adoptium](https://adoptium.net/){target="_blank"}
- **OpenAI API key** – provided by the workshop organizer
- **Podman or Docker** – see [Podman installation](https://podman.io/getting-started/installation){target="_blank"} or [Docker installation](https://docs.docker.com/get-docker/){target="_blank"}
    - If you use Podman, we recommend [Podman Desktop](https://podman-desktop.io/docs/installation){target="_blank"} for easier container management.
- **IDE with Java support** – IntelliJ, Eclipse, VSCode (with Java extension), etc.
- **Terminal** – to run commands
- _(Optional)_ **Git** – [Installation guide](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git){target="_blank"}

???+ note "Want to use our environment rather than yours?"
If you are running this as part of an instructor-led workshop and have been provided a virtual machine, [click here](rhel-setup.md) to learn about how to use it if you'd prefer it over using your own laptop.


---

## AI Model Requirements

You will need an OpenAI API key to complete this workshop.  
If your instructor provided a key, use that one. Otherwise, [create an API key](https://platform.openai.com/docs/quickstart/create-and-export-an-api-key){target="_blank"}.

??? info "No instructor-provided key?"
    New OpenAI developer accounts receive $5 in free trial credits.  
    If you already used your credits, you’ll need to fund your account.
    
    !!! tip
        Don’t worry — this workshop is inexpensive. The total cost should not exceed **$0.50 (~€0.43)**.  
        See the [OpenAI pricing calculator](https://openai.com/api/pricing/){target="_blank"}.

Once you have a key, set it as an environment variable:

=== "Linux / macOS"
    ```bash
    export OPENAI_API_KEY=<your-key>
    ```

=== "Windows PowerShell"
    ```powershell
    $Env:OPENAI_API_KEY = <your-key>
    ```

---

## Good to Know

### Quarkus Dev Mode

Run your Quarkus app in **dev mode** from the project directory:

```bash
./mvnw quarkus:dev
```

Dev mode automatically recompiles your code on every change.
Your app will be available at http://localhost:8080/.

!!! warning "Switching steps"
    Stop the running application (Ctrl+C) before starting the next step.

### Dev UI

Quarkus ships with a [Dev UI](https://quarkus.io/guides/dev-ui){target="\_blank"}, available only in dev mode at [http://localhost:8080/q/dev/](http://localhost:8080/q/dev/).
Think of it as your **toolbox** when building Quarkus applications.

### Debugging

To debug an app in dev mode, put breakpoints in your code and attach your IDE debugger.
In IntelliJ, use `Run > Attach to Process` and select the Quarkus process.
Other IDEs (Eclipse, VSCode) support similar remote debugging.

---

## Getting the Workshop Material

Either clone the repository with Git or download a ZIP archive.

### With Git

```shell
git clone https://github.com/quarkusio/quarkus-langchain4j-workshop.git
cd quarkus-langchain4j-workshop
```

### Direct Download

```shell
curl -L -o workshop.zip https://github.com/quarkusio/quarkus-langchain4j-workshop/archive/refs/heads/main.zip
unzip workshop.zip
cd quarkus-langchain4j-workshop-main
```

---

## Pre-Warming Caches

This workshop requires downloading Maven dependencies and Docker images.
To avoid bandwidth issues during the session, we recommend pre-downloading them.

### Warm up Maven

```shell
./mvnw verify
```

!!! tip 
    This command not only downloads dependencies but also verifies your setup before the workshop.

### Warm up Docker Images

* Podman: `podman pull pgvector/pgvector:pg17`
* Docker: `docker pull pgvector/pgvector:pg17`

---

## Importing the Project in Your IDE

!!! tip 
    Open the project from `section-1/step-01` in your IDE and use that directory throughout the workshop.

If you get stuck, simply switch to the `step-xx` directory of the last completed step.

---

## Next Step

Once ready, you can pick one of these entries points to start the workshop:

- If you discover Quarkus and Quarkus LangChain4j, start with [Section 1 - AI Apps](./section-1/step-01.md).
- If you want to learn more advanced AI-Infused features, such as MCP, Guardrails, Observability, and Fault Tolerance, start with [Section 1 - Step 08](./section-1/step-08.md).
- If you want to jump directly into agentic systems, start with [Section 2 - Agentic Workflows](./section-2/step-01.md).

