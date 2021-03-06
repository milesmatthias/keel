/*
 * Copyright 2018 Netflix, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.spinnaker.keel.rest

import com.netflix.spinnaker.keel.api.Resource
import com.netflix.spinnaker.keel.api.ResourceName
import com.netflix.spinnaker.keel.events.ResourceEvent
import com.netflix.spinnaker.keel.events.ResourceEventType.CREATE
import com.netflix.spinnaker.keel.events.ResourceEventType.DELETE
import com.netflix.spinnaker.keel.events.ResourceEventType.UPDATE
import com.netflix.spinnaker.keel.persistence.NoSuchResourceException
import com.netflix.spinnaker.keel.persistence.ResourceRepository
import com.netflix.spinnaker.keel.persistence.get
import com.netflix.spinnaker.keel.yaml.APPLICATION_YAML_VALUE
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(path = ["/resources"])
class ResourceController(
  private val publisher: ApplicationEventPublisher,
  private val resourceRepository: ResourceRepository
) {

  private val log by lazy { LoggerFactory.getLogger(javaClass) }

  @PostMapping(
    consumes = [APPLICATION_YAML_VALUE, APPLICATION_JSON_VALUE],
    produces = [APPLICATION_YAML_VALUE, APPLICATION_JSON_VALUE]
  )
  fun create(@RequestBody resource: Resource<*>): Resource<*> {
    // TODO: we need to take the resource type as well so we can actually parse and validate here
    log.info("Creating: $resource")
    publisher.publishEvent(ResourceEvent(CREATE, resource))
    return resource
  }

  @GetMapping(
    path = ["/{name}"],
    produces = [APPLICATION_YAML_VALUE, APPLICATION_JSON_VALUE]
  )
  fun get(@PathVariable("name") name: ResourceName): Resource<Any> {
    log.info("Getting: $name")
    return resourceRepository.get(name)
  }

  @PutMapping(
    path = ["/{name}"],
    produces = [APPLICATION_YAML_VALUE, APPLICATION_JSON_VALUE]
  )
  fun update(@PathVariable("name") name: ResourceName, @RequestBody resource: Resource<*>): Resource<*> {
    log.info("Updating: $resource")
    publisher.publishEvent(ResourceEvent(UPDATE, resource))
    return resource
  }

  @DeleteMapping(
    path = ["/{name}"],
    produces = [APPLICATION_YAML_VALUE, APPLICATION_JSON_VALUE]
  )
  fun delete(@PathVariable("name") name: ResourceName): Resource<*> {
    log.info("Deleting: $name")
    val resource = resourceRepository.get<Any>(name)
    publisher.publishEvent(ResourceEvent(DELETE, resource))
    return resource
  }

  @ExceptionHandler(NoSuchResourceException::class)
  @ResponseStatus(NOT_FOUND)
  fun onNotFound(e: NoSuchResourceException) {
    log.error(e.message)
  }
}
