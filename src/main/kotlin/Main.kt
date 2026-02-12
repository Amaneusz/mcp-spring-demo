import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.client.Client
import io.modelcontextprotocol.kotlin.sdk.client.SseClientTransport
import kotlinx.coroutines.runBlocking

fun main() {
//    val url = "https://mcp.deepwiki.com/sse" // This one does not work anymore :)
    val url = "https://mcp.api.coingecko.com/sse"
    val token = ""

    val client = createAsyncClient(url, token)
    runBlocking {
        client.listTools().tools.forEach {
            println("Tool: - ${it.name} - ${it.description}")
        }
    }
}

fun createAsyncClient(url: String, auth: String): Client {
    val client = Client(
        clientInfo = Implementation(
            name = "client",
            version = "1.0.0"
        )
    )

    val httpClient = HttpClient(CIO) {
        install(SSE)
    }

    val transport = SseClientTransport(
        client = httpClient,
        urlString = url,
        requestBuilder = {
            header("Authorization", "Bearer $auth")
        }
    )

    // Connect immediately; subsequent API calls can be made right away.
    runBlocking { client.connect(transport) }

    return client
}