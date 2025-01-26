# Step 10 - Serving the model in pure Java with Jlama

So far we relied on OpenAI to serve the LLM that we used to build our application, but the quarkus-langchain4j integration makes it straightforward to integrate any other service provider. For instance we could serve our model on our local machine through an [Ollama](https://ollama.com/) server. Even better we may want to serve it in Java and directly embedded in our Quarkus application without the need of querying an external service through REST calls. In this step we will see how to make this possible through [Jlama](https://github.com/tjake/Jlama).

## Introducing Jlama

Jlama is a library allowing to execute LLM inference in pure Java. It supports many LLM model families like Llama, Mistral, Qwen2 and Granite. It also implements out-of-the-box many useful LLM related features like functions calling, models quantization, mixture of experts and even distributed inference.

The final code of this step is available in the `step-10` directory.

## Adding Jlama dependencies

Jlama is well integrated with Quarkus through the dedicated langchain4j based extension. Note that for performance reasons Jlama uses the [Vector API](https://openjdk.org/jeps/469) which is still in preview in Java 23, and very likely will be released as a supported feature in Java 25.

For this reason the first step to do is enabling the `quarkus-maven-plugin` in our pom file to use this preview API, by adding the following configuration to it.

```xml title="pom.xml"
<configuration>
    <jvmArgs>--enable-preview --enable-native-access=ALL-UNNAMED</jvmArgs>
    <modules>
        <module>jdk.incubator.vector</module>
    </modules>
</configuration>
```

Then in the same file we must add the necessary dependencies to Jlama and the corresponding quarkus-langchain4j extension. This extension has to be used as an alternative to the openai one, so we could move that dependency in a profile (active by default) and put the Jlama ones into a different profile.

```xml title="pom.xml"
<profiles>
    <profile>
        <id>openai</id>
        <activation>
            <activeByDefault>true</activeByDefault>
            <property>
                <name>openai</name>
            </property>
        </activation>
        <dependencies>
            <dependency>
                <groupId>io.quarkiverse.langchain4j</groupId>
                <artifactId>quarkus-langchain4j-openai</artifactId>
                <version>${quarkus-langchain4j.version}</version>
            </dependency>
        </dependencies>
    </profile>
    <profile>
        <id>jlama</id>
        <activation>
            <property>
                <name>jlama</name>
            </property>
        </activation>
        <dependencies>
            <dependency>
                <groupId>io.quarkiverse.langchain4j</groupId>
                <artifactId>quarkus-langchain4j-jlama</artifactId>
                <version>${quarkus-langchain4j.version}</version>
            </dependency>
            <dependency>
                <groupId>com.github.tjake</groupId>
                <artifactId>jlama-core</artifactId>
                <version>${jlama.version}</version>
            </dependency>
            <dependency>
                <groupId>com.github.tjake</groupId>
                <artifactId>jlama-native</artifactId>
                <version>${jlama.version}</version>
                <classifier>${os.detected.classifier}</classifier>
            </dependency>
        </dependencies>
    </profile>
</profiles>
```

## Configuring Jlama

After having added the required dependencies it is now only necessary to configure the LLM served by Jlama adding the following entries to the `application.properties` file.

```properties
quarkus.langchain4j.jlama.chat-model.model-name=tjake/Llama-3.2-1B-Instruct-JQ4
quarkus.langchain4j.jlama.chat-model.temperature=0
quarkus.langchain4j.jlama.log-requests=true
quarkus.langchain4j.jlama.log-responses=true
```

Here we configured a relatively small model taken from the Huggingface repository of the Jlama main maintainer, but you can choose any other model. When the application is compiled for the first time the model is automatically downloaded locally by Jlama from Huggingface.

## Compiling and running the project

This time it is not advised to launch the Quarkus application in dev mode as we have done so far. This is because at the moment the dev mode disables the Java Hotspot C2 compilation, making Jlama extremely slow. 

Since we won't launch our application in dev mode anymore, we won't also be able to leverage the dev services normally available in that scenario, so we will simplify a bit the application to remove the need of those services, in particular getting rid of all the database connection used to store the customers and their bookings and for the embedding required by RAG. However we could keep using the RAG capabilities we developed by simply replacing the pgvector-based embedding store with the in-memory version provided out-of-the-box by LangChain4j. To do so we have to remove the `quarkus-langchain4j-pgvector` dependency and programmatically inject the `InMemoryEmbeddingStore` in the CDI context by adding the following class.

```java
package dev.langchain4j.quarkus.workshop;

import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

public class RagInMemoryStoreCreator {

    @Produces
    @ApplicationScoped
    public EmbeddingStore create() {
        return new InMemoryEmbeddingStore();
    }
}
```

Finally we are now ready to compile and package the project using the `jlama` profile

```shell
./mvnw clean package -Pjlama
```

and to launch the jar containing the web application using again all the flag necessary to enable the Vector API.

```shell
java -jar --enable-preview --enable-native-access=ALL-UNNAMED --add-modules jdk.incubator.vector target/quarkus-app/quarkus-run.jar
```

Now we can go back again to our chatbot and test the RAG pattern as we did in the step 05, but this time running the LLM inference engine directly embedded in our Java application and without using any external services. Open the browser at [http://localhost:8080](http://localhost:8080){target="_blank"} and ask a question related to the cancellation policy.

```
What can you tell me about your cancellation policy?
```

Note that it might take a bit longer than ChatGPT to answer the question.

![RAG with Jlama](images/chat-rag-with-jlama.png)