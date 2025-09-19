# Requirements

## Software requirements

- JDK 21.0 or later - Download it from [Adoptium](https://adoptium.net/){target="_blank"}
- A key for OpenAI API (provided by the workshop organizer)
- Podman or Docker - See [Podman installation](https://podman.io/getting-started/installation){target="_blank"} or [Docker installation](https://docs.docker.com/get-docker/){target="_blank"}
  - If you use Podman, Podman Desktop provides a great user experience to manage your containers: [Podman Desktop](https://podman-desktop.io/docs/installation){target="_blank"}
- Git (not mandatory) - See [Git installation](https://git-scm.com/book/en/v2/Getting-Started-Installing-Git){target="_blank"}
- An IDE with Java support (IntelliJ, Eclipse, VSCode with the Java extension, etc.)
- A terminal

???+ note "Want to use our environment rather than yours?"
    If you are running this as part of an instructor-led workshop and have been provided a virtual machine, [click here](rhel-setup.md) to learn about how to use it if you'd prefer it over using your own laptop.

## AI Model Requirements

You will need an OpenAI API key to complete this workshop. If your instructor has provided one for you to use, use it! [Click here](https://platform.openai.com/docs/quickstart/create-and-export-an-api-key){target="_blank"} to create one if you do not have one. 

??? info "Did your instructor not provide a key?"
    You should receive $5 in free OpenAI trial credits if this is the first time you are creating an OpenAI developer account. If you already have an account and have used your free trial credits, then you will need to fund your account.

    Don't worry, this workshop will not cost much. You can [check out the OpenAI pricing calculator](https://openai.com/api/pricing/){target="_blank"}.

    The cost for going through this workshop should not exceed $0.25 (~&euro;0.22).

Once you have an OpenAI API key, make sure you have set it as an environment variable, eg:

```bash
export OPENAI_API_KEY=<your-key>
```

```powershell
$Env:OPENAI_API_KEY = <your-key>
```

## Good to know

You can run a Quarkus application in [_dev mode_](https://quarkus.io/guides/maven-tooling#dev-mode){target="_blank"} by running the following command in the project directory:

### Quarkus dev mode

```bash
./mvnw quarkus:dev
```

This will start the application in dev mode, which means that the application will be recompiled automatically on every change in the source code.
Just refresh the browser to see the changes.
The application serves the application at [http://localhost:8080/](http://localhost:8080/).

!!! warning "Stopping the application"
    When switching steps, make sure to stop the running application before starting the next step. 
    You can exit the application by pressing `Ctrl+C` in the terminal where the application is running.

### Dev UI

Quarkus ships with a [Dev UI](https://quarkus.io/guides/dev-ui){target="_blank"}, which is available only in dev mode only at [http://localhost:8080/q/dev/](http://localhost:8080/q/dev/).
The Dev UI can be seen as your toolbox when building Quarkus applications.

### Debugging

For debugging a Quarkus application running in dev mode, put your breakpoints and select `Run > Attach to Process`, then select the Quarkus process (in IntelliJ).

## Let's get started

It's time to get started with the workshop.

### Getting the workshop material

Either use `git` or download the repository as a zip file.

#### With Git

If you haven't already, clone the repository and checkout the `main` branch.

```shell
git clone https://github.com/quarkusio/quarkus-langchain4j-workshop.git
```

Then navigate to the directory:

```shell
cd quarkus-langchain4j-workshop
```

#### Direct Download

If you didn't use the `git` approach, you can download the repository as a zip file from the [GitHub repository](https://github.com/quarkusio/quarkus-langchain4j-workshop/archive/refs/heads/main.zip):

```shell
curl -L -o workshop.zip https://github.com/quarkusio/quarkus-langchain4j-workshop/archive/refs/heads/main.zip
```

Then unzip the file and navigate to the directory:

```shell
unzip workshop.zip
cd quarkus-langchain4j-workshop-main
```

### Warming the caches

This workshop needs to download all sorts of Maven artifacts and Docker images.
Some of these artifacts are large, and because we have to share the internet connection at the workshop location, it is better to download them before the workshop.

If you’re getting ready for a workshop, you might find it helpful to pre-download some of these artifacts.
This can save strain on shared bandwidth.
If, however, you’re already attending a workshop, don’t worry about warming anything up.

### Warming up Maven

To warm up Maven, you can run the following command in the root directory of the project:

```shell
./mvnw verify
```

### Warming up Docker images

To download the Docker images, you can run one of the following commands:

=== "Podman"

    ``` shell
    podman pull pgvector/pgvector:pg16
    ```

=== "Docker"

    ```shell
    docker pull pgvector/pgvector:pg16
    ```

### Import the project in your IDE

Then, open the project from the `section-1/step-01` directory in your IDE and use that directory throughout the workshop. If you get stuck anywhere and would like to move on, simply switch to the `step-xx` directory of the last step you completed.

Once done, you can move on to the next step: [Step 1](section-1/step-01.md).
