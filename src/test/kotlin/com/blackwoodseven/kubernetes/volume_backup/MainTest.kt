package com.blackwoodseven.kubernetes.volume_backup

import kotlin.test.assertEquals
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it

class MainTest : Spek({
    describe("the system should be sane") {
        it("should calculate correctly") {
            assertEquals(4, 2 + 2)
        }
    }
})
