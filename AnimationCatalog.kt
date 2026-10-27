package com.mickai.pet

import org.json.JSONObject

data class AnimationSpec(val row: Int, val frames: Int, val frameMillis: Long = 125L)

object AnimationCatalog {
 fun fromJson(json: String): Map<String, AnimationSpec> {
  val root = JSONObject(json)
  val states = root.getJSONArray("animations")
  return buildMap {
   for (i in 0 until states.length()) {
    val item = states.getJSONObject(i)
    put(item.getString("name"), AnimationSpec(item.getInt("row"), item.getInt("frames"), item.optLong("frameMillis",125)))
   }
  }
 }
 fun frameAt(elapsedMs: Long, spec: AnimationSpec): Int =
  ((elapsedMs.coerceAtLeast(0) / spec.frameMillis.coerceAtLeast(1)) % spec.frames.coerceAtLeast(1)).toInt()
}
