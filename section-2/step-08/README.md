# Miles and Smiles - Kubernetes Deployment

This step demonstrates deploying the multi-agent car management system to Kubernetes/OpenShift with two routing options:

1. **OpenShift Route** (recommended for OpenShift) - Auto-generates hostname with TLS
2. **Gateway API HTTPRoute** (for standard Kubernetes) - Requires manual hostname configuration

## Architecture

The deployment consists of:

- **Main Application** (`miles-and-smiles`): Multi-agent system with supervisor pattern
- **A2A Agent** (`miles-and-smiles-a2a`): Remote pricing agent accessed via Agent-to-Agent protocol
- **PostgreSQL**: Database for car inventory and management data

## Prerequisites

- Kubernetes cluster (OpenShift 4.21+ recommended)
- `kubectl` CLI configured
- JBang installed (`curl -Ls https://sh.jbang.dev | bash -s - app setup`)
- OpenAI API key
- Container registry access (Quay.io configured in this example)

## Quick Start

### One-Command Deployment

1. **Set your OpenAI API key:**
   ```bash
   export OPENAI_API_KEY=your-key-here
   ```

2. **Deploy everything with a single command:**
   ```bash
   ./deploy.java all
   ```

This will automatically:
- Build both container images
- Push them to the registry
- Create the Kubernetes namespace
- Create secrets (including your OPENAI_API_KEY)
- Deploy PostgreSQL
- Deploy the A2A agent
- Deploy the main application

3. **Get the application URL:**
   ```bash
   ./deploy.java get-route
   ```

### Step-by-Step Deployment

If you prefer to run each step individually:

1. **Set your OpenAI API key:**
   ```bash
   export OPENAI_API_KEY=your-key-here
   ```

2. **Build container images:**
   ```bash
   ./deploy.java build
   ```

3. **Push images to registry:**
   ```bash
   ./deploy.java push
   ```

4. **Deploy to Kubernetes:**
   ```bash
   ./deploy.java deploy
   ```

5. **Get the application URL:**
   ```bash
   ./deploy.java get-route
   ```

## Available Commands

The `deploy.java` JBang script provides the following commands:

- `./deploy.java all` - **Build, push, and deploy everything in one command** (recommended)
- `./deploy.java build` - Build both container images locally
- `./deploy.java push` - Build and push images to registry
- `./deploy.java deploy` - Deploy all components to Kubernetes (creates secrets)
- `./deploy.java undeploy` - Remove all components
- `./deploy.java get-route` - Get the external application URL
- `./deploy.java logs-main` - View main application logs
- `./deploy.java logs-a2a` - View A2A agent logs
- `./deploy.java logs-postgres` - View PostgreSQL logs
- `./deploy.java port-forward` - Forward app to localhost:8080 (fallback)
- `./deploy.java clean` - Clean build artifacts

## Configuration

### Container Registry

Update the registry settings in both `application.properties` files:

```properties
quarkus.container-image.registry=quay.io
quarkus.container-image.group=your-username
```

### External Access Options

**Option 1: OpenShift Route (Recommended for OpenShift)**

The OpenShift Route (`kubernetes/route.yaml`) automatically generates a hostname and provides TLS termination. This is deployed by default with `./deploy.java all` or `./deploy.java deploy`.

To get the URL:
```bash
./deploy.java get-route
```

**Option 2: Gateway API HTTPRoute (For Standard Kubernetes)**

If you're not using OpenShift, you can use the Gateway API HTTPRoute included in `kubernetes/main-app.yaml`. You'll need to:

1. Remove or skip the OpenShift Route deployment
2. Configure your Gateway controller (e.g., Istio, Envoy Gateway)
3. Update the hostname in `kubernetes/main-app.yaml`:
   ```yaml
   spec:
     hostnames:
       - miles-and-smiles.apps.your-cluster.example.com
   ```

To use Gateway API only, comment out the route deployment in `deploy.java` or manually deploy without `route.yaml`.

### OpenAI Configuration

The OpenAI base URL and model can be configured in `kubernetes/configmap.yaml`:

```yaml
data:
  OPENAI_BASE_URL: "https://api.openai.com/v1"
  OPENAI_MODEL_NAME: "gpt-4o"
```

### A2A Agent Configuration

The main application connects to the remote A2A pricing agent using the Agent-to-Agent protocol. The A2A client discovers the agent's endpoint by fetching the agent card from `/.well-known/agent-card.json`.

**Development Mode:**
- Main app: `http://localhost:8080`
- A2A agent: `http://localhost:8888` (to avoid port conflicts)

**Production Mode (Kubernetes):**
- Main app: `http://miles-and-smiles.miles-and-smiles.svc.cluster.local`
- A2A agent: `http://miles-and-smiles-a2a.miles-and-smiles.svc.cluster.local` (port 80 → 8080)

#### Agent Card Configuration

The A2A agent card must return the correct URL for the environment. This is configured in the A2A agent's `application.properties`:

**A2A Agent** (`remote-a2a-agent/src/main/resources/application.properties`):
```properties
# Base URL for agent card - used by PricingAgentCard
a2a.base-url=http://localhost:8888/
%prod.a2a.base-url=http://miles-and-smiles-a2a.miles-and-smiles.svc.cluster.local/
```

The `PricingAgentCard` class injects this property to generate the correct URLs in the agent card:

```java
@ConfigProperty(name = "a2a.base-url", defaultValue = "http://localhost:8888/")
String baseUrl;
```

This ensures the agent card returns the Kubernetes service URL in production, allowing the main application to connect correctly.

#### Client Configuration

The main application's `PricingAgent` interface specifies where to fetch the agent card:

```java
@A2AClientAgent(
    a2aServerUrl = "http://miles-and-smiles-a2a.miles-and-smiles.svc.cluster.local",
    ...
)
```

The A2A client fetches the agent card from this URL, extracts the actual endpoint URL from the card, and uses that for all subsequent communication.

**Important:** The A2A agent must be running and accessible before the main application starts, as it fetches the agent card during initialization.

## Kubernetes Resources

The deployment creates the following resources in the `miles-and-smiles` namespace:

- **Namespace**: `miles-and-smiles`
- **ConfigMap**: Application configuration (database, A2A URL, OpenAI settings)
- **Secret**: Sensitive data (OpenAI API key, database credentials)
- **PostgreSQL Deployment**: Database with ephemeral storage
- **PostgreSQL Service**: Internal database access
- **Main App Deployment**: Multi-agent system
- **Main App Service**: Internal service access
- **A2A Agent Deployment**: Remote pricing agent
- **A2A Agent Service**: Internal A2A access
- **HTTPRoute**: External access via Gateway API

## Database

PostgreSQL is deployed with:
- Image: `docker.io/library/postgres:18`
- Ephemeral storage (data lost on pod restart)
- Credentials stored in Kubernetes Secret
- Health probes for liveness and readiness

For production, consider using a StatefulSet with persistent volumes.

## Health Checks

Both applications include Quarkus SmallRye Health endpoints:

- `/q/health/live` - Liveness probe
- `/q/health/ready` - Readiness probe
- `/q/health/started` - Startup probe

## Troubleshooting

### Check pod status:
```bash
kubectl get pods -n miles-and-smiles
```

### View logs:
```bash
./deploy.java logs-main
./deploy.java logs-a2a
./deploy.java logs-postgres
```

### Describe resources:
```bash
kubectl describe deployment miles-and-smiles -n miles-and-smiles
kubectl describe httproute miles-and-smiles -n miles-and-smiles
```

### Access via port-forward (if route is not working):
```bash
./deploy.java port-forward
# Then open http://localhost:8080
```

## Development vs Production

The application uses Quarkus profiles:

- **Dev mode** (`%dev`): Uses localhost PostgreSQL, port 8888 for A2A agent
- **Production** (`%prod`): Uses Kubernetes services, environment variables from secrets/configmaps

## Next Steps

- Configure persistent storage for PostgreSQL
- Set up monitoring and observability
- Configure resource limits and requests
- Set up horizontal pod autoscaling
- Configure TLS/SSL for HTTPRoute