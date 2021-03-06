package com.netflix.spinnaker.keel.api

import com.netflix.spinnaker.keel.plugin.Halt
import com.netflix.spinnaker.keel.plugin.Proceed
import com.netflix.spinnaker.keel.plugin.VetoPlugin
import com.netflix.spinnaker.kork.dynamicconfig.DynamicConfigService
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.whenever
import dev.minutest.junit.JUnit5Minutests
import dev.minutest.rootContext
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import java.util.*

internal object SimpleVetoPluginSpec : JUnit5Minutests {

  data class Fixture(
    val dynamicConfigService: DynamicConfigService,
    val request: Resource<*>
  ) {
    val subject: VetoPlugin = SimpleVetoPlugin(dynamicConfigService)
  }

  fun tests() = rootContext<Fixture> {
    fixture {
      Fixture(
        dynamicConfigService = mock(),
        request = Resource(
          apiVersion = SPINNAKER_API_V1,
          kind = "ec2.SecurityGroup",
          metadata = ResourceMetadata(
            name = ResourceName("ec2.SecurityGroup:keel:prod:us-east-1:keel"),
            uid = UUID.randomUUID(),
            resourceVersion = 1234L
          ),
          spec = randomData()
        )
      )
    }

    context("convergence is enabled") {
      before {
        whenever(dynamicConfigService.isEnabled("keel.converge.enabled", false)) doReturn true
      }

      after {
        reset(dynamicConfigService)
      }

      test("it approves resource convergence") {
        expectThat(subject.allow(request)).isEqualTo(Proceed)
      }
    }

    context("convergence is disabled") {
      before {
        whenever(dynamicConfigService.isEnabled("keel.converge.enabled", false)) doReturn false
      }

      after {
        reset(dynamicConfigService)
      }

      test("it denies resource convergence") {
        expectThat(subject.allow(request)).isA<Halt>()
      }
    }
  }
}

fun randomData(length: Int = 4): Map<String, Any> {
  val map = mutableMapOf<String, Any>()
  (0 until length).forEach { _ ->
    map[randomString()] = randomString()
  }
  return map
}

fun randomString(length: Int = 8) =
  UUID.randomUUID()
    .toString()
    .map { it.toInt().toString(16) }
    .joinToString("")
    .substring(0 until length)
