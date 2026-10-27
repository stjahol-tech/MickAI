package com.mickai.pet

import org.junit.Assert.*
import org.junit.Test

class AnimationCatalogTest {
 @Test fun parsesManifest() {
  val specs = AnimationCatalog.fromJson("""{"animations":[{"name":"idle","row":0,"frames":6,"frameMillis":125}]}""")
  assertEquals(6, specs.getValue("idle").frames)
  assertEquals(0, specs.getValue("idle").row)
 }
 @Test fun loopsCorrectly() {
  assertEquals(0, AnimationCatalog.frameAt(750, AnimationSpec(0,6,125)))
  assertEquals(3, AnimationCatalog.frameAt(375, AnimationSpec(0,6,125)))
 }
}
