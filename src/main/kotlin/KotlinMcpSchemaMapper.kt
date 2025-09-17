import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class Parameter(
    val id: String,
    val name: String,
    val description: String?,
    val type: ParameterType,
    val required: Boolean
)

data class ParameterType(
    val type: DataType,
    val listItemType: ParameterType?,
    val fields: List<Parameter>?
)

enum class DataType {
    STRING, NUMBER, DATE, BOOLEAN, LIST, OBJECT
}


/**
 * Converts a Kotlin MCP SDK Tool.Input (or any equivalent shape) to our List<Parameter> model.
 * This implementation relies only on the presence of the following fields on the passed [schema] instance:
 * - properties: kotlinx.serialization.json.JsonObject
 * - required: List<String>? (optional)
 *
 * Notes:
 * - We intentionally accept [schema] as Any to avoid taking a hard dependency on a specific Tool.Input class
 *   package name. The call site passes the Kotlin SDK's Tool.Input, which has the expected fields.
 */
fun jsonSchemaToParameters(schema: Any?, @Suppress("UNUSED_PARAMETER") objectMapper: ObjectMapper): List<Parameter> {
    if (schema == null) return emptyList()

    // Reflectively extract `properties: JsonObject` and `required: List<String>?` from Tool.Input
    val (rootProperties, rootRequired) = extractPropertiesAndRequired(schema) ?: return emptyList()

    return jsonObjectToParameters(rootProperties, rootRequired)
}

private fun extractPropertiesAndRequired(instance: Any): Pair<JsonObject, List<String>?>? {
    return try {
        val kClass = instance::class
        val propsMember = kClass.members.firstOrNull { it.name == "properties" }
        val requiredMember = kClass.members.firstOrNull { it.name == "required" }

        val properties = propsMember?.call(instance) as? JsonObject ?: return null
        val required = requiredMember?.call(instance) as? List<*>
        @Suppress("UNCHECKED_CAST")
        val requiredStrings = required?.filterIsInstance<String>()

        properties to requiredStrings
    } catch (_: Throwable) {
        null
    }
}

private fun jsonObjectToParameters(properties: JsonObject, required: List<String>?): List<Parameter> {
    return properties.map { (name, element) ->
        val obj = element.asJsonObjectOrNull() ?: JsonObject(emptyMap())
        val description = obj["description"]?.jsonPrimitive?.contentOrNull()

        Parameter(
            id = name,
            name = name,
            description = description,
            type = jsonElementToParameterType(obj),
            required = required?.contains(name) == true
        )
    }
}

private fun jsonElementToParameterType(obj: JsonObject): ParameterType {
    val explicitType = obj["type"]?.jsonPrimitive?.contentOrNull()
    val inferredType = when {
        explicitType != null -> explicitType
        obj.containsKey("properties") -> "object"
        obj.containsKey("items") -> "array"
        else -> null
    }

    val dataType = mapJsonTypeToDataType(inferredType)

    return when (dataType) {
        DataType.OBJECT -> {
            val nestedProps = obj["properties"].asJsonObjectOrNull()
            val nestedRequired = obj["required"].asStringListOrNull()
            val fields = nestedProps?.let { jsonObjectToParameters(it, nestedRequired) } ?: emptyList()
            ParameterType(DataType.OBJECT, null, fields)
        }
        DataType.LIST -> {
            val itemsObj = obj["items"].asJsonObjectOrNull()
            val itemType = itemsObj?.let { jsonElementToParameterType(it) }
            ParameterType(DataType.LIST, itemType, null)
        }
        DataType.STRING, DataType.NUMBER, DataType.BOOLEAN -> {
            ParameterType(dataType, null, null)
        }
        else -> {
            // Fallback for additional DataType variants (e.g., DATE)
            ParameterType(dataType, null, null)
        }
    }
}

// --- Helpers ---

private fun JsonElement?.asJsonObjectOrNull(): JsonObject? = try {
    this?.jsonObject
} catch (_: Throwable) {
    null
}

private fun JsonElement?.asStringListOrNull(): List<String>? {
    return try {
        val arr: JsonArray = this?.jsonArray ?: return null
        arr.mapNotNull { it.jsonPrimitive.contentOrNull() }
    } catch (_: Throwable) {
        null
    }
}

private fun JsonPrimitive?.contentOrNull(): String? = try {
    this?.content
} catch (_: Throwable) {
    null
}

fun mapJsonTypeToDataType(type: String?): DataType = when (type) {
    "string" -> DataType.STRING
    "number", "integer" -> DataType.NUMBER
    "boolean" -> DataType.BOOLEAN
    "object" -> DataType.OBJECT
    "array" -> DataType.LIST
    else -> DataType.STRING
}
