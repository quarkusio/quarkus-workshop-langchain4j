///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS info.picocli:picocli:4.7.5
//DEPS org.yaml:snakeyaml:2.2

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "deploy", mixinStandardHelpOptions = true, version = "1.0",
        description = "Miles and Smiles Kubernetes Deployment Tool",
        subcommands = {CommandLine.HelpCommand.class})
class deploy implements Callable<Integer> {

    @Command(name = "all", description = "Build, push, and deploy everything (requires OPENAI_API_KEY env var)")
    int all() {
        System.out.println("=== Miles and Smiles Complete Deployment ===\n");
        
        // Check for API key upfront
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("ERROR: OPENAI_API_KEY environment variable is not set");
            System.err.println("Please set it with: export OPENAI_API_KEY=your-key-here");
            return 1;
        }
        
        System.out.println("Step 1/3: Building container images...");
        if (build() != 0) {
            System.err.println("Build failed!");
            return 1;
        }
        
        System.out.println("\nStep 2/3: Pushing images to registry...");
        if (push() != 0) {
            System.err.println("Push failed!");
            return 1;
        }
        
        System.out.println("\nStep 3/3: Deploying to Kubernetes (including secret creation)...");
        if (deployToK8s() != 0) {
            System.err.println("Deployment failed!");
            return 1;
        }
        
        System.out.println("\n=== Deployment Complete! ===");
        System.out.println("\nTo get the application URL, run: ./deploy.java get-route");
        System.out.println("To view logs, run: ./deploy.java logs-main");
        return 0;
    }

    @Command(name = "build", description = "Build both container images")
    int build() {
        System.out.println("Building multi-agent-system...");
        if (runCommand("multi-agent-system", "./mvnw", "clean", "package", "-DskipTests") != 0) {
            return 1;
        }
        System.out.println("Building remote-a2a-agent...");
        if (runCommand("remote-a2a-agent", "./mvnw", "clean", "package", "-DskipTests") != 0) {
            return 1;
        }
        System.out.println("Build complete!");
        return 0;
    }

    @Command(name = "push", description = "Push both images to registry")
    int push() {
        System.out.println("Building and pushing multi-agent-system image...");
        if (runCommand("multi-agent-system", "./mvnw", "quarkus:image-push", 
                "-Dquarkus.container-image.build=true") != 0) {
            return 1;
        }
        System.out.println("Building and pushing remote-a2a-agent image...");
        if (runCommand("remote-a2a-agent", "./mvnw", "quarkus:image-push", 
                "-Dquarkus.container-image.build=true") != 0) {
            return 1;
        }
        System.out.println("Push complete!");
        return 0;
    }

    @Command(name = "deploy", description = "Deploy all components to Kubernetes (requires OPENAI_API_KEY env var)")
    int deployToK8s() {
        String apiKey = System.getenv("OPENAI_API_KEY");
        if (apiKey == null || apiKey.isEmpty()) {
            System.err.println("ERROR: OPENAI_API_KEY environment variable is not set");
            System.err.println("Please set it with: export OPENAI_API_KEY=your-key-here");
            return 1;
        }

        System.out.println("Creating namespace...");
        if (kubectl("apply", "-f", "kubernetes/namespace.yaml") != 0) return 1;

        System.out.println("Creating ConfigMap...");
        if (kubectl("apply", "-f", "kubernetes/configmap.yaml") != 0) return 1;

        System.out.println("Creating Secret with OPENAI_API_KEY...");
        if (createSecret(apiKey) != 0) return 1;

        System.out.println("Deploying PostgreSQL...");
        if (kubectl("apply", "-f", "kubernetes/postgresql.yaml") != 0) return 1;

        System.out.println("Waiting for PostgreSQL to be ready...");
        if (kubectl("wait", "--for=condition=ready", "pod", "-l", "app=postgresql", 
                "-n", "miles-and-smiles", "--timeout=120s") != 0) return 1;

        System.out.println("Deploying A2A Agent...");
        if (kubectl("apply", "-f", "kubernetes/a2a-agent.yaml") != 0) return 1;

        System.out.println("Waiting for A2A Agent to be ready...");
        if (kubectl("wait", "--for=condition=ready", "pod", 
                "-l", "app.kubernetes.io/name=miles-and-smiles-a2a",
                "-n", "miles-and-smiles", "--timeout=120s") != 0) return 1;

        System.out.println("Deploying Main Application...");
        if (kubectl("apply", "-f", "kubernetes/main-app.yaml") != 0) return 1;

        System.out.println("Waiting for Main Application to be ready...");
        if (kubectl("wait", "--for=condition=ready", "pod",
                "-l", "app.kubernetes.io/name=miles-and-smiles",
                "-n", "miles-and-smiles", "--timeout=120s") != 0) return 1;

        System.out.println("Creating OpenShift Route...");
        if (kubectl("apply", "-f", "kubernetes/route.yaml") != 0) return 1;

        System.out.println("\nDeployment complete!");
        System.out.println("\n" + "=".repeat(50));
        System.out.println("Application URL:");
        System.out.println("=".repeat(50));
        kubectl("get", "route", "miles-and-smiles", "-n", "miles-and-smiles",
                "-o", "jsonpath={.spec.host}");
        System.out.println("\n" + "=".repeat(50));
        return 0;
    }

    @Command(name = "undeploy", description = "Remove all components from Kubernetes")
    int undeploy() {
        System.out.println("Removing all components...");
        kubectl("delete", "-f", "kubernetes/route.yaml");
        kubectl("delete", "-f", "kubernetes/main-app.yaml");
        kubectl("delete", "-f", "kubernetes/a2a-agent.yaml");
        kubectl("delete", "-f", "kubernetes/postgresql.yaml");
        kubectl("delete", "-f", "kubernetes/secret.yaml");
        kubectl("delete", "-f", "kubernetes/configmap.yaml");
        kubectl("delete", "-f", "kubernetes/namespace.yaml");
        System.out.println("Undeployment complete!");
        return 0;
    }

    @Command(name = "get-route", description = "Get the external URL for the application")
    int getRoute() {
        System.out.println("Application URL:");
        System.out.println("================");
        System.out.print("https://");
        kubectl("get", "route", "miles-and-smiles", "-n", "miles-and-smiles",
                "-o", "jsonpath={.spec.host}");
        System.out.println("\n");
        return 0;
    }

    @Command(name = "logs-main", description = "View logs from main application")
    int logsMain() {
        return kubectl("logs", "-f", "-l", "app.kubernetes.io/name=miles-and-smiles",
                "-n", "miles-and-smiles");
    }

    @Command(name = "logs-a2a", description = "View logs from A2A agent")
    int logsA2a() {
        return kubectl("logs", "-f", "-l", "app.kubernetes.io/name=miles-and-smiles-a2a",
                "-n", "miles-and-smiles");
    }

    @Command(name = "logs-postgres", description = "View logs from PostgreSQL")
    int logsPostgres() {
        return kubectl("logs", "-f", "-l", "app=postgresql", "-n", "miles-and-smiles");
    }

    @Command(name = "port-forward", description = "Forward main app port to localhost:8080")
    int portForward() {
        System.out.println("Forwarding main application to localhost:8080...");
        System.out.println("This is a fallback option. The app should be accessible via the HTTPRoute.");
        System.out.println("Press Ctrl+C to stop");
        return kubectl("port-forward", "-n", "miles-and-smiles", 
                "service/miles-and-smiles", "8080:80");
    }

    @Command(name = "clean", description = "Clean build artifacts")
    int clean() {
        System.out.println("Cleaning multi-agent-system...");
        runCommand("multi-agent-system", "./mvnw", "clean");
        System.out.println("Cleaning remote-a2a-agent...");
        runCommand("remote-a2a-agent", "./mvnw", "clean");
        System.out.println("Clean complete!");
        return 0;
    }

    private int createSecret(String apiKey) {
        List<String> cmd = new ArrayList<>();
        cmd.add("kubectl");
        cmd.add("create");
        cmd.add("secret");
        cmd.add("generic");
        cmd.add("miles-and-smiles");
        cmd.add("--from-literal=OPENAI_API_KEY=" + apiKey);
        cmd.add("--from-literal=POSTGRES_USER=milesandsmiles");
        cmd.add("--from-literal=POSTGRES_PASSWORD=milesandsmiles123");
        cmd.add("--from-literal=POSTGRES_DB=milesandsmiles");
        cmd.add("--namespace=miles-and-smiles");
        cmd.add("--dry-run=client");
        cmd.add("-o");
        cmd.add("yaml");

        try {
            ProcessBuilder pb1 = new ProcessBuilder(cmd);
            Process p1 = pb1.start();

            ProcessBuilder pb2 = new ProcessBuilder("kubectl", "apply", "-f", "-");
            pb2.redirectInput(ProcessBuilder.Redirect.PIPE);
            Process p2 = pb2.start();

            p1.getInputStream().transferTo(p2.getOutputStream());
            p2.getOutputStream().close();

            int exitCode = p2.waitFor();
            return exitCode;
        } catch (Exception e) {
            System.err.println("Error creating secret: " + e.getMessage());
            return 1;
        }
    }

    private int kubectl(String... args) {
        List<String> cmd = new ArrayList<>();
        cmd.add("kubectl");
        for (String arg : args) {
            cmd.add(arg);
        }
        return runCommand(null, cmd.toArray(new String[0]));
    }

    private int runCommand(String workDir, String... command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            if (workDir != null) {
                pb.directory(new File(workDir));
            }
            pb.inheritIO();
            Process process = pb.start();
            return process.waitFor();
        } catch (Exception e) {
            System.err.println("Error running command: " + e.getMessage());
            return 1;
        }
    }

    @Override
    public Integer call() {
        CommandLine.usage(this, System.out);
        return 0;
    }

    public static void main(String... args) {
        int exitCode = new CommandLine(new deploy()).execute(args);
        System.exit(exitCode);
    }
}