package com.bellatrixos.desktop

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

/** Menerima field `type` berupa angka maupun string lama. */
class DesktopPinItemDeserializer : JsonDeserializer<DesktopItemPins> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): DesktopItemPins {
        val obj = json.asJsonObject
        val name = obj.get("name").asString
        val data = obj.get("data")?.takeUnless { it.isJsonNull }?.asString
        val position = obj.get("position").asInt
        val typeElement = obj.get("type")
        val type = if (typeElement.isJsonPrimitive && typeElement.asJsonPrimitive.isString) {
            when (typeElement.asString) {
                "ui_app" -> 0
                "apk_app" -> 1
                "folder" -> 3
                "file" -> 2
                else -> -1
            }
        } else {
            typeElement.asInt
        }
        return DesktopItemPins(type, position, name, data)
    }

    companion object {
        @JvmField val f4760a: HashMap<String, Int> = hashMapOf(
            "ui_app" to 0,
            "folder" to 3,
            "apk_app" to 1,
            "file" to 2
        )
    }
}
